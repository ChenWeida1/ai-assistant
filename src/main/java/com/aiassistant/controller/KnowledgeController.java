package com.aiassistant.controller;

import com.aiassistant.common.BusinessException;
import com.aiassistant.common.Result;
import com.aiassistant.dto.DocumentVO;
import com.aiassistant.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

/**
 * 知识库文档管理：列表、上传 txt/md、删除（同步清理分片与内存向量）。
 */
@RestController
@RequestMapping("/api/knowledge/documents")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    /** 按入库时间倒序列出文档摘要。 */
    @GetMapping
    public Result<List<DocumentVO>> list() {
        return Result.ok(knowledgeService.list());
    }

    /**
     * 上传知识库文件（multipart 字段名 {@code file}）。
     * 仅允许 .txt / .md，内容按 UTF-8 读取后切分、向量化并落库。
     */
    @PostMapping
    public Result<DocumentVO> upload(@RequestParam("file") MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename() == null ? "untitled.txt" : file.getOriginalFilename();
        String lower = filename.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".txt") && !lower.endsWith(".md")) {
            throw new BusinessException("只支持 .txt 或 .md 文件");
        }
        // 去掉扩展名作为文档标题
        String title = filename.replaceFirst("\\.(txt|md)$", "");
        String text = new String(file.getBytes(), StandardCharsets.UTF_8);
        return Result.ok(knowledgeService.ingest(title, text));
    }

    /** 删除文档及其分片、向量索引。 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return Result.ok(null);
    }
}
