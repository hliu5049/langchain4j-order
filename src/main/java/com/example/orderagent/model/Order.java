package com.example.orderagent.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Data
public class Order {
    private String orderId;
    private String customerName;
    private String productName;
    private int quantity;
    private double unitPrice;
    private double totalAmount;
    private String status; // CREATED, UPDATED, CANCELLED
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Order() {
        this.orderId = UUID.randomUUID().toString().substring(0, 8);
        this.createdAt = LocalDateTime.now();
        this.status = "CREATED";
    }

    // 构造函数
    public Order(String customerName, String productName, int quantity, double unitPrice) {
        this();
        this.customerName = customerName;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.totalAmount = quantity * unitPrice;
    }
    @Override
    public String toString() {
        return "📋 订单详情\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "🆔 订单号: " + orderId + "\n" +
                "👤 客户名称: " + customerName + "\n" +
                "📦 商品名称: " + productName + "\n" +
                "🔢 数量: " + quantity + "\n" +
                "💰 单价: ¥" + String.format("%.2f", unitPrice) + "\n" +
                "💵 总金额: ¥" + String.format("%.2f", totalAmount) + "\n" +
                "📊 订单状态: " + status + "\n" +
                "⏰ 创建时间: " + createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n" +
                (updatedAt != null ? "🔄 更新时间: " + updatedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : "") +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
    }
}