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

public class UdpCommunicationStrategy implements CommunicationStrategy {
    private DatagramSocket socket;


    @Override
    public void sendMessage(Message message, int nodeId) {
        
    }

    @Override
    public void startListening(int port, MessageHandler messageHandler, OrderHandler orderHandler) {
        try {
            socket = new DatagramSocket(port);
            new Thread(() -> {
                byte[] receiveBuffer = new byte[1024];
                while (!socket.isClosed()) {
                    try {
                        DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        socket.receive(packet);

                        String messageStr = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                        if (messageStr.startsWith("ORDER")) {
                            orderHandler.handleOrder(messageStr, (InetSocketAddress) packet.getSocketAddress());
                        } else {
                            ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
                            Message message = (Message) in.readObject();
                            messageHandler.handleMessage(message, (InetSocketAddress) packet.getSocketAddress());
                        }
                    } catch (Exception e) {
                        if (!socket.isClosed()) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }
    
}