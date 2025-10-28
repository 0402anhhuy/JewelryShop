package com.hhd.jewelry.controller.client;

import com.hhd.jewelry.config.MomoConfig;
import com.hhd.jewelry.entity.Order;
import com.hhd.jewelry.entity.User;
import com.hhd.jewelry.service.CartItemService;
import com.hhd.jewelry.service.OrderService;
import com.hhd.jewelry.service.ProductService;
import com.hhd.jewelry.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@AllArgsConstructor
public class PaymentController {
    private final UserService userService;
    private final OrderService orderService;
    private final MomoConfig momoConfig;
    private final ProductService  productService;
    private final CartItemService cartItemService;


    @GetMapping("/vnpay_return")
    public String vnpayReturn(HttpServletRequest request, Model model, Authentication auth) {
        User user = userService.findByEmail(auth.getName()).orElse(null);
        String status = request.getParameter("vnp_ResponseCode");
        String orderIdStr = request.getParameter("vnp_TxnRef");
        if (orderIdStr != null && !orderIdStr.isEmpty()) {
            try {
                Integer orderId = Integer.parseInt(orderIdStr);
                Order order = orderService.findById(orderId);

                if (order != null) {
                    if ("00".equals(status)) {
                        order = orderService.updateOrderStatus(order.getId(), Order.Status.PAID);
                        productService.updateStockForOrder(order);
                        try {
                            int deleted = cartItemService.deleteSelectedByUser(user);
                            model.addAttribute("status", "success");
                            model.addAttribute("message", "Thanh toán thành công!");
                        } catch (IllegalStateException e) {
                            // Xử lý khi kho không đủ hàng
                            order = orderService.updateOrderStatus(order.getId(), Order.Status.CANCELLED);
                            orderService.save(order);
                            model.addAttribute("status", "fail");
                            model.addAttribute("message", "Thanh toán thất bại: " + e.getMessage());
                        }
                    } else {
                        order = orderService.updateOrderStatus(order.getId(), Order.Status.CANCELLED);
                        orderService.save(order);
                        model.addAttribute("status", "fail");
                        model.addAttribute("message", "Thanh toán thất bại. Vui lòng thử lại.");
                    }
                } else {
                    model.addAttribute("status", "fail");
                    model.addAttribute("message", "Không tìm thấy đơn hàng của bạn.");
                }
            } catch (NumberFormatException e) {
                model.addAttribute("status", "fail");
                model.addAttribute("message", "Mã đơn hàng không hợp lệ.");
            }
        } else {
            model.addAttribute("status", "fail");
            model.addAttribute("message", "Giao dịch không hợp lệ.");
        }

        return "client_1/product/payment_result";
    }

    @GetMapping("/momo_return")
    public String momoReturn(
            HttpServletRequest request,
            Model model, Authentication auth
    ) {
        User user = userService.findByEmail(auth.getName()).orElse(null);
        String partnerCode = request.getParameter("partnerCode");
        String orderId = request.getParameter("orderId");
        String requestId = request.getParameter("requestId");
        String amount = request.getParameter("amount");
        String orderInfo = request.getParameter("orderInfo");
        String orderType = request.getParameter("orderType");
        String transId = request.getParameter("transId");
        String resultCode = request.getParameter("resultCode");
        String message = request.getParameter("message");
        String payType = request.getParameter("payType");
        String responseTime = request.getParameter("responseTime");
        String extraData = request.getParameter("extraData");
        String momoSignature = request.getParameter("signature");
        String accessKey = momoConfig.getAccessKey(); // Lấy accessKey từ config

        // Tạo chuỗi raw data để xác thực theo đúng thứ tự alphabet MoMo yêu cầu
        String rawData = "accessKey=" + accessKey +
                "&amount=" + amount +
                "&extraData=" + extraData +
                "&message=" + message +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&orderType=" + orderType +
                "&partnerCode=" + partnerCode +
                "&payType=" + payType +
                "&requestId=" + requestId +
                "&responseTime=" + responseTime +
                "&resultCode=" + resultCode +
                "&transId=" + transId;

        // Hash chuỗi raw data bằng secretKey
        String calculatedSignature = MomoConfig.hmacSHA256(momoConfig.getSecretKey(), rawData);

        // 1. So sánh chữ ký
        if (!calculatedSignature.equals(momoSignature)) {
            model.addAttribute("status", "fail");
            model.addAttribute("message", "Lỗi: Chữ ký không hợp lệ.");
            return "client_1/product/payment_result";
        }

        // 2. Kiểm tra kết quả giao dịch
        try {
            String orderIdDB = orderId.split("-")[0];
            Order order = orderService.findById(Integer.parseInt(orderIdDB));

            if (order == null) {
                model.addAttribute("status", "fail");
                model.addAttribute("message", "Lỗi: Không tìm thấy đơn hàng của bạn.");
                return "client_1/product/payment_result";
            }

            if ("0".equals(resultCode)) {
                order = orderService.updateOrderStatus(order.getId(), Order.Status.PAID);
                productService.updateStockForOrder(order);
                try {
                    int deleted = cartItemService.deleteSelectedByUser(user);
                    model.addAttribute("status", "success");
                    model.addAttribute("message", "Thanh toán bằng MoMo thành công cho đơn hàng #" + orderId);
                } catch (IllegalStateException e) {
                    // Xử lý khi kho không đủ hàng
                    order = orderService.updateOrderStatus(order.getId(), Order.Status.CANCELLED);
                    orderService.save(order);
                    model.addAttribute("status", "fail");
                    model.addAttribute("message", "Thanh toán thất bại: " + e.getMessage());
                }
            } else {
                order = orderService.updateOrderStatus(order.getId(), Order.Status.PAID);
                orderService.save(order);
                model.addAttribute("status", "fail");
                model.addAttribute("message", "Thanh toán thất bại. " + message);
            }
        } catch (NumberFormatException e) {
            model.addAttribute("status", "fail");
            model.addAttribute("message", "Lỗi: Mã đơn hàng không hợp lệ.");
        }

        return "client_1/product/payment_result";
    }
}