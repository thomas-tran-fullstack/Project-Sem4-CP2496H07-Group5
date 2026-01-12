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
    Role VARCHAR(20) CHECK (Role IN ('STAFF','STREAMER','CUSTOMER','ADMIN')),
    Status VARCHAR(20) DEFAULT 'ACTIVE',
    CreatedAt DATETIME DEFAULT GETDATE()
);

CREATE TABLE Roles (
    RoleID INT IDENTITY PRIMARY KEY,
    RoleName VARCHAR(50) UNIQUE
);

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
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
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
    -- House number / unit field (mapped by entity)
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
    CategoryName VARCHAR(100),
    Description TEXT,
    ImageURL VARCHAR(255),
    Status VARCHAR(20),
    CreatedAt DATETIME DEFAULT GETDATE()
);

CREATE TABLE Brands (
    BrandID INT IDENTITY PRIMARY KEY,
    BrandName VARCHAR(100),
    Description TEXT,
    Country VARCHAR(50),
    Email VARCHAR(100),
    Phone VARCHAR(20),
    Address VARCHAR(255),
    Website VARCHAR(255)
);

CREATE TABLE Products (
    ProductID INT IDENTITY PRIMARY KEY,
    CategoryID INT,
    BrandID INT,
    ProductName VARCHAR(100),
    Description TEXT,
    UnitPrice DECIMAL(10,2),
    StockQuantity INT,
    Status VARCHAR(20),
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
    ChangeReason VARCHAR(100),
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
    Status VARCHAR(20) CHECK (Status IN ('PENDING','PAID','SHIPPED','COMPLETED','CANCELLED')),
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
    Type VARCHAR(30), -- TOPUP, PAY, REFUND
    Status VARCHAR(20),
    CreatedAt DATETIME DEFAULT GETDATE()
);

CREATE TABLE Payments (
    PaymentID INT IDENTITY PRIMARY KEY,
    OrderID INT,
    PaymentMethod VARCHAR(50),
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
    PaymentMethod VARCHAR(50),
    PaymentDate DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (OrderID) REFERENCES Orders(OrderID),
    FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID)
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
    Status VARCHAR(20) DEFAULT 'PENDING',
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
    CustomerID INT,
    Content NVARCHAR(500),
    CreatedAt DATETIME DEFAULT GETDATE()
);

CREATE TABLE Offers (
    OfferID INT IDENTITY PRIMARY KEY,
    OfferName VARCHAR(100),
    Description TEXT,
    OfferType VARCHAR(30),
    DiscountValue INT,
    StartDate DATE,
    EndDate DATE,
    Status VARCHAR(20) DEFAULT 'ACTIVE',
    BannerImage VARCHAR(255),
    VoucherEnabled BIT
);

CREATE TABLE Vouchers (
    VoucherID INT IDENTITY PRIMARY KEY,
    VoucherCode VARCHAR(50),
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
    StoreName VARCHAR(100),
    Town VARCHAR(50),
    City VARCHAR(50),
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
    Action VARCHAR(100),
    ActionDate DATETIME DEFAULT GETDATE(),
    Description TEXT,
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);

/* =========================
   CMS - TRANG ĐỘNG
========================= */
CREATE TABLE Pages (
    PageID INT IDENTITY PRIMARY KEY,
    PageKey VARCHAR(100) UNIQUE,
    Title NVARCHAR(200),
    Content NVARCHAR(MAX),
    PageType VARCHAR(50), -- GUIDE, POLICY, ABOUT, HELP
    Status VARCHAR(20) DEFAULT 'ACTIVE',
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
    ImageURL VARCHAR(255),
    CreatedAt DATETIME DEFAULT GETDATE(),
    Status VARCHAR(20) DEFAULT 'ACTIVE'
);

/* =========================
   DASHBOARD & REPORT
========================= */
CREATE TABLE Reports (
    ReportID INT IDENTITY PRIMARY KEY,
    ReportType VARCHAR(50), -- SALES, ORDER, PRODUCT
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

CREATE TABLE CommunityPosts (
    PostID INT IDENTITY PRIMARY KEY,
    UserID INT,
    Content NVARCHAR(500),
    Status VARCHAR(20),
    CreatedAt DATETIME DEFAULT GETDATE()
);

/*=========================
   ADDITIONAL FEATURES

========================= */
CREATE TABLE Wishlists (
        WishlistID INT IDENTITY PRIMARY KEY,
        CustomerID INT NULL,
        Name NVARCHAR(200) DEFAULT 'My Wishlist',
        IsDefault BIT DEFAULT 0,
        CreatedAt DATETIME DEFAULT GETDATE(),
        UpdatedAt DATETIME DEFAULT GETDATE(),
        FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID)
    );

 CREATE TABLE WishlistItems (
        WishlistItemID INT IDENTITY PRIMARY KEY,
        WishlistID INT NOT NULL,
        ProductID INT NULL,
        Quantity INT DEFAULT 1,
        Note NVARCHAR(500) NULL,
        AddedAt DATETIME DEFAULT GETDATE(),
        FOREIGN KEY (WishlistID) REFERENCES Wishlists(WishlistID),
        FOREIGN KEY (ProductID) REFERENCES Products(ProductID)
    );


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
(3, 'Truong Van', 'Lam', 'Ho Chi Minh', 'Vietnam', '0909000002');
GO

INSERT INTO Admins (UserID, AdminLevel)
VALUES (1, 'SUPER_ADMIN');
GO

INSERT INTO Categories (CategoryName, Description, Status)
VALUES
('Food', 'Thực phẩm', 'ACTIVE'),
('Drink', 'Nước uống', 'ACTIVE'),
('Household', 'Đồ gia dụng', 'ACTIVE');
GO

INSERT INTO Brands (BrandName, Country)
VALUES
('Vinamilk', 'Vietnam'),
('TH True Milk', 'Vietnam'),
('Omo', 'Vietnam');
GO

INSERT INTO Products (CategoryID, BrandID, ProductName, UnitPrice, StockQuantity, Status)
VALUES
(1, 1, 'Sữa tươi Vinamilk 1L', 32000, 100, 'ACTIVE'),
(1, 2, 'Sữa TH True Milk 1L', 34000, 80, 'ACTIVE'),
(3, 3, 'Bột giặt Omo 3kg', 95000, 50, 'ACTIVE');
GO

INSERT INTO Offers (OfferName, OfferType, DiscountValue, StartDate, EndDate, Status, BannerImage, VoucherEnabled)
VALUES
('Giảm giá sữa', 'Percentage', 10, '2024-01-01', '2026-12-31', 'active', NULL, 0),
('Khuyến mãi đồ gia dụng', 'Fixed Amount', 50000, '2024-01-01', '2026-12-31', 'active', NULL, 1);
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

INSERT INTO Orders (CustomerID, TotalAmount, Status)
VALUES
(1, 159000, 'PAID');
GO

INSERT INTO OrderDetails (OrderID, ProductID, Quantity, UnitPrice, TotalPrice)
VALUES
(1, 1, 2, 32000, 64000),
(1, 3, 1, 95000, 95000);
GO

INSERT INTO Bills (OrderID, CustomerID, BillAmount, PaymentMethod)
VALUES
(1, 1, 159000, 'CREDIT_CARD');
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
