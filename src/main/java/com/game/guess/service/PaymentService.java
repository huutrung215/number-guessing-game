package com.game.guess.service;

import com.game.guess.dto.PaymentInitResponse;
import com.game.guess.entity.PaymentTransaction;
import com.game.guess.entity.PaymentTransaction.PaymentStatus;
import com.game.guess.repository.PaymentTransactionRepository;
import com.game.guess.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentTransactionRepository paymentRepo;
    private final UserRepository userRepo;
    private final VnpayService vnpayService;

    @Value("${app.game.buy-turns-amount}")
    private int buyTurnsAmount;

    @Value("${app.game.turn-price-vnd}")
    private long turnPriceVnd;

    /**
     * Bước 1: Tạo đơn hàng + trả URL VNPAY cho client redirect.
     */
    @Transactional
    public PaymentInitResponse createPayment(String username, String ipAddr) {
        long amountVnd = turnPriceVnd;
        String txnRef = "TURNS_" + username + "_" + UUID.randomUUID().toString().substring(0, 8);

        // Lưu đơn hàng PENDING trước
        PaymentTransaction tx = PaymentTransaction.builder()
                .txnRef(txnRef)
                .username(username)
                .turnsAmount(buyTurnsAmount)
                .amountVnd(amountVnd)
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepo.save(tx);

        String orderInfo = "Mua " + buyTurnsAmount + " luot choi game doan so";
        String paymentUrl = vnpayService.createPaymentUrl(txnRef, amountVnd, orderInfo, ipAddr);

        log.info("Created payment txnRef={} user={} amount={}", txnRef, username, amountVnd);
        return new PaymentInitResponse(txnRef, paymentUrl, amountVnd, buyTurnsAmount);
    }

    /**
     * Bước 2: Xử lý IPN từ VNPAY — nguồn tin cậy để cộng turns.
     * Đảm bảo idempotency: chỉ cộng turns nếu UPDATE PENDING→SUCCESS thành công.
     */
    @Transactional
    public IpnResult handleIpn(Map<String, String> params) {
        // 2.1. Verify chữ ký
        if (!vnpayService.verifySignature(params)) {
            log.warn("Invalid signature for txnRef={}", params.get("vnp_TxnRef"));
            return new IpnResult("97", "Invalid Checksum");
        }

        String txnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionStatus = params.get("vnp_TransactionStatus");
        String transactionNo = params.get("vnp_TransactionNo");
        String bankCode = params.get("vnp_BankCode");
        String amountStr = params.get("vnp_Amount");

        // 2.2. Tìm đơn
        PaymentTransaction tx = paymentRepo.findByTxnRef(txnRef).orElse(null);
        if (tx == null) {
            log.warn("Order not found txnRef={}", txnRef);
            return new IpnResult("01", "Order not Found");
        }

        // 2.3. Kiểm tra amount khớp (chống giả mạo)
        long vnpAmount = Long.parseLong(amountStr);
        if (vnpAmount != tx.getAmountVnd() * 100L) {
            log.warn("Amount mismatch txnRef={} expected={} got={}",
                    txnRef, tx.getAmountVnd() * 100, vnpAmount);
            return new IpnResult("04", "Invalid Amount");
        }

        // 2.4. Nếu đã SUCCESS → trả về 02 (idempotent)
        if (tx.getStatus() == PaymentStatus.SUCCESS) {
            return new IpnResult("02", "Order already confirmed");
        }

        // 2.5. Xử lý theo mã phản hồi
        boolean success = "00".equals(responseCode) && "00".equals(transactionStatus);

        if (success) {
            // ATOMIC update PENDING → SUCCESS
            int affected = paymentRepo.markAsSuccess(txnRef, responseCode, transactionNo, bankCode);
            if (affected == 0) {
                // Đã có request khác xử lý trước → không cộng turns nữa
                log.info("txnRef={} already processed by another request", txnRef);
                return new IpnResult("02", "Order already confirmed");
            }
            // Cộng turns cho user
            userRepo.addTurnsByUsername(tx.getUsername(), tx.getTurnsAmount());
            log.info("Payment success: user={} +{} turns (txnRef={})",
                    tx.getUsername(), tx.getTurnsAmount(), txnRef);
        } else {
            paymentRepo.markAsFailed(txnRef, responseCode);
            log.info("Payment failed: txnRef={} code={}", txnRef, responseCode);
        }

        return new IpnResult("00", "Confirm Success");
    }

    /**
     * Bước 3: Kiểm tra kết quả cho Return URL (chỉ hiển thị, KHÔNG cộng turns).
     */
    public PaymentTransaction getByTxnRef(String txnRef) {
        return paymentRepo.findByTxnRef(txnRef).orElse(null);
    }

    public record IpnResult(String rspCode, String message) {}
}