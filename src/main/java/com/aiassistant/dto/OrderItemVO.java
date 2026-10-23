package com.aiassistant.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemVO {
    private String productName;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal amount;
}
