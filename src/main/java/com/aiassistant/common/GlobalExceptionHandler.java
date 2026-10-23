package com.aiassistant.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.exception.RateLimitException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 全局异常处理：将业务异常与框架异常统一转为 {@link Result} JSON，避免堆栈泄露给前端。
 */
@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper;

    /** 可预期的业务错误（缺 Key、资源不存在等），直接回传业务码与文案。 */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException exception) {
        return Result.fail(exception.getCode(), exception.getMessage());
    }

    /** Bean Validation 失败（如 message 为空），取首个字段错误信息。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String message = fieldError == null ? "请求参数错误" : fieldError.getDefaultMessage();
        return Result.fail(message);
    }

    /** 请求体无法解析，或上传超过 spring.servlet.multipart 限制（2MB）。 */
    @ExceptionHandler({HttpMessageNotReadableException.class, MaxUploadSizeExceededException.class})
    public Result<Void> handleBadRequest(Exception exception) {
        return Result.fail("请求无法解析，或上传文件超过 2MB");
    }

    /** 智谱等上游限流/余额类错误，尽量回传可读文案。 */
    @ExceptionHandler({RateLimitException.class, HttpException.class})
    public Result<Void> handleUpstream(RuntimeException exception) {
        log.warn("上游模型调用失败: {}", exception.getMessage());
        return Result.fail(ResultCode.ERROR.getCode(), extractUpstreamMessage(exception.getMessage()));
    }

    /** 未捕获异常：记日志，对外返回通用错误，不暴露内部细节。 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception exception) {
        log.error("未处理异常", exception);
        return Result.fail(ResultCode.ERROR.getCode(), "服务暂时不可用，请稍后重试");
    }

    private String extractUpstreamMessage(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "大模型调用失败，请稍后重试";
        }
        try {
            int start = raw.indexOf('{');
            int end = raw.lastIndexOf('}');
            if (start >= 0 && end > start) {
                JsonNode root = objectMapper.readTree(raw.substring(start, end + 1));
                JsonNode message = root.path("error").path("message");
                if (message.isTextual() && StringUtils.hasText(message.asText())) {
                    return message.asText();
                }
            }
        } catch (Exception ignored) {
            // 解析失败时回退原文截断
        }
        String trimmed = raw.trim();
        return trimmed.length() > 200 ? trimmed.substring(0, 200) + "…" : trimmed;
    }
}
