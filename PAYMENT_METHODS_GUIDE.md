## Hướng Dẫn Sử Dụng - Payment Methods Feature

### 📋 Tổng Quan

Tính năng "Payment Methods" trên trang profile.xhtml cho phép người dùng:
- Thêm phương thức thanh toán (Visa, MasterCard, PayPal, Momo)
- Chọn phương thức thanh toán mặc định
- Quản lý các thẻ đã thêm
- Xóa các thẻ hết hạn

### 🎯 Các Tính Năng Chính

#### 1. **Modal 4 Tab** (`profile.xhtml`)
- **Visa & MasterCard**: Nhập chi tiết thẻ (tên chủ, số thẻ, hạn sử dụng, CVV)
- **PayPal**: Nhập email và tên hiển thị
- **Momo**: Nhập số điện thoại và tên hiển thị

#### 2. **Validation Logic** (`profile-payment.js`)
- **Card Number**: Luhn algorithm validation
- **Expiry Date**: Kiểm tra hạn sử dụng (MM/YY)
- **CVV**: 3 chữ số
- **Email/Phone**: Định dạng validation

#### 3. **Hiển Thị Danh Sách**
- Active card (mặc định): Hiển thị "Active" màu xanh, có border xanh
- Non-expired card: Hiển thị button "Select"
- Expired card: Hiển thị button xóa (hình delete.png), opacity 60%

### 📝 Cách Sử Dụng

#### Thêm Phương Thức Thanh Toán
1. Click "Add New" ở Payment Methods card
2. Modal sẽ mở với tab Visa được chọn mặc định
3. Chuyển tab nếu cần (MasterCard, PayPal, Momo)
4. Điền đầy đủ thông tin
5. Click "Add Payment Method"

#### Chọn Phương Thức Mặc Định
1. Tìm thẻ muốn chọn (phải là thẻ chưa hết hạn)
2. Click button "Select"
3. Thẻ sẽ được đánh dấu "Active" với border xanh

#### Xóa Thẻ
1. Chỉ có thể xóa thẻ hết hạn
2. Click button delete (hình xóa)
3. Xác nhận xóa

### 🔧 Cấu Trúc File

```
EZMart_Supermarket_Management-war/web/
├── pages/user/
│   └── profile.xhtml          # Modal + HTML structure
└── resources/js/
    └── profile-payment.js     # Logic xử lý
```

### 📱 Dữ Liệu Lưu Trữ

Hiện tại: Lưu trữ trên **localStorage** (tạm thời cho demo)
```javascript
// Cấu trúc dữ liệu
{
    id: 1234567890,
    type: 'visa',                    // 'visa', 'mastercard', 'paypal', 'momo'
    addedDate: '2026-01-13T...',
    isDefault: true,
    isExpired: false,
    
    // Visa/MasterCard
    cardholder: 'John Doe',
    cardnumber: '4242 4242 4242 4242',
    expiry: '12/25',
    cvv: '123',
    lastFour: '4242',
    
    // PayPal
    email: 'user@example.com',
    name: 'My PayPal',
    
    // Momo
    phone: '+84912345678',
    name: 'My Momo'
}
```

### ✨ Các Hàm Chính trong profile-payment.js

```javascript
// Modal Control
openPaymentModal()           // Mở modal thêm phương thức thanh toán
closePaymentModal()          // Đóng modal

// Tab Navigation
switchPaymentTab(tab)        // Chuyển tab (visa, mastercard, paypal, momo)

// Format Input
formatCardNumber(input, cardType)  // Format số thẻ (1234 5678...)
formatExpiry(input)                // Format hạn sử dụng (MM/YY)

// Validation
validateCardNumber(number)         // Luhn algorithm
validateExpiryDate(date)           // Kiểm tra hạn
validateCVV(cvv)                   // 3 chữ số
validateEmail(email)               // Email format
validatePhone(phone)               // Số điện thoại VN

validateVisaForm()                 // Validate toàn bộ Visa form
validateMasterCardForm()           // Validate toàn bộ MasterCard form
validatePayPalForm()               // Validate toàn bộ PayPal form
validateMomoForm()                 // Validate toàn bộ Momo form

// Payment Methods Management
savePaymentMethod()                // Lưu phương thức thanh toán mới
renderPaymentMethodsList()         // Render lại danh sách
setDefaultPaymentMethod(id)        // Chọn làm mặc định
deletePaymentMethod(id)            // Xóa phương thức thanh toán

// Utilities
loadPaymentMethods()               // Tải từ localStorage
savePaymentMethodsToStorage()      // Lưu vào localStorage
showPaymentNotification(msg, type) // Hiển thị thông báo
```

### 🎨 Hình Ảnh Được Sử Dụng

Cần có các file ảnh sau trong `web/images/`:
- `visa.png` - Logo Visa
- `card.png` - Logo MasterCard
- `paypal.png` - Logo PayPal
- `momo.png` - Logo Momo
- `delete.png` - Icon xóa

### 🔌 Kết Nối Backend (Tiếp Theo)

Hiện tại hệ thống sử dụng localStorage. Để kết nối backend:

1. **Tạo PaymentMethod Entity**:
   ```java
   @Entity
   public class PaymentMethod {
       @Id @GeneratedValue
       private Long id;
       
       @ManyToOne
       private User user;
       
       private String type;          // visa, mastercard, paypal, momo
       private String encryptedData;  // Encrypted card info
       private String lastFour;
       private String expiry;
       private boolean isDefault;
       private boolean isExpired;
       private LocalDateTime addedDate;
   }
   ```

2. **Tạo PaymentMethodController**:
   ```java
   @RestController
   @RequestMapping("/api/payment-methods")
   public class PaymentMethodController {
       @PostMapping
       public ResponseEntity<?> addPaymentMethod(@RequestBody PaymentMethodDTO dto) { ... }
       
       @GetMapping
       public ResponseEntity<?> getPaymentMethods() { ... }
       
       @PutMapping("/{id}/default")
       public ResponseEntity<?> setAsDefault(@PathVariable Long id) { ... }
       
       @DeleteMapping("/{id}")
       public ResponseEntity<?> deletePaymentMethod(@PathVariable Long id) { ... }
   }
   ```

3. **Update profile-payment.js**:
   - Gọi API backend thay vì localStorage
   - Thêm error handling cho API calls
   - Thêm loading states

### 📌 Lưu Ý Quan Trọng

1. **Security**: 
   - Luôn encrypt số thẻ khi lưu trữ backend
   - Không bao giờ log hoặc hiển thị full card number
   - Sử dụng HTTPS cho mọi transaction

2. **Validation**:
   - Card number: Luhn algorithm + length check
   - Expiry: Không được quá hạn
   - CVV: Luôn validate trước khi submit

3. **UX**:
   - First card được set mặc định tự động
   - Expired cards được vô hiệu hóa tự động
   - Clear error messages cho user

### 🚀 Testing

**Test Cases**:
1. Thêm Visa/MasterCard hợp lệ
2. Thêm PayPal/Momo hợp lệ
3. Validation errors (số thẻ sai, hạn hết, v.v.)
4. Chọn card mặc định
5. Xóa card hết hạn
6. Hiển thị active card với icon check

---

**Status**: ✅ Hoàn thành (Frontend)
**Tiếp Theo**: 🔄 Kết nối Backend API
