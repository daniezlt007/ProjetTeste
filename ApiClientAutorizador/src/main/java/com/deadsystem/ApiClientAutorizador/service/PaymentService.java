package com.deadsystem.ApiClientAutorizador.service;

import com.deadsystem.ApiClientAutorizador.model.PaymentRequest;
import com.deadsystem.ApiClientAutorizador.model.PaymentResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class PaymentService {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8583;

    public String sendISO8583Message(String isoMessage) {
        try (Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
             OutputStream output = socket.getOutputStream();
             InputStream input = socket.getInputStream()) {

            // Envia a mensagem ao servidor
            output.write(isoMessage.getBytes());
            output.flush();

            // Lê a resposta do servidor
            byte[] buffer = new byte[1024];
            int bytesRead = input.read(buffer);

            if (bytesRead <= 0) {
                throw new IOException("Servidor não enviou resposta ou conexão foi encerrada.");
            }

            return new String(buffer, 0, bytesRead);

        } catch (Exception e) {
            throw new RuntimeException("Erro ao comunicar com o servidor: " + e.getMessage(), e);
        }
    }

    public String generateISO8583Message(PaymentRequest request) {
        StringBuilder message = new StringBuilder();

        // MTI
        message.append("0200");

        // BIT 2 - Número do Cartão (LLVAR)
        String cardNumber = request.getCardNumber();
        message.append(String.format("%-32s", cardNumber != null ? cardNumber : ""));

        // BIT 3 - Código de Processamento
        message.append(String.format("%06d", request.getInstallments() == 1 ? 3000 : 3001));

        // BIT 4 - Valor da Transação
        message.append(String.format("%012d", (int) (request.getValue() * 100)));

        // BIT 7 - Data/Hora GMT (MMDDHHMMSS)
        message.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss")));

        // BIT 11 - NSU
        message.append(String.format("%06d", UUID.randomUUID().hashCode() & 0xfffff));

        // BIT 12 - Hora Local (HHMMSS)
        message.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss")));

        // BIT 13 - Data Local (MMDD)
        message.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd")));

        // BIT 14 - Data de Vencimento (AAMM)
        message.append(String.format("%02d%02d", request.getExpYear(), request.getExpMonth()));

        // BIT 42 - Código do Estabelecimento (15 caracteres fixos)
        message.append(String.format("%-15s", "IDENTIFIER"));

        // BIT 48 - Código de Segurança
        String cvv = request.getCvv();
        message.append(String.format("%03d", Integer.parseInt(cvv != null ? cvv : "0")));

        // BIT 22 - Número de Parcelas
        message.append(String.format("%02d", request.getInstallments()));

        return message.toString();
    }

    public PaymentResponse parseISO8583Response(String serverResponse, PaymentRequest request) {
        if (serverResponse.length() < 77) { // Ajustado para o comprimento correto da resposta
            throw new RuntimeException("Mensagem incompleta recebida do servidor: " + serverResponse);
        }

        PaymentResponse response = new PaymentResponse();
        response.setPaymentId(UUID.randomUUID().toString());
        response.setValue(request.getValue());
        response.setAuthorizationCode(serverResponse.substring(48, 54).trim()); // Código de autorização (BIT 38)
        response.setResponseCode(serverResponse.substring(54, 57).trim());      // Código de resposta (BIT 39)
        response.setTransactionDate(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yy-MM-dd")));
        response.setTransactionHour(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        return response;
    }


}
