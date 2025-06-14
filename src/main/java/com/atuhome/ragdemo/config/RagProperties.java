package com.atuhome.ragdemo.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {

    private Chunk chunk = new Chunk();
    private Search search = new Search();
    private Embedding embedding = new Embedding();

    @Data
    public static class Chunk {
        private int size = 1000;
        private int overlap = 200;
    }

    @Data
    public static class Search {
        private double similarityThreshold = 0.7;
        private int maxResults = 5;
    }

    @Data
    public static class Embedding {
        private int dimension = 384;
        private int batchSize = 10;
    }
}