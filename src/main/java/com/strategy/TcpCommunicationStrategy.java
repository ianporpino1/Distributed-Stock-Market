package com.strategy;

import com.patterns.Message;
import com.patterns.OrderMessage;
import com.patterns.Request;
import com.server.MessageHandler;
import com.server.OrderHandler;

import java.io.*;
import java.net.*;

import java.io.*;
import java.net.*;

public class TcpCommunicationStrategy implements CommunicationStrategy {
    private ServerSocket serverSocket;

    @Override
    public void sendRequest(Request request, InetSocketAddress recipient) {
        try (Socket socket = new Socket(recipient.getAddress(), recipient.getPort());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            out.writeObject(request);
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
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            InetSocketAddress remoteAddress = (InetSocketAddress) clientSocket.getRemoteSocketAddress();

            // Deserialize the Request object
            Request requestObject = (Request) in.readObject();

            // Process the Request based on its type
            if (requestObject.getType() == Request.RequestType.ORDER) {
                OrderMessage orderMessage = requestObject.getOrder();
                orderHandler.handleOrder(orderMessage, remoteAddress);
            } else if (requestObject.getType() == Request.RequestType.MESSAGE) {
                Message message = requestObject.getMessage();
                messageHandler.handleMessage(message, remoteAddress);
            } else {
                System.err.println("Tipo de requisição não suportado: " + requestObject.getType());
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
