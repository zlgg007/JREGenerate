package com.zlgg.model;

import java.nio.file.Path;

/**
 * JAR文件基本信息
 * 
 * @author zlgg
 * @version 1.0
 */
public class JarInfo {
    
    private final Path jarPath;
    private final String mainClass;
    private final int classCount;
    private final int dependencyCount;
    private final boolean isSpringBootJar;
    private final boolean isJavaFxApp;
    private final long jarSize;
    private final String version;
    
    private JarInfo(Builder builder) {
        this.jarPath = builder.jarPath;
        this.mainClass = builder.mainClass;
        this.classCount = builder.classCount;
        this.dependencyCount = builder.dependencyCount;
        this.isSpringBootJar = builder.isSpringBootJar;
        this.isJavaFxApp = builder.isJavaFxApp;
        this.jarSize = builder.jarSize;
        this.version = builder.version;
    }
    
    public Path getJarPath() {
        return jarPath;
    }
    
    public String getMainClass() {
        return mainClass != null ? mainClass : "未找到";
    }
    
    public int getClassCount() {
        return classCount;
    }
    
    public int getDependencyCount() {
        return dependencyCount;
    }
    
    public boolean isSpringBootJar() {
        return isSpringBootJar;
    }
    
    public boolean isJavaFxApp() {
        return isJavaFxApp;
    }
    
    public long getJarSize() {
        return jarSize;
    }
    
    public String getVersion() {
        return version != null ? version : "未知";
    }
    
    /**
     * 获取格式化的JAR大小
     */
    public String getFormattedSize() {
        if (jarSize < 1024) {
            return jarSize + " B";
        } else if (jarSize < 1024 * 1024) {
            return String.format("%.2f KB", jarSize / 1024.0);
        } else {
            return String.format("%.2f MB", jarSize / (1024.0 * 1024.0));
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Path jarPath;
        private String mainClass;
        private int classCount;
        private int dependencyCount;
        private boolean isSpringBootJar;
        private boolean isJavaFxApp;
        private long jarSize;
        private String version;
        
        public Builder jarPath(Path jarPath) {
            this.jarPath = jarPath;
            return this;
        }
        
        public Builder mainClass(String mainClass) {
            this.mainClass = mainClass;
            return this;
        }
        
        public Builder classCount(int classCount) {
            this.classCount = classCount;
            return this;
        }
        
        public Builder dependencyCount(int dependencyCount) {
            this.dependencyCount = dependencyCount;
            return this;
        }
        
        public Builder isSpringBootJar(boolean isSpringBootJar) {
            this.isSpringBootJar = isSpringBootJar;
            return this;
        }
        
        public Builder isJavaFxApp(boolean isJavaFxApp) {
            this.isJavaFxApp = isJavaFxApp;
            return this;
        }
        
        public Builder jarSize(long jarSize) {
            this.jarSize = jarSize;
            return this;
        }
        
        public Builder version(String version) {
            this.version = version;
            return this;
        }
        
        public JarInfo build() {
            return new JarInfo(this);
        }
    }
    
    @Override
    public String toString() {
        return "JarInfo{" +
                "jarPath=" + jarPath +
                ", mainClass='" + mainClass + '\'' +
                ", classCount=" + classCount +
                ", dependencyCount=" + dependencyCount +
                ", isSpringBootJar=" + isSpringBootJar +
                ", isJavaFxApp=" + isJavaFxApp +
                ", jarSize=" + jarSize +
                ", version='" + version + '\'' +
                '}';
    }
} 