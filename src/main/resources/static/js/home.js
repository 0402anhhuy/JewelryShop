document.addEventListener('click', async function (e) {
    const btn = e.target.closest('.add-to-cart-btn');
    if (!btn) return;

    const form = btn.closest('form');
    if (!form) return;

    e.preventDefault();
    if (btn.dataset.loading === '1') return;
    btn.dataset.loading = '1';
    const originalHTML = btn.innerHTML;

    // CSRF từ <meta>
    const tokenEl = document.querySelector('meta[name="_csrf"]');
    const headerEl = document.querySelector('meta[name="_csrf_header"]');
    const CSRF_TOKEN = tokenEl?.content || null;
    const CSRF_HEADER = headerEl?.content || 'X-CSRF-TOKEN';

    // Tạo URL và ép thêm ?ajax=1
    const url = new URL(form.action, window.location.origin);
    url.searchParams.set('ajax', '1');

    // Gom body form-encoded + đảm bảo ajax=1, quantity mặc định
    const fd = new FormData(form);
    fd.set('ajax', '1');                 // <<< QUAN TRỌNG cho params="ajax=1"
    if (!fd.has('quantity') || !fd.get('quantity')) fd.set('quantity', '1');

    const body = new URLSearchParams(fd);

    try {
        const res = await fetch(url.toString(), {
            method: 'POST',
            headers: Object.assign(
                { 'X-Requested-With': 'XMLHttpRequest' },
                CSRF_TOKEN ? { [CSRF_HEADER]: CSRF_TOKEN } : {}
            ),
            body
        });

        let data = {};
        try { data = await res.json(); } catch (_) {}

        if (res.ok && data.ok) {
            // Cập nhật UI
            btn.innerHTML = '<i class="fas fa-check"></i> Đã thêm!';
            btn.style.background = '#28a745';
            // cập nhật badge giỏ hàng nếu có
            const badge = document.querySelector('.cart-count, #cart-count, .cart-badge');
            if (badge && typeof data.count === 'number') badge.textContent = data.count;

            setTimeout(() => { btn.innerHTML = originalHTML; btn.style.background = '' }, 1500);
        } else if (data.reason === 'unauthenticated') {
            // chuyển hướng login nếu chưa đăng nhập
            const ret = encodeURIComponent(location.pathname + location.search);
            location.href = `/login?returnUrl=${ret}`;
        } else {
            btn.innerHTML = '<i class="fas fa-exclamation-triangle"></i> Lỗi';
            btn.style.background = '#dc3545';
            setTimeout(() => { btn.innerHTML = originalHTML; btn.style.background = '' }, 2000);
            console.error('Add to cart failed:', data);
            location.href = `/login`;
        }
    } catch (err) {
        btn.innerHTML = '<i class="fas fa-exclamation-triangle"></i> Lỗi mạng';
        btn.style.background = '#dc3545';
        setTimeout(() => { btn.innerHTML = originalHTML; btn.style.background = '' }, 2000);
        console.error(err);
    } finally {
        btn.dataset.loading = '0';
    }
});

// ===== Countdown (giữ bản gọn) =====
function tick() {
    document.querySelectorAll(".fs-countdown").forEach((el) => {
        const t = new Date(el.dataset.time).getTime();
        let diff = t - Date.now();
        if (diff < 0) diff = 0;

        const h = Math.floor(diff / 3600000);
        const m = Math.floor((diff % 3600000) / 60000);
        const s = Math.floor((diff % 60000) / 1000);

        const set = (sel, val) => {
            const node = el.querySelector(sel);
            if (node) node.textContent = String(val).padStart(2, "0");
        };
        set('[data-part="h"]', h);
        set('[data-part="m"]', m);
        set('[data-part="s"]', s);
    });
}

setInterval(tick, 1000);
tick();

// ===== Tabs ngày + scroller cho flash sale (nếu có) =====
(function initFlashSaleTabs() {
    const tabs = Array.from(document.querySelectorAll(".fs-tab"));
    const scroller = document.getElementById("fs-scroller");
    const prevBtn = document.querySelector(".fs-prev");
    const nextBtn = document.querySelector(".fs-next");

    if (!tabs.length || !scroller) return;

    function filterByDay(dayIndex) {
        document.querySelectorAll(".flashsale-item").forEach((card) => {
            const show = String(card.dataset.day) === String(dayIndex);
            card.style.display = show ? "" : "none";
        });
        scroller.scrollTo({left: 0, behavior: "auto"});
        updateNavButtons();
    }

    function updateNavButtons() {
        if (!prevBtn || !nextBtn) return;
        const max = scroller.scrollWidth - scroller.clientWidth;
        prevBtn.disabled = scroller.scrollLeft <= 0;
        nextBtn.disabled = scroller.scrollLeft >= max - 1;
    }

    tabs.forEach((tab, idx) => {
        tab.addEventListener("click", () => {
            tabs.forEach((t) => t.classList.remove("active"));
            tab.classList.add("active");
            filterByDay(idx);
        });
    });

    if (prevBtn && nextBtn) {
        prevBtn.addEventListener("click", () => {
            scroller.scrollBy({left: -scroller.clientWidth * 0.9, behavior: "smooth"});
            setTimeout(updateNavButtons, 400);
        });
        nextBtn.addEventListener("click", () => {
            scroller.scrollBy({left: scroller.clientWidth * 0.9, behavior: "smooth"});
            setTimeout(updateNavButtons, 400);
        });
        scroller.addEventListener("scroll", updateNavButtons);
    }

    const defaultTab = document.querySelector(".fs-tab.active") || tabs[0];
    if (defaultTab) defaultTab.click();
    else updateNavButtons();
})();
// js giup ap dung radio checkbox ngay lap tuc
document.addEventListener("DOMContentLoaded", function () {
    const form = document.querySelector(".filter-box");
    // Auto submit khi đổi checkbox, radio, select
    form.querySelectorAll("input[type=checkbox], input[type=radio], select")
        .forEach(el => el.addEventListener("change", () => form.submit()));
});


// ===== Detail page: gallery behavior (nếu có) =====
document.addEventListener("DOMContentLoaded", () => {
    const thumbs = document.querySelectorAll(".thumb-list img.thumb");
    const main = document.getElementById("mainImage");
    if (!thumbs.length || !main) return;

    thumbs.forEach((t) => t.classList.remove("active"));
    thumbs[0].classList.add("active");

    thumbs.forEach((img) => {
        img.addEventListener("click", () => {
            thumbs.forEach((i) => i.classList.remove("active"));
            img.classList.add("active");
            main.src = img.currentSrc || img.src;
            main.alt = img.alt || "main";
        });
    });
});

document.addEventListener("DOMContentLoaded", () => {
    const slider = document.querySelector('.viewed-list');
    const prevBtn = document.querySelector('.prev-btn');
    const nextBtn = document.querySelector('.next-btn');

    function getItemWidth() {
        return document.querySelector('.viewed-item').offsetWidth + 20;
        // 20 là gap (nếu bạn để gap khác thì chỉnh lại)
    }

    nextBtn.addEventListener('click', () => {
        slider.scrollLeft += getItemWidth();
    });

    prevBtn.addEventListener('click', () => {
        slider.scrollLeft -= getItemWidth();
    });
});


// ===== Collection carousel + filter products by collection =====
document.addEventListener('DOMContentLoaded', () => {
    const track = document.getElementById('collectionTrack');
    if (!track) return;

    const items = Array.from(track.querySelectorAll('.collection-item'));
    const dotsWrap = document.getElementById('collectionDots');
    const prevBtn = document.querySelector('.col-prev');
    const nextBtn = document.querySelector('.col-next');

    let activeIndex = 0;
    const spacing = 130; // khoảng cách giữa các item

    // tạo dots
    items.forEach((it, idx) => {
        const b = document.createElement('button');
        if (idx === 0) b.classList.add('active');
        b.addEventListener('click', () => setActive(idx));
        dotsWrap.appendChild(b);
    });

    function setActive(index) {
        activeIndex = (index + items.length) % items.length;

        items.forEach((it, i) => {
            it.classList.remove('active');
            let delta = i - activeIndex;

            // để hai bên chia đều, không dồn 1 phía
            const middle = Math.floor(items.length / 2);
            if (delta > middle) delta -= items.length;
            if (delta < -middle) delta += items.length;

            const tx = delta * spacing;
            const scale = delta === 0 ? 1 : 0.85;

            it.style.transform = `translateX(calc(-50% + ${tx}px)) scale(${scale})`;
            it.style.zIndex = String(100 - Math.abs(delta));

            if (delta === 0) it.classList.add('active');
        });

        // update dots
        dotsWrap.querySelectorAll("button").forEach((d, i) =>
            d.classList.toggle("active", i === activeIndex)
        );
    }


    // nav
    prevBtn && prevBtn.addEventListener('click', () => setActive(activeIndex - 1));
    nextBtn && nextBtn.addEventListener('click', () => setActive(activeIndex + 1));

    // click item
    items.forEach((it, idx) => it.addEventListener('click', () => setActive(idx)));

    // chạy ngay khi load để chia đều trái/phải
    setActive(3);
});