-- Migration to add revised_from_id and revision_to_id columns to memos table
ALTER TABLE memos 
ADD COLUMN IF NOT EXISTS revised_from_id UUID,
ADD COLUMN IF NOT EXISTS revision_to_id UUID;

-- Add Foreign Key Constraints if they do not exist
-- In PostgreSQL we can use a DO block to prevent errors if constraints already exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_memos_revised_from' AND table_name = 'memos'
    ) THEN
        ALTER TABLE memos 
        ADD CONSTRAINT fk_memos_revised_from 
        FOREIGN KEY (revised_from_id) REFERENCES memos(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints 
        WHERE constraint_name = 'fk_memos_revision_to' AND table_name = 'memos'
    ) THEN
        ALTER TABLE memos 
        ADD CONSTRAINT fk_memos_revision_to 
        FOREIGN KEY (revision_to_id) REFERENCES memos(id);
    END IF;
END $$;

COMMENT ON COLUMN memos.revised_from_id IS 'Reference to the original memo being revised';
COMMENT ON COLUMN memos.revision_to_id IS 'Reference to the new memo replacement';
