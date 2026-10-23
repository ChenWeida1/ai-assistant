package com.aiassistant.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentVO {
    private Long id;
    private String title;
    private Integer chunkCount;
    private String status;
    private LocalDateTime createdAt;
}
