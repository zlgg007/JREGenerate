package com.zlgg.ui.components;

import cn.hutool.core.date.DateUtil;
import com.zlgg.store.AppStore;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 日志显示组件
 * 提供线程安全的日志显示功能，支持自动滚动和日志清理
 * 
 * @author zlgg
 * @version 1.0
 */
public class LogArea {
    private final ScrollPane scrollPane;
    private final TextFlow textFlow;
    private final VBox container;
    private boolean editable = true;
    
    // 日志条目计数
    private final AtomicInteger logCount = new AtomicInteger(0);
    // 默认最大日志条目数（可以根据需要调整）
    private static final int MAX_LOG_ENTRIES = 1000;
    // 清理阈值，当超过这个值时进行清理
    private static final int CLEANUP_THRESHOLD = 800;

    public LogArea() {
        // 创建文本流容器
        textFlow = new TextFlow();
        textFlow.setStyle("-fx-background-color: #2c3e50; -fx-padding: 10;");
        textFlow.setMaxWidth(Double.MAX_VALUE);
        textFlow.setLineSpacing(3);
        
        // 创建滚动面板
        scrollPane = new ScrollPane(textFlow);
        scrollPane.setStyle("-fx-background-color: #2c3e50; -fx-background: #2c3e50;");
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setMaxWidth(Double.MAX_VALUE);
        scrollPane.setMaxHeight(Double.MAX_VALUE);
        
        // 创建主容器
        container = new VBox(scrollPane);
        container.setStyle("-fx-background-color: #2c3e50;");
        container.setMaxWidth(Double.MAX_VALUE);
        container.setMaxHeight(Double.MAX_VALUE);
        container.setPrefHeight(300); // 保持合适的默认高度
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // 设置文本流的宽度，并在容器大小变化时更新
        container.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.doubleValue() > 0) {
                double availableWidth = Math.max(200, newVal.doubleValue() - 40); // 确保最小宽度，为滚动条留出空间
                textFlow.setPrefWidth(availableWidth);
                textFlow.setMaxWidth(availableWidth);
            }
        });
        
        // 设置自动滚动到底部
        textFlow.heightProperty().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> scrollPane.setVvalue(1.0));
        });
    }

    /**
     * 获取组件节点
     */
    public Node getNode() {
        return container;
    }

    /**
     * 设置是否可编辑
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    /**
     * 获取是否可编辑
     */
    public boolean isEditable() {
        return editable;
    }

    /**
     * 记录信息日志
     */
    public void logInfo(String message) {
        log(createLogEntry(message, Color.LIGHTGREEN));
    }

    /**
     * 记录错误日志
     */
    public void logError(String message) {
        log(createLogEntry(message, Color.LIGHTCORAL));
    }

    /**
     * 记录警告日志
     */
    public void logWarning(String message) {
        log(createLogEntry(message, Color.ORANGE));
    }

    /**
     * 记录调试日志
     */
    public void logDebug(String message) {
        log(createLogEntry(message, Color.LIGHTBLUE));
    }

    /**
     * 记录成功日志
     */
    public void logSuccess(String message) {
        log(createLogEntry(message, Color.LIGHTGREEN));
    }

    /**
     * 创建日志条目
     */
    private Text[] createLogEntry(String message, Color color) {
        // 创建时间戳
        Text timestamp = new Text("[" + DateUtil.format(DateUtil.date(), "HH:mm:ss") + "] ");
        timestamp.setFont(Font.font("Consolas", 12));
        timestamp.setFill(Color.LIGHTGRAY);
        
        // 创建消息文本
        Text messageText = new Text(message + "\n");
        messageText.setFont(Font.font("Consolas", 12));
        messageText.setFill(color);
        
        return new Text[]{timestamp, messageText};
    }

    /**
     * 检查并清理日志
     */
    private void checkAndCleanupLogs() {
        if (logCount.get() > MAX_LOG_ENTRIES) {
            Platform.runLater(() -> {
                try {
                    AppStore.lock.lock();
                    // 保留最新的 CLEANUP_THRESHOLD 条日志
                    int totalNodes = textFlow.getChildren().size();
                    int nodesToRemove = totalNodes - (CLEANUP_THRESHOLD * 2); // 每条日志有2个节点（时间戳和消息）
                    if (nodesToRemove > 0) {
                        textFlow.getChildren().remove(0, nodesToRemove);
                        logCount.set(CLEANUP_THRESHOLD);
                    }
                } finally {
                    AppStore.lock.unlock();
                }
            });
        }
    }

    /**
     * 记录日志
     */
    private void log(Text... texts) {
        Platform.runLater(() -> {
            try {
                AppStore.lock.lock();
                textFlow.getChildren().addAll(texts);
                logCount.incrementAndGet();
                checkAndCleanupLogs();
            } finally {
                AppStore.lock.unlock();
            }
        });
    }

    /**
     * 清空日志
     */
    public void clear() {
        Platform.runLater(() -> {
            try {
                AppStore.lock.lock();
                textFlow.getChildren().clear();
                logCount.set(0);
            } finally {
                AppStore.lock.unlock();
            }
        });
    }

    /**
     * 获取日志内容
     */
    public String getLog() {
        StringBuilder content = new StringBuilder();
        for (Node node : textFlow.getChildren()) {
            if (node instanceof Text) {
                content.append(((Text) node).getText());
            }
        }
        return content.toString();
    }

    /**
     * 获取日志内容（别名方法）
     */
    public String getContent() {
        return getLog();
    }

    /**
     * 追加原始文本（不带时间戳）
     */
    public void appendText(String text, Color color) {
        Platform.runLater(() -> {
            try {
                AppStore.lock.lock();
                Text textNode = new Text(text);
                textNode.setFont(Font.font("Consolas", 12));
                textNode.setFill(color);
                textFlow.getChildren().add(textNode);
            } finally {
                AppStore.lock.unlock();
            }
        });
    }

    /**
     * 追加换行
     */
    public void appendNewLine() {
        appendText("\n", Color.WHITE);
    }
} 