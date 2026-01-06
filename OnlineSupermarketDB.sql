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
    FOREIGN KEY (ProductID) REFERENCES Products(ProductID),
    FOREIGN KEY (CustomerID) REFERENCES Customers(CustomerID)
);

CREATE TABLE Wishlists (
    WishlistID INT IDENTITY PRIMARY KEY,
    CustomerID INT,
    ProductID INT,
    CreatedAt DATETIME DEFAULT GETDATE()
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
    CreatedAt DATETIME DEFAULT GETDATE(),
    OfferID INT,
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

INSERT INTO Users (Username, PasswordHash, Email, Role)
VALUES 
('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'admin@ezmart.vn', 'ADMIN'),
('bo', '123456', 'bo@ezmart.vn', 'CUSTOMER'),
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

INSERT INTO Offers (OfferName, OfferType, DiscountValue, StartDate, EndDate)
VALUES
('Giảm giá sữa', 'DISCOUNT_PERCENT', 10, '2025-01-01', '2025-12-31');
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

INSERT INTO Reviews (ProductID, CustomerID, Rating, Comment)
VALUES
(1, 1, 5, N'Sữa ngon, dễ uống'),
(3, 1, 4, N'Sạch, giặt tốt');
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
