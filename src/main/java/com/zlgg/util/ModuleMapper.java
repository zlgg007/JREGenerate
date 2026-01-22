package com.zlgg.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Java类到模块的映射工具
 * 将Java类名映射到对应的Java模块
 * 
 * @author zlgg
 * @version 1.0
 */
public class ModuleMapper {
    
    private static final Logger logger = LoggerFactory.getLogger(ModuleMapper.class);
    
    private final Map<String, String> packageToModuleMap;
    
    public ModuleMapper() {
        this.packageToModuleMap = initializePackageToModuleMap();
        logger.debug("初始化模块映射表，共 {} 个映射条目", packageToModuleMap.size());
    }
    
    /**
     * 根据类名获取对应的Java模块
     * 
     * @param className 完整的类名
     * @return 对应的Java模块名，如果未找到返回null
     */
    public String getModuleForClass(String className) {
        if (className == null || className.isEmpty()) {
            return null;
        }
        
        // 首先检查完整包名
        String packageName = getPackageName(className);
        String module = packageToModuleMap.get(packageName);
        if (module != null) {
            return module;
        }
        
        // 如果没有找到精确匹配，尝试父包
        while (packageName.contains(".")) {
            int lastDot = packageName.lastIndexOf(".");
            packageName = packageName.substring(0, lastDot);
            module = packageToModuleMap.get(packageName);
            if (module != null) {
                return module;
            }
        }
        
        // 如果仍然没有找到，检查是否是Java核心类
        // 只有确定属于java.base的包才返回java.base，其他的返回null让上层处理
        if (className.startsWith("java.lang.") || className.startsWith("java.util.") || 
            className.startsWith("java.io.") || className.startsWith("java.nio.") ||
            className.startsWith("java.net.") || className.startsWith("java.text.") ||
            className.startsWith("java.time.") || className.startsWith("java.math.") ||
            className.startsWith("java.security.") && !className.startsWith("java.security.auth.kerberos") && !className.startsWith("java.security.sasl")) {
            return "java.base"; // 只有这些核心包才默认返回基础模块
        }
        
        return null; // 非Java核心类
    }
    
    /**
     * 从类名中提取包名
     */
    private String getPackageName(String className) {
        int lastDot = className.lastIndexOf(".");
        return lastDot > 0 ? className.substring(0, lastDot) : "";
    }
    
    /**
     * 初始化包名到模块的映射表
     */
    private Map<String, String> initializePackageToModuleMap() {
        Map<String, String> map = new HashMap<>();
        
        // java.base 模块
        map.put("java.lang", "java.base");
        map.put("java.lang.annotation", "java.base");
        map.put("java.lang.invoke", "java.base");
        map.put("java.lang.module", "java.base");
        map.put("java.lang.ref", "java.base");
        map.put("java.lang.reflect", "java.base");
        map.put("java.io", "java.base");
        map.put("java.math", "java.base");
        map.put("java.net", "java.base");
        map.put("java.net.spi", "java.base");
        map.put("java.nio", "java.base");
        map.put("java.nio.channels", "java.base");
        map.put("java.nio.channels.spi", "java.base");
        map.put("java.nio.charset", "java.base");
        map.put("java.nio.charset.spi", "java.base");
        map.put("java.nio.file", "java.base");
        map.put("java.nio.file.attribute", "java.base");
        map.put("java.nio.file.spi", "java.base");
        map.put("java.security", "java.base");
        map.put("java.security.cert", "java.base");
        map.put("java.security.interfaces", "java.base");
        map.put("java.security.spec", "java.base");
        map.put("java.text", "java.base");
        map.put("java.text.spi", "java.base");
        map.put("java.time", "java.base");
        map.put("java.time.chrono", "java.base");
        map.put("java.time.format", "java.base");
        map.put("java.time.temporal", "java.base");
        map.put("java.time.zone", "java.base");
        map.put("java.util", "java.base");
        map.put("java.util.concurrent", "java.base");
        map.put("java.util.concurrent.atomic", "java.base");
        map.put("java.util.concurrent.locks", "java.base");
        map.put("java.util.function", "java.base");
        map.put("java.util.jar", "java.base");
        map.put("java.util.regex", "java.base");
        map.put("java.util.spi", "java.base");
        map.put("java.util.stream", "java.base");
        map.put("java.util.zip", "java.base");
        map.put("javax.crypto", "java.base");
        map.put("javax.crypto.interfaces", "java.base");
        map.put("javax.crypto.spec", "java.base");
        map.put("javax.net", "java.base");
        map.put("javax.net.ssl", "java.base");
        map.put("javax.security.auth", "java.base");
        map.put("javax.security.auth.callback", "java.base");
        map.put("javax.security.auth.login", "java.base");
        map.put("javax.security.auth.spi", "java.base");
        map.put("javax.security.auth.x500", "java.base");
        map.put("javax.security.cert", "java.base");
        
        // java.desktop 模块 (Swing, AWT)
        map.put("java.awt", "java.desktop");
        map.put("java.awt.color", "java.desktop");
        map.put("java.awt.datatransfer", "java.desktop");
        map.put("java.awt.dnd", "java.desktop");
        map.put("java.awt.event", "java.desktop");
        map.put("java.awt.font", "java.desktop");
        map.put("java.awt.geom", "java.desktop");
        map.put("java.awt.im", "java.desktop");
        map.put("java.awt.im.spi", "java.desktop");
        map.put("java.awt.image", "java.desktop");
        map.put("java.awt.image.renderable", "java.desktop");
        map.put("java.awt.print", "java.desktop");
        map.put("javax.swing", "java.desktop");
        map.put("javax.swing.border", "java.desktop");
        map.put("javax.swing.colorchooser", "java.desktop");
        map.put("javax.swing.event", "java.desktop");
        map.put("javax.swing.filechooser", "java.desktop");
        map.put("javax.swing.plaf", "java.desktop");
        map.put("javax.swing.plaf.basic", "java.desktop");
        map.put("javax.swing.plaf.metal", "java.desktop");
        map.put("javax.swing.plaf.multi", "java.desktop");
        map.put("javax.swing.plaf.nimbus", "java.desktop");
        map.put("javax.swing.plaf.synth", "java.desktop");
        map.put("javax.swing.table", "java.desktop");
        map.put("javax.swing.text", "java.desktop");
        map.put("javax.swing.text.html", "java.desktop");
        map.put("javax.swing.text.html.parser", "java.desktop");
        map.put("javax.swing.text.rtf", "java.desktop");
        map.put("javax.swing.tree", "java.desktop");
        map.put("javax.swing.undo", "java.desktop");
        map.put("javax.accessibility", "java.desktop");
        map.put("javax.imageio", "java.desktop");
        map.put("javax.imageio.event", "java.desktop");
        map.put("javax.imageio.metadata", "java.desktop");
        map.put("javax.imageio.plugins.jpeg", "java.desktop");
        map.put("javax.imageio.plugins.tiff", "java.desktop");
        map.put("javax.imageio.spi", "java.desktop");
        map.put("javax.imageio.stream", "java.desktop");
        map.put("javax.print", "java.desktop");
        map.put("javax.print.attribute", "java.desktop");
        map.put("javax.print.attribute.standard", "java.desktop");
        map.put("javax.print.event", "java.desktop");
        map.put("javax.sound.sampled", "java.desktop");
        map.put("javax.sound.sampled.spi", "java.desktop");
        map.put("javax.sound.midi", "java.desktop");
        map.put("javax.sound.midi.spi", "java.desktop");
        
        // 补充java.desktop模块的遗漏包
        map.put("java.applet", "java.desktop");
        map.put("java.beans", "java.desktop");
        map.put("java.beans.beancontext", "java.desktop");
        
        // java.sql 模块
        map.put("java.sql", "java.sql");
        map.put("javax.sql", "java.sql");
        
        // java.xml 模块
        map.put("javax.xml", "java.xml");
        map.put("javax.xml.catalog", "java.xml");
        map.put("javax.xml.datatype", "java.xml");
        map.put("javax.xml.namespace", "java.xml");
        map.put("javax.xml.parsers", "java.xml");
        map.put("javax.xml.stream", "java.xml");
        map.put("javax.xml.stream.events", "java.xml");
        map.put("javax.xml.stream.util", "java.xml");
        map.put("javax.xml.transform", "java.xml");
        map.put("javax.xml.transform.dom", "java.xml");
        map.put("javax.xml.transform.sax", "java.xml");
        map.put("javax.xml.transform.stax", "java.xml");
        map.put("javax.xml.transform.stream", "java.xml");
        map.put("javax.xml.validation", "java.xml");
        map.put("javax.xml.xpath", "java.xml");
        map.put("org.w3c.dom", "java.xml");
        map.put("org.w3c.dom.bootstrap", "java.xml");
        map.put("org.w3c.dom.events", "java.xml");
        map.put("org.w3c.dom.ls", "java.xml");
        map.put("org.w3c.dom.ranges", "java.xml");
        map.put("org.w3c.dom.traversal", "java.xml");
        map.put("org.w3c.dom.views", "java.xml");
        map.put("org.xml.sax", "java.xml");
        map.put("org.xml.sax.ext", "java.xml");
        map.put("org.xml.sax.helpers", "java.xml");
        
        // java.logging 模块
        map.put("java.util.logging", "java.logging");
        
        // java.prefs 模块
        map.put("java.util.prefs", "java.prefs");
        
        // java.management 模块
        map.put("java.lang.management", "java.management");
        map.put("javax.management", "java.management");
        map.put("javax.management.loading", "java.management");
        map.put("javax.management.modelmbean", "java.management");
        map.put("javax.management.monitor", "java.management");
        map.put("javax.management.openmbean", "java.management");
        map.put("javax.management.relation", "java.management");
        map.put("javax.management.remote", "java.management");
        map.put("javax.management.timer", "java.management");
        
        // java.naming 模块
        map.put("javax.naming", "java.naming");
        map.put("javax.naming.directory", "java.naming");
        map.put("javax.naming.event", "java.naming");
        map.put("javax.naming.ldap", "java.naming");
        map.put("javax.naming.spi", "java.naming");
        
        // java.rmi 模块
        map.put("java.rmi", "java.rmi");
        map.put("java.rmi.activation", "java.rmi");
        map.put("java.rmi.dgc", "java.rmi");
        map.put("java.rmi.registry", "java.rmi");
        map.put("java.rmi.server", "java.rmi");
        map.put("javax.rmi.ssl", "java.rmi");
        
        // java.scripting 模块
        map.put("javax.script", "java.scripting");
        
        // java.security.jgss 模块
        map.put("javax.security.auth.kerberos", "java.security.jgss");
        map.put("javax.security.auth.sasl", "java.security.jgss");
        map.put("org.ietf.jgss", "java.security.jgss");
        
        // java.security.sasl 模块
        map.put("javax.security.sasl", "java.security.sasl");
        
        // java.net.http 模块 (Java 11+)
        map.put("java.net.http", "java.net.http");
        
        // java.compiler 模块
        map.put("javax.annotation.processing", "java.compiler");
        map.put("javax.lang.model", "java.compiler");
        map.put("javax.lang.model.element", "java.compiler");
        map.put("javax.lang.model.type", "java.compiler");
        map.put("javax.lang.model.util", "java.compiler");
        map.put("javax.tools", "java.compiler");
        
        // java.instrument 模块
        map.put("java.lang.instrument", "java.instrument");
        
        // JavaFX 模块 (如果存在)
        map.put("javafx.animation", "javafx.graphics");  // 动画API属于graphics模块
        map.put("javafx.application", "javafx.graphics"); // 应用生命周期API属于graphics模块
        map.put("javafx.beans", "javafx.base");
        map.put("javafx.beans.binding", "javafx.base");
        map.put("javafx.beans.property", "javafx.base");
        map.put("javafx.beans.value", "javafx.base");
        map.put("javafx.collections", "javafx.base");
        map.put("javafx.css", "javafx.graphics");    // CSS API属于graphics模块
        map.put("javafx.event", "javafx.base");
        map.put("javafx.fxml", "javafx.fxml");
        map.put("javafx.geometry", "javafx.graphics");
        map.put("javafx.print", "javafx.graphics");   // 打印API属于graphics模块
        map.put("javafx.scene", "javafx.graphics");   // 基础场景图API属于graphics模块
        map.put("javafx.scene.chart", "javafx.controls");
        map.put("javafx.scene.control", "javafx.controls");
        map.put("javafx.scene.control.cell", "javafx.controls");
        map.put("javafx.scene.control.skin", "javafx.controls");
        map.put("javafx.scene.effect", "javafx.graphics");
        map.put("javafx.scene.image", "javafx.graphics");
        map.put("javafx.scene.input", "javafx.graphics");
        map.put("javafx.scene.layout", "javafx.graphics");
        map.put("javafx.scene.paint", "javafx.graphics");
        map.put("javafx.scene.shape", "javafx.graphics");
        map.put("javafx.scene.text", "javafx.graphics");
        map.put("javafx.scene.transform", "javafx.graphics");
        map.put("javafx.stage", "javafx.graphics");
        map.put("javafx.util", "javafx.base");
        map.put("javafx.util.converter", "javafx.base");
        
        // JavaFX Web模块 - 包含HTMLEditor等Web组件
        map.put("javafx.scene.web", "javafx.web");
        
        // JavaFX 并发工具 - Task, Service等属于base模块
        map.put("javafx.concurrent", "javafx.base");
        
        // JavaFX Media模块
        map.put("javafx.scene.media", "javafx.media");
        
        // JavaFX Swing集成模块
        map.put("javafx.embed.swing", "javafx.swing");
        
        // jdk.httpserver 模块 - HTTP服务器API
        map.put("com.sun.net.httpserver", "jdk.httpserver");
        map.put("com.sun.net.httpserver.spi", "jdk.httpserver");
        
        // jdk.unsupported 模块 - 内部API访问
        map.put("sun.misc", "jdk.unsupported");
        map.put("sun.reflect", "jdk.unsupported");
        
        // jdk.management 模块 - 高级JMX管理
        map.put("com.sun.management", "jdk.management");
        
        // jdk.management.agent 模块 - JMX管理代理
        map.put("jdk.management.agent", "jdk.management.agent");
        
        // jdk.attach 模块 - 动态附加API
        map.put("com.sun.tools.attach", "jdk.attach");
        map.put("com.sun.tools.attach.spi", "jdk.attach");
        
        // jdk.jartool 模块 - JAR工具(jarsigner等)
        map.put("com.sun.jarsigner", "jdk.jartool");
        map.put("sun.security.tools.jarsigner", "jdk.jartool");
        
        // jdk.crypto.ec 模块 - 椭圆曲线加密
        map.put("sun.security.ec", "jdk.crypto.ec");
        
        // jdk.crypto.cryptoki 模块 - PKCS#11加密
        map.put("sun.security.pkcs11", "jdk.crypto.cryptoki");
        
        // jdk.security.auth 模块 - 扩展安全认证
        map.put("com.sun.security.auth", "jdk.security.auth");
        map.put("com.sun.security.auth.callback", "jdk.security.auth");
        map.put("com.sun.security.auth.login", "jdk.security.auth");
        map.put("com.sun.security.auth.module", "jdk.security.auth");
        
        // jdk.localedata 模块 - 本地化数据
        map.put("sun.text.resources", "jdk.localedata");
        map.put("sun.util.resources", "jdk.localedata");
        
        // jdk.jfr 模块 - Java Flight Recorder
        map.put("jdk.jfr", "jdk.jfr");
        map.put("jdk.jfr.consumer", "jdk.jfr");
        
        // jdk.zipfs 模块 - ZIP文件系统
        map.put("jdk.nio.zipfs", "jdk.zipfs");
        
        // jdk.jsobject 模块 - JavaScript对象
        map.put("netscape.javascript", "jdk.jsobject");
        
        // jdk.xml.dom 模块 - W3C DOM扩展
        map.put("org.w3c.dom.css", "jdk.xml.dom");
        map.put("org.w3c.dom.html", "jdk.xml.dom");
        map.put("org.w3c.dom.stylesheets", "jdk.xml.dom");
        map.put("org.w3c.dom.xpath", "jdk.xml.dom");
        
        // java.datatransfer 模块 - 数据传输
        map.put("java.awt.datatransfer", "java.datatransfer");
        
        // java.transaction.xa 模块 - XA事务
        map.put("javax.transaction.xa", "java.transaction.xa");
        
        return map;
    }
} 