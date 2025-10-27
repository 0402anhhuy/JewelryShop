// =========================================================
// 3. Module AUTH_UI (auth-ui.js)
// Xử lý Real-time validation, Remember Me, Forgot Password
// Yêu cầu: import utils.js, auth-service.js
// =========================================================

function initAuthUI() {
    const loginForm = document.getElementById('loginForm');
    const usernameInput = loginForm.querySelector('input[name="username"], input[placeholder*="email"], input[placeholder*="phone"]');
    const passwordInput = document.getElementById('passwordField');
    const rememberCheckbox = document.getElementById('rememberMe');
    const forgotPasswordLink = document.querySelector('.forgot-password');
    const socialGoogle = document.getElementById('googleLogin');
    const socialFacebook = document.getElementById('facebookLogin');

    if (!loginForm || !usernameInput || !passwordInput) return;

    // ===== HIỂN THỊ TOAST TỪ SERVER (RedirectAttributes) =====
    // Giả định các biến Thymeleaf được đưa vào body attribute hoặc global scope
    const toastType = document.body.getAttribute('data-toast-type');
    const toastTitle = document.body.getAttribute('data-toast-title');
    const toastMessage = document.body.getAttribute('data-toast-message');

    if (toastType && toastMessage) {
        showToast(toastType, toastTitle || '', toastMessage);
    }

    // ===== REAL-TIME VALIDATION (BLUR) =====
    usernameInput.addEventListener('blur', function() {
        const value = this.value.trim();
        if (value && !validateUsername(value)) {
            showToast('error', 'Lỗi', 'Email hoặc số điện thoại không hợp lệ');
        }
    });

    passwordInput.addEventListener('blur', function() {
        // Có thể thêm logic phức tạp hơn ở đây nếu cần (ví dụ: độ dài tối thiểu)
    });

    // ===== FORM SUBMIT LISTENER =====
    loginForm.addEventListener('submit', handleLoginFormSubmit);

    // ===== REMEMBER ME LOGIC =====
    // Load saved username
    const savedUsername = localStorage.getItem('rememberedUsername');
    if (savedUsername) {
        usernameInput.value = savedUsername;
    }

    // Save username on submit
    loginForm.addEventListener('submit', () => {
        if (rememberCheckbox && rememberCheckbox.checked) {
            localStorage.setItem('rememberedUsername', usernameInput.value);
        } else {
            localStorage.removeItem('rememberedUsername');
        }
    });

    // ===== FORGOT PASSWORD =====
    if (forgotPasswordLink) {
        forgotPasswordLink.addEventListener('click', e => {
            e.preventDefault();
            const currentEmail = usernameInput.value.trim();
            const email = prompt('Nhập email của bạn để đặt lại mật khẩu:', currentEmail);

            if (email) {
                if (validateUsername(email)) {
                    // Logic gửi yêu cầu đặt lại mật khẩu (cần endpoint backend)
                    showToast('info', 'Đang gửi link', 'Vui lòng kiểm tra email: ' + email);
                } else {
                    showToast('error', 'Lỗi', 'Email không hợp lệ');
                }
            }
        });
    }

    // ===== SOCIAL LOGIN (Demo) =====
    if (socialGoogle) {
        socialGoogle.addEventListener('click', () => showToast('info', 'Google Login', 'Đang chuyển hướng đến Google...'));
    }
    if (socialFacebook) {
        socialFacebook.addEventListener('click', () => showToast('info', 'Facebook Login', 'Đang chuyển hướng đến Facebook...'));
    }
}