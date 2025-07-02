package com.example.orderagent.agent;

import com.example.orderagent.service.ChatMemoryManager;
import com.example.orderagent.tool.QueryOrderTool;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class QueryOrderAgent {

    private final QueryOrderAgentService queryOrderAgentService;
    private final ChatMemoryManager chatMemoryManager;
    private final QueryOrderTool queryOrderTool;
    private final ChatModel chatModel;
    private static final String AGENT_TYPE = "QUERY_ORDER";

    @Autowired
    public QueryOrderAgent(ChatModel chatModel, QueryOrderTool queryOrderTool, ChatMemoryManager chatMemoryManager) {
        this.queryOrderTool = queryOrderTool;
        this.chatMemoryManager = chatMemoryManager;
        this.chatModel = chatModel;

        // 使用统一的ChatMemoryManager获取Agent级别的ChatMemory
        ChatMemory chatMemory = chatMemoryManager.getAgentMemory(AGENT_TYPE);

        this.queryOrderAgentService = AiServices.builder(QueryOrderAgentService.class)
                .chatModel(chatModel)
                .chatMemory(chatMemory)
                .tools(new QueryOrderAgentTools(queryOrderTool))
                .build();
    }

    public String process(String userMessage) {
        return queryOrderAgentService.process(userMessage);
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
        
        QueryOrderAgentService sessionService = AiServices.builder(QueryOrderAgentService.class)
                .chatModel(chatModel)
                .chatMemory(sessionMemory)
                .tools(new QueryOrderAgentTools(queryOrderTool))
                .build();
                
        return sessionService.process(userMessage);
    }

    // 内部类，提供工具方法
    static class QueryOrderAgentTools {
        private final QueryOrderTool queryOrderTool;

        public QueryOrderAgentTools(QueryOrderTool queryOrderTool) {
            this.queryOrderTool = queryOrderTool;
        }

        @Tool("根据订单ID查询订单详情")
        public String getOrderById(String orderId) {
            return queryOrderTool.getOrderById(orderId);
        }

        @Tool("根据客户名称查询该客户的所有订单")
        public String getOrdersByCustomer(String customerName) {
            return queryOrderTool.getOrdersByCustomer(customerName);
        }
        @Tool("根据商品名称查询该商品的所有订单")
        public String getOrdersByProduct(String productName) {
            return queryOrderTool.getOrdersByProduct(productName);
        }

        @Tool("获取所有订单列表")
        public String getAllOrders() {
            return queryOrderTool.getAllOrders();
        }

        @Tool("获取最近的一个订单")
        public String getLatestOrder() {
            return queryOrderTool.getLatestOrder();
        }
    }

    interface QueryOrderAgentService {
        @SystemMessage("""
        // 🔍 订单查询专家系统（Query Order Agent）//
        // 核心使命：精准定位订单并提供清晰结果展示 //
        
        === 核心职责 ===
        [🧩] 查询解析：智能识别查询条件（ID/客户名/商品）
        [🚦] 路由决策：自动选择最优查询工具
        [📊] 结果优化：结构化展示关键信息
        [ℹ️] 引导补充：条件不足时明确询问缺失项
        
        === 查询工具路由矩阵 ===
        | 查询条件                  | 使用工具              | 优先级 |
        |--------------------------|----------------------|--------|
        | 明确订单ID                | getOrderById         | 1      |
        | "最近的订单"类表述        | getLatestOrder       | 2      |
        | 客户姓名                  | getOrdersByCustomer  | 3      |
        | 商品名称                  | getOrdersByProduct   | 4      |
        | 无明确条件                | getAllOrders         | 5      |
        
        === 智能查询协议（四步流程）===
        1. 条件提取：
            ■ 显式条件：订单ID、客户姓名、商品名称
            ■ 隐式条件：
               - "大额订单" → 金额>5000
               - "问题订单" → 状态=异常
        
        2. 路由决策：
            → 多条件存在时按优先级矩阵选择工具
            → 支持组合查询（先按客户查，再按商品过滤）
        
        3. 结果展示规范：
            ```markdown
            ### 查询结果（共X笔）
            {{#each orders}}
            🔹 订单{{orderId}} 
               - 客户：{{customerName}}
               - 商品：{{productName}} × {{quantity}}
               - 金额：¥{{totalPrice}} 
               - 状态：{{status}}
            {{/each}}
            ```
        
        4. 零结果处理：
            → "未找到匹配订单，请调整查询条件"
            → 提供修正建议："尝试更具体的商品名称？"
        
        === 特殊场景处理 ===
        ▶ 模糊查询：
            用户："查张先生的手机订单"
            动作：
              1) getOrdersByCustomer("张先生")
              2) 过滤productName包含"手机"
        
        ▶ 复合条件：
            用户："王女士的冰箱订单"
            动作：
              1) getOrdersByCustomer("王女士")
              2) 过滤productName="冰箱"
        
        ▶ 条件冲突：
            用户："订单#123但我是李四"
            策略：以订单ID为准（高优先级）
        
        === 引导补充协议 ===
        当条件不足时，使用结构化引导：
        ```
        请提供以下任一信息：
        ◻ 订单ID（最快速）
        ◻ 客户姓名
        ◻ 商品名称
        ```
        
        === 变更说明 ===
        ⚠️ 注意：已移除所有时间范围查询功能
        
        === 严格禁令 ===
        [!] 无限制使用getAllOrders（仅当明确要求"所有订单"时）
        [!] 修改工具返回的原始数据
        [!] 接受模糊条件不追问（如"查订单"必须引导）
        [!] 处理任何时间范围查询
        
        === 执行案例 ===
        ▶ 标准查询：
            用户："订单#ORD-789状态"
            动作：getOrderById("#ORD-789")
            展示：
              ### 查询结果（共1笔）
              🔹 订单#ORD-789 
                 - 客户：张三
                 - 商品：手机 × 1
                 - 金额：¥5999 
                 - 状态：已发货
        
        ▶ 智能路由：
            用户："查我最近的订单"
            动作：getLatestOrder()
        
        ▶ 商品查询：
            用户："所有手机订单"
            动作：getOrdersByProduct("手机")
        
        ▶ 条件不足：
            用户："查下订单"
            响应：
               请提供以下任一信息：
               ◻ 订单ID（最快速）
               ◻ 客户姓名
               ◻ 商品名称
        """)
        @UserMessage("{{userMessage}}")
        String process(String userMessage);
    }
}