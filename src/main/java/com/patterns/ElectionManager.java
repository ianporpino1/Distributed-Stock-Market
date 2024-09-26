package com.patterns;

import com.server.FailureListener;
import com.server.ServerState;
import com.strategy.CommunicationStrategy;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ElectionManager {
    private final int serverId;
    private final ServerState serverState;

    private final Map<Integer, Integer> votedForAtGeneration = new ConcurrentHashMap<>();
    private final AtomicInteger votes = new AtomicInteger(0);
    private final Map<Integer, InetSocketAddress> nodeAddresses;
    
    private final int electionTimeout = 5000;
    private final CommunicationStrategy strategy;
    
    private final HeartbeatManager heartbeatManager;

    public ElectionManager(int serverId, Map<Integer, InetSocketAddress> nodeAddresses, CommunicationStrategy strategy,
                           HeartbeatManager heartbeatManager,
                           ServerState serverState) {
        this.serverId = serverId;
        this.serverState = serverState;
        this.nodeAddresses = nodeAddresses;
        this.strategy = strategy;
        this.heartbeatManager = heartbeatManager;
    }
    

    public void startElectionTimeout() {
        try {
            Thread.sleep(electionTimeout + new Random().nextInt(2000));
            if (serverState.getLeaderId() == -1) {
                startElection();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void startElection() {
        serverState.incrementGeneration();
        System.out.println("Iniciando Election... Generation: " + serverState.getCurrentGeneration());
        serverState.setServerRole(ServerRole.CANDIDATE);
        votedForAtGeneration.put(serverId, serverState.getCurrentGeneration());
        votes.set(1);

        for (int otherNodeId : nodeAddresses.keySet()) {
            if (otherNodeId != serverId) {
                Message requestVote = new Message(MessageType.REQUEST_VOTE, serverState.getCurrentGeneration(), serverId, -1);
                strategy.sendMessage(requestVote, nodeAddresses.get(otherNodeId));
            }
        }
        waitForVotes();
    }

    private void waitForVotes() {
        long waitUntil = System.currentTimeMillis() + electionTimeout;
        while (System.currentTimeMillis() < waitUntil && serverState.getServerRole() == ServerRole.CANDIDATE) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (serverState.getServerRole() == ServerRole.CANDIDATE && votes.get() <= (nodeAddresses.size() / 2)) {
            System.out.println("Nenhuma resposta de outros nós. Node " + serverId + " voltando a follower.");

            startElection();
        }
    }

    public void handleRequestVote(Message message) {
        if (message.getGeneration() >= serverState.getCurrentGeneration()) {
            serverState.setCurrentGeneration(message.getGeneration());
            if (votedForAtGeneration.getOrDefault(serverState.getCurrentGeneration(), -1) == -1) {
                votedForAtGeneration.put(serverState.getCurrentGeneration(), message.getSenderId());
                System.out.println(serverId + " votou para " + message.getSenderId() + " na geração " + serverState.getCurrentGeneration() + ".");

                Message vote = new Message(MessageType.VOTE, serverState.getCurrentGeneration(), serverId, -1);
                strategy.sendMessage(vote, nodeAddresses.get(message.getSenderId()));
            }
        }
    }


    public void handleVote(Message message) {
        if (serverState.getServerRole() == ServerRole.CANDIDATE && message.getGeneration() == serverState.getCurrentGeneration()) {
            votes.incrementAndGet();
            System.out.println("Node " + serverId + " recebeu um voto. Total: " + votes);

            if (votes.get() > (nodeAddresses.size() / 2)) {
                System.out.println("Node " + serverId + " se tornou o líder no termo " + serverState.getCurrentGeneration());
                becomeLeader();
            }
        }
    }

    private void becomeLeader() {
        serverState.setServerRole(ServerRole.LEADER);
        serverState.setLeaderId(serverId);
        votes.set(0);
        new Thread(heartbeatManager::sendHeartbeats).start();

        //talvez um listener p o heartbeatmanager comecar a enviar heartbeats
    }
    
    private void becomeFollower() {
        serverState.setServerRole(ServerRole.FOLLOWER);
    }

   
}

