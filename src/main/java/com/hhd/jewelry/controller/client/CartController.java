package com.hhd.jewelry.controller.client;

import com.hhd.jewelry.dto.CheckoutDto;
import com.hhd.jewelry.entity.*;
import com.hhd.jewelry.repository.*;
import com.hhd.jewelry.service.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class CartController {

    private static final double VAT_RATE = 0.1;

    private final ProductService productService;
    private final UserService userService;
    private final AddressService addressService;
    private final OrderService orderService;
    private final CartItemService cartItemService;
    private final CartService cartService;
    private final VNPayService vnPayService;
    private final MomoService momoService;


    @GetMapping("/cart")
    public String getCartPage(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/login";
        }
        String email = authentication.getName();
        User user = userService.findByEmail(email).orElse(null);
        if (user == null) {
            return "redirect:/login?error=user_not_found";
        }

        Cart cart = cartService.getCartByUser(user);
        if (cart == null) {
            cart = new Cart();
            cart.setUser(user);
            cartService.save(cart);
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

        var user = userService.findByEmail(auth.getName()).orElse(null);
        var cart = (user == null) ? null : cartService.getCartByUser(user);

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

            User user = userService.findByEmail(auth.getName()).orElse(null);
            if (user == null) return Map.of("ok", false, "reason", "no_user");

            Cart cart = cartService.getCartByUser(user);
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
            cartService.save(cart);
            for (CartItem item : cart.getItems()) {
                boolean isSelected = selectedIds != null && selectedIds.contains(item.getId());
                item.setSelected(isSelected);
            }
            cartService.save(cart);

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

            if (!cartItemService.existsById(itemId)) {
                return Map.of("ok", false, "reason", "item_not_found");
            }
            cartItemService.deleteById(itemId);

            User user = userService.findByEmail(auth.getName()).orElse(null);
            if (user == null) return Map.of("ok", false, "reason", "no_user");

            Cart cart = cartService.getCartByUser(user);
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
    @PostMapping("/cart/reorder/{id}")
    @ResponseBody
    public ResponseEntity<?> reoderItems(@PathVariable("id") Integer orderId, Authentication authentication){
        if (authentication == null || !authentication.isAuthenticated()) {
            // Chưa đăng nhập, trả về lỗi 401 Unauthorized
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Vui lòng đăng nhập để thực hiện chức năng này."));
        }
        User user = userService.findByEmail(authentication.getName()).orElse(null); // Hoặc cách bạn lấy user
        try {
            boolean success = cartService.addItemsFromOrderToCartAndSelect(orderId, user);

            if (success) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Đã thêm sản phẩm vào giỏ hàng thành công!"
                ));
            }
            else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("success", false, "message", "Đã xảy ra lỗi không xác định."));
            }

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST) // Hoặc CONFLICT (409) tùy ngữ cảnh
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau."));
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

        session.setAttribute("checkoutData", checkoutData);

        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/checkout/cart/{id}")
    public String getCartCheckOut(Model model, @PathVariable("id") Integer id, Authentication auth, HttpSession session) {
        // Cần đảm bảo các dependencies sau đã được tiêm: userService, cartRepository, cartItemService, addressService

        @SuppressWarnings("unchecked")
        Map<String, Object> checkoutData = (Map<String, Object>) session.getAttribute("checkoutData");

        if (checkoutData != null){
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

        User user = userService.findByEmail(auth.getName()).orElse(null);
        int cartSize = 0; // Khởi tạo cartSize

        if (user != null) {
            Cart cart = cartService.getCartByUser(user);
            if (cart != null && cart.getItems() != null) {
                cartSize = cart.getItems().stream()
                        .map(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                        .reduce(0, Integer::sum);
            }

            List<CartItem> allItems = cartItemService.findAllByCart_CartId(id);
            List<Address> allAddress = addressService.getAddressesByUser(user);

            List<CartItem> selectedItems = allItems.stream()
                    .filter(CartItem::isSelected)
                    .collect(Collectors.toList());

            model.addAttribute("isBuyNow", false);
            model.addAttribute("carts", selectedItems);
            model.addAttribute("user", user);
            model.addAttribute("cartId", id);
            model.addAttribute("allAddress", allAddress);
        }
        else {
            return "redirect:/login";
        }

        model.addAttribute("cartSize", cartSize);
        return "client_1/product/checkout";
    }

    @GetMapping("/checkout/{serialNumber}")
    public String getCheckout(Model model, @PathVariable("serialNumber") String serialNumber, Authentication auth) {
        User user = userService.findByEmail(auth.getName()).orElse(null);
        boolean isNotNewMember = true;
        if (user != null) {
            model.addAttribute("user", user);
            isNotNewMember = orderService.existsOrderByUser(user);
        }

        Product product = productService.getProductBySerialNumber(serialNumber);
        if (product == null) {
            return "redirect:/";
        }

        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(1);

        long price = product.getPrice() != null ? product.getPrice() : 0;
        int originalDiscount = product.getDiscount() != null ? product.getDiscount() : 0;
        int discountPercent = Math.min(originalDiscount, 100);
        long finalPrice = price - (price * discountPercent / 100);

        item.setPrice((int) finalPrice);

        long subtotal = finalPrice;
        long discount = 0;
        long shipping = 0;
        long tax = calculateTax(subtotal);
        long total = subtotal + shipping + tax;
        String shippingName;
        shippingName = "Giao hàng tiêu chuẩn";
        List<Address> allAddress = addressService.getAddressesByUser(user);
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
        model.addAttribute("allAddress", allAddress);
        return "client_1/product/checkout";
    }

    // ===== Xử lý xác nhận thanh toán =====
    @PostMapping("/checkout/confirm")
    @Transactional
    public String confirmCheckout(@Valid @ModelAttribute CheckoutDto form, BindingResult errors,
                                  Model model, Authentication auth, HttpServletRequest request) throws Exception {
        User user = userService.findByEmail(auth.getName()).orElse(null);
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
        long totalAmount = form.getTotal();
        Order order = orderService.createNewOrder(user, form);

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
            productService.updateStockForOrder(order);
            // Xóa các sản phẩm đã chọn khỏi giỏ hàng nếu thanh toán từ giỏ
            if (form.getCartId() != null) {
                int deleted = cartItemService.deleteSelectedByUser(user);
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
            List<CartItem> allItems = cartItemService.findAllByCart_CartId(form.getCartId());
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

    @PostMapping("/cart/add/{productId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToCartByProductId(@PathVariable Integer productId, Authentication auth) {
        Map<String, Object> response = new HashMap<>();

        if (auth == null || !auth.isAuthenticated()) {
            response.put("success", false);
            response.put("message", "unauthorized");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        try {
            Product product = productService.getProductById(Long.valueOf(productId));
            if (product == null) {
                response.put("success", false);
                response.put("message", "Không tìm thấy sản phẩm.");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            String serialNumber = product.getSerialNumber();
            productService.AddProductToCart(auth.getName(), serialNumber);

            User user = userService.findByEmail(auth.getName()).orElse(null);
            Cart cart = (user == null) ? null : cartService.getCartByUser(user);
            int count = 0;
            if (cart != null && cart.getItems() != null) {
                count = cart.getItems().stream()
                        .map(ci -> ci.getQuantity() == null ? 0 : ci.getQuantity())
                        .reduce(0, Integer::sum);
            }

            response.put("success", true);
            response.put("message", "Đã thêm sản phẩm " + product.getName() + " vào giỏ.");
            response.put("cartCount", count); // Trả về số lượng item mới
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi hệ thống: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}