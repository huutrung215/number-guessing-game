package com.game.guess.dto;

import lombok.*;

@Getter @Setter @AllArgsConstructor
public class PaymentInitResponse {
    private String txnRef;
    private String paymentUrl;
    private long amountVnd;
    private int turnsAmount;
}