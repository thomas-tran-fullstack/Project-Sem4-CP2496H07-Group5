CREATE DATABASE OnlineSupermarketDB;
GO
USE OnlineSupermarketDB;
GO

/* =========================
   USER & ACCOUNT
========================= */
CREATE TABLE Users (
    UserID INT IDENTITY PRIMARY KEY,
    Username VARCHAR(50) UNIQUE NOT NULL,
    PasswordHash VARCHAR(255) NOT NULL,
    Email VARCHAR(100),
    Role VARCHAR(20) CHECK (Role IN ('STAFF','CUSTOMER','ADMIN')),
    Status VARCHAR(20) DEFAULT 'ACTIVE',
    LastOnlineAt DATETIME NULL,
    CreatedAt DATETIME DEFAULT GETDATE(),
    BanUntil DATETIME NULL,
    AvatarUrl NVARCHAR(500) NULL DEFAULT 'user.png'
);


CREATE TABLE Roles (
    RoleID INT IDENTITY PRIMARY KEY,
    RoleName VARCHAR(50) UNIQUE
);

/* =========================
   STAFF (like Customers but no address)
========================= */
CREATE TABLE Staffs (
    StaffID INT IDENTITY PRIMARY KEY,
    UserID INT UNIQUE NOT NULL,
    FirstName NVARCHAR(50) NULL,
    MiddleName NVARCHAR(50) NULL,
    LastName NVARCHAR(50) NULL,
    MobilePhone NVARCHAR(20) NULL,
    CreatedAt DATETIME DEFAULT GETDATE(),
    AvatarUrl NVARCHAR(500) NULL,
    Status NVARCHAR(20) DEFAULT 'ACTIVE',
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);
GO

CREATE TABLE Customers (
    CustomerID INT IDENTITY PRIMARY KEY,
    UserID INT UNIQUE,
    -- Use NVARCHAR for name fields to preserve Unicode/diacritics
    FirstName NVARCHAR(50),
    MiddleName NVARCHAR(50),
    LastName NVARCHAR(50),
    Street NVARCHAR(100),
    City NVARCHAR(50),
    State NVARCHAR(50),
    Country NVARCHAR(50),
    HomePhone NVARCHAR(20),
    Latitude DECIMAL(9,6) NULL,
    Longitude DECIMAL(9,6) NULL,
    MobilePhone NVARCHAR(20),
    CreatedAt DATETIME DEFAULT GETDATE(),
    AvatarUrl NVARCHAR(500) NULL,
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);

/* =========================
   1:1 CHAT (Staff <-> Customer)
========================= */
CREATE TABLE ChatConversations (
    ConversationID INT IDENTITY PRIMARY KEY,
    CustomerID INT NOT NULL,
    StaffID INT NOT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    Status NVARCHAR(20) DEFAULT 'ACTIVE',
    LastMessageAt DATETIME2 NULL,
    RequestStatus NVARCHAR(20) DEFAULT 'ACCEPTED',
    AcceptedStaffID INT NULL,
    ChatStartTime DATETIME2 NULL,
    AutoRejectTime DATETIME2 NULL,
    RejectedStaffIDs NVARCHAR(MAX) NULL,
    CONSTRAINT FK_ChatConversations_Customers FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID),
    CONSTRAINT FK_ChatConversations_Staffs FOREIGN KEY (StaffID) REFERENCES Staffs(StaffID),
    CONSTRAINT UQ_ChatConversations UNIQUE(CustomerID, StaffID)
);
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


CREATE TABLE ChatMessages (
    MessageID BIGINT IDENTITY PRIMARY KEY,
    ConversationID INT NOT NULL,
    SenderRole NVARCHAR(20) NOT NULL CHECK (SenderRole IN ('STAFF','CUSTOMER')),
    SenderUserID INT NOT NULL,
    Content NVARCHAR(2000) NOT NULL,
    SentAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    IsRead BIT NOT NULL DEFAULT 0,
    MessageType NVARCHAR(20) DEFAULT 'TEXT', -- TEXT, IMAGE, FILE
    AttachmentUrl NVARCHAR(500) NULL,
    CONSTRAINT FK_ChatMessages_Conversation FOREIGN KEY (ConversationID) REFERENCES ChatConversations(ConversationID),
    CONSTRAINT FK_ChatMessages_SenderUser FOREIGN KEY (SenderUserID) REFERENCES Users(UserID)
);
GO

CREATE INDEX IX_ChatMessages_Conversation_SentAt
ON ChatMessages(ConversationID, SentAt);
GO


CREATE TABLE Permissions (
    PermissionID INT IDENTITY PRIMARY KEY,
    PermissionKey VARCHAR(100),
    Description NVARCHAR(255)
);

CREATE TABLE RolePermissions (
    RoleID INT,
    PermissionID INT,
    PRIMARY KEY (RoleID, PermissionID),
    FOREIGN KEY (RoleID) REFERENCES Roles(RoleID),
    FOREIGN KEY (PermissionID) REFERENCES Permissions(PermissionID)
);


CREATE TABLE Admins (
    AdminID INT IDENTITY PRIMARY KEY,
    UserID INT UNIQUE,
    AdminLevel VARCHAR(30),
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);

CREATE TABLE Addresses (
    AddressID INT IDENTITY(1,1) PRIMARY KEY,
    Label NVARCHAR(50) NULL,
    Type NVARCHAR(20) NULL,
    Region NVARCHAR(50) NULL,
    Street NVARCHAR(100) NULL,
    House NVARCHAR(100) NULL,
    City NVARCHAR(50) NULL,
    State NVARCHAR(50) NULL,
    Country NVARCHAR(50) NULL,
    Latitude DECIMAL(9,6) NULL,
    Longitude DECIMAL(9,6) NULL,
    IsDefault BIT NULL,
    CreatedAt DATETIME NULL,
    CustomerID INT NOT NULL,
    CONSTRAINT FK_Addresses_Customers FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID)
);

CREATE TABLE PersistentLogins (
    ID INT IDENTITY(1,1) PRIMARY KEY,
    UserID INT NOT NULL,
    Selector NVARCHAR(64) UNIQUE NOT NULL,
    ValidatorHash NVARCHAR(255) NOT NULL,
    ExpiresAt DATETIME NOT NULL,
    CONSTRAINT FK_PersistentLogins_Users FOREIGN KEY (UserID) REFERENCES Users(UserID)
);

CREATE TABLE CreditCards (
    CardID INT IDENTITY PRIMARY KEY,
    CustomerID INT NOT NULL,
    CardNumber VARCHAR(20),
    CardExpiry VARCHAR(10),
    CardType VARCHAR(20),
    IsDefault BIT DEFAULT 0,
    FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID)
);

/* =========================
   PRODUCT & CATEGORY
========================= */
CREATE TABLE Categories (
    CategoryID INT IDENTITY PRIMARY KEY,
    CategoryName NVARCHAR(100),
    Description TEXT,
    ImageURL VARCHAR(255),
    Status VARCHAR(20),
    CreatedAt DATETIME DEFAULT GETDATE()
);

CREATE TABLE Brands (
    BrandID INT IDENTITY PRIMARY KEY,
    BrandName NVARCHAR(100),
    Description TEXT,
    Country NVARCHAR(50),
    Email NVARCHAR(100),
    Phone NVARCHAR(20),
    Address NVARCHAR(255),
    Website NVARCHAR(255)
);

CREATE TABLE Products (
    ProductID INT IDENTITY PRIMARY KEY,
    CategoryID INT,
    BrandID INT,
    ProductName NVARCHAR(100),
    Description NVARCHAR(MAX),
    UnitPrice DECIMAL(10,2),
    StockQuantity INT,
    Status NVARCHAR(20),
    CreatedAt DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (CategoryID) REFERENCES Categories(CategoryID),
    FOREIGN KEY (BrandID) REFERENCES Brands(BrandID)
);

CREATE TABLE ProductImages (
    ImageID INT IDENTITY PRIMARY KEY,
    ProductID INT,
    ImageURL VARCHAR(255),
    FOREIGN KEY (ProductID) REFERENCES Products(ProductID)
);

CREATE TABLE ProductPriceHistory (
    PriceHistoryID INT IDENTITY PRIMARY KEY,
    ProductID INT,
    OldPrice DECIMAL(12,2),
    NewPrice DECIMAL(12,2),
    ChangeReason NVARCHAR(100),
    ChangedBy INT,
    ChangedAt DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (ProductID) REFERENCES Products(ProductID),
    FOREIGN KEY (ChangedBy) REFERENCES Users(UserID)
);
/* =========================
   CART & ORDER
========================= */
CREATE TABLE Carts (
    CartID INT IDENTITY PRIMARY KEY,
    CustomerID INT,
    CreatedAt DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID)
);

CREATE TABLE CartItems (
    CartItemID INT IDENTITY PRIMARY KEY,
    CartID INT,
    ProductID INT,
    Quantity INT,
    UnitPrice DECIMAL(10,2),
    FOREIGN KEY (CartID) REFERENCES Carts(CartID),
    FOREIGN KEY (ProductID) REFERENCES Products(ProductID)
);

CREATE TABLE Orders (
    OrderID INT IDENTITY PRIMARY KEY,
    CustomerID INT,
    OrderDate DATETIME DEFAULT GETDATE(),
    TotalAmount DECIMAL(12,2),
    Status NVARCHAR(20) CHECK (Status IN ('NEW','WAITING_CONFIRM','CONFIRMED','SHIPPING','COMPLETED','CANCELLED')),
    PaymentMethod NVARCHAR(50),
    PaymentStatus NVARCHAR(50),
    ShippingMethod NVARCHAR(50),
    StockDeducted BIT DEFAULT 0,
    CancelReason NVARCHAR(500) NULL,
    CreatedAt DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID)
);

CREATE TABLE OrderDetails (
    OrderDetailID INT IDENTITY PRIMARY KEY,
    OrderID INT,
    ProductID INT,
    Quantity INT,
    UnitPrice DECIMAL(10,2),
    TotalPrice DECIMAL(12,2),
    CustomerName NVARCHAR(100),
    CustomerAddress NVARCHAR(255),
    CustomerPhone NVARCHAR(20),
    ProductName NVARCHAR(100),
    ProductImage NVARCHAR(255),
    FOREIGN KEY (OrderID) REFERENCES Orders(OrderID),
    FOREIGN KEY (ProductID) REFERENCES Products(ProductID)
);

CREATE TABLE OrderTracking (
    TrackingID INT IDENTITY PRIMARY KEY,
    OrderID INT,
    Status NVARCHAR(100),
    UpdatedAt DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (OrderID) REFERENCES Orders(OrderID)
);


CREATE TABLE Wallets (
    WalletID INT IDENTITY PRIMARY KEY,
    CustomerID INT UNIQUE,
    Balance DECIMAL(14,2) DEFAULT 0
);


CREATE TABLE WalletTransactions (
    TransactionID INT IDENTITY PRIMARY KEY,
    WalletID INT,
    Amount DECIMAL(14,2),
    Type NVARCHAR(30), -- TOPUP, PAY, REFUND
    Status NVARCHAR(20),
    CreatedAt DATETIME DEFAULT GETDATE()
);

CREATE TABLE Payments (
    PaymentID INT IDENTITY PRIMARY KEY,
    OrderID INT,
    PaymentMethod NVARCHAR(50),
    Amount DECIMAL(12,2),
    PaidAt DATETIME DEFAULT GETDATE()
);

CREATE TABLE Refunds (
    RefundID INT IDENTITY PRIMARY KEY,
    OrderID INT,
    Amount DECIMAL(12,2),
    Reason NVARCHAR(255),
    Status VARCHAR(20),
    CreatedAt DATETIME DEFAULT GETDATE()
);

CREATE TABLE Bills (
    BillID INT IDENTITY PRIMARY KEY,
    OrderID INT,
    CustomerID INT,
    BillAmount DECIMAL(12,2),
    PaymentMethod NVARCHAR(50),
    PaymentDate DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (OrderID) REFERENCES Orders(OrderID),
    FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID)
);

CREATE TABLE PaymentProof (
    ProofID INT IDENTITY PRIMARY KEY,
    OrderID INT,
    ImagePath NVARCHAR(255),
    Note NVARCHAR(500),
    TransactionID NVARCHAR(50),
    Status NVARCHAR(20),
    UploadedAt DATETIME DEFAULT GETDATE(),
    VerifiedAt DATETIME NULL,
    VerifiedBy INT NULL,
    FOREIGN KEY (OrderID) REFERENCES Orders(OrderID),
    FOREIGN KEY (VerifiedBy) REFERENCES Users(UserID)
);

/* =========================
   REVIEW & OFFER
========================= */
CREATE TABLE Reviews (
    ReviewID INT IDENTITY PRIMARY KEY,
    ProductID INT,
    CustomerID INT,
    Rating INT CHECK (Rating BETWEEN 1 AND 5),
    Comment TEXT,
    CreatedAt DATETIME DEFAULT GETDATE(),
    Status NVARCHAR(20) DEFAULT 'PENDING',
    ModeratorID INT NULL,
    ModeratorNote NVARCHAR(MAX) NULL,
    IsFlagged BIT DEFAULT 0,
    Reply NVARCHAR(MAX) NULL,
    ReplyAt DATETIME NULL,
    UpdatedAt DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (ProductID) REFERENCES Products(ProductID),
    FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID)
);

CREATE TABLE Feedbacks (
    FeedbackID INT IDENTITY PRIMARY KEY,
    CustomerID INT NOT NULL,
    Subject NVARCHAR(200) NOT NULL,
    Content NVARCHAR(MAX) NOT NULL,
    FeedbackType NVARCHAR(50) DEFAULT 'GENERAL',
    Rating INT NULL CHECK (Rating BETWEEN 1 AND 5),
    Status NVARCHAR(20) DEFAULT 'OPEN',
    Response NVARCHAR(MAX) NULL,
    RespondedAt DATETIME NULL,
    CreatedAt DATETIME DEFAULT GETDATE(),
    UpdatedAt DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Feedbacks_Customers FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID)
);
GO

CREATE TABLE Offers (
    OfferID INT IDENTITY PRIMARY KEY,
    OfferName NVARCHAR(100),
    Description NVARCHAR(MAX),
    OfferType NVARCHAR(30),
    DiscountValue INT,
    StartDate DATE,
    EndDate DATE,
    Status NVARCHAR(20) DEFAULT 'ACTIVE',
    BannerImage NVARCHAR(255),
    VoucherEnabled BIT
);

CREATE TABLE Vouchers (
    VoucherID INT IDENTITY PRIMARY KEY,
    VoucherCode NVARCHAR(50),
    IsUsed BIT DEFAULT 0,
    ExpiryDate DATE,
    DiscountValue DECIMAL(12,2),
    CreatedAt DATETIME DEFAULT GETDATE(),
    CustomerID INT,
    OfferID INT,
    FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID),
    FOREIGN KEY (OfferID) REFERENCES Offers(OfferID)
);

CREATE TABLE ProductOffers (
    ProductOfferID INT IDENTITY PRIMARY KEY,
    ProductID INT,
    OfferID INT,
    FOREIGN KEY (ProductID) REFERENCES Products(ProductID),
    FOREIGN KEY (OfferID) REFERENCES Offers(OfferID)
);

/* =========================
   STORE & INVENTORY
========================= */
CREATE TABLE Stores (
    StoreID INT IDENTITY PRIMARY KEY,
    StoreName NVARCHAR(100),
    Town NVARCHAR(50),
    City NVARCHAR(50),
    Latitude DECIMAL(9,6),
    Longitude DECIMAL(9,6)
);

CREATE TABLE StoreProducts (
    StoreProductID INT IDENTITY PRIMARY KEY,
    StoreID INT,
    ProductID INT,
    StockQuantity INT,
    FOREIGN KEY (StoreID) REFERENCES Stores(StoreID),
    FOREIGN KEY (ProductID) REFERENCES Products(ProductID)
);

/* =========================
   SYSTEM LOG
========================= */
CREATE TABLE AuditLogs (
    AuditID INT IDENTITY PRIMARY KEY,
    UserID INT,
    Action NVARCHAR(100),
    ActionDate DATETIME DEFAULT GETDATE(),
    Description TEXT,
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);

/* =========================
   CMS - TRANG ĐỘNG
========================= */
CREATE TABLE Pages (
    PageID INT IDENTITY PRIMARY KEY,
    PageKey NVARCHAR(100) UNIQUE,
    Title NVARCHAR(200),
    Content NVARCHAR(MAX),
    PageType NVARCHAR(50), -- GUIDE, POLICY, ABOUT, HELP
    Status NVARCHAR(20) DEFAULT 'ACTIVE',
    CreatedAt DATETIME DEFAULT GETDATE()
);

CREATE TABLE FAQs (
    FAQID INT IDENTITY PRIMARY KEY,
    Question NVARCHAR(255),
    Answer NVARCHAR(MAX),
    Status VARCHAR(20) DEFAULT 'ACTIVE',
    CreatedAt DATETIME DEFAULT GETDATE()
);

CREATE TABLE News (
    NewsID INT IDENTITY PRIMARY KEY,
    Title NVARCHAR(255),
    Content NVARCHAR(MAX),
    ImageURL NVARCHAR(255),
    CreatedAt DATETIME DEFAULT GETDATE(),
    Status NVARCHAR(20) DEFAULT 'ACTIVE'
);

/* =========================
   DASHBOARD & REPORT
========================= */
CREATE TABLE Reports (
    ReportID INT IDENTITY PRIMARY KEY,
    ReportType NVARCHAR(50), -- SALES, ORDER, PRODUCT
    FromDate DATE,
    ToDate DATE,
    GeneratedAt DATETIME DEFAULT GETDATE(),
    CreatedBy INT,
    FOREIGN KEY (CreatedBy) REFERENCES Users(UserID)
);

CREATE TABLE SupportTickets (
    TicketID INT IDENTITY PRIMARY KEY,
    CustomerID INT,
    Subject NVARCHAR(200),
    Status VARCHAR(20),
    CreatedAt DATETIME DEFAULT GETDATE()
);

/* =========================================================
   LIVESTREAM – CHAT – COMMUNITY
========================================================= */
CREATE TABLE Livestreams (
    StreamID INT IDENTITY PRIMARY KEY,
    StreamTitle NVARCHAR(200),
    StaffID INT,
    Status VARCHAR(20),
    StartedAt DATETIME
);

CREATE TABLE LiveProducts (
    StreamID INT,
    ProductID INT,
    PRIMARY KEY (StreamID, ProductID)
);

CREATE TABLE Chats (
    ChatID INT IDENTITY PRIMARY KEY,
    SenderID INT,
    ReceiverID INT,
    Message NVARCHAR(500),
    IsAI BIT DEFAULT 0,
    CreatedAt DATETIME DEFAULT GETDATE()
);


/* =====================================================
   WISHLIST (SINGLE VERSION - NO DUPLICATES)
===================================================== */
CREATE TABLE Wishlists (
    WishlistID INT IDENTITY(1,1) PRIMARY KEY,
    CustomerID INT NOT NULL,
    Name NVARCHAR(200) DEFAULT N'My Wishlist',
    IsDefault BIT DEFAULT 0,
    CreatedAt DATETIME DEFAULT GETDATE(),
    UpdatedAt DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_Wishlists_Customers FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID) ON DELETE CASCADE
);
GO

CREATE TABLE WishlistItems (
    WishlistItemID INT IDENTITY(1,1) PRIMARY KEY,
    WishlistID INT NOT NULL,
    ProductID INT NOT NULL,
    Quantity INT DEFAULT 1,
    Note NVARCHAR(500) NULL,
    AddedAt DATETIME DEFAULT GETDATE(),
    CONSTRAINT FK_WishlistItems_Wishlists FOREIGN KEY (WishlistID) REFERENCES Wishlists(WishlistID) ON DELETE CASCADE,
    CONSTRAINT FK_WishlistItems_Products FOREIGN KEY (ProductID) REFERENCES Products(ProductID) ON DELETE CASCADE,
    CONSTRAINT UX_WishlistItems_Unique UNIQUE (WishlistID, ProductID)
);
GO

CREATE INDEX IX_Wishlists_CustomerID ON Wishlists(CustomerID);
GO

CREATE INDEX IX_WishlistItems_WishlistID ON WishlistItems(WishlistID);
GO

CREATE INDEX IX_WishlistItems_ProductID ON WishlistItems(ProductID);
GO

CREATE INDEX IX_WishlistItems_AddedAt ON WishlistItems(AddedAt DESC);
GO

CREATE VIEW vw_WishlistDetails AS
SELECT 
    wi.WishlistItemID,
    wi.WishlistID,
    w.CustomerID,
    c.FirstName,
    c.LastName,
    p.ProductID,
    p.ProductName,
    p.UnitPrice,
    p.StockQuantity,
    wi.Quantity,
    (p.UnitPrice * wi.Quantity) AS TotalPrice,
    wi.Note,
    wi.AddedAt,
    w.CreatedAt AS WishlistCreatedAt,
    w.UpdatedAt AS WishlistUpdatedAt
FROM WishlistItems wi
INNER JOIN Wishlists w ON wi.WishlistID = w.WishlistID
INNER JOIN Customers c ON w.CustomerID = c.CustomerID
INNER JOIN Products p ON wi.ProductID = p.ProductID;
GO

/* =====================================================
   WISHLIST STORED PROCEDURES
===================================================== */
CREATE PROCEDURE sp_GetWishlistItemCount
    @CustomerID INT
AS
BEGIN
    SELECT COUNT(*) AS ItemCount
    FROM WishlistItems wi
    INNER JOIN Wishlists w ON wi.WishlistID = w.WishlistID
    WHERE w.CustomerID = @CustomerID;
END;
GO

CREATE PROCEDURE sp_AddToWishlist
    @CustomerID INT,
    @ProductID INT,
    @Quantity INT = 1,
    @Note NVARCHAR(500) = NULL
AS
BEGIN
    DECLARE @WishlistID INT;
    
    IF @Quantity IS NULL OR @Quantity < 1 SET @Quantity = 1;
    
    SELECT TOP 1 @WishlistID = WishlistID
    FROM Wishlists
    WHERE CustomerID = @CustomerID AND IsDefault = 1;
    
    IF @WishlistID IS NULL
    BEGIN
        INSERT INTO Wishlists(CustomerID, Name, IsDefault, CreatedAt, UpdatedAt)
        VALUES (@CustomerID, N'My Wishlist', 1, GETDATE(), GETDATE());
        SET @WishlistID = SCOPE_IDENTITY();
    END
    
    IF EXISTS (SELECT 1 FROM WishlistItems WHERE WishlistID = @WishlistID AND ProductID = @ProductID)
    BEGIN
        UPDATE WishlistItems
        SET Quantity = Quantity + @Quantity,
            Note = COALESCE(@Note, Note),
            AddedAt = GETDATE()
        WHERE WishlistID = @WishlistID AND ProductID = @ProductID;
    END
    ELSE
    BEGIN
        INSERT INTO WishlistItems(WishlistID, ProductID, Quantity, Note, AddedAt)
        VALUES (@WishlistID, @ProductID, @Quantity, @Note, GETDATE());
    END
END;
GO

CREATE PROCEDURE sp_RemoveFromWishlist
    @CustomerID INT,
    @ProductID INT
AS
BEGIN
    DELETE FROM WishlistItems
    WHERE WishlistID IN (
        SELECT WishlistID FROM Wishlists WHERE CustomerID = @CustomerID AND IsDefault = 1
    )
    AND ProductID = @ProductID;
END;
GO

CREATE PROCEDURE sp_IsProductInWishlist
    @CustomerID INT,
    @ProductID INT
AS
BEGIN
    SELECT CASE 
        WHEN EXISTS (
            SELECT 1 FROM WishlistItems wi
            INNER JOIN Wishlists w ON wi.WishlistID = w.WishlistID
            WHERE w.CustomerID = @CustomerID AND wi.ProductID = @ProductID
        ) THEN 1 
        ELSE 0 
    END AS IsInWishlist;
END;
GO

CREATE PROCEDURE sp_GetCustomerWishlists
    @CustomerID INT
AS
BEGIN
    SELECT 
        w.WishlistID,
        w.CustomerID,
        w.Name,
        w.IsDefault,
        w.CreatedAt,
        w.UpdatedAt,
        COUNT(wi.WishlistItemID) AS ItemCount,
        COALESCE(SUM(p.UnitPrice * wi.Quantity), 0) AS TotalValue
    FROM Wishlists w
    LEFT JOIN WishlistItems wi ON w.WishlistID = wi.WishlistID
    LEFT JOIN Products p ON wi.ProductID = p.ProductID
    WHERE w.CustomerID = @CustomerID
    GROUP BY w.WishlistID, w.CustomerID, w.Name, w.IsDefault, w.CreatedAt, w.UpdatedAt
    ORDER BY w.IsDefault DESC, w.CreatedAt DESC;
END;
GO

CREATE PROCEDURE sp_GetWishlistItems
    @WishlistID INT
AS
BEGIN
    SELECT 
        wi.WishlistItemID,
        wi.ProductID,
        p.ProductName,
        p.UnitPrice,
        p.StockQuantity,
        wi.Quantity,
        (p.UnitPrice * wi.Quantity) AS TotalPrice,
        wi.Note,
        wi.AddedAt,
        CASE WHEN p.StockQuantity > 0 THEN 1 ELSE 0 END AS IsAvailable
    FROM WishlistItems wi
    INNER JOIN Products p ON wi.ProductID = p.ProductID
    WHERE wi.WishlistID = @WishlistID
    ORDER BY wi.AddedAt DESC;
END;
GO

CREATE PROCEDURE sp_GetWishlistSummary
    @WishlistID INT
AS
BEGIN
    SELECT 
        w.WishlistID,
        w.CustomerID,
        w.Name,
        w.IsDefault,
        w.CreatedAt,
        w.UpdatedAt,
        COUNT(wi.WishlistItemID) AS ItemCount,
        COALESCE(SUM(p.UnitPrice * wi.Quantity), 0) AS TotalValue,
        COALESCE(SUM(CASE WHEN p.StockQuantity > 0 THEN 1 ELSE 0 END), 0) AS AvailableItems
    FROM Wishlists w
    LEFT JOIN WishlistItems wi ON w.WishlistID = wi.WishlistID
    LEFT JOIN Products p ON wi.ProductID = p.ProductID
    WHERE w.WishlistID = @WishlistID
    GROUP BY w.WishlistID, w.CustomerID, w.Name, w.IsDefault, w.CreatedAt, w.UpdatedAt;
END;
GO

CREATE PROCEDURE sp_ClearWishlist
    @WishlistID INT
AS
BEGIN
    DELETE FROM WishlistItems WHERE WishlistID = @WishlistID;
    UPDATE Wishlists SET UpdatedAt = GETDATE() WHERE WishlistID = @WishlistID;
END;
GO

CREATE PROCEDURE sp_DeleteWishlist
    @WishlistID INT
AS
BEGIN
    DELETE FROM WishlistItems WHERE WishlistID = @WishlistID;
    DELETE FROM Wishlists WHERE WishlistID = @WishlistID;
END;
GO

CREATE PROCEDURE sp_CreateWishlist
    @CustomerID INT,
    @Name NVARCHAR(200) = N'My Wishlist',
    @IsDefault BIT = 0
AS
BEGIN
    INSERT INTO Wishlists(CustomerID, Name, IsDefault, CreatedAt, UpdatedAt)
    VALUES (@CustomerID, @Name, @IsDefault, GETDATE(), GETDATE());
    SELECT SCOPE_IDENTITY() AS WishlistID;
END;
GO

CREATE TRIGGER tr_UpdateWishlistTimestamp
ON WishlistItems
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    UPDATE Wishlists
    SET UpdatedAt = GETDATE()
    WHERE WishlistID IN (
        SELECT DISTINCT WishlistID FROM INSERTED
        UNION
        SELECT DISTINCT WishlistID FROM DELETED
    );
END;
GO



/* =========================
   VIDEO COOKING GUIDES
   (merged from database/videocooking_guides.sql, converted to T-SQL)
========================= */
CREATE TABLE VideoCookingGuides (
    VideoID INT IDENTITY PRIMARY KEY,
    Title NVARCHAR(255) NOT NULL,
    Description NVARCHAR(MAX),
    ThumbnailURL NVARCHAR(500),
    VideoURL NVARCHAR(500),
    DurationSeconds INT NULL,
    DurationText VARCHAR(32) NULL,
    Difficulty VARCHAR(10) NULL,
    Calories INT NULL,
    Category VARCHAR(100) NULL,
    Tags NVARCHAR(255) NULL,
    Ingredients NVARCHAR(MAX) NULL,
    IsFeatured BIT DEFAULT 0,
    Views BIGINT DEFAULT 0,
    Likes INT DEFAULT 0, 
    CreatedAt DATETIME DEFAULT GETDATE(),
    UpdatedAt DATETIME DEFAULT GETDATE()
);

CREATE TABLE UserSpendingSummary (
        UserSpendingID INT IDENTITY PRIMARY KEY,
        CustomerID INT NULL,
        PeriodType VARCHAR(10) NULL, -- WEEK, MONTH, YEAR
        PeriodStart DATE NULL,
        PeriodEnd DATE NULL,
        TotalSpent DECIMAL(12,2) DEFAULT 0,
        TransactionsCount INT DEFAULT 0,
        AverageOrderValue DECIMAL(12,2) DEFAULT 0,
        LoyaltyPointsEarned INT DEFAULT 0,
        EstimatedSavings DECIMAL(12,2) DEFAULT 0,
        CreatedAt DATETIME DEFAULT GETDATE(),
        UpdatedAt DATETIME DEFAULT GETDATE(),
        FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID)
    );

CREATE TABLE UserLoyalty (
        LoyaltyID INT IDENTITY PRIMARY KEY,
        CustomerID INT NOT NULL,
        PointsBalance INT DEFAULT 0,
        PointsEarnedLifetime INT DEFAULT 0,
        LastUpdated DATETIME DEFAULT GETDATE(),
        FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID)
    );

CREATE TABLE FrequentPurchases (
        FrequentID INT IDENTITY PRIMARY KEY,
        CustomerID INT NULL,
        ProductID INT NULL,
        PurchaseCount INT DEFAULT 0,
        LastPurchasedAt DATETIME NULL,
        FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID),
        FOREIGN KEY (ProductID) REFERENCES Products(ProductID)
    );




/* =========================================================
   BLOG / COMMUNITY POSTS
========================================================= */
CREATE TABLE CommunityPosts (
    PostID BIGINT IDENTITY PRIMARY KEY,
    CustomerID INT NULL,
    AuthorUserID INT NOT NULL,
    Title NVARCHAR(200) NOT NULL,
    Slug NVARCHAR(220) NULL UNIQUE,
    Content NVARCHAR(MAX) NOT NULL,
    CoverImageUrl NVARCHAR(500) NULL,
    MediaUrls NVARCHAR(MAX) NULL,
    Status NVARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (Status IN ('PENDING','APPROVED','REJECTED','HIDDEN','DELETED')),
    SubmittedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    ApprovedAt DATETIME2 NULL,
    ApprovedByUserID INT NULL,
    RejectedAt DATETIME2 NULL,
    RejectedByUserID INT NULL,
    RejectReason NVARCHAR(500) NULL,
    Visibility NVARCHAR(20) NOT NULL DEFAULT 'PUBLIC'
        CHECK (Visibility IN ('PUBLIC','FRIENDS','PRIVATE')),
    IsPinned BIT NOT NULL DEFAULT 0,
    PinnedAt DATETIME2 NULL,
    ViewCount BIGINT NOT NULL DEFAULT 0,
    LikeCount BIGINT NOT NULL DEFAULT 0,
    CommentCount BIGINT NOT NULL DEFAULT 0,
    UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_CommunityPosts_Customers FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID),
    CONSTRAINT FK_CommunityPosts_AuthorUser FOREIGN KEY (AuthorUserID) REFERENCES Users(UserID),
    CONSTRAINT FK_CommunityPosts_ApprovedBy FOREIGN KEY (ApprovedByUserID) REFERENCES Users(UserID),
    CONSTRAINT FK_CommunityPosts_RejectedBy FOREIGN KEY (RejectedByUserID) REFERENCES Users(UserID)
);
GO

CREATE INDEX IX_CommunityPosts_Status_SubmittedAt
ON CommunityPosts(Status, SubmittedAt);
GO

CREATE TABLE PostComments (
    CommentID BIGINT IDENTITY PRIMARY KEY,
    PostID BIGINT NOT NULL,
    AuthorUserID INT NOT NULL,
    CustomerID INT NULL,
    Content NVARCHAR(MAX) NOT NULL,
    LikeCount BIGINT NOT NULL DEFAULT 0,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    IsEdited BIT NOT NULL DEFAULT 0,
    IsDeleted BIT NOT NULL DEFAULT 0,
    CONSTRAINT FK_PostComments_CommunityPosts FOREIGN KEY (PostID) REFERENCES CommunityPosts(PostID) ON DELETE NO ACTION,
    CONSTRAINT FK_PostComments_AuthorUser FOREIGN KEY (AuthorUserID) REFERENCES Users(UserID),
    CONSTRAINT FK_PostComments_Customer FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID)
);
GO

CREATE TABLE PostLikes (
    LikeID BIGINT IDENTITY PRIMARY KEY,
    PostID BIGINT NOT NULL,
    UserID INT NOT NULL,
    CustomerID INT NULL,
    ReactionType VARCHAR(20) DEFAULT 'LIKE' CHECK (ReactionType IN ('LIKE','LOVE','HAHA','WOW','SAD','ANGRY')),
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_PostLikes_CommunityPosts FOREIGN KEY (PostID) REFERENCES CommunityPosts(PostID) ON DELETE NO ACTION,
    CONSTRAINT FK_PostLikes_User FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    CONSTRAINT FK_PostLikes_Customer FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID),
    CONSTRAINT UQ_PostLikes_User UNIQUE (PostID, UserID)
);
GO

CREATE TABLE CommentLikes (
    CommentLikeID BIGINT IDENTITY PRIMARY KEY,
    CommentID BIGINT NOT NULL,
    UserID INT NOT NULL,
    CustomerID INT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_CommentLikes_PostComments FOREIGN KEY (CommentID) REFERENCES PostComments(CommentID) ON DELETE NO ACTION,
    CONSTRAINT FK_CommentLikes_User FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    CONSTRAINT FK_CommentLikes_Customer FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID),
    CONSTRAINT UQ_CommentLikes_User UNIQUE (CommentID, UserID)
);
GO

CREATE TABLE PostShares (
    ShareID BIGINT IDENTITY PRIMARY KEY,
    PostID BIGINT NOT NULL,
    UserID INT NOT NULL,
    CustomerID INT NULL,
    ShareMessage NVARCHAR(MAX) NULL,
    SharedToFacebook BIT DEFAULT 0,
    SharedToTwitter BIT DEFAULT 0,
    SharedToWhatsApp BIT DEFAULT 0,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT FK_PostShares_CommunityPosts FOREIGN KEY (PostID) REFERENCES CommunityPosts(PostID) ON DELETE NO ACTION,
    CONSTRAINT FK_PostShares_User FOREIGN KEY (UserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    CONSTRAINT FK_PostShares_Customer FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID)
);
GO

CREATE TABLE UserFollows (
    FollowID BIGINT IDENTITY PRIMARY KEY,
    FollowerUserID INT NOT NULL,
    FollowerCustomerID INT NULL,
    FollowingUserID INT NOT NULL,
    FollowingCustomerID INT NULL,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    IsBlocked BIT DEFAULT 0,
    CONSTRAINT FK_UserFollows_FollowerUser FOREIGN KEY (FollowerUserID) REFERENCES Users(UserID) ON DELETE NO ACTION,
    CONSTRAINT FK_UserFollows_FollowerCustomer FOREIGN KEY (FollowerCustomerID) REFERENCES Customers(CustomerID),
    CONSTRAINT FK_UserFollows_FollowingUser FOREIGN KEY (FollowingUserID) REFERENCES Users(UserID) ON DELETE NO ACTION,
    CONSTRAINT FK_UserFollows_FollowingCustomer FOREIGN KEY (FollowingCustomerID) REFERENCES Customers(CustomerID),
    CONSTRAINT UQ_UserFollows UNIQUE (FollowerUserID, FollowingUserID)
);
GO

CREATE TABLE Notifications (
    NotificationID BIGINT IDENTITY PRIMARY KEY,
    RecipientUserID INT NOT NULL,
    ActorUserID INT NOT NULL,
    NotificationType VARCHAR(50) NOT NULL CHECK (NotificationType IN ('POST_LIKE','POST_COMMENT','COMMENT_LIKE','POST_SHARE','USER_FOLLOW','POST_APPROVED')),
    PostID BIGINT NULL,
    CommentID BIGINT NULL,
    Message NVARCHAR(500) NOT NULL,
    IsRead BIT NOT NULL DEFAULT 0,
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    ReadAt DATETIME2 NULL,
    CONSTRAINT FK_Notifications_RecipientUser FOREIGN KEY (RecipientUserID) REFERENCES Users(UserID) ON DELETE CASCADE,
    CONSTRAINT FK_Notifications_ActorUser FOREIGN KEY (ActorUserID) REFERENCES Users(UserID) ON DELETE NO ACTION,
    CONSTRAINT FK_Notifications_CommunityPosts FOREIGN KEY (PostID) REFERENCES CommunityPosts(PostID) ON DELETE NO ACTION,
    CONSTRAINT FK_Notifications_PostComments FOREIGN KEY (CommentID) REFERENCES PostComments(CommentID) ON DELETE NO ACTION
);
GO

CREATE INDEX IX_PostComments_PostID ON PostComments(PostID, CreatedAt DESC);
GO
CREATE INDEX IX_PostLikes_PostID ON PostLikes(PostID);
GO
CREATE INDEX IX_PostLikes_UserID ON PostLikes(UserID);
GO
CREATE INDEX IX_CommentLikes_CommentID ON CommentLikes(CommentID);
GO
CREATE INDEX IX_PostShares_PostID ON PostShares(PostID);
GO
CREATE INDEX IX_UserFollows_FollowerUserID ON UserFollows(FollowerUserID);
GO
CREATE INDEX IX_UserFollows_FollowingUserID ON UserFollows(FollowingUserID);
GO
CREATE INDEX IX_Notifications_RecipientUserID ON Notifications(RecipientUserID, IsRead, CreatedAt DESC);
GO

CREATE TABLE FeedbackTickets (
    TicketID BIGINT IDENTITY PRIMARY KEY,
    CustomerID INT NULL,
    SenderUserID INT NOT NULL,
    Subject NVARCHAR(200) NULL,
    Content NVARCHAR(MAX) NOT NULL,
    Type NVARCHAR(30) NOT NULL DEFAULT 'GENERAL'
        CHECK (Type IN ('GENERAL','BUG','FEATURE','COMPLAINT','PAYMENT','DELIVERY','REFUND','OTHER')),
    Priority NVARCHAR(10) NOT NULL DEFAULT 'NORMAL'
        CHECK (Priority IN ('LOW','NORMAL','HIGH','URGENT')),
    AttachmentUrl NVARCHAR(500) NULL,
    AttachmentUrls NVARCHAR(MAX) NULL,
    Status NVARCHAR(20) NOT NULL DEFAULT 'OPEN'
        CHECK (Status IN ('OPEN','IN_PROGRESS','WAITING_CUSTOMER','RESOLVED','REJECTED','CLOSED')),
    CreatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    UpdatedAt DATETIME2 NOT NULL DEFAULT SYSDATETIME(),
    AssignedToUserID INT NULL,
    AssignedAt DATETIME2 NULL,
    Response NVARCHAR(MAX) NULL,
    RespondedAt DATETIME2 NULL,
    ResolvedAt DATETIME2 NULL,
    ClosedAt DATETIME2 NULL,
    CloseNote NVARCHAR(500) NULL,
    CustomerRating INT NULL CHECK (CustomerRating BETWEEN 1 AND 5),
    CustomerNote NVARCHAR(500) NULL,
    CONSTRAINT FK_FeedbackTickets_Customers FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID),
    CONSTRAINT FK_FeedbackTickets_SenderUser FOREIGN KEY (SenderUserID) REFERENCES Users(UserID),
    CONSTRAINT FK_FeedbackTickets_AssignedToUser FOREIGN KEY (AssignedToUserID) REFERENCES Users(UserID)
);
GO

CREATE INDEX IX_FeedbackTickets_Status_CreatedAt
ON FeedbackTickets(Status, CreatedAt);
GO

-- Sample Data Insertions

INSERT INTO Users (Username, PasswordHash, Email, Role)
VALUES
('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'admin@ezmart.vn', 'ADMIN'),
('bo', '123456', 'bo@ezmart.vn', 'CUSTOMER'),
('toan', '123456', 'toan@ezmart.vn', 'STAFF'),
('lam', '123456', 'lam@ezmart.vn', 'CUSTOMER');
GO

INSERT INTO Customers (UserID, FirstName, LastName, City, Country, MobilePhone)
VALUES
(2, 'Pham Tan', 'Bo', 'Ho Chi Minh', 'Vietnam', '0909000001'),
(4, 'Truong Van', 'Lam', 'Ho Chi Minh', 'Vietnam', '0909000002');
GO

INSERT INTO Staffs (UserID, FirstName, LastName, PhoneNumber, Department, HireDate)
VALUES
(3, 'Nguyen Minh', 'Toan', '0909111111', 'Customer Support', GETDATE());
GO

INSERT INTO Admins (UserID, AdminLevel)
VALUES (1, 'SUPER_ADMIN');
GO

/* =========================
   FULL INSERT CATEGORIES
========================= */
INSERT INTO Categories (CategoryName, Description, Status)
VALUES
(N'Food',            N'Thực phẩm',            'Active'),
(N'Drink',           N'Nước uống',             'Active'),
(N'Household',       N'Đồ gia dụng',           'Active'),
(N'Fruit',           N'Trái cây tươi',         'Active'),
(N'Vegetable',       N'Rau củ tươi',           'Active'),
(N'Meat & Seafood',  N'Thịt cá hải sản',       'Active'),
(N'Snack',           N'Đồ ăn vặt',             'Active'),
(N'Personal Care',   N'Chăm sóc cá nhân',      'Active');
GO

/* =========================
   CATEGORY IMAGES (file name only)
========================= */

UPDATE Categories SET ImageURL = 'food.png'
WHERE CategoryName = N'Food';

UPDATE Categories SET ImageURL = 'drink.png'
WHERE CategoryName = N'Drink';

UPDATE Categories SET ImageURL = 'household.png'
WHERE CategoryName = N'Household';

UPDATE Categories SET ImageURL = 'fruit.png'
WHERE CategoryName = N'Fruit';

UPDATE Categories SET ImageURL = 'vegetable.png'
WHERE CategoryName = N'Vegetable';

UPDATE Categories SET ImageURL = 'meat-seafood.png'
WHERE CategoryName = N'Meat & Seafood';

UPDATE Categories SET ImageURL = 'snack.png'
WHERE CategoryName = N'Snack';

UPDATE Categories SET ImageURL = 'personal-care.png'
WHERE CategoryName = N'Personal Care';
GO

/* =========================
   FULL INSERT BRANDS
========================= */
INSERT INTO Brands (BrandName, Country, Email, Phone, Website)
VALUES
(N'Vinamilk',     N'Vietnam',      N'contact@vinamilk.com.vn', N'0280000001', N'https://www.vinamilk.com.vn'),
(N'TH True Milk', N'Vietnam',      N'contact@thtruemilk.vn',  N'0280000002', N'https://www.thmilk.vn'),
(N'Co.op Select', N'Vietnam',      N'contact@coopselect.vn',  N'0280000003', N'https://coopselect.vn'),
(N'Nestle',       N'Switzerland',  N'info@nestle.com',        N'+410000000', N'https://www.nestle.com'),
(N'Pepsi',        N'USA',          N'contact@pepsi.com',      N'+100000000', N'https://www.pepsi.com'),
(N'Heineken',     N'Netherlands',  N'info@heineken.com',      N'+310000000', N'https://www.theheinekencompany.com'),
(N'P&G',          N'USA',          N'contact@pg.com',         N'+100000001', N'https://us.pg.com'),
(N'Unilever',     N'UK',           N'contact@unilever.com',  N'+440000000', N'https://www.unilever.com');
GO

/* =========================
   FULL INSERT PRODUCTS
========================= */

INSERT INTO Products (CategoryID, BrandID, ProductName, Description, UnitPrice, StockQuantity, Status)
VALUES
/* ===== FOOD ===== */
((SELECT CategoryID FROM Categories WHERE CategoryName=N'Food'),
 (SELECT BrandID FROM Brands WHERE BrandName=N'Vinamilk'),
 N'Sữa tươi Vinamilk 1L', N'Sữa tươi tiệt trùng 1L.', 32, 150, 'Active'),

((SELECT CategoryID FROM Categories WHERE CategoryName=N'Food'),
 (SELECT BrandID FROM Brands WHERE BrandName=N'TH True Milk'),
 N'Sữa TH True Milk 1L', N'Sữa tươi nguyên chất.', 34, 120, 'Active'),

/* ===== FRUIT ===== */
((SELECT CategoryID FROM Categories WHERE CategoryName=N'Fruit'),
 (SELECT BrandID FROM Brands WHERE BrandName=N'Co.op Select'),
 N'Chuối Cavendish 1kg', N'Chuối tươi, phù hợp ăn liền.', 35, 100, 'Active'),

((SELECT CategoryID FROM Categories WHERE CategoryName=N'Fruit'),
 (SELECT BrandID FROM Brands WHERE BrandName=N'Co.op Select'),
 N'Táo đỏ 1kg', N'Táo giòn ngọt.', 120, 80, 'Active'),

((SELECT CategoryID FROM Categories WHERE CategoryName=N'Fruit'),
 (SELECT BrandID FROM Brands WHERE BrandName=N'Co.op Select'),
 N'Dâu tây 500g', N'Dâu tây tươi Đà Lạt.', 95, 60, 'Active'),

/* ===== VEGETABLE ===== */
((SELECT CategoryID FROM Categories WHERE CategoryName=N'Vegetable'),
 (SELECT BrandID FROM Brands WHERE BrandName=N'Co.op Select'),
 N'Rau muống 500g', N'Rau muống tươi.', 18, 200, 'Active'),

((SELECT CategoryID FROM Categories WHERE CategoryName=N'Vegetable'),
 (SELECT BrandID FROM Brands WHERE BrandName=N'Co.op Select'),
 N'Cà chua 1kg', N'Cà chua chín đỏ.', 45, 150, 'Active'),

((SELECT CategoryID FROM Categories WHERE CategoryName=N'Vegetable'),
 (SELECT BrandID FROM Brands WHERE BrandName=N'Co.op Select'),
 N'Cà rốt 1kg', N'Cà rốt tươi.', 32, 120, 'Active'),

/* ===== MEAT & SEAFOOD ===== */
((SELECT CategoryID FROM Categories WHERE CategoryName=N'Meat & Seafood'),
 (SELECT BrandID FROM Brands WHERE BrandName=N'Co.op Select'),
 N'Ức gà 500g', N'Ức gà tươi ít mỡ.', 95, 90, 'Active'),

((SELECT CategoryID FROM Categories WHERE CategoryName=N'Meat & Seafood'),
 (SELECT BrandID FROM Brands WHERE BrandName=N'Co.op Select'),
 N'Cá hồi phi lê 200g', N'Cá hồi tươi.', 320, 50, 'Active'),

((SELECT CategoryID FROM Categories WHERE CategoryName=N'Meat & Seafood'),
 (SELECT BrandID FROM Brands WHERE BrandName=N'Co.op Select'),
 N'Tôm thẻ 500g', N'Tôm thẻ tươi sống.', 210, 40, 'Active'),

/* ===== DRINK ===== */
((SELECT CategoryID FROM Categories WHERE CategoryName=N'Drink'),
 (SELECT BrandID FROM Brands WHERE BrandName=N'Pepsi'),
 N'Pepsi lon 330ml', N'Nước ngọt có ga.', 12, 400, 'Active'),

((SELECT CategoryID FROM Categories WHERE CategoryName=N'Drink'),
 (SELECT BrandID FROM Brands WHERE BrandName=N'Heineken'),
 N'Heineken lon 330ml', N'Bia Heineken lon.', 28, 300, 'Active'),

((SELECT CategoryID FROM Categories WHERE CategoryName=N'Drink'),
 (SELECT BrandID FROM Brands WHERE BrandName=N'Nestle'),
 N'Nước suối 500ml', N'Nước uống tinh khiết.', 6, 600, 'Active'),

/* ===== SNACK ===== */
((SELECT CategoryID FROM Categories WHERE CategoryName=N'Snack'),
 (SELECT BrandID FROM Brands WHERE BrandName=N'Nestle'),
 N'Socola thanh 40g', N'Socola ăn liền.', 15, 500, 'Active'),

((SELECT CategoryID FROM Categories WHERE CategoryName=N'Snack'),
 (SELECT BrandID FROM Brands WHERE BrandName=N'Nestle'),
 N'Bánh quy bơ 200g', N'Bánh quy bơ giòn.', 35, 250, 'Active'),

/* ===== HOUSEHOLD ===== */
((SELECT CategoryID FROM Categories WHERE CategoryName=N'Household'),
 (SELECT BrandID FROM Brands WHERE BrandName=N'Unilever'),
 N'Nước rửa chén 750ml', N'Nước rửa chén hương chanh.', 55, 180, 'Active'),

((SELECT CategoryID FROM Categories WHERE CategoryName=N'Household'),
 (SELECT BrandID FROM Brands WHERE BrandName=N'P&G'),
 N'Nước lau sàn 1L', N'Nước lau sàn diệt khuẩn.', 45, 140, 'Active'),

/* ===== PERSONAL CARE ===== */
((SELECT CategoryID FROM Categories WHERE CategoryName=N'Personal Care'),
 (SELECT BrandID FROM Brands WHERE BrandName=N'P&G'),
 N'Dầu gội 650ml', N'Dầu gội sạch gàu.', 120, 100, 'Active'),

((SELECT CategoryID FROM Categories WHERE CategoryName=N'Personal Care'),
 (SELECT BrandID FROM Brands WHERE BrandName=N'Unilever'),
 N'Sữa tắm 650g', N'Sữa tắm dưỡng ẩm.', 115, 95, 'Active'),

((SELECT CategoryID FROM Categories WHERE CategoryName=N'Personal Care'),
 (SELECT BrandID FROM Brands WHERE BrandName=N'P&G'),
 N'Kem đánh răng 100g', N'Kem đánh răng thơm mát.', 22, 220, 'Active');
GO

/* =========================================================
   FULL INSERT PRODUCT IMAGES
   (2–4 images per product)
========================================================= */

INSERT INTO ProductImages (ProductID, ImageURL)
SELECT p.ProductID, v.ImageURL
FROM Products p
JOIN (VALUES
/* ===== FOOD ===== */
(N'Sữa tươi Vinamilk 1L', '/resources/uploads/products/sua-tuoi-vinamilk-1l-1.jpg'),
(N'Sữa tươi Vinamilk 1L', '/resources/uploads/products/sua-tuoi-vinamilk-1l-2.png'),
(N'Sữa tươi Vinamilk 1L', '/resources/uploads/products/sua-tuoi-vinamilk-1l-3.jpg'),

(N'Sữa TH True Milk 1L', '/resources/uploads/products/sua-th-true-milk-1l-1.jpg'),
(N'Sữa TH True Milk 1L', '/resources/uploads/products/sua-th-true-milk-1l-2.png'),

/* ===== FRUIT ===== */
(N'Chuối Cavendish 1kg', '/resources/uploads/products/chuoi-cavendish-1kg-1.jpg'),
(N'Chuối Cavendish 1kg', '/resources/uploads/products/chuoi-cavendish-1kg-2.png'),
(N'Chuối Cavendish 1kg', '/resources/uploads/products/chuoi-cavendish-1kg-3.jpg'),

(N'Táo đỏ 1kg', '/resources/uploads/products/tao-do-1kg-1.jpg'),
(N'Táo đỏ 1kg', '/resources/uploads/products/tao-do-1kg-2.png'),

(N'Dâu tây 500g', '/resources/uploads/products/dau-tay-500g-1.jpg'),
(N'Dâu tây 500g', '/resources/uploads/products/dau-tay-500g-2.png'),
(N'Dâu tây 500g', '/resources/uploads/products/dau-tay-500g-3.jpg'),
(N'Dâu tây 500g', '/resources/uploads/products/dau-tay-500g-4.png'),

/* ===== VEGETABLE ===== */
(N'Rau muống 500g', '/resources/uploads/products/rau-muong-500g-1.jpg'),
(N'Rau muống 500g', '/resources/uploads/products/rau-muong-500g-2.png'),

(N'Cà chua 1kg', '/resources/uploads/products/ca-chua-1kg-1.jpg'),
(N'Cà chua 1kg', '/resources/uploads/products/ca-chua-1kg-2.png'),
(N'Cà chua 1kg', '/resources/uploads/products/ca-chua-1kg-3.jpg'),

(N'Cà rốt 1kg', '/resources/uploads/products/ca-rot-1kg-1.jpg'),
(N'Cà rốt 1kg', '/resources/uploads/products/ca-rot-1kg-2.png'),

/* ===== MEAT & SEAFOOD ===== */
(N'Ức gà 500g', '/resources/uploads/products/uc-ga-500g-1.jpg'),
(N'Ức gà 500g', '/resources/uploads/products/uc-ga-500g-2.png'),

(N'Cá hồi phi lê 200g', '/resources/uploads/products/ca-hoi-phile-200g-1.jpg'),
(N'Cá hồi phi lê 200g', '/resources/uploads/products/ca-hoi-phile-200g-2.png'),
(N'Cá hồi phi lê 200g', '/resources/uploads/products/ca-hoi-phile-200g-3.jpg'),

(N'Tôm thẻ 500g', '/resources/uploads/products/tom-the-500g-1.jpg'),
(N'Tôm thẻ 500g', '/resources/uploads/products/tom-the-500g-2.png'),
(N'Tôm thẻ 500g', '/resources/uploads/products/tom-the-500g-3.jpg'),

/* ===== DRINK ===== */
(N'Pepsi lon 330ml', '/resources/uploads/products/pepsi-330ml-1.jpg'),
(N'Pepsi lon 330ml', '/resources/uploads/products/pepsi-330ml-2.png'),

(N'Heineken lon 330ml', '/resources/uploads/products/heineken-330ml-1.jpg'),
(N'Heineken lon 330ml', '/resources/uploads/products/heineken-330ml-2.png'),
(N'Heineken lon 330ml', '/resources/uploads/products/heineken-330ml-3.jpg'),

(N'Nước suối 500ml', '/resources/uploads/products/nuoc-suoi-500ml-1.jpg'),
(N'Nước suối 500ml', '/resources/uploads/products/nuoc-suoi-500ml-2.png'),

/* ===== SNACK ===== */
(N'Socola thanh 40g', '/resources/uploads/products/socola-40g-1.jpg'),
(N'Socola thanh 40g', '/resources/uploads/products/socola-40g-2.png'),

(N'Bánh quy bơ 200g', '/resources/uploads/products/banh-quy-bo-200g-1.jpg'),
(N'Bánh quy bơ 200g', '/resources/uploads/products/banh-quy-bo-200g-2.png'),
(N'Bánh quy bơ 200g', '/resources/uploads/products/banh-quy-bo-200g-3.jpg'),

/* ===== HOUSEHOLD ===== */
(N'Nước rửa chén 750ml', '/resources/uploads/products/nuoc-rua-chen-750ml-1.jpg'),
(N'Nước rửa chén 750ml', '/resources/uploads/products/nuoc-rua-chen-750ml-2.png'),

(N'Nước lau sàn 1L', '/resources/uploads/products/nuoc-lau-san-1l-1.jpg'),
(N'Nước lau sàn 1L', '/resources/uploads/products/nuoc-lau-san-1l-2.png'),
(N'Nước lau sàn 1L', '/resources/uploads/products/nuoc-lau-san-1l-3.jpg'),

/* ===== PERSONAL CARE ===== */
(N'Dầu gội 650ml', '/resources/uploads/products/dau-goi-650ml-1.jpg'),
(N'Dầu gội 650ml', '/resources/uploads/products/dau-goi-650ml-2.png'),

(N'Sữa tắm 650g', '/resources/uploads/products/sua-tam-650g-1.jpg'),
(N'Sữa tắm 650g', '/resources/uploads/products/sua-tam-650g-2.png'),
(N'Sữa tắm 650g', '/resources/uploads/products/sua-tam-650g-3.jpg'),

(N'Kem đánh răng 100g', '/resources/uploads/products/kem-danh-rang-100g-1.jpg'),
(N'Kem đánh răng 100g', '/resources/uploads/products/kem-danh-rang-100g-2.png')
) AS v(ProductName, ImageURL)
ON p.ProductName = v.ProductName;
GO



INSERT INTO Offers (OfferName, OfferType, DiscountValue, StartDate, EndDate, Status, BannerImage, VoucherEnabled)
VALUES
(N'Giảm giá 10%', 'Percentage', 10, '2024-01-01', '2026-12-31', 'Active', 'banner1.jpg', 0),
(N'Khuyến mãi giảm 5$', 'Fixed Amount', 5, '2024-01-01', '2026-12-31', 'Active', 'banner2.jpg', 1);
GO

INSERT INTO ProductOffers (ProductID, OfferID)
VALUES
(1, 1),
(2, 1);
GO

INSERT INTO Carts (CustomerID)
VALUES (1);
GO

INSERT INTO CartItems (CartID, ProductID, Quantity, UnitPrice)
VALUES
(1, 1, 2, 32000),
(1, 3, 1, 95000);
GO


INSERT INTO Reviews (ProductID, CustomerID, Rating, Comment, CreatedAt, Status, IsFlagged)
VALUES
(1, 1, 5, N'The spinach was incredibly fresh and crisp. Delivery was faster than expected. Will definitely order again for my weekly meal prep!', GETDATE(), 'PUBLISHED', 0),
(2, 1, 3, N'Some strawberries were a bit bruised at the bottom of the box. Tastes good though.', GETDATE(), 'PENDING', 0),
(3, 2, 5, N'Perfect condition, very creamy. Makes great guacamole!', GETDATE(), 'PUBLISHED', 0),
(3, 2, 1, N'[Spam content removed by system] Visit my website for free coupons...', GETDATE(), 'REJECTED', 1);
GO

INSERT INTO Pages (PageKey, Title, Content, PageType)
VALUES
('guide', N'Hướng dẫn mua hàng', N'Các bước mua hàng trên EZMart...', 'GUIDE'),
('policy', N'Chính sách đơn hàng', N'Chính sách đổi trả và hoàn tiền...', 'POLICY'),
('about', N'Giới thiệu EZMart', N'EZMart là hệ thống siêu thị trực tuyến...', 'ABOUT');
GO

INSERT INTO FAQs (Question, Answer)
VALUES
(N'Làm sao để mua hàng?', N'Bạn cần đăng nhập và thêm sản phẩm vào giỏ hàng'),
(N'Có thanh toán online không?', N'Có, thanh toán qua thẻ');
GO

INSERT INTO News (Title, Content)
VALUES
(N'Khai trương EZMart Online', N'Giảm giá lớn cho tuần đầu ra mắt');
GO

INSERT INTO Reports (ReportType, FromDate, ToDate, CreatedBy)
VALUES
('SALES', '2025-01-01', '2025-12-31', 1);
GO

INSERT INTO UserLoyalty (CustomerID, PointsBalance, PointsEarnedLifetime)
VALUES
(1, 1250, 5120),
(2, 300, 890);

INSERT INTO UserSpendingSummary (CustomerID, PeriodType, PeriodStart, PeriodEnd, TotalSpent, TransactionsCount, AverageOrderValue, LoyaltyPointsEarned, EstimatedSavings)
VALUES
(1, 'MONTH', '2025-09-01', '2025-09-30', 4500000, 8, 562500, 120, 320000),
(2, 'MONTH', '2025-09-01', '2025-09-30', 1250000, 3, 416667, 30, 50000);

INSERT INTO FrequentPurchases (CustomerID, ProductID, PurchaseCount, LastPurchasedAt)
VALUES
(1, 1, 5, DATEADD(day, -3, GETDATE())),
(1, 2, 12, DATEADD(day, -7, GETDATE())),
(1, 3, 3, DATEADD(day, -10, GETDATE()));
GO

-- Sample data (from the videocooking_guides.sql file)
INSERT INTO VideoCookingGuides (Title, Description, ThumbnailURL, VideoURL, DurationSeconds, DurationText, Difficulty, Calories, Category, Tags, Ingredients, IsFeatured)
VALUES
(N'Grilled Salmon with Lemon Asparagus', N'Grilled salmon served with lemon asparagus - quick and delicious.', N'https://lh3.googleusercontent.com/aida-public/AB6AXuAFI5usTisj3SQ_i0ucZl7SZ5BtROx0syOd9sY9TDV_zGikFcHplmkU3hIB23Dd8HrrEbgnPMlKqEEgpddHr36iADLBDAFqQrNrhMnp7TSo3CJhOPoNZE-dsPgRwI6F3rRiAqnPKzmChIdONd0Q49_kOByV_8EValUX3xehsxCFwIkI_An1FMMmkGwII4Fj66P80cA6nM-jf-1Te3WlHzb5QeniJ7ejPAf44X-fgoYlBgTavDqFbp4GQZNk6bEbFoqmbH8antNe_gI', N'', 900, '15:00', 'Medium', 450, 'Dinner', N'salmon,asparagus,quick', N'salmon,asparagus,lemon,olive oil,salt,pepper', 0),
(N'Traditional Pho at Home: Step by Step', N'Make authentic Vietnamese Pho at home with rich broth and fresh herbs.', N'https://lh3.googleusercontent.com/aida-public/AB6AXuAuu-CTA15HgCRK0yLxaRZ64ji9GHcD5i5iZwPMjkY_vdZFKSYliKKlMUTvzN369kUf1hO2lWyXNZ_aUxwdhE4alZq-IRk2yxTx0EuErQSs4EeKg-UmG1c75DYxUqUJOGaBM31J_q5SpPmriFq7R_xQBzaHoH_RXsP_FGITS6wzjABfF0mVQRNqkomDpppszWhAg6OUo-bE64paEzox-XQMqdfWGGvlJRR2gdTacYC6PjmwpxzQEaiKnzI7QCOx8jmb1YrBHRRtonM', N'', 2730, '45:30', 'Hard', 520, 'Lunch', N'pho,noodles,broth', N'beef bones,ginger,onion,star anise,cinnamon,fish sauce,vermicelli', 0),
(N'5-Minute Creamy Avocado Smoothie', N'Quick and healthy avocado smoothie, perfect for breakfast.', N'https://lh3.googleusercontent.com/aida-public/AB6AXuAEAY9KL85OpZDTN_xxHQleaug7QGLUXU1apRs6PaOUwoJDJ2Yn7WEim5rhd719IFkx6bBBmPsgazxomGqSw4OrLG50clU9TyaItFtY3MKvrxVwRkjx_BxF_9m5LtApjCK7fqG4_pLnas65Z0CKmzLSweu34OldGL8Et03cDmtiZDrCbiMWbTba3YHcJHJf4W_lYlPHXc9OLI9khI6ikP5qrStvhDFGjxVoRpuGEX0zHG4FUwZnHrw9uzRDvPSjiSvLdhAX7Zig0tA', N'', 200, '03:20', 'Easy', 210, 'Breakfast', N'smoothie,avocado,quick', N'avocado,milk,honey,ice', 0),
(N'Spicy Beef Stir-Fry', N'Quick spicy beef stir-fry with vegetables.', N'https://lh3.googleusercontent.com/aida-public/AB6AXuCBmO5uvM64v9aKYE1sBi7YH-hRWQDGNDPPa7m141_NwleU8_LehV55Er6wYyb-5OZmzpyBhQmUly9y5yQ5ytkxL9LK_yCAkl1h6Mvr-xFRa6fGIwBA740RskncI4B2H0xENiOEwmq0bWGXjqzF-PuRNGlLZUOuS0rjrjN5yDz1giMsqWBgZIGs0hMEoVAPxdVzHbs77fO95gvpYimAShu1Niyt6YvEXHt9AyYKvvDJ8Mw0VQHI4O8upBLqrcQ70XP_xZbrR-Mzt9I', N'', 1200, '20:00', 'Medium', 380, 'Dinner', N'beef,stir-fry,spicy', N'beef,soy sauce,chili,vegetables', 0),
(N'The Ultimate Vegan Burger', N'Plant-based burger with all the fixings.', N'https://lh3.googleusercontent.com/aida-public/AB6AXuDCFxbzjnDnCJ49deK29RSdWePjKFFiynWpCHsB-y5FL8CNMHbNPyS-Mv1Z2ZA5mwz7CGkY1gtaNgIxfHZhcPfphLrSkajI_HdM9aDEegoxxznNg_6n7p64JYAexWK5YKjGklR0Hnu7bmigpysXufGIqSHJnOirBwlZpC0CuW6tNMg9hmQzq23PrSuq7rTTAZ4i7AWGvBBQU8jvrIIMPmzQaCz-ToY1wGz7Qyo61kULGKBog_0aXmJiWqg5lDXDUmG1vbwm5bJ5qwQ', N'', 1500, '25:00', 'Medium', 420, 'Lunch', N'vegan,burger,plant-based', N'vegan patty,bun,lettuce,tomato', 0),
(N'Classic Pasta Carbonara', N'Creamy carbonara with pancetta and parmesan.', N'https://lh3.googleusercontent.com/aida-public/AB6AXuBiKS8X6R3609hav3Bk0s0cgCqfZqpWhiBb7tKtEBR5N4t_TNehGsW6_vNtNA-eKUis1u5XA8fAyDkvy41z5j96Gvk9O8ngg-9VniyynczAIOT_jcxudz-8NVx_8PBUnBaiPRmkYwrmdHS1n6WbvNm5J6LCg-Z6dbU9g1K-3JqxDvb5Jyyt8SWU1H3Ou6MjajNA-Hp4k3GvYUbWTdQixLYyxYnIWV-UFHCkA9K6j7vgYoG-345R_Zl9BHlHb7YJABD3DIUOG9QDox0', N'', 1080, '18:00', 'Easy', 600, 'Dinner', N'pasta,carbonara', N'spaghetti,eggs,pancetta,parmesan', 0),
(N'Thai Mango Salad (Som Tum)', N'Tangy and spicy Thai mango salad.', N'https://lh3.googleusercontent.com/aida-public/AB6AXuD3qVIzN9SUbyDf4-IG_RL1TXguGr63xA7EXdnhG15QrERGmnEaO7Xr2Z6G5tGbXiyMwjYy8P7jl8odaDzDMiQ2LhJ1prAF1VydQ8BsJ_6LGiFkg_mVXke3ICPBxH1vMDv4NKFGNwwZQFLF9MhfIqo5t8wzOQnMYonOMVjXCVi8maEEGI8hJ56qKaII7D2gEMnkqxfXAP6OQFtTlPmPm9I5irr_8G7SK7SpOS2yI5YUThggnkPjxTr-HU5yL0kum3Q6ncFzs_MUyUA', N'', 600, '10:00', 'Easy', 150, 'Lunch', N'salad,mango,thai', N'mango,chili,fish sauce,lime', 0),
(N'Homemade Chicken Tikka Masala', N'Rich and creamy chicken tikka masala from scratch.', N'https://lh3.googleusercontent.com/aida-public/AB6AXuBYFW3LMxNz5GQrU8HQOk_NTMXRbFLINqU25JvId-TRKe94-lc7SKosliLemenGlvZeMUvY9YFJOfQcmGZffTqjMp0-mfoW9E214iCsElQMv7uz4TZNiH5Vm6ubH-CFtVEf9IiZkDhXqXJb4hNzP690odBjR0grCKB9mFavMn-yKJCxpiM_TAMkBVG6PhpNIvc7oolHQk5M5GcJpAnIKiiAYclooH1yFagn6zBRn4tlnO6j-Wh3l3r8bGXCZ3C3LHsa4DDzXU8zDJQ', N'', 2400, '40:00', 'Hard', 550, 'Dinner', N'curry,chicken,indian', N'chicken,yogurt,tomato,garam masala', 0);
GO
