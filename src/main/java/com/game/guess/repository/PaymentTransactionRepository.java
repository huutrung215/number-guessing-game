package com.game.guess.repository;

import com.game.guess.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTxnRef(String txnRef);

    boolean existsByTxnRef(String txnRef);

    /**
     * Atomic update từ PENDING → SUCCESS.
     * Nếu affected = 1 → đây là lần đầu xử lý (an toàn cộng turns).
     * Nếu affected = 0 → đã xử lý rồi (VNPAY retry IPN) → KHÔNG cộng turns nữa.
     */
    @Modifying
    @Query("UPDATE PaymentTransaction p SET p.status = 'SUCCESS', p.paidAt = CURRENT_TIMESTAMP, " +
           "p.vnpResponseCode = :responseCode, p.vnpTransactionNo = :transactionNo, p.bankCode = :bankCode " +
           "WHERE p.txnRef = :txnRef AND p.status = 'PENDING'")
    int markAsSuccess(@Param("txnRef") String txnRef,
                      @Param("responseCode") String responseCode,
                      @Param("transactionNo") String transactionNo,
                      @Param("bankCode") String bankCode);

    @Modifying
    @Query("UPDATE PaymentTransaction p SET p.status = 'FAILED', " +
           "p.vnpResponseCode = :responseCode " +
           "WHERE p.txnRef = :txnRef AND p.status = 'PENDING'")
    int markAsFailed(@Param("txnRef") String txnRef,
                     @Param("responseCode") String responseCode);
}