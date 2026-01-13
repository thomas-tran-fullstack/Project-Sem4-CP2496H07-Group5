## ⚡ Quick Add Payment Method Cheat Sheet

### ✅ VISA / MASTERCARD
```
Name:   TRAN BAO TOAN (hoặc tên của bạn)
Card:   4111 1111 1111 1111
Expiry: 12/26 (MM/YY - phải chưa hết hạn)
CVV:    123
```

### ✅ PAYPAL
```
Email: john.doe@gmail.com
Name:  My PayPal
```

### ✅ MOMO
```
Phone: +84912345678 (hoặc 0912345678)
Name:  My Momo
```

---

### ⚠️ EXPIRY DATE - NGUYÊN TẮC

**Hôm nay: 01/2026**

- ❌ `01/26` → EXPIRED (hết hạn)
- ✅ `02/26` → VALID
- ✅ `12/26` → VALID
- ✅ `01/27` → VALID

**Cách nhập**: Gõ `1226` → tự thành `12/26`

---

### 🧪 TEST NGAY

**Option 1 - Test Page**:
```
Mở: PAYMENT_METHODS_TEST.html
Click: "Load Demo Data"
```

**Option 2 - DevTools Console**:
```javascript
// Xem thẻ hiện tại
console.log(paymentMethods)

// Thêm demo
initDemoPaymentMethods()

// Xóa
localStorage.removeItem('paymentMethods')
```

---

### 📋 STEPS
1. Click "Add New"
2. Chọn tab (Visa/MasterCard/PayPal/Momo)
3. Nhập thông tin
4. Click "Add Payment Method"
5. ✅ Xong!

---

### 🔴 ERRORS THƯỜNG GẶP

| Error | Fix |
|-------|-----|
| "Card is expired" | Dùng MM/YY chưa hết hạn |
| "Invalid card number" | Dùng `4111 1111 1111 1111` |
| "Cardholder name must be 3+ chars" | Nhập tối thiểu 3 ký tự |
| "Invalid email" | Format: `text@domain.com` |
| "Invalid phone" | Format: `+84` hoặc `0` + 9-10 chữ số |

---

**Màu sắc - Danh sách hiển thị**:
- 🟢 Active: Xanh border + "Active" badge
- ⚪ Valid: Xám border + "Select" button
- 🔴 Expired: Xám + "Delete" button
