package com.aiassistant.service;

import com.aiassistant.ai.ShopAssistant;
import com.aiassistant.config.AiClient;
import com.aiassistant.dto.ChatRequest;
import com.aiassistant.dto.ChatResponse;
import com.aiassistant.dto.SourceVO;
import com.aiassistant.entity.ChatRecord;
import com.aiassistant.mapper.ChatRecordMapper;
import com.aiassistant.rag.KnowledgeRetriever;
import com.aiassistant.tool.OrderTools;
import com.aiassistant.tool.ToolTrace;
import com.aiassistant.tool.WeatherTools;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对话核心服务：组装 LangChain4j AiServices（模型 + 记忆 + RAG + 工具），
 * 按会话串行处理提问，并将问答与工具/引用信息写入 chat_record。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {

    private final AiClient aiClient;
    private final OrderTools orderTools;
    private final WeatherTools weatherTools;
    private final KnowledgeRetriever knowledgeRetriever;
    private final ChatRecordMapper chatRecordMapper;
    private final ObjectMapper objectMapper;

    /** 按 sessionId 缓存的内存窗口，避免每次从库全量重建。 */
    private final ConcurrentHashMap<String, ChatMemory> memories = new ConcurrentHashMap<>();

    /** 同一会话串行化，防止多请求交错导致记忆与 Trace 错乱。 */
    private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

    /** 懒加载的 AiServices 代理，双重检查保证只构建一次。 */
    private volatile ShopAssistant assistant;

    /**
     * 处理一次对话请求。
     * <ol>
     *   <li>解析或生成 sessionId</li>
     *   <li>按会话加锁，清空本轮工具/RAG 追踪</li>
     *   <li>调用大模型（可能触发订单/天气 Tool 与知识库检索）</li>
     *   <li>收集工具与引用，落库用户消息与助手回答</li>
     * </ol>
     * <p>
     * 用户消息与助手回答在同一事务中写入，避免只落库一半。
     */
    @Transactional
    public ChatResponse chat(ChatRequest request) {
        // 无 sessionId 时新建会话，保证前端可续聊
        String sessionId = StringUtils.hasText(request.getSessionId())
                ? request.getSessionId().trim()
                : UUID.randomUUID().toString().replace("-", "");
        String message = request.getMessage().trim();

        // 同一会话串行执行，避免并发读写 memory / ThreadLocal 追踪
        Object lock = locks.computeIfAbsent(sessionId, key -> new Object());
        synchronized (lock) {
            ToolTrace.clear();
            knowledgeRetriever.clear();
            String answer;
            try {
                answer = assistant().chat(sessionId, message);
            } catch (RuntimeException exception) {
                // 失败时丢弃内存中的会话缓存，下次从库重新加载，避免脏上下文
                memories.remove(sessionId);
                ToolTrace.clear();
                knowledgeRetriever.clear();
                throw exception;
            }

            // 取出本轮追踪结果（poll 后 ThreadLocal 清空）
            List<String> tools = ToolTrace.poll();
            List<SourceVO> sources = knowledgeRetriever.poll();

            // 用户消息与助手回答分别入库；工具与引用只挂在助手侧
            save(sessionId, "user", message, null, null);
            save(sessionId, "assistant", answer, writeJson(tools), writeJson(sources));

            ChatResponse response = new ChatResponse();
            response.setSessionId(sessionId);
            response.setAnswer(answer);
            response.setTools(tools);
            response.setSources(sources);
            return response;
        }
    }

    /**
     * 懒构建 ShopAssistant：绑定聊天模型、按会话记忆、RAG 检索器与订单/天气工具。
     */
    private ShopAssistant assistant() {
        ShopAssistant local = assistant;
        if (local == null) {
            synchronized (this) {
                if (assistant == null) {
                    assistant = AiServices.builder(ShopAssistant.class)
                            .chatModel(aiClient.chatModel())
                            .chatMemoryProvider(this::memory)
                            .contentRetriever(knowledgeRetriever)
                            .tools(orderTools, weatherTools)
                            .build();
                }
                local = assistant;
            }
        }
        return local;
    }

    /** 按 sessionId 提供聊天记忆；首次访问时从数据库回放最近消息。 */
    private ChatMemory memory(Object memoryId) {
        String sessionId = String.valueOf(memoryId);
        return memories.computeIfAbsent(sessionId, this::loadMemory);
    }

    /**
     * 从 chat_record 加载最近 20 条消息，按时间正序填入滑动窗口，用于服务重启后的多轮续聊。
     */
    private ChatMemory loadMemory(String sessionId) {
        MessageWindowChatMemory memory = MessageWindowChatMemory.withMaxMessages(20);
        List<ChatRecord> records = chatRecordMapper.selectList(new LambdaQueryWrapper<ChatRecord>()
                .eq(ChatRecord::getSessionId, sessionId)
                .orderByDesc(ChatRecord::getId)
                .last("limit 20"));
        // 库中按 id 倒序查出，再反转为时间正序再写入 memory
        List<ChatRecord> chronological = new ArrayList<>(records);
        Collections.reverse(chronological);
        for (ChatRecord record : chronological) {
            if ("user".equals(record.getRole())) {
                memory.add(dev.langchain4j.data.message.UserMessage.from(record.getContent()));
            } else if ("assistant".equals(record.getRole())) {
                memory.add(AiMessage.from(record.getContent()));
            }
        }
        return memory;
    }

    /** 将单条对话写入 chat_record。 */
    private void save(String sessionId, String role, String content, String toolsJson, String sourcesJson) {
        ChatRecord record = new ChatRecord();
        record.setSessionId(sessionId);
        record.setRole(role);
        record.setContent(content);
        record.setToolsJson(toolsJson);
        record.setSourcesJson(sourcesJson);
        record.setCreatedAt(LocalDateTime.now());
        chatRecordMapper.insert(record);
    }

    /** 工具列表 / 引用列表序列化为 JSON；失败时降级为 "[]"，不影响主流程落库。 */
    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            log.warn("对话附加信息序列化失败", exception);
            return "[]";
        }
    }
}
