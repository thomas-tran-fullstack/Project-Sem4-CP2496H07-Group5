# 🔧 Hướng dẫn sửa lỗi "Invalid email/username or password"

## ⚠️ Vấn đề
Không thể đăng nhập được dù nhập đúng tài khoản mẫu từ SQL vì các password hash trong database không khớp với SHA-256 hash được sử dụng bởi ứng dụng.

## 🔍 Nguyên nhân
Trong file `OnlineSupermarketDB.sql`, các password hashes được lưu là:
- `customer`: `b041c0aeb35bb0fa4aa668ca5a920b590196fdaf9a00eb852c9b7f4d123cc6d6`
- `admin`: `240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9`
- `staff`: `10176e7b7b24d317acfcf8d2064cfd2f24e154f7b5a96603077d5ef813d6a6b6`

Nhưng ứng dụng sử dụng SHA-256 hash, nên những hash này không khớp. Hash SHA-256 đúng của các password đó là:
- `customer`: `b6c45863875e34487ca3c155ed145efe12a74581e27befec5aa661b8ee8ca6dd`
- `admin`: `8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918`
- `staff`: `1562206543da764123c21bd524674f0a8aaf49c8a89744c97352fe677f7e4006`

## ✅ Cách sửa

### Tùy chọn 1: Tái tạo database (Khuyên dùng)

Nếu bạn chưa có dữ liệu quan trọng trong database:

1. Xóa database cũ
2. Chạy script SQL đã cập nhật `OnlineSupermarketDB.sql`
   ```bash
   sqlcmd -S localhost -U sa -P sa -i OnlineSupermarketDB.sql
   ```

### Tùy chọn 2: Update password hashes trong database hiện tại

Nếu bạn đã có dữ liệu quan trọng:

1. Mở SQL Server Management Studio (SSMS)
2. Kết nối đến server `localhost` với user `sa` password `sa`
3. Chạy script `fix_password_hashes.sql`
   ```sql
   USE OnlineSupermarketDB;
   GO
   
   UPDATE Users 
   SET PasswordHash = 'b6c45863875e34487ca3c155ed145efe12a74581e27befec5aa661b8ee8ca6dd'
   WHERE Username = 'customer';
   
   UPDATE Users 
   SET PasswordHash = '8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918'
   WHERE Username = 'admin';
   
   UPDATE Users 
   SET PasswordHash = '1562206543da764123c21bd524674f0a8aaf49c8a89744c97352fe677f7e4006'
   WHERE Username = 'staff';
   GO
   ```

## 📝 Tài khoản mẫu (sau khi sửa)

| Username | Password | Email | Role |
|----------|----------|-------|------|
| customer | customer | customer@ezmart.vn | CUSTOMER |
| admin | admin | admin@ezmart.vn | ADMIN |
| staff | staff | staff@ezmart.vn | STAFF |

## 🔐 Lưu ý bảo mật

**⚠️ CHỈ SỬ DỤNG NHỮNG PASSWORD NÀY CHO PHÁT TRIỂN/TESTING!**

Trong production, bạn nên:
1. Sử dụng password mạnh (random, độ dài >= 12 ký tự)
2. Sử dụng hashing algorithm an toàn như bcrypt, scrypt, Argon2
3. Không lưu trữ password dưới dạng plain text

## 🧪 Kiểm tra

Sau khi sửa, bạn có thể đăng nhập với:
- **Username**: `customer` hoặc `admin` hoặc `staff`
- **Password**: `customer` hoặc `admin` hoặc `staff` (theo username)

## 📚 Thêm tài khoản mới

Khi đăng ký tài khoản mới, ứng dụng sẽ tự động hash password sử dụng SHA-256, nên bạn không cần lo lắng.

---

**File liên quan:**
- `OnlineSupermarketDB.sql` - Đã được cập nhật với hash đúng
- `fix_password_hashes.sql` - Script để update hash cho database hiện tại
- `PasswordHashGenerator.java` - Utility để generate SHA-256 hash
