-- Fix missing Staffs data
USE OnlineSupermarketDB;
GO

-- Check if Staffs table has any data
SELECT 'Current Staffs Count:' as Info, COUNT(*) as [Count] FROM Staffs;
GO

-- Insert Staffs for existing STAFF users
INSERT INTO Staffs (UserID, FirstName, LastName, PhoneNumber, Department, HireDate)
SELECT UserID, 'Staff', Username, '0909111111', 'Customer Support', GETDATE()
FROM Users
WHERE Role = 'STAFF' 
  AND UserID NOT IN (SELECT UserID FROM Staffs)
GO

-- Verify
SELECT 'Staffs after insert:' as Info;
SELECT * FROM Staffs;
GO

-- Show all chat conversations
SELECT 'Chat Conversations:' as Info;
SELECT ConversationID, CustomerID, StaffID, Status, RequestStatus, CreatedAt FROM ChatConversations;
GO
