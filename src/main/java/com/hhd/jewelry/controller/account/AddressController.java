package com.hhd.jewelry.controller.account;

import com.hhd.jewelry.dto.AddressDto;
import com.hhd.jewelry.entity.Address;
import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.service.AddressService;
import com.hhd.jewelry.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;
    private final UserService userService;

    @PostMapping("/address/add-ajax")
    @ResponseBody
    public Map<String, Object> addAddressAjax(
            Authentication auth,
            @Valid @ModelAttribute("addressForm") AddressDto dto,
            BindingResult br) {

        Map<String, Object> response = new HashMap<>();

        if (br.hasErrors()) {
            response.put("success", false);
            response.put("toastType", "error");
            response.put("toastTitle", "Lỗi validation");

            String errorMessage = br.getAllErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage)
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
            address.setAddressType(dto.getAddressType());

            Address savedAddress = addressService.save(address);

            response.put("success", true);
            response.put("toastType", "success");
            response.put("toastTitle", "Thành công");
            response.put("toastMessage", "Thêm địa chỉ mới thành công!");
            response.put("data", Map.of(
                    "id", savedAddress.getId(),
                    "receiverName", savedAddress.getReceiverName(),
                    "phone", savedAddress.getPhone(),
                    "city", savedAddress.getCity(),
                    "district", savedAddress.getDistrict(),
                    "ward", savedAddress.getWard(),
                    "addressLine", savedAddress.getAddressLine(),
                    "addressType", savedAddress.getAddressType(),
                    "isDefault", savedAddress.getIsDefault()
            ));

        } catch (Exception e) {
            response.put("success", false);
            response.put("toastType", "error");
            response.put("toastTitle", "Lỗi hệ thống");
            response.put("toastMessage", "Không thể lưu địa chỉ: " + e.getMessage());
        }

        return response;
    }

    @PostMapping("/address/edit/{id}")
    @ResponseBody
    public Map<String, Object> editAddress(
            Authentication auth,
            @PathVariable Integer id,
            @Valid @ModelAttribute AddressDto dto,
            BindingResult br) {

        Map<String, Object> response = new HashMap<>();

        if (br.hasErrors()) {
            response.put("success", false);
            response.put("toastType", "error");
            response.put("toastTitle", "Lỗi validation");
            response.put("toastMessage", "Vui lòng kiểm tra lại thông tin địa chỉ!");
            response.put("errors", br.getFieldErrors().stream()
                    .collect(Collectors.toMap(
                            FieldError::getField,
                            FieldError::getDefaultMessage
                    )));
            return response;
        }

        try {
            User user = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            Address address = addressService.getAddressById(id);

            if (!address.getUser().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("toastType", "error");
                response.put("toastTitle", "Lỗi quyền truy cập");
                response.put("toastMessage", "Bạn không có quyền chỉnh sửa địa chỉ này!");
                return response;
            }

            boolean wasDefault = address.getIsDefault();

            address.setReceiverName(dto.getReceiverName());
            address.setPhone(dto.getPhone());
            address.setCity(dto.getCity());
            address.setDistrict(dto.getDistrict());
            address.setWard(dto.getWard());
            address.setAddressLine(dto.getAddressLine());
            address.setAddressType(dto.getAddressType());

            boolean newDefaultStatus = dto.getIsDefault() != null ? dto.getIsDefault() : false;

            if (newDefaultStatus && !wasDefault) {
                List<Address> userAddresses = addressService.getAddressesByUser(user);
                for (Address addr : userAddresses) {
                    if (addr.getIsDefault() && !addr.getId().equals(address.getId())) {
                        addr.setIsDefault(false);
                        addressService.save(addr);
                    }
                }
            }

            address.setIsDefault(newDefaultStatus);

            Address updatedAddress = addressService.save(address);

            response.put("success", true);
            response.put("toastType", "success");
            response.put("toastTitle", "Thành công");
            response.put("toastMessage", "Cập nhật địa chỉ thành công!");

            // QUAN TRỌNG: Trả về đầy đủ data để cập nhật UI
            response.put("data", Map.of(
                    "id", updatedAddress.getId(),
                    "receiverName", updatedAddress.getReceiverName(),
                    "phone", updatedAddress.getPhone(),
                    "city", updatedAddress.getCity(),
                    "district", updatedAddress.getDistrict(),
                    "ward", updatedAddress.getWard(),
                    "addressLine", updatedAddress.getAddressLine(),
                    "addressType", updatedAddress.getAddressType(),
                    "isDefault", updatedAddress.getIsDefault()
            ));

        } catch (Exception e) {
            response.put("success", false);
            response.put("toastType", "error");
            response.put("toastTitle", "Lỗi hệ thống");
            response.put("toastMessage", "Không thể cập nhật địa chỉ. Vui lòng thử lại!");
        }

        return response;
    }

    @GetMapping("/address/get/{id}")
    @ResponseBody
    public Map<String, Object> getAddress(Authentication auth, @PathVariable Integer id) {
        Map<String, Object> response = new HashMap<>();

        try {
            User user = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            Address address = addressService.getAddressById(id);

            if (!address.getUser().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("message", "Không có quyền truy cập");
                return response;
            }

            response.put("success", true);
            response.put("receiverName", address.getReceiverName());
            response.put("phone", address.getPhone());
            response.put("city", address.getCity());
            response.put("district", address.getDistrict());
            response.put("ward", address.getWard());
            response.put("addressLine", address.getAddressLine());
            response.put("addressType", address.getAddressType());
            response.put("isDefault", address.getIsDefault());

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Không thể tải thông tin địa chỉ");
        }

        return response;
    }

    @PostMapping("/address/set-default/{id}")
    @ResponseBody
    public Map<String, Object> setDefaultAddress(
            Authentication auth,
            @PathVariable Integer id) {

        Map<String, Object> response = new HashMap<>();

        try {
            User user = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            Address address = addressService.getAddressById(id);

            // Kiểm tra quyền sở hữu
            if (!address.getUser().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("toastType", "error");
                response.put("toastTitle", "Lỗi quyền truy cập");
                response.put("toastMessage", "Bạn không có quyền thay đổi địa chỉ này!");
                return response;
            }

            List<Address> userAddresses = addressService.getAddressesByUser(user);
            for (Address addr : userAddresses) {
                if (addr.getIsDefault()) {
                    addr.setIsDefault(false);
                    addressService.save(addr);
                }
            }

            address.setIsDefault(true);
            addressService.save(address);

            response.put("success", true);
            response.put("toastType", "success");
            response.put("toastTitle", "Thành công");
            response.put("toastMessage", "Đã đặt địa chỉ làm mặc định!");

            response.put("data", Map.of("id", address.getId()));

            return response;

        } catch (Exception e) {
            response.put("success", false);
            response.put("toastType", "error");
            response.put("toastTitle", "Lỗi hệ thống");
            response.put("toastMessage", "Không thể đặt địa chỉ mặc định. Vui lòng thử lại!");
            return response;
        }
    }

    @PostMapping("/address/delete/{id}")
    @ResponseBody
    public Map<String, Object> deleteAddress(
            Authentication auth,
            @PathVariable Integer id) {

        Map<String, Object> response = new HashMap<>();

        try {
            User user = userService.findByEmail(auth.getName())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));

            Address address = addressService.getAddressById(id);

            if (!address.getUser().getId().equals(user.getId())) {
                response.put("success", false);
                response.put("toastType", "error");
                response.put("toastTitle", "Lỗi quyền truy cập");
                response.put("toastMessage", "Bạn không có quyền xóa địa chỉ này!");
                return response;
            }

            boolean wasDefault = address.getIsDefault();

            addressService.delete(address);

            response.put("success", true);
            response.put("toastType", "success");
            response.put("toastTitle", "Thành công");
            response.put("toastMessage", "Đã xóa địa chỉ thành công!");

            if (wasDefault) {
                List<Address> remainingAddresses = addressService.getAddressesByUser(user);
                if (!remainingAddresses.isEmpty()) {
                    Address firstAddress = remainingAddresses.get(0);
                    firstAddress.setIsDefault(true);
                    addressService.save(firstAddress);

                    response.put("data", Map.of("newDefaultId", firstAddress.getId()));
                }
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("toastType", "error");
            response.put("toastTitle", "Lỗi hệ thống");
            response.put("toastMessage", "Không thể xóa địa chỉ. Vui lòng thử lại!");
        }

        return response;
    }
}