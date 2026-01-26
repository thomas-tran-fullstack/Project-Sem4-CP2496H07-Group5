-- Create test chat conversation
USE OnlineSupermarketDB;
GO

-- Clear old test data
DELETE FROM ChatMessages WHERE ConversationID IN (
    SELECT ConversationID FROM ChatConversations 
    WHERE CreatedAt > '2026-01-20'
);

DELETE FROM ChatConversations 
WHERE CreatedAt > '2026-01-20';

GO

-- Insert test conversation
-- Customer 1 (UserID=2) với Staff 1 (UserID=9)
INSERT INTO ChatConversations (CustomerID, StaffID, Status, RequestStatus, CreatedAt, LastMessageAt)
VALUES
(1, 1, 'ACTIVE', 'ACCEPTED', GETDATE(), GETDATE());

-- Get the new conversation ID
DECLARE @ConvID INT = SCOPE_IDENTITY();

-- Insert test messages
INSERT INTO ChatMessages (ConversationID, SenderRole, SenderUserID, Content, SentAt, IsRead, MessageType)
VALUES
(@ConvID, 'CUSTOMER', 2, 'Xin chào, tôi cần hỗ trợ', GETDATE(), 0, 'TEXT'),
(@ConvID, 'STAFF', 9, 'Xin chào bạn, tôi sẽ giúp bạn', GETDATE(), 0, 'TEXT'),
(@ConvID, 'CUSTOMER', 2, 'Cảm ơn bạn!', GETDATE(), 0, 'TEXT');

GO

-- Verify
SELECT 'Chat Conversations:' AS Info;
SELECT ConversationID, CustomerID, StaffID, Status, RequestStatus FROM ChatConversations;

SELECT 'Chat Messages:' AS Info;
SELECT ConversationID, SenderRole, SenderUserID, Content, SentAt FROM ChatMessages ORDER BY SentAt;
GO
