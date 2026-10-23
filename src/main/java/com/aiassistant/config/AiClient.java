package com.aiassistant.config;

import com.aiassistant.common.BusinessException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * 大模型客户端封装：通过智谱 OpenAI 兼容接口创建对话模型与向量模型。
 * <p>
 * 模型实例懒加载并缓存；未配置 {@code AI_API_KEY} 时，应用仍可启动，
 * 但真正调用时会抛出业务异常。
 */
@Component
@RequiredArgsConstructor
public class AiClient {

    private final AssistantProperties properties;

    /** 对话模型（glm-4.6v），双重检查锁定懒初始化。 */
    private volatile ChatModel chatModel;

    /** 嵌入模型（embedding-3），用于 RAG 入库与检索。 */
    private volatile EmbeddingModel embeddingModel;

    /** 获取对话模型；首次调用时按配置构建 OpenAiChatModel。 */
    public ChatModel chatModel() {
        ChatModel local = chatModel;
        if (local == null) {
            synchronized (this) {
                if (chatModel == null) {
                    AssistantProperties.Ai ai = requireAi();
                    chatModel = OpenAiChatModel.builder()
                            .baseUrl(ai.getBaseUrl())
                            .apiKey(ai.getApiKey())
                            .modelName(ai.getChatModel())
                            .temperature(ai.getTemperature())
                            .timeout(Duration.ofSeconds(120))
                            .build();
                }
                local = chatModel;
            }
        }
        return local;
    }

    /** 获取嵌入模型；首次调用时按配置构建 OpenAiEmbeddingModel。 */
    public EmbeddingModel embeddingModel() {
        EmbeddingModel local = embeddingModel;
        if (local == null) {
            synchronized (this) {
                if (embeddingModel == null) {
                    AssistantProperties.Ai ai = requireAi();
                    embeddingModel = OpenAiEmbeddingModel.builder()
                            .baseUrl(ai.getBaseUrl())
                            .apiKey(ai.getApiKey())
                            .modelName(ai.getEmbeddingModel())
                            .timeout(Duration.ofSeconds(90))
                            .build();
                }
                local = embeddingModel;
            }
        }
        return local;
    }

    /** 是否已配置 API Key，供健康检查与种子任务跳过判断。 */
    public boolean configured() {
        AssistantProperties.Ai ai = properties.getAi();
        return ai != null && StringUtils.hasText(ai.getApiKey());
    }

    /** 校验 AI 配置完整性，缺 Key 或 base-url 时抛业务异常。 */
    private AssistantProperties.Ai requireAi() {
        AssistantProperties.Ai ai = properties.getAi();
        if (ai == null || !StringUtils.hasText(ai.getApiKey())) {
            throw new BusinessException("未配置环境变量 AI_API_KEY，无法调用大模型");
        }
        if (!StringUtils.hasText(ai.getBaseUrl())) {
            throw new BusinessException("未配置 assistant.ai.base-url");
        }
        return ai;
    }
}
