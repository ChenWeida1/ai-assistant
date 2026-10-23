package com.aiassistant.service;

import com.aiassistant.dto.OrderItemVO;
import com.aiassistant.dto.OrderVO;
import com.aiassistant.entity.OrderItem;
import com.aiassistant.entity.ShopOrder;
import com.aiassistant.mapper.OrderItemMapper;
import com.aiassistant.mapper.ShopOrderMapper;
import com.aiassistant.support.OrderStatus;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 订单查询与自然语言格式化。
 * <p>
 * HTTP 接口与 LangChain4j 订单工具共用本服务，保证侧边栏与对话看到的数据一致。
 */
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final ShopOrderMapper orderMapper;
    private final OrderItemMapper itemMapper;

    /** 全部订单，按下单时间倒序。 */
    public List<OrderVO> listAll() {
        List<ShopOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<ShopOrder>()
                .orderByDesc(ShopOrder::getCreatedAt));
        return orders.stream().map(this::toVo).toList();
    }

    /** 精确匹配订单号。 */
    public OrderVO findByOrderNo(String orderNo) {
        ShopOrder order = orderMapper.selectOne(new LambdaQueryWrapper<ShopOrder>()
                .eq(ShopOrder::getOrderNo, orderNo.trim()));
        return order == null ? null : toVo(order);
    }

    /**
     * 按客户姓名模糊或手机号精确查询，最多 10 条。
     * 供工具「按客户查订单」使用。
     */
    public List<OrderVO> findByCustomer(String keyword) {
        String value = keyword.trim();
        List<ShopOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<ShopOrder>()
                .and(wrapper -> wrapper.like(ShopOrder::getUserName, value).or().eq(ShopOrder::getPhone, value))
                .orderByDesc(ShopOrder::getCreatedAt)
                .last("limit 10"));
        return orders.stream().map(this::toVo).toList();
    }

    /** 按状态码（如 SHIPPED）查询，最多 10 条。 */
    public List<OrderVO> findByStatus(String statusCode) {
        List<ShopOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<ShopOrder>()
                .eq(ShopOrder::getStatus, statusCode)
                .orderByDesc(ShopOrder::getCreatedAt)
                .last("limit 10"));
        return orders.stream().map(this::toVo).toList();
    }

    /** 将单笔订单格式化为模型可读的中文段落。 */
    public String formatOne(OrderVO order) {
        StringBuilder text = new StringBuilder();
        appendOrder(text, order);
        return text.toString();
    }

    /** 将多笔订单格式化为带序号说明的中文文本，供大模型直接引用。 */
    public String formatList(List<OrderVO> orders) {
        if (orders.isEmpty()) {
            return "没有查到符合条件的订单。";
        }
        StringBuilder text = new StringBuilder();
        text.append("共 ").append(orders.size()).append(" 笔订单：\n");
        for (OrderVO order : orders) {
            appendOrder(text, order);
            text.append("\n");
        }
        return text.toString().trim();
    }

    /** 拼接订单头信息与商品明细。 */
    private void appendOrder(StringBuilder text, OrderVO order) {
        text.append("订单号 ").append(order.getOrderNo())
                .append("，客户 ").append(order.getUserName())
                .append("，手机 ").append(order.getPhone())
                .append("，状态 ").append(order.getStatusLabel())
                .append("，金额 ").append(order.getTotalAmount()).append(" 元")
                .append("，收货人 ").append(order.getReceiver())
                .append("，地址 ").append(order.getAddress())
                .append("，下单时间 ").append(order.getCreatedAt());
        if (order.getRemark() != null && !order.getRemark().isBlank()) {
            text.append("，备注 ").append(order.getRemark());
        }
        text.append("。商品：");
        if (order.getItems() == null || order.getItems().isEmpty()) {
            text.append("无");
            return;
        }
        for (int i = 0; i < order.getItems().size(); i++) {
            OrderItemVO item = order.getItems().get(i);
            if (i > 0) {
                text.append("；");
            }
            text.append(item.getProductName())
                    .append(" x").append(item.getQuantity())
                    .append("（").append(item.getAmount()).append(" 元）");
        }
    }

    /** 实体转 VO，并加载订单行、填充中文状态标签。 */
    private OrderVO toVo(ShopOrder order) {
        OrderVO vo = new OrderVO();
        vo.setOrderNo(order.getOrderNo());
        vo.setUserName(order.getUserName());
        vo.setPhone(order.getPhone());
        vo.setStatus(order.getStatus());
        vo.setStatusLabel(OrderStatus.label(order.getStatus()));
        vo.setReceiver(order.getReceiver());
        vo.setAddress(order.getAddress());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setRemark(order.getRemark());
        vo.setCreatedAt(order.getCreatedAt());
        vo.setPayTime(order.getPayTime());
        List<OrderItem> items = itemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                .eq(OrderItem::getOrderId, order.getId()));
        vo.setItems(items.stream().map(item -> {
            OrderItemVO itemVo = new OrderItemVO();
            itemVo.setProductName(item.getProductName());
            itemVo.setQuantity(item.getQuantity());
            itemVo.setPrice(item.getPrice());
            itemVo.setAmount(item.getAmount());
            return itemVo;
        }).toList());
        return vo;
    }
}
