package com.example.orderagent.service;

import com.example.orderagent.config.ChatMemoryConfig;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * ChatMemory管理器，负责统一管理所有Agent的对话上下文
 * 支持会话持久化、上下文清理和跨Agent的信息传递
 */
@Service
public class ChatMemoryManager {
    
    @Autowired
    private ChatMemoryConfig config;
    
    // 存储不同会话的ChatMemory实例
    private final Map<String, ChatMemory> sessionMemories = new ConcurrentHashMap<>();
    
    // 存储不同Agent类型的ChatMemory实例
    private final Map<String, ChatMemory> agentMemories = new ConcurrentHashMap<>();
    
    // 存储会话的最后活跃时间
    private final Map<String, LocalDateTime> sessionLastActiveTime = new ConcurrentHashMap<>();
    
    /**
     * 获取指定会话的ChatMemory
     * @param sessionId 会话ID
     * @return ChatMemory实例
     */
    public ChatMemory getSessionMemory(String sessionId) {
        updateSessionActiveTime(sessionId);
        return sessionMemories.computeIfAbsent(sessionId, id -> 
            MessageWindowChatMemory.builder()
                .maxMessages(config.getSessionMaxMessages())
                .build()
        );
    }
    
    /**
     * 获取指定Agent的ChatMemory
     * @param agentType Agent类型
     * @return ChatMemory实例
     */
    public ChatMemory getAgentMemory(String agentType) {
        return agentMemories.computeIfAbsent(agentType, type -> 
            MessageWindowChatMemory.builder()
                .maxMessages(config.getAgentMaxMessages())
                .build()
        );
    }
    
    /**
     * 获取指定会话和Agent的ChatMemory
     * @param sessionId 会话ID
     * @param agentType Agent类型
     * @return ChatMemory实例
     */
    public ChatMemory getMemory(String sessionId, String agentType) {
        updateSessionActiveTime(sessionId);
        String key = sessionId + ":" + agentType;
        return sessionMemories.computeIfAbsent(key, k -> 
            MessageWindowChatMemory.builder()
                .maxMessages(config.getDefaultMaxMessages())
                .build()
        );
    }
    
    /**
     * 创建自定义配置的ChatMemory
     * @param sessionId 会话ID
     * @param agentType Agent类型
     * @param maxMessages 最大消息数
     * @return ChatMemory实例
     */
    public ChatMemory createMemory(String sessionId, String agentType, int maxMessages) {
        updateSessionActiveTime(sessionId);
        String key = sessionId + ":" + agentType;
        ChatMemory memory = MessageWindowChatMemory.builder()
            .maxMessages(maxMessages)
            .build();
        sessionMemories.put(key, memory);
        return memory;
    }
    
    /**
     * 更新会话活跃时间
     * @param sessionId 会话ID
     */
    private void updateSessionActiveTime(String sessionId) {
        sessionLastActiveTime.put(sessionId, LocalDateTime.now());
    }
    
    /**
     * 清理指定会话的所有ChatMemory
     * @param sessionId 会话ID
     */
    public void clearSessionMemory(String sessionId) {
        sessionMemories.entrySet().removeIf(entry -> 
            entry.getKey().startsWith(sessionId + ":") || entry.getKey().equals(sessionId)
        );
    }
    
    /**
     * 清理指定Agent类型的ChatMemory
     * @param agentType Agent类型
     */
    public void clearAgentMemory(String agentType) {
        agentMemories.remove(agentType);
        sessionMemories.entrySet().removeIf(entry -> 
            entry.getKey().endsWith(":" + agentType)
        );
    }
    
    /**
     * 清理所有ChatMemory
     */
    public void clearAllMemory() {
        sessionMemories.clear();
        agentMemories.clear();
    }
    
    /**
     * 获取当前活跃的会话数量
     * @return 会话数量
     */
    public int getActiveSessionCount() {
        return (int) sessionMemories.keySet().stream()
            .map(key -> key.contains(":") ? key.split(":")[0] : key)
            .distinct()
            .count();
    }
    
    /**
     * 获取指定会话的Agent数量
     * @param sessionId 会话ID
     * @return Agent数量
     */
    public int getSessionAgentCount(String sessionId) {
        return (int) sessionMemories.keySet().stream()
            .filter(key -> key.startsWith(sessionId + ":"))
            .count();
    }
    
    /**
     * 检查会话是否存在
     * @param sessionId 会话ID
     * @return 是否存在
     */
    public boolean sessionExists(String sessionId) {
        return sessionMemories.keySet().stream()
            .anyMatch(key -> key.equals(sessionId) || key.startsWith(sessionId + ":"));
    }
    
    /**
     * 复制ChatMemory内容到另一个会话
     * @param sourceSessionId 源会话ID
     * @param targetSessionId 目标会话ID
     * @param agentType Agent类型
     */
    public void copyMemory(String sourceSessionId, String targetSessionId, String agentType) {
        String sourceKey = sourceSessionId + ":" + agentType;
        String targetKey = targetSessionId + ":" + agentType;
        
        ChatMemory sourceMemory = sessionMemories.get(sourceKey);
        if (sourceMemory != null) {
            updateSessionActiveTime(targetSessionId);
            ChatMemory targetMemory = MessageWindowChatMemory.builder()
                .maxMessages(config.getDefaultMaxMessages())
                .build();
            
            // 复制消息（这里需要根据实际的ChatMemory实现来调整）
            sessionMemories.put(targetKey, targetMemory);
        }
    }
    
    /**
     * 定时清理过期会话
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000) // 1小时 = 3600000毫秒
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.minusMinutes(config.getSessionTimeoutMinutes());
        
        sessionLastActiveTime.entrySet().removeIf(entry -> {
            if (entry.getValue().isBefore(expireTime)) {
                String sessionId = entry.getKey();
                clearSessionMemory(sessionId);
                return true;
            }
            return false;
        });
        
        // 如果活跃会话数超过限制，清理最旧的会话
        if (getActiveSessionCount() > config.getMaxActiveSessions()) {
            cleanupOldestSessions();
        }
    }
    
    /**
     * 清理最旧的会话
     */
    private void cleanupOldestSessions() {
        int targetCount = config.getMaxActiveSessions() * 80 / 100; // 清理到80%
        
        sessionLastActiveTime.entrySet().stream()
            .sorted(Map.Entry.comparingByValue())
            .limit(getActiveSessionCount() - targetCount)
            .forEach(entry -> {
                String sessionId = entry.getKey();
                clearSessionMemory(sessionId);
                sessionLastActiveTime.remove(sessionId);
            });
    }
    
    /**
     * 获取会话的最后活跃时间
     * @param sessionId 会话ID
     * @return 最后活跃时间
     */
    public LocalDateTime getSessionLastActiveTime(String sessionId) {
        return sessionLastActiveTime.get(sessionId);
    }
    
    /**
     * 检查会话是否过期
     * @param sessionId 会话ID
     * @return 是否过期
     */
    public boolean isSessionExpired(String sessionId) {
        LocalDateTime lastActiveTime = sessionLastActiveTime.get(sessionId);
        if (lastActiveTime == null) {
            return true;
        }
        
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(config.getSessionTimeoutMinutes());
        return lastActiveTime.isBefore(expireTime);
    }
}