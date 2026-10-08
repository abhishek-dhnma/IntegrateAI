package com.integrateai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
@ConfigurationPropertiesScan
public class IntegrateAiApplication {
    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(IntegrateAiApplication.class, args);
    }

    private static void loadDotEnv() {
        Path envPath = Paths.get(".env");
        if (Files.exists(envPath)) {
            try {
                Files.readAllLines(envPath).stream()
                        .map(String::trim)
                        .filter(l -> !l.isEmpty() && !l.startsWith("#") && l.contains("="))
                        .forEach(l -> {
                            int idx = l.indexOf('=');
                            String key = l.substring(0, idx).trim();
                            String value = l.substring(idx + 1).trim();
                            if (System.getProperty(key) == null && System.getenv(key) == null) {
                                System.setProperty(key, value);
                            }
                        });
            } catch (Exception ignored) {
            }
        }
    }
}
