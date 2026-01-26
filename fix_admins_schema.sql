-- Fix Admins table schema
USE OnlineSupermarketDB;
GO

-- Check current Admins structure
PRINT 'Current Admins table structure:';
EXEC sp_columns Admins;
GO

-- Add CreatedAt column if missing
IF NOT EXISTS (
    SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_NAME = 'Admins' AND COLUMN_NAME = 'CreatedAt'
)
BEGIN
    ALTER TABLE Admins ADD CreatedAt DATETIME DEFAULT GETDATE();
    PRINT 'Added CreatedAt column to Admins table';
END
ELSE
BEGIN
    PRINT 'CreatedAt column already exists in Admins table';
END

GO

-- Verify result
SELECT 'Admins table after fix:' AS Info;
SELECT * FROM Admins;
GO
