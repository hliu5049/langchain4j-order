package com.example.orderagent.agent;

import com.example.orderagent.service.ChatMemoryManager;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TriageAgent {

    private final TriageAgentService triageAgentService;
    private final ChatMemoryManager chatMemoryManager;
    private final CreateOrderAgent createOrderAgent;
    private final QueryOrderAgent queryOrderAgent;
    private final UpdateOrderAgent updateOrderAgent;
    private final CancelOrderAgent cancelOrderAgent;
    private final ChatModel chatModel;
    private static final String AGENT_TYPE = "TRIAGE";
    private String currentSessionId;

    @Autowired
    public TriageAgent(ChatModel chatModel,
                       CreateOrderAgent createOrderAgent,
                       QueryOrderAgent queryOrderAgent,
                       UpdateOrderAgent updateOrderAgent,
                       CancelOrderAgent cancelOrderAgent,
                       ChatMemoryManager chatMemoryManager) {
        this.createOrderAgent = createOrderAgent;
        this.queryOrderAgent = queryOrderAgent;
        this.updateOrderAgent = updateOrderAgent;
        this.cancelOrderAgent = cancelOrderAgent;
        this.chatMemoryManager = chatMemoryManager;
        this.chatModel = chatModel;

        // 使用统一的ChatMemoryManager获取Agent级别的ChatMemory
        ChatMemory chatMemory = chatMemoryManager.getAgentMemory(AGENT_TYPE);

        this.triageAgentService = AiServices.builder(TriageAgentService.class)
                .chatModel(chatModel)
                .chatMemory(chatMemory)
                .tools(new TriageAgentTools(this))
                .build();
    }

    public String process(String userMessage) {
        return triageAgentService.process(userMessage);
    }

    /**
     * 处理带会话ID的用户消息
     * @param userMessage 用户消息
     * @param sessionId 会话ID
     * @return 处理结果
     */
    public String process(String userMessage, String sessionId) {
        this.currentSessionId = sessionId;

        // 使用会话级别的ChatMemory
        ChatMemory sessionMemory = chatMemoryManager.getMemory(sessionId, AGENT_TYPE);

        TriageAgentService sessionService = AiServices.builder(TriageAgentService.class)
                .chatModel(chatModel)
                .chatMemory(sessionMemory)
                .tools(new TriageAgentTools(this))
                .build();

        return sessionService.process(userMessage);
    }

    // 内部类，提供工具方法
    static class TriageAgentTools {
        private final TriageAgent triageAgent;

        public TriageAgentTools(TriageAgent triageAgent) {
            this.triageAgent = triageAgent;
        }

        /**
         * 从ChatMessage中提取文本内容
         * @param message ChatMessage对象
         * @return 消息的文本内容
         */
        private String getMessageText(ChatMessage message) {
            if (message instanceof dev.langchain4j.data.message.SystemMessage) {
                return ((dev.langchain4j.data.message.SystemMessage) message).text();
            } else if (message instanceof UserMessage) {
                return ((UserMessage) message).singleText();
            } else if (message instanceof AiMessage) {
                return ((AiMessage) message).text();
            } else if (message instanceof ToolExecutionResultMessage) {
                return ((ToolExecutionResultMessage) message).text();
            } else {
                return message.toString();
            }
        }

        @Tool("将请求转发给创建订单Agent")
        public String handoffToCreateOrderAgent() {
            if (triageAgent.currentSessionId != null) {
                ChatMemory sessionMemory = triageAgent.chatMemoryManager.getMemory(triageAgent.currentSessionId, AGENT_TYPE);
                
                // 构建包含历史上下文的消息
                StringBuilder contextMessage = new StringBuilder();
                
                // 收集用户的所有相关消息
                for (ChatMessage message : sessionMemory.messages()) {
                    if (message instanceof UserMessage) {
                        String userText = getMessageText(message);
                        if (contextMessage.length() > 0) {
                            contextMessage.append(" ");
                        }
                        contextMessage.append(userText);
                    }
                }
                
                // 如果没有找到用户消息，使用最后一条消息
                String finalMessage = contextMessage.length() > 0 ? 
                    contextMessage.toString() : 
                    getMessageText(sessionMemory.messages().get(sessionMemory.messages().size() - 1));
                    
                return triageAgent.createOrderAgent.process(finalMessage, triageAgent.currentSessionId);
            } else {
                ChatMemory agentMemory = triageAgent.chatMemoryManager.getAgentMemory(AGENT_TYPE);
                String userMessage = getMessageText(agentMemory.messages().get(agentMemory.messages().size() - 1));
                return triageAgent.createOrderAgent.process(userMessage);
            }
        }

        @Tool("将请求转发给查询订单Agent")
        public String handoffToQueryOrderAgent() {
            if (triageAgent.currentSessionId != null) {
                ChatMemory sessionMemory = triageAgent.chatMemoryManager.getMemory(triageAgent.currentSessionId, AGENT_TYPE);

                // 构建包含历史上下文的消息
                StringBuilder contextMessage = new StringBuilder();

                // 收集用户的所有相关消息
                for (ChatMessage message : sessionMemory.messages()) {
                    if (message instanceof UserMessage) {
                        String userText = getMessageText(message);
                        if (contextMessage.length() > 0) {
                            contextMessage.append(" ");
                        }
                        contextMessage.append(userText);
                    }
                }

                // 如果没有找到用户消息，使用最后一条消息
                String finalMessage = contextMessage.length() > 0 ?
                        contextMessage.toString() :
                        getMessageText(sessionMemory.messages().get(sessionMemory.messages().size() - 1));

                return triageAgent.queryOrderAgent.process(finalMessage, triageAgent.currentSessionId);
            } else {
                ChatMemory agentMemory = triageAgent.chatMemoryManager.getAgentMemory(AGENT_TYPE);
                String userMessage = getMessageText(agentMemory.messages().get(agentMemory.messages().size() - 1));
                return triageAgent.queryOrderAgent.process(userMessage);
            }
        }

        @Tool("将请求转发给更新订单Agent")
        public String handoffToUpdateOrderAgent() {
            if (triageAgent.currentSessionId != null) {
                ChatMemory sessionMemory = triageAgent.chatMemoryManager.getMemory(triageAgent.currentSessionId, AGENT_TYPE);

                // 构建包含历史上下文的消息
                StringBuilder contextMessage = new StringBuilder();

                // 收集用户的所有相关消息
                for (ChatMessage message : sessionMemory.messages()) {
                    if (message instanceof UserMessage) {
                        String userText = getMessageText(message);
                        if (contextMessage.length() > 0) {
                            contextMessage.append(" ");
                        }
                        contextMessage.append(userText);
                    }
                }

                // 如果没有找到用户消息，使用最后一条消息
                String finalMessage = contextMessage.length() > 0 ?
                        contextMessage.toString() :
                        getMessageText(sessionMemory.messages().get(sessionMemory.messages().size() - 1));
                return triageAgent.updateOrderAgent.process(finalMessage, triageAgent.currentSessionId);
            } else {
                ChatMemory agentMemory = triageAgent.chatMemoryManager.getAgentMemory(AGENT_TYPE);
                String userMessage = getMessageText(agentMemory.messages().get(agentMemory.messages().size() - 1));
                return triageAgent.updateOrderAgent.process(userMessage);
            }
        }

        @Tool("将请求转发给取消订单Agent")
        public String handoffToCancelOrderAgent() {
            if (triageAgent.currentSessionId != null) {
                ChatMemory sessionMemory = triageAgent.chatMemoryManager.getMemory(triageAgent.currentSessionId, AGENT_TYPE);

                // 构建包含历史上下文的消息
                StringBuilder contextMessage = new StringBuilder();

                // 收集用户的所有相关消息
                for (ChatMessage message : sessionMemory.messages()) {
                    if (message instanceof UserMessage) {
                        String userText = getMessageText(message);
                        if (contextMessage.length() > 0) {
                            contextMessage.append(" ");
                        }
                        contextMessage.append(userText);
                    }
                }

                // 如果没有找到用户消息，使用最后一条消息
                String finalMessage = contextMessage.length() > 0 ?
                        contextMessage.toString() :
                        getMessageText(sessionMemory.messages().get(sessionMemory.messages().size() - 1));
                return triageAgent.cancelOrderAgent.process(finalMessage, triageAgent.currentSessionId);
            } else {
                ChatMemory agentMemory = triageAgent.chatMemoryManager.getAgentMemory(AGENT_TYPE);
                String userMessage = getMessageText(agentMemory.messages().get(agentMemory.messages().size() - 1));
                return triageAgent.cancelOrderAgent.process(userMessage);
            }
        }
    }

    interface TriageAgentService {
        @SystemMessage("""
        # 订单意图路由专家（Triage Agent）
        你是指令分发的神经中枢，**仅识别用户意图并立即调用工具**，绝不处理具体业务逻辑。决策流程：
    
        ## 核心职责（立即调用工具）
        ✅ 创建订单 → `handoffToCreateOrderAgent`  
        ✅ 查询订单 → `handoffToQueryOrderAgent`  
        ✅ 修改订单 → `handoffToUpdateOrderAgent`  
        ✅ 取消订单 → `handoffToCancelOrderAgent`  
        ❓ 不明确 → **一次性澄清**（禁用推测）
    
        ## 智能识别引擎（优先级降序）
        | 意图类型 | 触发条件（满足任一即触发） | 关键动作 |
        |----------|---------------------------|----------|
        | 创建订单 | 1. 购买意图词（买/购/要/下单/订）<br>2. 商品+数量组合（“iPhone 两台”）<br>3. 新需求描述（“需要采购XX”） | 提取商品信息立即转交 |
        | 查询订单 | 1. 含订单号（字母数字组合）<br>2. 查询类动词（查/看/状态）<br>3. “我的订单”类表述 | 提取订单号立即转交 |
        | 修改订单 | 1. 修改类动词（改/换/更新/调整）<br>2. 变更字段词（地址/数量/收货人） | 无需参数直接转交 |
        | 取消订单 | 1. 取消类动词（取消/退/不要）<br>2. 终止动作（“别发了”/“作废”） | 无需参数直接转交 |
    
        ## 冲突解决机制
        🔥 多意图冲突：按「创建→查询→修改→取消」优先级处理  
        ⚠️ 模糊场景：当存在以下情况时**必须澄清**：
        - 无明确动作关键词（如：“订单...”）
        - 多动作混合（如：“想改地址又怕取消”）
        - 关键信息缺失（如：“查订单”但无订单号）
    
        ## 澄清协议（一次性输出）
        ```markdown
        请明确您的需求：
        🔹 1. 创建新订单 → 请提供商品信息  
        🔹 2. 查询订单 → 请提供订单号  
        🔹 3. 修改订单 → 请说明修改内容  
        🔹 4. 取消订单 → 请确认订单号  
        回复序号或直接描述即可
        ```
    
        ## 执行案例
        ▶️ 明确意图场景：
        - “创建订单” → `handoffToCreateOrderAgent()`  
        - “查询订单” → `handoffToQueryOrderAgent()`  
        - “修改订单” → `handoffToUpdateOrderAgent()`  
        - “取消订单” → `handoffToCancelOrderAgent()`  
        
        ▶️ 强制澄清场景：
        - “关于订单...” → 输出澄清模板  
        - “苹果手机” → 输出澄清模板（无动作词）  
        - “改地址但先别取消” → 输出澄清模板（意图冲突）
        """)
        @dev.langchain4j.service.UserMessage("{{userMessage}}")
        String process(String userMessage);
    }
}