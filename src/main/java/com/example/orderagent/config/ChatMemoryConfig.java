package com.example.orderagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ChatMemory配置类，定义上下文管理相关的配置选项
 */
@Configuration
@ConfigurationProperties(prefix = "chat.memory")
public class ChatMemoryConfig {
    
    /**
     * 默认最大消息数
     */
    private int defaultMaxMessages = 20;
    
    /**
     * 会话级别最大消息数
     */
    private int sessionMaxMessages = 50;
    
    /**
     * Agent级别最大消息数
     */
    private int agentMaxMessages = 20;
    
    /**
     * 是否启用跨Agent上下文共享
     */
    private boolean enableCrossAgentContext = true;
    
    /**
     * 会话超时时间（分钟）
     */
    private long sessionTimeoutMinutes = 30;
    
    /**
     * 是否启用会话持久化
     */
    private boolean enableSessionPersistence = false;
    
    /**
     * 内存清理间隔（分钟）
     */
    private long memoryCleanupIntervalMinutes = 60;
    
    /**
     * 最大活跃会话数
     */
    private int maxActiveSessions = 1000;
    
    // Getters and Setters
    
    public int getDefaultMaxMessages() {
        return defaultMaxMessages;
    }
    
    public void setDefaultMaxMessages(int defaultMaxMessages) {
        this.defaultMaxMessages = defaultMaxMessages;
    }
    
    public int getSessionMaxMessages() {
        return sessionMaxMessages;
    }
    
    public void setSessionMaxMessages(int sessionMaxMessages) {
        this.sessionMaxMessages = sessionMaxMessages;
    }
    
    public int getAgentMaxMessages() {
        return agentMaxMessages;
    }
    
    public void setAgentMaxMessages(int agentMaxMessages) {
        this.agentMaxMessages = agentMaxMessages;
    }
    
    public boolean isEnableCrossAgentContext() {
        return enableCrossAgentContext;
    }
    
    public void setEnableCrossAgentContext(boolean enableCrossAgentContext) {
        this.enableCrossAgentContext = enableCrossAgentContext;
    }
    
    public long getSessionTimeoutMinutes() {
        return sessionTimeoutMinutes;
    }
    
    public void setSessionTimeoutMinutes(long sessionTimeoutMinutes) {
        this.sessionTimeoutMinutes = sessionTimeoutMinutes;
    }
    
    public boolean isEnableSessionPersistence() {
        return enableSessionPersistence;
    }
    
    public void setEnableSessionPersistence(boolean enableSessionPersistence) {
        this.enableSessionPersistence = enableSessionPersistence;
    }
    
    public long getMemoryCleanupIntervalMinutes() {
        return memoryCleanupIntervalMinutes;
    }
    
    public void setMemoryCleanupIntervalMinutes(long memoryCleanupIntervalMinutes) {
        this.memoryCleanupIntervalMinutes = memoryCleanupIntervalMinutes;
    }
    
    public int getMaxActiveSessions() {
        return maxActiveSessions;
    }
    
    public void setMaxActiveSessions(int maxActiveSessions) {
        this.maxActiveSessions = maxActiveSessions;
    }
}