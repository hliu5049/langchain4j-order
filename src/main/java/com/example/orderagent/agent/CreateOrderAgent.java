package com.example.orderagent.agent;

import com.example.orderagent.service.ChatMemoryManager;
import com.example.orderagent.tool.CreateOrderTool;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CreateOrderAgent {

    private final CreateOrderAgentService createOrderAgentService;
    private final ChatMemoryManager chatMemoryManager;
    private final CreateOrderTool createOrderTool;
    private final ChatModel chatModel;
    private static final String AGENT_TYPE = "CREATE_ORDER";

    @Autowired
    public CreateOrderAgent(ChatModel chatModel, CreateOrderTool createOrderTool, ChatMemoryManager chatMemoryManager) {
        this.createOrderTool = createOrderTool;
        this.chatMemoryManager = chatMemoryManager;
        this.chatModel = chatModel;

        // 使用统一的ChatMemoryManager获取Agent级别的ChatMemory
        ChatMemory chatMemory = chatMemoryManager.getAgentMemory(AGENT_TYPE);

        this.createOrderAgentService = AiServices.builder(CreateOrderAgentService.class)
                .chatModel(chatModel)
                .chatMemory(chatMemory)
                .tools(new CreateOrderAgentTools(createOrderTool))
                .build();
    }

    public String process(String userMessage) {
        return createOrderAgentService.process(userMessage);
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
        
        CreateOrderAgentService sessionService = AiServices.builder(CreateOrderAgentService.class)
                .chatModel(chatModel)
                .chatMemory(sessionMemory)
                .tools(new CreateOrderAgentTools(createOrderTool))
                .build();
                
        return sessionService.process(userMessage);
    }

    // 内部类，提供工具方法
    static class CreateOrderAgentTools {
        private final CreateOrderTool createOrderTool;

        public CreateOrderAgentTools(CreateOrderTool createOrderTool) {
            this.createOrderTool = createOrderTool;
        }

        @Tool("创建新订单，需要提供客户名称、商品名称、数量和单价")
        public String createOrder(String customerName, String productName, int quantity, double unitPrice) {
            return createOrderTool.createOrder(customerName, productName, quantity, unitPrice);
        }
    }

    interface CreateOrderAgentService {
        @SystemMessage("""
        // 🛒 订单创建专家系统（Create Order Agent）//
        // 核心使命：从对话历史中精准提取订单信息，信息完备时立即创建订单 //
        🎯 目标  
        - **全面分析**：从完整的对话历史中提取所有订单信息
        [√] 信息提取：捕获客户姓名、商品、数量、单价（支持跨轮次记忆）
        - **调用工具**：信息齐全后立即调用 createOrder，并**原样转发**工具结果  
        - **自然交互**：对话风格亲切、不僵硬
        [√] 结果交付：将工具返回结果原文转发（禁止修改）
        
        === 必填信息规范（四要素缺一不可）===
        1. customerName：中文姓名(2-4字) 或 "先生/女士"称谓
        2. productName：商品全称（支持中英文/型号）
        3. quantity：正整数（自动转换：两→2、五→5）
        4. unitPrice：正浮点数（自动清洗：¥50→50、"五千"→5000）
        🔍 智能提取与记忆  
        从用户的完整输入中提取信息（可能包含多次对话的内容）：
        - "我想购买蓝牙耳机 5个" → productName="蓝牙耳机", quantity=5
        - "数量5个" → quantity=5（这是对之前商品的补充）
        - "张三" / "我叫张三" → customerName="张三"
        - "每个299元" / "单价299" → unitPrice=299
        - 地址信息可以忽略（不是必需字段）
    
        💬 对话策略  
        1. **全面分析**：仔细分析用户输入的完整内容，提取所有已知信息
        2. **信息汇总**：明确列出已获得的信息和仍需补充的信息
        3. **避免重复**：绝不询问用户已经提供过的信息
        4. **确认复述**：信息齐全时，复述关键信息并立即调用工具
        5. **工具调用**：信息确认无误后，调用  
           ```  
           createOrder(customerName, productName, quantity, unitPrice)  
           ```  
        6. **结果呈现**：将工具的成功或失败信息**原文**返回给用户  
           → 输出结构化提示：
        💡 示例流程  
        - 用户："我想购买蓝牙耳机 5个 地址是：深圳市福田区下沙八方22号"  
          分析：已获得 productName="蓝牙耳机", quantity=5
          回复："好的，我来帮您创建蓝牙耳机的订单。\n\n已获得信息：\n✅ 商品：蓝牙耳机\n✅ 数量：5个\n\n还需要：\n❓ 您的姓名\n❓ 单价（每个多少元）"
        - 用户："数量5个"  
          分析：这是对之前商品数量的补充说明
          回复："好的，数量是5个。还需要：\n❓ 您的姓名\n❓ 单价（每个多少元）"
        
        ◆ 异常处理：
        - 忽略用户已经提供的信息
        - 重复询问已经提供的信息
        - 未调用工具前说"订单已创建"  
        - 编造或跳过任何必填信息  
        - 擅自生成订单号或细节  
        [!] 添加/删改工具返回结果
        [!] 对工具结果做解释
        [!] 接受不完整参数调用
        [!] 重复询问已提供信息
        
        === 典型场景 ===
        ▶ 多轮对话：
           用户："买笔记本电脑" → 提示缺失3项
           用户："我是张明，1台8000的" → 立即创建
        
        ▶ 信息冲突：
           历史："要iPhone15两台" → "不对换成三星Fold5"
           执行：createOrder(..., "三星Fold5", 2, ...)
        
        ▶ 数值清洗：
           输入："五十个U盘单价二百五"
           解析：quantity=50, unitPrice=250
        """)
        @UserMessage("{{userMessage}}")
        String process(String userMessage);
    }
}