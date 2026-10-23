package com.aiassistant.ai;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * LangChain4j AiServices 对话接口。
 * 由 AssistantService 注入模型、记忆、RAG 与工具后生成代理实现。
 */
public interface ShopAssistant {

    /**
     * 发送一轮用户消息。
     *
     * @param sessionId 会话 ID，对应 ChatMemory
     * @param message   用户问题
     * @return 助手回答
     */
    @SystemMessage("""
            你是购物助手，用简体中文回答。
            订单、金额、物流状态必须调用订单工具，禁止编造订单。
            天气必须调用天气工具，禁止编造天气。
            退换货、运费、会员积分等问题只依据检索到的知识库片段；没有依据时明确说不知道。
            回答简洁，先给结论。
            """)
    String chat(@MemoryId String sessionId, @UserMessage String message);
}
