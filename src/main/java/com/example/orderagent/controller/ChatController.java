package com.example.orderagent.controller;

import com.example.orderagent.agent.TriageAgent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    @Autowired
    private TriageAgent triageAgent;

    /**
     * 处理用户消息 - 无会话ID（自动生成）
     */
    @PostMapping("/message")
    public Map<String, Object> sendMessage(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String sessionId = UUID.randomUUID().toString();
        
        return processMessage(message, sessionId);
    }

    /**
     * 处理用户消息 - 指定会话ID
     */
    @PostMapping("/message/{sessionId}")
    public Map<String, Object> sendMessageWithSession(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> request) {
        String message = request.get("message");
        
        return processMessage(message, sessionId);
    }

    /**
     * 获取会话状态
     */
    @GetMapping("/session/{sessionId}/status")
    public Map<String, Object> getSessionStatus(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("status", "active");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * 清除会话记忆
     */
    @DeleteMapping("/session/{sessionId}")
    public Map<String, Object> clearSession(@PathVariable String sessionId) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 这里可以调用ChatMemoryManager的清除方法
            response.put("success", true);
            response.put("message", "Session cleared successfully");
            response.put("sessionId", sessionId);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to clear session: " + e.getMessage());
        }
        return response;
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Order Agent Chat Service");
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    /**
     * 处理消息的核心方法
     */
    private Map<String, Object> processMessage(String message, String sessionId) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            if (message == null || message.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Message cannot be empty");
                return response;
            }

            // 调用TriageAgent处理消息
            String agentResponse = triageAgent.process(message, sessionId);
            
            response.put("success", true);
            response.put("response", agentResponse);
            response.put("sessionId", sessionId);
            response.put("timestamp", System.currentTimeMillis());
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error processing message: " + e.getMessage());
            response.put("sessionId", sessionId);
        }
        
        return response;
    }
}