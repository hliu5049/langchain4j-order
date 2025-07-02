package com.example.orderagent.tool;

import com.example.orderagent.model.Order;
import com.example.orderagent.service.OrderService;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class CreateOrderTool {

    private final OrderService orderService;

    public CreateOrderTool(OrderService orderService) {
        this.orderService = orderService;
    }
    /**
     * 创建新订单工具
     * @param customerName 客户名称，不能为空
     * @param productName 商品名称，不能为空
     * @param quantity 数量，必须大于0
     * @param unitPrice 单价，必须大于0
     * @return 订单创建结果
     */
    @Tool("创建新订单，需要提供客户名称、商品名称、数量和单价")
    public String createOrder(String customerName, String productName, int quantity, double unitPrice) {
        // 参数验证
        if (customerName == null || customerName.trim().isEmpty()) {
            return "错误：客户名称不能为空，请提供客户名称。";
        }
        if (productName == null || productName.trim().isEmpty()) {
            return "错误：商品名称不能为空，请提供商品名称。";
        }
        if (quantity <= 0) {
            return "错误：数量必须大于0，请提供有效的数量。";
        }
        if (unitPrice <= 0) {
            return "错误：单价必须大于0，请提供有效的单价。";
        }

        try {
            Order order = orderService.createOrder(customerName, productName, quantity, unitPrice);
            return "✅ 订单创建成功！\n\n" + order.toString() + "\n\n请确认以上信息是否正确？";
        } catch (Exception e) {
            return "❌ 订单创建失败：" + e.getMessage() + "\n请重试或联系客服。";
        }
    }
}