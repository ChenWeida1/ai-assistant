package com.aiassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.function.Function;

/**
 * Open-Meteo 天气客户端：先地理编码城市，再查当前天气，返回中文描述给大模型。
 * <p>
 * 注意：查询参数必须通过 {@link UriBuilder} 传递原始字符串，
 * 不要先 encode 再 {@code uri(String)}，否则中文城市名会被二次编码导致查不到。
 */
@Service
@RequiredArgsConstructor
public class WeatherClient {

    private final ObjectMapper objectMapper;
    private final RestClient restClient = buildRestClient();

    /**
     * 查询指定城市当前天气。
     *
     * @return 成功时为可读中文；城市不存在或接口异常时返回提示文案（不抛异常，便于工具侧消费）
     */
    public String query(String city) {
        try {
            // 1) 地理编码：城市名 → 经纬度
            JsonNode geo = objectMapper.readTree(get(uri -> uri
                    .scheme("https")
                    .host("geocoding-api.open-meteo.com")
                    .path("/v1/search")
                    .queryParam("name", city.trim())
                    .queryParam("count", 1)
                    .queryParam("language", "zh")
                    .build()));
            JsonNode results = geo.path("results");
            if (!results.isArray() || results.isEmpty()) {
                return "没有找到城市「" + city + "」，请换一个城市名再试。";
            }
            JsonNode place = results.get(0);
            double latitude = place.path("latitude").asDouble();
            double longitude = place.path("longitude").asDouble();
            String placeName = place.path("name").asText(city);
            String admin = place.path("admin1").asText("");

            // 2) 当前天气：气温、湿度、WMO 天气码、风速
            JsonNode weather = objectMapper.readTree(get(uri -> uri
                    .scheme("https")
                    .host("api.open-meteo.com")
                    .path("/v1/forecast")
                    .queryParam("latitude", latitude)
                    .queryParam("longitude", longitude)
                    .queryParam("current", "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m")
                    .queryParam("timezone", "Asia/Shanghai")
                    .build())).path("current");
            int code = weather.path("weather_code").asInt(-1);
            return placeName + (admin.isBlank() ? "" : "（" + admin + "）")
                    + "当前天气：" + describe(code)
                    + "，气温 " + weather.path("temperature_2m").asText("-") + "°C"
                    + "，湿度 " + weather.path("relative_humidity_2m").asText("-") + "%"
                    + "，风速 " + weather.path("wind_speed_10m").asText("-") + " km/h。";
        } catch (Exception exception) {
            return "天气接口暂时不可用：" + exception.getMessage();
        }
    }

    private String get(Function<UriBuilder, URI> uriFunction) {
        return restClient.get().uri(uriFunction).retrieve().body(String.class);
    }

    /** WMO weather_code → 中文现象描述。 */
    private String describe(int code) {
        return switch (code) {
            case 0 -> "晴";
            case 1 -> "大部晴朗";
            case 2 -> "局部多云";
            case 3 -> "阴";
            case 45, 48 -> "雾";
            case 51, 53, 55, 56, 57 -> "毛毛雨";
            case 61, 63, 65, 66, 67 -> "雨";
            case 71, 73, 75, 77 -> "雪";
            case 80, 81, 82 -> "阵雨";
            case 85, 86 -> "阵雪";
            case 95, 96, 99 -> "雷暴";
            default -> "未知现象";
        };
    }

    private static RestClient buildRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(8));
        return RestClient.builder().requestFactory(factory).build();
    }
}
