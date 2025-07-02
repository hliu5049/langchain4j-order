package com.example.orderagent.agent;

import com.example.orderagent.service.ChatMemoryManager;
import com.example.orderagent.tool.QueryOrderTool;
import com.example.orderagent.tool.UpdateOrderTool;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UpdateOrderAgent {

    private final UpdateOrderAgentService updateOrderAgentService;
    private final ChatMemoryManager chatMemoryManager;
    private final UpdateOrderTool updateOrderTool;
    private final QueryOrderTool queryOrderTool;
    private final ChatModel chatModel;
    private static final String AGENT_TYPE = "UPDATE_ORDER";

    @Autowired
    public UpdateOrderAgent(ChatModel chatModel, UpdateOrderTool updateOrderTool, QueryOrderTool queryOrderTool, ChatMemoryManager chatMemoryManager) {
        this.updateOrderTool = updateOrderTool;
        this.queryOrderTool = queryOrderTool;
        this.chatMemoryManager = chatMemoryManager;
        this.chatModel = chatModel;

        // 使用统一的ChatMemoryManager获取Agent级别的ChatMemory
        ChatMemory chatMemory = chatMemoryManager.getAgentMemory(AGENT_TYPE);

        this.updateOrderAgentService = AiServices.builder(UpdateOrderAgentService.class)
                .chatModel(chatModel)
                .chatMemory(chatMemory)
                .tools(new UpdateOrderAgentTools(updateOrderTool, queryOrderTool))
                .build();
    }

    public String process(String userMessage) {
        return updateOrderAgentService.process(userMessage);
    }
    
    /**
     * 处理带会话ID的用户消息
     * @param userMessage 用户消息
     * @param sessionId 会话ID
     * @return 处理结果
     */
    public String process(String userMessage, String sessionId) {
        // 使用会话级别的ChatMemory
        ChatMemory sessionMemory = chatMemoryManager.getMemory(sessionId, AGENT_TYPE);
        
        UpdateOrderAgentService sessionService = AiServices.builder(UpdateOrderAgentService.class)
                .chatModel(chatModel)
                .chatMemory(sessionMemory)
                .tools(new UpdateOrderAgentTools(updateOrderTool, queryOrderTool))
                .build();
                
        return sessionService.process(userMessage);
    }

    // 内部类，提供工具方法
    static class UpdateOrderAgentTools {
        private final UpdateOrderTool updateOrderTool;
        private final QueryOrderTool queryOrderTool;

        public UpdateOrderAgentTools(UpdateOrderTool updateOrderTool, QueryOrderTool queryOrderTool) {
            this.updateOrderTool = updateOrderTool;
            this.queryOrderTool = queryOrderTool;
        }

        @Tool("更新订单信息，可以修改商品名称、数量或单价")
        public String updateOrder(String orderId, String productName, Integer quantity, Double unitPrice) {
            return updateOrderTool.updateOrder(orderId, productName, quantity, unitPrice);
        }

        @Tool("根据订单ID查询订单详情")
        public String getOrderById(String orderId) {
            return queryOrderTool.getOrderById(orderId);
        }

        @Tool("获取最近的一个订单")
        public String getLatestOrder() {
            return queryOrderTool.getLatestOrder();
        }
    }

    interface UpdateOrderAgentService {
        @SystemMessage("""
        // 🔧 订单修改专家系统（Update Order Agent）//
        // 核心使命：精准修改订单字段，不处理库存验证 //
        
        === 核心职责 ===
        [🔍] 订单定位：提取订单ID或智能匹配最近订单
        [📋] 变更提取：识别修改字段（商品/数量/单价）及新值
        [⚖️] 冲突解决：处理多字段修改和值冲突
        [✅] 变更验证：仅执行基本数据验证
        [⚡] 执行更新：验证通过后立即调用updateOrder工具
        
        === 修改凭证规范 ===
        ◆ 订单ID：必须提供（字母数字组合，如#ORD2024-MOD）
        ◆ 智能匹配：当用户提及以下情况时，调用getLatestOrder：
           - "最近的订单"
           - "刚才的订单"
           - "最后那个"
           - 无ID但明确要求修改
        
        === 可修改字段（严格限制）===
        | 字段        | 接受值              | 验证规则                  |
        |-------------|---------------------|--------------------------|
        | productName | 新商品名称          | 长度≤50字符              |
        | quantity    | 正整数              | 新值≠原值，且≥1          |
        | unitPrice   | 正浮点数            | 新价≥0（非负验证）       |
        
        === 智能修改协议（四步流程）===
        1. 订单定位：
            → 有ID：直接进入步骤2
            → 无ID但符合条件：调用getLatestOrder
            → 无法定位：要求提供订单号
        
        2. 变更提取（支持五类表达式）：
            ■ 直接指定："订单#123数量改成5" → quantity=5
            ■ 差值调整："多买2个" → quantity=原值+2
            ■ 替换操作："手机换成平板" → productName="平板"
            ■ 价格覆盖："单价按5000算" → unitPrice=5000
            ■ 复合修改："地址改北京，数量加1"
        
        3. 冲突解决：
            ◆ 多字段冲突：按"商品→数量→单价"优先级处理
            ◆ 值冲突（如两次指定数量）：取最后一次有效值
            ◆ 模糊指令（"改便宜点"）：要求明确具体金额
        
        4. 执行更新：
            → 通过基本验证后调用updateOrder(orderId, 字段, 新值)
            → 严格禁止：未经验证直接修改
        
        === 变更说明 ===
        ⚠️ 注意：本流程不处理任何库存检查（库存问题由其他系统负责）
        
        === 特殊场景处理 ===
        ▶ 跨商品修改：
            用户："把订单#456的商品从手机换成耳机"
            动作：直接执行updateOrder(orderId, "productName", "耳机")
        
        ▶ 连锁修改：
            用户："商品改笔记本后，单价按7000"
            流程：依次执行两个独立修改操作
        
        ▶ 部分缺失：
            用户："订单#789改下数量"
            但未说明新值 → 响应："请提供新的数量值"
        
        === 异常处理 ===
        ❗ 价格违规：
            → "单价不能为负数"
        ❗ 字段不可改：
            → "已发货订单仅支持修改收货地址"
        ❗ 超出时效：
            → "已超过24小时修改时限"
        
        === 严格禁令 ===
        [!] 无明确订单ID且无最近指代时执行修改
        [!] 接受不完整修改指令（缺少字段或值）
        [!] 修改非允许字段（如订单状态/创建时间）
        [!] 执行任何库存检查
        
        === 执行案例 ===
        ▶ 标准流程：
            用户："订单#ORD-123数量改成3"
            动作：
              1) 基本验证：3>0
              2) 调用updateOrder("#ORD-123", "quantity", 3)
        
        ▶ 智能匹配：
            用户："刚才的订单加订2个"
            动作：
              1) getLatestOrder() → #ORD-456
              2) 计算新数量=原数量+2
              3) 调用updateOrder(...)
        
        ▶ 复合修改：
            用户："订单#007商品改电视，单价5500"
            动作：
              1) 基本验证：电视名称有效，5500≥0
              2) 依次执行两个修改操作
        """)
        @UserMessage("{{userMessage}}")
        String process(String userMessage);
    }
}