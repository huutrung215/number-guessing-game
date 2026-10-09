package com.game.guess.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions", indexes = {
    @Index(name = "idx_txn_ref", columnList = "txnRef", unique = true)
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String txnRef;                 // Mã đơn hàng gửi VNPAY

    @Column(nullable = false)
    private String username;                // User mua

    @Column(nullable = false)
    private Integer turnsAmount;            // Số turns sẽ cộng khi thành công

    @Column(nullable = false)
    private Long amountVnd;                 // Số tiền VND (chưa nhân 100)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;           // PENDING / SUCCESS / FAILED

    private String vnpResponseCode;         // Mã phản hồi từ VNPAY
    private String vnpTransactionNo;        // Mã giao dịch VNPAY
    private String bankCode;                // Mã ngân hàng

    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = PaymentStatus.PENDING;
    }

    public enum PaymentStatus { PENDING, SUCCESS, FAILED }
}   