package com.strategy;

import com.patterns.Request;
import com.server.MessageHandler;
import com.server.OrderHandler;

import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;


import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketException;


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
                        
                        Request request = deserializeRequest(packet.getData());

                        processRequest(request, handler, orderHandler, (InetSocketAddress) packet.getSocketAddress());
                    } catch (IOException | ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }


                }
            }).start();

        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

        private byte[] serializeRequest (Request request) throws IOException {
            try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                 ObjectOutputStream out = new ObjectOutputStream(byteOut)) {
                out.writeObject(request);
                return byteOut.toByteArray();
            }
        }

        private Request deserializeRequest ( byte[] data) throws IOException, ClassNotFoundException {
            try (ByteArrayInputStream byteIn = new ByteArrayInputStream(data);
                 ObjectInputStream in = new ObjectInputStream(byteIn)) {
                return (Request) in.readObject();
            }
        }

        private void processRequest (Request request, MessageHandler handler, OrderHandler
        orderHandler, InetSocketAddress sender){
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
