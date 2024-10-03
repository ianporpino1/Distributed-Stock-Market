package com.server;

import com.patterns.ServerRole;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerState {
    private final AtomicBoolean isLeader = new AtomicBoolean(false);
    private final AtomicInteger currentGeneration = new AtomicInteger(0);
    private volatile ServerRole serverRole = ServerRole.FOLLOWER;
    
    private final AtomicInteger leaderId = new AtomicInteger(-1);

    private final Map<Integer, Integer> votedForAtGeneration = new ConcurrentHashMap<>();
    private final AtomicInteger votes = new AtomicInteger(0);
    
    public int getLeaderId() {
        return leaderId.get();
    }
    
    public void setLeaderId(int leaderId) {
        this.leaderId.set(leaderId);    
    }
    
    public boolean isLeader() {
        return isLeader.get();
    }

    public void incrementGeneration() {
        currentGeneration.incrementAndGet();
    }

    public int getCurrentGeneration() {
        return currentGeneration.get();
    }

    public void setCurrentGeneration(int generation) {
        currentGeneration.set(generation);
    }
    
    public ServerRole getServerRole() {
        return serverRole;
    }

    public void setServerRole(ServerRole role) {
        this.serverRole = role;
        isLeader.set(role == ServerRole.LEADER);
    }
    public synchronized boolean voteFor(int nodeId, int generation) {
        if (!votedForAtGeneration.containsKey(nodeId) || votedForAtGeneration.get(nodeId) != generation) {
            votedForAtGeneration.put(nodeId, generation);
            return true; // Voto concedido
        }
        return false; // Já havia votado nesta geração
    }


    public Map<Integer, Integer> getVotedForAtGeneration() {
        return votedForAtGeneration;
    }
    public void addVotedForAtGeneration(int nodeId, int generation) {
        votedForAtGeneration.put(nodeId, generation);
    }

    public int getVotes() {
        return votes.get();
    }
    public void setVotes(int votes) {
        this.votes.set(votes);
    }
    public void incrementVotes() {
        votes.incrementAndGet();
    }
}
