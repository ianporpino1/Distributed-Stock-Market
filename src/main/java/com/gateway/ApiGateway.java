package com.gateway;

import com.patterns.*;
import com.server.FailureListener;
import com.server.MessageHandler;
import com.server.OrderHandler;
import com.strategy.CommunicationStrategy;
import com.strategy.HttpCommunicationStrategy;
import com.strategy.TcpCommunicationStrategy;
import com.strategy.UdpCommunicationStrategy;

import java.io.IOException;
import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class ApiGateway implements FailureListener, MessageHandler, OrderHandler {
    
    private static final int GATEWAY_PORT = 8080;
    
    private final CommunicationStrategy strategy;

    private final Map<Integer, InetSocketAddress> nodeAddresses;
    private final Map<Integer, Boolean> activeNodes;
    
    private final HeartbeatManager heartbeatManager;

    private int roundRobinIndex = 0;

    public ApiGateway(CommunicationStrategy strategy, Map<Integer, InetSocketAddress> nodeAddresses) {
        this.strategy = strategy;
        this.nodeAddresses = nodeAddresses;

        this.activeNodes = new HashMap<>(nodeAddresses.keySet().stream()
                .collect(Collectors.toMap(id -> id, id -> true)));
        
        this.heartbeatManager = new HeartbeatManager(nodeAddresses, strategy);
        
        this.heartbeatManager.addFailureListener(this);
    }
    
    private void start() {
        new Thread(() -> strategy.startListening(GATEWAY_PORT, this, this)).start();
    }


    @Override
    public void onNodeFailure(int failedId) {
        activeNodes.replace(failedId, false);
    }

    @Override
    public void handleMessage(Message message, InetSocketAddress sender) {
        if (Objects.requireNonNull(message.getType()) == MessageType.HEARTBEAT) {
            System.out.println("Received heartbeat from server " + message.getSenderId());
            handleHeartbeat(message);
        } else if(Objects.requireNonNull(message.getType()) == MessageType.RESPONSE){
            System.out.println("Received heartbeat from server " + message.getSenderId());
//            strategy.sendMessage();
        }
        
        else {
            System.out.println("Tipo de mensagem desconhecido: " + message.getType() + " " + sender);
        }
    }
    private void handleHeartbeat(Message message) {
        heartbeatManager.lastHeartbeatReceivedTimes.put(message.getSenderId(), System.currentTimeMillis());
        
    }

    @Override
    public void handleOrder(OrderMessage orderMessage, InetSocketAddress sender) {

        if (activeNodes.isEmpty() || !hasActiveNodes()) {
            System.out.println("Nenhum servidor ativo disponível para processar a ordem.");
            return;
        }

        int serverId = getNextActiveServerId();
        InetSocketAddress serverAddress = nodeAddresses.get(serverId);

        sendOrderToServer(orderMessage, serverAddress);
        
        //resposta ao cliente
    }
    private int getNextActiveServerId() {
        int originalIndex = roundRobinIndex;
        do {
            int currentId = roundRobinIndex;
            roundRobinIndex = (roundRobinIndex + 1) % nodeAddresses.size();
            if (activeNodes.getOrDefault(currentId, false)) {
                return currentId;
            }
        } while (roundRobinIndex != originalIndex);

        throw new RuntimeException("Nenhum servidor ativo encontrado.");
    }

    private boolean hasActiveNodes() {
        return activeNodes.values().stream().anyMatch(active -> active);
    }

    private void sendOrderToServer(OrderMessage orderMessage, InetSocketAddress serverAddress) {
        System.out.println("Enviando ordem para o servidor " + serverAddress + ": " + orderMessage);
        strategy.sendRequest(new Request(orderMessage), serverAddress);
    }


//    private static boolean isValidOrder(String data) {
//        String[] parts = data.split(":", 2);
//        if (parts.length != 2 || !parts[0].trim().equals("ORDER")) {
//            return false;
//        }
//        String[] orderDetails = parts[1].trim().split(";");
//
//        return orderDetails.length == 4 &&
//                ("BUY".equals(orderDetails[0]) || "SELL".equals(orderDetails[0])) &&
//                isNumeric(orderDetails[2]) && isNumeric(orderDetails[3]);
//    }
//
//    private static boolean isNumeric(String str) {
//        try {
//            Double.parseDouble(str);
//            return true;
//        } catch (NumberFormatException e) {
//            return false;
//        }
//    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java Main <protocol> [<nodeId> ...]");
            return;
        }
        String protocol = args[0].toLowerCase();

        Map<Integer, InetSocketAddress> nodeAddresses = new HashMap<>();

        for (int i = 1; i < args.length; i++) {
            int nodeId = Integer.parseInt(args[i]);
            int port = nodeId + 9001;
            nodeAddresses.put(nodeId, new InetSocketAddress("localhost", port));
        }

        CommunicationStrategy strategy = null;

        switch (protocol) {
            case "udp" -> strategy = new UdpCommunicationStrategy();
            case "tcp" -> strategy = new TcpCommunicationStrategy();
            case "http" -> strategy = new HttpCommunicationStrategy();
            default -> System.out.println("Protocolo não suportado.");
        }
        
        ApiGateway gateway = new ApiGateway(strategy,nodeAddresses);

        System.out.println("Gateway iniciado com protocolo: " + protocol.toUpperCase());
        System.out.println("NodeAddresses: " + nodeAddresses);
        
        gateway.start();
    }
    
}
