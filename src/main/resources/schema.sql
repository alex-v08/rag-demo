-- Activar la extensión pgvector en el esquema public
CREATE EXTENSION IF NOT EXISTS vector;

-- Tabla de documentos
CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500),
    file_size BIGINT,
    content_hash VARCHAR(64) UNIQUE,
    upload_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processing_started_at TIMESTAMP,
    processing_completed_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de chunks de documentos con embeddings vectoriales
CREATE TABLE IF NOT EXISTS document_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    char_start INTEGER,
    char_end INTEGER,
    page_number INTEGER,
    embedding vector(1024), -- Embeddings de 1024 dimensiones para bge-m3
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_chunks_document 
        FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT uk_chunk_index 
        UNIQUE(document_id, chunk_index)
);

-- Tabla de historial de preguntas y respuestas
CREATE TABLE IF NOT EXISTS qa_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question TEXT NOT NULL,
    answer TEXT,
    context_used TEXT,
    sources JSONB,
    model_used VARCHAR(100),
    response_time_ms INTEGER,
    feedback_rating INTEGER,
    feedback_comment TEXT,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para mejorar el rendimiento
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);
CREATE INDEX IF NOT EXISTS idx_documents_upload_date ON documents(upload_date);
CREATE INDEX IF NOT EXISTS idx_documents_content_hash ON documents(content_hash);

CREATE INDEX IF NOT EXISTS idx_chunks_document_id ON document_chunks(document_id);
CREATE INDEX IF NOT EXISTS idx_chunks_chunk_index ON document_chunks(chunk_index);
CREATE INDEX IF NOT EXISTS idx_chunks_page_number ON document_chunks(page_number);

-- Índice vectorial para búsqueda semántica (cosine similarity)
CREATE INDEX IF NOT EXISTS idx_chunks_embedding 
    ON document_chunks USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

CREATE INDEX IF NOT EXISTS idx_qa_created_at ON qa_history(created_at);
CREATE INDEX IF NOT EXISTS idx_qa_model_used ON qa_history(model_used);

-- Función para actualizar automáticamente updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger para actualizar updated_at en documents
CREATE TRIGGER update_documents_updated_at 
    BEFORE UPDATE ON documents 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insertar datos de ejemplo para verificar funcionamiento
INSERT INTO documents (filename, file_size, status, metadata) VALUES 
('documento_ejemplo.pdf', 1024, 'COMPLETED', '{"tipo": "ejemplo", "idioma": "es"}')
ON CONFLICT DO NOTHING;

-- Verificar la instalación
SELECT 'pgvector extension installed' as status, version() as postgresql_version;
SELECT 'Schema created successfully' as status;