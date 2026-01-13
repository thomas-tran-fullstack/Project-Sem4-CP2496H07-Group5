## 💳 Cách Nhập Số Thẻ - Chi Tiết Đầy Đủ

### ❓ Câu Hỏi: Số thẻ phải nhập thế nào?

**Trả lời**: 
- ✅ Bạn có thể nhập **bất kỳ số thẻ nào**
- ✅ **KHÔNG bắt buộc** phải là số thẻ trong dữ liệu mẫu
- ⚠️ **NHƯNG** số thẻ phải pass **Luhn Algorithm** validation

---

## 🧮 Luhn Algorithm là gì?

**Luhn Algorithm** là công thức toán học để validate số thẻ tín dụng.

```
Công thức:
1. Bắt đầu từ phải sang trái
2. Nhân đôi mỗi chữ số thứ 2
3. Nếu kết quả > 9, trừ 9
4. Cộng tất cả lại
5. Nếu chia hết cho 10 → ✅ VALID
```

**Ví dụ**:
```
Card: 4242 4242 4242 4242

Từ phải sang trái, nhân đôi mỗi chữ số thứ 2:
4, 8(2×4), 2, 8(2×4), 4, 8(2×4), 2, 8(2×4),
4, 8(2×4), 2, 8(2×4), 4, 8(2×4), 2, 8(2×4)

Cộng lại: 4+8+2+8+4+8+2+8+4+8+2+8+4+8+2+8 = 80
80 % 10 = 0 → ✅ VALID
```

---

## ✅ Test Card Numbers (Đã Pass Validation)

### **Chính Thức - Các Hãng Thẻ Lớn**

#### **Visa**
```
✅ 4242 4242 4242 4242
✅ 4111 1111 1111 1111
✅ 4012 8888 8888 1881
✅ 4222 2222 2222 2220
```

#### **MasterCard**
```
✅ 5555 4444 3333 2222
✅ 5105 1051 0510 5100
✅ 2720 9999 9999 9996
```

#### **American Express**
```
✅ 3782 822463 10005 (15 digits)
✅ 3714 496353 98431 (15 digits)
```

---

## ❌ Invalid Card Numbers (Không Pass)

```
❌ 4111 1111 1111 1112 (sai chữ số cuối)
❌ 1234 5678 9012 3456 (random, không valid)
❌ 0000 0000 0000 0000 (tất cả 0)
❌ 1111 1111 1111 1111 (tất cả 1)
```

---

## 🧪 Cách Test Card Numbers

### **Cách 1: Dùng Online Validator**
```
Google: "Luhn algorithm calculator"
Paste: 4242424242424242
Check: ✅ Valid
```

### **Cách 2: Dùng Terminal/Console**
```javascript
// Paste vào browser console
function validateCard(num) {
    const cleaned = num.replace(/\s/g, '');
    if (!/^\d+$/.test(cleaned)) return false;
    let sum = 0, even = false;
    for (let i = cleaned.length - 1; i >= 0; i--) {
        let d = parseInt(cleaned[i]);
        if (even) { d *= 2; if (d > 9) d -= 9; }
        sum += d;
        even = !even;
    }
    return sum % 10 === 0;
}

// Test
console.log(validateCard('4242 4242 4242 4242')); // true
console.log(validateCard('1234 5678 9012 3456')); // false
```

### **Cách 3: Dùng Website Test**
```
https://www.bincodes.com/bin-checker/
Paste card number
Check result
```

---

## 📝 Cách Nhập Đúng Format

### **Nhập Số Thẻ**

**Bước 1**: Gõ số thẻ (không cần space)
```
Gõ: 4242424242424242
```

**Bước 2**: Tự động format
```
Tự động thành: 4242 4242 4242 4242
```

**Bước 3**: System validate
```
- Check Luhn algorithm ✓
- Check length ✓
- Check format ✓
- If ✅ → OK
- If ❌ → Error message
```

---

## 🎯 Số Thẻ Để Sử Dụng

### **Sử Dụng Các Số Thẻ Này**

**Tốt nhất**: Dùng test card numbers chính thức
```
✅ 4242 4242 4242 4242 (Visa - Most common)
✅ 4111 1111 1111 1111 (Visa - Alternative)
✅ 5555 4444 3333 2222 (MasterCard)
```

**Tại sao?**
- Đã được các payment gateway (Stripe, PayPal) test
- 100% pass Luhn algorithm
- Được lưu trong profile-payment.js
- Dùng để test/demo

### **Không Cần Nhập Test Thẻ Ngoài Những Cái Trên**

Nếu bạn muốn nhập số thẻ khác:
1. Tính Luhn algorithm
2. Hoặc dùng online validator
3. Hoặc đơn giản là dùng test cards có sẵn

---

## 📋 Dữ Liệu Mẫu vs Custom

### **Dữ Liệu Mẫu** (Demo)
```
profile-payment.js có sẵn:
✅ 4242 4242 4242 4242
✅ 5555 4444 3333 2222

Trong PAYMENT_METHODS_TEST.html:
✅ Demo data có 5 thẻ sẵn
```

### **Custom - Thẻ Của Bạn**
```
Bạn có thể nhập:
✅ Bất kỳ số thẻ nào
⚠️ Miễn là pass Luhn algorithm
✅ Hoặc dùng test card numbers
```

---

## 🔐 Security Note

**IMPORTANT**: 
- 🔴 Đừng nhập số thẻ **thật** (real card) vào test page
- ✅ Chỉ dùng test card numbers (không có tiền)
- 🔴 Đừng chia sẻ card details trên internet

Test card numbers:
- Không có tiền
- Không thể xử lý transaction
- Chỉ dùng để test validation logic

---

## ✅ Bảng Tóm Tắt

| Loại | Ví Dụ | Kết Quả |
|------|-------|--------|
| **Visa Test** | 4242 4242 4242 4242 | ✅ VALID |
| **Visa Test** | 4111 1111 1111 1111 | ✅ VALID |
| **MasterCard Test** | 5555 4444 3333 2222 | ✅ VALID |
| **Random** | 1234 5678 9012 3456 | ❌ INVALID |
| **Sai Format** | 4242 4242 4242 424 | ❌ INVALID |

---

## 🚀 Cách Sử Dụng

### **Thêm Visa Card**
```
1. Click "Add New"
2. Chọn tab "Visa"
3. Nhập Cardholder: TRAN BAO TOAN
4. Nhập Card: 4242424242424242 (tự format)
5. Nhập Expiry: 1226 (tự format thành 12/26)
6. Nhập CVV: 123
7. Click "Add Payment Method"
✅ Done!
```

---

## 📞 Tóm Tắt

**Câu hỏi**: Phải nhập số thẻ nào?
**Trả lời**: Bất kỳ số nào pass Luhn algorithm

**Cách test**:
- Dùng test card numbers có sẵn
- Hoặc calculate Luhn algorithm
- Hoặc dùng online validator

**Khuyến nghị**: Dùng các số thẻ test chính thức:
```
✅ 4242 4242 4242 4242 (Visa)
✅ 5555 4444 3333 2222 (MasterCard)
```

**KHÔNG**:
- ❌ Đừng nhập card thật
- ❌ Đừng share card details
- ❌ Đừng dùng random numbers

---

**Hết!** Giờ bạn biết cách nhập số thẻ rồi 🎉
