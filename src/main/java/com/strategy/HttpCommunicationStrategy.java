package com.strategy;

import com.message.Message;
import com.server.ClientConnection;
import com.server.MessageHandler;
import com.server.OrderHandler;
import com.strategy.CommunicationStrategy;

import java.io.*;
import java.net.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpCommunicationStrategy implements CommunicationStrategy {
    private final Map<Integer, InetSocketAddress> serverAddresses;
    private final ExecutorService executorService;
    private MessageHandler messageHandler;
//    private final Map<Integer, ClientConnection> connections;
    private ServerSocket serverSocket;

    public HttpCommunicationStrategy(Map<Integer, InetSocketAddress> serverAddresses) {
        this.serverAddresses = serverAddresses;
        this.executorService = Executors.newCachedThreadPool();
//        this.connections = new ConcurrentHashMap<>();
    }

    @Override
    public void startListening(int port, MessageHandler messageHandler, OrderHandler orderHandler) throws IOException {
        this.messageHandler = messageHandler;
        serverSocket = new ServerSocket(port);
        executorService.submit(this::acceptConnections);
    }

    private void acceptConnections() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted HTTP connection from " + clientSocket.getInetAddress().getHostAddress());
                executorService.submit(() -> handleClientConnection(clientSocket));
            } catch (IOException e) {
                System.err.println("Error accepting connection: " + e.getMessage());
            }
        }
    }

    private void handleClientConnection(Socket clientSocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

            String line;
            StringBuilder requestBuilder = new StringBuilder();
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                requestBuilder.append(line).append("\r\n");
            }

            Map<String, String> headers = parseHeaders(requestBuilder.toString());
            int contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));

            char[] contentBuffer = new char[contentLength];
            reader.read(contentBuffer, 0, contentLength);
            String encodedContent = new String(contentBuffer);

            byte[] serializedMessage = Base64.getDecoder().decode(encodedContent);

            ByteArrayInputStream bais = new ByteArrayInputStream(serializedMessage);
            ObjectInputStream ois = new ObjectInputStream(bais);
            Message message = (Message) ois.readObject();

            messageHandler.handleMessage(message, (InetSocketAddress) clientSocket.getRemoteSocketAddress());

            String response = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: text/plain\r\n" +
                    "Content-Length: 2\r\n" +
                    "\r\n" +
                    "OK";
            writer.write(response);
            writer.flush();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error handling client connection: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    @Override
    public void sendMessage(Message message, int nodeId) {
        executorService.submit(() -> {
            try {
                InetSocketAddress serverAddress = serverAddresses.get(nodeId);
                if (serverAddress == null) {
                    throw new IOException("Server address not found for node " + nodeId);
                }

                try (Socket socket = new Socket(serverAddress.getAddress(), serverAddress.getPort());
                     OutputStream out = socket.getOutputStream();
                     BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out))) {

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ObjectOutputStream oos = new ObjectOutputStream(baos);
                    oos.writeObject(message);
                    oos.flush();
                    byte[] serializedMessage = baos.toByteArray();

                    String encodedContent = Base64.getEncoder().encodeToString(serializedMessage);

                    String httpRequest = "POST / HTTP/1.1\r\n" +
                            "Host: " + serverAddress.getHostName() + "\r\n" +
                            "Content-Type: application/octet-stream\r\n" +
                            "Content-Length: " + encodedContent.length() + "\r\n" +
                            "\r\n" +
                            encodedContent;

                    writer.write(httpRequest);
                    writer.flush();
                } catch (IOException e) {
                    System.err.println("Erro ao enviar mensagem HTTP: " + e.getMessage());
                }
            } catch (IOException e) {
                System.err.println("Error sending message to node " + nodeId + ": " + e.getMessage());
            }
        });
    }

//    private ClientConnection getOrCreateConnection(int targetId) throws IOException {
//        return connections.computeIfAbsent(targetId, id -> {
//            try {
//                InetSocketAddress address = serverAddresses.get(id);
//                Socket socket = new Socket(address.getHostName(), address.getPort());
//                System.out.println("Established HTTP connection to node " + id + " on port " + address.getPort());
//                return new ClientConnection(socket);
//            } catch (IOException e) {
//                throw new RuntimeException("Failed to connect to node " + id, e);
//            }
//        });
//    }

    private Map<String, String> parseHeaders(String request) {
        Map<String, String> headers = new ConcurrentHashMap<>();
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