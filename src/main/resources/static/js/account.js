// =========================================================
// 3. Module ACCOUNT (account.js)
// Yêu cầu: import utils.js
// =========================================================

// --- Profile Edit Functions ---
let isEditing = false;
let originalData = {};

function enableEditMode() {
    isEditing = true;

    originalData = {
        email: document.getElementById('emailInput').value,
        phone: document.getElementById('phoneInput').value
    };

    document.getElementById('emailInput').readOnly = false;
    document.getElementById('phoneInput').readOnly = false;

    document.getElementById('editProfileBtn').style.display = 'none';
    document.getElementById('saveBtn').style.display = 'inline-block';
    document.getElementById('cancelEditBtn').style.display = 'inline-block';

    document.getElementById('saveBtn').disabled = true;
    document.getElementById('emailInput').focus();
}

function disableEditMode() {
    isEditing = false;

    document.getElementById('emailInput').readOnly = true;
    document.getElementById('phoneInput').readOnly = true;

    document.getElementById('editProfileBtn').style.display = 'inline-block';
    document.getElementById('saveBtn').style.display = 'none';
    document.getElementById('cancelEditBtn').style.display = 'none';

    document.getElementById('saveBtn').disabled = true;
}

function cancelEdit() {
    document.getElementById('emailInput').value = originalData.email;
    document.getElementById('phoneInput').value = originalData.phone;

    disableEditMode();
    showToast('info', 'Đã hủy', 'Hủy chỉnh sửa thông tin');
}

function checkFormChanges() {
    if (!isEditing) return;

    const currentData = {
        email: document.getElementById('emailInput').value,
        phone: document.getElementById('phoneInput').value
    };

    const hasChanges =
        currentData.email !== originalData.email ||
        currentData.phone !== originalData.phone;

    document.getElementById('saveBtn').disabled = !hasChanges;
}

function initProfileEdit() {
    document.getElementById('emailInput').addEventListener('input', checkFormChanges);
    document.getElementById('phoneInput').addEventListener('input', checkFormChanges);
    document.getElementById('editProfileBtn').addEventListener('click', enableEditMode);
    document.getElementById('cancelEditBtn').addEventListener('click', cancelEdit);

    const profileForm = document.getElementById('profileForm');
    if (profileForm) {
        profileForm.addEventListener('submit', async function(e) {
            e.preventDefault();

            if (!isEditing) {
                showToast('warning', 'Thông báo', 'Vui lòng bấm "Chỉnh sửa" để thay đổi thông tin');
                return;
            }

            const submitBtn = this.querySelector('button[type="submit"]');
            const originalText = submitBtn.innerHTML;

            submitBtn.disabled = true;
            submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang lưu...';

            try {
                const csrfToken = document.querySelector('input[name="_csrf"]').value;
                const formData = new FormData(this);

                const response = await fetch('/account/update-ajax', {
                    method: 'POST',
                    headers: { 'X-CSRF-TOKEN': csrfToken },
                    body: formData
                });

                const result = await response.json();
                showToast(result.toastType, result.toastTitle, result.toastMessage);

                if (result.success) {
                    originalData = { email: result.data.email, phone: result.data.phone };
                    disableEditMode();
                }

            } catch (error) {
                console.error('Error:', error);
                showToast('error', 'Lỗi hệ thống', 'Không thể kết nối đến server. Vui lòng thử lại!');
            } finally {
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalText;
            }
        });
    }
}

// --- Tab Navigation and Initialization ---
function initAccountPage() {
    // Lấy toast data từ Thymeleaf (nếu Controller dùng RedirectAttributes)
    const toastType = document.body.getAttribute('data-toast-type');
    const toastTitle = document.body.getAttribute('data-toast-title');
    const toastMessage = document.body.getAttribute('data-toast-message');

    if (toastType && toastMessage) {
        showToast(toastType, toastTitle || '', toastMessage);
    }

    // Khởi tạo Profile Edit
    initProfileEdit();

    // ⭐️ LOGIC TAB NAVIGATION ⭐️
    const menuLinks = document.querySelectorAll('.menu-link');
    const contentSections = document.querySelectorAll('.content-section');

    function activateSection(sectionId) {
        menuLinks.forEach(l => l.classList.remove('active'));
        contentSections.forEach(section => section.classList.remove('active'));

        const targetLink = document.querySelector(`.menu-link[data-section="${sectionId}"]`);
        const targetContent = document.getElementById(sectionId);

        if (targetLink) targetLink.classList.add('active');
        if (targetContent) targetContent.classList.add('active');
    }

    // Xử lý khi trang tải (DOMContentLoaded)
    const hash = window.location.hash.substring(1);
    let targetSection = 'profile';

    if (hash && document.getElementById(hash)) {
        targetSection = hash;
    }

    activateSection(targetSection);

    menuLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const sectionId = link.getAttribute('data-section');

            activateSection(sectionId);

            window.history.pushState(null, '', window.location.pathname + '#' + sectionId);
        });
    });

    // Orders Tab Filtering (giữ nguyên logic)
    const tabButtons = document.querySelectorAll('.orders-tabs .tab-btn');
    const orderCards = document.querySelectorAll('.orders-list .order-card');
    const ordersListContainer = document.querySelector('.orders-list');

    let filterEmptyState = document.getElementById('filter-empty-state');
    if (!filterEmptyState && ordersListContainer) {
        filterEmptyState = document.createElement('div');
        filterEmptyState.id = 'filter-empty-state';
        filterEmptyState.className = 'empty-state';
        filterEmptyState.style.display = 'none';
        filterEmptyState.innerHTML = `
            <i class="fas fa-filter"></i>
            <h3>Không tìm thấy đơn hàng</h3>
            <p>Không có đơn hàng nào phù hợp với trạng thái này.</p>
        `;
        const tabsContainer = document.querySelector('.orders-tabs');
        if (tabsContainer) {
            tabsContainer.after(filterEmptyState);
        }
    }

    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            tabButtons.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            const filterStatus = btn.getAttribute('data-status');
            let hasVisibleCards = false;

            orderCards.forEach(card => {
                if (filterStatus === 'all') {
                    card.style.display = 'block';
                    hasVisibleCards = true;
                } else {
                    const statusElement = card.querySelector('.order-status');
                    const targetClass = 'status-' + filterStatus;

                    if (statusElement && statusElement.classList.contains(targetClass)) {
                        card.style.display = 'block';
                        hasVisibleCards = true;
                    } else {
                        card.style.display = 'none';
                    }
                }
            });

            if (filterEmptyState) {
                filterEmptyState.style.display = hasVisibleCards ? 'none' : 'block';
            }
        });
    });
}

// --- Password/OTP Logic (Tách khỏi initAccountPage) ---
document.getElementById('sendOtpBtn')?.addEventListener('click', async function () {
    const email = document.getElementById('emailInput').value.trim();
    if (!email) {
        showToast('warning', 'Thiếu thông tin', 'Vui lòng nhập email trước khi gửi OTP.');
        return;
    }
    const token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    showToast('info', 'Đang gửi', 'Hệ thống đang gửi mã OTP đến email của bạn.');

    try {
        const response = await fetch('/account/request-otp', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded', [header]: token },
            body: new URLSearchParams({ email })
        });

        const result = await response.text();

        if (result === 'OK') {
            showToast('success', 'Thành công', 'Mã OTP đã được gửi đến email của bạn.');
        } else if (result === 'NOT_FOUND') {
            showToast('error', 'Không tìm thấy', 'Email này chưa được đăng ký trong hệ thống.');
        } else if (result === 'INVALID_EMAIL') {
            showToast('warning', 'Email không hợp lệ', 'Vui lòng nhập địa chỉ email hợp lệ.');
        } else {
            showToast('error', 'Thất bại', 'Không thể gửi OTP. Vui lòng thử lại.');
        }
    } catch (err) {
        console.error(err);
        showToast('error', 'Lỗi kết nối', 'Không thể kết nối đến máy chủ.');
    }
});

document.getElementById('verifyOtpBtn')?.addEventListener('click', async function () {
    const email = document.getElementById('emailInput').value.trim();
    const otp = document.getElementById('otpInput').value.trim();
    const newPassword = document.getElementById('newPasswordInput').value.trim();
    const confirmPassword = document.getElementById('confirmPasswordInput').value.trim();

    if (!email || !otp || !newPassword || !confirmPassword) {
        showToast('warning', 'Thiếu thông tin', 'Vui lòng nhập đủ Email, OTP, Mật khẩu và Xác nhận mật khẩu.');
        return;
    }
    if (newPassword !== confirmPassword) {
        showToast('warning', 'Không khớp', 'Mật khẩu mới và xác nhận mật khẩu không khớp.');
        return;
    }

    const token = document.querySelector('meta[name="_csrf"]').getAttribute('content');
    const header = document.querySelector('meta[name="_csrf_header"]').getAttribute('content');

    showToast('info', 'Đang xác minh', 'Vui lòng chờ trong giây lát...');

    try {
        const response = await fetch('/account/verify-otp', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded', [header]: token },
            body: new URLSearchParams({ email, otp, newPassword })
        });

        const result = await response.text();

        if (result === 'SUCCESS') {
            showToast('success', 'Thành công', 'Mật khẩu đã được đổi thành công!');
            setTimeout(() => window.location.href = '/login', 1500);
        } else if (result === 'INVALID') {
            showToast('error', 'Sai OTP', 'Mã OTP không hợp lệ hoặc đã hết hạn.');
        } else if (result === 'NOT_FOUND') {
            showToast('error', 'Không tìm thấy', 'Email không tồn tại trong hệ thống.');
        } else {
            showToast('error', 'Lỗi', 'Không thể đổi mật khẩu. Vui lòng thử lại.');
        }
    } catch (err) {
        console.error(err);
        showToast('error', 'Lỗi kết nối', 'Không thể kết nối tới máy chủ.');
    }
});

document.addEventListener('DOMContentLoaded', initAccountPage);