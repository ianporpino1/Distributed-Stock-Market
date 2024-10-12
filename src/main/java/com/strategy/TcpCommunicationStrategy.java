package com.strategy;

import com.message.Message;
import com.message.OrderRequest;
import com.message.OrderResponse;
import com.message.VoteResponseMessage;
import com.server.ClientConnection;
import com.server.MessageHandler;
import com.server.OrderHandler;
import com.sun.source.tree.NewArrayTree;

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

    @Override
    public void startListening(int port, MessageHandler messageHandler, OrderHandler orderHandler) throws IOException {
        this.messageHandler = messageHandler;
        this.orderHandler = orderHandler;
        serverSocket = new ServerSocket(port);
        executorService.submit(this::acceptConnections);
    }

    @Override
    public OrderResponse forwardOrder(OrderRequest orderRequest, InetSocketAddress clientAddress, int serverId) {
        try {
            ClientConnection connection = getOrCreateConnection(serverId);

            String response;

            synchronized (connection.getOut()) {
                connection.getOut().write(orderRequest.toString());
                connection.getOut().newLine();
                connection.getOut().flush();

                response = connection.getIn().readLine();
            }
            System.out.println(response);

            return OrderResponse.fromString(response);

        } catch (IOException e) {
            System.err.println("Erro ao encaminhar pedido para o servidor " + serverId + ": " + e.getMessage());
            return new OrderResponse("FAILED");
        }
    }

    public void handleClientConnection(Socket clientSocket) {
        try {
            ClientConnection connection = new ClientConnection(clientSocket);

            int remotePort = ((InetSocketAddress) clientSocket.getRemoteSocketAddress()).getPort();
            connections.putIfAbsent(remotePort, connection);

            while (!Thread.currentThread().isInterrupted()) {
                String messageStr = connection.getIn().readLine();
                Message message = Message.fromString(messageStr);

                if (message instanceof OrderRequest orderRequest) {
                    OrderResponse response = orderHandler.handleOrder(orderRequest, (InetSocketAddress) clientSocket.getRemoteSocketAddress());

                    synchronized (connection.getOut()) {
                        connection.getOut().write(response.toString());
                        connection.getOut().newLine();
                        connection.getOut().flush();
                    }
                    if (isClientConnection(clientSocket)) {
                        closeConnection(clientSocket, remotePort);
                        break;
                    }
                } else {
                    messageHandler.handleMessage(message, (InetSocketAddress) clientSocket.getRemoteSocketAddress());
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao lidar com conexão do cliente: " + e.getMessage());
        }
    }

    private boolean isClientConnection(Socket socket) {
        return socket.getLocalPort() == 8080;
    }

    private void closeConnection(Socket socket, int port) throws IOException {
        socket.close();
        connections.remove(port);
    }

    @Override
    public void sendMessage(Message message, int nodeId) {
        executorService.submit(() -> {
            try {
                ClientConnection connection = getOrCreateConnection(nodeId);

                if (connection.getSocket() != null && !connection.getSocket().isClosed()) {
                    connection.getOut().write(message.toString());
                    connection.getOut().newLine();
                    connection.getOut().flush();
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
