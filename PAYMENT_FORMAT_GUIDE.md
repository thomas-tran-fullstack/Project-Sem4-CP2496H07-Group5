## 📝 Hướng Dẫn Thêm Payment Method - Format Đúng

### 🎯 Các Bước Thêm Payment Method Đúng

---

## 1️⃣ TAB VISA

### Nhập Thông Tin:
- **Cardholder Name**: `TRAN BAO TOAN` (chữ hoa, 3+ ký tự)
- **Card Number**: `4111 1111 1111 1111` (hoặc `4242 4242 4242 4242`)
  - Format tự động: Gõ 16 chữ số → tự thêm space
  - Ví dụ: `4111111111111111` → `4111 1111 1111 1111`
- **Expiry Date**: `12/26` (MM/YY format, **chưa hết hạn**)
  - ❌ **SAI**: `12/12` (đã hết hạn)
  - ❌ **SAI**: `13/25` (tháng không hợp lệ)
  - ✅ **ĐÚNG**: `12/26`, `01/27`, `05/28`, etc.
  - Gõ `1226` → tự format thành `12/26`
- **CVV**: `123` (3 chữ số)

### Validation:
- ✅ Card number: Phải pass Luhn algorithm check
- ✅ Expiry: Phải là tháng hợp lệ (01-12) và năm chưa hết hạn
- ✅ Cardholder: Tối thiểu 3 ký tự

### Test Card Numbers (Đã được validate):
```
✓ 4242 4242 4242 4242
✓ 4111 1111 1111 1111
✓ 5555 4444 3333 2222 (MasterCard)
```

---

## 2️⃣ TAB MASTERCARD

### Nhập Thông Tin:
- **Cardholder Name**: Tương tự Visa (3+ ký tự)
- **Card Number**: `5555 4444 3333 2222`
  - Format tự động giống Visa
- **Expiry Date**: Tương tự Visa (MM/YY, chưa hết hạn)
- **CVV**: 3 chữ số

### Validation:
- Giống Visa - phải pass Luhn algorithm

---

## 3️⃣ TAB PAYPAL

### Nhập Thông Tin:
- **PayPal Email**: `john.doe@gmail.com`
  - ❌ **SAI**: `john.doe` (không có @)
  - ❌ **SAI**: `john@.com` (không có tên)
  - ✅ **ĐÚNG**: `user@email.com`, `abc@example.com`
- **Display Name**: `My PayPal` (2+ ký tự, để tham khảo)

### Validation:
- ✅ Email format: `text@domain.com`
- ✅ Display name: 2+ ký tự

---

## 4️⃣ TAB MOMO

### Nhập Thông Tin:
- **Phone Number**: `+84912345678` hoặc `0912345678`
  - ❌ **SAI**: `84912345678` (thiếu + hoặc 0)
  - ❌ **SAI**: `+8491234567` (quá ít chữ số)
  - ✅ **ĐÚNG**: `+84912345678`, `0912345678`, `+84987654321`
  - Format: `+84` hoặc `0` + 9-10 chữ số
- **Display Name**: `My Momo` (2+ ký tự)

### Validation:
- ✅ Phone: `+84` hoặc `0` + 9-10 chữ số
- ✅ Display name: 2+ ký tự

---

## ❌ Lỗi Thường Gặp

| Lỗi | Nguyên Nhân | Cách Sửa |
|-----|-----------|---------|
| "Card is expired or invalid date format" | Ngày hết hạn quá khứ, hoặc format sai | Dùng MM/YY với ngày chưa hết hạn (tính từ tháng/năm hiện tại) |
| "Invalid card number" | Số thẻ không pass Luhn algorithm | Dùng test card number có sẵn |
| "CVV must be 3 digits" | CVV không phải 3 chữ số | Gõ đúng 3 chữ số |
| "Cardholder name must be at least 3 characters" | Tên quá ngắn | Nhập tối thiểu 3 ký tự |
| "Invalid email format" | Email sai format | Đảm bảo có `@` và `.` |
| "Invalid Vietnamese phone number format" | Số điện thoại sai format | Bắt đầu bằng `+84` hoặc `0` + 9-10 chữ số |

---

## 🧪 Test Ngay

### Cách 1: Dùng Test Page
1. Mở file: `PAYMENT_METHODS_TEST.html` trong browser
2. Click "Load Demo Data"
3. Xem danh sách 5 thẻ demo (1 active, 1 expired, etc.)

### Cách 2: Dùng DevTools Console
```javascript
// Xem danh sách thẻ hiện tại
console.log(paymentMethods)

// Xem dữ liệu lưu trữ
console.log(localStorage.getItem('paymentMethods'))

// Thêm thẻ demo
initDemoPaymentMethods()

// Xóa tất cả
localStorage.removeItem('paymentMethods')
paymentMethods = []
```

---

## 📊 Định Dạng Đúng

### Visa/MasterCard Card:
```
Cardholder: TRAN BAO TOAN
Card: 4111 1111 1111 1111
Expiry: 12/26 (tháng này là 01/2026, nên 12/26 = 12/2026 ✓)
CVV: 123
```

### PayPal:
```
Email: john.doe@gmail.com
Display Name: My PayPal Account
```

### Momo:
```
Phone: +84912345678 (hoặc 0912345678)
Display Name: My Momo
```

---

## ✅ Kiểm Tra Ngày Hết Hạn

**Hôm nay là: Tháng 01 năm 2026**

| Expiry | Kết Quả | Lý Do |
|--------|--------|-------|
| `01/26` | ❌ Expired | Tháng 01 năm 2026 - hết hạn |
| `02/26` | ✅ Valid | Tháng 02 năm 2026 - còn hạn |
| `12/25` | ❌ Expired | Năm 2025 - đã qua |
| `12/26` | ✅ Valid | Tháng 12 năm 2026 - còn hạn |
| `01/27` | ✅ Valid | Năm 2027 - còn hạn |

**Quy tắc**:
- Nếu YY (năm) < hiện tại: ❌ EXPIRED
- Nếu YY bằng hiện tại nhưng MM (tháng) < tháng hiện tại: ❌ EXPIRED
- Ngược lại: ✅ VALID

---

## 🎯 Quá Trình Thêm Payment Method

```
1. Click "Add New" ở Payment Methods card
   ↓
2. Modal mở, chọn tab (Visa/MasterCard/PayPal/Momo)
   ↓
3. Nhập đầy đủ thông tin đúng format
   ↓
4. Kiểm tra error messages:
   - Nếu có error đỏ → sửa lại
   - Nếu không có error → OK
   ↓
5. Click "Add Payment Method"
   ↓
6. Modal đóng, thẻ được thêm vào danh sách
   ↓
7. Thẻ đầu tiên auto-set làm "Active" (xanh + check icon)
```

---

## 💾 Lưu Ý Quan Trọng

1. **Lưu Trữ Tạm**: Hiện dữ liệu lưu ở browser (localStorage)
   - Xóa cache → mất dữ liệu
   - Chỉ là demo, backend sẽ lưu trữ thực

2. **Security**: 
   - Không gửi card details đến server chưa encrypted
   - Khi deploy, phải encrypt trước gửi backend
   - Không bao giờ log full card number

3. **Format Hiển Thị**:
   - Active: Xanh border + "Active" badge
   - Valid: Xám border + "Select" button
   - Expired: Xám mờ + "Delete" button

---

## 🚀 Tiếp Theo

1. **Thêm thẻ Visa/MasterCard đúng format**
2. **Chọn làm mặc định** → Sẽ có "Active" label
3. **Thêm PayPal/Momo** → Để reference
4. **Khi deploy**, backend sẽ handle storage + encryption

---

**Liên hệ**: Xem file `PAYMENT_METHODS_GUIDE.md` cho chi tiết kỹ thuật
