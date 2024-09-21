package com.udp;

import java.io.IOException;
import java.net.*;

public class UdpGateway {

    private static final String[] INSTANCES_IP = {"127.0.0.1", "127.0.0.1"};
    private static final int[] INSTANCES_PORT = {9001, 9002};
    private static final int GATEWAY_PORT = 8080;
    private static final int TIMEOUT = 2000;


    private static int currentInstanceIndex = 0;

    public static void main(String[] args) {
        try (DatagramSocket gatewaySocket = new DatagramSocket(GATEWAY_PORT)) {
            System.out.println("UDP Gateway iniciado e escutando na porta " + GATEWAY_PORT);

            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket clientPacket = new DatagramPacket(buffer, buffer.length);
                gatewaySocket.receive(clientPacket);

                String receivedData = new String(clientPacket.getData(), 0, clientPacket.getLength());
                System.out.println("Pacote recebido: " + receivedData);
                
                String response;
                if (isValidRequest(receivedData)) {
                    //recebe resposta do servidor
                    response = forwardPacket(receivedData);
                } else {
                    response = "Erro: Solicitação inválida";
                }
                // Envia a resposta de volta para o cliente original
                byte[] responseData = response.getBytes();
                DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length,
                        clientPacket.getAddress(), clientPacket.getPort());
                gatewaySocket.send(responsePacket);
                System.out.println("Resposta enviada de volta para " + clientPacket.getAddress() + ":" + clientPacket.getPort());
            }
        } catch (SocketException e) {
            System.err.println("Erro ao criar o socket UDP: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("Erro ao receber/enviar pacotes: " + e.getMessage());
        }
    }

    private static String forwardPacket(String data) throws IOException {
        for (int i = 0; i < INSTANCES_IP.length; i++) {
            String ipAddress = INSTANCES_IP[currentInstanceIndex];
            int port = INSTANCES_PORT[currentInstanceIndex];

            byte[] sendData = data.getBytes();
            InetAddress address = InetAddress.getByName(ipAddress);
            DatagramPacket packet = new DatagramPacket(sendData, sendData.length, address, port);

            try (DatagramSocket socket = new DatagramSocket()) {
                socket.setSoTimeout(TIMEOUT);

                socket.send(packet);
                System.out.println("Pacote redirecionado para " + ipAddress + ":" + port);

                try {
                    byte[] receiveData = new byte[1024];
                    DatagramPacket responsePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(responsePacket);
                    
                    currentInstanceIndex = (currentInstanceIndex + 1) % INSTANCES_IP.length;
                    return new String(responsePacket.getData(), 0, responsePacket.getLength());
                } catch (SocketTimeoutException e) {
                    currentInstanceIndex = (currentInstanceIndex + 1) % INSTANCES_IP.length;
                    System.out.println("Timeout ao esperar pela resposta da instância " + ipAddress + ":" + port);
                }
            }
        }
        return "Erro: Nenhuma instância respondeu";
    }

    private static boolean isValidRequest(String data) {
        String[] parts = data.split(":", 2);
        if (parts.length != 2 || !parts[0].trim().equals("ORDER")) {
            return false;
        }
        String[] orderDetails = parts[1].trim().split(";");

        return orderDetails.length == 4 &&
                ("BUY".equals(orderDetails[0]) || "SELL".equals(orderDetails[0])) &&
                isNumeric(orderDetails[2]) && isNumeric(orderDetails[3]);
    }

    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
