package com.zlgg.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 配置管理器
 * 负责加载和保存应用程序配置
 * 
 * @author zlgg
 * @version 1.0
 */
public class ConfigManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);
    
    private static final String CONFIG_DIR = "config";
    private static final String CONFIG_FILE = "app.json";
    
    private final ObjectMapper objectMapper;
    private final Path configPath;
    
    public ConfigManager() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        // 创建配置文件路径
        this.configPath = Paths.get(CONFIG_DIR, CONFIG_FILE);
        
        // 确保配置目录存在
        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException e) {
            logger.warn("无法创建配置目录: {}", e.getMessage());
        }
    }
    
    /**
     * 加载配置
     * 
     * @return 应用程序配置，如果加载失败则返回默认配置
     */
    public AppConfig loadConfig() {
        try {
            if (Files.exists(configPath)) {
                logger.info("加载配置文件: {}", configPath);
                return objectMapper.readValue(configPath.toFile(), AppConfig.class);
            } else {
                logger.info("配置文件不存在，使用默认配置");
                return new AppConfig();
            }
        } catch (IOException e) {
            logger.error("加载配置失败: {}", e.getMessage(), e);
            return new AppConfig();
        }
    }
    
    /**
     * 保存配置
     * 
     * @param config 要保存的配置
     * @return 保存是否成功
     */
    public boolean saveConfig(AppConfig config) {
        try {
            logger.info("保存配置到文件: {}", configPath);
            objectMapper.writeValue(configPath.toFile(), config);
            logger.info("配置保存成功");
            return true;
        } catch (IOException e) {
            logger.error("保存配置失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取配置文件路径
     * 
     * @return 配置文件路径
     */
    public Path getConfigPath() {
        return configPath;
    }
    
    /**
     * 检查配置文件是否存在
     * 
     * @return 配置文件是否存在
     */
    public boolean configExists() {
        return Files.exists(configPath);
    }
    
    /**
     * 删除配置文件
     * 
     * @return 删除是否成功
     */
    public boolean deleteConfig() {
        try {
            if (Files.exists(configPath)) {
                Files.delete(configPath);
                logger.info("配置文件已删除: {}", configPath);
                return true;
            }
            return true;
        } catch (IOException e) {
            logger.error("删除配置文件失败: {}", e.getMessage(), e);
            return false;
        }
    }
} 