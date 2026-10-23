package com.aiassistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("shop_order")
public class ShopOrder {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private String userName;
    private String phone;
    private String status;
    private String receiver;
    private String address;
    private BigDecimal totalAmount;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime payTime;
}
