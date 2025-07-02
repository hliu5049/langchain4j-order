package com.example.orderagent.service;

import com.example.orderagent.model.Order;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {
    // 使用内存存储订单，实际应用中应该使用数据库
    private final Map<String, Order> orderMap = new HashMap<>();

    // 创建订单
    public Order createOrder(String customerName, String productName, int quantity, double unitPrice) {
        // 业务逻辑验证
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("客户名称不能为空");
        }
        if (productName == null || productName.trim().isEmpty()) {
            throw new IllegalArgumentException("商品名称不能为空");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("数量必须大于0");
        }
        if (unitPrice <= 0) {
            throw new IllegalArgumentException("单价必须大于0");
        }

        Order order = new Order(customerName, productName, quantity, unitPrice);
        orderMap.put(order.getOrderId(), order);

        // 记录日志
        System.out.println("订单创建成功：" + order.getOrderId());

        return order;
    }

    // 查询订单
    public Order getOrderById(String orderId) {
        return orderMap.get(orderId);
    }

    // 查询客户的所有订单
    public List<Order> getOrdersByCustomer(String customerName) {
        return orderMap.values().stream()
                .filter(order -> order.getCustomerName().equalsIgnoreCase(customerName))
                .collect(Collectors.toList());
    }


    // 查询客户的所有订单
    public List<Order> getOrdersByProductName(String productName) {
        return orderMap.values().stream()
                .filter(order -> order.getProductName().equalsIgnoreCase(productName))
                .collect(Collectors.toList());
    }

    // 获取所有订单
    public List<Order> getAllOrders() {
        return new ArrayList<>(orderMap.values());
    }

    // 更新订单
    public Order updateOrder(String orderId, String productName, Integer quantity, Double unitPrice) {
        Order order = orderMap.get(orderId);
        if (order == null) {
            return null;
        }

        if (productName != null) {
            order.setProductName(productName);
        }
        if (quantity != null) {
            order.setQuantity(quantity);
        }
        if (unitPrice != null) {
            order.setUnitPrice(unitPrice);
        }

        order.setStatus("UPDATED");
        order.setUpdatedAt(LocalDateTime.now());
        return order;
    }

    // 取消订单
    public Order cancelOrder(String orderId) {
        Order order = orderMap.get(orderId);
        if (order == null) {
            return null;
        }
        order.setStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());
        return order;
    }

    // 获取最近的订单
    public Order getLatestOrder() {
        if (orderMap.isEmpty()) {
            return null;
        }
        
        return orderMap.values().stream()
                .max((o1, o2) -> {
                    LocalDateTime date1 = o1.getUpdatedAt() != null ? o1.getUpdatedAt() : o1.getCreatedAt();
                    LocalDateTime date2 = o2.getUpdatedAt() != null ? o2.getUpdatedAt() : o2.getCreatedAt();
                    return date1.compareTo(date2);
                })
                .orElse(null);
    }
}