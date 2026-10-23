package com.aiassistant.controller;

import com.aiassistant.common.Result;
import com.aiassistant.config.AiClient;
import com.aiassistant.config.AssistantProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 健康检查：供前端顶部展示 API Key 是否已配置及当前对话模型名。
 */
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final AiClient aiClient;
    private final AssistantProperties properties;

    /** GET /api/health → { aiConfigured, model } */
    @GetMapping
    public Result<Map<String, Object>> health() {
        return Result.ok(Map.of(
                "aiConfigured", aiClient.configured(),
                "model", properties.getAi().getChatModel()
        ));
    }
}
