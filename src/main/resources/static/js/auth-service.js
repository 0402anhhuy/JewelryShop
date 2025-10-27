// =========================================================
// 2. Module AUTH_SERVICE (auth-service.js)
// Xử lý gửi form AJAX và reset UI
// Yêu cầu: import utils.js
// =========================================================

// Hàm reset button
function resetLoginButton() {
    const loginBtn = document.getElementById('loginBtn');
    const passwordInput = document.getElementById('passwordField');
    if (!loginBtn) return;

    loginBtn.disabled = false;
    const icon = loginBtn.querySelector('i');
    const text = loginBtn.querySelector('span');

    if (icon) {
        icon.classList.remove('fa-spinner', 'fa-spin');
        icon.classList.add('fa-sign-in-alt');
    }
    if (text) {
        text.textContent = 'Đăng nhập';
    }
    if (passwordInput) {
        passwordInput.focus();
    }
}

// Xử lý Form Submit chính
async function handleLoginFormSubmit(e) {
    e.preventDefault();

    const loginForm = document.getElementById('loginForm');
    const usernameInput = loginForm.querySelector('input[name="username"], input[placeholder*="email"], input[placeholder*="phone"]');
    const passwordInput = document.getElementById('passwordField');
    const loginBtn = document.getElementById('loginBtn');

    if (!loginForm || !usernameInput || !passwordInput || !loginBtn) return;

    const usernameValue = usernameInput.value.trim();
    const passwordValue = passwordInput.value.trim();
    let isValid = true;
    let isUsernameValid = true;

    // Validate username
    if (!usernameValue) {
        isValid = false;
        isUsernameValid = false;
    } else if (!validateUsername(usernameValue)) {
        isValid = false;
        isUsernameValid = false;
        showToast('error', 'Lỗi', 'Email hoặc số điện thoại không hợp lệ');
    }

    if (!passwordValue) {
        isValid = false;
    }

    if (!isValid) {
        if (isUsernameValid) {
            passwordInput.value = '';
            passwordInput.focus();
        }
        return;
    }

    // 🌀 Hiệu ứng loading
    loginBtn.disabled = true;
    const icon = loginBtn.querySelector('i');
    const text = loginBtn.querySelector('span');
    if (icon) {
        icon.classList.remove('fa-sign-in-alt');
        icon.classList.add('fa-spinner', 'fa-spin');
    }
    if (text) {
        text.textContent = 'Đang đăng nhập...';
    }

    // Gửi request với CSRF token
    try {
        // Cần đảm bảo bạn có trường input ẩn cho CSRF token
        const csrfTokenInput = document.querySelector('input[name="_csrf"]');
        const csrfToken = csrfTokenInput ? csrfTokenInput.value : '';

        const response = await fetch(loginForm.action, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: new URLSearchParams({
                username: usernameValue,
                password: passwordValue,
                _csrf: csrfToken
            })
        });

        if (response.redirected) {
            if (response.url.includes('/login?error')) {
                showToast('error', 'Lỗi đăng nhập', 'Tài khoản hoặc mật khẩu không đúng!');
                resetLoginButton();
            } else {
                // Đăng nhập thành công, chuyển hướng
                window.location.href = response.url;
            }
        } else if (response.ok) {
            // Trường hợp Spring Security trả về HTML hoặc thông báo lỗi trong body
            const html = await response.text();
            if (html.includes('Invalid credentials') || html.includes('Bad credentials') || html.includes('error')) {
                showToast('error', 'Lỗi đăng nhập', 'Tài khoản hoặc mật khẩu không đúng!');
            } else {
                // Coi như thành công nếu không có lỗi rõ ràng và không redirect
                window.location.reload();
            }
            resetLoginButton();
        } else {
            showToast('error', 'Lỗi đăng nhập', 'Tài khoản hoặc mật khẩu không đúng!');
            resetLoginButton();
        }
    } catch (error) {
        console.error('Error:', error);
        showToast('error', 'Lỗi hệ thống', 'Có lỗi xảy ra, vui lòng thử lại');
        resetLoginButton();
    }
}