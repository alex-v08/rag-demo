-- H2 Database Schema for Tests

-- Documents table
CREATE TABLE IF NOT EXISTS documents (
    id UUID PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500),
    file_size BIGINT,
    content_hash VARCHAR(64) UNIQUE,
    upload_date TIMESTAMP NOT NULL,
    processing_started_at TIMESTAMP,
    processing_completed_at TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message CLOB,
    metadata CLOB
);

-- Document chunks table (without vector type)
CREATE TABLE IF NOT EXISTS document_chunks (
    id UUID PRIMARY KEY,
    document_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL,
    content CLOB NOT NULL,
    char_start INTEGER,
    char_end INTEGER,
    page_number INTEGER,
    embedding CLOB, -- Store as serialized string
    metadata CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chunks_document FOREIGN KEY (document_id) REFERENCES documents(id) ON DELETE CASCADE,
    CONSTRAINT uk_chunk_index UNIQUE(document_id, chunk_index)
);

-- QA History table
CREATE TABLE IF NOT EXISTS qa_history (
    id UUID PRIMARY KEY,
    question CLOB NOT NULL,
    answer CLOB NOT NULL,
    confidence_score DOUBLE,
    processing_time_ms INTEGER,
    context_used CLOB,
    sources_used CLOB,
    metadata CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_documents_status ON documents(status);
CREATE INDEX IF NOT EXISTS idx_documents_upload_date ON documents(upload_date);
CREATE INDEX IF NOT EXISTS idx_chunks_document_id ON document_chunks(document_id);
CREATE INDEX IF NOT EXISTS idx_chunks_chunk_index ON document_chunks(chunk_index);
CREATE INDEX IF NOT EXISTS idx_qa_created_at ON qa_history(created_at);