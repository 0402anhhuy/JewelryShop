// =========================================================
// 1. Module UTILS (utils.js) - Dùng chung cho mọi trang
// =========================================================

// 1. ===== TOAST NOTIFICATION (Phiên bản đầy đủ hỗ trợ confirm/cooldown) =====
function showToast(type, title, message, showConfirm = false, confirmCallback = null) {
    const toastContainer = document.getElementById('toastContainer');
    if (!toastContainer) return;

    const toast = document.createElement('div');
    // Sử dụng cú pháp toast Account, nhưng chấp nhận các icon/title từ Register
    toast.className = `toast ${type || 'info'}`;

    const iconMap = { success: '✓', error: '✕', info: 'ℹ', warning: '⚠' };
    const icon = iconMap[type] || iconMap.info;

    let buttonsHtml = '';
    if (showConfirm && confirmCallback) {
        buttonsHtml = `
            <div class="toast-actions">
                <button class="toast-btn confirm">Xác nhận</button>
                <button class="toast-btn cancel">Hủy</button>
            </div>
        `;
    }

    toast.innerHTML = `
        <div class="toast-icon">${icon}</div>
        <div class="toast-content">
            <div class="toast-title">${title || ''}</div>
            <div class="toast-message">${message || ''}</div>
            ${buttonsHtml}
        </div>
        ${!showConfirm ? '<button class="toast-close">×</button>' : ''}
    `;

    toastContainer.appendChild(toast);

    if (!showConfirm) {
        const closeBtn = toast.querySelector('.toast-close');
        if (closeBtn) closeBtn.addEventListener('click', () => removeToast(toast));
        setTimeout(() => removeToast(toast), 4000);
    } else if (confirmCallback) {
        const confirmBtn = toast.querySelector('.toast-btn.confirm');
        const cancelBtn = toast.querySelector('.toast-btn.cancel');
        if (confirmBtn) {
            confirmBtn.addEventListener('click', () => {
                try { confirmCallback(); } catch (e) { console.error('Confirm callback error:', e); }
                finally { removeToast(toast); }
            });
        }
        if (cancelBtn) cancelBtn.addEventListener('click', () => removeToast(toast));
    }
}

function removeToast(toast) {
    if (toast && toast.parentNode) {
        toast.classList.add('hiding');
        setTimeout(() => {
            if (toast.parentNode) {
                toast.remove();
            }
        }, 300);
    }
}

// 2. ===== CSRF TOKEN HELPER (Dùng cho mọi form/AJAX) =====
function getCsrfToken() {
    const meta = document.querySelector('meta[name="_csrf"]');
    if (meta) return meta.getAttribute('content') || '';
    const input = document.querySelector('input[name="_csrf"]');
    if (input) return input.value || '';
    const hidden = document.querySelector('input[type="hidden"][name]');
    if (hidden && /csrf/i.test(hidden.name)) return hidden.value || '';
    return '';
}

// 3. ===== VALIDATION (Email/Phone - Dùng cho Login/Register/Account) =====
function validateEmail(emailValue) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(emailValue);
}

function validateUsername(value) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    const phoneRegex = /^[0-9]{10,11}$/;
    return emailRegex.test(value) || phoneRegex.test(value.replace(/\s/g, ''));
}


// 4. ===== PASSWORD TOGGLE (Dùng cho Login/Register) =====
function togglePassword(fieldId) {
    const input = document.getElementById(fieldId);
    if (!input) return;
    const button = input.parentElement.querySelector('.toggle-password');
    const icon = button?.querySelector('i');

    if (input.type === 'password') {
        input.type = 'text';
        if (icon) {
            icon.classList.remove('fa-eye');
            icon.classList.add('fa-eye-slash');
        }
    } else {
        input.type = 'password';
        if (icon) {
            icon.classList.remove('fa-eye-slash');
            icon.classList.add('fa-eye');
        }
    }
}


// 5. ===== ĐỊNH DẠNG DỮ LIỆU (Dùng cho Account/Order) =====
function formatDateTime(dateTimeString) {
    if (!dateTimeString) return 'N/A';
    try {
        const date = new Date(dateTimeString);
        if (isNaN(date.getTime())) { return 'Ngày lỗi'; }
        const day = String(date.getDate()).padStart(2, '0');
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const year = date.getFullYear();
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        return `${hours}:${minutes} ${day}/${month}/${year}`;
    } catch (e) {
        return 'Ngày lỗi';
    }
}
function formatCurrency(amount) {
    if (amount === null || amount === undefined) return '0 ₫';
    try {
        const numAmount = Number(amount);
        if (isNaN(numAmount)) { return 'Lỗi giá'; }
        return numAmount.toLocaleString('vi-VN', { style: 'currency', currency: 'VND' });
    } catch (e) {
        return 'Lỗi giá';
    }
}
function getDisplayStatusText(status) {
    switch(status) {
        case 'PENDING': return 'Chờ xác nhận';
        case 'PAID': return 'Đang xử lý';
        case 'SHIPPING': return 'Đang giao';
        case 'DELIVERED': return 'Đã giao hàng';
        case 'COMPLETED': return 'Hoàn thành';
        case 'CANCELLED': return 'Đã hủy';
        case 'RETURNED': return 'Trả hàng';
        default: return status || 'N/A';
    }
}