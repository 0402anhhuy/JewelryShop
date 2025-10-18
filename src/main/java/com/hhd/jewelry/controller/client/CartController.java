package com.hhd.jewelry.controller.client;

import com.hhd.jewelry.dto.CheckoutDto;
import com.hhd.jewelry.entity.*;
import com.hhd.jewelry.repository.*;
import com.hhd.jewelry.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class CartController {

    private final ProductService productService;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderService orderService;
    private final CartItemService cartItemService;
    private final VNPayService vnPayService;
    private final MomoService momoService;
    private static final double VAT_RATE = 0.1;     // 3% VAT
    private static final long SHIP_BASE = 30000;       // Phí ship mặc định
    private static final long FREESHIP_MIN = 1000000;  // Miễn ship nếu tạm tính >= 1,000,000đ

    // ===== Hiển thị giỏ hàng =====
    @GetMapping("/cart")
    public String getCartPage(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return "redirect:/login?error=user_not_found";
        }

        Cart cart = cartRepository.findByUser(user);
        if (cart == null) {
            cart = new Cart();
            cart.setUser(user);
            cart = cartRepository.save(cart);
        }

        List<CartItem> cartItems = cart.getItems() != null ? cart.getItems() : new ArrayList<>();
        boolean isNotNewMember = orderService.existsOrderByUser(user);
        int subtotal = cartItems.stream()
                .filter(item -> item != null && item.getProduct() != null && item.isSelected())
                .mapToInt(item -> {
                    Product p = item.getProduct();
                    int price = p.getPrice() != null ? p.getPrice() : 0;
                    int originalDiscount = p.getDiscount() != null ? p.getDiscount() : 0;
                    int discountPercent = Math.min(originalDiscount, 100);
                    int quantity = item.getQuantity() != null ? item.getQuantity() : 0;
                    int finalPrice = price - (price * discountPercent / 100);
                    return finalPrice * quantity;
                })
                .sum();

        int discountValue = 0;
        int total = subtotal - discountValue;

        model.addAttribute("cartid", cart.getCartId());
        model.addAttribute("carts", cartItems);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("discount", discountValue);
        model.addAttribute("total", total);
        model.addAttribute("isNotNewMember", isNotNewMember);
        return "client_1/product/cart";
    }

    // ===== Thêm vào giỏ hàng =====
    @PostMapping(value = "/cart/add/{serialnumber}", params = "ajax=1")
    @ResponseBody
    public Map<String, Object> addToCartAjax(@PathVariable String serialnumber, Authentication auth) {
        // ... (Logic cũ của bạn giữ nguyên) ...
        if (auth == null || !auth.isAuthenticated()) {
            return Map.of("ok", false, "reason", "unauthenticated");
        }
        Product p = productService.getProductBySerialNumber(serialnumber);
        if (p == null) return Map.of("ok", false, "reason", "not_found");

        productService.AddProductToCart(auth.getName(), p.getSerialNumber());

        var user = userRepository.findByEmail(auth.getName()).orElse(null);
        var cart = (user == null) ? null : cartRepository.findByUser(user);

        int count = 0;
        if (cart != null && cart.getItems() != null) {
            count = cart.getItems().stream()
                    .map(ci -> ci.getQuantity() == null ? 0 : ci.getQuantity())
                    .reduce(0, Integer::sum);
        }
        return Map.of("ok", true, "count", count);
    }

    // ===== Cập nhật giỏ hàng =====
    @PostMapping("/cart/update")
    @ResponseBody
    @Transactional
    public Map<String, Object> updateCartAjax(@RequestParam Integer itemId,
                                              @RequestParam Integer qty,
                                              @RequestParam(value = "selectedIds", required = false) List<Integer> selectedIds,
                                              Authentication auth) {
        try {
            // ... (Logic cũ của bạn giữ nguyên) ...
            if (auth == null || !auth.isAuthenticated()) {
                return Map.of("ok", false, "reason", "unauthenticated");
            }
            if (qty == null || qty <= 0) qty = 1;

            User user = userRepository.findByEmail(auth.getName()).orElse(null);
            if (user == null) return Map.of("ok", false, "reason", "no_user");

            Cart cart = cartRepository.findByUser(user);
            if (cart == null || cart.getItems() == null) {
                return Map.of("ok", false, "reason", "no_cart");
            }

            CartItem updated = cart.getItems().stream()
                    .filter(ci -> ci != null && Objects.equals(ci.getId(), itemId))
                    .findFirst()
                    .orElse(null);

            if (updated == null) {
                return Map.of("ok", false, "reason", "no_item");
            }

            updated.setQuantity(qty);
            cartRepository.save(cart);
            if (selectedIds != null) {
                for (CartItem item : cart.getItems()) {
                    boolean isSelected = selectedIds.contains(item.getId());
                    item.setSelected(isSelected);  // <- đảm bảo entity CartItem có field `selected`
                }
                cartRepository.save(cart); // Save lại toàn bộ cart sau khi update selected
            }

            int priceOfUpdatedItem = (updated.getProduct() != null && updated.getProduct().getPrice() != null)
                    ? updated.getProduct().getPrice() : 0;
            int originalDiscount = (updated.getProduct() != null && updated.getProduct().getDiscount() != null)
                    ? updated.getProduct().getDiscount() : 0;
            int discountOfUpdatedItem = Math.min(originalDiscount, 100);
            int finalPrice = priceOfUpdatedItem - (priceOfUpdatedItem * discountOfUpdatedItem / 100);
            int lineTotal = finalPrice * updated.getQuantity();

            int subtotal = 0;
            if (selectedIds != null && !selectedIds.isEmpty()) {
                Set<Integer> selectedSet = new HashSet<>(selectedIds);
                subtotal = cart.getItems().stream()
                        .filter(ci -> ci != null && selectedSet.contains(ci.getId()) && ci.getProduct() != null)
                        .mapToInt(ci -> {
                            int price = ci.getProduct().getPrice() != null ? ci.getProduct().getPrice() : 0;
                            int originalItemDiscount = ci.getProduct().getDiscount() != null ? ci.getProduct().getDiscount() : 0;
                            int itemDiscount = Math.min(originalItemDiscount, 100);
                            int quantity = ci.getQuantity() != null ? ci.getQuantity() : 0;
                            int itemFinalPrice = price - (price * itemDiscount / 100);
                            return itemFinalPrice * quantity;
                        })
                        .sum();
            }

            int discountValue = 0;
            int total = subtotal - discountValue;

            Map<String, Object> res = new HashMap<>();
            res.put("ok", true);
            res.put("itemId", itemId);
            res.put("qty", updated.getQuantity());
            res.put("lineTotal", lineTotal);
            res.put("subtotal", subtotal);
            res.put("discount", discountValue);
            res.put("total", total);
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("ok", false, "reason", "Lỗi hệ thống: " + e.getMessage());
        }
    }

    // ===== Xóa sản phẩm trong giỏ =====
    @PostMapping("/cart/remove")
    @ResponseBody
    @Transactional
    public Map<String, Object> removeItemAjax(@RequestParam("itemId") Integer itemId,
                                              @RequestParam(value = "selectedIds", required = false) List<Integer> selectedIds,
                                              Authentication auth) {
        try {
            // ... (Logic cũ của bạn giữ nguyên) ...
            if (auth == null || !auth.isAuthenticated()) {
                return Map.of("ok", false, "reason", "unauthenticated");
            }

            if (!cartItemRepository.existsById(itemId)) {
                return Map.of("ok", false, "reason", "item_not_found");
            }
            cartItemRepository.deleteById(itemId);

            User user = userRepository.findByEmail(auth.getName()).orElse(null);
            if (user == null) return Map.of("ok", false, "reason", "no_user");

            Cart cart = cartRepository.findByUser(user);
            if (cart == null) return Map.of("ok", true, "subtotal", 0, "total", 0, "discount", 0);

            int subtotal = 0;
            if (selectedIds != null && !selectedIds.isEmpty()) {
                selectedIds.remove(itemId);
                Set<Integer> selectedSet = new HashSet<>(selectedIds);
                subtotal = cart.getItems().stream()
                        .filter(ci -> ci != null && selectedSet.contains(ci.getId()) && ci.getProduct() != null)
                        .mapToInt(ci -> {
                            int price = ci.getProduct().getPrice() != null ? ci.getProduct().getPrice() : 0;
                            int originalDiscount = ci.getProduct().getDiscount() != null ? ci.getProduct().getDiscount() : 0;
                            int itemDiscount = Math.min(originalDiscount, 100);
                            int quantity = ci.getQuantity() != null ? ci.getQuantity() : 0;
                            return (price - (price * itemDiscount / 100)) * quantity;
                        })
                        .sum();
            }

            int discountValue = 0;
            int total = subtotal - discountValue;

            Map<String, Object> response = new HashMap<>();
            response.put("ok", true);
            response.put("subtotal", subtotal);
            response.put("discount", discountValue);
            response.put("total", total);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("ok", false, "reason", "Lỗi hệ thống: " + e.getMessage());
        }
    }

    // ===== Trang checkout (Mua từ giỏ hàng) =====
    // *** ĐÃ SỬA LỖI: Chỉ lấy các sản phẩm được chọn và tính tổng ban đầu ***
    @GetMapping("/checkout/cart/{id}")
    public String getCartCheckOut(Model model, @PathVariable("id") Integer id, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        List<CartItem> allItems = cartItemRepository.findAllByCart_CartId(id);

        // Chỉ lấy các sản phẩm ĐƯỢC CHỌN để thanh toán
        List<CartItem> selectedItems = allItems.stream()
                .filter(CartItem::isSelected)
                .collect(Collectors.toList());

        // Tính toán tổng tiền ban đầu (dựa trên logic đã sửa ở getCartPage)
        long subtotal = selectedItems.stream()
                .mapToLong(item -> {
                    Product p = item.getProduct();
                    long price = p.getPrice() != null ? p.getPrice() : 0;
                    int originalDiscount = p.getDiscount() != null ? p.getDiscount() : 0;
                    int discountPercent = Math.min(originalDiscount, 100);
                    int quantity = item.getQuantity() != null ? item.getQuantity() : 1;
                    long finalPrice = price - (price * discountPercent / 100);
                    return finalPrice * quantity;
                })
                .sum();

        long discount = 0; // Mặc định là 0 khi tải trang
        long shipping = calculateShipping(subtotal, null);
        long tax = calculateTax(subtotal);
        long total = subtotal + shipping + tax;

        model.addAttribute("carts", selectedItems); // Chỉ gửi các sản phẩm được chọn
        model.addAttribute("user", user);
        model.addAttribute("cartId", id);

        // Gửi các giá trị đã tính toán ra view
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("discount", discount);
        model.addAttribute("shippingFee", shipping);
        model.addAttribute("tax", tax);
        model.addAttribute("total", total);

        return "client_1/product/checkout";
    }

    // ===== Trang checkout (Mua ngay) =====
    // *** ĐÃ HOÀN THIỆN: Tính toán tổng tiền ban đầu cho "Mua ngay" ***
    @GetMapping("/checkout/{serialNumber}")
    public String getCheckout(Model model, @PathVariable("serialNumber") String serialNumber, Authentication auth) {
        if (auth != null && auth.isAuthenticated()) {
            model.addAttribute("user", userRepository.findByEmail(auth.getName()).orElse(null));
        }

        Product product = productService.getProductBySerialNumber(serialNumber);
        if (product == null) {
            // Xử lý nếu không tìm thấy sản phẩm
            return "redirect:/";
        }

        // Tạo một CartItem "ảo" để hiển thị
        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(1);

        // Tính toán giá tiền
        long price = product.getPrice() != null ? product.getPrice() : 0;
        int originalDiscount = product.getDiscount() != null ? product.getDiscount() : 0;
        int discountPercent = Math.min(originalDiscount, 100);
        long finalPrice = price - (price * discountPercent / 100);

        // Gán giá đã giảm vào CartItem "ảo"
        item.setPrice((int) finalPrice); // Sử dụng trường price để lưu giá cuối cùng

        long subtotal = finalPrice; // Vì số lượng là 1
        long discount = 0;
        long shipping = calculateShipping(subtotal, null);
        long tax = calculateTax(subtotal);
        long total = subtotal + shipping + tax;

        model.addAttribute("carts", List.of(item));
        model.addAttribute("serialNumber", serialNumber);

        model.addAttribute("subtotal", subtotal);
        model.addAttribute("discount", discount);
        model.addAttribute("shippingFee", shipping);
        model.addAttribute("tax", tax);
        model.addAttribute("total", total);

        return "client_1/product/checkout";
    }

    // ===== Xử lý xác nhận thanh toán =====
    // *** ĐÃ SỬA LỖI BẢO MẬT: Tính toán lại toàn bộ ở server ***
    @PostMapping("/checkout/confirm")
    @Transactional
    public String confirmCheckout(@Valid @ModelAttribute CheckoutDto form, BindingResult errors,
                                  Model model, Authentication auth, HttpServletRequest request) throws Exception {

        if (errors.hasErrors()) {
            // (Giữ nguyên logic xử lý lỗi của bạn)
            model.addAttribute("coupon", form.getCoupon());
            model.addAttribute("paymentMethod", form.getPaymentMethod());
            model.addAttribute("receiver", form);
            return "client_1/product/checkout";
        }

        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        if (user == null) throw new IllegalStateException("Người dùng không tồn tại!");

        // (Giữ nguyên logic lưu địa chỉ)
        Address address = new Address();
        address.setUser(user);
        address.setReceiverName(form.getReceiverName());
        address.setPhone(form.getReceiverPhone());
        address.setAddressLine(form.getAddressLine());
        address.setCity(form.getProvince());
        address.setDistrict(form.getDistrict());
        address.setWard(form.getWard());
        address.setIsDefault(false);
        addressRepository.save(address);

        // Tạo đơn hàng (nhưng chưa set tổng tiền vội)
        Order order = new Order();
        order.setUser(user);
        order.setStatus(Order.Status.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        order.setPaid(false);
        order.setMethodPay(form.getPaymentMethod());
        orderService.save(order);

        // --- BẮT ĐẦU TÍNH TOÁN PHÍA SERVER ---

        // Bước 1: Tạo danh sách OrderItem và tính giá chính xác
        List<OrderItem> items = new ArrayList<>();

        // Trường hợp Mua ngay
        if (form.getSerialNumber() != null && !form.getSerialNumber().isEmpty()) {
            Product product = productService.getProductBySerialNumber(form.getSerialNumber());
            if (product == null) throw new IllegalStateException("Sản phẩm không tồn tại!");

            long price = product.getPrice() != null ? product.getPrice() : 0;
            int originalDiscount = product.getDiscount() != null ? product.getDiscount() : 0;
            int discountPercent = Math.min(originalDiscount, 100);
            long finalPrice = price - (price * discountPercent / 100);

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(product);
            oi.setQuantity(1);
            oi.setUnitPrice(BigDecimal.valueOf(finalPrice)); // Giá đã giảm
            oi.setTotalPrice(BigDecimal.valueOf(finalPrice)); // Tổng = giá * 1
            items.add(oi);
        }
        // Trường hợp Mua từ giỏ hàng
        else if (form.getCartId() != null) {
            Cart cart = cartRepository.findById(form.getCartId()).orElse(null);
            if (cart == null) throw new IllegalStateException("Giỏ hàng không tồn tại!");

            // Chỉ xử lý các sản phẩm được CHỌN
            List<CartItem> selectedItems = cart.getItems().stream()
                    .filter(CartItem::isSelected)
                    .collect(Collectors.toList());

            if (selectedItems.isEmpty()) throw new IllegalStateException("Không có sản phẩm nào được chọn trong giỏ hàng!");

            for (CartItem ci : selectedItems) {
                Product product = ci.getProduct();
                int qty = Math.max(1, ci.getQuantity());

                long price = product.getPrice() != null ? product.getPrice() : 0;
                int originalDiscount = product.getDiscount() != null ? product.getDiscount() : 0;
                int discountPercent = Math.min(originalDiscount, 100);
                long finalPrice = price - (price * discountPercent / 100);

                OrderItem oi = new OrderItem();
                oi.setOrder(order);
                oi.setProduct(product);
                oi.setQuantity(qty);
                oi.setUnitPrice(BigDecimal.valueOf(finalPrice)); // Giá đã giảm
                oi.setTotalPrice(BigDecimal.valueOf(finalPrice).multiply(BigDecimal.valueOf(qty)));
                items.add(oi);
            }
        }

        if (items.isEmpty()) throw new IllegalStateException("Không có sản phẩm hợp lệ!");

        orderItemRepository.saveAll(items);
        order.setItems(items);

        // Bước 2: Tính toán tổng tiền (Subtotal, Discount, Shipping, Tax)
        // Subtotal là tổng tiền của các OrderItem (đã bao gồm giảm giá TẠI SẢN PHẨM)
        long subtotal = items.stream()
                .mapToLong(item -> item.getTotalPrice().longValue())
                .sum();

        // Giảm giá (từ mã coupon)
        long discount = calculateCouponDiscount(subtotal, form.getCoupon());

        // Phí vận chuyển
        long shipping = calculateShipping(subtotal - discount, form.getCoupon());

        // Thuế
        long tax = calculateTax(subtotal - discount);

        // TỔNG TIỀN CUỐI CÙNG (do server tính, bỏ qua form.getTotal())
        long totalAmount = (subtotal - discount) + shipping + tax;
        if (totalAmount < 0) totalAmount = 0;

        // Bước 3: Cập nhật tổng tiền chính xác vào đơn hàng
        order.setTotalAmount(totalAmount);
        orderService.save(order);

        // --- KẾT THÚC TÍNH TOÁN PHÍA SERVER ---

        // Bước 4: Xử lý thanh toán với TỔNG TIỀN ĐÚNG
        String paymentMethod = form.getPaymentMethod();
        String orderInfo = "Thanh toan don hang " + order.getId();

        if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
            if (totalAmount < 5000)
                throw new IllegalStateException("Tổng tiền phải >= 5.000đ để thanh toán qua VNPAY");
            String paymentUrl = vnPayService.createPaymentUrl(order.getId(), totalAmount, orderInfo, request);
            return "redirect:" + paymentUrl;
        } else if ("MOMO".equalsIgnoreCase(paymentMethod)) {
            if (totalAmount < 5000)
                throw new IllegalStateException("Tổng tiền phải >= 5.000đ để thanh toán qua MOMO");
            String momoUrl = momoService.createMomoPayment(order.getId(), totalAmount, orderInfo, request);
            return "redirect:" + momoUrl;
        } else {
            // COD - Thanh toán khi nhận hàng
            order.setStatus(Order.Status.PENDING);
            orderService.save(order);

            // Xóa các sản phẩm đã chọn khỏi giỏ hàng nếu thanh toán từ giỏ
            if (form.getCartId() != null) {
                // Chỉ xóa các item đã được chọn (nằm trong list `items`)
                List<Integer> itemIdsToDelete = items.stream()
                        .map(oi -> oi.getProduct().getId())
                        .collect(Collectors.toList());

                // Lấy các CartItem tương ứng để xóa
                Cart cart = cartRepository.findById(form.getCartId()).get();
                List<CartItem> itemsToDelete = cart.getItems().stream()
                        .filter(ci -> ci.isSelected() && itemIdsToDelete.contains(ci.getProduct().getId()))
                        .collect(Collectors.toList());

                cartItemRepository.deleteAll(itemsToDelete);

                // Cập nhật kho hàng
                productService.updateStockForOrder(order);
            }

            return "redirect:/"; // Chuyển hướng về trang chủ (hoặc trang cảm ơn)
        }
    }


    // --- CÁC HÀM HELPER TÍNH TOÁN (PRIVATE) ---

    /**
     * Tính giảm giá từ mã coupon (Logic khớp với checkout.js)
     */
    private long calculateCouponDiscount(long subtotal, String couponCode) {
        if (couponCode == null || couponCode.trim().isEmpty()) return 0;
        String code = couponCode.trim().toUpperCase();
        switch (code) {
            case "GIAM5":
                return (long) Math.floor(subtotal * 0.05);
            case "FIX50K":
                return Math.min(50000, subtotal);
            case "FREESHIP":
                return 0; // Được xử lý ở hàm shipping
            default:
                return 0;
        }
    }

    /**
     * Tính phí vận chuyển (Logic khớp với checkout.js)
     */
    private long calculateShipping(long baseAmount, String couponCode) {
        String code = (couponCode == null) ? "" : couponCode.trim().toUpperCase();
        if (baseAmount >= FREESHIP_MIN) return 0; // Miễn ship nếu đạt ngưỡng
        if ("FREESHIP".equals(code)) return 0; // Miễn ship nếu dùng code
        return SHIP_BASE;
    }

    /**
     * Tính thuế VAT (Logic khớp với checkout.js)
     */
    private long calculateTax(long baseAmount) {
        long base = Math.max(0, baseAmount); // Không tính thuế trên số âm
        return (long) Math.floor(base * VAT_RATE);
    }
}