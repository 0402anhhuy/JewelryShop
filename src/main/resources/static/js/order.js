// =========================================================
// 5. Module ORDER (order.js)
// Yêu cầu: import utils.js, và hàm closeAddressModal (từ address.js)
// =========================================================

const orderDetailModal = document.getElementById('orderDetailModal');

// --- Order List Actions ---
function cancelOrder(id) {
    showToast('warning', 'Xác nhận hủy đơn', 'Bạn có chắc muốn hủy đơn hàng #' + id + '?', true, () => {
        const token = getCsrfToken();
        const header = document.querySelector("meta[name='_csrf_header']").getAttribute("content");

        fetch(`/orders/delete/${id}`, {
            method: 'POST',
            headers: { [header]: token }
        })
            .then(response => {
                if (response.ok) {
                    showToast('success', 'Đã hủy', 'Đơn hàng #' + id + ' đã được hủy thành công!');
                    location.reload();
                } else {
                    showToast('fail', 'Thất bại', 'Không thể xóa đơn hàng này. Có thể đơn hàng đã được xử lý.');
                }
            })
            .catch(error => {
                console.error('Lỗi khi gửi request:', error);
                showToast('fail', 'Thất bại', 'Đã xảy ra lỗi kết nối. Vui lòng thử lại!');
            });
    });
}
function confirmDelivered(id){
    showToast('warning',
        'Xác nhận đã nhận hàng',
        'Bạn có chắc đã nhận được đơn hàng #' + id + '? Đơn hàng sẽ được chuyển sang trạng thái "Hoàn thành".',
        true,
        () => {
            const token = document.querySelector("meta[name='_csrf']").getAttribute("content");
            const header = document.querySelector("meta[name='_csrf_header']").getAttribute("content");
            showToast('info', 'Đang xử lý', 'Đang xác nhận đơn hàng của bạn...');
            fetch(`/orders/confirm-delivery/${id}`, {
                method: 'POST',
                headers: {
                    [header]: token // Gửi kèm CSRF token
                }
            })
                .then(response => {
                    if (response.ok) {
                        // 4. Thành công
                        showToast('success', 'Thành công', 'Đã xác nhận nhận hàng. Đơn hàng #' + id + ' đã hoàn thành!');

                        location.reload();
                    } else {
                        // 5. Lỗi từ server (ví dụ: đơn hàng không hợp lệ)
                        showToast('error', 'Thất bại', 'Không thể xác nhận đơn hàng. Vui lòng thử lại.');
                    }
                })
                .catch(error => {
                    // 6. Lỗi mạng
                    console.error('Lỗi khi gửi request xác nhận:', error);
                    showToast('error', 'Lỗi kết nối', 'Đã xảy ra lỗi kết nối. Vui lòng thử lại!');
                });
        }
    );
}

function reorder(id) {
    showToast('info', 'Mua lại', 'Đang thêm sản phẩm từ đơn #' + id + ' vào giỏ hàng...');

    const token = getCsrfToken();
    const header = document.querySelector("meta[name='_csrf_header']").getAttribute("content");

    fetch(`/cart/reorder/${id}`, {
        method: 'POST',
        headers: { [header]: token, 'Content-Type': 'application/json' }
    })
        .then(response => {
            if (!response.ok) {
                return response.json().then(errData => {
                    throw new Error(errData.message || 'Không thể thêm sản phẩm vào giỏ.');
                });
            }
            return response.json();
        })
        .then(data => {
            showToast('success', 'Thành công', data.message || 'Đã thêm sản phẩm vào giỏ hàng thành công!');
            window.location.href = '/cart';
        })
        .catch(error => {
            console.error('Lỗi khi mua lại:', error);
            showToast('error', 'Thất bại', error.message || 'Đã xảy ra lỗi. Vui lòng thử lại!');
        });
}

function reviewOrder(id) {
    showToast('info', 'Đánh giá', 'Chuyển hướng đến trang đánh giá cho đơn #' + id);
    window.location.href = '/reviews/order/' + id;
}

// --- Order Detail Modal ---
function closeOrderDetailModal() {
    if (orderDetailModal) {
        orderDetailModal.classList.remove('show');
        document.body.style.overflow = 'auto';
    }
}

async function showOrderDetailModal(orderId) {
    if (!orderDetailModal) {
        console.error("Order detail modal not found!");
        return;
    }

    orderDetailModal.classList.add('show');
    document.body.style.overflow = 'hidden';

    const modalOrderIdEl = orderDetailModal.querySelector('.order-id-section h2');
    const modalOrderDateEl = orderDetailModal.querySelector('#modalOrderDate');
    const modalOrderStatusBadgeEl = orderDetailModal.querySelector('#modalOrderStatusBadge');
    const modalConsigneeEl = orderDetailModal.querySelector('#modalOrderConsignee');
    const modalPhoneEl = orderDetailModal.querySelector('#modalOrderPhone');
    const modalAddressEl = orderDetailModal.querySelector('#modalOrderAddress');
    const modalNoteContainerEl = orderDetailModal.querySelector('#modalOrderNote')?.parentElement;
    const modalNoteEl = orderDetailModal.querySelector('#modalOrderNote');
    const modalPaymentMethodEl = orderDetailModal.querySelector('#modalOrderPaymentMethod');
    const modalPaymentStatusEl = orderDetailModal.querySelector('#modalOrderPaymentStatus');
    const modalPaymentStatusIconEl = modalPaymentStatusEl?.querySelector('i');
    const modalPaymentStatusTextEl = modalPaymentStatusEl?.querySelector('span');
    const modalItemsContainerEl = orderDetailModal.querySelector('#modalOrderItems');
    const modalSubtotalEl = orderDetailModal.querySelector('#modalSubtotal');
    const modalShippingFeeEl = orderDetailModal.querySelector('#modalShippingFee');
    const modalDiscountContainerEl = orderDetailModal.querySelector('#modalDiscount')?.parentElement;
    const modalDiscountEl = orderDetailModal.querySelector('#modalDiscount');
    const modalFinalTotalEl = orderDetailModal.querySelector('#modalFinalTotal');
    const modalVatEl = orderDetailModal.querySelector('#modalVat');
    const modalCancelButton = orderDetailModal.querySelector('#modalCancelButton');
    const modalTimelineContainerEl = orderDetailModal.querySelector('#modalOrderTimeline');

    if (modalOrderIdEl) modalOrderIdEl.innerHTML = `<i class="fas fa-receipt"></i> Đơn hàng #${orderId}`;
    if (modalOrderDateEl) modalOrderDateEl.textContent = 'Đang tải...';
    if (modalOrderStatusBadgeEl) {
        modalOrderStatusBadgeEl.className = 'order-status-badge';
        modalOrderStatusBadgeEl.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang tải...';
    }
    if (modalItemsContainerEl) modalItemsContainerEl.innerHTML = '<p style="text-align: center; padding: 20px;">Đang tải sản phẩm...</p>';
    if (modalTimelineContainerEl) modalTimelineContainerEl.innerHTML = '<p style="text-align: center; padding: 10px;">Đang tải lịch sử...</p>';

    try {
        const response = await fetch(`/account/orders/detail/${orderId}`);
        if (!response.ok) {
            throw new Error(`Lỗi ${response.status}: ${response.statusText || 'Không thể tải dữ liệu'}`);
        }
        const orderData = await response.json();

        // Header
        if (modalOrderIdEl) modalOrderIdEl.innerHTML = `<i class="fas fa-receipt"></i> Đơn hàng #${orderData.id}`;
        if (modalOrderDateEl) modalOrderDateEl.textContent = formatDateTime(orderData.createdAt);
        if (modalOrderStatusBadgeEl) {
            modalOrderStatusBadgeEl.className = 'order-status-badge';
            if(orderData.status) {
                modalOrderStatusBadgeEl.classList.add(`status-${orderData.status.toLowerCase()}`);
            }
            let statusIcon = 'fa-question-circle';
            if(orderData.status === 'PENDING') statusIcon = 'fa-clock';
            else if(orderData.status === 'PAID') statusIcon = 'fa-cogs';
            else if(orderData.status === 'SHIPPING') statusIcon = 'fa-shipping-fast';
            else if(orderData.status === 'DELIVERED') statusIcon = 'fa-check-circle';
            else if(orderData.status === 'CANCELLED') statusIcon = 'fa-times-circle';
            else if(orderData.status === 'RETURNED') statusIcon = 'fa-undo';
            const statusText = getDisplayStatusText(orderData.status);
            modalOrderStatusBadgeEl.innerHTML = `<i class="fas ${statusIcon}"></i> <span>${statusText || 'N/A'}</span>`;
        }

        // Thông tin giao hàng
        if (modalConsigneeEl) modalConsigneeEl.textContent = orderData.consignee || 'N/A';
        if (modalPhoneEl) modalPhoneEl.textContent = orderData.shippingPhone || 'N/A';
        const addressParts = [
            orderData.shippingAddressLine, orderData.shippingWard,
            orderData.shippingDistrict, orderData.shippingCity
        ].filter(part => part && part.trim() !== '').join(', ');
        if (modalAddressEl) modalAddressEl.textContent = addressParts || 'N/A';

        if (modalNoteContainerEl) {
            if (orderData.note && orderData.note.trim() !== '') {
                if (modalNoteEl) modalNoteEl.textContent = orderData.note;
                modalNoteContainerEl.style.display = 'block';
            } else {
                modalNoteContainerEl.style.display = 'none';
            }
        }

        // Thông tin thanh toán
        if (modalPaymentMethodEl) modalPaymentMethodEl.textContent = orderData.methodPay || 'N/A';
        if (modalPaymentStatusEl) {
            if (orderData.paid) {
                modalPaymentStatusEl.style.color = 'var(--success-color)';
                if(modalPaymentStatusIconEl) modalPaymentStatusIconEl.className = 'fas fa-check-circle';
                if(modalPaymentStatusTextEl) modalPaymentStatusTextEl.textContent = ' Đã thanh toán';
            } else {
                modalPaymentStatusEl.style.color = 'var(--warning-color)';
                if(modalPaymentStatusIconEl) modalPaymentStatusIconEl.className = 'fas fa-clock';
                if(modalPaymentStatusTextEl) modalPaymentStatusTextEl.textContent = ' Chưa thanh toán / Chờ xử lý';
            }
        }

        // Danh sách sản phẩm
        if (modalItemsContainerEl) {
            modalItemsContainerEl.innerHTML = '';
            if (orderData.items && orderData.items.length > 0) {
                orderData.items.forEach(item => {
                    const itemDiv = document.createElement('div');
                    itemDiv.className = 'product-item';
                    const product = item.product || {};
                    const imageUrl = product.imageUrls?.[0] || '/images/default-product.png';
                    const itemQuantity = item.quantity || 0;
                    const itemTotalPrice = item.totalPrice || 0;
                    const displayNameParts = [product.name, product.gemstone, product.material];
                    const productName = displayNameParts.filter(part => part && String(part).trim() !== '').join(' ') || 'Sản phẩm không xác định';

                    itemDiv.innerHTML = `
                    <div class="product-image"><img src="${imageUrl}" alt="${productName}"></div>
                    <div class="product-info">
                        <div><div class="product-name">${productName}</div>
                        <div class="product-details"><span class="product-detail-item">
                            <i class="fas fa-cube"></i> Số lượng: ${itemQuantity}
                        </span></div></div>
                        <div class="product-price">${formatCurrency(itemTotalPrice)}</div>
                    </div>`;
                    modalItemsContainerEl.appendChild(itemDiv);
                });
            } else {
                modalItemsContainerEl.innerHTML = '<p style="text-align: center; padding: 15px;">Đơn hàng không có sản phẩm nào.</p>';
            }
        }

        // Lịch sử đơn hàng (Timeline)
        if(modalTimelineContainerEl){
            modalTimelineContainerEl.innerHTML = '';
            const baseFlow = ['PENDING', 'PAID', 'SHIPPING', 'DELIVERED', 'COMPLETED'];
            const currentStatus = orderData.status;
            let relevantFlow = [...baseFlow];

            if (currentStatus === 'CANCELLED') {
                let lastGoodStatusIndex = -1;
                if (orderData.statusHistory && orderData.statusHistory.length > 1) {
                    const lastGoodStatus = orderData.statusHistory[orderData.statusHistory.length - 2].status;
                    lastGoodStatusIndex = baseFlow.indexOf(lastGoodStatus);
                }
                relevantFlow = baseFlow.slice(0, lastGoodStatusIndex + 1);
                relevantFlow.push('CANCELLED');
            } else if (currentStatus === 'RETURNED') {
                relevantFlow.push('RETURNED');
            }

            const currentStepIndex = relevantFlow.indexOf(currentStatus);
            const historyMap = new Map();
            if (orderData.statusHistory) {
                orderData.statusHistory.forEach(item => { historyMap.set(item.status, { timestamp: item.timestamp, }); });
            }

            relevantFlow.forEach((status, index) => {
                const timelineItemDiv = document.createElement('div');
                timelineItemDiv.className = 'timeline-item';
                let dotClass = '';
                let timelineTime = 'Chưa cập nhật';
                const historyData = historyMap.get(status);
                if (historyData) {
                    timelineTime = formatDateTime(historyData.timestamp);
                }

                if (index < currentStepIndex) {
                    dotClass = 'completed';
                } else if (index === currentStepIndex) {
                    if (currentStatus === 'PENDING') { dotClass = ''; }
                    else if (['PAID', 'SHIPPING'].includes(currentStatus)) { dotClass = ''; }
                    else { dotClass = 'completed'; }
                } else {
                    dotClass = 'pending';
                }

                const timelineTitle = getDisplayStatusText(status);

                timelineItemDiv.innerHTML = `
                    <div class="timeline-dot ${dotClass}"></div>
                    <div class="timeline-content">
                        <div class="timeline-title">${timelineTitle}</div>
                        <div class="timeline-time">${timelineTime}</div>
                    </div>
                `;
                modalTimelineContainerEl.appendChild(timelineItemDiv);
            });
        }

        // Tổng kết đơn hàng
        const subtotal = ((orderData.totalAmount || 0) - (orderData.shippingFee || 0))/(1.1*0.8);
        const vatAmount = (subtotal - (orderData.discount || 0))*0.1;
        if (modalSubtotalEl) modalSubtotalEl.textContent = formatCurrency(subtotal);
        if (modalShippingFeeEl) modalShippingFeeEl.textContent = formatCurrency(orderData.shippingFee || 0);
        if (modalDiscountContainerEl) {
            if (orderData.discount && orderData.discount > 0) {
                if (modalDiscountEl) modalDiscountEl.textContent = `- ${formatCurrency(orderData.discount)}`;
                modalDiscountContainerEl.style.display = 'flex';
            } else {
                modalDiscountContainerEl.style.display = 'none';
            }
        }
        if (modalFinalTotalEl) modalFinalTotalEl.textContent = formatCurrency(orderData.totalAmount || 0);
        if (modalVatEl) modalVatEl.textContent = formatCurrency(vatAmount);

        if(modalCancelButton){
            if (['PENDING', 'PAID'].includes(orderData.status)) {
                modalCancelButton.style.display = 'inline-flex';
                modalCancelButton.onclick = () => cancelOrder(orderId);
            } else {
                modalCancelButton.style.display = 'none';
            }
        }


    } catch (error) {
        console.error('Lỗi khi tải chi tiết đơn hàng:', error);
        if (modalItemsContainerEl) modalItemsContainerEl.innerHTML = `<p style='color: var(--danger-color); text-align: center; padding: 20px;'>${error.message}</p>`;
        if (modalOrderDateEl) modalOrderDateEl.textContent = 'Lỗi';
        if (modalOrderStatusBadgeEl) modalOrderStatusBadgeEl.innerHTML = '<i class="fas fa-exclamation-triangle"></i> Lỗi';
        showToast('error', 'Lỗi', `Không thể tải chi tiết đơn hàng: ${error.message}`);
    }
}

// --- Global Modal Handlers ---
window.onclick = function(event) {
    if (event.target === orderDetailModal) {
        closeOrderDetailModal();
    }
    const addressModal = document.getElementById('addressModal');
    if (event.target === addressModal && typeof closeAddressModal === 'function') {
        closeAddressModal();
    }
}

document.addEventListener('keydown', function(event) {
    if (event.key === 'Escape') {
        closeOrderDetailModal();
    }
});