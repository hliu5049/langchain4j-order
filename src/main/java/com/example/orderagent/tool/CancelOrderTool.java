package com.example.orderagent.tool;

import com.example.orderagent.model.Order;
import com.example.orderagent.service.OrderService;
import org.springframework.stereotype.Component;

@Component
public class CancelOrderTool {

    private final OrderService orderService;

    public CancelOrderTool(OrderService orderService) {
        this.orderService = orderService;
    }

    public String cancelOrder(String orderId) {
        try {
            Order order = orderService.getOrderById(orderId);
            if (order == null) {
                return "未找到ID为 " + orderId + " 的订单";
            }
            
            // 使用OrderService取消订单
            order = orderService.cancelOrder(orderId);
            
            return "订单 " + orderId + " 已成功取消。\n" +
                   "订单详情：\n" +
                   "- 客户：" + order.getCustomerName() + "\n" +
                   "- 商品：" + order.getProductName() + "\n" +
                   "- 数量：" + order.getQuantity() + "\n" +
                   "- 单价：" + order.getUnitPrice() + "\n" +
                   "- 总价：" + (order.getQuantity() * order.getUnitPrice());
        } catch (Exception e) {
            return "取消订单时发生错误：" + e.getMessage();
        }
    }
}