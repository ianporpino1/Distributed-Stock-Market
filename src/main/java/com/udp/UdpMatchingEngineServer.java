package com.udp;

import com.model.Order;
import com.model.OrderType;
import com.patterns.Message;
import com.patterns.MessageType;
import com.patterns.ServerRole;
import com.service.MatchingEngine;
import com.service.OrderBookService;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class UdpMatchingEngineServer {
    private final MatchingEngine matchingEngine;
    private int serverId;
    private final AtomicInteger currentGeneration = new AtomicInteger(0);
    private volatile ServerRole serverRole;
    private final AtomicInteger votedFor = new AtomicInteger(-1);
    private final AtomicInteger votes = new AtomicInteger(0);
    private int leaderId;
    private Set<Integer> clusterNodes;
    private Map<Integer, InetSocketAddress> nodeAddresses;
    private final ReentrantLock roleLock = new ReentrantLock();
    private volatile boolean electionInProgress = true;
    private ScheduledExecutorService heartbeatChecker = Executors.newScheduledThreadPool(1);
    private Map<Integer, Long> lastHeartbeatReceivedTimes = new ConcurrentHashMap<>();
    private long timeoutThreshold = 4000;
    private int electionTimeout = 5000;
    private int heartbeatInterval = 2000;
    private static final int PORT = 9001;
    private DatagramSocket socket;

    public UdpMatchingEngineServer(MatchingEngine matchingEngine, int serverId, Map<Integer, InetSocketAddress> nodeAddresses) throws SocketException {
        this.matchingEngine = matchingEngine;
        this.serverId = serverId;
        this.nodeAddresses = nodeAddresses;
        this.clusterNodes = nodeAddresses.keySet();
        this.serverRole = ServerRole.FOLLOWER;
        this.socket = new DatagramSocket(serverId + PORT);
        
        heartbeatChecker.scheduleWithFixedDelay(this::checkHeartbeatTimeouts, heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
        
        System.out.println("Server started at port " + (serverId + PORT) + " with id " + serverId);
    }

    public void start() {
        new Thread(this::listenForMessages).start();
        startElectionTimeout();
    }
    
    public void setServerRole(ServerRole newRole) {
        roleLock.lock();
        try {
            serverRole = newRole;

            if (newRole == ServerRole.FOLLOWER) {
                if (heartbeatChecker.isShutdown()) {
                    heartbeatChecker = Executors.newScheduledThreadPool(1);
                    heartbeatChecker.scheduleWithFixedDelay(this::checkHeartbeatTimeouts, heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
                }
            } else {
                heartbeatChecker.shutdownNow();
            }
        } finally {
            roleLock.unlock();
        }
    }
    
    private void checkHeartbeatTimeouts() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Integer, Long> entry : lastHeartbeatReceivedTimes.entrySet()) {
            long timeSinceLastHeartbeat = now - entry.getValue();
            System.out.println("Tempo desde o último heartbeat do servidor " + entry.getKey() + ": " + timeSinceLastHeartbeat);
            if (timeSinceLastHeartbeat >= timeoutThreshold) {
                System.out.println("Servidor " + entry.getKey() + " considerado falho.");
                
                if(leaderId == entry.getKey())  startElection();
            }
        }
    }

    private void listenForMessages() {
        byte[] receiveBuffer = new byte[1024];
        System.out.println("Escutando mensagens...");
        while (true) {
            try {
                DatagramPacket packet = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                socket.receive(packet);

                String messageStr = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
                if(messageStr.startsWith("ORDER")){
                    handleOrder(messageStr);
                    System.out.println("Mensagem recebida: " + messageStr);
                }
                else{
                    ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
                    Message message = (Message) in.readObject();
                    System.out.println("Mensagem recebida: " + message.toString());
                    handleMessage(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleOrder(String messageStr) {
        String[] parts = messageStr.split(":", 2);
        Order order = parseOrder(parts[1]);
        matchingEngine.processOrder(order);

    }
    private Order parseOrder(String message) {
        try {
            String orderDetails = message.trim();
            StringTokenizer tokenizer = new StringTokenizer(orderDetails, ";");

            if (tokenizer.countTokens() != 4) {
                System.out.println("Formato de ordem inválido: " + message);
                return null;
            }

            String type = tokenizer.nextToken().trim();
            String symbol = tokenizer.nextToken().trim();
            int quantity = Integer.parseInt(tokenizer.nextToken().trim());
            double price = Double.parseDouble(tokenizer.nextToken().trim());

            return new Order(symbol, OrderType.valueOf(type), quantity, price);
        } catch (Exception e) {
            System.out.println("Erro ao processar ordem: " + message);
            return null;
        }
    }


    private void handleMessage(Message message) {
        switch (message.getType()) {
            case REQUEST_VOTE:
                handleRequestVote(message);
                break;
            case VOTE:
                handleVote(message);
                break;
            case HEARTBEAT:
                handleHeartbeat(message);
                break; 
            default:
                System.out.println("Tipo de mensagem desconhecido: " + message.getType());
        }
    }

    private void handleRequestVote(Message message) {
        if (message.getGeneration() >= getCurrentGeneration()) {
            currentGeneration.set(message.getGeneration());
            if (votedFor.get() == -1 || votedFor.get() == message.getSenderId()) { //TALVEZ ERRO. GETCANDIDATEID()
                votedFor.set(message.getSenderId());
                Message vote = new Message(MessageType.VOTE, getCurrentGeneration(), serverId, -1);
                sendMessage(vote, nodeAddresses.get(message.getSenderId()));
            }
        }
    }

    private void handleHeartbeat(Message message) {
        if (message.getGeneration() >= getCurrentGeneration()) {
            roleLock.lock();
            try {
                if(serverRole != ServerRole.FOLLOWER){
                    setServerRole(ServerRole.FOLLOWER);
                }
                currentGeneration.set(message.getGeneration());
                lastHeartbeatReceivedTimes.put(message.getLeaderId(), System.currentTimeMillis());
                electionInProgress = false;
                leaderId = message.getLeaderId();
                System.out.println("Node " + serverId + " reconhece o líder " + message.getLeaderId() + " no termo " + message.getGeneration());
            } finally {
                roleLock.unlock();
            }
        }
    }

    private void startElectionTimeout() {
        try {
            Thread.sleep(electionTimeout + new Random().nextInt(2000));
            if (serverRole == ServerRole.FOLLOWER && electionInProgress) {
                startElection();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void startElection() {
        System.out.println("Iniciando Election...");
        incrementGeneration();
        setServerRole(ServerRole.CANDIDATE);
        votedFor.set(serverId); 
        votes.set(1);

        for (int otherNodeId : nodeAddresses.keySet()) {
            if (otherNodeId != serverId) {
                Message requestVote = new Message(MessageType.REQUEST_VOTE, getCurrentGeneration(), serverId, -1);
                sendMessage(requestVote, nodeAddresses.get(otherNodeId));
            }
        }
        waitForVotes();
    }
    private void waitForVotes() {
        long waitUntil = System.currentTimeMillis() + electionTimeout;
        while (System.currentTimeMillis() < waitUntil && serverRole == ServerRole.CANDIDATE) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (serverRole == ServerRole.CANDIDATE && votes.get() <= (nodeAddresses.size() / 2)) {
            System.out.println("Nenhuma resposta de outros nós. Node " + serverId + " se tornando líder.");
            electionInProgress=false;
            becomeLeader();
        }
    }

    private void handleVote(Message message) {
        if (serverRole == ServerRole.CANDIDATE && message.getGeneration() == getCurrentGeneration()) {
            votes.incrementAndGet(); // incrementa votos
            System.out.println("Node " + serverId + " recebeu um voto. Total: " + votes);

            if (votes.get() > (nodeAddresses.size() / 2)) {
                System.out.println("Node " + serverId + " se tornou o líder no termo " + getCurrentGeneration());
                becomeLeader();
            }
        }
    }

    private void becomeLeader() {
        roleLock.lock();
        try {
            setServerRole(ServerRole.LEADER);
            votes.set(0);
            new Thread(this::sendHeartbeats).start();
        } finally {
            roleLock.unlock();
        }
    }

    private void sendMessage(Message message, InetSocketAddress recipient) {
        try {
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteOut);
            out.writeObject(message);
            byte[] data = byteOut.toByteArray();

            DatagramPacket packet = new DatagramPacket(data, data.length, recipient.getAddress(), recipient.getPort());
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendHeartbeats() {
        while (serverRole == ServerRole.LEADER) {
            for (int otherNodeId : nodeAddresses.keySet()) {
                if (otherNodeId != serverId) {
                    Message heartbeat = new Message(MessageType.HEARTBEAT, getCurrentGeneration(), serverId, serverId);
                    sendMessage(heartbeat, nodeAddresses.get(otherNodeId));
                }
            }
            try {
                Thread.sleep(heartbeatInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public int getCurrentGeneration() {
        return currentGeneration.get();
    }

    public void incrementGeneration() {
        currentGeneration.incrementAndGet();
    }
    

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Uso: java Main <nodeId> [<nodeId> ...]");
            return;
        }

        Map<Integer, InetSocketAddress> nodeAddresses = new HashMap<>();

        for (int i = 0; i < args.length; i++) {
            int nodeId = Integer.parseInt(args[i]);
            int port = nodeId + PORT;
            nodeAddresses.put(nodeId, new InetSocketAddress("localhost", port));
        }

        MatchingEngine matchingEngine = new MatchingEngine(new OrderBookService());
        UdpMatchingEngineServer server = new UdpMatchingEngineServer(matchingEngine, Integer.parseInt(args[0]), nodeAddresses);
        server.start();
    }
}
