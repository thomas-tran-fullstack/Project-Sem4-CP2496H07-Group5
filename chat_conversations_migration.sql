-- 1) cho phép StaffID null
ALTER TABLE dbo.ChatConversations
ALTER COLUMN StaffID INT NULL;

-- 2) bỏ unique(CustomerID, StaffID) để cho phép nhiều cuộc chat theo thời gian
ALTER TABLE dbo.ChatConversations
DROP CONSTRAINT UQ_ChatConversations;
   -- tên index có thể khác, bạn check SHOW INDEX trước
-- hoặc DROP INDEX <tên_unique_index> ;

-- 3) (optional) index tăng tốc
CREATE INDEX idx_chat_pending ON ChatConversations(RequestStatus, CreatedAt);
CREATE INDEX idx_chat_staff_accept ON ChatConversations(AcceptedStaffID, RequestStatus, LastMessageAt);
