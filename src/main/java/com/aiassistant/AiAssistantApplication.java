package com.aiassistant;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 购物 AI 助手启动类。
 * <p>
 * 扫描 MyBatis Mapper，启动后自动加载知识库种子（见 KnowledgeSeedRunner）。
 */
@MapperScan("com.aiassistant.mapper")
@SpringBootApplication
public class AiAssistantApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAssistantApplication.class, args);
    }
}
