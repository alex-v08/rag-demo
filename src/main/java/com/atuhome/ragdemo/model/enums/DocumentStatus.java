package com.atuhome.ragdemo.model.enums;

public enum DocumentStatus {
    PENDING("Pendiente de procesamiento"),
    PROCESSING("Procesando"),
    COMPLETED("Procesamiento completado"),
    FAILED("Error en procesamiento");

    private final String description;

    DocumentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}