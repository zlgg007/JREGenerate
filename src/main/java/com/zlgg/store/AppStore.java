package com.zlgg.store;

import java.util.concurrent.locks.ReentrantLock;

/**
 * 应用程序状态管理类
 * 提供全局状态管理和线程同步
 * 
 * @author zlgg
 * @version 1.0
 */
public class AppStore {
    
    /**
     * 全局锁，用于线程同步
     */
    public static final ReentrantLock lock = new ReentrantLock();
    
    /**
     * 应用程序状态
     */
    public enum AppState {
        READY,      // 就绪状态
        ANALYZING,  // 分析中
        BUILDING,   // 构建中
        COMPLETED,  // 完成
        ERROR       // 错误
    }
    
    private static volatile AppState currentState = AppState.READY;
    
    /**
     * 获取当前应用程序状态
     */
    public static AppState getCurrentState() {
        return currentState;
    }
    
    /**
     * 设置应用程序状态
     */
    public static void setState(AppState state) {
        currentState = state;
    }
    
    /**
     * 检查是否处于忙碌状态
     */
    public static boolean isBusy() {
        return currentState == AppState.ANALYZING || currentState == AppState.BUILDING;
    }
} 