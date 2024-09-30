package com.server;

import com.model.Order;
import com.model.OrderType;
import com.patterns.*;
import com.service.MatchingEngine;
import com.service.OrderBookService;
import com.strategy.CommunicationStrategy;
import com.strategy.HttpCommunicationStrategy;
import com.strategy.TcpCommunicationStrategy;
import com.strategy.UdpCommunicationStrategy;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class Server implements MessageHandler, OrderHandler, FailureListener, LeaderElectedListener {
    private final MatchingEngine matchingEngine;
    private final int serverId;
    private final Map<Integer, InetSocketAddress> nodeAddresses;
    private final CommunicationStrategy strategy;
    private final ElectionManager electionManager;
    private final HeartbeatManager heartbeatManager;

    private final ServerState serverState;


    private static final int PORT = 9001;

    public Server(MatchingEngine matchingEngine, int serverId,
                  Map<Integer, InetSocketAddress> nodeAddresses,
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
        new Thread(() -> strategy.startListening(serverId + PORT, this, this)).start();

        electionManager.startElectionTimeout();
    }

    @Override
    public void onNodeFailure(int failedId) {
        if (serverState.getLeaderId() == failedId) {
            electionManager.startElection();
        }
    }

    @Override
    public void onLeaderElected() {
        Message heartbeat = new Message(MessageType.HEARTBEAT, serverState.getCurrentGeneration(), serverId, serverId);
        heartbeatManager.startSendingHeartbeats(heartbeat);
    }

    public void handleMessage(Message message, InetSocketAddress sender) {
        switch (message.getType()) {
            case REQUEST_VOTE:
                System.out.println("Received request vote from " + message.getSenderId());
                electionManager.handleVoteRequest(message);
                break;
            case VOTE:
                System.out.println("Received vote from " + message.getSenderId());
                electionManager.handleVoteResponse(message);
                break;
            case HEARTBEAT:
                System.out.println("Received heartbeat from " + message.getSenderId());
                handleHeartbeat(message);
                break;
            default:
                System.out.println("Tipo de mensagem desconhecido: " + message.getType());
        }
    }


    public void handleHeartbeat(Message message) {
        if (message.getGeneration() >= serverState.getCurrentGeneration()) {
            if (serverState.getServerRole() != ServerRole.FOLLOWER) {
                serverState.setServerRole(ServerRole.FOLLOWER);
                System.out.println("Node " + serverId + " reconhece o líder " + message.getLeaderId() + " no termo " + serverState.getCurrentGeneration());
            }
            serverState.setLeaderId(message.getLeaderId());
            serverState.setCurrentGeneration(message.getGeneration());
            heartbeatManager.lastHeartbeatReceivedTimes.put(message.getSenderId(), System.currentTimeMillis());
            heartbeatManager.failedServers.remove(serverId);
        }
    }

    @Override
    public void handleOrder(OrderMessage orderMessage, InetSocketAddress sender) {
        System.out.println("Ordem recebida: " + orderMessage);
        Order order = new Order(orderMessage.getSymbol(),
                OrderType.valueOf(orderMessage.getOperation()),
                orderMessage.getQuantity(),
                orderMessage.getPrice());

        matchingEngine.processOrder(order);

        //RESPOSTA AO GATEWAY
        String response = "Order processed: " + order;
        strategy.sendRequest(new Request(response), sender);

    }

    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Uso: java Main <protocol> <serverId> [<nodeId> ...]");
            return;
        }

        String protocol = args[0].toLowerCase();
        int serverId = Integer.parseInt(args[1]);

        Map<Integer, InetSocketAddress> nodeAddresses = new HashMap<>();

        for (int i = 1; i < args.length; i++) {
            int nodeId = Integer.parseInt(args[i]);
            if (nodeId != serverId) {
                int port = nodeId + PORT;
                nodeAddresses.put(nodeId, new InetSocketAddress("localhost", port));
            }
        }

        CommunicationStrategy strategy = null;

        switch (protocol) {
            case "udp" -> strategy = new UdpCommunicationStrategy();
            case "tcp" -> strategy = new TcpCommunicationStrategy();
            case "http" -> strategy = new HttpCommunicationStrategy();
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
