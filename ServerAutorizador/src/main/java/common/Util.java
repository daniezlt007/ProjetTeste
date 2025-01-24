package common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Util {

    public static void handleClient(Socket clientSocket) {
        try (
                InputStream input = clientSocket.getInputStream();
                OutputStream output = clientSocket.getOutputStream()
        ) {
            // Lê a mensagem do cliente
            byte[] buffer = new byte[1024];
            int bytesRead = input.read(buffer);
            if (bytesRead == -1) {
                System.err.println("Nenhuma mensagem recebida do cliente.");
                return;
            }
            String message = new String(buffer, 0, bytesRead);

            System.out.println("Mensagem recebida: " + message);
            String response = processISO8583Message(message);

            // Simula timeout se a resposta for null
            if (response == null) {
                System.out.println("Timeout simulado para a mensagem.");
                return;
            }
            output.write(response.getBytes());
            output.flush();

        } catch (IOException e) {
            System.err.println("Erro ao processar cliente: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar socket do cliente: " + e.getMessage());
            }
        }
    }


    public static String processISO8583Message(String message) {
        try {
            // Decodificar a mensagem ISO8583
            Map<String, String> fields = decodeISO8583Message(message);
            // Regras de negócio
            //double transactionValue = Double.parseDouble(fields.get("4")) / 100.0;
            double transactionValue = Double.parseDouble(fields.getOrDefault("4", "0"));
            if (transactionValue > 1000.00) {
                // Simular timeout
                return null;
            }
            String responseCode = transactionValue % 2 == 0 ? "000" : "051";
            // Criar resposta ISO8583
            return encodeISO8583Response(fields, responseCode);

        } catch (Exception e) {
            System.err.println("Erro ao processar mensagem: " + e.getMessage());
            return null;
        }
    }

    private static Map<String, String> decodeISO8583Message(String message) {
        Map<String, String> fields = new HashMap<>();

        try {
            // Ajustar posições com base no layout enviado pelo cliente
            if (message.length() >= 36) {
                fields.put("2", message.substring(4, 36).trim()); // Número do cartão
            }
            if (message.length() >= 42) {
                fields.put("3", message.substring(36, 42).trim()); // Código de processamento
            }
            if (message.length() >= 54) {
                fields.put("4", message.substring(42, 54).trim()); // Valor da transação
            }
            if (message.length() >= 64) {
                fields.put("7", message.substring(54, 64).trim()); // Data/Hora Transmissão GMT
            }
            if (message.length() >= 70) {
                fields.put("11", message.substring(64, 70).trim()); // NSU
            }
            if (message.length() >= 76) {
                fields.put("12", message.substring(70, 76).trim()); // Hora Local (HHMMSS)
            }
            if (message.length() >= 80) {
                fields.put("13", message.substring(76, 80).trim()); // Data Local (MMDD)
            }
            if (message.length() >= 84) {
                fields.put("14", message.substring(80, 84).trim()); // Data de vencimento
            }
            if (message.length() >= 99) {
                fields.put("42", message.substring(84, 99).trim()); // Código do Estabelecimento
            }
            if (message.length() >= 102) {
                fields.put("48", message.substring(99, 102).trim()); // Código de Segurança (CVV)
            }
        } catch (Exception e) {
            System.err.println("Erro ao decodificar a mensagem: " + e.getMessage());
        }

        return fields;
    }

    private static String encodeISO8583Response(Map<String, String> requestFields, String responseCode) {
        StringBuilder response = new StringBuilder();
        response.append("0210"); // MTI para resposta
        response.append(requestFields.getOrDefault("4", "000000000000")); // Valor da transação (12 dígitos)
        response.append(requestFields.getOrDefault("7", "0000000000"));   // Data/Hora Transmissão GMT (10 dígitos)
        response.append(requestFields.getOrDefault("11", "000000"));      // NSU (6 dígitos)
        response.append(requestFields.getOrDefault("12", "000000"));      // Hora Local (6 dígitos)
        response.append(requestFields.getOrDefault("13", "0000"));        // Data Local (4 dígitos)
        response.append("123456");                                        // Código de autorização fictício (6 dígitos)
        response.append(responseCode);                                    // Código de resposta (3 dígitos)
        response.append("               ");                               // Código do estabelecimento (15 espaços)
        response.append("000000000000");                                  // NSU Host (12 dígitos fictícios)

        System.out.println("Resposta gerada pelo servidor: " + response.toString());
        return response.toString();
    }

}
