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

public class Server implements MessageHandler, OrderHandler {
    private final MatchingEngine matchingEngine;
    private final int serverId;
    private final Map<Integer, InetSocketAddress> nodeAddresses;
    private final CommunicationStrategy strategy;
    private final ElectionManager electionManager;
    private final HeartbeatManager heartbeatManager;

    private final ServerState serverState;
    
    
    private static int PORT = 9001;

    public Server(MatchingEngine matchingEngine, int serverId,
                  Map<Integer, InetSocketAddress> nodeAddresses,
                  CommunicationStrategy strategy) {
        this.matchingEngine = matchingEngine;
        this.serverId = serverId;
        this.nodeAddresses = nodeAddresses;
        this.strategy = strategy;
        this.serverState = new ServerState();
        
        this.heartbeatManager = new HeartbeatManager(serverId, nodeAddresses, strategy, serverState);
        
        this.electionManager = new ElectionManager(serverId, nodeAddresses, strategy, heartbeatManager, serverState);
        

        this.heartbeatManager.addLeaderFailureListener(electionManager);
    }

    public void start() {
        new Thread(() -> strategy.startListening(serverId + PORT, this, this)).start();
        
        electionManager.startElectionTimeout();

    }

    public void handleMessage(Message message, InetSocketAddress sender) {
        switch (message.getType()) {
            case REQUEST_VOTE:
                System.out.println("Received request vote from " + sender.getPort());
                electionManager.handleRequestVote(message);
                break;
            case VOTE:
                System.out.println("Received vote from " + sender.getPort());
                electionManager.handleVote(message);
                break;
            case HEARTBEAT:
                System.out.println("Received heartbeat from " + sender.getPort());
                heartbeatManager.handleHeartbeat(message);
                break;
            default:
                System.out.println("Tipo de mensagem desconhecido: " + message.getType());
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
                
                //RESPOSTA AO GATEWAY
                String response = "Order processed: " + order;
                strategy.sendMessage(new Response(MessageType.RESPONSE,response), sender);
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
