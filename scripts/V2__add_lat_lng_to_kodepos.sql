-- Migration to add latitude and longitude to kode_pos_diy table
ALTER TABLE kode_pos_diy 
ADD COLUMN IF NOT EXISTS latitude DECIMAL(10, 8),
ADD COLUMN IF NOT EXISTS longitude DECIMAL(11, 8);

COMMENT ON COLUMN kode_pos_diy.latitude IS 'Coordinate Latitude (Decimal)';
COMMENT ON COLUMN kode_pos_diy.longitude IS 'Coordinate Longitude (Decimal)';
