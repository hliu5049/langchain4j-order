package com.example.orderagent.tool;

import com.example.orderagent.model.Order;
import com.example.orderagent.service.OrderService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class QueryOrderTool {

    private final OrderService orderService;

    public QueryOrderTool(OrderService orderService) {
        this.orderService = orderService;
    }

    @Tool("根据订单ID查询订单详情")
    public String getOrderById(String orderId) {
        Order order = orderService.getOrderById(orderId);
        if (order == null) {
            return "未找到订单号为 " + orderId + " 的订单";
        }
        return order.toString();
    }

    @Tool("根据客户名称查询该客户的所有订单")
    public String getOrdersByCustomer(String customerName) {
        List<Order> orders = orderService.getOrdersByCustomer(customerName);
        if (orders.isEmpty()) {
            return "未找到客户 " + customerName + " 的订单";
        }
        
        return "客户 " + customerName + " 的订单列表：\n" + 
                orders.stream()
                      .map(Order::toString)
                      .collect(Collectors.joining("\n\n"));
    }


    @Tool("根据时间范围查询该时间范围内所有的订单")
    public String getOrdersByProduct(String productName) {
        List<Order> orders = orderService.getOrdersByProductName(productName);
        if (orders.isEmpty()) {
            return "未找到商品名称 " + productName + " 的订单";
        }

        return "商品名称 " + productName + " 的订单列表：\n" +
                orders.stream()
                        .map(Order::toString)
                        .collect(Collectors.joining("\n\n"));
    }

    @Tool("获取所有订单列表")
    public String getAllOrders() {
        List<Order> orders = orderService.getAllOrders();
        if (orders.isEmpty()) {
            return "当前没有任何订单";
        }
        
        return "所有订单列表：\n" + 
                orders.stream()
                      .map(Order::toString)
                      .collect(Collectors.joining("\n\n"));
    }

    @Tool("获取最近的一个订单")
    public String getLatestOrder() {
        Order order = orderService.getLatestOrder();
        if (order == null) {
            return "当前没有任何订单";
        }
        return "最近的订单：\n" + order.toString();
    }
}