package com.aiassistant.bootstrap;

import com.aiassistant.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动后执行：若知识库为空且已配置 API Key，则写入内置政策文档。
 * <p>
 * 初始化失败只打警告，不阻断应用启动。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeSeedRunner implements ApplicationRunner {

    private final KnowledgeService knowledgeService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            knowledgeService.seedIfEmpty();
        } catch (Exception exception) {
            log.warn("知识库初始化未完成: {}", exception.getMessage());
        }
    }
}
