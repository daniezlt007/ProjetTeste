package com.deadsystem.ApiClientAutorizador.controller;

import com.deadsystem.ApiClientAutorizador.model.PaymentRequest;
import com.deadsystem.ApiClientAutorizador.model.PaymentResponse;
import com.deadsystem.ApiClientAutorizador.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/authorization")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping
    public Map<String, Object> authorizePayment(@RequestBody PaymentRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            String isoMessage = paymentService.generateISO8583Message(request);

            String serverResponse = paymentService.sendISO8583Message(isoMessage);
            PaymentResponse paymentResponse = paymentService.parseISO8583Response(serverResponse, request);
            response.put("payment_id", paymentResponse.getPaymentId());
            response.put("value", paymentResponse.getValue());
            response.put("response_code", paymentResponse.getResponseCode());
            response.put("authorization_code", paymentResponse.getAuthorizationCode());
            response.put("transaction_date", paymentResponse.getTransactionDate());
            response.put("transaction_hour", paymentResponse.getTransactionHour());

        } catch (Exception e) {
            response.put("error", "Erro ao processar a transação: " + e.getMessage());
        }

        return response;
    }

}