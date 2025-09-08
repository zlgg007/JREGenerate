package com.zlgg.util;

import com.zlgg.ui.components.LogArea;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 日志管理器
 * 统一管理控制台日志和UI日志的输出
 * 
 * @author zlgg
 * @version 1.0
 */
public class LogManager {
    
    private static final Logger logger = LoggerFactory.getLogger(LogManager.class);
    private static LogArea uiLogArea;
    
    /**
     * 设置UI日志区域
     */
    public static void setUILogArea(LogArea logArea) {
        uiLogArea = logArea;
    }
    
    /**
     * 记录信息日志 - 只显示在UI
     */
    public static void logInfo(String message) {
        if (uiLogArea != null) {
            uiLogArea.logInfo(message);
        }
    }
    
    /**
     * 记录警告日志 - 显示在UI，重要警告也在控制台
     */
    public static void logWarning(String message) {
        if (uiLogArea != null) {
            uiLogArea.logWarning(message);
        }
        // 只有重要警告才在控制台输出
        if (message.contains("JavaFX") || message.contains("jmods") || message.contains("路径不存在")) {
            logger.warn(message);
        }
    }
    
    /**
     * 记录错误日志 - 显示简短信息在UI，详细信息在控制台
     */
    public static void logError(String shortMessage, Throwable throwable) {
        logger.error(shortMessage, throwable);
        if (uiLogArea != null) {
            uiLogArea.logError(shortMessage);
        }
    }
    
    /**
     * 记录错误日志 - 只显示在UI
     */
    public static void logError(String message) {
        if (uiLogArea != null) {
            uiLogArea.logError(message);
        }
    }
    
    /**
     * 记录成功日志 - 只显示在UI
     */
    public static void logSuccess(String message) {
        if (uiLogArea != null) {
            uiLogArea.logSuccess(message);
        }
    }
    
    /**
     * 记录调试日志 - 只显示在控制台
     */
    public static void logDebug(String message, Object... args) {
        if (logger.isDebugEnabled()) {
            logger.debug(message, args);
        }
    }
    
    /**
     * 记录进度信息 - 只显示在UI
     */
    public static void logProgress(String message) {
        if (uiLogArea != null) {
            uiLogArea.logInfo("⚡ " + message);
        }
    }
    
    /**
     * 记录步骤完成 - 只显示在UI
     */
    public static void logStepComplete(String message) {
        if (uiLogArea != null) {
            uiLogArea.logSuccess("✓ " + message);
        }
    }
} 