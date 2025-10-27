package com.hhd.jewelry.controller.auth;

import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.service.UserService;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/forgot-password")
@RequiredArgsConstructor
public class ForgotPasswordController {

    private static final Logger log = LoggerFactory.getLogger(ForgotPasswordController.class);

    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    // 🧭 Hiển thị form quên mật khẩu
    @GetMapping
    public String getForgotPage(Model model) {
        if (!model.containsAttribute("email")) {
            model.addAttribute("email", "");
        }
        return "client/homepage/forgot-password";
    }

    // 🧩 Xử lý khi user gửi form đổi mật khẩu
    @PostMapping
    public String postForgotPage(
            @RequestParam("email") @Email String email,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model,
            RedirectAttributes ra) {

        email = (email == null) ? "" : email.trim().toLowerCase();
        log.info("POST /forgot-password -> {}", email);

        // 🛑 1. Kiểm tra email hợp lệ
        if (!StringUtils.hasText(email)) {
            model.addAttribute("error", "Vui lòng nhập email.");
            return "client/homepage/forgot-password";
        }

        var userOpt = userService.findByEmail(email);
        if (userOpt.isEmpty()) {
            model.addAttribute("error", "Email không tồn tại trong hệ thống.");
            model.addAttribute("email", email);
            return "client/homepage/forgot-password";
        }

        // 🔐 2. Kiểm tra mật khẩu hợp lệ
        if (!StringUtils.hasText(password) || password.length() < 8) {
            model.addAttribute("error", "Mật khẩu phải có ít nhất 8 ký tự.");
            model.addAttribute("email", email);
            return "client/homepage/forgot-password";
        }

        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp.");
            model.addAttribute("email", email);
            return "client/homepage/forgot-password";
        }

        // ✅ 3. Cập nhật mật khẩu mới
        try {
            User user = userOpt.get();
            user.setPasswordHash(passwordEncoder.encode(password));
            userService.save(user);

            log.info("Password reset for {}", email);

            ra.addFlashAttribute("registered", "Đổi mật khẩu thành công. Hãy đăng nhập bằng mật khẩu mới.");
            return "redirect:/login";
        } catch (Exception e) {
            log.error("Lỗi khi đổi mật khẩu cho {}: {}", email, e.getMessage());
            model.addAttribute("error", "Không thể đổi mật khẩu: " + e.getMessage());
            return "client/homepage/forgot-password";
        }
    }
}
