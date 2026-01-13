// Test Data & Demo for Payment Methods
// Run this in browser console to populate demo data

function initDemoPaymentMethods() {
    const demoData = [
        {
            id: 1700000001,
            type: 'visa',
            cardholder: 'JOHN DOE',
            cardnumber: '4242 4242 4242 4242',
            expiry: '12/25',
            cvv: '123',
            lastFour: '4242',
            addedDate: new Date().toISOString(),
            isDefault: true,
            isExpired: false
        },
        {
            id: 1700000002,
            type: 'mastercard',
            cardholder: 'JANE SMITH',
            cardnumber: '5555 5555 5555 4444',
            expiry: '08/26',
            cvv: '456',
            lastFour: '4444',
            addedDate: new Date().toISOString(),
            isDefault: false,
            isExpired: false
        },
        {
            id: 1700000003,
            type: 'paypal',
            email: 'john.doe@gmail.com',
            name: 'My PayPal',
            addedDate: new Date().toISOString(),
            isDefault: false,
            isExpired: false
        },
        {
            id: 1700000004,
            type: 'momo',
            phone: '+84912345678',
            name: 'My Momo Account',
            addedDate: new Date().toISOString(),
            isDefault: false,
            isExpired: false
        },
        {
            id: 1700000005,
            type: 'visa',
            cardholder: 'EXPIRED CARD',
            cardnumber: '4111 1111 1111 1111',
            expiry: '09/23',  // Expired
            cvv: '789',
            lastFour: '1111',
            addedDate: new Date().toISOString(),
            isDefault: false,
            isExpired: true
        }
    ];
    
    localStorage.setItem('paymentMethods', JSON.stringify(demoData));
    console.log('✅ Demo payment methods initialized!');
    console.log('Demo data:', demoData);
    
    // Reload page to see changes
    location.reload();
}

// Visa test card: 4242 4242 4242 4242
// MasterCard test card: 5555 4444 3333 2222
// Invalid test card: 4111 1111 1111 1111 (expired in demo)
// All with CVV: any 3 digits, Expiry: any future date (MM/YY)

console.log('Payment Methods Test Suite');
console.log('============================');
console.log('');
console.log('To load demo data, run:');
console.log('  initDemoPaymentMethods()');
console.log('');
console.log('Test Card Numbers:');
console.log('  Visa: 4242 4242 4242 4242 (valid)');
console.log('  MasterCard: 5555 4444 3333 2222 (valid)');
console.log('  Invalid: 4111 1111 1111 1111');
console.log('');
console.log('Test PayPal Email: john.doe@gmail.com');
console.log('Test Momo Phone: +84912345678');
console.log('');
console.log('To clear all payment methods, run:');
console.log('  localStorage.removeItem("paymentMethods")');
console.log('');
