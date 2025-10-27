    function formatVND(num){
        return new Intl.NumberFormat('vi-VN').format(Number(num||0)) + ' đ';
    }

    function getSelectedItemIds(){
        return Array.from(document.querySelectorAll('.list-group-item'))
            .filter(el => el.querySelector('.item-check')?.checked)
            .map(el => el.dataset.itemId);
    }
    async function updateState(itemEl, newQty){
        // Nếu không truyền itemEl (VD khi chỉ tick/untick) ta lấy tạm item đầu tiên để gửi
        if (!itemEl) itemEl = document.querySelector('.list-group-item');
        if (!itemEl) return;

        const itemId    = itemEl.dataset.itemId;
        const qtyInput  = itemEl.querySelector('.qty-input');
        const curQty    = Math.max(1, Number(qtyInput?.value || 1));
        const qty       = (newQty == null) ? curQty : Math.max(1, Number(newQty));

        const csrfName  = document.getElementById('csrfName')?.value;
        const csrfToken = document.getElementById('csrfToken')?.value;

        const body = new URLSearchParams({ itemId, qty });
        // gửi kèm DS item đang tick để server tính tổng đúng
        const selected = getSelectedItemIds();
        selected.forEach(id => body.append('selectedIds', id));
        if (csrfName && csrfToken) body.set(csrfName, csrfToken);

        const res  = await fetch('/cart/update', {
            method: 'POST',
            headers: {'Content-Type': 'application/x-www-form-urlencoded'},
            body
        });
        const data = await res.json();

        if (data?.ok) {
            // Cập nhật dòng đã thao tác
            const lineQtyInput = itemEl.querySelector('.qty-input');
            const lineTotalEl  = itemEl.querySelector('.line-total');
            if (lineQtyInput) lineQtyInput.value = data.qty;
            if (lineTotalEl)  lineTotalEl.innerText = formatVND(data.lineTotal);

            // Cập nhật tổng
            const subEl = document.getElementById('subtotal');
            const disEl = document.getElementById('discount');
            const totEl = document.getElementById('total');
            if (subEl) subEl.innerText = formatVND(data.subtotal);
            if (disEl) disEl.innerText = formatVND(data.discount);
            if (totEl) totEl.innerText = formatVND(data.total);
        } else if (data?.reason === 'unauthenticated') {
            window.location.href = '/login';
        }
    }

    document.addEventListener('DOMContentLoaded', () => {
        // +/- click
        document.addEventListener('click', (e)=>{
            const btn = e.target.closest('.btn-minus, .btn-plus');
            if (!btn) return;

            const itemEl = btn.closest('.list-group-item');
            const input  = itemEl.querySelector('.qty-input');
            let val      = Math.max(1, Number(input.value || 1));
            if (btn.classList.contains('btn-minus')) val = Math.max(1, val - 1);
            if (btn.classList.contains('btn-plus'))  val = val + 1;

            updateState(itemEl, val);
        });

        // Gõ số trực tiếp
        document.addEventListener('change', (e)=>{
            if (!e.target.classList.contains('qty-input')) return;
            const itemEl = e.target.closest('.list-group-item');
            const val    = Math.max(1, Number(e.target.value || 1));
            updateState(itemEl, val);
        });

        // Chọn tất cả
        const checkAll = document.getElementById('checkAll');
        if (checkAll) {
            checkAll.addEventListener('change', ()=>{
                const checks = document.querySelectorAll('.item-check');
                checks.forEach(c => c.checked = checkAll.checked);
                // gọi 1 lần để server tính tổng theo danh sách tick hiện tại
                updateState(null, null);
            });
        }

        // Chọn từng dòng
        document.querySelectorAll('.item-check').forEach(chk=>{
            chk.addEventListener('change', ()=>{
                // sync trạng thái "chọn tất cả"
                if (checkAll) {
                    const all = document.querySelectorAll('.item-check');
                    checkAll.checked = Array.from(all).every(c => c.checked);
                }
                // gọi 1 lần để server tính tổng theo danh sách tick hiện tại
                updateState(null, null);
            });
        });

        // Tính lần đầu (dựa trên mặc định tick)
        updateState(null, null);
    });
