package com.server;

import com.patterns.ServerRole;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerState {
    private final AtomicBoolean isLeader = new AtomicBoolean(false);
    private final AtomicInteger currentGeneration = new AtomicInteger(0);
    private volatile ServerRole serverRole = ServerRole.FOLLOWER;
    
    private final AtomicInteger leaderId = new AtomicInteger(-1);;
    
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

    
}
