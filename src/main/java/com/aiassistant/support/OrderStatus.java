package com.aiassistant.support;

import java.util.Map;

public final class OrderStatus {

    private static final Map<String, String> LABELS = Map.of(
            "PENDING_PAYMENT", "待付款",
            "PAID", "已付款",
            "SHIPPED", "已发货",
            "COMPLETED", "已完成",
            "CANCELLED", "已取消"
    );

    private OrderStatus() {
    }

    public static String label(String code) {
        return LABELS.getOrDefault(code, code);
    }

    public static String codeOf(String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        if (LABELS.containsKey(value)) {
            return value;
        }
        for (Map.Entry<String, String> entry : LABELS.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }
}
