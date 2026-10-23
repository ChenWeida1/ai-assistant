package com.aiassistant.controller;

import com.aiassistant.common.Result;
import com.aiassistant.dto.ChatRequest;
import com.aiassistant.dto.ChatResponse;
import com.aiassistant.service.AssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 对话接口：接收用户问题，委托助手服务完成 Tool Calling / RAG 后返回回答。
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AssistantService assistantService;

    /**
     * POST /api/chat
     * <p>
     * 请求体：{@code sessionId} 可空（空则新建会话），{@code message} 必填。
     * 返回：会话 ID、模型回答、本次调用的工具名、RAG 引用片段。
     */
    @PostMapping
    public Result<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return Result.ok(assistantService.chat(request));
    }
}
