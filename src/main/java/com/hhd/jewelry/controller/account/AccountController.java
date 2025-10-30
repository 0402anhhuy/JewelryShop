package com.hhd.jewelry.controller.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hhd.jewelry.dto.ProfileDto;
import com.hhd.jewelry.entity.*;
import com.hhd.jewelry.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    private final UserService userService;
    private final WishlistService wishlistService;
    private final AddressService addressService;
    private final OrderService orderService;
    private final OtpService otpService;
    private final CartService cartService;
    private final ReviewService reviewService;


    @GetMapping("/account")
    public String getAccountPage(
            Authentication auth,
            Model model,
            @RequestParam(defaultValue = "0") int wishlistPage,
            @RequestParam(defaultValue = "8") int wishlistSize) {

        Optional<User> userOpt = userService.findByEmail(auth.getName());

        if (userOpt.isEmpty()) {
            SecurityContextHolder.clearContext();
            return "redirect:/login?error=session_expired";
        }

        User user = userOpt.get();
        model.addAttribute("user", user);

        if (!model.containsAttribute("form")) {
            ProfileDto form = new ProfileDto();
            form.setFullName(user.getFullName());
            form.setEmail(user.getEmail());
            form.setPhone(user.getPhone());
            form.setGender(user.getGender());
            form.setDateOfBirth(user.getDateOfBirth());
            model.addAttribute("form", form);
        }

        int cartSize = 0;
        Cart cart = cartService.getCartByUser(user);
        if (cart != null && cart.getItems() != null) {
            cartSize = cart.getItems().stream()
                    .map(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                    .reduce(0, Integer::sum);
        }
        model.addAttribute("cartSize", cartSize);

        List<Address> addresses = addressService.getAddressByUserId(user.getId());
        model.addAttribute("addresses", addresses);

        Page<Wishlist> wishlistItems = wishlistService.getWishlistByUserId(
                user.getId(), PageRequest.of(wishlistPage, wishlistSize, Sort.by("id").descending())
        );
        model.addAttribute("wishlistItems", wishlistItems);
        model.addAttribute("wishlistPage", wishlistPage);
        model.addAttribute("wishlistTotalPages", wishlistItems.getTotalPages());

        return "client_1/homepage/account";
    }

    @PostMapping("/account/update-ajax")
    @ResponseBody
    public Map<String, Object> updateProfileAjax(
            Authentication auth, @Valid @ModelAttribute("form") ProfileDto form,
            BindingResult br, HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        Optional<User> userOpt = userService.findByEmail(auth.getName());
        if (userOpt.isEmpty()) {
            response.put("success", false);
            response.put("toastType", "error");
            response.put("toastTitle", "Lỗi phiên làm việc");
            response.put("toastMessage", "Phiên làm việc đã hết hạn. Vui lòng đăng nhập lại.");
            return response;
        }

        User currentUser = userOpt.get();

        if (!form.getEmail().equals(currentUser.getEmail()) &&
                userService.existsByEmailAndIdNot(form.getEmail(), currentUser.getId())) {
            br.rejectValue("email", "Duplicated", "Email đã được dùng bởi tài khoản khác");
        }

        if (form.getPhone() != null && !form.getPhone().equals(currentUser.getPhone()) &&
                userService.existsByPhoneAndIdNot(form.getPhone(), currentUser.getId())) {
            br.rejectValue("phone", "Duplicated", "Số điện thoại đã được dùng.");
        }

        if (br.hasErrors()) {
            response.put("success", false);
            response.put("toastType", "error");
            response.put("toastTitle", "Lỗi cập nhật");

            String errorMessage = br.getFieldErrors().stream()
                    .filter(error -> "email".equals(error.getField()) || "phone".equals(error.getField()))
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));

            if (errorMessage.isEmpty()) {
                errorMessage = "Vui lòng kiểm tra lại thông tin!";
            }

            response.put("toastMessage", errorMessage);
            response.put("errors", br.getFieldErrors().stream()
                    .filter(error -> "email".equals(error.getField()) || "phone".equals(error.getField()))
                    .collect(Collectors.toMap(
                            FieldError::getField,
                            FieldError::getDefaultMessage
                    )));
            return response;
        }

        try {
            String oldEmail = currentUser.getEmail();
            boolean emailChanged = !form.getEmail().equals(oldEmail);

            currentUser.setEmail(form.getEmail());

            if (form.getPhone() != null && !form.getPhone().trim().isEmpty()) {
                currentUser.setPhone(form.getPhone());
            }

            userService.save(currentUser);

            if (emailChanged) {
                updateAuthentication(currentUser.getEmail(), auth, request);
                response.put("emailChanged", true);
                response.put("redirectUrl", "/account");
            }

            response.put("success", true);
            response.put("toastType", "success");
            response.put("toastTitle", "Thành công");
            response.put("toastMessage", "Cập nhật thông tin thành công!");
            response.put("data", Map.of(
                    "email", currentUser.getEmail(),
                    "phone", currentUser.getPhone() != null ? currentUser.getPhone() : "",
                    "gender", currentUser.getGender() != null ? currentUser.getGender() : ""
            ));

        } catch (Exception e) {
            response.put("success", false);
            response.put("toastType", "error");
            response.put("toastTitle", "Lỗi hệ thống");
            response.put("toastMessage", "Không thể cập nhật thông tin: " + e.getMessage());
        }

        return response;
    }

    @GetMapping("/account/orders/detail/{orderId}")
    @Transactional(readOnly = true)
    public ResponseEntity<Order> getOrderDetail(@PathVariable("orderId") Integer orderId) {
        Order order = orderService.getOrderByOrderId(orderId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Không tìm thấy đơn hàng với ID: " + orderId));
        return ResponseEntity.ok(order);
    }
    private void updateAuthentication(String newEmail, Authentication oldAuth, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken newAuth = new UsernamePasswordAuthenticationToken(
                newEmail,
                oldAuth.getCredentials(),
                oldAuth.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(newAuth);

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        request.getSession(true);
    }

    @PostMapping("/account/request-otp")
    @ResponseBody
    public String requestOtp(@RequestParam("email") String email) {
        String v = (email == null) ? "" : email.trim().toLowerCase();

        if (!v.endsWith("@gmail.com")) {
            return "INVALID_EMAIL";
        }
        if (!userService.existsByEmail(v)) {
            return "NOT_FOUND";
        }

        try {
            otpService.sendOtp(v);
            log.info("Sent OTP to {}", v);
            return "OK";
        } catch (Exception e) {
            log.error("Send OTP failed for {}: {}", v, e.getMessage());
            return "FAILED";
        }
    }

    @PostMapping("/account/verify-otp")
    @ResponseBody
    public String verifyOtpAndChangePassword(@RequestParam String email,
                                             @RequestParam String otp,
                                             @RequestParam String newPassword) {
        if (!userService.existsByEmail(email)) return "NOT_FOUND";

        boolean valid = otpService.verify(email, otp);
        if (!valid) return "INVALID";

        userService.updatePassword(email, newPassword);
        return "SUCCESS";
    }

    @PostMapping("/reviews/submit")
    public ResponseEntity<String> submitReview(
            @RequestParam("orderId") Integer orderId,
            @RequestParam("reviews") String reviewsJson,
            @RequestParam MultiValueMap<String, MultipartFile> allFiles) {

        log.info("--- (Controller) Đã nhận request /reviews/submit cho Order ID: {}", orderId);
        log.info("--- (Controller) Các keys có trong 'allFiles' ({} keys):", allFiles.size());
        allFiles.keySet().forEach(key -> log.info("--- (Controller) Key nhận được: {}", key));
        // ⭐====== KẾT THÚC THÊM LOG ======⭐
        try {
            Order order = orderService.findById(orderId);

            if (!order.getStatus().name().equals("COMPLETED")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Chỉ có thể đánh giá đơn hàng đã hoàn thành!");
            }

            if (order.getIsReviewed() != null && order.getIsReviewed()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Đơn hàng này đã được đánh giá trước đó!");
            }

            ObjectMapper mapper = new ObjectMapper();
            List<ItemReview> reviews = mapper.readValue(reviewsJson, new TypeReference<>() {});

            Map<Integer, List<MultipartFile>> filesMap = new HashMap<>();
            allFiles.forEach((key, files) -> {
                if (key.startsWith("files_")) {
                    Integer productId = Integer.parseInt(key.substring(6));
                    filesMap.put(productId, files);
                }
            });

            ReviewRequest request = ReviewRequest.builder()
                    .orderId(orderId)
                    .reviews(reviews)
                    .build();
            log.info("--- (Controller) Gọi reviewService.saveAll với {} sản phẩm có file trong map.", filesMap.size());
            reviewService.saveAll(request, filesMap); // ⭐⭐⭐

            order.setIsReviewed(true);
            orderService.save(order);

            return ResponseEntity.ok("Cảm ơn bạn đã đánh giá!");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Lỗi: " + e.getMessage());
        }
    }

    @GetMapping("/reviews/order/{id}")
    public String reviewOrderPage(@PathVariable Integer id, Model model) {
        Order order = orderService.findById(id);
        model.addAttribute("order", order);
        return "client_1/homepage/review-order";
    }
}