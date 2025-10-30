// TRONG FILE: checkout.js

// === TOÀN BỘ LOGIC TÍNH TOÁN ĐƯỢC DI CHUYỂN VÀO ĐÂY ===

// Khởi tạo các biến toàn cục (sẽ được gán giá trị khi DOM load)
let IS_BUY_NOW = false;
let BASE_SUBTOTAL = 0;
let IS_NOT_NEW_MEMBER = true;
const VAT_RATE = 0.1; // 10% VAT (phải khớp với controller)

/**
 * Hàm định dạng số thành tiền tệ VND.
 */
function formatCurrency(num) {
    if (typeof num !== 'number') {
        num = 0;
    }
    return num.toLocaleString('vi-VN') + ' đ';
}

/**
 * Tính giảm giá từ mã coupon (logic sao chép từ cart.html)
 */
function calculateCouponDiscount(subtotal, couponCode) {
    if (!couponCode) return 0;

    switch (couponCode.trim().toUpperCase()) {
        case 'NEWMEMBER20':
            // Giảm 20%
            return Math.floor(subtotal * 0.20);
        case 'FREESHIP':
            // Mã này không giảm tiền hàng, sẽ được xử lý ở phí ship
            return 0;
        // Thêm các mã giảm giá khác của bạn ở đây
        default:
            return 0;
    }
}

/**
 * Hàm tính toán và cập nhật lại toàn bộ Bảng tóm tắt đơn hàng
 */
function updateFinalSummary() {
    // Lấy các element HIỂN THỊ
    const subtotalEl = document.getElementById('subtotalDisplay');
    const discountEl = document.getElementById('discountDisplay');
    const shippingEl = document.getElementById('shippingDisplay');
    const totalEl = document.getElementById('totalDisplay');
    const taxEl = document.getElementById('taxDisplay');
    const shippingNameEl = document.getElementById('shippingNameDisplay');
    const couponEl = document.getElementById('couponDisplay');

    // Lấy các INPUT ẨN (để submit form)
    const subtotalInput = document.getElementById('subtotalVal');
    const discountInput = document.getElementById('discountVal');
    const shippingInput = document.getElementById('shippingVal');
    const shippingNameInput = document.getElementById('shippingNameVal');
    const totalInput = document.getElementById('totalVal');
    const taxInput = document.getElementById('taxVal');
    const couponInputHidden = document.getElementById('couponCodeVal');

    // Lấy element nhập mã coupon
    const couponInput = document.getElementById('couponInput');

    // === BƯỚC 1: LẤY CÁC GIÁ TRỊ CƠ SỞ ===
    // (Sử dụng các biến toàn cục đã được gán)
    const subtotal = BASE_SUBTOTAL;
    let couponCode = couponInput ? couponInput.value.trim().toUpperCase() : (couponInputHidden ? couponInputHidden.value : '');

    // Cập nhật Tạm tính (luôn luôn)
    if (subtotalEl) subtotalEl.textContent = formatCurrency(subtotal);
    if (subtotalInput) subtotalInput.value = subtotal;

    // === BƯỚC 2: KIỂM TRA ĐIỀU KIỆN VOUCHER ===
    const newMemberVoucher = document.getElementById('voucher-newmember');
    const newMemberRadio = document.getElementById('radio-newmember');

    if (newMemberVoucher && newMemberRadio) {
        if (IS_NOT_NEW_MEMBER) {
            newMemberVoucher.classList.add('voucher-disabled');
            newMemberRadio.disabled = true;
            if (couponCode === 'NEWMEMBER20') {
                couponInput.value = '';
                newMemberRadio.checked = false;
                couponCode = '';
            }
        } else {
            newMemberVoucher.classList.remove('voucher-disabled');
            newMemberRadio.disabled = false;
        }
    }

    // --- 2.2 Tính giảm giá (để kiểm tra FREESHIP) ---
    const discountAmount = calculateCouponDiscount(subtotal, couponCode);
    const subtotalAfterDiscount = subtotal - discountAmount;

    // --- 2.3 Kiểm tra FREESHIP ---
    const freeshipVoucher = document.getElementById('voucher-freeship');
    const freeshipRadio = document.getElementById('radio-freeship');

    if (freeshipVoucher && freeshipRadio) {
        if (subtotalAfterDiscount < 10000000) { // 10 triệu
            freeshipVoucher.classList.add('voucher-disabled');
            freeshipRadio.disabled = true;
            if (couponCode === 'FREESHIP') {
                couponInput.value = '';
                freeshipRadio.checked = false;
                updateFinalSummary();
                return;
            }
        } else {
            freeshipVoucher.classList.remove('voucher-disabled');
            freeshipRadio.disabled = false;
        }
    }

    // === BƯỚC 3: TÍNH TOÁN CÁC GIÁ TRỊ CUỐI CÙNG ===

    // --- 3.1 Giảm giá ---
    if (discountEl) discountEl.textContent = '- ' + formatCurrency(discountAmount);
    if (discountInput) discountInput.value = discountAmount;
    if (couponEl) couponEl.textContent = couponCode || 'Không có';
    if (couponInputHidden) couponInputHidden.value = couponCode;

    // --- 3.2 Phí vận chuyển ---
    const shippingRadio = document.querySelector('input[name="shipping"]:checked');
    let shippingFee = parseInt(document.getElementById('shippingVal')?.value || '0', 10);
    let shippingName = document.getElementById('shippingNameVal')?.value || 'Giao hàng tiêu chuẩn';

    if (shippingRadio) {
        const option = shippingRadio.closest('.shipping-option');
        shippingName = option.querySelector('.shipping-name').textContent.trim();
        const shippingPriceText = option.querySelector('.shipping-price').textContent;
        if (shippingPriceText !== 'Miễn phí') {
            shippingFee = parseInt(shippingPriceText.replace(/[^0-9]/g, ''), 10);
        } else {
            shippingFee = 0;
        }
    }

    // Áp dụng FREESHIP (nếu còn hợp lệ)
    if (couponCode === 'FREESHIP' && (subtotal - discountAmount) >= 10000000) {
        shippingFee = 0;
    }

    if (shippingEl) shippingEl.textContent = (shippingFee === 0) ? 'Miễn phí' : formatCurrency(shippingFee);
    if (shippingNameEl) shippingNameEl.textContent = shippingName;
    if (shippingInput) shippingInput.value = shippingFee;
    if (shippingNameInput) shippingNameInput.value = shippingName;

    // --- 3.3 Thuế VAT ---
    // (Sửa lỗi: Tính thuế trên subtotalAfterDiscount)
    const taxAmount = Math.floor(subtotalAfterDiscount * VAT_RATE);
    if (taxEl) taxEl.textContent = formatCurrency(taxAmount);
    if (taxInput) taxInput.value = taxAmount;

    // --- 3.4 Tổng cộng ---
    const finalTotal = (subtotal - discountAmount) + shippingFee + taxAmount;
    const finalTotalNonNegative = finalTotal < 0 ? 0 : finalTotal;
    if (totalEl) totalEl.textContent = formatCurrency(finalTotalNonNegative);
    if (totalInput) totalInput.value = finalTotalNonNegative;
}


// ===================================================================
//  KHỞI CHẠY VÀ GẮN SỰ KIỆN
// ===================================================================
document.addEventListener('DOMContentLoaded', function() {

    // --- BƯỚC 1: ĐỌC DỮ LIỆU TỪ "CẦU NỐI" HTML ---
    const form = document.getElementById('checkout-form');
    const dataset = form ? form.dataset : {};

    // Gán giá trị từ DOM cho các biến toàn cục
    IS_BUY_NOW = dataset.isBuyNow === 'true';
    BASE_SUBTOTAL = parseInt(dataset.baseSubtotal || '0', 10);
    IS_NOT_NEW_MEMBER = dataset.isNotNewMember === 'true';

    // --- BƯỚC 2: GẮN LOGIC CỦA TRANG CHECKOUT ---

    // Logic chọn payment
    document.querySelectorAll('.payment-option').forEach(option => {
        option.addEventListener('click', function() {
            document.querySelectorAll('.payment-option').forEach(opt => {
                opt.classList.remove('selected');
            });
            this.classList.add('selected');
            this.querySelector('input[type="radio"]').checked = true;
        });
    });

    // Logic tự động check shipping (Đã sửa lỗi)
    const initialShippingFee = parseInt(document.getElementById('shippingVal')?.value || '0', 10);
    const initialShippingName = document.getElementById('shippingNameVal')?.value || 'Giao hàng tiêu chuẩn';

    document.querySelectorAll('input[name="shipping"]').forEach(radio => {
        const option = radio.closest('.shipping-option');
        const priceText = option.querySelector('.shipping-price').textContent;
        let price = 0;
        if (priceText !== 'Miễn phí') {
            price = parseInt(priceText.replace(/[^0-9]/g, ''), 10);
        }
        const optionName = option.querySelector('.shipping-name').textContent.trim();

        if (price === initialShippingFee && optionName === initialShippingName) {
            radio.checked = true;
        }
    });

    // Tính toán lần đầu khi vào trang
    updateFinalSummary();

    // Gắn sự kiện cho radio shipping
    document.querySelectorAll('input[name="shipping"]').forEach(radio => {
        radio.addEventListener('change', updateFinalSummary);
    });

    // Chỉ riêng phần voucher/coupon mới cần IS_BUY_NOW
    // (Vì khi mua từ giỏ hàng, logic này đã nằm ở cart.html)
    if (IS_BUY_NOW) {
        document.querySelectorAll('input[name="voucher"]').forEach(radio => {
            radio.addEventListener('change', function () {
                const voucherCode = this.value;
                const couponInput = document.getElementById('couponInput');
                if (couponInput) couponInput.value = voucherCode;
                updateFinalSummary();
            });
        });

        const applyBtn = document.getElementById('applyCouponBtn');
        if (applyBtn) {
            applyBtn.addEventListener('click', function (e) {
                e.preventDefault();
                updateFinalSummary();
            });
        }

        const couponInputEl = document.getElementById('couponInput');
        if (couponInputEl) {
            couponInputEl.addEventListener('keypress', function (e) {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    updateFinalSummary();
                }
            });
        }
    }
});