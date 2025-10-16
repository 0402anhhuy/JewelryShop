package com.hhd.jewelry.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hhd.jewelry.config.MomoConfig;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

@Service
@AllArgsConstructor
public class MomoService {

    private final MomoConfig momoConfig;
    private final Gson gson = new Gson();

    public String createMomoPayment(long order_Id, long amount, String orderInfo, HttpServletRequest request) throws Exception {
        String partnerCode = momoConfig.getPartnerCode();
        String accessKey = momoConfig.getAccessKey();
        String secretKey = momoConfig.getSecretKey();
        String returnUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + MomoConfig.MOMO_RETURN_URL;
        String ipnUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + MomoConfig.MOMO_IPN_URL;
        String requestId = UUID.randomUUID().toString();
        String orderId = String.valueOf(order_Id) + "-" + System.currentTimeMillis(); // Nên dùng orderId từ hệ thống của bạn
        String requestType = "captureWallet";
        String extraData = ""; // Có thể để trống hoặc truyền thêm dữ liệu JSON base64

        // Chuỗi để tạo chữ ký
        String rawSignature = "accessKey=" + accessKey +
                "&amount=" + amount +
                "&extraData=" + extraData +
                "&ipnUrl=" + ipnUrl +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + partnerCode +
                "&redirectUrl=" + returnUrl + // MoMo dùng redirectUrl thay vì returnUrl
                "&requestId=" + requestId +
                "&requestType=" + requestType;

        String signature = MomoConfig.hmacSHA256(secretKey, rawSignature);

        // Tạo JSON body
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("partnerCode", partnerCode);
        jsonObject.addProperty("accessKey", accessKey);
        jsonObject.addProperty("requestId", requestId);
        jsonObject.addProperty("amount", amount);
        jsonObject.addProperty("orderId", orderId);
        jsonObject.addProperty("orderInfo", orderInfo);
        jsonObject.addProperty("redirectUrl", returnUrl);
        jsonObject.addProperty("ipnUrl", ipnUrl);
        jsonObject.addProperty("extraData", extraData);
        jsonObject.addProperty("requestType", requestType);
        jsonObject.addProperty("signature", signature);
        jsonObject.addProperty("lang", "vi");

        // Gửi yêu cầu POST đến MoMo
        String jsonBody = gson.toJson(jsonObject);
        HttpURLConnection con = (HttpURLConnection) new URL(MomoConfig.MOMO_PAYMENT_URL).openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setDoOutput(true);
        try (DataOutputStream wr = new DataOutputStream(con.getOutputStream())) {
            wr.writeBytes(jsonBody);
            wr.flush();
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }

        JsonObject responseJson = gson.fromJson(response.toString(), JsonObject.class);
        return responseJson.get("payUrl").getAsString();
    }
}
