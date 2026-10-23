package com.aiassistant.tool;

import com.aiassistant.dto.OrderVO;
import com.aiassistant.service.OrderQueryService;
import com.aiassistant.support.OrderStatus;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderTools {

    private final OrderQueryService orderQueryService;

    @Tool("根据订单号查询订单详情，包括状态、收货人、金额和商品明细")
    public String queryOrderByNo(@P("订单号，例如 ORD20260925001") String orderNo) {
        ToolTrace.record("查询订单");
        if (orderNo == null || orderNo.isBlank()) {
            return "请提供订单号。";
        }
        OrderVO order = orderQueryService.findByOrderNo(orderNo);
        if (order == null) {
            return "没有找到订单号 " + orderNo.trim() + "。";
        }
        return orderQueryService.formatOne(order);
    }

    @Tool("按客户姓名或手机号查询该客户的订单列表")
    public String queryOrdersByCustomer(@P("客户姓名或手机号，例如 张三 或 13800001111") String customer) {
        ToolTrace.record("查询客户订单");
        if (customer == null || customer.isBlank()) {
            return "请提供客户姓名或手机号。";
        }
        List<OrderVO> orders = orderQueryService.findByCustomer(customer);
        if (orders.isEmpty()) {
            return "没有找到客户「" + customer.trim() + "」的订单。";
        }
        return orderQueryService.formatList(orders);
    }

    @Tool("按订单状态查询订单。状态只能是：待付款、已付款、已发货、已完成、已取消")
    public String queryOrdersByStatus(@P("订单状态中文，例如 已发货") String status) {
        ToolTrace.record("按状态查订单");
        String code = OrderStatus.codeOf(status);
        if (code == null) {
            return "无法识别的状态。请使用：待付款、已付款、已发货、已完成、已取消。";
        }
        return orderQueryService.formatList(orderQueryService.findByStatus(code));
    }
}
