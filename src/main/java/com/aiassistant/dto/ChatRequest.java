package com.aiassistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** POST /api/chat 请求体。 */
@Data
public class ChatRequest {

    /** 会话 ID；为空时由服务端生成，用于多轮上下文与落库。 */
    private String sessionId;

    /** 用户问题，最长 2000 字。 */
    @NotBlank(message = "请输入问题")
    @Size(max = 2000, message = "问题不能超过 2000 字")
    private String message;
}
