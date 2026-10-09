package com.game.guess.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
public class VnpayService {

    @Value("${vnpay.tmn-code}")     private String tmnCode;
    @Value("${vnpay.hash-secret}")  private String hashSecret;
    @Value("${vnpay.pay-url}")      private String payUrl;
    @Value("${vnpay.return-url}")   private String returnUrl;
    @Value("${vnpay.version}")      private String version;
    @Value("${vnpay.command}")      private String command;
    @Value("${vnpay.curr-code}")    private String currCode;
    @Value("${vnpay.order-type}")   private String orderType;
    @Value("${vnpay.locale}")       private String locale;

    /**
     * Tạo URL thanh toán VNPAY.
     * @param txnRef    mã đơn hàng duy nhất
     * @param amountVnd số tiền VND (chưa nhân 100)
     * @param orderInfo mô tả đơn hàng
     * @param ipAddr    IP khách hàng
     */
    public String createPaymentUrl(String txnRef, long amountVnd, String orderInfo, String ipAddr) {
        String vnp_Version = version;
        String vnp_Command = command;
        String vnp_TmnCode = tmnCode;
        long vnp_Amount = amountVnd * 100L; // VNPAY yêu cầu ×100
        String vnp_CurrCode = currCode;
        String vnp_TxnRef = txnRef;
        String vnp_OrderInfo = orderInfo;
        String vnp_OrderType = orderType;
        String vnp_Locale = locale;
        String vnp_ReturnUrl = returnUrl;
        String vnp_IpAddr = ipAddr;

        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss");
        fmt.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String vnp_CreateDate = fmt.format(new Date());

        // TreeMap đảm bảo sort key theo alphabet
        Map<String, String> vnp_Params = new TreeMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(vnp_Amount));
        vnp_Params.put("vnp_CurrCode", vnp_CurrCode);
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", vnp_OrderInfo);
        vnp_Params.put("vnp_OrderType", vnp_OrderType);
        vnp_Params.put("vnp_Locale", vnp_Locale);
        vnp_Params.put("vnp_ReturnUrl", vnp_ReturnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        // Build hashData + query string (đều encode UTF-8)
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (Map.Entry<String, String> entry : vnp_Params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                hashData.append(key).append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.US_ASCII)).append('&');
                query.append(key).append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.US_ASCII)).append('&');
            }
        }

        // Bỏ dấu & cuối cùng
        hashData.setLength(hashData.length() - 1);
        query.setLength(query.length() - 1);

        String secureHash = hmacSHA512(hashSecret, hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);

        return payUrl + "?" + query;
    }

    /**
     * Verify chữ ký từ params VNPAY trả về.
     * @return true nếu chữ ký hợp lệ
     */
    public boolean verifySignature(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null) return false;

        // Clone và bỏ các field chữ ký
        Map<String, String> copy = new TreeMap<>(params);
        copy.remove("vnp_SecureHash");
        copy.remove("vnp_SecureHashType");

        StringBuilder hashData = new StringBuilder();
        for (Map.Entry<String, String> entry : copy.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (value != null && !value.isEmpty()) {
                hashData.append(key).append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.US_ASCII)).append('&');
            }
        }
        if (hashData.length() > 0) hashData.setLength(hashData.length() - 1);

        String computed = hmacSHA512(hashSecret, hashData.toString());
        return computed.equalsIgnoreCase(receivedHash);
    }

    /** Lấy IP thật của client (đằng sau proxy/nginx) */
    public String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip == null ? "127.0.0.1" : ip;
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA512");
            hmac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            byte[] bytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo HMAC-SHA512", e);
        }
    }
}