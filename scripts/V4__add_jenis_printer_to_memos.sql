-- V4: Add jenis_printer column to memos table
-- This migration adds support for printer type selection on memos

ALTER TABLE memos
    ADD COLUMN IF NOT EXISTS jenis_printer VARCHAR(50);