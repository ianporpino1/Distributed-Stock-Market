package com.strategy;


import com.message.Message;
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
    //private final InetSocketAddress gatewayAddress;
    private final ExecutorService executorService;
    private MessageHandler messageHandler;
    private DatagramSocket socket;

    public UdpCommunicationStrategy(Map<Integer, InetSocketAddress> serverAddresses) {
        this.serverAddresses = serverAddresses;
        this.executorService = Executors.newCachedThreadPool();
        
    }

    @Override
    public void startListening(int port, MessageHandler messageHandler, OrderHandler orderHandler) throws IOException {
        this.messageHandler = messageHandler;
        socket = new DatagramSocket(port);
        System.out.println("UDP socket listening on port " + port);
        executorService.submit(this::receiveMessages);
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
            

            System.out.println("Recebida mensagem de " + senderAddress + ": " + message.getClass().getName());

            messageHandler.handleMessage(message, senderAddress);

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro ao processar pacote UDP: " + e.getMessage());
        }
    }

    @Override
    public void sendMessage(Message message, int nodeId) {
        executorService.submit(() -> {
            try {
                InetSocketAddress address = serverAddresses.get(nodeId);
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