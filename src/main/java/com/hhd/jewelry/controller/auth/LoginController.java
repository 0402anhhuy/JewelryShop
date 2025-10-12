package com.hhd.jewelry.controller.auth;

import com.hhd.jewelry.dto.LoginDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class LoginController {
    @GetMapping("/login")
    public String getLoginPage(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            Model model) {

        if (error != null) {
            model.addAttribute("toastType", "error");
            model.addAttribute("toastTitle", "Lỗi đăng nhập");
            model.addAttribute("toastMessage", "Tài khoản hoặc mật khẩu không đúng!");
        }

        if (logout != null) {
            model.addAttribute("toastType", "success");
            model.addAttribute("toastTitle", "Thành công");
            model.addAttribute("toastMessage", "Đã đăng xuất thành công!");
        }

        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new LoginDto());
        }

        return "client_1/homepage/login";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login?logout=true";
    }
}