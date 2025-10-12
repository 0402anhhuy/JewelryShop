package com.hhd.jewelry.controller.account;

import com.hhd.jewelry.dto.AddressDto;
import com.hhd.jewelry.entity.Address;
import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.service.AddressService;
import com.hhd.jewelry.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/account/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final UserService userService;

    @PostMapping("/add")
    public String addAddress(
            Authentication auth,
            @Valid @ModelAttribute("addressForm") AddressDto dto,
            BindingResult br, RedirectAttributes ra) {

        if (br.hasErrors()) {
            ra.addFlashAttribute("toastType", "error");
            ra.addFlashAttribute("toastTitle", "Lỗi validation");
            ra.addFlashAttribute("toastMessage", "Vui lòng kiểm tra lại thông tin địa chỉ!");
            ra.addFlashAttribute("org.springframework.validation.BindingResult.addressForm", br);
            ra.addFlashAttribute("addressForm", dto);
            return "redirect:/account#addresses";
        }

        try {
            User user = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            Address address = new Address();
            address.setUser(user);
            address.setReceiverName(dto.getReceiverName());
            address.setPhone(dto.getPhone());
            address.setCity(dto.getCity());
            address.setDistrict(dto.getDistrict());
            address.setWard(dto.getWard());
            address.setAddressLine(dto.getAddressLine());
            address.setIsDefault(dto.getIsDefault() != null ? dto.getIsDefault() : false);

            addressService.save(address);

            ra.addFlashAttribute("toastType", "success");
            ra.addFlashAttribute("toastTitle", "Thành công");
            ra.addFlashAttribute("toastMessage", "Thêm địa chỉ mới thành công!");

        } catch (Exception e) {
            ra.addFlashAttribute("toastType", "error");
            ra.addFlashAttribute("toastTitle", "Lỗi hệ thống");
            ra.addFlashAttribute("toastMessage", "Không thể lưu địa chỉ. Vui lòng thử lại!");
        }

        return "redirect:/account#addresses";
    }

    @PostMapping("/edit/{id}")
    public String editAddress(
            Authentication auth,
            @PathVariable Integer id,
            @Valid @ModelAttribute("addressForm") AddressDto dto,
            BindingResult br,
            RedirectAttributes ra) {

        if (br.hasErrors()) {
            ra.addFlashAttribute("toastType", "error");
            ra.addFlashAttribute("toastTitle", "Lỗi validation");
            ra.addFlashAttribute("toastMessage", "Vui lòng kiểm tra lại thông tin địa chỉ!");
            ra.addFlashAttribute("org.springframework.validation.BindingResult.addressForm", br);
            ra.addFlashAttribute("addressForm", dto);
            return "redirect:/account#addresses";
        }

        try {
            User user = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            Address address = addressService.getAddressById(id);

            // Kiểm tra quyền sở hữu
            if (!address.getUser().getId().equals(user.getId())) {
                ra.addFlashAttribute("toastType", "error");
                ra.addFlashAttribute("toastTitle", "Lỗi quyền truy cập");
                ra.addFlashAttribute("toastMessage", "Bạn không có quyền chỉnh sửa địa chỉ này!");
                return "redirect:/account#addresses";
            }

            // Cập nhật thông tin từ DTO
            address.setReceiverName(dto.getReceiverName());
            address.setPhone(dto.getPhone());
            address.setCity(dto.getCity());
            address.setDistrict(dto.getDistrict());
            address.setWard(dto.getWard());
            address.setAddressLine(dto.getAddressLine());
            address.setIsDefault(dto.getIsDefault() != null ? dto.getIsDefault() : false);

            addressService.save(address);

            ra.addFlashAttribute("toastType", "success");
            ra.addFlashAttribute("toastTitle", "Thành công");
            ra.addFlashAttribute("toastMessage", "Cập nhật địa chỉ thành công!");

        } catch (Exception e) {
            ra.addFlashAttribute("toastType", "error");
            ra.addFlashAttribute("toastTitle", "Lỗi hệ thống");
            ra.addFlashAttribute("toastMessage", "Không thể cập nhật địa chỉ. Vui lòng thử lại!");
        }

        return "redirect:/account#addresses";
    }

    @PostMapping("/delete/{id}")
    public String deleteAddress(
            Authentication auth,
            @PathVariable Integer id,
            RedirectAttributes ra) {

        try {
            User user = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            Address address = addressService.getAddressById(id);

            // Kiểm tra quyền sở hữu
            if (!address.getUser().getId().equals(user.getId())) {
                ra.addFlashAttribute("toastType", "error");
                ra.addFlashAttribute("toastTitle", "Lỗi quyền truy cập");
                ra.addFlashAttribute("toastMessage", "Bạn không có quyền xóa địa chỉ này!");
                return "redirect:/account#addresses";
            }

            addressService.delete(address);

            ra.addFlashAttribute("toastType", "success");
            ra.addFlashAttribute("toastTitle", "Thành công");
            ra.addFlashAttribute("toastMessage", "Đã xóa địa chỉ thành công!");

        } catch (Exception e) {
            ra.addFlashAttribute("toastType", "error");
            ra.addFlashAttribute("toastTitle", "Lỗi hệ thống");
            ra.addFlashAttribute("toastMessage", "Không thể xóa địa chỉ. Vui lòng thử lại!");
        }

        return "redirect:/account#addresses";
    }
}