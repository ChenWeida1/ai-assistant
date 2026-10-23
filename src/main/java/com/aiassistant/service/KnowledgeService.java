package com.aiassistant.service;

import com.aiassistant.common.BusinessException;
import com.aiassistant.common.ResultCode;
import com.aiassistant.config.AiClient;
import com.aiassistant.config.AssistantProperties;
import com.aiassistant.dto.DocumentVO;
import com.aiassistant.entity.KbChunk;
import com.aiassistant.entity.KbDocument;
import com.aiassistant.mapper.KbChunkMapper;
import com.aiassistant.mapper.KbDocumentMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库业务：文档列表、启动种子入库、上传切分向量化、删除。
 * <p>
 * 向量同时写入内存索引（检索）与 MySQL {@code kb_chunk.embedding}（重启恢复）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeService {

    private final KbDocumentMapper documentMapper;
    private final KbChunkMapper chunkMapper;
    private final VectorStoreService vectorStoreService;
    private final AiClient aiClient;
    private final AssistantProperties properties;

    /** 文档摘要列表，按 id 倒序。 */
    public List<DocumentVO> list() {
        return documentMapper.selectList(new LambdaQueryWrapper<KbDocument>()
                        .orderByDesc(KbDocument::getId))
                .stream()
                .map(this::toVo)
                .toList();
    }

    /**
     * 库为空且已配置 API Key 时，将 classpath:knowledge/*.md 写入知识库。
     * 已有文档则跳过，避免重复种子。
     */
    public void seedIfEmpty() {
        if (!aiClient.configured()) {
            log.info("未配置 AI_API_KEY，跳过知识库初始化");
            return;
        }
        if (documentMapper.selectCount(null) > 0) {
            return;
        }
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:knowledge/*.md");
            for (Resource resource : resources) {
                String filename = resource.getFilename() == null ? "knowledge.md" : resource.getFilename();
                String title = filename.replaceFirst("\\.md$", "");
                String text = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
                ingest(title, text);
            }
            log.info("已写入内置知识库 {} 篇", resources.length);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("内置知识库初始化失败: {}", exception.getMessage());
        }
    }

    /**
     * 入库一篇文档：切分 → 嵌入 → 写入内存向量库 → 落库 document/chunk。
     * 同一事务内完成 MySQL 写入；向量库为进程内结构，失败时依赖事务回滚避免脏 document。
     */
    @Transactional
    public DocumentVO ingest(String title, String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessException("文档内容为空");
        }
        AssistantProperties.Rag rag = properties.getRag();
        Metadata metadata = new Metadata();
        metadata.put("title", title);
        // 递归切分，块大小与重叠来自配置
        List<TextSegment> segments = DocumentSplitters.recursive(rag.getChunkSize(), rag.getChunkOverlap())
                .split(Document.from(text, metadata));
        if (segments.isEmpty()) {
            throw new BusinessException("文档切分后没有可用内容");
        }

        // 先插文档拿自增 id，再给每个分片挂上 documentId 元数据
        List<TextSegment> withMeta = new ArrayList<>(segments.size());
        KbDocument document = new KbDocument();
        document.setTitle(title);
        document.setChunkCount(segments.size());
        document.setStatus("READY");
        document.setCreatedAt(LocalDateTime.now());
        documentMapper.insert(document);

        for (int i = 0; i < segments.size(); i++) {
            Metadata chunkMeta = new Metadata();
            chunkMeta.put("documentId", String.valueOf(document.getId()));
            chunkMeta.put("title", title);
            chunkMeta.put("chunkIndex", String.valueOf(i));
            withMeta.add(TextSegment.from(segments.get(i).text(), chunkMeta));
        }

        // 批量向量化后写入内存索引，再把向量 JSON 持久化到 kb_chunk
        List<Embedding> embeddings = aiClient.embeddingModel().embedAll(withMeta).content();
        List<String> embeddingIds = vectorStoreService.add(embeddings, withMeta);
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < withMeta.size(); i++) {
            KbChunk chunk = new KbChunk();
            chunk.setDocumentId(document.getId());
            chunk.setChunkIndex(i);
            chunk.setContent(withMeta.get(i).text());
            chunk.setEmbeddingId(embeddingIds.get(i));
            chunk.setEmbedding(vectorStoreService.toJson(embeddings.get(i)));
            chunk.setCreatedAt(now);
            chunkMapper.insert(chunk);
        }
        return toVo(document);
    }

    /** 删除文档、库内分片，以及内存向量中同 documentId 的条目。 */
    @Transactional
    public void delete(Long id) {
        KbDocument document = documentMapper.selectById(id);
        if (document == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "文档不存在");
        }
        chunkMapper.delete(new LambdaQueryWrapper<KbChunk>().eq(KbChunk::getDocumentId, id));
        documentMapper.deleteById(id);
        vectorStoreService.removeByDocumentId(id);
    }

    private DocumentVO toVo(KbDocument document) {
        DocumentVO vo = new DocumentVO();
        vo.setId(document.getId());
        vo.setTitle(document.getTitle());
        vo.setChunkCount(document.getChunkCount());
        vo.setStatus(document.getStatus());
        vo.setCreatedAt(document.getCreatedAt());
        return vo;
    }
}
