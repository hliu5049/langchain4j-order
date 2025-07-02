package com.example.orderagent;

import com.example.orderagent.agent.CreateOrderAgent;
import com.example.orderagent.agent.TriageAgent;
import com.example.orderagent.service.ChatMemoryManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 测试上下文记忆和信息传递功能
 * 验证修复后的系统能否正确处理用户反馈的问题场景
 */
@SpringBootTest
public class ContextMemoryTest {

    @Autowired
    private TriageAgent triageAgent;
    
    @Autowired
    private CreateOrderAgent createOrderAgent;
    
    @Autowired
    private ChatMemoryManager chatMemoryManager;

    @Test
    public void testUserFeedbackScenario() {
        String sessionId = "test-session-" + System.currentTimeMillis();
        
        System.out.println("=== 测试用户反馈的问题场景 ===");
        
        // 第一次对话：用户提供商品名称、数量和地址
        String message1 = "我想购买蓝牙耳机 5个 地址是：深圳市福田区下沙八方22号";
        System.out.println("\n用户: " + message1);
        
        String response1 = triageAgent.process(message1, sessionId);
        System.out.println("助手: " + response1);
        
        // 验证：应该识别为创建订单意图，并提取到商品名称和数量
        assert response1.contains("蓝牙耳机") : "应该识别到商品名称：蓝牙耳机";
        assert response1.contains("5") || response1.contains("5个") : "应该识别到数量：5个";
        assert !response1.contains("请提供商品名称") : "不应该再询问商品名称";
        
        // 第二次对话：用户补充数量信息（实际上已经提供过了）
        String message2 = "数量5个";
        System.out.println("\n用户: " + message2);
        
        String response2 = triageAgent.process(message2, sessionId);
        System.out.println("助手: " + response2);
        
        // 验证：应该记住之前的信息，不重复询问商品名称
        assert !response2.contains("请提供商品名称") : "不应该重复询问商品名称";
        assert !response2.contains("请提供更详细的信息") : "不应该要求提供更详细信息";
        
        System.out.println("\n=== 测试完成 ===");
    }
    
    @Test
    public void testContextTransfer() {
        String sessionId = "context-test-" + System.currentTimeMillis();
        
        System.out.println("=== 测试上下文传递功能 ===");
        
        // 模拟完整的对话历史传递
        String fullContext = "我想购买蓝牙耳机 5个 地址是：深圳市福田区下沙八方22号 数量5个";
        System.out.println("\n完整上下文: " + fullContext);
        
        String response = createOrderAgent.process(fullContext, sessionId);
        System.out.println("CreateOrderAgent响应: " + response);
        
        // 验证：应该从完整上下文中提取到商品名称和数量
        assert response.contains("蓝牙耳机") : "应该从上下文中提取到商品名称";
        assert response.contains("5") : "应该从上下文中提取到数量";
        assert !response2.contains("请提供商品名称") : "不应该询问已提供的商品名称";
        
        System.out.println("\n=== 上下文传递测试完成 ===");
    }
    
    @Test
    public void testMemoryPersistence() {
        String sessionId = "memory-test-" + System.currentTimeMillis();
        
        System.out.println("=== 测试记忆持久化 ===");
        
        // 第一步：提供部分信息
        String step1 = "我想买蓝牙耳机";
        System.out.println("\n步骤1 - 用户: " + step1);
        String response1 = triageAgent.process(step1, sessionId);
        System.out.println("助手: " + response1);
        
        // 第二步：补充数量
        String step2 = "要5个";
        System.out.println("\n步骤2 - 用户: " + step2);
        String response2 = triageAgent.process(step2, sessionId);
        System.out.println("助手: " + response2);
        
        // 验证：第二次回复应该记住第一次的商品信息
        assert response2.contains("蓝牙耳机") || response2.contains("商品") : "应该记住之前提到的商品";
        assert !response2.contains("请提供商品名称") : "不应该重复询问商品名称";
        
        System.out.println("\n=== 记忆持久化测试完成 ===");
    }
}