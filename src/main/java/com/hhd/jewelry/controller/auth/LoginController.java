package com.hhd.jewelry.controller.auth;


import com.hhd.jewelry.dto.LoginDto;
import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class LoginController {

    private final LoginService loginService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String getLoginPage(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new LoginDto());
        }
        return "client_1/homepage/login";
    }

    @PostMapping("/login")
    public String login(@ModelAttribute("form") LoginDto form, RedirectAttributes redirectAttributes, Model model) {
        User user = loginService.findByEmailOrPhone(form.getUsername());

        if (user == null) {
            model.addAttribute("loginError", "Tài khoản không tồn tại!");
            return "client_1/homepage/login";
        }

        if (!passwordEncoder.matches(form.getPassword(), user.getPasswordHash())) {
            model.addAttribute("loginError", "Mật khẩu không đúng!");
            return "client_1/homepage/login";
        }

        redirectAttributes.addFlashAttribute("loginSuccess", true);
        redirectAttributes.addFlashAttribute("userName", user.getFullName());
        return "redirect:/home";
    }
}
