package com.server;

import com.message.*;
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

        this.heartbeatManager = new HeartbeatManager(nodeAddresses, strategy);

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

        heartbeatManager.startHeartbeatsToGateway(new HeartbeatMessage(
                serverState.getCurrentGeneration(),
                serverId,
                8080));
    }

    @Override
    public void onNodeFailure(int failedId) {
        if (serverState.getLeaderId() == failedId) {
            electionManager.startElection();
        }
    }

    @Override
    public void onLeaderElected() {
        HeartbeatMessage heartbeat = new HeartbeatMessage(serverState.getCurrentGeneration(), serverId, serverState.getLeaderId());
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
    public OrderResponse handleOrder(OrderRequest orderRequest, InetSocketAddress sender) {

        System.out.println("Ordem recebida: " + orderRequest.toString());
        
        if(serverState.getServerRole() == ServerRole.FOLLOWER) {
            return strategy.forwardOrder(orderRequest,sender, serverState.getLeaderId());
        }
        else{
            Order order = new Order(orderRequest.getSymbol(),
                    OrderType.valueOf(orderRequest.getOperation()),
                    orderRequest.getQuantity(),
                    orderRequest.getPrice());

            matchingEngine.processOrder(order);

            return new OrderResponse("SUCCESS");//retornar status do order(pendente, completa)
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
