package com.example.orderagent.tool;

import com.example.orderagent.model.Order;
import com.example.orderagent.service.OrderService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class UpdateOrderTool {

    private final OrderService orderService;

    public UpdateOrderTool(OrderService orderService) {
        this.orderService = orderService;
    }

    @Tool("更新订单信息，可以修改商品名称、数量或单价")
    public String updateOrder(String orderId, String productName, Integer quantity, Double unitPrice) {
        Order order = orderService.updateOrder(orderId, productName, quantity, unitPrice);
        if (order == null) {
            return "未找到订单号为 " + orderId + " 的订单";
        }
        return "订单更新成功：\n" + order.toString();
    }
}