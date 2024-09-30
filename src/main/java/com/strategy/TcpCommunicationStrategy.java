package com.strategy;

import com.patterns.Message;
import com.patterns.OrderMessage;
import com.patterns.Request;
import com.server.MessageHandler;
import com.server.OrderHandler;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class TcpCommunicationStrategy implements CommunicationStrategy {
    private ServerSocket serverSocket;

    @Override
    public void sendRequest(Request request, InetSocketAddress recipient) {
        try (Socket socket = new Socket(recipient.getAddress(), recipient.getPort());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {
            out.writeObject(request);
            out.flush();
        } catch (IOException e) {
            System.err.println("Erro ao enviar requisição: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void startListening(int port, MessageHandler handler, OrderHandler orderHandler) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("TCP Server listening on port " + port);

            while (!serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    new Thread(() -> handleClientConnection(clientSocket, handler, orderHandler)).start();
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        System.err.println("Erro ao aceitar conexão: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao iniciar o servidor: " + e.getMessage());
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


                if (receivedString.startsWith("ORDER:")) {
                    orderHandler.handleOrder(parseOrderRequest(receivedString).getOrder(), remoteAddress);
                }
            } else {
                ObjectInputStream in = new ObjectInputStream(bis);
                Request request = (Request) in.readObject();
                InetSocketAddress remoteAddress = (InetSocketAddress) clientSocket.getRemoteSocketAddress();

                processRequest(request,messageHandler,orderHandler,remoteAddress);
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
                System.out.println("Resposta recebida: " + request.getResponseContent());
                break;
            default:
                System.err.println("Tipo de requisição desconhecido: " + request.getType());
        }
    }
}