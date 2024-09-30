package com.patterns;

import com.server.LeaderElectedListener;
import com.server.ServerState;
import com.strategy.CommunicationStrategy;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Random;


public class ElectionManager {
    private final int serverId;
    private final ServerState state;

   
    private final Map<Integer, InetSocketAddress> nodeAddresses;

    private final int electionTimeout = 5000;
    private final CommunicationStrategy strategy;

    private LeaderElectedListener listener;


    public ElectionManager(int serverId, Map<Integer, InetSocketAddress> nodeAddresses, CommunicationStrategy strategy,
                           ServerState state) {
        this.serverId = serverId;
        this.state = state;
        this.nodeAddresses = nodeAddresses;
        this.strategy = strategy;
    }


    public void startElectionTimeout() {
        try {
            Thread.sleep(electionTimeout + new Random().nextInt(2000));
            if (state.getLeaderId() == -1) {
                startElection();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void startElection() {
        state.incrementGeneration();
        System.out.println("Iniciando Election... Generation: " + state.getCurrentGeneration());
        state.setServerRole(ServerRole.CANDIDATE);
        state.addVotedForAtGeneration(serverId,state.getCurrentGeneration());
        
        state.incrementVotes();

        for (int otherNodeId : nodeAddresses.keySet()) {
            Message requestVote = new Message(MessageType.REQUEST_VOTE, state.getCurrentGeneration(), serverId, -1);
            strategy.sendRequest(new Request(requestVote), nodeAddresses.get(otherNodeId));
        }
        waitForVotes();
    }

    private void waitForVotes() {
        long waitUntil = System.currentTimeMillis() + electionTimeout;
        while (System.currentTimeMillis() < waitUntil && state.getServerRole() == ServerRole.CANDIDATE) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (state.getServerRole() == ServerRole.CANDIDATE && state.getVotes() <= (nodeAddresses.size() / 2)) {
            System.out.println("Nenhuma resposta de outros nós. Node " + serverId + " voltando a follower.");

            startElection();
        }
    }

    public void handleVoteRequest(Message message) {
        if (message.getGeneration() >= state.getCurrentGeneration()) {
            state.setCurrentGeneration(message.getGeneration());
            if (state.getVotedForAtGeneration().getOrDefault(state.getCurrentGeneration(), -1) == -1) {
                state.addVotedForAtGeneration(message.getSenderId(), state.getCurrentGeneration());
                System.out.println(serverId + " votou para " + message.getSenderId() + " na geração " + state.getCurrentGeneration() + ".");

                Message vote = new Message(MessageType.VOTE, state.getCurrentGeneration(), serverId, -1);
                strategy.sendRequest(new Request(vote), nodeAddresses.get(message.getSenderId()));
            }
        }
    }


    public void handleVoteResponse(Message message) {
        synchronized (state){
            if (state.getServerRole() == ServerRole.CANDIDATE && message.getGeneration() == state.getCurrentGeneration()) {
                state.incrementVotes();
                System.out.println("Node " + serverId + " recebeu um voto. Total: " + state.getVotes());
                
                if (state.getVotes() > (nodeAddresses.size() / 2)) {
                    System.out.println("Node " + serverId + " se tornou o líder no termo " + state.getCurrentGeneration());
                    becomeLeader();
                }
            }
        }
    }

    private void becomeLeader() {
        state.setServerRole(ServerRole.LEADER);
        state.setLeaderId(serverId);
        state.setVotes(0);

        listener.onLeaderElected();
    }

//    private void becomeFollower() {
//        serverState.setServerRole(ServerRole.FOLLOWER);
//    }

    public void addLeaderElectedListener(LeaderElectedListener listener) {
        this.listener = listener;
    }
}

