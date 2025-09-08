package com.zlgg;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * JRE生成工具主应用程序
 * 基于JavaFX的可视化JAR包分析和自定义JRE构建工具
 * 
 * @author zlgg
 * @version 1.0
 */
public class JREGenerateApplication extends Application {
    
    private static final Logger logger = LoggerFactory.getLogger(JREGenerateApplication.class);
    
    @Override
    public void start(Stage stage) throws IOException {
        logger.info("启动JRE生成工具...");
        
        try {
            // 加载FXML布局文件
            FXMLLoader fxmlLoader = new FXMLLoader(
                JREGenerateApplication.class.getResource("/fxml/main-view.fxml")
            );
            
            Scene scene = new Scene(fxmlLoader.load(), 1200, 800);
            
            // 设置应用程序样式
            scene.getStylesheets().add(
                JREGenerateApplication.class.getResource("/css/application.css").toExternalForm()
            );
            
            // 配置主窗口
            stage.setTitle("JRE生成工具 v1.0");
            stage.setScene(scene);
            stage.setMinWidth(800);
            stage.setMinHeight(600);
            
            // 设置应用程序图标
            try (InputStream iconStream = getClass().getResourceAsStream("/images/app.png")) {
                if (iconStream != null) {
                    stage.getIcons().add(new Image(iconStream));
                }
            } catch (Exception e) {
                logger.warn("无法加载应用程序图标: {}", e.getMessage());
            }
            
            stage.show();
            logger.info("应用程序启动成功");
            
        } catch (Exception e) {
            logger.error("应用程序启动失败", e);
            throw e;
        }
    }

    public static void main(String[] args) {
        logger.info("JRE生成工具启动中...");
        launch();
    }
} 