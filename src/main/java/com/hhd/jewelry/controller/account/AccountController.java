package com.hhd.jewelry.controller.account;

import com.hhd.jewelry.dto.ProfileDto;
import com.hhd.jewelry.entity.Address;
import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.entity.Wishlist;
import com.hhd.jewelry.service.AddressService;
import com.hhd.jewelry.service.UserService;
import com.hhd.jewelry.service.WishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class AccountController {

    private final UserService userService;
    private final WishlistService wishlistService;
    private final AddressService addressService;

    @GetMapping("/account")
    public String getAccountPage(
            Authentication auth,
            Model model,
            @RequestParam(defaultValue = "0") int addressPage,
            @RequestParam(defaultValue = "5") int addressSize,
            @RequestParam(defaultValue = "0") int wishlistPage,
            @RequestParam(defaultValue = "8") int wishlistSize) {

        User user = userService.findByEmail(auth.getName()).orElseThrow();
        model.addAttribute("user", user);

        if (!model.containsAttribute("form")) {
            ProfileDto form = new ProfileDto();
            form.setFullname(user.getFullName());
            form.setEmail(user.getEmail());
            form.setPhone(user.getPhone());
            form.setGender(user.getGender());
            form.setDateOfBirth(user.getDateOfBirth());
            model.addAttribute("form", form);
        }

        Page<Address> addresses = addressService.getAddressByUserId(
                user.getId(), PageRequest.of(addressPage, addressSize, Sort.by("isDefault").descending())
        );
        model.addAttribute("addresses", addresses);
        model.addAttribute("addressPage", addressPage);
        model.addAttribute("addressTotalPages", addresses.getTotalPages());

        Page<Wishlist> wishlistItems = wishlistService.getWishlistByUserId(
                user.getId(), PageRequest.of(wishlistPage, wishlistSize, Sort.by("id").descending())
        );
        model.addAttribute("wishlistItems", wishlistItems);
        model.addAttribute("wishlistPage", wishlistPage);
        model.addAttribute("wishlistTotalPages", wishlistItems.getTotalPages());

        return "client_1/homepage/account";
    }

    // Thêm vào AccountController

    @PostMapping("/account/update-ajax")
    @ResponseBody
    public Map<String, Object> updateProfileAjax(
            Authentication auth,
            @Valid @ModelAttribute("form") ProfileDto form,
            BindingResult br) {

        Map<String, Object> response = new HashMap<>();

        User currentUser = userService.findByEmail(auth.getName()).orElseThrow();

        // Validation checks
        if (!form.getEmail().equals(currentUser.getEmail()) &&
                userService.existsByEmailAndIdNot(form.getEmail(), currentUser.getId())) {
            br.rejectValue("email", "Duplicated", "Email đã được dùng bởi tài khoản khác.");
        }

        if (form.getPhone() != null && !form.getPhone().equals(currentUser.getPhone()) &&
                userService.existsByPhoneAndIdNot(form.getPhone(), currentUser.getId())) {
            br.rejectValue("phone", "Duplicated", "Số điện thoại đã được dùng.");
        }

        if (br.hasErrors()) {
            response.put("success", false);
            response.put("toastType", "error");
            response.put("toastTitle", "Lỗi validation");

            // Lấy message từ BindingResult
            String errorMessage = br.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining(", "));

            response.put("toastMessage", errorMessage);
            response.put("errors", br.getFieldErrors().stream()
                    .collect(Collectors.toMap(
                            FieldError::getField,
                            FieldError::getDefaultMessage
                    )));
            return response;
        }

        try {
            currentUser.setEmail(form.getEmail());
            currentUser.setPhone(form.getPhone());
            currentUser.setGender(form.getGender());

            userService.save(currentUser);

            response.put("success", true);
            response.put("toastType", "success");
            response.put("toastTitle", "Thành công");
            response.put("toastMessage", "Cập nhật thông tin thành công!");
            response.put("data", Map.of(
                    "email", currentUser.getEmail(),
                    "phone", currentUser.getPhone(),
                    "gender", currentUser.getGender() != null ? currentUser.getGender().toString() : ""
            ));

        } catch (Exception e) {
            response.put("success", false);
            response.put("toastType", "error");
            response.put("toastTitle", "Lỗi hệ thống");
            response.put("toastMessage", "Không thể cập nhật thông tin: " + e.getMessage());
        }

        return response;
    }
}