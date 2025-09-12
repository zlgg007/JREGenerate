package com.zlgg.analyzer;

import com.zlgg.model.AnalysisResult;
import com.zlgg.model.ClassDependency;
import com.zlgg.model.JarInfo;
import com.zlgg.model.BuildConfiguration;
import com.zlgg.util.ModuleMapper;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * JAR文件分析器
 * 使用ASM字节码分析技术深度解析JAR文件及其依赖关系
 * 
 * @author zlgg
 * @version 1.0
 */
public class JarAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(JarAnalyzer.class);
    
    // JavaFX相关包前缀
    private static final Set<String> JAVAFX_PACKAGES = Set.of(
        "javafx.", "com.sun.javafx.", "com.sun.glass.", "com.sun.prism."
    );
    
    // Spring Boot相关标识
    private static final Set<String> SPRING_BOOT_INDICATORS = Set.of(
        "BOOT-INF/", "org.springframework.boot"
    );
    
    private final ModuleMapper moduleMapper;
    
    public JarAnalyzer() {
        this.moduleMapper = new ModuleMapper();
    }
    
    /**
     * 分析JAR文件
     * 
     * @param jarPath JAR文件路径
     * @param progressCallback 进度回调函数
     * @return 分析结果
     * @throws IOException 文件读取异常
     */
    public AnalysisResult analyze(Path jarPath, Consumer<Double> progressCallback) throws IOException {
        long startTime = System.currentTimeMillis();
        
        // 初始化进度
        progressCallback.accept(0.0);
        
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            // 第一阶段：收集基本信息 (0-20%)
            logger.debug("第一阶段：收集JAR基本信息");
            JarInfo jarInfo = collectJarInfo(jarFile, jarPath);
            progressCallback.accept(20.0);
            
            // 第二阶段：分析类文件 (20-70%)
            logger.debug("第二阶段：分析类文件依赖关系");
            Map<String, ClassDependency> classDependencies = new ConcurrentHashMap<>();
            Set<String> requiredModules = ConcurrentHashMap.newKeySet();
            Set<String> externalJars = ConcurrentHashMap.newKeySet();
            
            analyzeClasses(jarFile, classDependencies, requiredModules, externalJars, 
                          progress -> progressCallback.accept(20.0 + progress * 0.5));
            
            // 第三阶段：处理Spring Boot结构 (70-90%)
            if (jarInfo.isSpringBootJar()) {
                logger.debug("第三阶段：分析Spring Boot依赖结构");
                analyzeSpringBootDependencies(jarFile, classDependencies, requiredModules, externalJars,
                                            progress -> progressCallback.accept(70.0 + progress * 0.2));
                
                // 强制添加Spring Boot必需的模块（解决运行时动态加载的问题）
                addSpringBootEssentialModules(requiredModules);
            }
            progressCallback.accept(90.0);
            
            // 第四阶段：检测JavaFX依赖 (90-95%)
            logger.debug("第四阶段：检测JavaFX依赖");
            boolean requiresJavaFx = detectJavaFxDependencyEnhanced(jarFile, classDependencies.values());
            if (requiresJavaFx) {
                logger.debug("检测到JavaFX依赖，智能添加相关模块");
                addJavaFxModules(jarFile, requiredModules, classDependencies.values());
            }
            progressCallback.accept(95.0);
            
            // 第五阶段：添加常用的运行时必需模块 (95-98%)
            logger.debug("第五阶段：添加运行时必需模块");
            addCommonRuntimeModules(requiredModules, classDependencies.values(), null);
            progressCallback.accept(98.0);
            
            // 第六阶段：使用jdeps补充分析 (98-100%)
            if (requiredModules.size() < 8) { // 如果检测到的模块太少，用jdeps补充
                logger.debug("检测到的模块较少({}个)，使用jdeps补充分析", requiredModules.size());
                supplementWithJdeps(jarPath, requiredModules);
            }
            progressCallback.accept(100.0);
            
            long analysisTime = System.currentTimeMillis() - startTime;
            
            AnalysisResult result = new AnalysisResult(
                jarInfo,
                requiredModules,
                new ArrayList<>(classDependencies.values()),
                new ArrayList<>(externalJars),
                requiresJavaFx,
                analysisTime
            );
            
            // 只在控制台记录最终的分析结果摘要
            logger.info("JAR分析完成: {}ms, {}个模块, {}个类", 
                       analysisTime, requiredModules.size(), classDependencies.size());
            
            return result;
        }
    }
    
    /**
     * 分析JAR文件
     * 
     * @param jarPath JAR文件路径
     * @param progressCallback 进度回调函数
     * @param buildConfig 构建配置
     * @return 分析结果
     * @throws IOException 文件读取异常
     */
    public AnalysisResult analyze(Path jarPath, Consumer<Double> progressCallback, BuildConfiguration buildConfig) throws IOException {
        long startTime = System.currentTimeMillis();
        
        // 初始化进度
        progressCallback.accept(0.0);
        
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            // 第一阶段：收集基本信息 (0-20%)
            logger.debug("第一阶段：收集JAR基本信息");
            JarInfo jarInfo = collectJarInfo(jarFile, jarPath);
            progressCallback.accept(20.0);
            
            // 第二阶段：分析类文件 (20-70%)
            logger.debug("第二阶段：分析类文件依赖关系");
            Map<String, ClassDependency> classDependencies = new ConcurrentHashMap<>();
            Set<String> requiredModules = ConcurrentHashMap.newKeySet();
            Set<String> externalJars = ConcurrentHashMap.newKeySet();
            
            analyzeClasses(jarFile, classDependencies, requiredModules, externalJars, 
                          progress -> progressCallback.accept(20.0 + progress * 0.5));
            
            // 第三阶段：处理Spring Boot结构 (70-90%)
            if (jarInfo.isSpringBootJar()) {
                logger.debug("第三阶段：分析Spring Boot依赖结构");
                analyzeSpringBootDependencies(jarFile, classDependencies, requiredModules, externalJars,
                                            progress -> progressCallback.accept(70.0 + progress * 0.2));
                
                // 强制添加Spring Boot必需的模块（解决运行时动态加载的问题）
                addSpringBootEssentialModules(requiredModules);
            }
            progressCallback.accept(90.0);
            
            // 第四阶段：检测JavaFX依赖 (90-95%)
            logger.debug("第四阶段：检测JavaFX依赖");
            boolean requiresJavaFx = detectJavaFxDependencyEnhanced(jarFile, classDependencies.values());
            if (requiresJavaFx) {
                logger.debug("检测到JavaFX依赖，智能添加相关模块");
                addJavaFxModules(jarFile, requiredModules, classDependencies.values());
            }
            progressCallback.accept(95.0);
            
            // 第五阶段：添加常用的运行时必需模块 (95-98%)
            logger.debug("第五阶段：添加运行时必需模块");
            addCommonRuntimeModules(requiredModules, classDependencies.values(), buildConfig);
            progressCallback.accept(98.0);
            
            // 第六阶段：使用jdeps补充分析 (98-100%)
            if (requiredModules.size() < 8) { // 如果检测到的模块太少，用jdeps补充
                logger.debug("检测到的模块较少({}个)，使用jdeps补充分析", requiredModules.size());
                supplementWithJdeps(jarPath, requiredModules);
            }
            progressCallback.accept(100.0);
            
            long analysisTime = System.currentTimeMillis() - startTime;
            
            AnalysisResult result = new AnalysisResult(
                jarInfo,
                requiredModules,
                new ArrayList<>(classDependencies.values()),
                new ArrayList<>(externalJars),
                requiresJavaFx,
                analysisTime
            );
            
            // 只在控制台记录最终的分析结果摘要
            logger.info("JAR分析完成: {}ms, {}个模块, {}个类", 
                       analysisTime, requiredModules.size(), classDependencies.size());
            
            return result;
        }
    }
    
    /**
     * 收集JAR基本信息
     */
    private JarInfo collectJarInfo(JarFile jarFile, Path jarPath) throws IOException {
        logger.debug("收集JAR基本信息: {}", jarPath.getFileName());
        
        // 读取Manifest信息
        Manifest manifest = jarFile.getManifest();
        String mainClass = null;
        String version = null;
        
        if (manifest != null) {
            mainClass = manifest.getMainAttributes().getValue("Main-Class");
            version = manifest.getMainAttributes().getValue("Implementation-Version");
            logger.debug("发现Main-Class: {}", mainClass);
            if (version != null) {
                logger.debug("版本信息: {}", version);
            }
        }
        
        // 统计信息
        int classCount = 0;
        int dependencyCount = 0;
        boolean isSpringBootJar = false;
        boolean isJavaFxApp = false;
        
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            String entryName = entry.getName();
            
            if (entryName.endsWith(".class")) {
                classCount++;
                
                // 检测JavaFX应用
                if (!isJavaFxApp && isJavaFxClass(entryName)) {
                    isJavaFxApp = true;
                }
            } else if (entryName.endsWith(".jar")) {
                dependencyCount++;
            }
            
            // 检测Spring Boot应用
            if (!isSpringBootJar && SPRING_BOOT_INDICATORS.stream().anyMatch(entryName::startsWith)) {
                isSpringBootJar = true;
                logger.debug("检测到Spring Boot应用结构");
            }
        }
        
        logger.debug("JAR文件统计: {} 个类文件, {} 个依赖JAR", classCount, dependencyCount);
        if (isJavaFxApp) {
            logger.debug("检测到JavaFX应用");
        }
        
        return JarInfo.builder()
                .jarPath(jarPath)
                .mainClass(mainClass)
                .classCount(classCount)
                .dependencyCount(dependencyCount)
                .isSpringBootJar(isSpringBootJar)
                .isJavaFxApp(isJavaFxApp)
                .jarSize(Files.size(jarPath))
                .version(version)
                .build();
    }
    
    /**
     * 分析类文件依赖关系
     */
    private void analyzeClasses(JarFile jarFile, 
                               Map<String, ClassDependency> classDependencies,
                               Set<String> requiredModules,
                               Set<String> externalJars,
                               Consumer<Double> progressCallback) {
        
        logger.debug("开始分析类文件依赖关系");
        
        List<JarEntry> classEntries = new ArrayList<>();
        Enumeration<JarEntry> entries = jarFile.entries();
        
        // 收集所有类文件（包括内部类，因为它们可能包含重要的依赖关系）
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".class")) {
                classEntries.add(entry);
            }
        }
        
        int totalClasses = classEntries.size();
        int processedClasses = 0;
        
        logger.debug("开始分析 {} 个类文件", totalClasses);
        
        for (JarEntry entry : classEntries) {
            try {
                analyzeClassFile(jarFile, entry, classDependencies, requiredModules);
                processedClasses++;
                
                // 更新进度
                double progress = (double) processedClasses / totalClasses;
                progressCallback.accept(progress);
                
                // 每处理10%的类文件输出一次日志
                if (processedClasses % Math.max(1, totalClasses / 10) == 0) {
                    logger.debug("已处理 {}/{} 个类文件 ({}%)", 
                               processedClasses, totalClasses, 
                               String.format("%.1f", progress * 100));
                }
                
            } catch (Exception e) {
                logger.warn("分析类文件失败: {}, 错误: {}", entry.getName(), e.getMessage());
            }
        }
        
        logger.debug("类文件分析完成，共处理 {} 个类", processedClasses);
    }
    
    /**
     * 使用ASM分析单个类文件
     */
    private void analyzeClassFile(JarFile jarFile, 
                                 JarEntry entry,
                                 Map<String, ClassDependency> classDependencies,
                                 Set<String> requiredModules) throws IOException {
        
        try (InputStream is = jarFile.getInputStream(entry)) {
            ClassReader classReader = new ClassReader(is);
            
            DependencyCollector collector = new DependencyCollector();
            classReader.accept(collector, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            
            String className = classReader.getClassName().replace('/', '.');
            Set<String> dependencies = collector.getDependencies();
            
            // 映射到Java模块
            String javaModule = moduleMapper.getModuleForClass(className);
            if (javaModule != null) {
                requiredModules.add(javaModule);
            }
            
            // 检查依赖的模块
            for (String dep : dependencies) {
                String depModule = moduleMapper.getModuleForClass(dep);
                if (depModule != null) {
                    requiredModules.add(depModule);
                } else if (dep.startsWith("java.") || dep.startsWith("javax.")) {
                    // 只记录可能重要的未映射类，忽略已知的第三方库
                    if (!isKnownThirdPartyClass(dep)) {
                        logger.debug("发现未映射的Java类: {}", dep);
                    }
                }
            }
            
            boolean isJavaFxClass = isJavaFxClass(className);
            
            ClassDependency classDep = new ClassDependency(className, dependencies, javaModule, isJavaFxClass);
            classDependencies.put(className, classDep);
        }
    }
    
    /**
     * 分析Spring Boot应用的依赖JAR
     */
    private void analyzeSpringBootDependencies(JarFile jarFile,
                                             Map<String, ClassDependency> classDependencies,
                                             Set<String> requiredModules,
                                             Set<String> externalJars,
                                             Consumer<Double> progressCallback) {
        
        logger.debug("分析Spring Boot依赖JAR");
        
        List<JarEntry> jarEntries = new ArrayList<>();
        Enumeration<JarEntry> entries = jarFile.entries();
        
        // 收集BOOT-INF/lib/下的JAR文件
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().startsWith("BOOT-INF/lib/") && entry.getName().endsWith(".jar")) {
                jarEntries.add(entry);
                externalJars.add(entry.getName());
            }
        }
        
        // 这里可以进一步分析内嵌的JAR文件，但考虑到性能和复杂性，暂时记录即可
        logger.debug("发现 {} 个Spring Boot依赖JAR", jarEntries.size());
        
        progressCallback.accept(1.0);
    }
    
    /**
     * 增强的JavaFX依赖检测，包括FXML文件分析
     */
    private boolean detectJavaFxDependencyEnhanced(JarFile jarFile, Collection<ClassDependency> classDependencies) {
        // 首先检查类依赖
        boolean hasJavaFxClasses = classDependencies.stream().anyMatch(ClassDependency::isJavaFxClass);
        if (hasJavaFxClasses) {
            return true;
        }
        
        // 检查FXML文件中的JavaFX组件引用
        try {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".fxml")) {
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        String content = new String(is.readAllBytes(), "UTF-8");
                        // 检查是否包含HTMLEditor或其他Web组件
                        if (content.contains("HTMLEditor") || content.contains("WebView") || 
                            content.contains("WebEngine") || content.contains("javafx.scene.web")) {
                            logger.debug("在FXML文件 {} 中检测到JavaFX Web组件", entry.getName());
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("分析FXML文件时出错: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * 智能添加JavaFX相关模块 - 只添加实际需要的模块
     */
    private void addJavaFxModules(JarFile jarFile, Set<String> requiredModules, Collection<ClassDependency> classDependencies) {
        // 基础模块 - JavaFX应用必需
        requiredModules.addAll(Arrays.asList(
            "javafx.base",
            "javafx.graphics"
        ));
        
        // 检查是否需要Controls模块
        if (needsJavaFxModule(classDependencies, "javafx.scene.control", "javafx.scene.chart")) {
            requiredModules.add("javafx.controls");
            logger.debug("检测到JavaFX Controls依赖，添加javafx.controls模块");
        }
        
        // 检查是否需要FXML模块
        if (needsJavaFxModule(classDependencies, "javafx.fxml") || hasResourceFiles(jarFile, ".fxml")) {
            requiredModules.add("javafx.fxml");
            logger.debug("检测到FXML依赖，添加javafx.fxml模块");
        }
        
        // 检查是否需要Web模块
        boolean hasWebDependencies = needsJavaFxModule(classDependencies, "javafx.scene.web");
        boolean hasWebInFxml = hasResourceFiles(jarFile, ".fxml") && hasWebComponentsInFxml(jarFile);
        logger.debug("JavaFX Web检测: 类依赖={}, FXML组件={}", hasWebDependencies, hasWebInFxml);
        
        if (hasWebDependencies || hasWebInFxml) {
            requiredModules.add("javafx.web");
            logger.debug("检测到JavaFX Web依赖，添加javafx.web模块");
        }
        
        // 检查是否需要Media模块
        if (needsJavaFxModule(classDependencies, "javafx.scene.media")) {
            requiredModules.add("javafx.media");
            logger.debug("检测到JavaFX Media依赖，添加javafx.media模块");
        }
        
        // 检查是否需要Swing集成模块
        if (needsJavaFxModule(classDependencies, "javafx.embed.swing")) {
            requiredModules.add("javafx.swing");
            logger.debug("检测到JavaFX Swing依赖，添加javafx.swing模块");
        }
    }
    
    /**
     * 检查是否需要特定的JavaFX模块
     */
    private boolean needsJavaFxModule(Collection<ClassDependency> classDependencies, String... packagePrefixes) {
        Set<String> foundDependencies = new HashSet<>();
        boolean found = classDependencies.stream()
            .flatMap(dep -> dep.getDependencies().stream())
            .anyMatch(className -> {
                for (String prefix : packagePrefixes) {
                    if (className.startsWith(prefix)) {
                        foundDependencies.add(className);
                        return true;
                    }
                }
                return false;
            });
        
        if (found && logger.isDebugEnabled()) {
            logger.debug("找到JavaFX依赖 [{}]: {}", String.join(",", packagePrefixes), foundDependencies);
        }
        
        return found;
    }
    
    /**
     * 检查是否有特定类型的资源文件
     */
    private boolean hasResourceFiles(JarFile jarFile, String extension) {
        try {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(extension)) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.warn("检查资源文件时出错: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * 检查FXML文件中是否包含Web组件
     */
    private boolean hasWebComponentsInFxml(JarFile jarFile) {
        try {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".fxml")) {
                    try (InputStream is = jarFile.getInputStream(entry)) {
                        String content = new String(is.readAllBytes(), "UTF-8");
                        if (content.contains("HTMLEditor") || content.contains("WebView") || 
                            content.contains("WebEngine") || content.contains("javafx.scene.web")) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("分析FXML文件时出错: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * 添加Spring Boot应用必需的模块
     * 这些模块经常在运行时动态加载，静态分析难以发现
     */
    private void addSpringBootEssentialModules(Set<String> requiredModules) {
        // 基于实际Spring Boot应用运行经验，这些模块是必需的
        String[] essentialModules = {
            "java.naming",           // JNDI支持，Logback等需要
            "java.logging",          // 日志框架支持
            "java.management",       // JMX监控支持
            "java.security.jgss",    // Kerberos/JGSS支持，Tomcat需要
            "java.security.sasl",    // SASL认证支持
            "java.scripting",        // 脚本引擎支持
            "java.rmi",              // RMI支持，Spring远程调用
            "java.xml",              // XML处理
            "java.sql",              // 数据库支持
            "java.compiler",         // 动态编译支持（JSP等）
            "java.instrument",       // 字节码增强（AOP等）
            "java.datatransfer",     // 数据传输支持
            "java.transaction.xa",   // XA事务支持
            "java.prefs",            // 偏好设置API
            // Spring Boot 3.x 特别需要的JDK模块
            "jdk.unsupported",       // Spring Boot Loader需要访问sun.misc.Unsafe等内部API
            "jdk.management",        // JMX管理和监控支持
            "jdk.crypto.ec",         // 椭圆曲线加密算法支持
            "jdk.crypto.cryptoki",   // PKCS#11加密支持
            "jdk.localedata",        // 本地化数据支持
            "jdk.jfr",               // Java Flight Recorder支持
            "jdk.security.auth"      // 扩展的安全认证支持
            // 移除了一些不常用的模块：jdk.accessibility, java.smartcardio
        };
        
        int addedCount = 0;
        for (String module : essentialModules) {
            if (requiredModules.add(module)) {
                addedCount++;
            }
        }
        
        logger.info("为Spring Boot应用强制添加了 {} 个必需模块", addedCount);
        logger.debug("添加的模块: {}", String.join(", ", essentialModules));
    }
    
    /**
     * 添加常用的运行时必需模块
     * 这些模块经常在运行时被间接使用，但静态分析难以发现
     */
    private void addCommonRuntimeModules(Set<String> requiredModules, Collection<ClassDependency> classDependencies, BuildConfiguration buildConfig) {
        // 检查是否使用了日志框架（logback、slf4j等）
        boolean hasLoggingFramework = classDependencies.stream()
            .flatMap(dep -> dep.getDependencies().stream())
            .anyMatch(className -> 
                className.startsWith("ch.qos.logback.") ||
                className.startsWith("org.slf4j.") ||
                className.startsWith("java.util.logging.") ||
                className.startsWith("org.apache.logging.log4j.")
            );
        
        if (hasLoggingFramework) {
            // 日志框架通常需要这些模块
            String[] loggingModules = {
                "java.naming",           // JNDI支持，Logback等需要
                "java.logging",          // Java内置日志支持
                "java.management"        // JMX支持，用于日志管理
            };
            
            int addedCount = 0;
            for (String module : loggingModules) {
                if (requiredModules.add(module)) {
                    addedCount++;
                }
            }
            
            if (addedCount > 0) {
                logger.debug("检测到日志框架，添加了 {} 个日志相关模块", addedCount);
            }
        }
        
        // 检查是否使用了JavaFX（额外的运行时模块）
        boolean hasJavaFx = classDependencies.stream()
            .anyMatch(ClassDependency::isJavaFxClass);
            
        if (hasJavaFx) {
            // JavaFX应用可能需要的额外模块
            String[] javafxRuntimeModules = {
                "java.desktop",          // AWT/Swing集成
                "java.datatransfer",     // 剪贴板支持
                "java.prefs"             // 偏好设置API
            };
            
            int addedCount = 0;
            for (String module : javafxRuntimeModules) {
                if (requiredModules.add(module)) {
                    addedCount++;
                }
            }
            
            if (addedCount > 0) {
                logger.debug("检测到JavaFX应用，添加了 {} 个JavaFX运行时模块", addedCount);
            }
        }
        
        // 检查是否使用了反射或动态加载
        boolean hasReflection = classDependencies.stream()
            .flatMap(dep -> dep.getDependencies().stream())
            .anyMatch(className -> 
                className.startsWith("java.lang.reflect.") ||
                className.startsWith("java.lang.Class") ||
                className.contains("ClassLoader")
            );
            
        if (hasReflection) {
            // 反射和动态加载可能需要的模块
            String[] reflectionModules = {
                "java.compiler",         // 动态编译支持
                "java.instrument"        // 字节码增强支持
            };
            
            int addedCount = 0;
            for (String module : reflectionModules) {
                if (requiredModules.add(module)) {
                    addedCount++;
                }
            }
            
            if (addedCount > 0) {
                logger.debug("检测到反射使用，添加了 {} 个反射相关模块", addedCount);
            }
        }
        
        // 添加高级功能支持模块 (javaagent、JNI、keytool、jarsigner等)
        addAdvancedFunctionalityModules(requiredModules, classDependencies, buildConfig);
    }
    
    /**
     * 添加高级功能支持模块
     * 支持 javaagent、agentpath、JNI、JNA、keytool、jarsigner 等功能
     */
    private void addAdvancedFunctionalityModules(Set<String> requiredModules, Collection<ClassDependency> classDependencies, BuildConfiguration buildConfig) {
        // 如果没有启用高级功能支持，跳过
        if (buildConfig != null && !buildConfig.isEnableAdvancedFeatures()) {
            logger.debug("高级功能支持未启用，跳过高级模块添加");
            return;
        }
        
        // 检测是否可能使用JavaAgent或字节码增强
        boolean mayUseAgent = classDependencies.stream()
            .flatMap(dep -> dep.getDependencies().stream())
            .anyMatch(className -> 
                className.startsWith("java.lang.instrument.") ||
                className.contains("Agent") ||
                className.contains("Instrumentation") ||
                className.startsWith("net.bytebuddy.") ||
                className.startsWith("org.objectweb.asm.") ||
                className.startsWith("javassist.") ||
                className.startsWith("org.springframework.aop.")
            );
        
        // 检测是否可能使用JNI/JNA
        boolean mayUseNative = classDependencies.stream()
            .flatMap(dep -> dep.getDependencies().stream())
            .anyMatch(className -> 
                className.startsWith("com.sun.jna.") ||
                className.contains("Native") ||
                className.contains("JNI") ||
                className.startsWith("jnr.ffi.")
            );
        
        // 检测是否可能使用加密/签名功能
        boolean mayUseCrypto = classDependencies.stream()
            .flatMap(dep -> dep.getDependencies().stream())
            .anyMatch(className -> 
                className.startsWith("java.security.") ||
                className.startsWith("javax.crypto.") ||
                className.contains("Certificate") ||
                className.contains("KeyStore") ||
                className.contains("Signature") ||
                className.startsWith("org.bouncycastle.")
            );
        
        // JavaAgent/AgentPath支持模块
        if (mayUseAgent) {
            String[] agentModules = {
                "java.instrument",        // Java Instrumentation API (javaagent核心)
                "java.management",        // JMX管理API (Agent监控常用)
                "jdk.attach",            // 动态附加API (agentpath需要)
                "jdk.management.agent",   // JMX管理代理
                "jdk.unsupported"        // 内部API访问 (某些Agent框架需要)
            };
            
            int addedCount = 0;
            for (String module : agentModules) {
                if (requiredModules.add(module)) {
                    addedCount++;
                }
            }
            
            if (addedCount > 0) {
                logger.info("检测到可能使用JavaAgent，添加了 {} 个Agent支持模块", addedCount);
                logger.debug("Agent模块: {}", String.join(", ", agentModules));
            }
        }
        
        // JNI/JNA支持模块
        if (mayUseNative) {
            String[] nativeModules = {
                "jdk.unsupported"        // JNA需要访问一些内部API
                // 注意：JNI的核心API在java.base中，无需额外模块
            };
            
            int addedCount = 0;
            for (String module : nativeModules) {
                if (requiredModules.add(module)) {
                    addedCount++;
                }
            }
            
            if (addedCount > 0) {
                logger.info("检测到可能使用JNI/JNA，添加了 {} 个本地接口支持模块", addedCount);
                logger.debug("Native模块: {}", String.join(", ", nativeModules));
            }
        }
        
        // 加密/签名工具支持模块 (keytool、jarsigner等)
        if (mayUseCrypto) {
            String[] cryptoModules = {
                "jdk.crypto.ec",         // 椭圆曲线加密算法 (现代加密标准)
                "jdk.crypto.cryptoki",   // PKCS#11硬件加密支持
                "java.security.jgss",    // Kerberos/JGSS认证支持
                "jdk.security.auth",     // 扩展安全认证支持
                "java.naming",           // LDAP证书存储支持
                "jdk.jartool"           // JAR工具支持 (包含jarsigner)
            };
            
            int addedCount = 0;
            for (String module : cryptoModules) {
                if (requiredModules.add(module)) {
                    addedCount++;
                }
            }
            
            if (addedCount > 0) {
                logger.info("检测到加密/安全功能使用，添加了 {} 个加密支持模块", addedCount);
                logger.debug("加密模块: {}", String.join(", ", cryptoModules));
            }
        }
        
        // 总是添加一些常用的高级功能模块（适用于企业级应用）
        String[] commonAdvancedModules = {
            "java.instrument",        // 字节码增强 (Spring AOP等框架常用)
            "jdk.unsupported",       // 内部API访问 (许多框架需要)
            "jdk.crypto.ec",         // 现代加密算法支持
            "jdk.management",        // 高级JMX管理功能
            "jdk.jartool"           // JAR工具 (包含keytool和jarsigner)
        };
        
        int alwaysAddedCount = 0;
        for (String module : commonAdvancedModules) {
            if (requiredModules.add(module)) {
                alwaysAddedCount++;
            }
        }
        
        if (alwaysAddedCount > 0) {
            logger.info("默认添加了 {} 个企业级应用常用的高级功能模块", alwaysAddedCount);
            logger.debug("默认高级模块: {}", String.join(", ", commonAdvancedModules));
        }
    }
    
    /**
     * 使用jdeps补充模块分析
     */
    private void supplementWithJdeps(Path jarPath, Set<String> requiredModules) {
        try {
            // 构建jdeps命令
            String javaHome = System.getProperty("java.home");
            Path jdepsPath = Paths.get(javaHome, "bin", "jdeps" + (System.getProperty("os.name").toLowerCase().contains("windows") ? ".exe" : ""));
            
            if (!Files.exists(jdepsPath)) {
                logger.warn("jdeps工具不存在: {}", jdepsPath);
                return;
            }
            
            List<String> command = new ArrayList<>();
            command.add(jdepsPath.toString());
            command.add("--print-module-deps");
            command.add("--ignore-missing-deps"); // 忽略缺失的依赖
            command.add(jarPath.toString());
            
            logger.debug("执行jdeps命令: {}", String.join(" ", command));
            
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();
            
            // 读取输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        // jdeps输出的是逗号分隔的模块列表
                        String[] modules = line.split(",");
                        for (String module : modules) {
                            module = module.trim();
                            if (!module.isEmpty()) {
                                requiredModules.add(module);
                                logger.debug("jdeps发现模块: {}", module);
                            }
                        }
                    }
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                logger.debug("jdeps分析完成，当前模块总数: {}", requiredModules.size());
            } else {
                logger.warn("jdeps分析失败，退出码: {}", exitCode);
            }
            
        } catch (Exception e) {
            logger.warn("jdeps补充分析失败: {}", e.getMessage());
        }
    }
    
    /**
     * 检查是否为JavaFX类
     */
    private boolean isJavaFxClass(String className) {
        return JAVAFX_PACKAGES.stream().anyMatch(className::startsWith);
    }
    
    /**
     * 检查是否为已知的第三方库类（避免无意义的日志输出）
     */
    private boolean isKnownThirdPartyClass(String className) {
        return className.startsWith("javax.money.") ||           // JSR 354 货币API
               className.startsWith("javax.ws.rs.") ||           // JAX-RS
               className.startsWith("javax.servlet.") ||         // Servlet API
               className.startsWith("javax.mail.") ||            // JavaMail
               className.startsWith("javax.activation.") ||      // JAF
               className.startsWith("javax.annotation.") ||      // JSR 305注解
               className.startsWith("javax.inject.") ||          // JSR 330依赖注入
               className.startsWith("javax.validation.") ||      // Bean Validation
               className.startsWith("javax.persistence.") ||     // JPA
               className.startsWith("javax.enterprise.") ||      // CDI
               className.startsWith("javax.interceptor.") ||     // Interceptors
               className.startsWith("javax.decorator.") ||       // Decorators
               className.startsWith("javax.jms.") ||             // JMS
               className.startsWith("javax.jws.") ||             // JAX-WS
               className.startsWith("javax.xml.ws.") ||          // JAX-WS
               className.startsWith("javax.xml.bind.") ||        // JAXB
               className.startsWith("javax.faces.") ||           // JSF
               className.startsWith("javax.portlet.") ||         // Portlet API
               className.startsWith("javax.ejb.") ||             // EJB
               className.startsWith("javax.resource.");          // JCA
    }
    
    /**
     * ASM类访问器，用于收集类依赖关系
     */
    private static class DependencyCollector extends ClassVisitor {
        
        private final Set<String> dependencies = new HashSet<>();
        
        public DependencyCollector() {
            super(Opcodes.ASM9);
        }
        
        @Override
        public void visit(int version, int access, String name, String signature, 
                         String superName, String[] interfaces) {
            if (superName != null) {
                addDependency(superName);
            }
            if (interfaces != null) {
                for (String iface : interfaces) {
                    addDependency(iface);
                }
            }
        }
        
        @Override
        public org.objectweb.asm.FieldVisitor visitField(int access, String name, String descriptor, 
                                                        String signature, Object value) {
            // 解析字段类型
            parseTypeDescriptor(descriptor);
            if (signature != null) {
                parseSignature(signature);
            }
            return null;
        }
        
        @Override
        public org.objectweb.asm.MethodVisitor visitMethod(int access, String name, String descriptor, 
                                                          String signature, String[] exceptions) {
            // 解析方法签名
            parseMethodDescriptor(descriptor);
            if (signature != null) {
                parseSignature(signature);
            }
            if (exceptions != null) {
                for (String exception : exceptions) {
                    addDependency(exception);
                }
            }
            
            // 返回方法访问器来分析方法体中的依赖
            return new org.objectweb.asm.MethodVisitor(Opcodes.ASM9) {
                @Override
                public void visitTypeInsn(int opcode, String type) {
                    addDependency(type);
                }
                
                @Override
                public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
                    addDependency(owner);
                    parseTypeDescriptor(descriptor);
                }
                
                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
                    addDependency(owner);
                    parseMethodDescriptor(descriptor);
                }
                
                @Override
                public void visitLdcInsn(Object value) {
                    if (value instanceof org.objectweb.asm.Type) {
                        org.objectweb.asm.Type type = (org.objectweb.asm.Type) value;
                        if (type.getSort() == org.objectweb.asm.Type.OBJECT) {
                            addDependency(type.getInternalName());
                        }
                    }
                }
                
                @Override
                public void visitLocalVariable(String name, String descriptor, String signature, 
                                             org.objectweb.asm.Label start, org.objectweb.asm.Label end, int index) {
                    parseTypeDescriptor(descriptor);
                    if (signature != null) {
                        parseSignature(signature);
                    }
                }
            };
        }
        
        @Override
        public void visitInnerClass(String name, String outerName, String innerName, int access) {
            addDependency(name);
        }
        
        private void parseTypeDescriptor(String descriptor) {
            org.objectweb.asm.Type type = org.objectweb.asm.Type.getType(descriptor);
            addTypeReference(type);
        }
        
        private void parseMethodDescriptor(String descriptor) {
            org.objectweb.asm.Type methodType = org.objectweb.asm.Type.getMethodType(descriptor);
            addTypeReference(methodType.getReturnType());
            for (org.objectweb.asm.Type argType : methodType.getArgumentTypes()) {
                addTypeReference(argType);
            }
        }
        
        private void parseSignature(String signature) {
            // 简单的泛型签名解析
            if (signature != null) {
                // 提取L...;格式的类引用
                int start = 0;
                while ((start = signature.indexOf('L', start)) != -1) {
                    int end = signature.indexOf(';', start);
                    if (end != -1) {
                        String className = signature.substring(start + 1, end);
                        addDependency(className);
                        start = end + 1;
                    } else {
                        break;
                    }
                }
            }
        }
        
        private void addTypeReference(org.objectweb.asm.Type type) {
            if (type.getSort() == org.objectweb.asm.Type.OBJECT) {
                addDependency(type.getInternalName());
            } else if (type.getSort() == org.objectweb.asm.Type.ARRAY) {
                addTypeReference(type.getElementType());
            }
        }
        
        private void addDependency(String internalName) {
            if (internalName != null && !internalName.startsWith("java/lang/Object")) {
                // 转换内部类名为标准类名
                String className = internalName.replace('/', '.');
                
                // 过滤掉基本类型和数组
                if (!className.startsWith("[") && !isPrimitiveType(className)) {
                    dependencies.add(className);
                }
            }
        }
        
        private boolean isPrimitiveType(String className) {
            return className.equals("byte") || className.equals("short") || 
                   className.equals("int") || className.equals("long") ||
                   className.equals("float") || className.equals("double") ||
                   className.equals("boolean") || className.equals("char");
        }
        
        public Set<String> getDependencies() {
            return dependencies;
        }
    }
} 