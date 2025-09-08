package com.zlgg.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 应用程序配置类
 * 用于保存和加载应用程序的所有配置信息
 * 
 * @author zlgg
 * @version 1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AppConfig {
    
    // JAR文件路径
    private String jarPath;
    
    // 是否启用JavaFX支持
    private boolean enableJavaFx;
    
    // JavaFX SDK路径
    private String javafxSdkPath;
    
    // 输出目录
    private String outputDirectory;
    
    // 构建配置
    private BuildConfig buildConfig;
    
    // 构建配置子类
    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BuildConfig {
        private boolean enableCompression = true;
        private boolean stripDebugInfo = true;
        private boolean noManPages = true;
        private boolean noHeaderFiles = true;
        private int compressionLevel = 2;
        
        // Getters and Setters
        public boolean isEnableCompression() {
            return enableCompression;
        }
        
        public void setEnableCompression(boolean enableCompression) {
            this.enableCompression = enableCompression;
        }
        
        public boolean isStripDebugInfo() {
            return stripDebugInfo;
        }
        
        public void setStripDebugInfo(boolean stripDebugInfo) {
            this.stripDebugInfo = stripDebugInfo;
        }
        
        public boolean isNoManPages() {
            return noManPages;
        }
        
        public void setNoManPages(boolean noManPages) {
            this.noManPages = noManPages;
        }
        
        public boolean isNoHeaderFiles() {
            return noHeaderFiles;
        }
        
        public void setNoHeaderFiles(boolean noHeaderFiles) {
            this.noHeaderFiles = noHeaderFiles;
        }
        
        public int getCompressionLevel() {
            return compressionLevel;
        }
        
        public void setCompressionLevel(int compressionLevel) {
            this.compressionLevel = compressionLevel;
        }
    }
    
    // 默认构造函数
    public AppConfig() {
        this.buildConfig = new BuildConfig();
    }
    
    // Getters and Setters
    public String getJarPath() {
        return jarPath;
    }
    
    public void setJarPath(String jarPath) {
        this.jarPath = jarPath;
    }
    
    public boolean isEnableJavaFx() {
        return enableJavaFx;
    }
    
    public void setEnableJavaFx(boolean enableJavaFx) {
        this.enableJavaFx = enableJavaFx;
    }
    
    public String getJavafxSdkPath() {
        return javafxSdkPath;
    }
    
    public void setJavafxSdkPath(String javafxSdkPath) {
        this.javafxSdkPath = javafxSdkPath;
    }
    
    public String getOutputDirectory() {
        return outputDirectory;
    }
    
    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
    
    public BuildConfig getBuildConfig() {
        return buildConfig;
    }
    
    public void setBuildConfig(BuildConfig buildConfig) {
        this.buildConfig = buildConfig;
    }
} 