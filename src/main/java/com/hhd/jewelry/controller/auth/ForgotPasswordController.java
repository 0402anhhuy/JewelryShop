package com.hhd.jewelry.controller.auth;

import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.service.OtpService;
import com.hhd.jewelry.service.UserService;
import jakarta.servlet.http.HttpSession;
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

import java.util.Map;

@Controller
@RequestMapping("/forgot-password")
@RequiredArgsConstructor
public class ForgotPasswordController {

    private static final Logger log = LoggerFactory.getLogger(ForgotPasswordController.class);

    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final OtpService otpService;

    // ===================================================
    // API 1: GỬI OTP (AJAX)
    // ===================================================
    @PostMapping("/send-otp")
    @ResponseBody
    public Map<String, Object> sendOtp(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        email = (email == null) ? "" : email.trim().toLowerCase();

        // 1. Kiểm tra email có tồn tại (vì đây là Forgot Password)
        if (!userService.existsByEmail(email)) {
            // Trả về OK để tránh tiết lộ thông tin user, nhưng thực tế không gửi
            // Hoặc trả về lỗi để JS hiển thị thông báo
            return Map.of("ok", false, "error", "Email không tồn tại trong hệ thống.");
        }

        try {
            // 2. Gửi mã OTP
            otpService.sendOtp(email);
            log.info("Sent OTP for password reset to {}", email);
            return Map.of("ok", true);
        } catch (Exception e) {
            log.error("Failed to send OTP to {}: {}", email, e.getMessage());
            return Map.of("ok", false, "error", "Lỗi hệ thống khi gửi mã OTP.");
        }
    }

    // TRONG ForgotPasswordController.java

    @PostMapping("/verify-otp")
    @ResponseBody
    public Map<String, Object> verifyOtp(@RequestBody Map<String, String> payload, HttpSession session) {
        String email = payload.get("email");
        String otp = payload.get("otp");
        email = (email == null) ? "" : email.trim().toLowerCase();

        // 1. Kiểm tra email (giữ nguyên)
        if (!userService.existsByEmail(email)) {
            return Map.of("verified", false, "error", "Email không hợp lệ.");
        }

        // 2. Gọi Service để xác minh và lưu Session
        if (otpService.verify(email, otp)) {
            // ✅ Sửa: Chỉ cần kiểm tra một lần. Nếu đúng thì lưu Session.
            session.setAttribute("OTP_VERIFIED_EMAIL", email);
            return Map.of("verified", true);
        }
        else {
            // Xóa Session nếu mã OTP sai hoặc hết hạn
            session.removeAttribute("OTP_VERIFIED_EMAIL");
            return Map.of("verified", false, "error", "Mã OTP không đúng hoặc đã hết hạn.");
        }
    }

    @GetMapping
    public String getForgotPage(Model model) {
        // ... (Giữ nguyên)
        return "client_1/homepage/forgot-password";
    }

    @PostMapping
    public String postForgotPage(
            @RequestParam("email") @Email String email,
            @RequestParam("password") String password,
            @RequestParam("confirmPassword") String confirmPassword,
            Model model,
            RedirectAttributes ra,
            HttpSession session) { // ✅ THÊM HttpSession

        email = (email == null) ? "" : email.trim().toLowerCase();

        // --- 1. KIỂM TRA TRẠNG THÁI XÁC MINH OTP TỪ SESSION ---
        String verifiedEmail = (String) session.getAttribute("OTP_VERIFIED_EMAIL");

        if (verifiedEmail == null || !verifiedEmail.equalsIgnoreCase(email)) {
            log.warn("Attempt to reset password without valid OTP verification for: {}", email);
            model.addAttribute("error", "Vui lòng gửi và xác minh mã OTP trước khi đặt lại mật khẩu.");
            model.addAttribute("email", email);
            // Sau khi lỗi, không hiển thị trang đặt lại mật khẩu.
            return "client_1/homepage/forgot-password";
        }
        // -----------------------------------------------------

        var userOpt = userService.findByEmail(email);

        // 2. Validation cơ bản (giữ nguyên)
        if (userOpt.isEmpty() || !StringUtils.hasText(password) || password.length() < 8 || !password.equals(confirmPassword)) {
            // ... (xử lý lỗi validation và trả về trang) ...
            // Cần đảm bảo rằng email vẫn được truyền vào model khi lỗi validation.
            model.addAttribute("email", email);
            return "client_1/homepage/forgot-password";
        }

        // 3. Cập nhật mật khẩu
        try {
            User user = userOpt.get();
            user.setPasswordHash(passwordEncoder.encode(password));
            userService.save(user); // ✅ LƯU MẬT KHẨU MỚI

            // ✅ XÓA CỜ SAU KHI THÀNH CÔNG ĐỂ NGĂN DÙNG LẠI FORM
            session.removeAttribute("OTP_VERIFIED_EMAIL");

            ra.addFlashAttribute("registered", "Đổi mật khẩu thành công. Hãy đăng nhập bằng mật khẩu mới.");
            return "redirect:/login";
        } catch (Exception e) {
            log.error("Lỗi khi đổi mật khẩu cho {}: {}", email, e.getMessage());
            model.addAttribute("error", "Không thể đổi mật khẩu: " + e.getMessage());
            model.addAttribute("email", email);
            return "client_1/homepage/forgot-password";
        }
    }
}