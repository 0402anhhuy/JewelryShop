package com.hhd.jewelry.service;

import com.hhd.jewelry.config.VNPayConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class VNPayService {

    // Inject bean VNPayConfig vào đây thông qua constructor
    private final VNPayConfig vnpayConfig;

    public VNPayService(VNPayConfig vnpayConfig) {
        this.vnpayConfig = vnpayConfig;
    }

    public String createPaymentUrl(long orderId, long total, String orderInfo, HttpServletRequest request) throws UnsupportedEncodingException {
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = String.valueOf(orderId);
        String vnp_IpAddr = VNPayConfig.getIpAddress(request); // Dùng phương thức static

        // Lấy giá trị từ bean đã được inject, KHÔNG dùng static
        String vnp_TmnCode = vnpayConfig.getTmnCode();

        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(total * 100));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", orderInfo);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");

        String returnUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + VNPayConfig.VNP_RETURN_URL; // Dùng hằng số static
        vnp_Params.put("vnp_ReturnUrl", returnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        // Đảm bảo múi giờ là GMT+7 cho VNPAY
//        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT-7"));
//        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
//        String vnp_CreateDate = formatter.format(cld.getTime());
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String vnp_CreateDate = now.format(formatter);
        String vnp_ExpireDate = now.plusMinutes(15).format(formatter);

        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);
//
//        cld.add(Calendar.MINUTE, 15);
//        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);
        // THÊM LẠI 4 DÒNG NÀY ĐỂ DEBUG
        System.out.println("--- VNPAY DEBUG ---");
        System.out.println("Final CreateDate sent: " + vnp_CreateDate);
        System.out.println("Final ExpireDate sent: " + vnp_ExpireDate);
        System.out.println(vnp_TmnCode);
        System.out.println("--- END DEBUG ---");
        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        // ... (phần code hash và build query giữ nguyên)
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String queryUrl = query.toString();
        // Lấy giá trị từ bean đã được inject, KHÔNG dùng static
        String vnp_SecureHash = VNPayConfig.hmacSHA512(vnpayConfig.getHashSecret(), hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;

        return VNPayConfig.VNP_PAY_URL + "?" + queryUrl; // Dùng hằng số static
    }
}