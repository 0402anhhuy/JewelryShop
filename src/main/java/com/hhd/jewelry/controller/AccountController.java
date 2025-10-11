package com.hhd.jewelry.controller;

import com.hhd.jewelry.dto.ProfileDto;
import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;

    @GetMapping("/account")
    public String getAccountPage(Authentication auth, Model model) {
        User u = userService.findByEmail(auth.getName()).orElseThrow();

        if (!model.containsAttribute("form")) {
            ProfileDto form = new ProfileDto();
            form.setFullname(u.getFullName());
            form.setEmail(u.getEmail());
            form.setPhone(u.getPhone());
            form.setGender(u.getGender());
            form.setDateOfBirth(u.getDateOfBirth());
            model.addAttribute("form", form);
        }
        return "client_1/homepage/account";
    }

    @PostMapping("/account")
    public String postAccountPage(Authentication auth, @Valid @ModelAttribute("form") ProfileDto form, BindingResult br, RedirectAttributes ra) {
        User u = userService.findByEmail(auth.getName()).orElseThrow();

        if (userService.existsByEmailAndIdNot(form.getEmail(), u.getId())) {
            br.rejectValue("email", "Duplicated", "Email đã được dùng bởi tài khoản khác.");
        }

        if (StringUtils.hasText(form.getPhone()) && userService.existsByPhoneAndIdNot(form.getPhone(), u.getId())) {
            br.rejectValue("phone", "Duplicated", "Số điện thoại đã được dùng.");
        }

        if (br.hasErrors()) return "client_1/homepage/account";

        u.setFullName(form.getFullname());
        u.setEmail(form.getEmail());
        u.setPhone(form.getPhone());
        u.setGender(form.getGender());
        u.setDateOfBirth(form.getDateOfBirth());
        userService.save(u);

        ra.addFlashAttribute("saved", "Cập nhật thông tin thành công.");
        return "redirect:/account";
    }
}
