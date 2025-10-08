package com.hhd.jewelry.service.impl;

import com.hhd.jewelry.service.MailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
@Service
@Profile("prod")
public class BrevoApiService implements MailService {
    @Value("${BREVO_API_KEY}")
    private String apiKey;

    @Value("${BREVO_SENDER_EMAIL}")
    private String senderEmail;

    @Value("${BREVO_SENDER_NAME}")
    private String senderName;

    private static final String API_URL = "https://api.brevo.com/v3/smtp/email";

    public void send(String to, String subject, String text) {
        try {
            Map<String, Object> payload = Map.of(
                    "sender", Map.of("email", senderEmail, "name", senderName),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "htmlContent", buildHtml(text),
                    "textContent", text
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            String cleanKey = apiKey == null ? "" : apiKey.replaceAll("\\s+", ""); // <- vá ký tự ẩn
            headers.set("api-key", cleanKey);

            HttpEntity<Map<String, Object>> req = new HttpEntity<>(payload, headers);
            RestTemplate rt = new RestTemplate();
            ResponseEntity<String> res = rt.exchange(API_URL, HttpMethod.POST, req, String.class);

            int code = res.getStatusCodeValue();
            if (code != 200 && code != 201 && code != 202) {
                throw new RuntimeException("Brevo HTTP " + code + " body=" + res.getBody());
            }
        } catch (Exception e) {
            throw new RuntimeException("Send OTP failed via Brevo API: " + e.getMessage(), e);
        }
    }

    private String buildHtml(String text) {
        return """
        <html><body>
          <div style="font-family:Arial,Helvetica,sans-serif;max-width:520px;margin:0 auto;">
            <div style="padding:16px 20px;border:1px solid #eee;border-radius:12px;">
              <h2 style="margin:0 0 8px;">Xác thực đăng ký</h2>
              <p style="margin:8px 0;">%s</p>
              <p style="font-size:12px;color:#666;margin-top:16px;">
                Nếu bạn không yêu cầu, vui lòng bỏ qua email này.
              </p>
            </div>
          </div>
        </body></html>
        """.formatted(text);
    }
}
