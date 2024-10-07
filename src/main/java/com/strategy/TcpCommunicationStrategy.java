package com.strategy;

import com.message.Message;
import com.message.OrderRequest;
import com.message.OrderResponse;
import com.server.ClientConnection;
import com.server.MessageHandler;
import com.server.OrderHandler;

import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TcpCommunicationStrategy implements CommunicationStrategy {
    private final Map<Integer, InetSocketAddress> serverAddresses;
    private final ExecutorService executorService;
    private MessageHandler messageHandler;
    private OrderHandler orderHandler;
    private ServerSocket serverSocket;
    private final Map<Integer, ClientConnection> connections;

    public TcpCommunicationStrategy(Map<Integer, InetSocketAddress> serverAddresses) {
        this.serverAddresses = serverAddresses;
        this.executorService = Executors.newCachedThreadPool();
        this.connections = new ConcurrentHashMap<>();
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
        return null;
    }

    private void acceptConnections() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket clientSocket = serverSocket.accept();
                executorService.submit(() -> handleClientConnection(clientSocket));
            } catch (IOException e) {
                System.err.println("Erro ao aceitar conexão: " + e.getMessage());
            }
        }
    }

    public void handleClientConnection(Socket clientSocket) {
        try {
            ClientConnection connection = new ClientConnection(clientSocket);
            connections.putIfAbsent(((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getPort(), connection);

            while (!Thread.currentThread().isInterrupted()) {
                Message message = (Message) connection.getIn().readObject();

                if(message instanceof OrderRequest orderRequest) {
                    orderHandler.handleOrder(orderRequest,(InetSocketAddress) clientSocket.getRemoteSocketAddress());
                }
                else{
                    messageHandler.handleMessage(message, (InetSocketAddress) clientSocket.getRemoteSocketAddress());
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("NAO EH MENSAGEM: " + e.getMessage());
        }

        //ELSE
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"))) {

            String inputLine = in.readLine();

            OrderRequest orderRequest = new OrderRequest(inputLine);
            System.out.println(orderRequest);
            orderHandler.handleOrder(orderRequest, (InetSocketAddress) clientSocket.getRemoteSocketAddress());


        } catch (IOException e) {
            System.err.println("Error handling client connection: " + e.getMessage());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    private String processMessage(String message) {
        // Aqui você pode adicionar a lógica para processar a mensagem recebida
        return "Processed: " + message; // Exemplo simples
    }

    @Override
    public void sendMessage(Message message, int nodeId) {
        executorService.submit(() -> {
            try {
                ClientConnection connection = getOrCreateConnection(nodeId);

                if (connection.getSocket() != null && !connection.getSocket().isClosed()) {
                    connection.getOut().writeObject(message);
                    connection.getOut().flush();
                    connection.getOut().reset();
                } else {
                    System.err.println("Socket para o servidor " + nodeId + " está fechado ou nulo.");
                    connections.remove(nodeId);
                }
            } catch (IOException e) {
                System.err.println("Erro ao obter conexão para o servidor " + nodeId + ": " + e.getMessage());
                connections.remove(nodeId);
            }
        });
    }

    private ClientConnection getOrCreateConnection(int targetId) throws IOException {
        return connections.computeIfAbsent(targetId, id -> {
            try {
                InetSocketAddress address;
                if(targetId == 8080){
                    address = new InetSocketAddress("localhost", 8080);
                }
                else{
                    address = serverAddresses.get(id);
                }
                Socket socket = new Socket(address.getHostName(), address.getPort());
                System.out.println("Conexão estabelecida com o servidor " + id + " em " + address.getPort());
                return new ClientConnection(socket);
            } catch (IOException e) {
                throw new RuntimeException("Não foi possível conectar ao servidor " + id, e);
            }
        });
    }
}