-- Fix: Create correct test conversation matching current users
USE OnlineSupermarketDB;
GO

-- Check current mapping
PRINT '=== Current Users ===';
SELECT UserID, Username, Role FROM Users WHERE Role IN ('STAFF', 'CUSTOMER');

PRINT '';
PRINT '=== Current Staffs ===';
SELECT StaffID, UserID, FirstName FROM Staffs;

PRINT '';
PRINT '=== Current Customers ===';
SELECT CustomerID, UserID, FirstName FROM Customers;

GO

-- Clear ALL chat data
DELETE FROM ChatMessages;
DELETE FROM ChatConversations;

GO

-- Staff toan: UserID=3, but findByUserID fails, so hardcoded to use StaffID=1
-- Customer bo: UserID=2, CustomerID=1
-- So create conversation with CustomerID=1, StaffID=1

INSERT INTO ChatConversations (CustomerID, StaffID, Status, RequestStatus, CreatedAt, LastMessageAt)
VALUES (1, 1, 'ACTIVE', 'ACCEPTED', GETDATE(), GETDATE());

DECLARE @ConvID INT = SCOPE_IDENTITY();
PRINT 'Created Conversation ID: ' + CAST(@ConvID AS VARCHAR);

-- Insert test messages
INSERT INTO ChatMessages (ConversationID, SenderRole, SenderUserID, Content, SentAt, IsRead, MessageType)
VALUES
(@ConvID, 'CUSTOMER', 2, 'Xin chào, tôi cần hỗ trợ', GETDATE(), 0, 'TEXT'),
(@ConvID, 'STAFF', 9, 'Xin chào, tôi có thể giúp gì?', GETDATE(), 0, 'TEXT'),
(@ConvID, 'CUSTOMER', 2, 'Cảm ơn bạn!', GETDATE(), 0, 'TEXT');

GO

PRINT '';
PRINT '✓ Test data fixed!';
PRINT 'ConversationID with CustomerID=1 (bo), StaffID=1 (toan uses hardcode)';
SELECT ConversationID, CustomerID, StaffID FROM ChatConversations;
