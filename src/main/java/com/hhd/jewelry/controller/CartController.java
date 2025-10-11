package com.hhd.jewelry.controller;

import com.hhd.jewelry.dto.CheckoutDto;
import com.hhd.jewelry.entity.*;
import com.hhd.jewelry.repository.*;
import com.hhd.jewelry.service.ProductService;
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
import java.util.stream.Stream;

@Controller
@RequiredArgsConstructor
public class CartController {
    private final ProductService productService;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository  cartItemRepository;

    @GetMapping("/cart")
    public String  getCartPage(Model model, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email).orElse(null);
        Cart cart = cartRepository.findByUser(user);
        List<CartItem> cartItems = cart.getItems();
        int subtotal = 0;
        int discount = 0; // tuỳ bạn tính
        int total = 0;
        model.addAttribute("cartid", cart.getCartId());
        model.addAttribute("carts", cartItems);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("discount", discount);
        model.addAttribute("total", total);

        return "client_1/product/cart";
    }

    @PostMapping(value="/cart/add/{serialnumber}", params="ajax=1")
    @ResponseBody
    public Map<String, Object> addToCartAjax(@PathVariable String serialnumber, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return Map.of("ok", false, "reason", "unauthenticated");
        }
        Product p = productService.getProductBySerialNumber(serialnumber);
        if (p == null) return Map.of("ok", false, "reason", "not_found");

        productService.AddProductToCart(auth.getName(), p.getSerialNumber());

        // Tính lại tổng số lượng để trả về
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
    @PostMapping("/cart/update")
    @ResponseBody
    @Transactional
    public Map<String, Object> updateCartAjax(@RequestParam Integer itemId,
                                                  @RequestParam Integer qty,
                                                  @RequestParam(value = "selectedIds", required = false) List<Integer> selectedIds,
                                                  Authentication auth) {
        // 1) Xác thực
        if (auth == null || !auth.isAuthenticated()) {
            return Map.of("ok", false, "reason", "unauthenticated");
        }
        if (qty == null || qty <= 0) qty = 1;

        // 2) Lấy user + cart
        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        if (user == null) return Map.of("ok", false, "reason", "no_user");
        Cart cart = cartRepository.findByUser(user);
        if (cart == null) return Map.of("ok", false, "reason", "no_cart");

        // 3) Tìm cart item cần cập nhật
        CartItem updated = cart.getItems().stream()
                .filter(ci -> Objects.equals(ci.getId(), itemId))
                .findFirst()
                .orElse(null);
        if (updated == null) return Map.of("ok", false, "reason", "no_item");

        // 4) Cập nhật số lượng & lưu
        updated.setQuantity(qty);
        // Do @OneToMany(cascade = ALL) trên Cart → save cart là đủ
        cartRepository.save(cart);

        // 5) Tính lineTotal của dòng vừa cập nhật
        int lineTotal = (updated.getPrice() == null ? 0 : updated.getPrice())
                * (updated.getQuantity() == null ? 0 : updated.getQuantity());

        // 6) Tính subtotal theo "selectedIds"
        Stream<CartItem> stream = cart.getItems().stream();
        // Nếu client gửi selectedIds (kể cả rỗng) → chỉ tính các id đó
        int subtotal = 0;
        if (selectedIds != null) {
            Set<Integer> selectedSet = new HashSet<>(selectedIds);
            stream = stream.filter(ci -> selectedSet.contains(ci.getId()));
            subtotal = stream
                    .mapToInt(ci -> (ci.getPrice() == null ? 0 : ci.getPrice())
                            * (ci.getQuantity() == null ? 0 : ci.getQuantity()))
                    .sum();
        }
        int discount = 0;
        int total = subtotal - discount;

        // 7) Trả JSON cho client
        Map<String, Object> res = new HashMap<>();
        res.put("ok", true);
        res.put("itemId", itemId);
        res.put("qty", updated.getQuantity());
        res.put("lineTotal", lineTotal);
        res.put("subtotal", subtotal);
        res.put("discount", discount);
        res.put("total", total);
        return res;
    }

    @GetMapping("/cart/remove/{id}")
    public String removeItem(@PathVariable("id") Integer id,
                             Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) return "redirect:/login";

        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        if (user == null) return "redirect:/cart";
        Cart cart = cartRepository.findByUser(user);
        if (cart == null || cart.getItems() == null) return "redirect:/cart";
        productService.RemoveProductToCart(cart.getCartId(), id);
        return "redirect:/cart";
    }

    @GetMapping("/checkout/cart/{id}")
    public String getCartCheckOut(Model model, @PathVariable("id") Integer id, Authentication auth){
        User user  = userRepository.findByEmail(auth.getName()).orElse(null);
        List<CartItem> cartItems = cartItemRepository.findAllByCart_CartId(id);
        model.addAttribute("carts", cartItems);
        model.addAttribute("user", user);
        model.addAttribute("cartId", id);
        return "client/product/checkout";
    }

    @GetMapping("/checkout/{serialNumber}")
    public String getCheckout(Model model, @PathVariable("serialNumber") String serialNumber, Authentication auth){
        if  (auth != null && auth.isAuthenticated()) {model.addAttribute("user", userRepository.findByEmail(auth.getName()).orElse(null));};
        List<CartItem> cartItems = new ArrayList<>();
        Product product = productService.getProductBySerialNumber(serialNumber);
        CartItem item = new CartItem();
        item.setProduct(product);
        item.setQuantity(1);
        item.setPrice(product.getPrice());
        cartItems.add(item);
        model.addAttribute("carts", cartItems);
        model.addAttribute("serialNumber", serialNumber);
        return "client/product/checkout";
    }

    @PostMapping("/checkout/confirm")
    @Transactional
    public String confirmCheckout(@Valid @ModelAttribute CheckoutDto form, BindingResult errors, Model model, Authentication auth, @ModelAttribute("carts") List<CartItem> carts){
        // 1) Validate đúng các field trên form
        if (errors.hasErrors()) {
            // Bind lại vài biến view bạn đang dùng để render lại trang
            model.addAttribute("coupon", form.getCoupon());
            model.addAttribute("paymentMethod", form.getPaymentMethod());
            model.addAttribute("receiver", form);
            return "client/product/checkout";
        }
        User user = userRepository.findByEmail(auth.getName()).orElse(null);
        Address address  = new Address();
        address.setUser(user);
        address.setReceiverName(form.getReceiverName());
        address.setPhone(form.getReceiverPhone());
        address.setAddressLine(form.getAddressLine());
        address.setCity(form.getProvince());
        address.setDistrict(form.getDistrict());
        address.setWard(form.getWard());
        address.setIsDefault(false);
        addressRepository.save(address);

        Order order = new Order();
        order.setUser(user);
        order.setStatus(Order.Status.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        orderRepository.save(order); // cần id

        List<OrderItem> items = new ArrayList<>();
//        thanh toán sản phẩm lẻ
        if (form.getSerialNumber() != null && !form.getSerialNumber().isEmpty()) {
            Product product = productService.getProductBySerialNumber(form.getSerialNumber());
            if (product == null) throw new IllegalStateException("Sản phẩm không tồn tại");

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setProduct(product);
            oi.setQuantity(1);
            oi.setTotalPrice(BigDecimal.valueOf(product.getPrice()));
            items.add(oi);
        }

//        Thanh toán toàn giỏ hàng
        else if (form.getCartId() != null) {
            Cart cart = cartRepository.findById(form.getCartId()).orElse(null);
            if (cart == null || cart.getItems().isEmpty())
                throw new IllegalStateException("Giỏ hàng trống");

            for (CartItem ci : cart.getItems()) {
                Product product = ci.getProduct();
                int qty = Math.max(1, ci.getQuantity());
                OrderItem oi = new OrderItem();
                oi.setOrder(order);
                oi.setProduct(product);
                oi.setQuantity(qty);
                oi.setTotalPrice(BigDecimal.valueOf(product.getPrice()).multiply(BigDecimal.valueOf(qty)));
                items.add(oi);
            }
            cart.getItems().clear();
            cartRepository.save(cart);
        }

        if (items.isEmpty()) throw new IllegalStateException("Không có sản phẩm hợp lệ");
        orderItemRepository.saveAll(items);
        order.setItems(items);

        return "redirect:/";
    }
}

