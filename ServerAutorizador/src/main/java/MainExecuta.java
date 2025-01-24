
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class MainExecuta {

    private static final int PORT = 8583; // Porta para escutar conexões

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor Autorizador ISO8583 iniciado na porta " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado: " + clientSocket.getInetAddress());
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) {
        try (
                InputStream input = clientSocket.getInputStream();
                OutputStream output = clientSocket.getOutputStream()
        ) {
            // Lê a mensagem do cliente
            byte[] buffer = new byte[1024];
            int bytesRead = input.read(buffer);

            if (bytesRead <= 0) {
                System.err.println("Nenhuma mensagem recebida ou conexão fechada pelo cliente.");
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

    private static String processISO8583Message(String message) {
        try {
            Map<String, String> fields = decodeISO8583Message(message);
            double transactionValue = Double.parseDouble(fields.getOrDefault("4", "0"));

            if (transactionValue > 1000.00) {
                // Simular timeout
                System.out.println("Transação acima de R$ 1000. Timeout simulado.");
                return null;
            }

            String responseCode = transactionValue % 2 == 0 ? "000" : "051";
            return encodeISO8583Response(fields, responseCode);

        } catch (Exception e) {
            System.err.println("Erro ao processar mensagem: " + e.getMessage());
            return encodeErrorResponse("Erro ao processar mensagem");
        }
    }

    private static Map<String, String> decodeISO8583Message(String message) {
        Map<String, String> fields = new HashMap<>();

        try {
            // Mapear campos de acordo com o layout especificado
            fields.put("2", message.substring(4, 36).trim()); // Número do cartão
            fields.put("3", message.substring(36, 42).trim()); // Código de processamento
            fields.put("4", message.substring(42, 54).trim()); // Valor da transação
            fields.put("7", message.substring(54, 64).trim()); // Data/Hora Transmissão GMT
            fields.put("11", message.substring(64, 70).trim()); // NSU
            fields.put("12", message.substring(70, 76).trim()); // Hora Local (HHMMSS)
            fields.put("13", message.substring(76, 80).trim()); // Data Local (MMDD)
            fields.put("14", message.substring(80, 84).trim()); // Data de vencimento
        } catch (StringIndexOutOfBoundsException e) {
            System.err.println("Mensagem incompleta: " + e.getMessage());
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

    private static String encodeErrorResponse(String errorMessage) {
        StringBuilder response = new StringBuilder();
        response.append("0210"); // MTI para erro
        response.append("000000000000"); // Valor fictício
        response.append("0000000000"); // Data/Hora fictícia
        response.append("000000"); // NSU fictício
        response.append("000000"); // Hora fictícia
        response.append("0000");   // Data fictícia
        response.append("      "); // Código de autorização vazio
        response.append("999");    // Código de erro genérico
        System.err.println("Erro codificado na resposta: " + errorMessage);
        return response.toString();
    }

}
