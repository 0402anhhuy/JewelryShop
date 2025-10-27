// =========================================================
// 3. Module REGISTER_MAIN (register-main.js)
// Yêu cầu: import utils.js, address.js
// =========================================================

// --- OTP Logic (Tách khỏi init) ---
const emailInput = document.getElementById('email');
const resendOtpBtn = document.getElementById('resendOtpBtn');
let otpCooldown = 0;
let otpTimer = null;

function updateResendOtpButton() {
    if (!resendOtpBtn) return;
    if (otpCooldown > 0) {
        resendOtpBtn.disabled = true;
        resendOtpBtn.textContent = `Gửi lại (${otpCooldown}s)`;
        resendOtpBtn.style.opacity = '0.6';
        otpCooldown--;
        otpTimer = setTimeout(updateResendOtpButton, 1000);
    } else {
        resendOtpBtn.disabled = false;
        resendOtpBtn.textContent = 'Gửi lại OTP';
        resendOtpBtn.style.opacity = '1';
        clearTimeout(otpTimer);
    }
}

async function sendOtp(emailValue, isResend = false) {
    const { token, header } = {
        token: document.querySelector('meta[name="_csrf"]')?.content,
        header: document.querySelector('meta[name="_csrf_header"]')?.content
    };

    try {
        if (isResend) { showToast('Đang gửi lại OTP...', 'info'); }

        const resp = await fetch('/register/request-otp', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded', [header]: token },
            body: new URLSearchParams({ email: emailValue })
        });

        const text = await resp.text();

        if (text === 'OK') {
            const message = isResend ? 'OTP đã được gửi lại đến ' + emailValue : 'OTP đã được gửi đến ' + emailValue;
            showToast(message, 'success');

            if (!isResend || isResend && otpCooldown === 0) {
                otpCooldown = 60; // Bắt đầu đếm ngược 60s
                updateResendOtpButton();
            }
        }
        else if (text === 'EXISTS') { showToast('Email đã tồn tại. Vui lòng chọn email khác', 'error', 'Đăng ký thất bại'); }
        else if (text === 'INVALID_EMAIL') { showToast('Email không hợp lệ', 'error', 'Đăng ký thất bại'); }
        else { showToast('Gửi OTP thất bại: ' + text, 'error', 'Lỗi OTP'); }
    } catch (err) {
        showToast('Lỗi khi gửi OTP: ' + err.message, 'error', 'Lỗi kết nối');
    }
}


function initRegisterMain() {
    const form = document.getElementById('registerForm');
    const submitBtn = form?.querySelector('.submit-btn');
    const passwordInput = document.getElementById('password');


    // ===== Hiển thị Toast từ Server (RedirectAttributes) =====
    const toastType = /*[[${toastType}]]*/ null;
    const toastTitle = /*[[${toastTitle}]]*/ null;
    const toastMessage = /*[[${toastMessage}]]*/ null;

    if (toastType && toastMessage) {
        showToast(toastMessage, toastType, toastTitle || '');
    }

    // ===== Email Validation (Blur & Auto-Send Logic) =====
    emailInput?.addEventListener('blur', () => {
        const value = emailInput.value.trim();
        if (value === '') return;
        if (!validateEmail(value)) { showToast('Email không hợp lệ', 'error'); }
    });

    // Auto-Send Logic
    let debounceTimer = null;
    emailInput?.addEventListener('input', () => {
        clearTimeout(debounceTimer);
        const value = emailInput.value.trim();
        if (!validateEmail(value)) return;

        if (otpCooldown > 0) {
            showToast('Vui lòng đợi một lúc trước khi gửi lại OTP.', 'warning');
            return;
        }

        debounceTimer = setTimeout(() => {
            sendOtp(value, false);
        }, 1000);
    });

    // Reset cooldown nếu email thay đổi
    emailInput?.addEventListener('input', () => {
        if (otpCooldown > 0) {
            const currentEmail = emailInput.value.trim();
            if (!currentEmail || !validateEmail(currentEmail)) {
                otpCooldown = 0;
                updateResendOtpButton();
            }
        }
    });

    // Event listener for resend OTP button
    resendOtpBtn?.addEventListener('click', () => {
        const emailValue = emailInput.value.trim();
        if (!emailValue) { showToast('Vui lòng nhập email trước khi gửi lại OTP', 'error'); return; }
        if (!validateEmail(emailValue)) { showToast('Email không hợp lệ', 'error'); return; }
        sendOtp(emailValue, true);
    });


    // ===== Password Strength Checker =====
    if (passwordInput) {
        // ... (Logic Password Strength Checker giữ nguyên) ...
        let strengthContainer = document.getElementById('passwordStrength');
        // Logic tạo strengthContainer nếu chưa có giữ nguyên
        // Logic input event listener giữ nguyên
        // ...
    }

    // ===== Address Picker Integration Init =====
    // Tự động tìm và khởi tạo address picker (hàm này có trong address.js)
    if (typeof initAddressPickerIntegration === 'function') {
        initAddressPickerIntegration();
    }


    // ===== Form Submit =====
    form?.addEventListener('submit', e => {
        e.preventDefault();

        let invalid = false;
        const fields = form.querySelectorAll('input, select, textarea');

        fields.forEach(f => {
            if (!f.checkValidity()) { invalid = true; }
        });

        if (!document.getElementById('terms').checked) {
            invalid = true;
            showToast('Bạn phải đồng ý điều khoản', 'error');
        }

        if (invalid) {
            if(submitBtn) hideSpinner(submitBtn);
            showToast('Vui lòng kiểm tra lại thông tin!', 'error');
            return;
        }

        if(submitBtn) showSpinner(submitBtn);
        showToast('Đang xử lý đăng ký...', 'info');

        setTimeout(() => {
            form.submit();
        }, 2000);
    });

    // ===== Social Login =====
    document.querySelector('.social-btn.google')?.addEventListener('click', () => {
        showToast('Đang chuyển đến đăng ký với Google...', 'info');
    });

    document.querySelector('.social-btn.facebook')?.addEventListener('click', () => {
        showToast('Đang chuyển đến đăng ký với Facebook...', 'info');
    });
}

document.addEventListener('DOMContentLoaded', initRegisterMain);