package com.atuhome.ragdemo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
@Tag(name = "Health Check", description = "APIs para verificación de salud del sistema")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    @GetMapping
    @Operation(
        summary = "Check de salud básico",
        description = "Verifica que la aplicación esté funcionando correctamente"
    )
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("application", "legal-rag-mvp");
        health.put("version", "0.0.1-SNAPSHOT");
        
        return ResponseEntity.ok(health);
    }

    @GetMapping("/detailed")
    @Operation(
        summary = "Check de salud detallado",
        description = "Verifica el estado de todos los componentes del sistema"
    )
    public ResponseEntity<DetailedHealth> detailedHealth() {
        DetailedHealth health = DetailedHealth.builder()
                .status("UP")
                .timestamp(LocalDateTime.now())
                .application("legal-rag-mvp")
                .version("0.0.1-SNAPSHOT")
                .uptime(getUptimeMillis())
                .memoryUsage(getMemoryUsage())
                .build();
        
        return ResponseEntity.ok(health);
    }

    private long getUptimeMillis() {
        return System.currentTimeMillis() - ManagementFactory.getRuntimeMXBean().getStartTime();
    }

    private Map<String, Object> getMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new HashMap<>();
        
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        memory.put("max", maxMemory);
        memory.put("total", totalMemory);
        memory.put("used", usedMemory);
        memory.put("free", freeMemory);
        memory.put("usage_percentage", (double) usedMemory / totalMemory * 100);
        
        return memory;
    }

    public static class DetailedHealth {
        private final String status;
        private final LocalDateTime timestamp;
        private final String application;
        private final String version;
        private final long uptime;
        private final Map<String, Object> memoryUsage;

        private DetailedHealth(Builder builder) {
            this.status = builder.status;
            this.timestamp = builder.timestamp;
            this.application = builder.application;
            this.version = builder.version;
            this.uptime = builder.uptime;
            this.memoryUsage = builder.memoryUsage;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getStatus() { return status; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public String getApplication() { return application; }
        public String getVersion() { return version; }
        public long getUptime() { return uptime; }
        public Map<String, Object> getMemoryUsage() { return memoryUsage; }

        public static class Builder {
            private String status;
            private LocalDateTime timestamp;
            private String application;
            private String version;
            private long uptime;
            private Map<String, Object> memoryUsage;

            public Builder status(String status) {
                this.status = status;
                return this;
            }

            public Builder timestamp(LocalDateTime timestamp) {
                this.timestamp = timestamp;
                return this;
            }

            public Builder application(String application) {
                this.application = application;
                return this;
            }

            public Builder version(String version) {
                this.version = version;
                return this;
            }

            public Builder uptime(long uptime) {
                this.uptime = uptime;
                return this;
            }

            public Builder memoryUsage(Map<String, Object> memoryUsage) {
                this.memoryUsage = memoryUsage;
                return this;
            }

            public DetailedHealth build() {
                return new DetailedHealth(this);
            }
        }
    }
}