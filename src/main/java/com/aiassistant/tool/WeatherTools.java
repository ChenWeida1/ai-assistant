package com.aiassistant.tool;

import com.aiassistant.service.WeatherClient;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WeatherTools {

    private final WeatherClient weatherClient;

    @Tool("查询指定城市的当前天气，包括天气现象、气温、湿度和风速")
    public String queryWeather(@P("城市名称，例如 北京、上海、深圳") String city) {
        ToolTrace.record("查询天气");
        if (city == null || city.isBlank()) {
            return "请提供城市名称。";
        }
        return weatherClient.query(city);
    }
}
