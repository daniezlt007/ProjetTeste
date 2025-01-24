package com.deadsystem.ApiClientAutorizador.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {

    private double value;
    private String cardNumber;
    private int installments;
    private int expMonth;
    private int expYear;
    private String cvv;

}
