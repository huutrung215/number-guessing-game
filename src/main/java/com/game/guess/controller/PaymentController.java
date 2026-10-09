package com.game.guess.controller;

import com.game.guess.dto.PaymentInitResponse;
import com.game.guess.entity.PaymentTransaction;
import com.game.guess.service.PaymentService;
import com.game.guess.service.VnpayService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.http.HttpStatus;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final VnpayService vnpayService;

    /**
     * POST /buy-turns — Tạo đơn hàng và trả URL VNPAY.
     * Thay thế hoàn toàn endpoint /buy-turns cũ.
     */
    @PostMapping("/buy-turns")
    public ResponseEntity<PaymentInitResponse> buyTurns(Authentication auth,
                                                       HttpServletRequest request) {
        String ip = vnpayService.getClientIp(request);
        PaymentInitResponse response = paymentService.createPayment(auth.getName(), ip);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /vnpay-return — User được redirect về đây sau khi thanh toán.
     * CHỈ hiển thị kết quả, KHÔNG cộng turns.
     */
    @GetMapping("/vnpay-return")
    public ResponseEntity<Void> vnpayReturn(@RequestParam Map<String, String> params) {
        boolean validSignature = vnpayService.verifySignature(params);
        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");

        boolean success = validSignature
                && "00".equals(responseCode)
                && "00".equals(transactionStatus);

        // Fallback: xử lý ngay tại Return URL (idempotent)
        if (success) {
            try {
                paymentService.handleIpn(params);
                log.info("✅ Payment processed at Return URL: txnRef={}", txnRef);
            } catch (Exception e) {
                log.warn("⚠️ Fallback failed for txnRef={}", txnRef, e);
                success = false;
            }
        }

        // Redirect về trang game với thông báo
        String status = success ? "success" : "failed";
        String redirectUrl = String.format("/game.html?payment=%s&txnRef=%s",
                status, URLEncoder.encode(txnRef, StandardCharsets.UTF_8));

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    /**
     * GET /vnpay-ipn — VNPAY gọi server-to-server để xác nhận.
     * ĐÂY LÀ NƠI CỘNG TURNS.
     * Phải trả về JSON {"RspCode":"00","Message":"Confirm Success"}.
     */
    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> params) {
        log.info("VNPAY IPN received: txnRef={}", params.get("vnp_TxnRef"));

        PaymentService.IpnResult ipnResult = paymentService.handleIpn(params);

        Map<String, String> body = new HashMap<>();
        body.put("RspCode", ipnResult.rspCode());
        body.put("Message", ipnResult.message());

        return ResponseEntity.ok(body);
    }
}