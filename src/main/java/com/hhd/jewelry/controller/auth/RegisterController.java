package com.hhd.jewelry.controller.auth;

import com.hhd.jewelry.dto.RegisterDto;
import com.hhd.jewelry.entity.Cart;
import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.service.CartService;
import com.hhd.jewelry.service.OtpService;
import com.hhd.jewelry.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/register")
@RequiredArgsConstructor
public class RegisterController {

    private static final Logger log = LoggerFactory.getLogger(RegisterController.class);

    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final UserService userService;
    private final CartService cartService;

    @GetMapping
    public String getRegisterPage(Model model, HttpServletRequest request) {
        request.getSession(true);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterDto());
        }

        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            model.addAttribute("_csrf", token);
        }

        return "client_1/homepage/register";
    }

    @PostMapping("/request-otp")
    @ResponseBody
    public String requestOtp(@RequestParam("email") String email) {
        return normalizeEmail(email, userService, otpService, log);
    }

    @Transactional
    @PostMapping
    public String postRegisterPage(@Valid @ModelAttribute("form") RegisterDto form,
                                   BindingResult br, Model model, RedirectAttributes ra) {
        return logForm(form, br, model, ra, log, userService, passwordEncoder, cartService);
    }

    public String normalizeEmail(@RequestParam("email") String email, UserService userService, OtpService otpService, Logger log) {
        String v = (email == null) ? "" : email.trim().toLowerCase();

        if (!StringUtils.hasText(v) || !v.endsWith("@gmail.com")) {
            return "INVALID_EMAIL";
        }
        if (userService.existsByEmail(v)) {
            return "EXISTS";
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

    public String logForm(@ModelAttribute("form") @Valid RegisterDto form,
                          BindingResult br, Model model, RedirectAttributes ra, Logger log,
                          UserService userService, PasswordEncoder passwordEncoder, CartService cartService) {
        log.info("POST /register -> form: {}", form);

        if (br.hasErrors()) {
            log.warn("Validation error: {}", br);
            ra.addFlashAttribute("form", br);
            ra.addFlashAttribute("form", form);
            return "redirect:/register";
        }

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            ra.addFlashAttribute("toastType", "error");
            ra.addFlashAttribute("toastTitle", "Lỗi mật khẩu");
            ra.addFlashAttribute("toastMessage", "Mật khẩu xác nhận không khớp.");
            ra.addFlashAttribute("form", form);
            return "redirect:/register";
        }

        if (userService.existsByEmail(form.getEmail())) {
            ra.addFlashAttribute("toastType", "error");
            ra.addFlashAttribute("toastTitle", "Lỗi email");
            ra.addFlashAttribute("toastMessage", "Email đã được sử dụng.");
            ra.addFlashAttribute("form", form);
            return "redirect:/register";
        }

        if (StringUtils.hasText(form.getPhone()) && userService.existsByPhone(form.getPhone())) {
            ra.addFlashAttribute("toastType", "error");
            ra.addFlashAttribute("toastTitle", "Lỗi số điện thoại");
            ra.addFlashAttribute("toastMessage", "Số điện thoại đã được sử dụng.");
            ra.addFlashAttribute("form", form);
            return "redirect:/register";
        }

        if (!otpService.verify(form.getEmail(), form.getOtp())) {
            ra.addFlashAttribute("toastType", "error");
            ra.addFlashAttribute("toastTitle", "Lỗi OTP");
            ra.addFlashAttribute("toastMessage", "Mã OTP không đúng hoặc đã hết hạn.");
            ra.addFlashAttribute("form", form);
            return "redirect:/register";
        }

        try {
            User u = new User();
            u.setFullName(form.getFullName());
            u.setEmail(form.getEmail());
            u.setPhone(form.getPhone());
            u.setGender(form.getGender());
            u.setDateOfBirth(form.getDateOfBirth());
            u.setAddress(form.getAddress());
            u.setPasswordHash(passwordEncoder.encode(form.getPassword()));
            u.setRole(User.Role.USER);
            u.setCreatedAt(LocalDateTime.now());

            userService.save(u);

            Cart cart = new Cart();
            cart.setUser(u);
            cartService.save(cart);

            u.setCart(cart);
            userService.save(u);

            log.info("User {} created successfully with cart {}", u.getEmail(), cart.getCartId());

            // THÀNH CÔNG: Hiển thị toast ở trang login
            ra.addFlashAttribute("toastType", "success");
            ra.addFlashAttribute("toastTitle", "Thành công");
            ra.addFlashAttribute("toastMessage", "Đăng ký thành công! Vui lòng đăng nhập.");
            return "redirect:/login";

        } catch (Exception e) {
            log.error("Save user failed", e);
            ra.addFlashAttribute("toastType", "error");
            ra.addFlashAttribute("toastTitle", "Lỗi hệ thống");
            ra.addFlashAttribute("toastMessage", "Không thể lưu người dùng. Vui lòng thử lại.");
            ra.addFlashAttribute("form", form);
            return "redirect:/register";
        }
    }
}