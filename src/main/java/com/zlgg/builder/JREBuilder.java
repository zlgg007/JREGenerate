package com.zlgg.builder;

import com.zlgg.model.AnalysisResult;
import com.zlgg.model.BuildConfiguration;
import com.zlgg.util.LogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

/**
 * JRE构建器
 * 使用jdeps和jlink工具构建最小化的Java运行环境
 * 
 * @author zlgg
 * @version 1.0
 */
public class JREBuilder {
    
    private static final Logger logger = LoggerFactory.getLogger(JREBuilder.class);
    
    // 实际的JRE输出路径（用户选择路径下的library子目录）
    private Path actualOutputPath;
    
    /**
     * 构建自定义JRE
     * 
     * @param analysisResult 分析结果
     * @param config 构建配置
     * @param progressCallback 进度回调
     * @throws Exception 构建异常
     */
    public void buildJRE(AnalysisResult analysisResult, 
                        BuildConfiguration config, 
                        Consumer<Double> progressCallback) throws Exception {
        
        logger.info("开始构建自定义JRE");
        logger.info("用户选择目录: {}", config.getOutputPath());
        logger.info("必需模块: {}", analysisResult.getRequiredModules());
        
        progressCallback.accept(0.0);
        
        try {
            // 第一阶段：准备构建环境 (0-10%)
            prepareEnvironment(config);
            progressCallback.accept(10.0);
            
            // 第二阶段：验证依赖模块 (10-30%)
            List<String> validatedModules = validateModules(analysisResult, config);
            progressCallback.accept(30.0);
            
            // 第三阶段：执行jlink构建 (30-90%)
            executeJlink(validatedModules, config, 
                        progress -> progressCallback.accept(30.0 + progress * 0.6));
            
            // 第四阶段：后处理 (90-100%)
            postProcess(config);
            progressCallback.accept(100.0);
            
            logger.info("自定义JRE构建完成");
            
        } catch (Exception e) {
            logger.error("JRE构建失败", e);
            throw new RuntimeException("JRE构建失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 准备构建环境
     */
    private void prepareEnvironment(BuildConfiguration config) throws IOException {
        logger.debug("准备构建环境");
        
        // 获取用户指定的父目录，在其下创建library子目录作为实际输出目录
        Path userSelectedPath = config.getOutputPath();
        Path actualOutputPath = userSelectedPath.resolve("library");
        
        logger.info("用户选择目录: {}", userSelectedPath);
        logger.info("实际输出目录: {}", actualOutputPath);
        LogManager.logInfo("📁 用户选择目录: " + userSelectedPath);
        LogManager.logInfo("📁 实际JRE输出目录: " + actualOutputPath);
        
        // 确保父目录存在
        Files.createDirectories(userSelectedPath);
        
        // 如果library子目录已存在，删除它
        if (Files.exists(actualOutputPath)) {
            logger.warn("JRE输出目录已存在，将被删除: {}", actualOutputPath);
            LogManager.logWarning("JRE输出目录已存在，正在删除: " + actualOutputPath);
            
            // 递归删除已存在的library目录
            try {
                deleteDirectoryRecursively(actualOutputPath);
                LogManager.logInfo("✓ 成功删除已存在的library目录");
            } catch (IOException e) {
                throw new RuntimeException("无法删除已存在的library目录: " + actualOutputPath + ", " + e.getMessage(), e);
            }
        }
        
        // 存储实际输出路径供后续使用
        this.actualOutputPath = actualOutputPath;
        
        // 验证Java环境
        String javaHome = System.getProperty("java.home");
        LogManager.logInfo("🔧 验证构建环境...");
        LogManager.logInfo("Java环境: " + javaHome);
        
        Path jlinkPath = Paths.get(javaHome, "bin", "jlink" + getExecutableSuffix());
        if (!Files.exists(jlinkPath)) {
            throw new RuntimeException("找不到jlink工具，请确保使用JDK而不是JRE: " + jlinkPath);
        }
        LogManager.logInfo("✓ jlink工具检查通过");
        
        // 检查系统模块
        Path jmodsPath = Paths.get(javaHome, "jmods");
        if (!Files.exists(jmodsPath)) {
            // 尝试替代路径
            Path alternativeJmods = Paths.get(javaHome).getParent().resolve("jmods");
            if (!Files.exists(alternativeJmods)) {
                throw new RuntimeException("找不到系统模块(jmods)，请确保使用完整的JDK: " + jmodsPath);
            } else {
                LogManager.logWarning("使用替代jmods路径: " + alternativeJmods);
            }
        } else {
            LogManager.logInfo("✓ 系统模块检查通过");
        }
        
        // 检查Java版本
        String javaVersion = System.getProperty("java.version");
        LogManager.logInfo("Java版本: " + javaVersion);
        
        // 如果启用JavaFX，验证JavaFX SDK路径
        if (config.isIncludeJavaFx() && config.getJavafxSdkPath() != null) {
            Path javafxModsPath = config.getJavafxSdkPath().resolve("javafx-jmods");
            if (!Files.exists(javafxModsPath)) {
                LogManager.logWarning("JavaFX jmods路径不存在，将不包含JavaFX模块");
            } else {
                LogManager.logInfo("✓ JavaFX模块检查通过");
            }
        }
    }
    
    /**
     * 验证和优化模块列表
     */
    private List<String> validateModules(AnalysisResult analysisResult, BuildConfiguration config) {
        LogManager.logInfo("🔍 验证和过滤模块依赖");
        
        List<String> modules = new ArrayList<>(analysisResult.getRequiredModules());
        
        // 确保包含基础模块
        if (!modules.contains("java.base")) {
            modules.add("java.base");
        }
        
        // 处理JavaFX模块
        List<String> javafxModules = modules.stream()
                .filter(module -> module.startsWith("javafx."))
                .collect(Collectors.toList());
        
        if (!javafxModules.isEmpty()) {
            LogManager.logInfo("检测到JavaFX模块: " + javafxModules);
            
            if (!config.isIncludeJavaFx()) {
                LogManager.logWarning("检测到JavaFX模块但未启用JavaFX支持，将移除JavaFX模块");
                modules = modules.stream()
                        .filter(module -> !module.startsWith("javafx."))
                        .collect(Collectors.toList());
            } else if (config.getJavafxSdkPath() == null) {
                LogManager.logWarning("启用了JavaFX支持但未指定JavaFX SDK路径，将移除JavaFX模块");
                modules = modules.stream()
                        .filter(module -> !module.startsWith("javafx."))
                        .collect(Collectors.toList());
            } else {
                Path javafxModsPath = config.getJavafxSdkPath().resolve("javafx-jmods");
                if (!Files.exists(javafxModsPath)) {
                    LogManager.logWarning("JavaFX jmods路径不存在: " + javafxModsPath + "，将移除JavaFX模块");
                    modules = modules.stream()
                            .filter(module -> !module.startsWith("javafx."))
                            .collect(Collectors.toList());
                } else {
                    LogManager.logInfo("✓ JavaFX模块验证通过，路径: " + javafxModsPath);
                }
            }
        }
        
        // 移除重复模块
        List<String> uniqueModules = modules.stream().distinct().collect(Collectors.toList());
        LogManager.logInfo("最终模块列表 (" + uniqueModules.size() + "个): " + uniqueModules);
        
        return uniqueModules;
    }
    
    /**
     * 添加JavaFX模块
     */
    private void addJavaFxModules(List<String> modules) {
        List<String> javafxModules = List.of(
            "javafx.base",
            "javafx.controls", 
            "javafx.fxml",
            "javafx.graphics"
        );
        
        for (String module : javafxModules) {
            if (!modules.contains(module)) {
                modules.add(module);
            }
        }
        
        logger.debug("添加JavaFX模块: {}", javafxModules);
    }
    
    /**
     * 执行jlink命令构建JRE
     */
    private void executeJlink(List<String> modules, 
                             BuildConfiguration config,
                             Consumer<Double> progressCallback) throws Exception {
        
        LogManager.logInfo("⚙️ 执行jlink构建，模块数量: " + modules.size());
        
        List<String> command = buildJlinkCommand(modules, config);
        logger.debug("jlink命令: {}", String.join(" ", command));
        
        // 执行命令
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(new File(System.getProperty("user.dir")));
        
        Process process = processBuilder.start();
        
        // 收集标准输出和错误输出
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        
        // 读取标准输出
        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), "UTF-8"))) {
                
                String line;
                int lineCount = 0;
                
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    logger.debug("jlink输出: {}", line);
                    lineCount++;
                    
                    // 模拟进度更新
                    if (lineCount % 10 == 0) {
                        double progress = Math.min(0.9, lineCount / 100.0);
                        progressCallback.accept(progress);
                    }
                }
            } catch (IOException e) {
                logger.error("读取jlink输出失败", e);
            }
        });
        
        // 读取错误输出
        Thread errorThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), "UTF-8"))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                    logger.error("jlink错误输出: {}", line);
                }
            } catch (IOException e) {
                logger.error("读取jlink错误输出失败", e);
            }
        });
        
        outputThread.start();
        errorThread.start();
        
        int exitCode = process.waitFor();
        
        // 等待输出线程完成
        try {
            outputThread.join(1000);
            errorThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        if (exitCode != 0) {
            String errorMessage = "jlink执行失败，退出码: " + exitCode;
            if (errorOutput.length() > 0) {
                errorMessage += "\n错误信息: " + errorOutput.toString().trim();
            }
            if (output.length() > 0) {
                errorMessage += "\n输出信息: " + output.toString().trim();
            }
            errorMessage += "\n执行命令: " + String.join(" ", command);
            
            logger.error("jlink执行失败: {}", errorMessage);
            throw new RuntimeException(errorMessage);
        }
        
        progressCallback.accept(1.0);
        LogManager.logStepComplete("jlink执行成功");
    }
    
    /**
     * 构建jlink命令
     */
    private List<String> buildJlinkCommand(List<String> modules, BuildConfiguration config) {
        List<String> command = new ArrayList<>();
        
        // jlink可执行文件路径
        String javaHome = System.getProperty("java.home");
        Path jlinkPath = Paths.get(javaHome, "bin", "jlink" + getExecutableSuffix());
        command.add(jlinkPath.toString());
        
        // 系统模块路径
        Path jmodsPath = Paths.get(javaHome, "jmods");
        StringBuilder modulePathBuilder = new StringBuilder();
        
        if (Files.exists(jmodsPath)) {
            modulePathBuilder.append(jmodsPath.toString());
        } else {
            LogManager.logWarning("系统jmods路径不存在，尝试使用替代路径");
            // 尝试其他可能的路径
            Path alternativeJmods = Paths.get(javaHome).getParent().resolve("jmods");
            if (Files.exists(alternativeJmods)) {
                modulePathBuilder.append(alternativeJmods.toString());
                LogManager.logInfo("✓ 使用替代jmods路径");
            }
        }
        
        // JavaFX模块路径
        if (config.isIncludeJavaFx() && config.getJavafxSdkPath() != null) {
            Path javafxModsPath = config.getJavafxSdkPath().resolve("javafx-jmods");
            if (Files.exists(javafxModsPath)) {
                if (modulePathBuilder.length() > 0) {
                    modulePathBuilder.append(File.pathSeparator);
                }
                modulePathBuilder.append(javafxModsPath.toString());
                LogManager.logInfo("✓ 添加JavaFX模块路径");
            } else {
                LogManager.logWarning("JavaFX jmods路径不存在");
            }
        }
        
        // 设置模块路径
        if (modulePathBuilder.length() > 0) {
            command.add("--module-path");
            command.add(modulePathBuilder.toString());
        }
        
        // 添加模块
        command.add("--add-modules");
        command.add(String.join(",", modules));
        
        // 输出路径
        command.add("--output");
        command.add(actualOutputPath.toString());
        
        // 压缩选项
        if (config.isCompress()) {
            command.add("--compress");
            command.add(String.valueOf(config.getCompressionLevel()));
        }
        
        // 调试信息
        if (config.isStripDebug()) {
            command.add("--strip-debug");
        }
        
        // 手册页
        if (config.isNoManPages()) {
            command.add("--no-man-pages");
        }
        
        // 头文件
        if (config.isNoHeaderFiles()) {
            command.add("--no-header-files");
        }
        
        // 详细输出
        command.add("--verbose");
        
        return command;
    }
    
    /**
     * 后处理操作
     */
    private void postProcess(BuildConfiguration config) throws IOException {
        logger.debug("执行后处理操作");
        
        Path outputPath = actualOutputPath;
        
        // 验证JRE是否构建成功
        Path javaExecutable = outputPath.resolve("bin").resolve("java" + getExecutableSuffix());
        if (!Files.exists(javaExecutable)) {
            throw new RuntimeException("JRE构建失败，找不到java可执行文件: " + javaExecutable);
        }
        
        // 创建版本信息文件
        createVersionInfo(outputPath);
        
        // 显示JRE大小信息
        long jreSize = calculateDirectorySize(outputPath);
        logger.info("生成的JRE大小: {} MB", String.format("%.2f", jreSize / (1024.0 * 1024.0)));
    }
    
    /**
     * 创建版本信息文件
     */
    private void createVersionInfo(Path jrePath) throws IOException {
        Path versionFile = jrePath.resolve("VERSION.txt");
        
        List<String> versionInfo = List.of(
            "自定义JRE构建信息",
            "构建时间: " + java.time.LocalDateTime.now(),
            "构建工具: JREGenerate v1.0",
            "Java版本: " + System.getProperty("java.version"),
            "操作系统: " + System.getProperty("os.name") + " " + System.getProperty("os.arch")
        );
        
        Files.write(versionFile, versionInfo);
        logger.debug("创建版本信息文件: {}", versionFile);
    }
    
    /**
     * 计算目录大小
     */
    private long calculateDirectorySize(Path directory) throws IOException {
        return Files.walk(directory)
                .filter(Files::isRegularFile)
                .mapToLong(file -> {
                    try {
                        return Files.size(file);
                    } catch (IOException e) {
                        logger.warn("无法获取文件大小: {}", file);
                        return 0;
                    }
                })
                .sum();
    }
    
    /**
     * 递归删除目录
     */
    private void deleteDirectoryRecursively(Path directory) throws IOException {
        if (!Files.exists(directory)) {
            return;
        }
        
        logger.debug("开始递归删除目录: {}", directory);
        
        try {
            Files.walk(directory)
                    .sorted((path1, path2) -> path2.toString().length() - path1.toString().length()) // 先删除深层文件
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            logger.debug("删除: {}", path);
                        } catch (IOException e) {
                            logger.warn("无法删除文件/目录: {}, 错误: {}", path, e.getMessage());
                            throw new RuntimeException("删除文件失败: " + path, e);
                        }
                    });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
        
        logger.debug("目录删除完成: {}", directory);
    }
    
    /**
     * 获取可执行文件后缀
     */
    private String getExecutableSuffix() {
        return System.getProperty("os.name").toLowerCase().contains("windows") ? ".exe" : "";
    }
} 