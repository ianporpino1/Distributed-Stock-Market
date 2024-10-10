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

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream outStream = new ObjectOutputStream(byteStream);
            outStream.writeObject(orderRequest);
            outStream.flush();
            byte[] sendData = byteStream.toByteArray();

            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddresses.get(serverId));
            socket.send(sendPacket);

            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            socket.receive(receivePacket);

            ByteArrayInputStream byteInputStream = new ByteArrayInputStream(receivePacket.getData());
            ObjectInputStream inStream = new ObjectInputStream(byteInputStream);
            Object response = inStream.readObject();

            if (response instanceof OrderResponse) {
                return (OrderResponse) response;
            } else {
                System.err.println("Resposta inesperada do servidor: " + response);
                return new OrderResponse("ERROR: Resposta inesperada do servidor");
            }

        } catch (IOException | ClassNotFoundException e) {
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
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(packet.getData());
             ObjectInputStream in = new ObjectInputStream(byteStream)) {

            Message message = (Message) in.readObject();
            InetSocketAddress senderAddress = new InetSocketAddress(packet.getAddress(), packet.getPort());
            
            if(message instanceof OrderRequest orderRequest) {
                OrderResponse response = orderHandler.handleOrder(orderRequest, senderAddress);
                sendResponse(response,senderAddress);
            } else{
                messageHandler.handleMessage(message, senderAddress);
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("Pacote não é um objeto serializado, tentando ler como String...");
            handleString(packet);
        }
        
    }
    
    private void handleString(DatagramPacket packet) {
        try {
            String receivedString = new String(packet.getData(), 0, packet.getLength(), "UTF-8");
            InetSocketAddress senderAddress = new InetSocketAddress(packet.getAddress(), packet.getPort());
            
            OrderRequest orderRequest = new OrderRequest(receivedString);
            OrderResponse response = orderHandler.handleOrder(orderRequest, senderAddress);
            
            sendResponse(response, senderAddress);
        } catch (Exception e) {
            System.err.println("Erro ao processar pacote como String: " + e.getMessage());
        }
    }

    private void sendResponse(OrderResponse response, InetSocketAddress clientAddress) {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(byteStream)) {
            
            out.writeObject(response);
            out.flush();
            byte[] responseData = byteStream.toByteArray();
            
            System.out.println(response.getResponseMessage());
            
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
                if(nodeId == gatewayAddress.getPort()) {
                    address = gatewayAddress;
                }
                else{
                    address = serverAddresses.get(nodeId);
                }
                if (address == null) {
                    System.err.println("Endereço para o servidor " + nodeId + " não encontrado.");
                    return;
                }

                byte[] data = serializeMessage(message);
                DatagramPacket packet = new DatagramPacket(data, data.length, address.getAddress(), address.getPort());

                socket.send(packet);
            } catch (IOException e) {
                System.err.println("Erro ao enviar mensagem para " + nodeId + ": " + e.getMessage());
            }
        });
    }

    private byte[] serializeMessage(Message message) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(byteStream)) {

            out.writeObject(message);
            out.flush();
            return byteStream.toByteArray();
        }
    }
    
}