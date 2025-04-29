package com.strategy;

import com.message.Message;
import com.message.OrderRequest;
import com.message.OrderResponse;
import com.server.MessageHandler;
import com.server.OrderHandler;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HttpCommunicationStrategy implements CommunicationStrategy {
    private final Map<Integer, InetSocketAddress> serverAddresses;
    private final ExecutorService executorService;
    private final InetSocketAddress gatewayAddress;
    private MessageHandler messageHandler;
    private OrderHandler orderHandler;
    private ServerSocket serverSocket;

    public HttpCommunicationStrategy(Map<Integer, InetSocketAddress> serverAddresses, InetSocketAddress gatewayAddress) {
        this.serverAddresses = serverAddresses;
        this.gatewayAddress = gatewayAddress;
        this.executorService = Executors.newCachedThreadPool();
    }

    @Override
    public void startListening(int port, MessageHandler messageHandler, OrderHandler orderHandler) throws IOException {
        this.messageHandler = messageHandler;
        this.orderHandler = orderHandler;
        serverSocket = new ServerSocket(port);
        executorService.submit(this::acceptConnections);
    }

    @Override
    public OrderResponse forwardOrder(OrderRequest orderRequest, InetSocketAddress clientAddress, int serverId) {
        InetSocketAddress address = serverAddresses.get(serverId);
        try (Socket socket = new Socket(address.getAddress(), address.getPort());
             OutputStream out = socket.getOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String content = orderRequest.toString();
            sendPostRequest(writer, address, "/forward/order", content);

            OrderResponse orderResponse = readResponse(reader);
            System.out.println(orderResponse);

           return orderResponse;
        } catch (IOException e) {
            System.err.println("Erro ao enviar mensagem HTTP para o servidor " + serverId + ": " + e.getMessage());
            return new OrderResponse("FAILED");
        }
    }

    private void acceptConnections() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> handleClientConnection(clientSocket));
            } catch (IOException e) {
                if (serverSocket.isClosed()) {
                    break;
                }
            }
        }
    }

    private void handleClientConnection(Socket clientSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

            InetSocketAddress address = (InetSocketAddress) clientSocket.getRemoteSocketAddress();

            String headers = readRequest(reader);
            String content = getContent(headers, reader);

            OrderResponse response = processHttpRequest(headers, content, address);

            if (response != null) {
                String httpResponse = createHttpResponse(response);
                writer.write(httpResponse);
                writer.flush();
            }
        } catch (IOException e) {
            System.err.println("Error handling client connection: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private OrderResponse processHttpRequest(String httpRequest, String content, InetSocketAddress address) throws IOException {
        if (httpRequest.startsWith("POST /message")) {
            Message message = Message.fromString(content);
            messageHandler.handleMessage(message, address);
        } else if (httpRequest.startsWith("POST /order")) {
            return orderHandler.handleOrder(OrderRequest.fromString(content), address);
        } else if (httpRequest.startsWith("POST /forward/order")) {
            OrderRequest order = OrderRequest.fromString(content);
            return orderHandler.handleOrder(order, address);
        }
        return null;
    }

    @Override
    public void sendMessage(Message message, int nodeId) {
        executorService.submit(() -> {
            InetSocketAddress address = serverAddresses.getOrDefault(nodeId, gatewayAddress);

            if (address == null) {
                System.err.println("Endereço para o servidor " + nodeId + " não encontrado.");
                return;
            }

            try (Socket socket = new Socket(address.getAddress(), address.getPort());
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

                String content = message.toString();
                sendPostRequest(writer, address, "/message", content);

            } catch (IOException e) {
                System.err.println("Erro ao enviar mensagem HTTP: " + e.getMessage());
            }
        });
    }

    public static void sendPostRequest(BufferedWriter writer, InetSocketAddress address, String path, String content) throws IOException {
        String httpRequest = "POST " + path + " HTTP/1.1\r\n" +
                "Host: " + address.getHostName() + "\r\n" +
                "Content-Type: application/octet-stream\r\n" +
                "Content-Length: " + content.length() + "\r\n" +
                "\r\n" +
                content;

        writer.write(httpRequest);
        writer.flush();
    }

    public static String readRequest(BufferedReader reader) throws IOException {
        String line;
        StringBuilder requestBuilder = new StringBuilder();

        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            requestBuilder.append(line).append("\r\n");
        }

        return requestBuilder.toString();
    }

    public static String getContent(String headers, BufferedReader reader) throws IOException {
        Map<String, String> headerMap = parseHeaders(headers);
        int contentLength = Integer.parseInt(headerMap.getOrDefault("Content-Length", "0"));
        char[] contentBuffer = new char[contentLength];
        reader.read(contentBuffer, 0, contentLength);
        return new String(contentBuffer);
    }

    public static OrderResponse readResponse(BufferedReader reader) throws IOException {
        String line;
        StringBuilder responseBuilder = new StringBuilder();

        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            responseBuilder.append(line).append("\r\n");
        }

        Map<String, String> headers = parseHeaders(responseBuilder.toString());
        int contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
        char[] contentBuffer = new char[contentLength];
        reader.read(contentBuffer, 0, contentLength);
        System.out.println(new String(contentBuffer));

        return OrderResponse.fromString(new String(contentBuffer));
    }

    public static Map<String, String> parseHeaders(String headersString) {
        Map<String, String> headers = new ConcurrentHashMap<>();
        String[] lines = headersString.split("\r\n");
        for (String line : lines) {
            String[] headerParts = line.split(": ", 2);
            if (headerParts.length == 2) {
                headers.put(headerParts[0], headerParts[1]);
            }
        }
        return headers;
    }

    public static String createHttpResponse(OrderResponse response) throws IOException {
        String body = response.toString();
        String statusLine;
        if (body.contains("COMPLETED") || body.contains("PENDING")) {
            statusLine = "HTTP/1.1 200 OK";
        } else {
            statusLine = "HTTP/1.1 400 FAILED";
        }

        return statusLine + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "\r\n" +
                body;
    }
}
