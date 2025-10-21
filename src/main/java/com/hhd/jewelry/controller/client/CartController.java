package com.hhd.jewelry.controller.client;

import com.hhd.jewelry.dto.CheckoutDto;
import com.hhd.jewelry.entity.*;
import com.hhd.jewelry.repository.*;
import com.hhd.jewelry.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    private final AddressService addressService;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderService orderService;
    private final CartItemService cartItemService;
    private final VNPayService vnPayService;
    private final MomoService momoService;
    private static final double VAT_RATE = 0.1;

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
            for (CartItem item : cart.getItems()) {
                boolean isSelected = selectedIds != null && selectedIds.contains(item.getId());
                item.setSelected(isSelected);  // <- đảm bảo entity CartItem có field `selected`
            }
            cartRepository.save(cart); // Save lại toàn bộ cart sau khi update selected

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
    @PostMapping("/cart/prepare-checkout")
    public ResponseEntity<?> prepareCheckout(@RequestParam String cartId,
                                             @RequestParam(required = false) String couponCode,
                                             @RequestParam(required = false) String shippingName,
                                             @RequestParam(required = false, defaultValue = "0") long shippingFee,
                                             @RequestParam long subtotal,
                                             @RequestParam long discount,
                                             @RequestParam long total,
                                             HttpSession session) {
        Map<String, Object> checkoutData = new HashMap<>();
        checkoutData.put("cartId", cartId);
        checkoutData.put("couponCode", couponCode);
        checkoutData.put("shippingName", shippingName);
        checkoutData.put("shippingFee", shippingFee);
        checkoutData.put("subtotal", subtotal);
        checkoutData.put("discount", discount);
        checkoutData.put("total", total);

        // ✅ Lưu vào session tạm
        session.setAttribute("checkoutData", checkoutData);

        return ResponseEntity.ok(Map.of("ok", true));
    }
    // ===== Trang checkout (Mua từ giỏ hàng) =====
    // *** ĐÃ SỬA LỖI: Chỉ lấy các sản phẩm được chọn và tính tổng ban đầu ***
    @GetMapping("/checkout/cart/{id}")
    public String getCartCheckOut(Model model, @PathVariable("id") Integer id, Authentication auth, HttpSession session) {
        @SuppressWarnings("unchecked")
        Map<String, Object> checkoutData = (Map<String, Object>) session.getAttribute("checkoutData");
        if(checkoutData != null){
            String couponCode = (String) checkoutData.getOrDefault("couponCode", "");
            String shippingName = (String) checkoutData.getOrDefault("shippingName", "");
            long subtotal = (long) checkoutData.getOrDefault("subtotal", 0L);
            long discount = (long) checkoutData.getOrDefault("discount", 0L);
            long shipping = (long) checkoutData.getOrDefault("shippingFee", 0L);
            long tax = calculateTax(subtotal);
            long total = subtotal - discount + shipping + tax;
            model.addAttribute("shippingName", shippingName);
            model.addAttribute("couponCode", couponCode);
            model.addAttribute("subtotal", subtotal);
            model.addAttribute("discount", discount);
            model.addAttribute("shippingFee", shipping);
            model.addAttribute("tax", tax);
            model.addAttribute("total", total);
        }
        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        List<CartItem> allItems = cartItemRepository.findAllByCart_CartId(id);
        List<Address> allAddress = addressService.getAddressesByUser(user);
        // Chỉ lấy các sản phẩm ĐƯỢC CHỌN để thanh toán
        List<CartItem> selectedItems = allItems.stream()
                .filter(CartItem::isSelected)
                .collect(Collectors.toList());

        model.addAttribute("isBuyNow", false); //
        model.addAttribute("carts", selectedItems); // Chỉ gửi các sản phẩm được chọn
        model.addAttribute("user", user);
        model.addAttribute("cartId", id);
        model.addAttribute("allAddress", allAddress);

        return "client_1/product/checkout";
    }

    // ===== Trang checkout (Mua ngay) =====
    // *** ĐÃ HOÀN THIỆN: Tính toán tổng tiền ban đầu cho "Mua ngay" ***
    @GetMapping("/checkout/{serialNumber}")
    public String getCheckout(Model model, @PathVariable("serialNumber") String serialNumber, Authentication auth) {
        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        boolean isNotNewMember = true;
        if (user != null) {
            model.addAttribute("user", user);
            isNotNewMember = orderService.existsOrderByUser(user);
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

        item.setPrice((int) finalPrice);

        long subtotal = finalPrice; // Vì số lượng là 1
        long discount = 0; // Ban đầu không giảm giá
        long shipping = 0; // Phí ship mặc định
        long tax = calculateTax(subtotal);
        long total = subtotal + shipping + tax;
        String shippingName = ""; // ✅ Tên ship mặc định
        if (shipping == 0) {
            // Có thể là freeship do subtotal >= FREESHIP_MIN
            shippingName = "Giao hàng tiêu chuẩn";
        }
        model.addAttribute("isBuyNow", true);
        model.addAttribute("carts", List.of(item));
        model.addAttribute("serialNumber", serialNumber);
        model.addAttribute("isNotNewMember", isNotNewMember);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("discount", discount);
        model.addAttribute("couponCode", "");
        model.addAttribute("shippingFee", shipping);
        model.addAttribute("shippingName", shippingName);
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
        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        if (user == null) throw new IllegalStateException("Người dùng không tồn tại!");
// --- VALIDATION ĐỊA CHỈ ---
        boolean hasOtherErrors = errors.hasErrors() && !(errors.getErrorCount() == 1 && errors.hasFieldErrors("selectedAddressId"));
        boolean noAddressSelected = form.getSelectedAddressId() == null;

        if (hasOtherErrors || noAddressSelected) {
            if (noAddressSelected && !hasOtherErrors) {
                // Chỉ thêm lỗi này nếu chưa có lỗi nào khác về addressId
                errors.rejectValue("selectedAddressId", "NotNull", "Vui lòng chọn địa chỉ giao hàng");
            }
            // Nạp lại dữ liệu cần thiết cho view và trả về
            reloadCheckoutModelOnError(model, form, user);
            return "client_1/product/checkout";
        }
            // --- LẤY ĐỊA CHỈ ĐÃ CHỌN ---
                Address selectedAddress = addressService.getAddressByIdAndUser(form.getSelectedAddressId(), user)
                .orElseThrow(() -> {
                    // Trường hợp ID không hợp lệ hoặc không thuộc về user
                    errors.rejectValue("selectedAddressId", "Invalid", "Địa chỉ đã chọn không hợp lệ.");
                    // Ném exception để dừng xử lý và rollback transaction
                    return new IllegalStateException("Địa chỉ không hợp lệ hoặc không thuộc về người dùng!");
                });
        // Tạo đơn hàng (nhưng chưa set tổng tiền vội)
        Order order = new Order();
        order.setUser(user);
        order.setStatus(Order.Status.PENDING); // Trạng thái ban đầu
        order.setCreatedAt(LocalDateTime.now());
        order.setPaid(false);
        order.setMethodPay(form.getPaymentMethod());
        order.setNote(form.getNote());
        order.setConsignee(selectedAddress.getReceiverName());
        order.setShippingPhone(selectedAddress.getPhone());
        order.setShippingAddressLine(selectedAddress.getAddressLine());
        order.setShippingWard(selectedAddress.getWard());
        order.setShippingDistrict(selectedAddress.getDistrict());
        order.setShippingCity(selectedAddress.getCity());
        order.setCouponCode(form.getCouponCode());
        order.setShippingName(form.getShippingName());
        order.setShippingFee(form.getShippingFee());
        order.setDiscount(form.getDiscount());
        order.setTax(form.getTax());
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
        if(form.getShippingName().equalsIgnoreCase("Nhận tại cửa hàng")){
            order.setShippingMethod(Order.ShippingMethod.STORE_PICKUP);
        }

        // TỔNG TIỀN CUỐI CÙNG (do server tính, bỏ qua form.getTotal())
        long totalAmount = form.getTotal();
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
                if(cartItemService.deleteSelectedByUser(user) == 1){
                    // Cập nhật kho hàng
                    productService.updateStockForOrder(order);
                }
            }
            return "redirect:/"; // Chuyển hướng về trang chủ (hoặc trang cảm ơn)
        }
    }
    private void reloadCheckoutModelOnError(Model model, CheckoutDto form, User user) {
        model.addAttribute("user", user);
        model.addAttribute("allAddress", addressService.getAddressesByUser(user)); // Luôn cần load lại địa chỉ
        model.addAttribute("isBuyNow", form.getSerialNumber() != null);

        // Nạp lại cart items
        if (form.getCartId() != null) {
            List<CartItem> allItems = cartItemRepository.findAllByCart_CartId(form.getCartId());
            model.addAttribute("carts", allItems.stream().filter(CartItem::isSelected).collect(Collectors.toList()));
        } else if (form.getSerialNumber() != null) {
            Product product = productService.getProductBySerialNumber(form.getSerialNumber());
            if (product != null) {
                CartItem item = new CartItem(); /* Tạo CartItem ảo */
                item.setProduct(product); item.setQuantity(1);
                long price = product.getPrice() != null ? product.getPrice() : 0;
                int discount = Math.min(product.getDiscount() != null ? product.getDiscount() : 0, 100);
                item.setPrice((int)(price - (price * discount / 100)));
                model.addAttribute("carts", List.of(item));
            } else { model.addAttribute("carts", List.of()); }
        } else { model.addAttribute("carts", List.of()); }

        // Nạp lại các giá trị form
        model.addAttribute("couponCode", form.getCouponCode());
        model.addAttribute("shippingName", form.getShippingName());
        model.addAttribute("shippingFee", form.getShippingFee());
        model.addAttribute("subtotal", form.getSubtotal());
        model.addAttribute("discount", form.getDiscount());
        model.addAttribute("tax", form.getTax());
        model.addAttribute("total", form.getTotal());
        model.addAttribute("paymentMethod", form.getPaymentMethod());
        model.addAttribute("note", form.getNote());
        model.addAttribute("isNotNewMember", orderService.existsOrderByUser(user));
    }
    /**
     * Tính thuế VAT (Logic khớp với checkout.js)
     */
    private long calculateTax(long baseAmount) {
        long base = Math.max(0, baseAmount); // Không tính thuế trên số âm
        return (long) Math.floor(base * VAT_RATE);
    }
}