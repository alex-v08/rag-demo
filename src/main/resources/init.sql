-- init.sql
-- Script de inicialización para la base de datos del sistema RAG

-- Crear extensiones necesarias
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS vector;

-- Tabla de documentos
CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500),
    file_size BIGINT,
    content_hash VARCHAR(64),
    upload_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    processing_started_at TIMESTAMP,
    processing_completed_at TIMESTAMP,
    error_message TEXT,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de chunks de documentos
CREATE TABLE IF NOT EXISTS document_chunks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    content TEXT NOT NULL,
    embedding vector(384), -- Dimensión para all-MiniLM-L6-v2
    char_start INTEGER,
    char_end INTEGER,
    page_number INTEGER,
    metadata JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(document_id, chunk_index)
);

-- Tabla de historial Q&A
CREATE TABLE IF NOT EXISTS qa_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    question TEXT NOT NULL,
    answer TEXT,
    context_used TEXT,
    sources JSONB,
    model_used VARCHAR(100),
    response_time_ms INTEGER,
    feedback_rating INTEGER CHECK (feedback_rating >= 1 AND feedback_rating <= 5),
    feedback_comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Índices para optimizar consultas
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);
CREATE INDEX IF NOT EXISTS idx_documents_filename ON documents(filename);
CREATE INDEX IF NOT EXISTS idx_documents_upload_date ON documents(upload_date DESC);
CREATE INDEX IF NOT EXISTS idx_documents_content_hash ON documents(content_hash);

CREATE INDEX IF NOT EXISTS idx_chunks_document_id ON document_chunks(document_id);
CREATE INDEX IF NOT EXISTS idx_chunks_chunk_index ON document_chunks(chunk_index);
CREATE INDEX IF NOT EXISTS idx_chunks_created_at ON document_chunks(created_at);

-- Índice vectorial para búsqueda semántica
CREATE INDEX IF NOT EXISTS idx_chunks_embedding ON document_chunks 
USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

CREATE INDEX IF NOT EXISTS idx_qa_history_created ON qa_history(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_qa_history_rating ON qa_history(feedback_rating);

-- Función para actualizar timestamp de updated_at automáticamente
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger para actualizar updated_at en la tabla documents
DROP TRIGGER IF EXISTS update_documents_updated_at ON documents;
CREATE TRIGGER update_documents_updated_at 
    BEFORE UPDATE ON documents 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Vistas útiles para reportes
CREATE OR REPLACE VIEW document_stats AS
SELECT 
    status,
    COUNT(*) as document_count,
    SUM(file_size) as total_size_bytes,
    AVG(file_size) as avg_size_bytes,
    MIN(upload_date) as oldest_upload,
    MAX(upload_date) as newest_upload
FROM documents
GROUP BY status;

CREATE OR REPLACE VIEW qa_stats AS
SELECT 
    DATE(created_at) as date,
    COUNT(*) as total_questions,
    AVG(response_time_ms) as avg_response_time_ms,
    COUNT(CASE WHEN feedback_rating IS NOT NULL THEN 1 END) as questions_with_feedback,
    AVG(feedback_rating) as avg_rating
FROM qa_history
GROUP BY DATE(created_at)
ORDER BY date DESC;

-- Datos de ejemplo (opcional para testing)
-- COMMENT: Descomenta las siguientes líneas para insertar datos de prueba

-- INSERT INTO documents (filename, file_size, status, upload_date) VALUES 
-- ('test_document.pdf', 1024000, 'COMPLETED', CURRENT_TIMESTAMP - INTERVAL '1 day'),
-- ('legal_code.pdf', 2048000, 'PROCESSING', CURRENT_TIMESTAMP - INTERVAL '2 hours');

-- Comentarios explicativos
COMMENT ON TABLE documents IS 'Tabla principal que almacena la información de los documentos PDF cargados';
COMMENT ON TABLE document_chunks IS 'Tabla que almacena los fragmentos de texto extraídos de los documentos con sus embeddings';
COMMENT ON TABLE qa_history IS 'Tabla que almacena el historial de preguntas y respuestas del sistema RAG';

COMMENT ON COLUMN document_chunks.embedding IS 'Vector de 384 dimensiones generado por el modelo all-MiniLM-L6-v2';
COMMENT ON COLUMN qa_history.sources IS 'JSON que contiene información sobre los chunks utilizados para generar la respuesta';

-- Verificar que las extensiones están instaladas correctamente
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
        RAISE EXCEPTION 'La extensión pgvector no está instalada. Instala con: CREATE EXTENSION vector;';
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'uuid-ossp') THEN
        RAISE EXCEPTION 'La extensión uuid-ossp no está instalada. Instala con: CREATE EXTENSION "uuid-ossp";';
    END IF;
    
    RAISE NOTICE 'Base de datos inicializada correctamente para el sistema RAG';
END $$;