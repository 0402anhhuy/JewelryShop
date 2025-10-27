package com.hhd.jewelry.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CheckoutDto {
    private String serialNumber;  // nếu != null thì thanh toán sản phẩm lẻ
    private Integer cartId;// nếu != null thì thanh toán giỏ
    // Mã giảm giá (nếu có)
    private String couponCode;
    private String shippingName;
    private Long subtotal;
    private Long discount;
    private Long shippingFee;
    private Long tax;
    private Long total;
    private String note;
    // COD | VNPAY | MOMO
    @NotBlank(message = "Vui lòng chọn phương thức thanh toán")
    private String paymentMethod;
    @NotNull(message = "Vui lòng chọn địa chỉ giao hàng")
    private Integer selectedAddressId;
}
