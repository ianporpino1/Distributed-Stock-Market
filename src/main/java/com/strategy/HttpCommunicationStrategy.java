package com.strategy;

import com.message.Message;
import com.message.OrderRequest;
import com.message.OrderResponse;
import com.server.MessageHandler;
import com.server.OrderHandler;

import java.io.*;
import java.net.*;
import java.util.Base64;
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

            String encodedContent = serializeToBase64(orderRequest);

            sendPostRequest(writer, address, "/forward/order", encodedContent);

            String responseContent = readResponse(reader);
            System.out.println(responseContent);
            
            if(responseContent.equals("SUCCESS")) {
                return new OrderResponse(responseContent);
            }

        } catch (IOException e) {
            System.err.println("Erro ao enviar mensagem HTTP para o servidor " + serverId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return null;
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
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private OrderResponse processHttpRequest(String httpRequest, String content, InetSocketAddress address) throws IOException, ClassNotFoundException {
        if (httpRequest.startsWith("POST /message")) {
            Message message = deserializeFromBase64(content, Message.class);
            messageHandler.handleMessage(message, address);
        } else if (httpRequest.startsWith("POST /order")) {
            return orderHandler.handleOrder(new OrderRequest(content), address);
        } else if (httpRequest.startsWith("POST /forward/order")) {
            OrderRequest order = deserializeFromBase64(content, OrderRequest.class);
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

                String encodedMessage = serializeToBase64(message);
                sendPostRequest(writer, address, "/message", encodedMessage);

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

    public static String readResponse(BufferedReader reader) throws IOException {
        String line;
        StringBuilder responseBuilder = new StringBuilder();

        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            responseBuilder.append(line).append("\r\n");
        }

        Map<String, String> headers = parseHeaders(responseBuilder.toString());
        int contentLength = Integer.parseInt(headers.getOrDefault("Content-Length", "0"));
        char[] contentBuffer = new char[contentLength];
        int bytesRead = reader.read(contentBuffer, 0, contentLength);

        if (bytesRead == -1) {
            System.err.println("Erro: Nenhuma resposta do servidor ou resposta vazia.");
        }

        return new String(contentBuffer);
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
        String body;
        String statusLine;

        if ("SUCCESS".equals(response.getResponseMessage())) {
//            body = serializeToBase64(response);
            body = "SUCCESS";
            statusLine = "HTTP/1.1 200 OK";
        } else {
            body = "FAIL";
            statusLine = "HTTP/1.1 400 FAILED";
        }
        

        return statusLine + "\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: " + body.length() + "\r\n" +
                "\r\n" +
                body;
    }

    public static String serializeToBase64(Object object) throws IOException {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {

            objectOutputStream.writeObject(object);
            objectOutputStream.flush();
            byte[] serializedData = byteArrayOutputStream.toByteArray();
            return Base64.getEncoder().encodeToString(serializedData);
        }
    }

    public static <T> T deserializeFromBase64(String base64String, Class<T> clazz) throws IOException, ClassNotFoundException {
        byte[] serializedData = Base64.getDecoder().decode(base64String);
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serializedData);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {

            Object object = objectInputStream.readObject();
            return clazz.cast(object);
        }catch (EOFException e){
            return null;
        }
    }
}
