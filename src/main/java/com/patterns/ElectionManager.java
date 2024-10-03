package com.patterns;

import com.message.RequestVoteMessage;
import com.message.VoteResponseMessage;
import com.server.LeaderElectedListener;
import com.server.ServerState;
import com.strategy.CommunicationStrategy;

import java.util.Random;
import java.util.Set;

public class ElectionManager {
    private final int serverId;
    private final ServerState state;
    private final Set<Integer> nodeAddresses;
    private final int electionTimeout = 5000;
    private final CommunicationStrategy strategy;
    private LeaderElectedListener listener;

    public ElectionManager(int serverId, Set<Integer> nodeAddresses, CommunicationStrategy strategy,
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
            Thread.currentThread().interrupt();
        }
    }

    public void startElection() {
        state.incrementGeneration();
        System.out.println("Iniciando Election... Generation: " + state.getCurrentGeneration());
        state.setServerRole(ServerRole.CANDIDATE);
        state.setVotes(0);
        state.voteFor(serverId, state.getCurrentGeneration());

        state.incrementVotes();
        
        RequestVoteMessage requestVoteMessage = new RequestVoteMessage(state.getCurrentGeneration(), serverId, -1);

        for (int nodeId : nodeAddresses) {
            strategy.sendMessage(requestVoteMessage, nodeId);
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
            state.setServerRole(ServerRole.FOLLOWER);
        }
    }

    public void handleRequestVote(RequestVoteMessage message) {
        boolean voteGranted = false;
        if (message.getGeneration() >= state.getCurrentGeneration()) {
            state.setCurrentGeneration(message.getGeneration());
            if (state.voteFor(message.getSenderId(), message.getGeneration())) {
                System.out.println(serverId + " votou para " + message.getSenderId() + " na geração " + state.getCurrentGeneration());
                voteGranted = true;
            }
        }
        VoteResponseMessage response = new VoteResponseMessage(state.getCurrentGeneration(), voteGranted, serverId);
        strategy.sendMessage(response, message.getSenderId());
    }

    public void handleVoteResponse(VoteResponseMessage message) {
        synchronized (state) {
            if (state.getServerRole() == ServerRole.CANDIDATE && message.getGeneration() == state.getCurrentGeneration()) {
                if (message.isVoteGranted()) {
                    state.incrementVotes();
                    System.out.println("Node " + serverId + " recebeu um voto. Total: " + state.getVotes());

                    if (state.getVotes() > (nodeAddresses.size() / 2)) {
                        System.out.println("Node " + serverId + " se tornou o líder na geração " + state.getCurrentGeneration());
                        becomeLeader();
                    }
                }
            }
        }
    }

    private void becomeLeader() {
        state.setServerRole(ServerRole.LEADER);
        state.setLeaderId(serverId);
        state.setVotes(0);
        if (listener != null) {
            listener.onLeaderElected();
        }
    }

    public void addLeaderElectedListener(LeaderElectedListener listener) {
        this.listener = listener;
    }
}
