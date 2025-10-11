package com.hhd.jewelry.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class RegisterDto {

    @NotBlank(message = "Vui lòng nhập họ và tên")
    private String fullName;

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Vui lòng nhập email")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@gmail\\.com$", message = "Chỉ chấp nhận email @gmail.com")
    private String email;

    @Pattern(regexp = "^(03|05|07|08|09)[0-9]{8}$", message = "Số điện thoại Việt Nam không hợp lệ (VD: 0912345678)")
    private String phone;

    @NotBlank(message = "Vui lòng chọn giới tính")
    private String gender;

    @NotNull(message = "Vui lòng chọn ngày sinh")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Past(message = "Ngày sinh phải ở quá khứ")
    private LocalDate dateOfBirth;

    @Size(max = 255, message = "Địa chỉ quá dài")
    private String address;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự")
    private String password;

    @NotBlank(message = "Vui lòng xác nhận mật khẩu")
    private String confirmPassword;

    @NotBlank
    private String otp;

    @AssertTrue(message = "Mật khẩu nhập lại chưa khớp")
    public boolean isPasswordMatch() {
        if (password == null || confirmPassword == null) return false;
        return password.equals(confirmPassword);
    }
}
