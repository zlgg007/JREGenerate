package com.zlgg.model;

import java.util.List;
import java.util.Set;

/**
 * JAR文件分析结果
 * 包含分析过程中收集的所有信息
 * 
 * @author zlgg
 * @version 1.0
 */
public class AnalysisResult {
    
    private final JarInfo jarInfo;
    private final Set<String> requiredModules;
    private final List<ClassDependency> classDependencies;
    private final List<String> externalJars;
    private final boolean requiresJavaFx;
    private final long analysisTimeMs;
    
    public AnalysisResult(JarInfo jarInfo, 
                         Set<String> requiredModules,
                         List<ClassDependency> classDependencies,
                         List<String> externalJars,
                         boolean requiresJavaFx,
                         long analysisTimeMs) {
        this.jarInfo = jarInfo;
        this.requiredModules = requiredModules;
        this.classDependencies = classDependencies;
        this.externalJars = externalJars;
        this.requiresJavaFx = requiresJavaFx;
        this.analysisTimeMs = analysisTimeMs;
    }
    
    /**
     * 获取JAR基本信息
     */
    public JarInfo getJarInfo() {
        return jarInfo;
    }
    
    /**
     * 获取必需的Java模块集合
     */
    public Set<String> getRequiredModules() {
        return requiredModules;
    }
    
    /**
     * 获取类依赖关系列表
     */
    public List<ClassDependency> getClassDependencies() {
        return classDependencies;
    }
    
    /**
     * 获取外部JAR依赖列表
     */
    public List<String> getExternalJars() {
        return externalJars;
    }
    
    /**
     * 是否需要JavaFX支持
     */
    public boolean requiresJavaFx() {
        return requiresJavaFx;
    }
    
    /**
     * 获取分析耗时（毫秒）
     */
    public long getAnalysisTimeMs() {
        return analysisTimeMs;
    }
    
    @Override
    public String toString() {
        return "AnalysisResult{" +
                "jarInfo=" + jarInfo +
                ", requiredModules=" + requiredModules.size() +
                ", classDependencies=" + classDependencies.size() +
                ", externalJars=" + externalJars.size() +
                ", requiresJavaFx=" + requiresJavaFx +
                ", analysisTimeMs=" + analysisTimeMs +
                '}';
    }
} 