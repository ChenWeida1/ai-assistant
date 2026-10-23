package com.aiassistant.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {
    private String orderNo;
    private String userName;
    private String phone;
    private String status;
    private String statusLabel;
    private String receiver;
    private String address;
    private BigDecimal totalAmount;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime payTime;
    private List<OrderItemVO> items;
}
