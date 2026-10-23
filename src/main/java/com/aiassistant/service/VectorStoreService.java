package com.aiassistant.service;

import com.aiassistant.entity.KbChunk;
import com.aiassistant.entity.KbDocument;
import com.aiassistant.mapper.KbChunkMapper;
import com.aiassistant.mapper.KbDocumentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 进程内向量索引封装（LangChain4j {@link InMemoryEmbeddingStore}）。
 * <p>
 * 不使用外部向量库：检索走内存；持久化依赖 MySQL {@code kb_chunk}，
 * 启动时 {@link #rebuild()} 从库恢复索引。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorStoreService {

    private final KbChunkMapper chunkMapper;
    private final KbDocumentMapper documentMapper;
    private final ObjectMapper objectMapper;

    /** 运行时相似度检索所用的内存向量库。 */
    private final InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

    /**
     * 启动时从 kb_chunk 重建内存索引。
     * 失败则空索引运行，避免阻断应用启动。
     */
    @PostConstruct
    public void rebuild() {
        try {
            List<KbChunk> chunks = chunkMapper.selectList(null);
            if (chunks.isEmpty()) {
                return;
            }
            // 预先加载标题，避免每个 chunk 再查一次文档表
            Map<Long, String> titles = new HashMap<>();
            for (KbDocument document : documentMapper.selectList(null)) {
                titles.put(document.getId(), document.getTitle());
            }
            List<String> ids = new ArrayList<>();
            List<Embedding> embeddings = new ArrayList<>();
            List<TextSegment> segments = new ArrayList<>();
            for (KbChunk chunk : chunks) {
                float[] vector = objectMapper.readValue(chunk.getEmbedding(), float[].class);
                ids.add(chunk.getEmbeddingId());
                embeddings.add(Embedding.from(vector));
                segments.add(toSegment(chunk, titles.get(chunk.getDocumentId())));
            }
            store.addAll(ids, embeddings, segments);
            log.info("向量库已从数据库恢复 {} 条", ids.size());
        } catch (Exception exception) {
            log.warn("启动时重建向量库失败，将以空索引运行: {}", exception.getMessage());
        }
    }

    /** 批量写入向量，返回库内条目 ID 列表（写入 kb_chunk.embedding_id）。 */
    public synchronized List<String> add(List<Embedding> embeddings, List<TextSegment> segments) {
        return store.addAll(embeddings, segments);
    }

    /** 按文档 id 删除内存中所有相关向量（与 MySQL 删除配套）。 */
    public synchronized void removeByDocumentId(Long documentId) {
        store.removeAll(MetadataFilterBuilder.metadataKey("documentId").isEqualTo(String.valueOf(documentId)));
    }

    /**
     * 相似度检索。
     *
     * @param query    查询向量
     * @param topK     最多返回条数
     * @param minScore 最低分数阈值
     */
    public synchronized List<EmbeddingMatch<TextSegment>> search(Embedding query, int topK, double minScore) {
        return store.search(EmbeddingSearchRequest.builder()
                .queryEmbedding(query)
                .maxResults(topK)
                .minScore(minScore)
                .build()).matches();
    }

    /** 将向量序列化为 JSON 数组字符串，便于存入 MEDIUMTEXT。 */
    public String toJson(Embedding embedding) {
        try {
            return objectMapper.writeValueAsString(embedding.vector());
        } catch (Exception exception) {
            throw new IllegalStateException("向量序列化失败", exception);
        }
    }

    /** 从库记录还原带元数据的 TextSegment，供检索命中后展示标题等。 */
    private TextSegment toSegment(KbChunk chunk, String title) {
        Metadata metadata = new Metadata();
        metadata.put("documentId", String.valueOf(chunk.getDocumentId()));
        metadata.put("title", title == null ? "" : title);
        metadata.put("chunkIndex", String.valueOf(chunk.getChunkIndex()));
        return TextSegment.from(chunk.getContent(), metadata);
    }
}
