-- Cleanup script to ensure clean database initialization
-- This will prevent conflicts with existing vector indexes and views

-- Drop existing views that might prevent schema changes
DROP VIEW IF EXISTS document_stats CASCADE;

-- Drop existing indexes that might conflict
DROP INDEX IF EXISTS idx_document_chunks_embedding_cosine CASCADE;
DROP INDEX IF EXISTS idx_document_chunks_embedding CASCADE;

-- Drop existing vector columns that might have wrong types
ALTER TABLE IF EXISTS document_chunks DROP COLUMN IF EXISTS embedding CASCADE;

-- Create vector extension if it doesn't exist
CREATE EXTENSION IF NOT EXISTS vector;

-- Let Spring Boot handle the rest of the schema creation