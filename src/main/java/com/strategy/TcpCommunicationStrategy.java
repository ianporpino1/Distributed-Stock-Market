package com.strategy;

import com.patterns.Message;
import com.patterns.OrderMessage;
import com.server.MessageHandler;
import com.server.OrderHandler;

import java.io.*;
import java.net.*;

public class TcpCommunicationStrategy implements CommunicationStrategy {
    private ServerSocket serverSocket;

    @Override
    public void sendMessage(Message message, InetSocketAddress recipient) {
        try (Socket socket = new Socket(recipient.getAddress(), recipient.getPort());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            System.err.println("Erro ao enviar mensagem: " + e.getMessage());
        }
    }

    @Override
    public void startListening(int port, MessageHandler messageHandler, OrderHandler orderHandler) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("TCP Server listening on port " + port);

            new Thread(() -> {
                while (!serverSocket.isClosed()) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        new Thread(() -> handleClientConnection(clientSocket, messageHandler, orderHandler)).start();
                    } catch (IOException e) {
                        if (!serverSocket.isClosed()) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void handleClientConnection(Socket clientSocket, MessageHandler messageHandler, OrderHandler orderHandler) {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            BufferedInputStream bis = new BufferedInputStream(inputStream);
            bis.mark(4);

            byte[] header = new byte[4];
            bis.read(header, 0, 4);
            bis.reset();

            
            String headerString = new String(header);

            if (headerString.equals("ORDE")) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(bis));
                String receivedString = reader.readLine();
                InetSocketAddress remoteAddress = (InetSocketAddress) clientSocket.getRemoteSocketAddress();

                if (receivedString.startsWith("ORDER")) {
                    orderHandler.handleOrder(receivedString, remoteAddress);
                }
            } else {
                ObjectInputStream in = new ObjectInputStream(bis);
                Object receivedObject = in.readObject();
                InetSocketAddress remoteAddress = (InetSocketAddress) clientSocket.getRemoteSocketAddress();

                if (receivedObject instanceof Message message) {
                    messageHandler.handleMessage(message, remoteAddress);
                } else {
                    System.err.println("Tipo de mensagem não suportado: " + receivedObject.getClass().getName());
                }
            }
        } catch (EOFException e) {
            System.err.println("Conexão fechada pelo cliente.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro ao processar a conexão: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar o socket do cliente: " + e.getMessage());
            }
        }
    }

}
