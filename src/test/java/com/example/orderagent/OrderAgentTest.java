package com.example.orderagent;

import com.example.orderagent.agent.TriageAgent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 订单代理测试类
 * 用于测试订单创建流程的改进效果
 */
@SpringBootTest
public class OrderAgentTest {

    @Autowired
    private TriageAgent triageAgent;

    /**
     * 测试场景1：用户说"我想创建一个新订单"
     */
    @Test
    public void testCreateOrderIntent1() {
        String userMessage = "我想创建一个新订单";
        String response = triageAgent.process(userMessage);
        System.out.println("用户输入: " + userMessage);
        System.out.println("系统回复: " + response);
        System.out.println("---");
    }

    /**
     * 测试场景2：用户说"我想购买蓝牙耳机"
     */
    @Test
    public void testCreateOrderIntent2() {
        String userMessage = "我想购买蓝牙耳机";
        String response = triageAgent.process(userMessage);
        System.out.println("用户输入: " + userMessage);
        System.out.println("系统回复: " + response);
        System.out.println("---");
    }

    /**
     * 测试场景3：用户说"5个，收货地址：深圳市福田区下沙八方22号"
     */
    @Test
    public void testCreateOrderIntent3() {
        String userMessage = "5个，收货地址：深圳市福田区下沙八方22号";
        String response = triageAgent.process(userMessage);
        System.out.println("用户输入: " + userMessage);
        System.out.println("系统回复: " + response);
        System.out.println("---");
    }

    /**
     * 测试完整的订单创建流程
     */
    @Test
    public void testCompleteOrderFlow() {
        String sessionId = "test-session-001";
        
        // 第一步：用户表达购买意图
        String message1 = "我想购买蓝牙耳机";
        String response1 = triageAgent.process(message1, sessionId);
        System.out.println("步骤1 - 用户: " + message1);
        System.out.println("步骤1 - 系统: " + response1);
        System.out.println();
        
        // 第二步：用户提供部分信息
        String message2 = "5个，收货地址：深圳市福田区下沙八方22号";
        String response2 = triageAgent.process(message2, sessionId);
        System.out.println("步骤2 - 用户: " + message2);
        System.out.println("步骤2 - 系统: " + response2);
        System.out.println();
        
        // 第三步：用户提供完整信息
        String message3 = "我叫张三，单价299元";
        String response3 = triageAgent.process(message3, sessionId);
        System.out.println("步骤3 - 用户: " + message3);
        System.out.println("步骤3 - 系统: " + response3);
        System.out.println("---");
    }
}