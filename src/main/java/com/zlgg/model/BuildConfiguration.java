package com.zlgg.model;

import java.nio.file.Path;

/**
 * JRE构建配置
 * 
 * @author zlgg
 * @version 1.0
 */
public class BuildConfiguration {
    
    private final Path outputPath;
    private final Path javafxSdkPath;
    private final boolean compress;
    private final boolean stripDebug;
    private final boolean noManPages;
    private final boolean noHeaderFiles;
    private final int compressionLevel;
    private final boolean includeJavaFx;
    
    private BuildConfiguration(Builder builder) {
        this.outputPath = builder.outputPath;
        this.javafxSdkPath = builder.javafxSdkPath;
        this.compress = builder.compress;
        this.stripDebug = builder.stripDebug;
        this.noManPages = builder.noManPages;
        this.noHeaderFiles = builder.noHeaderFiles;
        this.compressionLevel = builder.compressionLevel;
        this.includeJavaFx = builder.includeJavaFx;
    }
    
    public Path getOutputPath() {
        return outputPath;
    }
    
    public Path getJavafxSdkPath() {
        return javafxSdkPath;
    }
    
    public boolean isCompress() {
        return compress;
    }
    
    public boolean isStripDebug() {
        return stripDebug;
    }
    
    public boolean isNoManPages() {
        return noManPages;
    }
    
    public boolean isNoHeaderFiles() {
        return noHeaderFiles;
    }
    
    public int getCompressionLevel() {
        return compressionLevel;
    }
    
    public boolean isIncludeJavaFx() {
        return includeJavaFx;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Path outputPath;
        private Path javafxSdkPath;
        private boolean compress = true;
        private boolean stripDebug = true;
        private boolean noManPages = true;
        private boolean noHeaderFiles = true;
        private int compressionLevel = 2;
        private boolean includeJavaFx = false;
        
        public Builder outputPath(Path outputPath) {
            this.outputPath = outputPath;
            return this;
        }
        
        public Builder javafxSdkPath(Path javafxSdkPath) {
            this.javafxSdkPath = javafxSdkPath;
            this.includeJavaFx = javafxSdkPath != null;
            return this;
        }
        
        public Builder compress(boolean compress) {
            this.compress = compress;
            return this;
        }
        
        public Builder stripDebug(boolean stripDebug) {
            this.stripDebug = stripDebug;
            return this;
        }
        
        public Builder noManPages(boolean noManPages) {
            this.noManPages = noManPages;
            return this;
        }
        
        public Builder noHeaderFiles(boolean noHeaderFiles) {
            this.noHeaderFiles = noHeaderFiles;
            return this;
        }
        
        public Builder compressionLevel(int compressionLevel) {
            this.compressionLevel = compressionLevel;
            return this;
        }
        
        public Builder includeJavaFx(boolean includeJavaFx) {
            this.includeJavaFx = includeJavaFx;
            return this;
        }
        
        public BuildConfiguration build() {
            if (outputPath == null) {
                throw new IllegalArgumentException("输出路径不能为空");
            }
            return new BuildConfiguration(this);
        }
    }
    
    @Override
    public String toString() {
        return "BuildConfiguration{" +
                "outputPath=" + outputPath +
                ", javafxSdkPath=" + javafxSdkPath +
                ", compress=" + compress +
                ", stripDebug=" + stripDebug +
                ", noManPages=" + noManPages +
                ", noHeaderFiles=" + noHeaderFiles +
                ", compressionLevel=" + compressionLevel +
                ", includeJavaFx=" + includeJavaFx +
                '}';
    }
} 