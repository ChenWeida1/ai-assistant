package com.aiassistant.rag;

import com.aiassistant.config.AiClient;
import com.aiassistant.config.AssistantProperties;
import com.aiassistant.dto.SourceVO;
import com.aiassistant.service.VectorStoreService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 内容检索器：将用户问题向量化后在内存知识库中检索相关片段。
 * <p>
 * 由 AiServices 在对话时自动调用；命中片段通过 ThreadLocal 暂存，
 * 供 {@link com.aiassistant.service.AssistantService} 在响应中返回给前端。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeRetriever implements ContentRetriever {

    private final VectorStoreService vectorStoreService;
    private final AiClient aiClient;
    private final AssistantProperties properties;

    /** 本轮检索命中，供前端展示「引用」；poll/clear 后清空。 */
    private final ThreadLocal<List<SourceVO>> last = new ThreadLocal<>();

    /**
     * 执行一次检索：问题嵌入 → topK/minScore 过滤 → 返回给模型的 Content，
     * 同时缓存 SourceVO 供 API 响应。
     * <p>
     * 嵌入或检索失败时降级为空结果，避免拖垮整轮对话（订单/天气等 Tool 仍可继续）。
     */
    @Override
    public List<Content> retrieve(Query query) {
        try {
            AssistantProperties.Rag rag = properties.getRag();
            Embedding embedding = aiClient.embeddingModel().embed(query.text()).content();
            List<EmbeddingMatch<TextSegment>> matches = vectorStoreService.search(
                    embedding, rag.getTopK(), rag.getMinScore());
            List<SourceVO> sources = new ArrayList<>();
            List<Content> contents = new ArrayList<>();
            for (EmbeddingMatch<TextSegment> match : matches) {
                TextSegment segment = match.embedded();
                SourceVO source = new SourceVO();
                source.setTitle(segment.metadata().getString("title"));
                source.setContent(segment.text());
                source.setScore(match.score());
                sources.add(source);
                contents.add(Content.from(segment));
            }
            last.set(sources);
            return contents;
        } catch (RuntimeException exception) {
            log.warn("知识库检索失败，本轮跳过 RAG: {}", exception.getMessage());
            last.set(List.of());
            return List.of();
        }
    }

    /** 取出并清空本轮引用列表。 */
    public List<SourceVO> poll() {
        List<SourceVO> sources = last.get();
        last.remove();
        return sources == null ? List.of() : sources;
    }

    /** 对话开始前清空，避免串会话残留。 */
    public void clear() {
        last.remove();
    }
}
