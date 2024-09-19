package com.udp;

import com.model.Order;
import com.model.OrderType;
import com.service.MatchingEngine;
import com.service.OrderBookService;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;

public class UdpMatchingEngineServer {
    private static final int[] PORTS = {9001, 9002, 9003, 9004};
    private static final int MAX_PORTS = PORTS.length;
    
    private final MatchingEngine matchingEngine;
    private DatagramSocket socket;
    
    public UdpMatchingEngineServer(MatchingEngine matchingEngine) {
        this.matchingEngine = matchingEngine;
    }

    public void start() {
        for (int i = 0; i < MAX_PORTS; i++) {
            int port = PORTS[i];
            try {
                socket = new DatagramSocket(port);
                System.out.println("UDP Matching Engine Server started on port " + port);
                runServer();
                break;
            } catch (SocketException e) {
                System.err.println("Port " + port + " is already in use. Trying next port...");
            }
        }

        if (socket == null) {
            System.err.println("No available ports found. Server could not start.");
        }
    }

    private void runServer() {
        byte[] receiveBuffer = new byte[1024];
        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                Order order = parseOrder(message);

                if (order == null) {
                    System.out.println("Server error: Null Order");
                    String errorResponse = "Invalid Order Format";
                    byte[] errorResponseData = errorResponse.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket errorPacket = new DatagramPacket(errorResponseData, errorResponseData.length, packet.getAddress(), packet.getPort());
                    socket.send(errorPacket);
                    continue;
                }

                matchingEngine.processOrder(order);

                System.out.println("Order processed: " + order);
                String response = "Order processed: " + order;
                byte[] responseData = response.getBytes(StandardCharsets.UTF_8);
                DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, packet.getAddress(), packet.getPort());
                socket.send(responsePacket);
            } catch (IOException e) {
                System.err.println("Error receiving or sending packet: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private Order parseOrder(String message) {
        try {
            StringTokenizer tokenizer = new StringTokenizer(message,";");
            String type = null;
            String symbol = null;
            int quantity = 0;
            Double price = null;
        
            while (tokenizer.hasMoreTokens()) {
                type = tokenizer.nextToken();
                symbol = tokenizer.nextToken();
                quantity = Integer.parseInt(tokenizer.nextToken());
                price = Double.parseDouble(tokenizer.nextToken());
            }
            return new Order(symbol, OrderType.valueOf(type), quantity, price);
        } catch (Exception e) {
            System.out.println("Erro ao processar ordem: " + message);
            return null;
        }
    }


    public static void main(String[] args) {
        MatchingEngine matchingEngine = new MatchingEngine(new OrderBookService());
        UdpMatchingEngineServer server = new UdpMatchingEngineServer(matchingEngine);
        server.start();
    }
}
