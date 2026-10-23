package com.aiassistant.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 应用自定义配置，对应 application.yml 中 {@code assistant.*}。
 */
@Data
@Component
@ConfigurationProperties(prefix = "assistant")
public class AssistantProperties {

    /** 大模型相关：地址、Key、模型名、温度。 */
    private Ai ai = new Ai();

    /** RAG 切分与检索参数。 */
    private Rag rag = new Rag();

    /** 智谱 OpenAI 兼容接口配置；apiKey 通常来自环境变量 AI_API_KEY。 */
    @Data
    public static class Ai {
        /** 接口根地址，如 https://open.bigmodel.cn/api/paas/v4 */
        private String baseUrl;
        /** API Key，未配置时对话与入库不可用。 */
        private String apiKey;
        /** 对话模型名，默认 glm-4.6v。 */
        private String chatModel;
        /** 向量模型名，默认 embedding-3。 */
        private String embeddingModel;
        /** 采样温度，偏低更利于事实性回答。 */
        private Double temperature = 0.2;
    }

    /** 知识库切分与相似度检索阈值。 */
    @Data
    public static class Rag {
        /** 文档切分块大小（字符近似）。 */
        private int chunkSize = 400;
        /** 相邻块重叠长度，减轻边界语义断裂。 */
        private int chunkOverlap = 60;
        /** 检索返回的最大片段数。 */
        private int topK = 3;
        /** 最低相似度分数，低于此值的命中丢弃。 */
        private double minScore = 0.35;
    }
}
