-- Migration script: Update ChatConversations table for advanced chat management
-- Adds support for: pending requests, acceptance/rejection, exclusive assignment, auto-reject

USE OnlineSupermarketDB;
GO

-- Update ChatConversations table
IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('ChatConversations') AND name = 'RequestStatus')
BEGIN
    ALTER TABLE ChatConversations ADD RequestStatus NVARCHAR(20) DEFAULT 'ACCEPTED';
    -- RequestStatus: 'PENDING' (waiting for staff acceptance), 'ACCEPTED' (staff accepted), 'REJECTED' (all staff rejected), 'CLOSED' (chat ended)
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('ChatConversations') AND name = 'AcceptedStaffID')
BEGIN
    ALTER TABLE ChatConversations ADD AcceptedStaffID INT NULL;
    -- Which staff member accepted this conversation
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('ChatConversations') AND name = 'ChatStartTime')
BEGIN
    ALTER TABLE ChatConversations ADD ChatStartTime DATETIME2 NULL;
    -- When the staff accepted and chat actually started
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('ChatConversations') AND name = 'AutoRejectTime')
BEGIN
    ALTER TABLE ChatConversations ADD AutoRejectTime DATETIME2 NULL;
    -- When auto-reject timer will trigger (5 minutes after PENDING creation)
END
GO

IF NOT EXISTS (SELECT 1 FROM sys.columns WHERE object_id = OBJECT_ID('ChatConversations') AND name = 'RejectedStaffIDs')
BEGIN
    ALTER TABLE ChatConversations ADD RejectedStaffIDs NVARCHAR(MAX) NULL;
    -- Comma-separated StaffIDs that rejected this request
END
GO

-- Optional: Add index for finding pending requests
CREATE INDEX IX_ChatConversations_RequestStatus 
ON ChatConversations(RequestStatus, CreatedAt)
WHERE RequestStatus = 'PENDING';
GO

-- Create new table to track pending chat requests (for customer widget to show status)
IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = 'ChatRequests')
BEGIN
    CREATE TABLE ChatRequests (
        RequestID INT IDENTITY PRIMARY KEY,
        CustomerID INT NOT NULL,
        CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
        Status NVARCHAR(20) DEFAULT 'PENDING', -- PENDING, ACCEPTED, REJECTED, EXPIRED
        AssignedStaffID INT NULL,
        RejectionTime DATETIME2 NULL,
        CONSTRAINT FK_ChatRequests_Customer FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID)
    );
    
    CREATE INDEX IX_ChatRequests_Customer ON ChatRequests(CustomerID, Status);
END
GO

PRINT 'ChatConversations schema migration completed successfully!';
