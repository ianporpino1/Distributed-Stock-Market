package com.strategy;

import com.patterns.Message;
import com.patterns.OrderMessage;
import com.patterns.Request;
import com.server.MessageHandler;
import com.server.OrderHandler;

import java.net.InetSocketAddress;

import java.io.*;
import java.net.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import java.io.*;
import java.net.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class HttpCommunicationStrategy implements CommunicationStrategy {
    private ServerSocket serverSocket;

    @Override
    public void sendRequest(Request request, InetSocketAddress recipient) {
        try (Socket socket = new Socket(recipient.getAddress(), recipient.getPort());
             OutputStream out = socket.getOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(request);
            oos.flush();
            byte[] serializedMessage = baos.toByteArray();

            String encodedContent = Base64.getEncoder().encodeToString(serializedMessage);

            String httpRequest = "POST / HTTP/1.1\r\n" +
                    "Host: " + recipient.getHostName() + "\r\n" +
                    "Content-Type: application/octet-stream\r\n" +
                    "Content-Length: " + encodedContent.length() + "\r\n" +
                    "\r\n" +
                    encodedContent;

            writer.write(httpRequest);
            writer.flush();
        } catch (IOException e) {
            System.err.println("Erro ao enviar mensagem HTTP: " + e.getMessage());
        }
    }

    @Override
    public void startListening(int port, MessageHandler messageHandler, OrderHandler orderHandler) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("HTTP Server listening on port " + port);

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
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

            String line;
            StringBuilder requestBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                requestBuilder.append(line).append("\r\n");
            }

            String request = requestBuilder.toString();
            Map<String, String> headers = parseHeaders(request);

            int contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
            char[] contentBuffer = new char[contentLength];
            reader.read(contentBuffer, 0, contentLength);
            String encodedContent = new String(contentBuffer);

            InetSocketAddress remoteAddress = (InetSocketAddress) clientSocket.getRemoteSocketAddress();
            byte[] serializedMessage = Base64.getDecoder().decode(encodedContent);

            // Deserialize Request object from the encoded content
            ByteArrayInputStream bais = new ByteArrayInputStream(serializedMessage);
            ObjectInputStream ois = new ObjectInputStream(bais);
            Request requestObject = (Request) ois.readObject();

            // Process the Request based on its type
            if (requestObject.getType() == Request.RequestType.ORDER) {
                OrderMessage orderMessage = requestObject.getOrder();
                orderHandler.handleOrder(orderMessage, remoteAddress);
            } else if (requestObject.getType() == Request.RequestType.MESSAGE) {
                Message message = requestObject.getMessage();
                messageHandler.handleMessage(message, remoteAddress);
            }

            // Send HTTP response
            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Length: 2\r\n" +
                    "\r\n" +
                    "OK";
            writer.write(response);
            writer.flush();

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erro ao processar a conexão HTTP: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Erro ao fechar o socket do cliente: " + e.getMessage());
            }
        }
    }

    private Map<String, String> parseHeaders(String request) {
        Map<String, String> headers = new HashMap<>();
        String[] lines = request.split("\r\n");
        for (int i = 1; i < lines.length; i++) {
            String[] parts = lines[i].split(": ", 2);
            if (parts.length == 2) {
                headers.put(parts[0], parts[1]);
            }
        }
        return headers;
    }
}

