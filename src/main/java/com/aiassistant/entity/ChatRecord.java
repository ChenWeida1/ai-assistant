package com.aiassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_record")
public class ChatRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private String role;
    private String content;
    private String toolsJson;
    private String sourcesJson;
    private LocalDateTime createdAt;
}
