package com.deadsystem.ApiClientAutorizador.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentResponse {

    private String paymentId;
    private double value;
    private String responseCode;
    private String authorizationCode;
    private String transactionDate;
    private String transactionHour;

}
