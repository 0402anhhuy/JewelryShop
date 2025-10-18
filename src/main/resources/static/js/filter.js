/**
 * Filter JavaScript - Xử lý bộ lọc sản phẩm động
 */

document.addEventListener('DOMContentLoaded', function() {

    // ===== AUTO SUBMIT FORM KHI CHECKBOX THAY ĐỔI =====
    const filterForm = document.getElementById('filterForm');

    if (filterForm) {
        // Tất cả checkbox trong form
        const checkboxes = filterForm.querySelectorAll('input[type="checkbox"]');
        checkboxes.forEach(checkbox => {
            checkbox.addEventListener('change', function() {
                // Delay nhỏ để UX mượt hơn
                setTimeout(() => {
                    filterForm.submit();
                }, 100);
            });
        });

        // Select bộ sưu tập
        const collectionSelect = filterForm.querySelector('select[name="collectionName"]');
        if (collectionSelect) {
            collectionSelect.addEventListener('change', function() {
                filterForm.submit();
            });
        }
    }

    // ===== XÓA TẤT CẢ FILTER =====
    const clearAllBtn = document.querySelector('.clear-filters, a[href="/products"]');
    if (clearAllBtn) {
        clearAllBtn.addEventListener('click', function(e) {
            if (this.classList.contains('clear-filters')) {
                e.preventDefault();
                window.location.href = '/products';
            }
        });
    }

    // ===== MATERIAL PILLS (NẾU DÙNG PILLS THAY VÌ CHECKBOX) =====
    document.querySelectorAll('.pill').forEach(pill => {
        pill.addEventListener('click', function() {
            this.classList.toggle('active');

            // Tự động submit sau khi chọn
            const form = this.closest('form');
            if (form) {
                // Tạo hidden input cho material đã chọn
                const activePills = form.querySelectorAll('.pill.active');

                // Xóa old hidden inputs
                form.querySelectorAll('input[name="material"][type="hidden"]').forEach(inp => inp.remove());

                // Thêm hidden inputs mới
                activePills.forEach(p => {
                    const input = document.createElement('input');
                    input.type = 'hidden';
                    input.name = 'material';
                    input.value = p.textContent.trim();
                    form.appendChild(input);
                });

                setTimeout(() => form.submit(), 100);
            }
        });
    });

    // ===== CATEGORY FILTER TỪNG SECTION =====
    document.querySelectorAll('.category').forEach(catLink => {
        catLink.addEventListener('click', function(e) {
            // Nếu không có href thì prevent và submit form
            const href = this.getAttribute('href');
            if (!href || href === '#') {
                e.preventDefault();
                const catName = this.textContent.trim();
                window.location.href = `/products?catName=${encodeURIComponent(catName)}`;
            }
        });
    });

    // ===== COLLECTION FILTER =====
    document.querySelectorAll('.collection-item').forEach(item => {
        item.addEventListener('click', function(e) {
            const collectionId = this.getAttribute('data-collection-id');
            const collectionName = this.querySelector('.collection-name')?.textContent.trim();

            if (collectionName) {
                e.preventDefault();
                window.location.href = `/products?collectionName=${encodeURIComponent(collectionName)}`;
            }
        });
    });

    // ===== PRICE RANGE SLIDER =====
    const priceSlider = document.querySelector('.price-slider');
    const maxPriceInput = document.querySelector('input[name="maxPrice"]');

    if (priceSlider && maxPriceInput) {
        priceSlider.addEventListener('input', function() {
            const value = parseInt(this.value);
            maxPriceInput.value = value;
        });

        // Submit khi mouseup (sau khi kéo xong)
        priceSlider.addEventListener('change', function() {
            const form = this.closest('form');
            if (form) form.submit();
        });
    }

    // ===== SORT DROPDOWN =====
    const sortDropdown = document.querySelector('.sort-dropdown, select[onchange*="updateSort"]');
    if (sortDropdown) {
        sortDropdown.addEventListener('change', function() {
            const sortValue = this.value;
            const url = new URL(window.location.href);
            url.searchParams.set('sort', sortValue);
            url.searchParams.set('page', '0'); // Reset về trang 1
            window.location.href = url.toString();
        });
    }

    // ===== LOADING STATE CHO FORM =====
    if (filterForm) {
        filterForm.addEventListener('submit', function() {
            const submitBtn = this.querySelector('button[type="submit"]');
            if (submitBtn) {
                submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang lọc...';
                submitBtn.disabled = true;
            }
        });
    }

    // ===== ACTIVE STATE CHO PAGINATION =====
    document.querySelectorAll('.page-btn').forEach(btn => {
        btn.addEventListener('click', function(e) {
            // Smooth scroll lên đầu trang
            if (!this.classList.contains('disabled')) {
                window.scrollTo({ top: 0, behavior: 'smooth' });
            }
        });
    });

    // ===== HOVER EFFECT CHO PRODUCT CARDS =====
    document.querySelectorAll('.product-card').forEach(card => {
        card.addEventListener('mouseenter', function() {
            const actions = this.querySelector('.quick-actions');
            if (actions) actions.style.opacity = '1';
        });

        card.addEventListener('mouseleave', function() {
            const actions = this.querySelector('.quick-actions');
            if (actions) actions.style.opacity = '0';
        });
    });

    // ===== WISHLIST TOGGLE =====
    document.querySelectorAll('.action-btn').forEach(btn => {
        const icon = btn.querySelector('.fa-heart');
        if (icon) {
            btn.addEventListener('click', function(e) {
                e.preventDefault();
                icon.classList.toggle('far');
                icon.classList.toggle('fas');

                if (icon.classList.contains('fas')) {
                    this.style.color = '#e74c3c';
                    // Có thể call API lưu wishlist ở đây
                } else {
                    this.style.color = '#333';
                }
            });
        }
    });

    // ===== HIỂN thị SỐ LƯỢNG FILTER ĐANG ACTIVE =====
    function updateFilterCount() {
        const activeFilters = document.querySelectorAll('input[type="checkbox"]:checked').length;
        const filterTitle = document.querySelector('.filter-header h3');

        if (filterTitle && activeFilters > 0) {
            filterTitle.innerHTML = `<i class="fas fa-filter"></i> Bộ Lọc <span class="badge bg-warning text-dark ms-2">${activeFilters}</span>`;
        } else if (filterTitle) {
            filterTitle.innerHTML = '<i class="fas fa-filter"></i> Bộ Lọc';
        }
    }

    // Gọi khi trang load
    updateFilterCount();

    // Gọi khi checkbox thay đổi
    document.querySelectorAll('input[type="checkbox"]').forEach(cb => {
        cb.addEventListener('change', updateFilterCount);
    });
});

/**
 * Helper function: Update sort parameter
 */
function updateSort(sortValue) {
    const url = new URL(window.location.href);
    url.searchParams.set('sort', sortValue);
    url.searchParams.set('page', '0');
    window.location.href = url.toString();
}

/**
 * Helper function: Build filter URL
 */
function buildFilterUrl(params) {
    const url = new URL(window.location.origin + '/products');
    Object.keys(params).forEach(key => {
        if (Array.isArray(params[key])) {
            params[key].forEach(val => url.searchParams.append(key, val));
        } else if (params[key]) {
            url.searchParams.set(key, params[key]);
        }
    });
    return url.toString();
}

/**
 * Helper function: Highlight active filters
 */
function highlightActiveFilters() {
    const urlParams = new URLSearchParams(window.location.search);

    // Highlight categories
    const selectedCats = urlParams.getAll('catName');
    selectedCats.forEach(cat => {
        const checkbox = document.querySelector(`input[name="catName"][value="${cat}"]`);
        if (checkbox) checkbox.checked = true;
    });

    // Highlight materials
    const selectedMats = urlParams.getAll('material');
    selectedMats.forEach(mat => {
        const checkbox = document.querySelector(`input[name="material"][value="${mat}"]`);
        if (checkbox) checkbox.checked = true;

        const pill = Array.from(document.querySelectorAll('.pill'))
            .find(p => p.textContent.trim() === mat);
        if (pill) pill.classList.add('active');
    });

    // Highlight collection
    const selectedColl = urlParams.get('collectionName');
    if (selectedColl) {
        const select = document.querySelector('select[name="collectionName"]');
        if (select) select.value = selectedColl;
    }
}

// Call on page load
document.addEventListener('DOMContentLoaded', highlightActiveFilters);