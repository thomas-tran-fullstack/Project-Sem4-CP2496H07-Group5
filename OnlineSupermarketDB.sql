CREATE DATABASE OnlineSupermarketDB;
GO
USE OnlineSupermarketDB;
GO
CREATE TABLE Users (
    UserID INT IDENTITY PRIMARY KEY,
    Username VARCHAR(50) UNIQUE NOT NULL,
    PasswordHash VARCHAR(255) NOT NULL,
    Email VARCHAR(100),
    Role VARCHAR(20) CHECK (Role IN ('GUEST','CUSTOMER','ADMIN')),
    Status VARCHAR(20) DEFAULT 'ACTIVE',
    CreatedAt DATETIME DEFAULT GETDATE()
);
CREATE TABLE Customers (
    CustomerID INT IDENTITY PRIMARY KEY,
    UserID INT NOT NULL,
    FirstName VARCHAR(50),
    MiddleName VARCHAR(50),
    LastName VARCHAR(50),
    Street VARCHAR(100),
    City VARCHAR(50),
    State VARCHAR(50),
    Country VARCHAR(50),
    HomePhone VARCHAR(20),
    MobilePhone VARCHAR(20),
    CreatedAt DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);

CREATE TABLE Admins (
    AdminID INT IDENTITY PRIMARY KEY,
    UserID INT NOT NULL,
    AdminLevel VARCHAR(20),
    CreatedAt DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
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
CREATE TABLE Categories (
    CategoryID INT IDENTITY PRIMARY KEY,
    CategoryName VARCHAR(100),
    Description TEXT,
    Status VARCHAR(20),
    CreatedAt DATETIME DEFAULT GETDATE()
);

CREATE TABLE Brands (
    BrandID INT IDENTITY PRIMARY KEY,
    BrandName VARCHAR(100),
    Description TEXT,
    Country VARCHAR(50)
);
CREATE TABLE Products (
    ProductID INT IDENTITY PRIMARY KEY,
    CategoryID INT,
    BrandID INT,
    ProductName VARCHAR(100),
    Description TEXT,
    UnitPrice DECIMAL(10,2),
    StockQuantity INT,
    DiscountPercent INT DEFAULT 0,
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
CREATE TABLE Offers (
    OfferID INT IDENTITY PRIMARY KEY,
    OfferName VARCHAR(100),
    OfferType VARCHAR(30),
    DiscountValue INT,
    StartDate DATE,
    EndDate DATE
);

CREATE TABLE ProductOffers (
    ProductOfferID INT IDENTITY PRIMARY KEY,
    ProductID INT,
    OfferID INT,
    FOREIGN KEY (ProductID) REFERENCES Products(ProductID),
    FOREIGN KEY (OfferID) REFERENCES Offers(OfferID)
);
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
CREATE TABLE AuditLogs (
    AuditID INT IDENTITY PRIMARY KEY,
    UserID INT,
    Action VARCHAR(100),
    ActionDate DATETIME DEFAULT GETDATE(),
    Description TEXT,
    FOREIGN KEY (UserID) REFERENCES Users(UserID)
);
