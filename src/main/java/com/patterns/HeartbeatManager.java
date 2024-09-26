package com.patterns;

import com.server.FailureListener;
import com.server.ServerState;
import com.strategy.CommunicationStrategy;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class HeartbeatManager {
    private final int serverId;

    private final Map<Integer, InetSocketAddress> nodeAddresses;
    private CommunicationStrategy strategy;
    private ScheduledExecutorService heartbeatChecker = Executors.newScheduledThreadPool(1);
    private Map<Integer, Long> lastHeartbeatReceivedTimes = new ConcurrentHashMap<>();
    public int heartbeatInterval = 2000;
    
    private ScheduledFuture<?> heartbeatTask;

    private final List<FailureListener> listeners = new ArrayList<FailureListener>();


    private final ServerState serverState;

    public HeartbeatManager(int serverId, Map<Integer, InetSocketAddress> nodeAddresses,
                            CommunicationStrategy strategy, ServerState serverState) {
        this.serverId = serverId;
        this.nodeAddresses = nodeAddresses;
        this.strategy = strategy;
        this.serverState = serverState;

        startHeartbeatChecker();
    }

    public void startSendingHeartbeats() {
        stopSendingHeartbeats();
        heartbeatTask = heartbeatChecker.scheduleAtFixedRate(
                this::sendHeartbeats,
                0,
                heartbeatInterval,
                TimeUnit.MILLISECONDS
        );
    }

    public void stopSendingHeartbeats() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
        }
    }

    private void sendHeartbeats() {
        if (serverState.getServerRole() == ServerRole.LEADER) {
            nodeAddresses.forEach((otherNodeId, address) -> {
                Message heartbeat = new Message(
                        MessageType.HEARTBEAT,
                        serverState.getCurrentGeneration(),
                        serverId,
                        serverId
                );
                System.out.println(heartbeat + " para: " + otherNodeId);
                strategy.sendMessage(heartbeat, address);
            });
        }

    }


    public void addFailureListener(FailureListener listener) {
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

                notifyNodeFailure(entry.getKey());
            }
        }

    }

    private void notifyNodeFailure(int failedId) {
        for (FailureListener listener : listeners) {
            listener.onNodeFailure(failedId);
        }
    }

    public void handleHeartbeat(Message message) {
        if (message.getGeneration() >= serverState.getCurrentGeneration()) {
            if (serverState.getServerRole() != ServerRole.FOLLOWER) serverState.setServerRole(ServerRole.FOLLOWER);
            serverState.setLeaderId(message.getLeaderId());
            serverState.setCurrentGeneration(message.getGeneration());
            lastHeartbeatReceivedTimes.put(message.getSenderId(), System.currentTimeMillis());
            System.out.println("Node " + serverId + " reconhece o líder " + message.getLeaderId() + " no termo " + serverState.getCurrentGeneration());
        }
    }

}
