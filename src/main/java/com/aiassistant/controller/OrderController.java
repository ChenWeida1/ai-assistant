package com.aiassistant.controller;

import com.aiassistant.common.BusinessException;
import com.aiassistant.common.Result;
import com.aiassistant.common.ResultCode;
import com.aiassistant.dto.OrderVO;
import com.aiassistant.service.OrderQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 订单只读 API：供前端侧边栏展示；对话侧通过 OrderTools 走同一套查询服务。
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderQueryService orderQueryService;

    /** 全部订单列表（含商品明细）。 */
    @GetMapping
    public Result<List<OrderVO>> list() {
        return Result.ok(orderQueryService.listAll());
    }

    /** 按订单号查详情；不存在返回 404。 */
    @GetMapping("/{orderNo}")
    public Result<OrderVO> detail(@PathVariable String orderNo) {
        OrderVO order = orderQueryService.findByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        }
        return Result.ok(order);
    }
}
