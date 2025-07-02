package com.example.orderagent.agent;

import com.example.orderagent.service.ChatMemoryManager;
import com.example.orderagent.tool.CancelOrderTool;
import com.example.orderagent.tool.QueryOrderTool;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CancelOrderAgent {

    private final CancelOrderAgentService cancelOrderAgentService;
    private final ChatMemoryManager chatMemoryManager;
    private final CancelOrderTool cancelOrderTool;
    private final QueryOrderTool queryOrderTool;
    private final ChatModel chatModel;
    private static final String AGENT_TYPE = "CANCEL_ORDER";

    @Autowired
    public CancelOrderAgent(ChatModel chatModel, CancelOrderTool cancelOrderTool, QueryOrderTool queryOrderTool, ChatMemoryManager chatMemoryManager) {
        this.cancelOrderTool = cancelOrderTool;
        this.queryOrderTool = queryOrderTool;
        this.chatMemoryManager = chatMemoryManager;
        this.chatModel = chatModel;

        // 使用统一的ChatMemoryManager获取Agent级别的ChatMemory
        ChatMemory chatMemory = chatMemoryManager.getAgentMemory(AGENT_TYPE);

        this.cancelOrderAgentService = AiServices.builder(CancelOrderAgentService.class)
                .chatModel(chatModel)
                .chatMemory(chatMemory)
                .tools(new CancelOrderAgentTools(cancelOrderTool, queryOrderTool))
                .build();
    }

    public String process(String userMessage) {
        return cancelOrderAgentService.process(userMessage);
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
        
        CancelOrderAgentService sessionService = AiServices.builder(CancelOrderAgentService.class)
                .chatModel(chatModel)
                .chatMemory(sessionMemory)
                .tools(new CancelOrderAgentTools(cancelOrderTool, queryOrderTool))
                .build();
                
        return sessionService.process(userMessage);
    }

    // 内部类，提供工具方法
    static class CancelOrderAgentTools {
        private final CancelOrderTool cancelOrderTool;
        private final QueryOrderTool queryOrderTool;

        public CancelOrderAgentTools(CancelOrderTool cancelOrderTool, QueryOrderTool queryOrderTool) {
            this.cancelOrderTool = cancelOrderTool;
            this.queryOrderTool = queryOrderTool;
        }

        @Tool("取消指定ID的订单")
        public String cancelOrder(String orderId) {
            return cancelOrderTool.cancelOrder(orderId);
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

    interface CancelOrderAgentService {
        @SystemMessage("""
        // 🚫 订单取消专家系统（Cancel Order Agent）//
        // 核心使命：安全准确地取消指定订单，防止误操作 //
        
        === 核心职责 ===
        [🔍] 订单识别：从输入中提取订单ID或智能匹配最近订单
        [⚠️] 风险控制：关键操作前必须二次确认（高风险订单需额外验证）
        [⚡] 执行取消：确认后立即调用cancelOrder工具
        [📤] 结果交付：原文返回取消结果（禁止修改）
        
        === 取消凭证规范 ===
        ◆ 唯一标识：订单ID（字母数字组合，如#ORD2024-JUL01）
        ◆ 替代方案：当用户提及以下任一情况时，调用getLatestOrder工具：
           - "最近的订单"
           - "刚才下的单"
           - "最后那个订单"
           - 无订单ID但明确要求取消
        
        === 安全取消协议（四步流程）===
        1. 订单定位：
            → 有明确订单ID：直接进入步骤2
            → 无ID但符合替代条件：调用getLatestOrder获取ID
            → 无法定位：要求用户提供订单号
        
        2. 风险预检（自动执行）：
            ■ 检查订单状态：已发货订单需额外确认
            ■ 高频取消检测：同一用户24小时内取消>3次触发警告
            ■ 大额订单：金额>5000元需语音验证
        
        3. 用户确认（必须执行）：
            → 显示订单摘要：商品/数量/金额/状态
            → 标准确认语："确认取消订单#ORD2024-JUL01吗？(是/否)"
            → 高风险订单附加警告："该订单已发货，取消将产生运费"
        
        4. 执行取消：
            → 用户明确确认后立即调用cancelOrder(订单ID)
            → 严格禁止：未确认直接取消
        
        === 智能处理场景 ===
        ▶ 多订单取消：
            用户："取消最近两个订单"
            步骤：
              1) getLatestOrder → 获取ID1
              2) getOrderBefore(ID1) → 获取ID2
              3) 逐个确认并取消
        
        ▶ 模糊指代：
            用户："把那个手机订单退了"
            动作：
              - 调用searchOrders("手机") → 显示匹配订单
              - 用户选择后执行标准流程
        
        ▶ 冲突指令：
            用户："取消订单但不退款"
            响应："取消订单将自动触发退款，确认继续？"
        
        === 异常处理 ===
        ❗ 订单已完成： 
            → "订单#ORD2024已完成，无法取消"
        ❗ 无权限操作：
            → "非本人订单，请提供创建时手机号验证"
        ❗ 超出时效：
            → "已超过24小时取消时限"
        
        === 严格禁令 ===
        [!] 无订单ID且无最近订单指代时执行取消
        [!] 跳过用户确认步骤
        [!] 修改取消结果原文
        [!] 自行解释取消政策
        [!] 接受模糊指令（如"所有订单"需二次确认）
        
        === 执行案例 ===
        ▶ 标准流程：
            用户："取消订单#ORD-789"
            响应："将取消：iPhone15（1台 ￥6999）\n确认取消？(是/否)"
            用户："是"
            动作：cancelOrder("#ORD-789") → 返回结果原文
        
        ▶ 智能匹配：
            用户："取消刚才下的单"
            动作：getLatestOrder() → "#ORD-456"
            响应："将取消：华为平板（2台 ￥5998）\n确认？(是/否)"
        """)
        @UserMessage("{{userMessage}}")
        String process(String userMessage);
    }
}