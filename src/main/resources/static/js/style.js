function updateCountdown() {
    const daysElement = document.getElementById('days');
    const hoursElement = document.getElementById('hours');
    const minutesElement = document.getElementById('minutes');
    const secondsElement = document.getElementById('seconds');

    // Set the target date (2 days from now)
    const targetDate = new Date();
    targetDate.setDate(targetDate.getDate() + 2);
    targetDate.setHours(15, 38, 49, 0);

    function update() {
        const now = new Date();
        const difference = targetDate - now;

        if (difference <= 0) {
            // Sale has ended
            daysElement.textContent = '00';
            hoursElement.textContent = '00';
            minutesElement.textContent = '00';
            secondsElement.textContent = '00';
            return;
        }

        const days = Math.floor(difference / (1000 * 60 * 60 * 24));
        const hours = Math.floor((difference % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
        const minutes = Math.floor((difference % (1000 * 60 * 60)) / (1000 * 60));
        const seconds = Math.floor((difference % (1000 * 60)) / 1000);

        daysElement.textContent = days.toString().padStart(2, '0');
        hoursElement.textContent = hours.toString().padStart(2, '0');
        minutesElement.textContent = minutes.toString().padStart(2, '0');
        secondsElement.textContent = seconds.toString().padStart(2, '0');
    }

    // Update immediately and then every second
    update();
    setInterval(update, 1000);
}

// Product data for the trending carousel
const trendingProducts = [
    {
        title: "14KT Yellow Gold Diamond Hoops",
        category: "Women Elastage",
        price: "Rs. 4,584.00",
        icon: "gem"
    },
    {
        title: "14KT Rose Gold Diamond Hoops",
        category: "Women Collection",
        price: "Rs. 5,200.00",
        icon: "gem"
    },
    {
        title: "14KT White Gold Diamond Hoops",
        category: "Women Collection",
        price: "Rs. 5,100.00",
        icon: "gem"
    },
    {
        title: "Elegant Gold Necklace",
        category: "Neckwear Collection",
        price: "Rs. 7,850.00",
        icon: "medal"
    },
    {
        title: "Diamond Studded Bracelet",
        category: "Bangles Collection",
        price: "Rs. 6,450.00",
        icon: "circle"
    },
    {
        title: "Pearl & Gold Earrings",
        category: "Earrings Collection",
        price: "Rs. 3,990.00",
        icon: "headphones"
    },
    {
        title: "Royal Gold Ring",
        category: "Rings Collection",
        price: "Rs. 5,750.00",
        icon: "circle"
    },
    {
        title: "Exquisite Gold Pendant",
        category: "Pendants Collection",
        price: "Rs. 4,250.00",
        icon: "medal"
    }
];

// Collection data for the collection carousel
const collections = [
    {
        title: "Wedding Collection",
        items: "25+ Items",
        badge: "New",
        icon: "heart",
        description: "Elegant pieces for your special day"
    },
    {
        title: "Classic Gold Series",
        items: "40+ Items",
        badge: "Popular",
        icon: "crown",
        description: "Timeless designs in pure gold"
    },
    {
        title: "Modern Minimalist",
        items: "18+ Items",
        badge: "Trending",
        icon: "star",
        description: "Simple and contemporary designs"
    },
    {
        title: "Vintage Collection",
        items: "32+ Items",
        badge: "Classic",
        icon: "history",
        description: "Inspired by timeless eras"
    },
    {
        title: "Diamond Luxe",
        items: "15+ Items",
        badge: "Premium",
        icon: "gem",
        description: "Exquisite diamond pieces"
    },
    {
        title: "Festive Edition",
        items: "28+ Items",
        badge: "Seasonal",
        icon: "gift",
        description: "Perfect for celebrations"
    },
    {
        title: "Custom Designs",
        items: "Custom Orders",
        badge: "Bespoke",
        icon: "pen-fancy",
        description: "Personalized just for you"
    },
    {
        title: "Men's Collection",
        items: "22+ Items",
        badge: "Exclusive",
        icon: "user-tie",
        description: "Sophisticated pieces for men"
    }
];

// Pagination functionality
document.addEventListener('DOMContentLoaded', function() {
    updateCountdown();

    // Pagination button clicks
    const pageButtons = document.querySelectorAll('.page-btn');
    pageButtons.forEach(button => {
        button.addEventListener('click', function() {
            // Remove active class from all buttons
            pageButtons.forEach(btn => btn.classList.remove('active'));
            // Add active class to clicked button
            this.classList.add('active');
        });
    });
});

// Initialize the carousels and countdown
document.addEventListener('DOMContentLoaded', function() {
    // Initialize Countdown Timer
    updateCountdown();

    // Initialize Trending Products Carousel
    initializeCarousel(
        'trending-carousel',
        'trending-indicators',
        'trending-prev',
        'trending-next',
        trendingProducts,
        createProductCard
    );

    // Initialize Collections Carousel
    initializeCarousel(
        'collections-carousel',
        'collections-indicators',
        'collections-prev',
        'collections-next',
        collections,
        createCollectionCard
    );
});

// Function to create product card
function createProductCard(item) {
    return `
                <div class="product-card">
                    <div class="product-image">
                        <i class="fas fa-${item.icon}"></i>
                    </div>
                    <div class="product-title">${item.title}</div>
                    <div class="product-category">${item.category}</div>
                    <div class="product-price">${item.price}</div>
                </div>
            `;
}

// Function to create collection card
function createCollectionCard(item) {
    return `
                <div class="collection-card">
                    <div class="collection-image">
                        <i class="fas fa-${item.icon}"></i>
                        <span class="collection-badge">${item.badge}</span>
                    </div>
                    <div class="collection-title">${item.title}</div>
                    <div class="collection-items">${item.items}</div>
                    <p style="font-size: 13px; color: #666; margin-bottom: 10px; flex-grow: 1;">${item.description}</p>
                    <a href="#" class="collection-link">Explore</a>
                </div>
            `;
}

// Generic carousel initialization function
function initializeCarousel(trackId, indicatorsId, prevBtnId, nextBtnId, items, createCardFunction) {
    const carouselTrack = document.getElementById(trackId);
    const indicatorsContainer = document.getElementById(indicatorsId);
    const prevBtn = document.getElementById(prevBtnId);
    const nextBtn = document.getElementById(nextBtnId);

    // Create carousel items
    items.forEach((item, index) => {
        const carouselItem = document.createElement('div');
        carouselItem.className = 'carousel-item';
        carouselItem.innerHTML = createCardFunction(item);
        carouselTrack.appendChild(carouselItem);

        // Create indicator
        const indicator = document.createElement('div');
        indicator.className = 'indicator';
        if (index === 0) indicator.classList.add('active');
        indicator.addEventListener('click', () => {
            goToSlide(index, carouselTrack, indicatorsContainer, itemsPerView, items.length);
        });
        indicatorsContainer.appendChild(indicator);
    });

    // Carousel functionality
    let currentIndex = 0;
    let itemsPerView = getItemsPerView();
    let autoSlideInterval;

    function updateCarousel() {
        const itemWidth = 100 / itemsPerView;
        const translateX = -currentIndex * itemWidth;
        carouselTrack.style.transform = `translateX(${translateX}%)`;

        // Update active indicator
        document.querySelectorAll(`#${indicatorsId} .indicator`).forEach((indicator, index) => {
            indicator.classList.toggle('active', index === currentIndex);
        });
    }

    function nextSlide() {
        const maxIndex = Math.ceil(items.length / itemsPerView) - 1;
        if (currentIndex < maxIndex) {
            currentIndex++;
        } else {
            currentIndex = 0;
        }
        updateCarousel();
    }

    function prevSlide() {
        const maxIndex = Math.ceil(items.length / itemsPerView) - 1;
        if (currentIndex > 0) {
            currentIndex--;
        } else {
            currentIndex = maxIndex;
        }
        updateCarousel();
    }

    function goToSlide(index, track, indicators, itemsPerView, totalItems) {
        currentIndex = index;
        updateCarousel();
    }

    // Event listeners
    nextBtn.addEventListener('click', nextSlide);
    prevBtn.addEventListener('click', prevSlide);

    // Update on window resize
    window.addEventListener('resize', function() {
        const newItemsPerView = getItemsPerView();
        if (itemsPerView !== newItemsPerView) {
            itemsPerView = newItemsPerView;
            currentIndex = 0;
            updateCarousel();
        }
    });

    // Initialize carousel
    updateCarousel();

    // Auto slide
    function startAutoSlide() {
        autoSlideInterval = setInterval(nextSlide, 4000);
    }

    function stopAutoSlide() {
        clearInterval(autoSlideInterval);
    }

    startAutoSlide();

    // Pause auto slide on hover
    carouselTrack.addEventListener('mouseenter', stopAutoSlide);
    carouselTrack.addEventListener('mouseleave', startAutoSlide);

    // Touch support for mobile
    let startX = 0;
    let endX = 0;

    carouselTrack.addEventListener('touchstart', (e) => {
        startX = e.touches[0].clientX;
    });

    carouselTrack.addEventListener('touchend', (e) => {
        endX = e.changedTouches[0].clientX;
        handleSwipe();
    });

    function handleSwipe() {
        const swipeThreshold = 50;
        if (startX - endX > swipeThreshold) {
            nextSlide();
        } else if (endX - startX > swipeThreshold) {
            prevSlide();
        }
    }
}

function getItemsPerView() {
    if (window.innerWidth < 480) return 1;
    if (window.innerWidth < 768) return 2;
    if (window.innerWidth < 992) return 3;
    return 5;
}

(function(){
    document.addEventListener('click', function(e){
        const q = e.target.closest('.extra-blog-faq .faq-question');
        if (!q) return;
        const item = q.parentElement;
        const answer = item.querySelector('.faq-answer');

        // Close other answers
        document.querySelectorAll('.extra-blog-faq .faq-answer').forEach(a => {
            if (a !== answer) a.classList.remove('open');
        });
        document.querySelectorAll('.extra-blog-faq .faq-question').forEach(qq => {
            if (qq !== q) qq.classList.remove('active');
        });

        const isOpen = answer.classList.contains('open');
        if (!isOpen) {
            answer.classList.add('open');
            q.classList.add('active');
        } else {
            answer.classList.remove('open');
            q.classList.remove('active');
        }
    });
})();

// ===== Collection carousel and filter products by collection =====
document.addEventListener('DOMContentLoaded', function() {
    const track = document.getElementById('collectionTrack');
    const items = Array.from(track.querySelectorAll('.collection-item'));
    const prevBtn = document.querySelector('.collection-prev');
    const nextBtn = document.querySelector('.collection-next');
    const dotsContainer = document.getElementById('collectionDots');

    let currentIndex = 0;

    items.forEach((_, index) => {
        const dot = document.createElement('button');
        dot.addEventListener('click', () => goToSlide(index));
        dotsContainer.appendChild(dot);
    });

    const dots = dotsContainer.querySelectorAll('button');

    function updateCarousel() {
        items.forEach((item, index) => {
            item.classList.remove('active', 'left-1', 'left-2', 'right-1', 'right-2');

            const diff = index - currentIndex;

            if (diff === 0) {
                item.classList.add('active');
            } else if (diff === -1 || diff === items.length - 1) {
                item.classList.add('left-1');
            } else if (diff === -2 || diff === items.length - 2) {
                item.classList.add('left-2');
            } else if (diff === 1 || diff === -(items.length - 1)) {
                item.classList.add('right-1');
            } else if (diff === 2 || diff === -(items.length - 2)) {
                item.classList.add('right-2');
            }
        });

        dots.forEach((dot, index) => {
            dot.classList.toggle('active', index === currentIndex);
        });
    }

    function goToSlide(index) {
        currentIndex = index;
        updateCarousel();
    }

    function nextSlide() {
        currentIndex = (currentIndex + 1) % items.length;
        updateCarousel();
    }

    function prevSlide() {
        currentIndex = (currentIndex - 1 + items.length) % items.length;
        updateCarousel();
    }

    nextBtn.addEventListener('click', nextSlide);
    prevBtn.addEventListener('click', prevSlide);

    let autoPlay = setInterval(nextSlide, 4000);

    track.addEventListener('mouseenter', () => clearInterval(autoPlay));
    track.addEventListener('mouseleave', () => {
        autoPlay = setInterval(nextSlide, 4000);
    });
    updateCarousel();
});