package com.hhd.jewelry.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CheckoutDto {
    private String serialNumber;  // nếu != null thì thanh toán sản phẩm lẻ
    private Integer cartId;// nếu != null thì thanh toán giỏ
    @NotBlank(message = "Họ tên bắt buộc")
    private String receiverName;

    @NotBlank(message = "Số điện thoại bắt buộc")
    @Pattern(regexp = "^(03|05|07|08|09)\\d{8}$",
            message = "Số điện thoại Việt Nam không hợp lệ (VD: 0912345678)")
    private String receiverPhone;

    @Email(message = "Email không hợp lệ")
    private String receiverEmail;

    @NotBlank(message = "Địa chỉ / Số nhà, đường bắt buộc")
    private String addressLine;

    // 3 trường tên địa giới hành chính (được addressPicker điền)
    @NotBlank(message = "Phường/Xã bắt buộc")
    private String ward;

    @NotBlank(message = "Quận/Huyện bắt buộc")
    private String district;

    @NotBlank(message = "Tỉnh/Thành phố bắt buộc")
    private String province;

    // Optional: chuỗi địa chỉ đầy đủ (nếu bạn có input hidden name="fullAddress")
    private String fullAddress;

    // Mã giảm giá (nếu có)
    private String coupon;
    private String shippingName;
    private Long subtotal;
    private Long discount;
    private Long shippingFee;
    private Long tax;
    private Long total;
    // COD | VNPAY | MOMO
    @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
    private String paymentMethod;
}
