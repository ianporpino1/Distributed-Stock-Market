package com.server;

import com.message.HeartbeatMessage;
import com.message.Message;
import com.message.RequestVoteMessage;
import com.message.VoteResponseMessage;
import com.model.Order;
import com.model.OrderType;

import com.patterns.ElectionManager;
import com.patterns.HeartbeatManager;
import com.patterns.ServerRole;
import com.service.MatchingEngine;
import com.service.OrderBookService;
import com.strategy.CommunicationStrategy;
import com.strategy.HttpCommunicationStrategy;
import com.strategy.TcpCommunicationStrategy;
import com.strategy.UdpCommunicationStrategy;

import java.awt.image.ImageConsumer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;

public class Server implements MessageHandler, OrderHandler, FailureListener, LeaderElectedListener {
    private final MatchingEngine matchingEngine;
    private final int serverId;
    private final Set<Integer> nodeAddresses;
    private final CommunicationStrategy strategy;
    private final ElectionManager electionManager;
    private final HeartbeatManager heartbeatManager;

    private final ServerState serverState;
    
    
    private static final int PORT = 9001;

    public Server(MatchingEngine matchingEngine, int serverId,
                  Set<Integer> nodeAddresses,
                  CommunicationStrategy strategy) {
        this.matchingEngine = matchingEngine;
        this.serverId = serverId;
        this.nodeAddresses = nodeAddresses;
        this.strategy = strategy;
        this.serverState = new ServerState();
        
        this.heartbeatManager = new HeartbeatManager(serverId, nodeAddresses, strategy);
        
        this.electionManager = new ElectionManager(serverId, nodeAddresses, strategy, serverState);
        

        this.heartbeatManager.addFailureListener(this);
        this.electionManager.addLeaderElectedListener(this);
    }

    public void start() {
        new Thread(() -> {
            try {
                strategy.startListening(serverId + PORT, this, this);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();
        
        electionManager.startElectionTimeout();
    }

    @Override
    public void onNodeFailure(int failedId) {
        if(serverState.getLeaderId() == failedId){
            electionManager.startElection();
        }
    }
    @Override
    public void onLeaderElected() {
        HeartbeatMessage heartbeat = new HeartbeatMessage(serverState.getCurrentGeneration(), serverId, serverId);
        heartbeatManager.startSendingHeartbeats(heartbeat);
    }

    public void handleMessage(Message message, InetSocketAddress sender) {
        switch (message) {
            case RequestVoteMessage requestVoteMessage -> electionManager.handleRequestVote(requestVoteMessage);
            case VoteResponseMessage voteResponseMessage -> electionManager.handleVoteResponse(voteResponseMessage);
            case HeartbeatMessage heartbeatMessage -> handleHeartbeat(heartbeatMessage);
            case null, default -> System.out.println("Mensagem desconhecida recebida.");
        }
    }

    public void handleHeartbeat(HeartbeatMessage message) {
        if (message.getGeneration() >= serverState.getCurrentGeneration()) {
            if (serverState.getServerRole() != ServerRole.FOLLOWER) {
                serverState.setServerRole(ServerRole.FOLLOWER);
                System.out.println("Node " + serverId + " reconhece o líder " + message.getLeaderId() + " no termo " + serverState.getCurrentGeneration());
            }
            System.out.println("Recebeu heartbeat de " + message.getLeaderId());
            serverState.setLeaderId(message.getLeaderId());
            serverState.setCurrentGeneration(message.getGeneration());
            heartbeatManager.lastHeartbeatReceivedTimes.put(message.getSenderId(), System.currentTimeMillis());
            heartbeatManager.failedServers.remove(serverId);
        }
    }
    
    @Override
    public void handleOrder(String orderMessage, InetSocketAddress sender) {
        System.out.println("Ordem recebida: " + orderMessage);
        String[] parts = orderMessage.split(":", 2);
        if (parts.length == 2 && parts[0].equals("ORDER")) {
            Order order = parseOrder(parts[1]);
            if (order != null) {
                matchingEngine.processOrder(order);
                
//                //RESPOSTA AO GATEWAY
//                String response = "Order processed: " + order;
//                strategy.sendMessage(new Response(MessageType.RESPONSE, response), sender);
            }
        } else {
            System.out.println("Formato de ordem inválido: " + orderMessage);
        }
    }

    private Order parseOrder(String orderDetails) {
        try {
            StringTokenizer tokenizer = new StringTokenizer(orderDetails.trim(), ";");

            if (tokenizer.countTokens() != 4) {
                System.out.println("Formato de ordem inválido: " + orderDetails);
                return null;
            }

            String type = tokenizer.nextToken().trim();
            String symbol = tokenizer.nextToken().trim();
            int quantity = Integer.parseInt(tokenizer.nextToken().trim());
            double price = Double.parseDouble(tokenizer.nextToken().trim());

            return new Order(symbol, OrderType.valueOf(type), quantity, price);
        } catch (Exception e) {
            System.out.println("Erro ao processar ordem: " + orderDetails);
            return null;
        }
    }
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java Main <protocol> <serverId> [<nodeId> ...]");
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
                int port = nodeId + PORT;
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

        MatchingEngine matchingEngine = new MatchingEngine(new OrderBookService());
        
        Server server = new Server(matchingEngine, serverId, nodeAddresses, strategy);

        System.out.println("Servidor iniciado com protocolo: " + protocol.toUpperCase());
        System.out.println("ServerId: " + serverId);
        System.out.println("NodeAddresses: " + nodeAddresses);
        
        server.start();
    }
}
