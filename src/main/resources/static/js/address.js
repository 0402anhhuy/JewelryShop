// =========================================================
// 2. Module ADDRESS (address.js)
// Yêu cầu: Đã có showToast và getCsrfToken (từ utils.js)
// =========================================================

const apiUrl = 'https://provinces.open-api.vn/api';

// ----------------- ADDRESS PICKER FUNCTIONS -----------------

function resetSelect(select, placeholder) {
    if (!select) return;
    select.innerHTML = `<option value="">${placeholder}</option>`;
    select.disabled = true;
}

// Hàm tự động điều chỉnh chiều cao textarea
function autoResizeAddress() {
    const fullAddressInput = document.getElementById('fullAddress');
    if (!fullAddressInput) return;

    fullAddressInput.style.height = '50px';

    const scrollHeight = fullAddressInput.scrollHeight;

    if (scrollHeight > 50) {
        fullAddressInput.style.height = scrollHeight + 'px';
    }
}

// Cập nhật hàm updateFullAddress để gọi autoResizeAddress
function updateFullAddress() {
    const addressLine = document.getElementById('addressLine')?.value || '';
    const wardSelect = document.getElementById('ward');
    const districtSelect = document.getElementById('district');
    const citySelect = document.getElementById('city');

    const ward = wardSelect?.options[wardSelect.selectedIndex]?.text;
    const district = districtSelect?.options[districtSelect.selectedIndex]?.text;
    const city = citySelect?.options[citySelect.selectedIndex]?.text;

    let fullAddress = '';
    if (addressLine) fullAddress += addressLine;
    if (ward && ward !== '- Chọn Phường/Xã -') fullAddress += (fullAddress ? ', ' : '') + ward;
    if (district && district !== '- Chọn Quận/Huyện -') fullAddress += (fullAddress ? ', ' : '') + district;
    if (city && city !== '- Chọn Tỉnh/Thành -') fullAddress += (fullAddress ? ', ' : '') + city;

    const fullAddressInput = document.getElementById('fullAddress');
    if (fullAddressInput) {
        fullAddressInput.value = fullAddress;
        autoResizeAddress();
    }
}

async function loadCities() {
    const cityEl = document.getElementById('city');
    const districtEl = document.getElementById('district');
    const wardEl = document.getElementById('ward');

    if (!cityEl) return;

    try {
        resetSelect(cityEl, '- Đang tải... -');
        resetSelect(districtEl, '- Chọn Quận/Huyện -');
        resetSelect(wardEl, '- Chọn Phường/Xã -');

        const response = await fetch(`${apiUrl}/?depth=1`);
        const provinces = await response.json();

        cityEl.innerHTML = '<option value="">- Chọn Tỉnh/Thành -</option>';

        provinces.forEach(province => {
            const option = document.createElement('option');
            option.value = province.name;
            option.textContent = province.name;
            option.dataset.code = province.code;
            cityEl.appendChild(option);
        });

        cityEl.disabled = false;
    } catch (error) {
        console.error('Lỗi khi load tỉnh/thành:', error);
        if (cityEl) {
            cityEl.innerHTML = '<option value="">- Lỗi tải dữ liệu -</option>';
        }
    }
}

async function loadDistricts(provinceCode) {
    const districtEl = document.getElementById('district');
    const wardEl = document.getElementById('ward');

    if (!districtEl) return;

    try {
        resetSelect(districtEl, '- Đang tải... -');
        resetSelect(wardEl, '- Chọn Phường/Xã -');

        if (!provinceCode) {
            resetSelect(districtEl, '- Chọn Quận/Huyện -');
            updateFullAddress();
            return;
        }

        const response = await fetch(`${apiUrl}/p/${provinceCode}?depth=2`);
        const province = await response.json();

        districtEl.innerHTML = '<option value="">- Chọn Quận/Huyện -</option>';

        province.districts.forEach(district => {
            const option = document.createElement('option');
            option.value = district.name;
            option.textContent = district.name;
            option.dataset.code = district.code;
            districtEl.appendChild(option);
        });

        districtEl.disabled = false;
        updateFullAddress();
    } catch (error) {
        console.error('Lỗi khi load quận/huyện:', error);
        if (districtEl) {
            districtEl.innerHTML = '<option value="">- Lỗi tải dữ liệu -</option>';
        }
    }
}

async function loadWards(districtCode) {
    const wardEl = document.getElementById('ward');

    if (!wardEl) return;

    try {
        resetSelect(wardEl, '- Đang tải... -');

        if (!districtCode) {
            resetSelect(wardEl, '- Chọn Phường/Xã -');
            updateFullAddress();
            return;
        }

        const response = await fetch(`${apiUrl}/d/${districtCode}?depth=2`);
        const district = await response.json();

        wardEl.innerHTML = '<option value="">- Chọn Phường/Xã -</option>';

        district.wards.forEach(ward => {
            const option = document.createElement('option');
            option.value = ward.name;
            option.textContent = ward.name;
            wardEl.appendChild(option);
        });

        wardEl.disabled = false;
        updateFullAddress();
    } catch (error) {
        console.error('Lỗi khi load phường/xã:', error);
        if (wardEl) {
            wardEl.innerHTML = '<option value="">- Lỗi tải dữ liệu -</option>';
        }
    }
}

// Hàm khởi tạo address picker với dữ liệu có sẵn (cho edit)
function initAddressPickerForEdit(address) {
    const cityEl = document.getElementById('city');
    const districtEl = document.getElementById('district');
    const wardEl = document.getElementById('ward');

    if (!cityEl || !districtEl || !wardEl) return;

    // Remove old listeners (Bằng cách cloneNode, tránh lỗi listeners kép)
    const newCityEl = cityEl.cloneNode(true);
    const newDistrictEl = districtEl.cloneNode(true);
    const newWardEl = wardEl.cloneNode(true);

    cityEl.parentNode.replaceChild(newCityEl, cityEl);
    districtEl.parentNode.replaceChild(newDistrictEl, districtEl);
    wardEl.parentNode.replaceChild(newWardEl, wardEl);

    // Add event listeners
    document.getElementById('city').addEventListener('change', function() {
        const selectedOption = this.options[this.selectedIndex];
        const provinceCode = selectedOption?.dataset.code;
        loadDistricts(provinceCode);
    });

    document.getElementById('district').addEventListener('change', function() {
        const selectedOption = this.options[this.selectedIndex];
        const districtCode = selectedOption?.dataset.code;
        loadWards(districtCode);
    });

    document.getElementById('ward').addEventListener('change', updateFullAddress);

    const addressLineEl = document.getElementById('addressLine');
    if (addressLineEl) {
        const newAddressLineEl = addressLineEl.cloneNode(true);
        addressLineEl.parentNode.replaceChild(newAddressLineEl, addressLineEl);
        document.getElementById('addressLine').addEventListener('input', updateFullAddress);
    }

    // Load cities và chọn giá trị từ address
    loadCities().then(() => {
        if (address.city) {
            const citySelect = document.getElementById('city');
            for (let i = 0; i < citySelect.options.length; i++) {
                if (citySelect.options[i].text === address.city) {
                    citySelect.selectedIndex = i;
                    const provinceCode = citySelect.options[i].dataset.code;

                    loadDistricts(provinceCode).then(() => {
                        if (address.district) {
                            const districtSelect = document.getElementById('district');
                            for (let j = 0; j < districtSelect.options.length; j++) {
                                if (districtSelect.options[j].text === address.district) {
                                    districtSelect.selectedIndex = j;
                                    const districtCode = districtSelect.options[j].dataset.code;

                                    loadWards(districtCode).then(() => {
                                        if (address.ward) {
                                            const wardSelect = document.getElementById('ward');
                                            for (let k = 0; k < wardSelect.options.length; k++) {
                                                if (wardSelect.options[k].text === address.ward) {
                                                    wardSelect.selectedIndex = k;
                                                    break;
                                                }
                                            }
                                        }
                                        updateFullAddress();
                                    });
                                    break;
                                }
                            }
                        }
                    });
                    break;
                }
            }
        }
        updateFullAddress();
    });
}

function initAddressPickerForModal() {
    const cityEl = document.getElementById('city');
    const districtEl = document.getElementById('district');
    const wardEl = document.getElementById('ward');
    const addressLineEl = document.getElementById('addressLine');

    if (!cityEl || !districtEl || !wardEl) return;

    // Remove old listeners bằng cách clone
    const newCityEl = cityEl.cloneNode(true);
    const newDistrictEl = districtEl.cloneNode(true);
    const newWardEl = wardEl.cloneNode(true);

    cityEl.parentNode.replaceChild(newCityEl, cityEl);
    districtEl.parentNode.replaceChild(newDistrictEl, districtEl);
    wardEl.parentNode.replaceChild(newWardEl, wardEl);

    // Add event listeners
    document.getElementById('city').addEventListener('change', function() {
        const selectedOption = this.options[this.selectedIndex];
        const provinceCode = selectedOption?.dataset.code;
        loadDistricts(provinceCode);
    });

    document.getElementById('district').addEventListener('change', function() {
        const selectedOption = this.options[this.selectedIndex];
        const districtCode = selectedOption?.dataset.code;
        loadWards(districtCode);
    });

    document.getElementById('ward').addEventListener('change', updateFullAddress);

    if (addressLineEl) {
        const newAddressLineEl = addressLineEl.cloneNode(true);
        addressLineEl.parentNode.replaceChild(newAddressLineEl, addressLineEl);
        document.getElementById('addressLine').addEventListener('input', updateFullAddress);
    }

    // Ẩn checkbox đặt làm mặc định khi thêm mới
    const defaultCheckbox = document.querySelector('[name="isDefault"]');
    if (defaultCheckbox) {
        defaultCheckbox.closest('.form-group').style.display = 'none';
    }

    // Load cities khi mở modal
    loadCities();
}

// ----------------- MODAL & CRUD HANDLERS -----------------

// Hàm đóng modal (Cần export để Order Module gọi)
function closeAddressModal() {
    const modal = document.getElementById('addressModal');
    const form = document.getElementById('addressForm');

    modal.style.display = 'none';

    if (form) {
        form.reset();
        form.action = '/account/address/add-ajax';
        document.getElementById('addressModalTitle').textContent = 'Thêm địa chỉ mới';

        form.querySelectorAll('.error').forEach(input => {
            input.classList.remove('error');
            input.style.borderColor = '';
        });
    }
}

// Hàm mở modal thêm địa chỉ
function showAddAddressModal() {
    const modal = document.getElementById('addressModal');
    const form = document.getElementById('addressForm');

    if (form) {
        form.reset();
        form.querySelectorAll('.error').forEach(input => {
            input.classList.remove('error');
            input.style.borderColor = '';
        });
        form.action = '/account/address/add-ajax';
        document.getElementById('addressModalTitle').textContent = 'Thêm địa chỉ mới';
    }

    const defaultCheckbox = form.querySelector('[name="isDefault"]');
    if (defaultCheckbox) {
        defaultCheckbox.checked = false;
        defaultCheckbox.closest('.form-group').style.display = 'none';
    }

    initAddressPickerForModal();

    modal.style.display = 'flex';
}


// Hàm submit form (dùng cho cả Add và Edit)
function saveAddress(event) {
    event.preventDefault();

    const form = event.target;
    const formData = new FormData(form);

    const submitBtn = form.querySelector('button[type="submit"]');
    const originalText = submitBtn.innerHTML;
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang lưu...';

    const csrfToken = document.querySelector('input[name="_csrf"]').value;

    const isEdit = form.action.includes('/edit/');
    const addressId = isEdit ? form.action.match(/\/edit\/(\d+)/)?.[1] : null;

    fetch(form.action, {
        method: 'POST',
        headers: {
            'X-CSRF-TOKEN': csrfToken,
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: new URLSearchParams(formData).toString()
    })
        .then(response => {
            if (!response.ok) {
                return response.json().then(errorData => { throw new Error(errorData.message || 'Lỗi mạng'); });
            }
            return response.json();
        })
        .then(result => {
            showToast(result.toastType, result.toastTitle, result.toastMessage);

            if (result.success) {
                closeAddressModal();
                form.reset();

                if (isEdit && addressId) {
                    if (result.data) {
                        updateAddressCardInGrid(addressId, result.data);
                    }
                    if (result.data && result.data.isDefault) {
                        updateDefaultAddressUI(result.data.id);
                    }
                } else {
                    if (result.data) {
                        addAddressCardToGrid(result.data);
                        if (result.data.isDefault) {
                            updateDefaultAddressUI(result.data.id);
                        }
                    }
                }
            } else {
                if (result.errors) {
                    Object.keys(result.errors).forEach(field => {
                        const input = form.querySelector(`[name="${field}"]`);
                        if (input) {
                            input.classList.add('error');
                            input.style.borderColor = 'var(--danger-color)';
                        }
                    });
                }
            }
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('error', 'Lỗi', error.message || 'Không thể lưu địa chỉ. Vui lòng thử lại!');
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalText;
        });
}

// Hàm thêm địa chỉ mới vào grid
function addAddressCardToGrid(address) {
    const addressGrid = document.querySelector('.address-grid');
    if (!addressGrid) return;

    const isDefault = address.isDefault !== undefined ? address.isDefault : false;

    const addressCard = document.createElement('div');
    addressCard.className = 'address-card';
    addressCard.setAttribute('data-address-id', String(address.id));

    if (isDefault) {
        addressCard.classList.add('default');
        updateDefaultAddressUI(address.id);
    }

    const addressTypeText = address.addressType === 'home' ? 'Nhà riêng' :
        (address.addressType === 'office' ? 'Văn phòng' : 'Khác');

    addressCard.innerHTML = `
        ${isDefault ? '<span class="default-badge"><i class="fas fa-check"></i> Mặc định</span>' : ''}
        <div class="address-name">${address.receiverName || ''}</div>
        <div class="address-phone">
            <i class="fas fa-phone"></i>
            <span>${address.phone || ''}</span>
        </div>
        <div class="address-detail">
            <span>${address.addressLine || ''}</span>
            ${address.ward ? ', ' + address.ward : ''}
            ${address.district ? ', ' + address.district : ''}
            ${address.city ? ', ' + address.city : ''}
        </div>
        <span>${addressTypeText}</span>
        <div class="address-actions">
            <button class="btn btn-sm btn-primary" onclick="editAddress(${address.id})">
                <i class="fas fa-edit"></i> Sửa
            </button>
            <button class="btn btn-sm btn-secondary" onclick="confirmDeleteAddress(${address.id})">
                <i class="fas fa-trash"></i> Xóa
            </button>
        </div>
    `;

    const addButton = addressGrid.querySelector('.add-address-card');
    if (addButton) {
        addressGrid.insertBefore(addressCard, addButton);
    } else {
        addressGrid.appendChild(addressCard);
    }

    addressCard.style.opacity = '0';
    addressCard.style.transform = 'scale(0.9)';
    setTimeout(() => {
        addressCard.style.transition = 'all 0.3s ease';
        addressCard.style.opacity = '1';
        addressCard.style.transform = 'scale(1)';
    }, 10);
}

// Hàm cập nhật card địa chỉ trong grid (dùng khi edit)
function updateAddressCardInGrid(addressId, addressData) {
    const addressCard = document.querySelector(`.address-card[data-address-id="${String(addressId)}"]`);
    if (!addressCard) {
        console.error('Address card not found:', addressId);
        return;
    }

    const isDefault = addressData.isDefault !== undefined ? addressData.isDefault : false;
    const addressTypeText = addressData.addressType === 'home' ? 'Nhà riêng' :
        (addressData.addressType === 'office' ? 'Văn phòng' : 'Khác');

    // Cập nhật class và badge
    if (isDefault) {
        addressCard.classList.add('default');
        if (!addressCard.querySelector('.default-badge')) {
            const defaultBadge = document.createElement('span');
            defaultBadge.className = 'default-badge';
            defaultBadge.innerHTML = '<i class="fas fa-check"></i> Mặc định';
            addressCard.insertBefore(defaultBadge, addressCard.firstChild);
        }
        updateDefaultAddressUI(addressId);
    } else {
        addressCard.classList.remove('default');
        const defaultBadge = addressCard.querySelector('.default-badge');
        if (defaultBadge) {
            defaultBadge.remove();
        }
    }

    const addressName = addressCard.querySelector('.address-name');
    const addressPhone = addressCard.querySelector('.address-phone span');
    const addressDetail = addressCard.querySelector('.address-detail');
    const addressType = addressCard.querySelector('.address-detail + span');

    if (addressName) addressName.textContent = addressData.receiverName || '';
    if (addressPhone) addressPhone.textContent = addressData.phone || '';
    if (addressDetail) {
        addressDetail.innerHTML = `
        <span>${addressData.addressLine || ''}</span>
        ${addressData.ward ? ', ' + addressData.ward : ''}
        ${addressData.district ? ', ' + addressData.district : ''}
        ${addressData.city ? ', ' + addressData.city : ''}
    `;
    }
    if (addressType) addressType.textContent = addressTypeText;

    addressCard.style.transform = 'scale(0.95)';
    setTimeout(() => {
        addressCard.style.transition = 'all 0.3s ease';
        addressCard.style.transform = 'scale(1)';
    }, 10);
}

// Hàm cập nhật giao diện khi đặt địa chỉ mặc định
function updateDefaultAddressUI(newDefaultId) {
    const addressGrid = document.querySelector('.address-grid');
    if (!addressGrid) return;

    const addressCards = addressGrid.querySelectorAll('.address-card');

    const targetId = String(newDefaultId);
    addressCards.forEach(card => {
        const addressId = String(card.getAttribute('data-address-id') || '');
        if (addressId === targetId) {
            card.classList.add('default');
            if (!card.querySelector('.default-badge')) {
                const defaultBadge = document.createElement('span');
                defaultBadge.className = 'default-badge';
                defaultBadge.innerHTML = '<i class="fas fa-check"></i> Mặc định';
                card.insertBefore(defaultBadge, card.firstChild);
            }
        } else {
            card.classList.remove('default');
            const defaultBadge = card.querySelector('.default-badge');
            if (defaultBadge) defaultBadge.remove();
        }
    });
}

function editAddress(id) {
    showToast('info', 'Đang tải', 'Đang tải thông tin địa chỉ...');

    fetch(`/account/address/get/${id}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Không thể tải thông tin địa chỉ');
            }
            return response.json();
        })
        .then(address => {
            const modal = document.getElementById('addressModal');
            const form = document.getElementById('addressForm');

            if (!form || !modal) {
                throw new Error('Không tìm thấy form hoặc modal');
            }

            form.action = `/account/address/edit/${id}`;
            document.getElementById('addressModalTitle').textContent = 'Chỉnh sửa địa chỉ';

            form.querySelector('[name="receiverName"]').value = address.receiverName || '';
            form.querySelector('[name="phone"]').value = address.phone || '';
            form.querySelector('[name="addressType"]').value = address.addressType || 'home';
            form.querySelector('[name="addressLine"]').value = address.addressLine || '';

            const defaultCheckbox = form.querySelector('[name="isDefault"]');
            if (defaultCheckbox) {
                defaultCheckbox.checked = address.isDefault || false;
                defaultCheckbox.closest('.form-group').style.display = 'block';
            }

            initAddressPickerForEdit(address);

            modal.style.display = 'flex';

        })
        .catch(error => {
            console.error('Error loading address:', error);
            showToast('error', 'Lỗi', 'Không thể tải thông tin địa chỉ. Vui lòng thử lại!');
        });
}

function confirmDeleteAddress(id) {
    showToast('warning', 'Xác nhận xóa',
        'Bạn có chắc muốn xóa địa chỉ này? Hành động này không thể hoàn tác.',
        true,
        () => performDeleteAddress(id)
    );
}

function removeAddressCardFromGrid(addressId) {
    const addressCard = document.querySelector(`.address-card[data-address-id="${String(addressId)}"]`);
    if (addressCard) {
        addressCard.style.transform = 'scale(0.9)';
        addressCard.style.opacity = '0';
        setTimeout(() => {
            addressCard.remove();
        }, 300);
    }
}

function performDeleteAddress(id) {
    const csrfToken = getCsrfToken();
    showToast('info', 'Đang xử lý', 'Đang xóa địa chỉ...');

    fetch(`/account/address/delete/${id}`, {
        method: 'POST',
        headers: {
            'X-CSRF-TOKEN': csrfToken,
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: `_csrf=${encodeURIComponent(csrfToken)}`
    })
        .then(response => {
            if (!response.ok) {
                return response.json().then(errorData => { throw new Error(errorData.toastMessage || 'Lỗi mạng'); });
            }
            return response.json();
        })
        .then(result => {
            if (result.success) {
                removeAddressCardFromGrid(id);
                showToast('success', 'Thành công', result.toastMessage || 'Đã xóa địa chỉ thành công!');
                if (result.data && result.data.newDefaultId) {
                    updateDefaultAddressUI(result.data.newDefaultId);
                }
            } else {
                showToast('error', 'Lỗi', result.toastMessage || 'Không thể xóa địa chỉ!');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('error', 'Lỗi', error.message || 'Không thể xóa địa chỉ. Vui lòng thử lại!');
        });
}

function performSetDefaultAddress(id) {
    const csrfToken = getCsrfToken();
    showToast('info', 'Đang xử lý', 'Đang đặt địa chỉ mặc định...');

    fetch(`/account/address/set-default/${id}`, {
        method: 'POST',
        headers: {
            'X-CSRF-TOKEN': csrfToken,
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: `_csrf=${encodeURIComponent(csrfToken)}`
    })
        .then(response => {
            if (!response.ok) {
                return response.json().then(errorData => { throw new Error(errorData.toastMessage || 'Lỗi mạng'); });
            }
            return response.json();
        })
        .then(result => {
            if (result.success) {
                showToast(result.toastType || 'success', result.toastTitle || 'Thành công', result.message || result.toastMessage);
                const newDefaultId = result.data && result.data.id ? result.data.id : id;
                updateDefaultAddressUI(newDefaultId);
            } else {
                showToast(result.toastType || 'error', result.toastTitle || 'Lỗi', result.message || result.toastMessage);
            }
        })
        .catch(error => {
            console.error('Error:', error);
            showToast('error', 'Lỗi', error.message || 'Không thể đặt địa chỉ mặc định. Vui lòng thử lại!');
        });
}