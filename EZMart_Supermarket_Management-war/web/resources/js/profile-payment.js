// Payment Methods Management - Profile Page

// Store payment methods (simulating backend data)
let paymentMethods = [];
let currentTab = 'visa';

// Get context path (application root)
function getContextPath() {
    // Try to get from input element
    const input = document.querySelector('input[id$="contextPath"]');
    if (input && input.value) {
        return input.value;
    }
    
    // Try to get from window variable
    if (window._ctx) {
        return window._ctx;
    }
    
    // Extract from current path (e.g., /EZMart_Supermarket_Management-war)
    const pathArray = window.location.pathname.split('/');
    if (pathArray.length > 1 && pathArray[1]) {
        return '/' + pathArray[1];
    }
    
    return '';
}

// Diagnostic: Check session on page load
function checkSessionDiagnostic() {
    const contextPath = getContextPath();
    console.log('[DIAGNOSTIC] Checking session with contextPath:', contextPath);
    
    fetch(contextPath + '/diagnostic/session', {
        method: 'GET',
        credentials: 'include'
    })
    .then(response => response.json())
    .then(data => {
        console.log('[DIAGNOSTIC] Session info:', data);
        if (data.hasSession) {
            console.log('[DIAGNOSTIC] Session ID:', data.sessionId);
            console.log('[DIAGNOSTIC] Session attributes:');
            if (data.attributes && data.attributes.length > 0) {
                data.attributes.forEach(attr => {
                    if (attr.isCurrentCustomerId) {
                        console.log('[DIAGNOSTIC] ✓ currentCustomerId:', attr.value);
                    } else if (attr.isCurrentCustomer) {
                        console.log('[DIAGNOSTIC] ✓ currentCustomer:', attr.type);
                    } else if (attr.isCurrentUser) {
                        console.log('[DIAGNOSTIC] ✓ currentUser:', attr.type);
                    } else {
                        console.log('[DIAGNOSTIC]   ' + attr.name + ' (' + attr.type + ')');
                    }
                });
            } else {
                console.log('[DIAGNOSTIC] ⚠ No session attributes found!');
            }
        } else {
            console.log('[DIAGNOSTIC] ⚠ No session found!');
        }
    })
    .catch(err => console.error('[DIAGNOSTIC] Error:', err));
}

// Initialize payment methods on page load
document.addEventListener('DOMContentLoaded', function() {
    // Debug: Check session on page load
    checkSessionDiagnostic();
    
    loadPaymentMethods();
    renderPaymentMethodsList();
});

// Load payment methods from backend
function loadPaymentMethods() {
    const contextPath = getContextPath();
    const url = contextPath + '/resources/api/payment-methods';
    
    console.log('Loading payment methods from:', url);
    
    fetch(url, {
        method: 'GET',
        headers: {
            'Accept': 'application/json'
        },
        credentials: 'include'
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => {
                throw new Error('Failed to load payment methods: ' + response.status + ' ' + text);
            });
        }
        return response.json();
    })
    .then(data => {
        console.log('Payment methods loaded:', data);
        paymentMethods = data && Array.isArray(data) ? data : [];
        renderPaymentMethodsList();
    })
    .catch(error => {
        console.error('Error loading payment methods:', error);
        paymentMethods = [];
        renderPaymentMethodsList();
    });
}

// Save payment methods to local storage (temporary - backend should handle this)
function savePaymentMethodsToStorage() {
    localStorage.setItem('paymentMethods', JSON.stringify(paymentMethods));
}

// Open Payment Modal
function openPaymentModal() {
    const modal = document.getElementById('paymentModal');
    if (modal) {
        modal.classList.remove('hidden');
        // Reset form
        resetPaymentForm();
        switchPaymentTab('visa');
    }
}

// Close Payment Modal
function closePaymentModal() {
    const modal = document.getElementById('paymentModal');
    if (modal) {
        modal.classList.add('hidden');
        resetPaymentForm();
    }
}

// Reset all form fields
function resetPaymentForm() {
    // Visa fields
    document.getElementById('visa_cardholder').value = '';
    document.getElementById('visa_cardnumber').value = '';
    document.getElementById('visa_expiry').value = '';
    document.getElementById('visa_cvv').value = '';
    
    // MasterCard fields
    document.getElementById('mastercard_cardholder').value = '';
    document.getElementById('mastercard_cardnumber').value = '';
    document.getElementById('mastercard_expiry').value = '';
    document.getElementById('mastercard_cvv').value = '';
    
    // PayPal fields
    document.getElementById('paypal_email').value = '';
    document.getElementById('paypal_name').value = '';
    
    // Momo fields
    document.getElementById('momo_phone').value = '';
    document.getElementById('momo_name').value = '';
    
    // Clear error messages
    clearAllErrors();
}

// Switch between payment method tabs
function switchPaymentTab(tab) {
    currentTab = tab;
    
    // Hide all tab contents
    const tabContents = document.querySelectorAll('.payment-tab-content');
    tabContents.forEach(content => {
        content.classList.add('hidden');
    });
    
    // Show selected tab content
    const selectedTab = document.getElementById('tab-' + tab);
    if (selectedTab) {
        selectedTab.classList.remove('hidden');
    }
    
    // Update tab buttons
    const tabButtons = document.querySelectorAll('.payment-tab-btn');
    tabButtons.forEach(btn => {
        btn.classList.remove('active', 'border-primary', 'text-primary', 'border-b-2');
        btn.classList.add('border-transparent', 'text-gray-600');
    });
    
    // Highlight active tab button
    const activeBtn = document.querySelector('[data-tab="' + tab + '"]');
    if (activeBtn) {
        activeBtn.classList.remove('border-transparent', 'text-gray-600');
        activeBtn.classList.add('active', 'border-primary', 'text-primary', 'border-b-2');
    }
}

// Format card number with spaces (1234 5678 9012 3456)
function formatCardNumber(input, cardType) {
    let value = input.value.replace(/\s/g, '');
    value = value.replace(/[^0-9]/g, '');
    
    if (value.length > 0) {
        let formatted = value.match(/.{1,4}/g).join(' ');
        input.value = formatted;
    }
    
    clearError(cardType + '_cardnumber_error');
}

// Format expiry date (MM/YY)
function formatExpiry(input) {
    let value = input.value.replace(/\D/g, '');
    
    if (value.length >= 2) {
        value = value.substring(0, 2) + '/' + value.substring(2, 4);
    }
    
    input.value = value;
    clearError(currentTab + '_expiry_error');
}

// Validate card number using Luhn algorithm
function validateCardNumber(cardNumber) {
    const cleaned = cardNumber.replace(/\s/g, '');
    
    // Check length (should be 13-19 digits)
    if (cleaned.length < 13 || cleaned.length > 19) {
        return false;
    }
    
    // Check if all digits
    if (!/^\d+$/.test(cleaned)) {
        return false;
    }
    
    // Luhn algorithm
    let sum = 0;
    let isEven = false;
    
    for (let i = cleaned.length - 1; i >= 0; i--) {
        let digit = parseInt(cleaned[i], 10);
        
        if (isEven) {
            digit *= 2;
            if (digit > 9) {
                digit -= 9;
            }
        }
        
        sum += digit;
        isEven = !isEven;
    }
    
    return sum % 10 === 0;
}

// Validate expiry date
function validateExpiryDate(expiryStr) {
    const parts = expiryStr.split('/');
    if (parts.length !== 2) {
        return false;
    }
    
    const month = parseInt(parts[0], 10);
    const year = parseInt(parts[1], 10);
    
    // Validate month
    if (month < 1 || month > 12) {
        return false;
    }
    
    // Validate year (assume 20xx format for YY)
    const currentDate = new Date();
    const currentYear = currentDate.getFullYear() % 100;
    const currentMonth = currentDate.getMonth() + 1;
    
    // If year is less than current year, it's expired
    if (year < currentYear) {
        return false;
    }
    
    // If year equals current year, check if month has passed
    if (year === currentYear && month < currentMonth) {
        return false;
    }
    
    return true;
}

// Validate CVV
function validateCVV(cvv) {
    return /^\d{3}$/.test(cvv);
}

// Validate email
function validateEmail(email) {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(email);
}

// Validate phone number
function validatePhone(phone) {
    // Vietnamese phone number format: +84 or 0 followed by 9-10 digits
    const re = /^(\+84|0)[0-9]{9,10}$/;
    return re.test(phone.replace(/[\s-]/g, ''));
}

// Clear single error
function clearError(elementId) {
    const errorEl = document.getElementById(elementId);
    if (errorEl) {
        errorEl.textContent = '';
        errorEl.classList.add('hidden');
    }
}

// Clear all error messages
function clearAllErrors() {
    const errorElements = document.querySelectorAll('[id$="_error"]');
    errorElements.forEach(el => {
        el.textContent = '';
        el.classList.add('hidden');
    });
}

// Show error message
function showError(elementId, message) {
    const errorEl = document.getElementById(elementId);
    if (errorEl) {
        errorEl.textContent = message;
        errorEl.classList.remove('hidden');
    }
}

// Validate Visa form
function validateVisaForm() {
    clearAllErrors();
    let isValid = true;
    
    const cardholder = document.getElementById('visa_cardholder').value.trim();
    const cardnumber = document.getElementById('visa_cardnumber').value.trim();
    const expiry = document.getElementById('visa_expiry').value.trim();
    const cvv = document.getElementById('visa_cvv').value.trim();
    
    // Validate cardholder name
    if (!cardholder) {
        showError('visa_cardholder_error', 'Cardholder name is required');
        isValid = false;
    } else if (cardholder.length < 3) {
        showError('visa_cardholder_error', 'Cardholder name must be at least 3 characters');
        isValid = false;
    }
    
    // Validate card number
    if (!cardnumber) {
        showError('visa_cardnumber_error', 'Card number is required');
        isValid = false;
    } else if (!validateCardNumber(cardnumber)) {
        showError('visa_cardnumber_error', 'Invalid card number');
        isValid = false;
    }
    
    // Validate expiry
    if (!expiry) {
        showError('visa_expiry_error', 'Expiry date is required');
        isValid = false;
    } else if (!validateExpiryDate(expiry)) {
        showError('visa_expiry_error', 'Card is expired or invalid date format');
        isValid = false;
    }
    
    // Validate CVV
    if (!cvv) {
        showError('visa_cvv_error', 'CVV is required');
        isValid = false;
    } else if (!validateCVV(cvv)) {
        showError('visa_cvv_error', 'CVV must be 3 digits');
        isValid = false;
    }
    
    return isValid;
}

// Validate MasterCard form
function validateMasterCardForm() {
    clearAllErrors();
    let isValid = true;
    
    const cardholder = document.getElementById('mastercard_cardholder').value.trim();
    const cardnumber = document.getElementById('mastercard_cardnumber').value.trim();
    const expiry = document.getElementById('mastercard_expiry').value.trim();
    const cvv = document.getElementById('mastercard_cvv').value.trim();
    
    // Validate cardholder name
    if (!cardholder) {
        showError('mastercard_cardholder_error', 'Cardholder name is required');
        isValid = false;
    } else if (cardholder.length < 3) {
        showError('mastercard_cardholder_error', 'Cardholder name must be at least 3 characters');
        isValid = false;
    }
    
    // Validate card number
    if (!cardnumber) {
        showError('mastercard_cardnumber_error', 'Card number is required');
        isValid = false;
    } else if (!validateCardNumber(cardnumber)) {
        showError('mastercard_cardnumber_error', 'Invalid card number');
        isValid = false;
    }
    
    // Validate expiry
    if (!expiry) {
        showError('mastercard_expiry_error', 'Expiry date is required');
        isValid = false;
    } else if (!validateExpiryDate(expiry)) {
        showError('mastercard_expiry_error', 'Card is expired or invalid date format');
        isValid = false;
    }
    
    // Validate CVV
    if (!cvv) {
        showError('mastercard_cvv_error', 'CVV is required');
        isValid = false;
    } else if (!validateCVV(cvv)) {
        showError('mastercard_cvv_error', 'CVV must be 3 digits');
        isValid = false;
    }
    
    return isValid;
}

// Validate PayPal form
function validatePayPalForm() {
    clearAllErrors();
    let isValid = true;
    
    const email = document.getElementById('paypal_email').value.trim();
    const name = document.getElementById('paypal_name').value.trim();
    
    // Validate email
    if (!email) {
        showError('paypal_email_error', 'PayPal email is required');
        isValid = false;
    } else if (!validateEmail(email)) {
        showError('paypal_email_error', 'Invalid email format');
        isValid = false;
    }
    
    // Validate name
    if (!name) {
        showError('paypal_name_error', 'Display name is required');
        isValid = false;
    } else if (name.length < 2) {
        showError('paypal_name_error', 'Display name must be at least 2 characters');
        isValid = false;
    }
    
    return isValid;
}

// Validate Momo form
function validateMomoForm() {
    clearAllErrors();
    let isValid = true;
    
    const phone = document.getElementById('momo_phone').value.trim();
    const name = document.getElementById('momo_name').value.trim();
    
    // Validate phone
    if (!phone) {
        showError('momo_phone_error', 'Phone number is required');
        isValid = false;
    } else if (!validatePhone(phone)) {
        showError('momo_phone_error', 'Invalid Vietnamese phone number format');
        isValid = false;
    }
    
    // Validate name
    if (!name) {
        showError('momo_name_error', 'Display name is required');
        isValid = false;
    } else if (name.length < 2) {
        showError('momo_name_error', 'Display name must be at least 2 characters');
        isValid = false;
    }
    
    return isValid;
}

// Save payment method
function savePaymentMethod() {
    let isValid = false;
    let paymentData = {};
    
    // Validate based on current tab
    if (currentTab === 'visa') {
        isValid = validateVisaForm();
        if (isValid) {
            paymentData.cardType = 'VISA';
            paymentData.cardholderName = document.getElementById('visa_cardholder').value.trim();
            paymentData.cardNumber = document.getElementById('visa_cardnumber').value.trim().replace(/\s/g, '');
            paymentData.cardExpiry = document.getElementById('visa_expiry').value.trim();
        }
    } else if (currentTab === 'mastercard') {
        isValid = validateMasterCardForm();
        if (isValid) {
            paymentData.cardType = 'MASTERCARD';
            paymentData.cardholderName = document.getElementById('mastercard_cardholder').value.trim();
            paymentData.cardNumber = document.getElementById('mastercard_cardnumber').value.trim().replace(/\s/g, '');
            paymentData.cardExpiry = document.getElementById('mastercard_expiry').value.trim();
        }
    } else if (currentTab === 'paypal') {
        isValid = validatePayPalForm();
        if (isValid) {
            paymentData.cardType = 'PAYPAL';
            paymentData.email = document.getElementById('paypal_email').value.trim();
            paymentData.cardholderName = document.getElementById('paypal_name').value.trim();
        }
    } else if (currentTab === 'momo') {
        isValid = validateMomoForm();
        if (isValid) {
            paymentData.cardType = 'MOMO';
            paymentData.phone = document.getElementById('momo_phone').value.trim();
            paymentData.cardholderName = document.getElementById('momo_name').value.trim();
        }
    }
    
    if (isValid) {
        // Get context path
        const contextPath = getContextPath();
        const url = contextPath + '/resources/api/payment-methods';
        
        // Send to server
        const formData = new FormData();
        formData.append('cardType', paymentData.cardType);
        if (paymentData.cardNumber) formData.append('cardNumber', paymentData.cardNumber);
        if (paymentData.cardExpiry) formData.append('cardExpiry', paymentData.cardExpiry);
        if (paymentData.cardholderName) formData.append('cardholderName', paymentData.cardholderName);
        if (paymentData.email) formData.append('email', paymentData.email);
        if (paymentData.phone) formData.append('phone', paymentData.phone);
        formData.append('isDefault', paymentMethods.length === 0 ? 'true' : 'false');

        // Debug: log what we're sending
        console.log('Sending payment data to:', url);
        console.log('FormData contents:');
        for (let [key, value] of formData.entries()) {
            if (key === 'cardNumber' && value) {
                console.log('  ' + key + '=***' + value.substring(Math.max(0, value.length-4)));
            } else {
                console.log('  ' + key + '=' + value);
            }
        }

        fetch(contextPath + '/resources/api/payment-methods', {
            method: 'POST',
            body: formData,
            credentials: 'include'
        })
        .then(response => {
            console.log('Save payment response status:', response.status);
            if (!response.ok) {
                return response.text().then(text => {
                    console.log('Server response body:', text);
                    try {
                        const data = JSON.parse(text);
                        throw new Error(data.error || 'Failed to save payment method (status: ' + response.status + ')');
                    } catch (e) {
                        throw new Error('Failed to save payment method (status: ' + response.status + '). Server response: ' + text);
                    }
                }).catch(err => {
                    throw new Error('Failed to save payment method (status: ' + response.status + ')');
                });
            }
            return response.json();
        })
        .then(data => {
            console.log('Save payment success:', data);
            if (data.success) {
                // Reload payment methods
                loadPaymentMethods();
                
                // Show success message
                showPaymentNotification('Payment method added successfully!', 'success');
                
                // Close modal
                closePaymentModal();
            } else {
                showPaymentNotification(data.error || 'Failed to save payment method', 'error');
            }
        })
        .catch(error => {
            console.error('Error saving payment method:', error);
            showPaymentNotification(error.message || 'Failed to save payment method', 'error');
        });
    }
}

// Render payment methods list
function renderPaymentMethodsList() {
    const container = document.getElementById('paymentMethodsList');
    if (!container) return;
    
    if (paymentMethods.length === 0) {
        container.innerHTML = '<p class="text-center text-gray-500 py-6">No payment methods added yet</p>';
        return;
    }
    
    // Sort methods: non-expired active first, then other non-expired, then expired
    const sorted = [...paymentMethods].sort((a, b) => {
        if (a.isDefault) return -1;
        if (b.isDefault) return 1;
        return 0;
    });
    
    container.innerHTML = '';
    
    sorted.forEach(method => {
        const card = document.createElement('div');
        card.className = `payment-method-card flex items-center justify-between p-4 rounded-lg border transition-all ${
            method.isDefault 
                ? 'border-2 border-primary dark:border-primary bg-primary/5 dark:bg-primary/10'
                : 'border-[#e5e7eb] dark:border-[#2a3e32] bg-[#f9fafb] dark:bg-[#1f3326]'
        }`;
        
        // Left side - Card info
        const leftDiv = document.createElement('div');
        leftDiv.className = 'flex items-center gap-4 flex-1';
        
        // Get card image based on type
        let imagePath = '../../resources/images/';
        let cardTypeDisplay = '';
        
        if (method.type === 'VISA') {
            imagePath += 'visa.png';
            cardTypeDisplay = 'Visa';
        } else if (method.type === 'MASTERCARD') {
            imagePath += 'card.png';
            cardTypeDisplay = 'MasterCard';
        } else if (method.type === 'PAYPAL') {
            imagePath += 'paypal.png';
            cardTypeDisplay = 'PayPal';
        } else if (method.type === 'MOMO') {
            imagePath += 'momo.png';
            cardTypeDisplay = 'Momo';
        }
        
        // Card image
        const imgDiv = document.createElement('div');
        imgDiv.className = 'w-12 h-8 bg-white rounded border flex items-center justify-center flex-shrink-0';
        const img = document.createElement('img');
        img.src = imagePath;
        img.className = 'h-4';
        img.alt = method.type;
        imgDiv.appendChild(img);
        leftDiv.appendChild(imgDiv);
        
        // Card details
        const detailsDiv = document.createElement('div');
        detailsDiv.className = 'flex flex-col flex-1';
        
        let cardTitle = '';
        let cardSubtitle = '';
        
        if (method.type === 'VISA' || method.type === 'MASTERCARD') {
            cardTitle = `${cardTypeDisplay} ${method.cardNumber}`;
            cardSubtitle = `Expires ${method.expiry}`;
        } else if (method.type === 'PAYPAL') {
            cardTitle = `PayPal`;
            cardSubtitle = method.email || 'PayPal Account';
        } else if (method.type === 'MOMO') {
            cardTitle = `Momo`;
            cardSubtitle = method.phone || 'Momo Account';
        }
        
        const titleEl = document.createElement('span');
        titleEl.className = 'text-sm font-bold text-[#111813] dark:text-white';
        titleEl.textContent = cardTitle;
        
        const subtitleEl = document.createElement('span');
        subtitleEl.className = 'text-xs text-[#4b5563] dark:text-[#d1d5db]';
        subtitleEl.textContent = cardSubtitle;
        
        detailsDiv.appendChild(titleEl);
        detailsDiv.appendChild(subtitleEl);
        leftDiv.appendChild(detailsDiv);
        
        card.appendChild(leftDiv);
        
        // Right side - Action button or status
        const rightDiv = document.createElement('div');
        rightDiv.className = 'flex items-center gap-3';
        
        if (method.isDefault) {
            // Active status for default card
            const activeLabel = document.createElement('div');
            activeLabel.className = 'flex items-center gap-2 px-3 py-1 bg-green-100 dark:bg-green-900/20 rounded-full';
            
            const activeIcon = document.createElement('span');
            activeIcon.className = 'material-symbols-outlined text-green-600 dark:text-green-400';
            activeIcon.style.fontSize = '16px';
            activeIcon.textContent = 'check_circle';
            
            const activeText = document.createElement('span');
            activeText.className = 'text-xs font-bold text-green-600 dark:text-green-400';
            activeText.textContent = 'Active';
            
            activeLabel.appendChild(activeIcon);
            activeLabel.appendChild(activeText);
            rightDiv.appendChild(activeLabel);
            
            // Delete button
            const deleteBtn = document.createElement('button');
            deleteBtn.type = 'button';
            deleteBtn.className = 'p-2 hover:bg-red-100 rounded transition';
            deleteBtn.title = 'Delete payment method';
            deleteBtn.onclick = () => deletePaymentMethod(method.id);
            
            const deleteIcon = document.createElement('span');
            deleteIcon.className = 'material-symbols-outlined text-red-600';
            deleteIcon.style.fontSize = '20px';
            deleteIcon.textContent = 'delete';
            deleteBtn.appendChild(deleteIcon);
            rightDiv.appendChild(deleteBtn);
        } else {
            // Select button for non-default cards
            const selectBtn = document.createElement('button');
            selectBtn.type = 'button';
            selectBtn.className = 'px-4 py-2 border border-gray-300 dark:border-gray-600 rounded text-sm font-medium text-gray-700 dark:text-gray-300 hover:bg-gray-50 dark:hover:bg-gray-700 transition';
            selectBtn.textContent = 'Select';
            selectBtn.onclick = () => setDefaultPaymentMethod(method.id);
            rightDiv.appendChild(selectBtn);
            
            // Delete button
            const deleteBtn = document.createElement('button');
            deleteBtn.type = 'button';
            deleteBtn.className = 'p-2 hover:bg-red-100 rounded transition';
            deleteBtn.title = 'Delete payment method';
            deleteBtn.onclick = () => deletePaymentMethod(method.id);
            
            const deleteIcon = document.createElement('span');
            deleteIcon.className = 'material-symbols-outlined text-red-600';
            deleteIcon.style.fontSize = '20px';
            deleteIcon.textContent = 'delete';
            deleteBtn.appendChild(deleteIcon);
            rightDiv.appendChild(deleteBtn);
        }
        
        card.appendChild(rightDiv);
        container.appendChild(card);
    });
}

// Set payment method as default
function setDefaultPaymentMethod(methodId) {
    const method = paymentMethods.find(m => m.id === methodId);
    if (!method) {
        showPaymentNotification('Payment method not found', 'error');
        return;
    }

    const contextPath = getContextPath();
    
    // Send as query parameters for PUT request
    const url = contextPath + '/resources/api/payment-methods?cardId=' + method.id + '&isDefault=true';
    
    console.log('[Payment] Setting default payment method with URL:', url);

    fetch(url, {
        method: 'PUT',
        credentials: 'include'
    })
    .then(response => {
        console.log('[Payment] Set default response status:', response.status);
        if (!response.ok) {
            return response.json().then(data => {
                console.log('[Payment] Error response:', data);
                throw new Error(data.error || 'Failed to set default payment method');
            }).catch(err => {
                throw new Error('Failed to set default payment method');
            });
        }
        return response.json();
    })
    .then(data => {
        console.log('[Payment] Set default response data:', data);
        if (data.success) {
            // Reload payment methods
            loadPaymentMethods();
            showPaymentNotification('Payment method set as default', 'success');
        } else {
            showPaymentNotification(data.error || 'Failed to set default payment method', 'error');
        }
    })
    .catch(error => {
        console.error('Error setting default payment method:', error);
        showPaymentNotification(error.message || 'Failed to set default payment method', 'error');
    });
}

// Delete payment method
function deletePaymentMethod(methodId) {
    if (confirm('Are you sure you want to delete this payment method?')) {
        const contextPath = getContextPath();
        
        // Find the card in the current list to get its ID
        const cardToDelete = paymentMethods.find(m => m.id === methodId);
        if (!cardToDelete) {
            showPaymentNotification('Payment method not found', 'error');
            return;
        }

        fetch(contextPath + '/resources/api/payment-methods?cardId=' + cardToDelete.id, {
            method: 'DELETE',
            credentials: 'include'
        })
        .then(response => {
            if (!response.ok) {
                return response.json().then(data => {
                    throw new Error(data.error || 'Failed to delete payment method');
                }).catch(err => {
                    throw new Error('Failed to delete payment method');
                });
            }
            return response.json();
        })
        .then(data => {
            if (data.success) {
                // Reload payment methods
                loadPaymentMethods();
                showPaymentNotification('Payment method deleted successfully', 'success');
            } else {
                showPaymentNotification(data.error || 'Failed to delete payment method', 'error');
            }
        })
        .catch(error => {
            console.error('Error deleting payment method:', error);
            showPaymentNotification(error.message || 'Failed to delete payment method', 'error');
        });
    }
}

// Show notification (similar to address notifications)
function showPaymentNotification(message, type) {
    var container = document.getElementById('payment-notification-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'payment-notification-container';
        container.style.cssText = 'position: fixed; top: 20px; right: 20px; z-index: 9999; pointer-events: none;';
        document.body.appendChild(container);
    }

    var notification = document.createElement('div');
    notification.className = 'notification-popup';
    notification.textContent = message;

    var bgColor = type === 'success' ? '#10b981' : '#ef4444';
    var borderColor = type === 'success' ? '#059669' : '#dc2626';

    notification.style.cssText = 'background-color: ' + bgColor + '; ' +
        'color: white; ' +
        'padding: 14px 20px; ' +
        'border-radius: 8px; ' +
        'margin-bottom: 10px; ' +
        'font-size: 14px; ' +
        'font-weight: 500; ' +
        'box-shadow: 0 4px 12px rgba(0,0,0,0.15); ' +
        'border-left: 4px solid ' + borderColor + '; ' +
        'animation: slideIn 0.3s ease-out forwards; ' +
        'pointer-events: auto; ' +
        'min-width: 250px; ' +
        'max-width: 400px;';

    container.appendChild(notification);

    // Auto-remove after 4 seconds with fade-out animation
    var timeout = setTimeout(function () {
        notification.style.animation = 'slideOut 0.3s ease-out forwards';
        setTimeout(function () {
            try { notification.parentNode.removeChild(notification); } catch (e) { }
        }, 300);
    }, 4000);

    return { element: notification, timeout: timeout };
}

// Add CSS animations if not already present
if (!document.getElementById('payment-animations')) {
    const style = document.createElement('style');
    style.id = 'payment-animations';
    style.textContent = `
        @keyframes slideIn {
            from {
                transform: translateX(400px);
                opacity: 0;
            }
            to {
                transform: translateX(0);
                opacity: 1;
            }
        }
        
        @keyframes slideOut {
            from {
                transform: translateX(0);
                opacity: 1;
            }
            to {
                transform: translateX(400px);
                opacity: 0;
            }
        }
    `;
    document.head.appendChild(style);
}
