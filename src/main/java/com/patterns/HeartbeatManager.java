package com.patterns;

import com.server.LeaderFailureListener;
import com.server.ServerState;
import com.strategy.CommunicationStrategy;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class HeartbeatManager {
    private final int serverId;
    
    private final Map<Integer, InetSocketAddress> nodeAddresses;
    private CommunicationStrategy strategy;
    private ScheduledExecutorService heartbeatChecker = Executors.newScheduledThreadPool(1);
    private Map<Integer, Long> lastHeartbeatReceivedTimes = new ConcurrentHashMap<>();
    public int heartbeatInterval = 2000;

    private final List<LeaderFailureListener> listeners = new ArrayList<>();


    private final ServerState serverState;
    
    public HeartbeatManager(int serverId, Map<Integer, InetSocketAddress> nodeAddresses,
                            CommunicationStrategy strategy, ServerState serverState) {
        this.serverId = serverId;
        this.nodeAddresses = nodeAddresses;
        this.strategy = strategy;
        this.serverState = serverState;

        startHeartbeatChecker();
    }
    
    public void addLeaderFailureListener(LeaderFailureListener listener) {
        listeners.add(listener);
    }

    private void startHeartbeatChecker() {
        heartbeatChecker.scheduleWithFixedDelay(this::checkHeartbeatTimeouts,
                heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
    }

    private void checkHeartbeatTimeouts() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Integer, Long> entry : lastHeartbeatReceivedTimes.entrySet()) {
            long timeSinceLastHeartbeat = now - entry.getValue();
            System.out.println("Tempo desde o último heartbeat do servidor " + entry.getKey() + ": " + timeSinceLastHeartbeat);
            long timeoutThreshold = 4000;
            if (timeSinceLastHeartbeat >= timeoutThreshold) {
                System.out.println("Servidor " + entry.getKey() + " considerado falho.");
                
                if (serverState.getLeaderId() == entry.getKey()) {
                    System.out.println("LIDER " + serverState.getLeaderId() + " FALHOU..."  );
                    serverState.setLeaderId(-1);
                    
                    notifyLeaderFailure();
                }
            }
        }

    }
    private void notifyLeaderFailure() {
        for (LeaderFailureListener listener : listeners) {
            listener.onLeaderFailure();
        }
    }

    public void handleHeartbeat(Message message) {
        if (message.getGeneration() >= serverState.getCurrentGeneration()) {
            if(serverState.getServerRole() != ServerRole.FOLLOWER) serverState.setServerRole(ServerRole.FOLLOWER);
            serverState.setElectionInProgress(false);
            serverState.setLeaderId(message.getLeaderId());
            serverState.setCurrentGeneration(message.getGeneration());
            lastHeartbeatReceivedTimes.put(message.getSenderId(), System.currentTimeMillis());
            System.out.println("Node " + serverId + " reconhece o líder " + message.getLeaderId() + " no termo " + serverState.getCurrentGeneration());
        }
    }

    public void sendHeartbeats() {
        while (serverState.getServerRole() == ServerRole.LEADER) {
            nodeAddresses.forEach((otherNodeId, address) -> {
                if (otherNodeId != serverId) {
                    Message heartbeat = new Message(MessageType.HEARTBEAT, serverState.getCurrentGeneration(), serverId, serverId);
                    System.out.println(heartbeat +  " para: " + otherNodeId);
                    strategy.sendMessage(heartbeat, address);
                }
            });
            try {
                Thread.sleep(heartbeatInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Heartbeat sending thread interrupted: " + e.getMessage());
                break;
            }
        }
    }
}
