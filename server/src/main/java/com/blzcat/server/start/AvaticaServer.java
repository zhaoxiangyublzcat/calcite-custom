package com.blzcat.server.start;

import org.apache.calcite.avatica.jdbc.JdbcMeta;
import org.apache.calcite.avatica.remote.Driver;
import org.apache.calcite.avatica.remote.LocalService;
import org.apache.calcite.avatica.server.HttpServer;

import io.micrometer.common.util.StringUtils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.Properties;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Order(1)
public class AvaticaServer implements ApplicationRunner {
    /**
     * 配置文件路径
     */
    @Value("${calcite.model.path}")
    private String modelPath;

    /**
     * avatica 端口
     */
    @Value("${avatica.port:8082}")
    private int avaticaPort;

    @Override
    public void run(ApplicationArguments args) {
        try {
            Class.forName("org.apache.calcite.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        Properties info = new Properties();
        info.setProperty("caseSensitive", "false");
        if (modelPath != null && new File(modelPath).exists()) {
            info.setProperty("model", modelPath);
        } else {
            URL resource = getClass().getClassLoader().getResource(".");
            String localPath = resource.getPath().split("sql-server")[0];
            if (StringUtils.isBlank(localPath)) {
                throw new RuntimeException("Cannot found model.json in localPath (" + localPath +
                    ")");
            }
            String model = localPath + "model.json";
            info.setProperty("model", model);
            log.warn("Can not found model config file {}, use localPath {}", modelPath, model);
        }
        LocalService service;
        try {
            service = new LocalService(new JdbcMeta("jdbc:calcite:", info));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        HttpServer server = new HttpServer.Builder()
            .withHandler(service, Driver.Serialization.JSON)
            .withPort(avaticaPort)
            .build();
        server.start();
    }
}
