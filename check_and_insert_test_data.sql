-- Quick check database state
USE OnlineSupermarketDB;
GO

PRINT '=== DATABASE STATE ===';

PRINT 'Users (STAFF):';
SELECT UserID, Username, Role FROM Users WHERE Role = 'STAFF';

PRINT 'Staffs:';
SELECT StaffID, UserID, FirstName, LastName FROM Staffs;

PRINT 'Customers:';
SELECT CustomerID, UserID, FirstName, LastName FROM Customers;

PRINT 'ChatConversations:';
SELECT ConversationID, CustomerID, StaffID, Status, RequestStatus, CreatedAt FROM ChatConversations;

PRINT 'ChatMessages:';
SELECT ConversationID, SenderRole, SenderUserID, Content, SentAt FROM ChatMessages;

GO

PRINT '';
PRINT '=== INSERT TEST DATA ===';

-- Clear old test conversations
DELETE FROM ChatMessages WHERE ConversationID IN (
    SELECT ConversationID FROM ChatConversations WHERE CreatedAt > DATEADD(HOUR, -1, GETDATE())
);
DELETE FROM ChatConversations WHERE CreatedAt > DATEADD(HOUR, -1, GETDATE());

GO

-- Insert 1 test conversation
-- Assuming: Customer ID=1, Staff ID=1
INSERT INTO ChatConversations (CustomerID, StaffID, Status, RequestStatus, CreatedAt, LastMessageAt)
VALUES (1, 1, 'ACTIVE', 'ACCEPTED', GETDATE(), GETDATE());

DECLARE @ConvID INT = SCOPE_IDENTITY();
PRINT 'Created ConversationID: ' + CAST(@ConvID AS VARCHAR);

-- Add test messages
INSERT INTO ChatMessages (ConversationID, SenderRole, SenderUserID, Content, SentAt, IsRead, MessageType)
VALUES
(@ConvID, 'CUSTOMER', 2, 'Xin chào', GETDATE(), 0, 'TEXT'),
(@ConvID, 'STAFF', 9, 'Xin chào bạn', GETDATE(), 0, 'TEXT');

GO

PRINT '';
PRINT '=== RESULT ===';
PRINT 'ChatConversations after insert:';
SELECT ConversationID, CustomerID, StaffID, Status, RequestStatus FROM ChatConversations;

PRINT 'ChatMessages after insert:';
SELECT ConversationID, SenderRole, SenderUserID, Content FROM ChatMessages;
