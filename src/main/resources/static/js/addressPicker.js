export function initAddressPicker({
                                      provinceEl, wardEl, streetEl, addressEl,
                                      groupDistrict, groupWard,
                                      provinceNameHidden, districtNameHidden, wardNameHidden
                                  }) {
    const v2 = 'https://provinces.open-api.vn/api/v2';
    const v1 = 'https://provinces.open-api.vn/api';


    if (!provinceEl || !wardEl || !addressEl) return;

    // Ẩn hoàn toàn phần Quận/Huyện
    if (groupDistrict) groupDistrict.style.display = 'none';

    // Cache + Abort
    const cache = { provinces: null, wardsByProvince: new Map() };
    let currentAbort = null;

    const show = (el) => el && el.classList.remove('d-none');
    const hide = (el) => el && el.classList.add('d-none');
    const getText = (sel) =>
        sel && sel.selectedIndex >= 0 ? (sel.options[sel.selectedIndex].text || '') : '';
    const resetSelect = (sel, ph) => {
        sel.innerHTML = `<option value="" hidden>${ph}</option>`;
        sel.value = '';
        sel.disabled = true;
    };
    const requireSelect = (sel, required, enabled = true) => {
        sel.required = !!required;
        sel.disabled = !enabled;
    };

    async function fetchJson(urls, { signal } = {}) {
        for (const u of urls) {
            try {
                const r = await fetch(u, { cache: 'no-store', signal });
                if (r.ok) return await r.json();
            } catch (_) {
            }
        }
        throw new Error('All endpoints failed');
    }

    async function loadProvinces() {
        if (!cache.provinces) {
            cache.provinces = await fetchJson([`${v2}/?depth=1`, `${v1}/?depth=1`]);
        }
        provinceEl.innerHTML =
            '<option value="" hidden>-- Chọn Tỉnh/Thành --</option>' +
            cache.provinces.map(p => `<option value="${p.code}">${p.name}</option>`).join('');
    }

    async function wardsByProvince(provinceCode, signal) {
        // cache
        if (cache.wardsByProvince.has(provinceCode)) {
            return cache.wardsByProvince.get(provinceCode);
        }

        // 1) Thử depth=3 để lấy luôn districts + wards
        try {
            const p3 = await fetchJson(
                [`${v2}/p/${provinceCode}?depth=3`, `${v1}/p/${provinceCode}?depth=3`],
                { signal }
            );
            if (p3?.districts?.length && p3.districts.some(d => d.wards?.length)) {
                const wards = p3.districts.flatMap(d =>
                    (d.wards || []).map(w => ({
                        code: w.code,
                        name: w.name,
                        districtName: d.name,
                        uniqueId: `${d.code}_${w.code}`,
                    }))
                );
                cache.wardsByProvince.set(provinceCode, wards);
                return wards;
            }
        } catch (_) { /* fallback tiếp */ }

        // 2) Lấy districts (depth=2 hoặc thường) rồi gọi song song wards từng district
        const prov = await fetchJson(
            [
                `${v2}/p/${provinceCode}?depth=2`,
                `${v1}/p/${provinceCode}?depth=2`,
                `${v1}/p/${provinceCode}`
            ],
            { signal }
        );
        const districts = prov?.districts || [];

        if (!districts.length) {
            cache.wardsByProvince.set(provinceCode, []);
            return [];
        }

        // gọi song song, chịu lỗi từng district
        const results = await Promise.allSettled(
            districts.map(d =>
                fetchJson(
                    [
                        `${v2}/d/${d.code}?depth=2`,
                        `${v1}/d/${d.code}?depth=2`,
                        `${v1}/d/${d.code}`
                    ],
                    { signal }
                )
            )
        );

        let wards = [];
        for (let i = 0; i < districts.length; i++) {
            const r = results[i];
            if (r.status === 'fulfilled') {
                const val = r.value;
                const list = Array.isArray(val) ? val : (val?.wards || []);
                wards.push(
                    ...list.map(w => ({
                        code: w.code ?? w.id,
                        name: w.name,
                        districtName: districts[i].name,
                        uniqueId: `${districts[i].code}_${w.code ?? w.id}`,
                    }))
                );
            }
        }
        // sort theo quận rồi tên phường
        wards.sort((a, b) => a.districtName.localeCompare(b.districtName) || a.name.localeCompare(b.name));

        cache.wardsByProvince.set(provinceCode, wards);
        return wards;
    }

    async function loadWards(provinceCode) {
        // hủy request cũ khi đổi tỉnh
        currentAbort?.abort();
        currentAbort = new AbortController();
        const { signal } = currentAbort;

        resetSelect(wardEl, '-- Đang tải... --');
        show(groupWard);
        requireSelect(wardEl, true, false);

        if (!provinceCode) {
            resetSelect(wardEl, '-- Chọn Phường/Xã --');
            composeAddress();
            return;
        }

        try {
            const wards = await wardsByProvince(provinceCode, signal);

            if (!wards.length) {
                resetSelect(wardEl, '-- Không có dữ liệu phường/xã --');
                composeAddress();
                return;
            }

            // render: nhóm theo quận/huyện (optgroup), KHÔNG loại trùng tên
            resetSelect(wardEl, '-- Chọn Phường/Xã --');
            let currentGroup = null, currentDistrict = '';
            for (const w of wards) {
                if (w.districtName !== currentDistrict) {
                    currentDistrict = w.districtName;
                    currentGroup = document.createElement('optgroup');
                    currentGroup.label = `--- ${currentDistrict} ---`;
                    wardEl.appendChild(currentGroup);
                }
                const opt = document.createElement('option');
                opt.value = w.uniqueId;
                opt.textContent = w.name;
                opt.setAttribute('data-ward-name', w.name);
                opt.setAttribute('data-district-name', w.districtName);
                currentGroup.appendChild(opt);
            }
            requireSelect(wardEl, true, true);
        } catch (e) {
            console.error('loadWards failed:', e);
            resetSelect(wardEl, '-- Lỗi tải dữ liệu --');
            wardEl.disabled = false;
            wardEl.required = true;
        } finally {
            composeAddress();
        }
    }

    function composeAddress() {
        const pName = getText(provinceEl);
        const sel = wardEl.options[wardEl.selectedIndex];
        const wName = sel?.getAttribute('data-ward-name') || '';
        const dName = sel?.getAttribute('data-district-name') || '';
        const street = (streetEl?.value || '').trim();

        if (provinceNameHidden) provinceNameHidden.value = pName;
        if (wardNameHidden) wardNameHidden.value = wName;
        if (districtNameHidden) districtNameHidden.value = dName;

        addressEl.value = [street, wName, dName, pName].filter(Boolean).join(', ');

        const ok = Boolean(pName && wName);
        addressEl.classList.remove('is-valid', 'is-invalid');
        addressEl.classList.add(ok ? 'is-valid' : (pName || wName || street) ? 'is-invalid' : '');

        provinceEl.classList.remove('is-valid', 'is-invalid');
        wardEl.classList.remove('is-valid', 'is-invalid');
        if (pName) provinceEl.classList.add('is-valid');
        if (wName) wardEl.classList.add('is-valid');
    }

    provinceEl.addEventListener('change', () => loadWards(provinceEl.value));
    wardEl.addEventListener('change', composeAddress);
    streetEl?.addEventListener('input', composeAddress);

    loadProvinces().catch(e => console.error(e));
}
