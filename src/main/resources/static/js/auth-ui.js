// 3. Module AUTH_UI (auth-ui.js)
function initAuthUI() {
    const loginForm = document.getElementById('loginForm');
    const usernameInput = loginForm.querySelector('input[name="username"], input[placeholder*="email"], input[placeholder*="phone"]');
    const passwordInput = document.getElementById('passwordField');
    const rememberCheckbox = document.getElementById('rememberMe');

    if (!loginForm || !usernameInput || !passwordInput) return;

    // ===== HIỂN THỊ TOAST TỪ SERVER (RedirectAttributes) =====
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
    });

    // ===== FORM SUBMIT LISTENER =====
    loginForm.addEventListener('submit', handleLoginFormSubmit);

    // ===== REMEMBER ME LOGIC =====
    const savedUsername = localStorage.getItem('rememberedUsername');
    if (savedUsername) {
        usernameInput.value = savedUsername;
    }

    loginForm.addEventListener('submit', () => {
        if (rememberCheckbox && rememberCheckbox.checked) {
            localStorage.setItem('rememberedUsername', usernameInput.value);
        } else {
            localStorage.removeItem('rememberedUsername');
        }
    });
}