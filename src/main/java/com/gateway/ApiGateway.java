package com.gateway;

import com.message.*;
import com.patterns.HeartbeatManager;
import com.server.FailureListener;
import com.server.MessageHandler;
import com.server.OrderHandler;
import com.strategy.CommunicationStrategy;
import com.strategy.HttpCommunicationStrategy;
import com.strategy.TcpCommunicationStrategy;
import com.strategy.UdpCommunicationStrategy;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ApiGateway implements FailureListener, MessageHandler, OrderHandler {
    private static final int GATEWAY_PORT = 8080;

    private CommunicationStrategy strategy;
    private Set<Integer> nodeAddresses;
    private final Map<Integer, Boolean> activeNodes;
    private final HeartbeatManager heartbeatManager;

    private int roundRobinIndex = 0;

    public ApiGateway(CommunicationStrategy strategy, Set<Integer> nodeAddresses) {
        this.strategy = strategy;
        this.nodeAddresses = nodeAddresses;

        this.activeNodes = new HashMap<>(nodeAddresses.stream()
                .collect(Collectors.toMap(id -> id, id -> true)));

        this.heartbeatManager = new HeartbeatManager(nodeAddresses, strategy);

        this.heartbeatManager.addFailureListener(this);
    }

    private void start() {
        new Thread(() -> {
            try {
                strategy.startListening(GATEWAY_PORT, this, this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }


    @Override
    public void onNodeFailure(int failedId) {
        activeNodes.replace(failedId, false);
    }


    @Override
    public void handleMessage(Message message, InetSocketAddress sender) {
        if(message instanceof HeartbeatMessage heartbeatMessage) {
            handleHeartbeat(heartbeatMessage);
        }
        else {
            System.out.println("Mensagem desconhecida recebida.");
        }
    }

    @Override
    public OrderResponse handleOrder(OrderRequest orderRequest, InetSocketAddress sender) {
        if (activeNodes.isEmpty() || !hasActiveNodes()) {
            System.out.println("Nenhum servidor ativo disponível para processar a ordem.");
            return new OrderResponse("Nenhum servidor ativo.");
        }

        int serverId = getNextActiveServerId();

        strategy.sendMessage(orderRequest, serverId);
        
        return new OrderResponse("OK");
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

    private void handleHeartbeat(HeartbeatMessage message) {
        heartbeatManager.lastHeartbeatReceivedTimes.put(message.getSenderId(), System.currentTimeMillis());

    }
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Uso: java Main <protocol> [<nodeId> ...]");
            return;
        }
        String protocol = args[0].toLowerCase();
        int serverId = Integer.parseInt(args[1]);

        Set<Integer> nodeAddresses = new HashSet<>();

        for (int i = 1; i < args.length; i++) {
            int nodeId = Integer.parseInt(args[i]);
            if (nodeId != serverId) {
                nodeAddresses.add(nodeId);
            }
        }
        
        Map<Integer, InetSocketAddress> serverAddresses = new HashMap<>();

        for (int i = 1; i < args.length; i++) {
            int nodeId = Integer.parseInt(args[i]);
            if (nodeId != serverId) {
                int port = nodeId + 9001;
                serverAddresses.put(nodeId, new InetSocketAddress("localhost", port));
            }
        }

        CommunicationStrategy strategy = null;

        switch (protocol) {
            case "udp" -> strategy = new UdpCommunicationStrategy(serverAddresses);
            case "tcp" -> strategy = new TcpCommunicationStrategy(serverAddresses);
            case "http" -> strategy = new HttpCommunicationStrategy(serverAddresses);
            default -> System.out.println("Protocolo não suportado.");
        }

        ApiGateway gateway = new ApiGateway(strategy,nodeAddresses);

        System.out.println("Gateway iniciado com protocolo: " + protocol.toUpperCase());
        System.out.println("NodeAddresses: " + nodeAddresses);

        gateway.start();
    }
}
