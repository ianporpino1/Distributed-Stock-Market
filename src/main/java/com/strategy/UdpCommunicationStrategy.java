package com.strategy;

import com.patterns.OrderMessage;
import com.patterns.Request;
import com.server.MessageHandler;
import com.server.OrderHandler;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Arrays;


public class UdpCommunicationStrategy implements CommunicationStrategy {
    private DatagramSocket socket;

    @Override
    public void sendRequest(Request request, InetSocketAddress recipient) {
        try {
            byte[] buffer = serializeRequest(request);
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, recipient);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startListening(int port, MessageHandler handler, OrderHandler orderHandler) {
        try {
            socket = new DatagramSocket(port);
            System.out.println("Listening on port: " + port);

            new Thread(() -> {
                byte[] buffer = new byte[1024];
                while (!socket.isClosed()) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    try {
                        socket.receive(packet);
                        Request request = deserializeRequest(packet.getData(), packet.getLength());
                        processRequest(request, handler, orderHandler, (InetSocketAddress) packet.getSocketAddress());
                    } catch (IOException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] serializeRequest(Request request) throws IOException {
        try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(byteOut)) {
            out.writeObject(request);
            return byteOut.toByteArray();
        }
    }

    private Request deserializeRequest(byte[] data, int length) throws IOException, ClassNotFoundException {
        String requestString = new String(data, 0, length).trim();

        if (requestString.startsWith("ORDER:")) {
            return parseOrderRequest(requestString);
        }

        try (ByteArrayInputStream byteIn = new ByteArrayInputStream(data, 0, length);
             ObjectInputStream in = new ObjectInputStream(byteIn)) {
            return (Request) in.readObject();
        }
    }

    private Request parseOrderRequest(String requestString) {
        String[] parts = requestString.split(":", 2);
        if (parts.length != 2) {
            System.out.println("Formato de ordem inválido: " + requestString);
            return null;
        }

        String[] orderDetails = parts[1].split(";");
        if (orderDetails.length != 4) {
            System.out.println("Formato de ordem inválido: " + Arrays.toString(orderDetails));
            return null;
        }

        String type = orderDetails[0].trim();
        String symbol = orderDetails[1].trim();
        int quantity;
        double price;

        try {
            quantity = Integer.parseInt(orderDetails[2].trim());
            price = Double.parseDouble(orderDetails[3].trim());
        } catch (NumberFormatException e) {
            System.out.println("Erro ao analisar quantidade ou preço: " + e.getMessage());
            return null;
        }

        return new Request(new OrderMessage(type, symbol, quantity, price));
    }
    

    private void processRequest(Request request, MessageHandler handler, OrderHandler orderHandler, InetSocketAddress sender) {
        switch (request.getType()) {
            case MESSAGE:
                handler.handleMessage(request.getMessage(), sender);
                break;
            case ORDER:
                orderHandler.handleOrder(request.getOrder(), sender);
                break;
            case RESPONSE:
                // Implementar o que fazer com uma resposta, se necessário
                break;
        }
    }
}
