-- Final fix: Create complete test data for staff chat
USE OnlineSupermarketDB;
GO

PRINT '=== STEP 1: Verify Staffs table has data ===';
SELECT 'Staffs:' AS [Info];
SELECT StaffID, UserID, FirstName, LastName FROM Staffs;
GO

PRINT '';
PRINT '=== STEP 2: Clear old chat data ===';
DELETE FROM ChatMessages WHERE ConversationID IN (
    SELECT ConversationID FROM ChatConversations WHERE CreatedAt > DATEADD(HOUR, -2, GETDATE())
);
DELETE FROM ChatConversations WHERE CreatedAt > DATEADD(HOUR, -2, GETDATE());
PRINT 'Cleared old test data';
GO

PRINT '';
PRINT '=== STEP 3: Insert test conversation ===';
-- Insert conversation: Customer 1 (CustomerID=1, UserID=2) with Staff 1 (StaffID=1, UserID=9)
INSERT INTO ChatConversations (CustomerID, StaffID, Status, RequestStatus, CreatedAt, LastMessageAt)
VALUES (1, 1, 'ACTIVE', 'ACCEPTED', GETDATE(), GETDATE());

DECLARE @ConvID INT = SCOPE_IDENTITY();
PRINT 'Created Conversation ID: ' + CAST(@ConvID AS VARCHAR);

-- Insert test messages
INSERT INTO ChatMessages (ConversationID, SenderRole, SenderUserID, Content, SentAt, IsRead, MessageType)
VALUES
(@ConvID, 'CUSTOMER', 2, 'Hello, I need help', GETDATE(), 0, 'TEXT'),
(@ConvID, 'STAFF', 9, 'Hi, what can I help you with?', GETDATE(), 0, 'TEXT'),
(@ConvID, 'CUSTOMER', 2, 'Thank you!', GETDATE(), 0, 'TEXT');

PRINT 'Inserted 3 test messages';
GO

PRINT '';
PRINT '=== STEP 4: Verify final data ===';
PRINT 'ChatConversations:';
SELECT ConversationID, CustomerID, StaffID, Status, RequestStatus, CreatedAt FROM ChatConversations;

PRINT '';
PRINT 'ChatMessages (sample):';
SELECT TOP 10 ConversationID, SenderRole, SenderUserID, Content, SentAt FROM ChatMessages ORDER BY SentAt DESC;
GO

PRINT '';
PRINT '✓ Test data created successfully!';
PRINT 'Now reload the web app and:';
PRINT '1. Logout current user';
PRINT '2. Login as toan (staff) - should see the conversation';
PRINT '3. Login as bo (customer) - should see chat history';
