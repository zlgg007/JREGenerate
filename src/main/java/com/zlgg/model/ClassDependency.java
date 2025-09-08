package com.zlgg.model;

import java.util.Objects;
import java.util.Set;

/**
 * 类依赖关系信息
 * 
 * @author zlgg
 * @version 1.0
 */
public class ClassDependency {
    
    private final String className;
    private final Set<String> dependencies;
    private final String javaModule;
    private final boolean isJavaFxClass;
    
    public ClassDependency(String className, Set<String> dependencies, String javaModule, boolean isJavaFxClass) {
        this.className = className;
        this.dependencies = dependencies;
        this.javaModule = javaModule;
        this.isJavaFxClass = isJavaFxClass;
    }
    
    public String getClassName() {
        return className;
    }
    
    public Set<String> getDependencies() {
        return dependencies;
    }
    
    public String getJavaModule() {
        return javaModule;
    }
    
    public boolean isJavaFxClass() {
        return isJavaFxClass;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ClassDependency that = (ClassDependency) o;
        return Objects.equals(className, that.className);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(className);
    }
    
    @Override
    public String toString() {
        return "ClassDependency{" +
                "className='" + className + '\'' +
                ", dependencies=" + dependencies.size() +
                ", javaModule='" + javaModule + '\'' +
                ", isJavaFxClass=" + isJavaFxClass +
                '}';
    }
} 