package com.zlgg.ui;

import com.zlgg.analyzer.JarAnalyzer;
import com.zlgg.builder.JREBuilder;
import com.zlgg.config.AppConfig;
import com.zlgg.config.ConfigManager;
import com.zlgg.model.AnalysisResult;
import com.zlgg.model.BuildConfiguration;
import com.zlgg.model.JarInfo;
import com.zlgg.store.AppStore;
import com.zlgg.ui.components.LogArea;
import com.zlgg.util.LogManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ResourceBundle;

/**
 * 主界面控制器
 * 负责处理用户界面交互和业务逻辑调度
 * 
 * @author zlgg
 * @version 1.0
 */
public class MainController implements Initializable {
    
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    
    // FXML 注入的UI组件
    @FXML private TextField jarPathField;
    @FXML private Button browseJarButton;
    @FXML private TextField javafxSdkPathField;
    @FXML private Button browseJavafxButton;
    @FXML private CheckBox enableJavafxCheckBox;
    @FXML private TextField outputDirField;
    @FXML private Button browseOutputButton;
    @FXML private Button analyzeButton;
    @FXML private Button buildJreButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label statusLabel;
    @FXML private TextArea logTextArea;
    @FXML private TreeView<String> dependencyTreeView;
    @FXML private VBox configurationPane;
    @FXML private CheckBox compressJreCheckBox;
    @FXML private CheckBox stripDebugCheckBox;
    @FXML private CheckBox noManPagesCheckBox;
    @FXML private CheckBox noHeaderFilesCheckBox;
    @FXML private ComboBox<String> compressionLevelComboBox;
    @FXML private VBox logContainer;
    
    // 业务对象
    private JarAnalyzer jarAnalyzer;
    private JREBuilder jreBuilder;
    private AnalysisResult currentAnalysis;
    
    // 日志组件
    private LogArea logArea;
    
    // 配置管理器
    private ConfigManager configManager;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        logger.info("初始化主界面控制器");
        
        // 初始化业务对象
        jarAnalyzer = new JarAnalyzer();
        jreBuilder = new JREBuilder();
        
        // 初始化配置管理器
        configManager = new ConfigManager();
        
        // 初始化日志组件
        initializeLogArea();
        
        // 设置全局日志管理器
        LogManager.setUILogArea(logArea);
        
        // 初始化UI组件
        initializeUI();
        
        // 绑定事件处理器
        bindEventHandlers();
        
        // 初始化状态
        AppStore.setState(AppStore.AppState.READY);
        
        // 加载保存的配置
        loadSavedConfig();
        
        logger.info("主界面控制器初始化完成");
        LogManager.logInfo("🎯 JRE生成工具启动完成，准备就绪");
    }
    
    /**
     * 初始化日志区域
     */
    private void initializeLogArea() {
        logArea = new LogArea();
        
        // 将日志组件添加到右侧日志容器中
        logContainer.getChildren().add(logArea.getNode());
        VBox.setVgrow(logArea.getNode(), Priority.ALWAYS);
    }
    
    /**
     * 初始化UI组件
     */
    private void initializeUI() {
        // 初始化压缩级别下拉框
        compressionLevelComboBox.getItems().addAll("0", "1", "2");
        compressionLevelComboBox.setValue("2");
        
        // 设置默认选项
        compressJreCheckBox.setSelected(true);
        stripDebugCheckBox.setSelected(true);
        noManPagesCheckBox.setSelected(true);
        noHeaderFilesCheckBox.setSelected(true);
        
        // 初始状态设置
        buildJreButton.setDisable(true);
        progressBar.setVisible(false);
        updateStatusLabel("就绪", false);
        
        // JavaFX SDK路径初始状态为禁用
        javafxSdkPathField.setDisable(true);
        browseJavafxButton.setDisable(true);
        
        logArea.logInfo("用户界面初始化完成");
    }
    
    /**
     * 绑定事件处理器
     */
    private void bindEventHandlers() {
        // JAR文件选择
        browseJarButton.setOnAction(e -> browseJarFile());
        
        // JavaFX SDK路径选择
        browseJavafxButton.setOnAction(e -> browseJavafxSdk());
        
        // 输出目录选择
        browseOutputButton.setOnAction(e -> browseOutputDirectory());
        
        // 分析按钮
        analyzeButton.setOnAction(e -> analyzeJar());
        
        // 构建JRE按钮
        buildJreButton.setOnAction(e -> buildJRE());
        
        // JavaFX启用复选框
        enableJavafxCheckBox.setOnAction(e -> toggleJavafxSdk());
    }
    
    /**
     * 浏览JAR文件
     */
    private void browseJarFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("选择JAR文件");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("JAR文件", "*.jar")
        );
        
        File selectedFile = fileChooser.showOpenDialog(getStage());
        if (selectedFile != null) {
            jarPathField.setText(selectedFile.getAbsolutePath());
            LogManager.logInfo("📁 已选择JAR文件: " + selectedFile.getName());
        }
    }
    
    /**
     * 浏览JavaFX SDK目录
     */
    private void browseJavafxSdk() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("选择JavaFX SDK目录");
        
        File selectedDir = directoryChooser.showDialog(getStage());
        if (selectedDir != null) {
            javafxSdkPathField.setText(selectedDir.getAbsolutePath());
            LogManager.logInfo("📁 已选择JavaFX SDK目录");
        }
    }
    
    /**
     * 浏览输出目录
     */
    private void browseOutputDirectory() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("选择JRE输出目录");
        
        File selectedDir = directoryChooser.showDialog(getStage());
        if (selectedDir != null) {
            outputDirField.setText(selectedDir.getAbsolutePath());
            LogManager.logInfo("📁 已选择输出目录");
        }
    }
    
    /**
     * 切换JavaFX SDK启用状态
     */
    private void toggleJavafxSdk() {
        boolean enabled = enableJavafxCheckBox.isSelected();
        javafxSdkPathField.setDisable(!enabled);
        browseJavafxButton.setDisable(!enabled);
        
        if (enabled) {
            logArea.logInfo("已启用JavaFX支持");
        } else {
            logArea.logInfo("已禁用JavaFX支持");
            javafxSdkPathField.clear();
        }
    }
    
    /**
     * 分析JAR文件
     */
    private void analyzeJar() {
        String jarPath = jarPathField.getText().trim();
        if (jarPath.isEmpty()) {
            showAlert("错误", "请选择要分析的JAR文件");
            return;
        }
        
        // 保存当前配置
        saveCurrentConfig();
        
        File jarFile = new File(jarPath);
        if (!jarFile.exists() || !jarFile.isFile()) {
            showAlert("错误", "选择的JAR文件不存在或不是有效文件");
            return;
        }
        
        // 设置状态
        AppStore.setState(AppStore.AppState.ANALYZING);
        setUIBusy(true);
        
        // 创建分析任务
        Task<AnalysisResult> analysisTask = new Task<AnalysisResult>() {
            @Override
            protected AnalysisResult call() throws Exception {
                updateMessage("正在分析JAR文件...");
                updateProgress(0, 100);
                
                LogManager.logInfo("🔍 开始分析JAR文件: " + jarFile.getName());
                
                return jarAnalyzer.analyze(jarFile.toPath(), progress -> {
                    Platform.runLater(() -> {
                        updateProgress(progress, 100);
                        String progressMsg = "分析进度: " + String.format("%.1f", progress) + "%";
                        updateMessage(progressMsg);
                        
                        // 添加进度日志
                        if (progress == 20.0) {
                            LogManager.logStepComplete("JAR基本信息收集完成");
                        } else if (progress >= 21.0 && progress <= 69.0 && progress % 10 == 0) {
                            LogManager.logProgress("正在分析类文件... (" + String.format("%.0f", progress) + "%)");
                        } else if (progress == 70.0) {
                            LogManager.logStepComplete("类文件依赖分析完成");
                        } else if (progress == 90.0) {
                            LogManager.logStepComplete("Spring Boot结构分析完成");
                        } else if (progress == 100.0) {
                            LogManager.logStepComplete("JavaFX依赖检测完成");
                        }
                    });
                });
            }
            
            @Override
            protected void succeeded() {
                currentAnalysis = getValue();
                Platform.runLater(() -> {
                    // 解除属性绑定
                    statusLabel.textProperty().unbind();
                    progressBar.progressProperty().unbind();
                    
                    AppStore.setState(AppStore.AppState.READY);
                    setUIBusy(false);
                    updateStatusLabel("分析完成", false);
                    buildJreButton.setDisable(false);
                    
                    // 隐藏进度条
                    progressBar.setVisible(false);
                    
                    // 更新依赖关系树
                    updateDependencyTree();
                    
                    LogManager.logSuccess("JAR文件分析完成!");
                    LogManager.logInfo("📊 分析结果统计:");
                    LogManager.logInfo("  • 必需模块: " + currentAnalysis.getRequiredModules().size() + " 个");
                    LogManager.logInfo("  • 类依赖: " + currentAnalysis.getClassDependencies().size() + " 个");
                    LogManager.logInfo("  • 分析耗时: " + currentAnalysis.getAnalysisTimeMs() + "ms");
                    
                    if (currentAnalysis.requiresJavaFx()) {
                        LogManager.logWarning("检测到JavaFX依赖，已自动启用JavaFX支持");
                        enableJavafxCheckBox.setSelected(true);
                        toggleJavafxSdk();
                    }
                    
                    LogManager.logInfo("🚀 现在可以构建自定义JRE了!");
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    // 解除属性绑定
                    statusLabel.textProperty().unbind();
                    progressBar.progressProperty().unbind();
                    
                    AppStore.setState(AppStore.AppState.ERROR);
                    setUIBusy(false);
                    updateStatusLabel("分析失败", true);
                    
                    // 隐藏进度条
                    progressBar.setVisible(false);
                    
                    Throwable exception = getException();
                    LogManager.logError("分析失败: " + exception.getMessage(), exception);
                    showAlert("分析失败", "JAR文件分析过程中发生错误: " + exception.getMessage());
                });
            }
        };
        
        // 绑定进度和状态
        progressBar.progressProperty().bind(analysisTask.progressProperty());
        statusLabel.textProperty().bind(analysisTask.messageProperty());
        
        // 显示进度条
        progressBar.setVisible(true);
        
        // 启动分析任务
        Thread analysisThread = new Thread(analysisTask);
        analysisThread.setDaemon(true);
        analysisThread.start();
    }
    
    /**
     * 构建自定义JRE
     */
    private void buildJRE() {
        if (currentAnalysis == null) {
            showAlert("错误", "请先分析JAR文件");
            return;
        }
        
        String outputDir = outputDirField.getText().trim();
        if (outputDir.isEmpty()) {
            showAlert("错误", "请选择JRE输出目录");
            return;
        }
        
        // 构建配置
        BuildConfiguration config = createBuildConfiguration();
        
        // 设置状态
        AppStore.setState(AppStore.AppState.BUILDING);
        setUIBusy(true);
        
        // 创建构建任务
        Task<Void> buildTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("正在构建自定义JRE...");
                updateProgress(0, 100);
                
                LogManager.logInfo("🔨 开始构建自定义JRE");
                LogManager.logInfo("📂 输出路径: " + config.getOutputPath());
                LogManager.logInfo("📦 包含模块: " + currentAnalysis.getRequiredModules().size() + " 个");
                if (config.isIncludeJavaFx()) {
                    LogManager.logInfo("🎨 包含JavaFX支持");
                }
                
                jreBuilder.buildJRE(currentAnalysis, config, progress -> {
                    Platform.runLater(() -> {
                        updateProgress(progress, 100);
                        updateMessage("构建进度: " + String.format("%.1f", progress) + "%");
                    });
                });
                
                return null;
            }
            
            @Override
            protected void succeeded() {
                Platform.runLater(() -> {
                    // 解除属性绑定
                    statusLabel.textProperty().unbind();
                    progressBar.progressProperty().unbind();
                    
                    AppStore.setState(AppStore.AppState.COMPLETED);
                    setUIBusy(false);
                    updateStatusLabel("构建完成", false);
                    
                    // 隐藏进度条
                    progressBar.setVisible(false);
                    
                    LogManager.logSuccess("自定义JRE构建完成!");
                    LogManager.logInfo("📂 JRE已生成到: " + config.getOutputPath());
                    LogManager.logInfo("🎉 现在可以使用这个自定义JRE运行您的应用程序了!");
                    
                    // 显示构建成功对话框
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("构建完成");
                    alert.setHeaderText("自定义JRE构建成功!");
                    alert.setContentText("JRE已生成到: " + config.getOutputPath() + 
                                       "\n\n您现在可以使用这个自定义JRE运行您的应用程序了。");
                    alert.showAndWait();
                });
            }
            
            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    // 解除属性绑定
                    statusLabel.textProperty().unbind();
                    progressBar.progressProperty().unbind();
                    
                    AppStore.setState(AppStore.AppState.ERROR);
                    setUIBusy(false);
                    updateStatusLabel("构建失败", true);
                    
                    // 隐藏进度条
                    progressBar.setVisible(false);
                    
                    Throwable exception = getException();
                    String userMessage = "构建失败";
                    String detailMessage = exception.getMessage();
                    
                    // 根据错误类型提供更友好的用户提示
                    if (detailMessage != null) {
                        if (detailMessage.contains("jlink执行失败")) {
                            userMessage = "JRE构建失败，请检查Java环境和模块依赖";
                        } else if (detailMessage.contains("jmods")) {
                            userMessage = "缺少Java模块，请确保使用完整的JDK";
                        } else if (detailMessage.contains("jlink工具不存在")) {
                            userMessage = "请使用JDK而不是JRE运行此工具";
                        }
                    }
                    
                    LogManager.logError(userMessage, exception);
                    showAlert("构建失败", userMessage + "\n\n详细信息请查看控制台日志");
                });
            }
        };
        
        // 绑定进度和状态
        progressBar.progressProperty().bind(buildTask.progressProperty());
        statusLabel.textProperty().bind(buildTask.messageProperty());
        
        // 显示进度条
        progressBar.setVisible(true);
        
        // 启动构建任务
        Thread buildThread = new Thread(buildTask);
        buildThread.setDaemon(true);
        buildThread.start();
    }
    
    /**
     * 创建构建配置
     */
    private BuildConfiguration createBuildConfiguration() {
        BuildConfiguration.Builder builder = BuildConfiguration.builder()
            .outputPath(Paths.get(outputDirField.getText().trim()))
            .compress(compressJreCheckBox.isSelected())
            .stripDebug(stripDebugCheckBox.isSelected())
            .noManPages(noManPagesCheckBox.isSelected())
            .noHeaderFiles(noHeaderFilesCheckBox.isSelected())
            .compressionLevel(Integer.parseInt(compressionLevelComboBox.getValue()));
        
        // JavaFX配置
        boolean enableJavaFx = enableJavafxCheckBox.isSelected();
        builder.includeJavaFx(enableJavaFx);
        
        if (enableJavaFx) {
            String javafxPath = javafxSdkPathField.getText().trim();
            if (!javafxPath.isEmpty()) {
                builder.javafxSdkPath(Paths.get(javafxPath));
            }
        }
        
        return builder.build();
    }
    
    /**
     * 更新依赖关系树
     */
    private void updateDependencyTree() {
        if (currentAnalysis == null) {
            return;
        }
        
        TreeItem<String> root = new TreeItem<>("依赖关系分析结果");
        root.setExpanded(true);
        
        // 添加必需模块
        TreeItem<String> modulesItem = new TreeItem<>("必需模块 (" + currentAnalysis.getRequiredModules().size() + ")");
        modulesItem.setExpanded(true);
        for (String module : currentAnalysis.getRequiredModules()) {
            modulesItem.getChildren().add(new TreeItem<>(module));
        }
        root.getChildren().add(modulesItem);
        
        // 添加JAR信息
        TreeItem<String> jarInfoItem = new TreeItem<>("JAR信息");
        jarInfoItem.setExpanded(true);
        
        JarInfo jarInfo = currentAnalysis.getJarInfo();
        jarInfoItem.getChildren().add(new TreeItem<>("主类: " + jarInfo.getMainClass()));
        jarInfoItem.getChildren().add(new TreeItem<>("类文件数: " + jarInfo.getClassCount()));
        jarInfoItem.getChildren().add(new TreeItem<>("JAR大小: " + jarInfo.getFormattedSize()));
        jarInfoItem.getChildren().add(new TreeItem<>("依赖数: " + jarInfo.getDependencyCount()));
        jarInfoItem.getChildren().add(new TreeItem<>("Spring Boot应用: " + (jarInfo.isSpringBootJar() ? "是" : "否")));
        jarInfoItem.getChildren().add(new TreeItem<>("JavaFX应用: " + (jarInfo.isJavaFxApp() ? "是" : "否")));
        
        root.getChildren().add(jarInfoItem);
        
        dependencyTreeView.setRoot(root);
    }
    
    /**
     * 设置UI忙碌状态
     */
    private void setUIBusy(boolean busy) {
        analyzeButton.setDisable(busy);
        buildJreButton.setDisable(busy || currentAnalysis == null);
        browseJarButton.setDisable(busy);
        browseOutputButton.setDisable(busy);
        browseJavafxButton.setDisable(busy);
        
        progressBar.setVisible(busy);
    }
    
    /**
     * 更新状态标签
     */
    private void updateStatusLabel(String text, boolean isError) {
        statusLabel.setText(text);
        if (isError) {
            statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        } else {
            statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        }
    }
    
    /**
     * 显示警告对话框
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * 保存当前配置
     */
    private void saveCurrentConfig() {
        try {
            AppConfig config = getCurrentConfig();
            boolean success = configManager.saveConfig(config);
            if (success) {
                LogManager.logInfo("💾 配置已保存");
            } else {
                LogManager.logError("配置保存失败");
            }
        } catch (Exception e) {
            logger.error("保存配置时发生错误", e);
            LogManager.logError("保存配置失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 加载保存的配置
     */
    private void loadSavedConfig() {
        try {
            AppConfig config = configManager.loadConfig();
            applyConfig(config);
            
            if (configManager.configExists()) {
                LogManager.logInfo("📄 已加载保存的配置");
            } else {
                LogManager.logInfo("📄 使用默认配置");
            }
        } catch (Exception e) {
            logger.error("加载配置时发生错误", e);
            LogManager.logError("加载配置失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取当前UI配置
     */
    private AppConfig getCurrentConfig() {
        AppConfig config = new AppConfig();
        
        // 基本配置
        config.setJarPath(jarPathField.getText().trim());
        config.setEnableJavaFx(enableJavafxCheckBox.isSelected());
        config.setJavafxSdkPath(javafxSdkPathField.getText().trim());
        config.setOutputDirectory(outputDirField.getText().trim());
        
        // 构建配置
        AppConfig.BuildConfig buildConfig = config.getBuildConfig();
        buildConfig.setEnableCompression(compressJreCheckBox.isSelected());
        buildConfig.setStripDebugInfo(stripDebugCheckBox.isSelected());
        buildConfig.setNoManPages(noManPagesCheckBox.isSelected());
        buildConfig.setNoHeaderFiles(noHeaderFilesCheckBox.isSelected());
        
        // 压缩级别
        String compressionLevel = compressionLevelComboBox.getValue();
        if (compressionLevel != null && !compressionLevel.isEmpty()) {
            try {
                buildConfig.setCompressionLevel(Integer.parseInt(compressionLevel));
            } catch (NumberFormatException e) {
                buildConfig.setCompressionLevel(2); // 默认值
            }
        }
        
        return config;
    }
    
    /**
     * 应用配置到UI
     */
    private void applyConfig(AppConfig config) {
        if (config == null) return;
        
        // 基本配置
        if (config.getJarPath() != null) {
            jarPathField.setText(config.getJarPath());
        }
        
        enableJavafxCheckBox.setSelected(config.isEnableJavaFx());
        
        if (config.getJavafxSdkPath() != null) {
            javafxSdkPathField.setText(config.getJavafxSdkPath());
        }
        
        if (config.getOutputDirectory() != null) {
            outputDirField.setText(config.getOutputDirectory());
        }
        
        // 构建配置
        AppConfig.BuildConfig buildConfig = config.getBuildConfig();
        if (buildConfig != null) {
            compressJreCheckBox.setSelected(buildConfig.isEnableCompression());
            stripDebugCheckBox.setSelected(buildConfig.isStripDebugInfo());
            noManPagesCheckBox.setSelected(buildConfig.isNoManPages());
            noHeaderFilesCheckBox.setSelected(buildConfig.isNoHeaderFiles());
            compressionLevelComboBox.setValue(String.valueOf(buildConfig.getCompressionLevel()));
        }
        
        // 更新JavaFX相关UI状态
        toggleJavafxSdk();
    }
    
    /**
     * 获取当前Stage
     */
    private Stage getStage() {
        return (Stage) jarPathField.getScene().getWindow();
    }
} 