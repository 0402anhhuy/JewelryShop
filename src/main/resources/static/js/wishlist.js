// =========================================================
// 4. Module WISHLIST_CART (wishlist-cart.js)
// Yêu cầu: import utils.js
// =========================================================

// Helper: Tìm SerialNumber từ Product ID
function getProductSerialNumberFromDOM(productId) {
    const itemElement = document.querySelector(`.wishlist-item[data-id="${productId}"]`);
    return itemElement ? itemElement.getAttribute('data-serial-number') : null;
}

// --- Wishlist Management ---

// 1. Hàm gọi xác nhận (chỉ cần Wishlist Item ID)
function removeFromWishlist(wishlistItemId) {
    showToast('warning', 'Xác nhận xóa', 'Bạn có chắc muốn xóa sản phẩm này khỏi danh sách yêu thích?', true, () => {
        performRemoveFromWishlist(wishlistItemId);
    });
}

// 2. Hàm thực hiện AJAX xóa
async function performRemoveFromWishlist(wishlistItemId) {
    const itemElement = document.querySelector(`.wishlist-item[data-wishlist-id="${wishlistItemId}"]`);
    const productId = itemElement ? itemElement.getAttribute('data-id') : null;

    if (!productId || !itemElement) {
        showToast('error', 'Lỗi', 'Không tìm thấy sản phẩm trên giao diện để xóa.');
        return;
    }

    const token = getCsrfToken();
    const header = document.querySelector('meta[name="_csrf_header"]').content;

    showToast('info', 'Đang xử lý', 'Đang xóa sản phẩm khỏi Wishlist...');

    try {
        const res = await fetch(`/wishlist/remove/${wishlistItemId}`, {
            method: 'POST',
            headers: { [header]: token, 'Content-Type': 'application/json' }
        });

        const result = await res.json();

        if (result.success) {
            itemElement.style.transform = 'scale(0.9)';
            itemElement.style.opacity = '0';
            setTimeout(() => {
                itemElement.remove();

                const countElement = document.querySelector('.wishlist-count span');
                const wishlistContainer = document.getElementById('wishlist-container');
                const emptyState = document.querySelector('#wishlist .empty-state');
                const countDiv = document.querySelector('.wishlist-count');

                let newCount = 0;
                if(countElement) {
                    const currentCount = parseInt(countElement.textContent) || 0;
                    newCount = Math.max(0, currentCount - 1);
                    countElement.textContent = newCount;
                }

                if (newCount === 0) {
                    if (wishlistContainer) wishlistContainer.style.display = 'none';
                    if (emptyState) emptyState.style.display = 'block';
                    if (countDiv) countDiv.style.display = 'none';
                }
            }, 300);

            const heartIcons = document.querySelectorAll(`.action-btn[data-id="${productId}"] i.fa-heart`);
            heartIcons.forEach(icon => {
                icon.classList.remove('fas');
                icon.classList.add('far');
                icon.style.color = '';
            });

            showToast('success', 'Thành công', result.message || 'Đã xóa sản phẩm khỏi Wishlist.');

        } else {
            showToast('error', 'Lỗi', result.message || 'Không thể xóa sản phẩm. Vui lòng thử lại.');
        }

    } catch (error) {
        console.error('Lỗi khi xóa wishlist:', error);
        showToast('error', 'Lỗi mạng', 'Không thể kết nối đến server để xóa sản phẩm.');
    }
}

// --- Cart Actions ---

/**
 * Thêm sản phẩm vào giỏ hàng bằng AJAX.
 * @param {number} productId - ID của sản phẩm cần thêm.
 */
async function addToCart(productId) {
    const token = getCsrfToken();
    const header = document.querySelector('meta[name="_csrf_header"]').content;

    showToast('info', 'Đang xử lý', 'Đang thêm sản phẩm vào giỏ hàng...');

    try {
        const res = await fetch(`/cart/add/${productId}`, {
            method: 'POST',
            headers: { [header]: token, 'Content-Type': 'application/json' }
        });

        const result = await res.json();

        if (res.ok && result.success) {
            showToast('success', 'Thành công', result.message || 'Đã thêm sản phẩm vào giỏ hàng!');

            if (result.cartCount !== undefined) {
                const cartBadge = document.querySelector('.header-icons .badge');
                if (cartBadge) {
                    cartBadge.textContent = result.cartCount;
                    cartBadge.style.display = result.cartCount > 0 ? 'block' : 'none';
                }
            }
        } else {
            showToast('error', 'Thất bại', result.message || 'Không thể thêm sản phẩm. Vui lòng thử lại.');

            if (res.status === 401 || result.message === 'unauthorized') {
                setTimeout(() => window.location.href = '/login', 1500);
            }
        }
    } catch (error) {
        console.error('Lỗi khi thêm vào giỏ hàng:', error);
        showToast('error', 'Lỗi mạng', 'Không thể kết nối đến server.');
    }
}

/**
 * Chuyển hướng đến trang chi tiết sản phẩm dựa trên Serial Number.
 * @param {number} productId - ID của sản phẩm (dùng để tìm Serial Number).
 */
function viewProduct(productId) {
    const serialNumber = getProductSerialNumberFromDOM(productId);

    if (!serialNumber) {
        showToast('error', 'Lỗi', 'Không tìm thấy mã Serial Number để xem chi tiết.');
        return;
    }

    const productUrl = `/products/detail/${serialNumber}`;
    showToast('info', 'Chuyển hướng', `Đang chuyển đến trang chi tiết sản phẩm...`);

    setTimeout(() => {
        window.location.href = productUrl;
    }, 500);
}