package com.aiassistant.dto;

import lombok.Data;

import java.util.List;

/** POST /api/chat 响应 data。 */
@Data
public class ChatResponse {

    /** 会话 ID，前端后续请求需原样带回以保持多轮上下文。 */
    private String sessionId;

    /** 助手回答正文。 */
    private String answer;

    /** 本轮实际调用的工具名称列表，如「查询订单」「查询天气」。 */
    private List<String> tools;

    /** 本轮 RAG 命中的知识库片段，供前端展示引用。 */
    private List<SourceVO> sources;
}
