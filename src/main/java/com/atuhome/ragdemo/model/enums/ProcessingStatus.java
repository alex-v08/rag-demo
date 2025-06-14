package com.atuhome.ragdemo.model.enums;

public enum ProcessingStatus {
    EXTRACTING_TEXT("Extrayendo texto del documento"),
    CHUNKING("Dividiendo documento en chunks"),
    GENERATING_EMBEDDINGS("Generando embeddings"),
    STORING("Almacenando en base de datos"),
    COMPLETED("Procesamiento completado"),
    ERROR("Error en el procesamiento");

    private final String description;

    ProcessingStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}