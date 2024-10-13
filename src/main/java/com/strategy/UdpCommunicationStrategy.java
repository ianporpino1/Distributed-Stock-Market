package com.strategy;

import com.message.Message;
import com.message.OrderRequest;
import com.message.OrderResponse;
import com.server.MessageHandler;
import com.server.OrderHandler;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UdpCommunicationStrategy implements CommunicationStrategy {
    private final Map<Integer, InetSocketAddress> serverAddresses;
    private final InetSocketAddress gatewayAddress;
    private final ExecutorService executorService;
    private MessageHandler messageHandler;
    private OrderHandler orderHandler;
    private DatagramSocket socket;

    public UdpCommunicationStrategy(Map<Integer, InetSocketAddress> serverAddresses, InetSocketAddress gatewayAddress) {
        this.serverAddresses = serverAddresses;
        this.gatewayAddress = gatewayAddress;
        this.executorService = Executors.newCachedThreadPool();
    }

    @Override
    public void startListening(int port, MessageHandler messageHandler, OrderHandler orderHandler) throws IOException {
        this.messageHandler = messageHandler;
        this.orderHandler = orderHandler;
        socket = new DatagramSocket(port);
        System.out.println("UDP socket listening on port " + port);
        executorService.submit(this::receiveMessages);
    }

    @Override
    public OrderResponse forwardOrder(OrderRequest orderRequest, InetSocketAddress clientAddress, int serverId) {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket();

            String requestStr = orderRequest.toString();
            byte[] sendData = requestStr.getBytes(StandardCharsets.UTF_8);

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddresses.get(serverId));
            socket.send(sendPacket);

            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);

            String responseStr = new String(receivePacket.getData(), 0, receivePacket.getLength());
            return OrderResponse.fromString(responseStr);

        } catch (IOException e) {
            System.err.println("Erro ao encaminhar pedido para o servidor " + serverId + ": " + e.getMessage());
            return new OrderResponse("FAILED");
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    private void receiveMessages() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                executorService.submit(() -> handlePacket(packet));
            } catch (IOException e) {
                System.err.println("Erro ao receber pacote: " + e.getMessage());
            }
        }
    }

    private void handlePacket(DatagramPacket packet) {
        try {
            String receivedString = new String(packet.getData(), 0, packet.getLength());
            InetSocketAddress senderAddress = new InetSocketAddress(packet.getAddress(), packet.getPort());

            Message message = Message.fromString(receivedString);

            if (message instanceof OrderRequest orderRequest) {
                OrderResponse response = orderHandler.handleOrder(orderRequest, senderAddress);
                sendResponse(response, senderAddress);
            } else {
                messageHandler.handleMessage(message, senderAddress);
            }
        } catch (Exception e) {
            System.err.println("Erro ao processar pacote: " + e.getMessage());
        }
    }

    private void sendResponse(OrderResponse response, InetSocketAddress clientAddress) {
        try {
            System.out.println(response.getResponseMessage());
            String responseStr = response.toString();
            byte[] responseData = responseStr.getBytes();

            DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length, clientAddress);
            socket.send(responsePacket);
        } catch (IOException e) {
            System.err.println("Erro ao enviar resposta UDP: " + e.getMessage());
        }
    }

    @Override
    public void sendMessage(Message message, int nodeId) {
        executorService.submit(() -> {
            try {
                InetSocketAddress address;
                if (nodeId == gatewayAddress.getPort()) {
                    address = gatewayAddress;
                } else {
                    address = serverAddresses.get(nodeId);
                }
                if (address == null) {
                    System.err.println("Endereço para o servidor " + nodeId + " não encontrado.");
                    return;
                }

                String messageStr = message.toString();
                byte[] data = messageStr.getBytes();
                DatagramPacket packet = new DatagramPacket(data, data.length, address.getAddress(), address.getPort());

                socket.send(packet);
            } catch (IOException e) {
                System.err.println("Erro ao enviar mensagem para " + nodeId + ": " + e.getMessage());
            }
        });
    }
}
