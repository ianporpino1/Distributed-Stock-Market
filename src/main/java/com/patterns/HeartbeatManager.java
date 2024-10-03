package com.patterns;

import com.message.HeartbeatMessage;
import com.server.FailureListener;
import com.strategy.CommunicationStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class HeartbeatManager {
    private final int serverId;

    private final Set<Integer> nodeAddresses;
    public final Set<Integer> failedServers = ConcurrentHashMap.newKeySet();
    private CommunicationStrategy strategy;
    private ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(2); //provavelmente tera de ser 2 threads
    public Map<Integer, Long> lastHeartbeatReceivedTimes = new ConcurrentHashMap<>();// uma para enviar heartbeats para o gateway e outra p receber
    public int heartbeatInterval = 2000;                                              // heartbeats do lider
    
    private ScheduledFuture<?> heartbeatTask;

    private final List<FailureListener> listeners = new ArrayList<>();
    

    public HeartbeatManager(int serverId, Set<Integer> nodeAddresses,
                            CommunicationStrategy strategy) {
        this.serverId = serverId;
        this.nodeAddresses = nodeAddresses;
        this.strategy = strategy;

        startHeartbeatChecker();
    }

    public void startSendingHeartbeats(HeartbeatMessage heartbeat) {
        heartbeatTask = heartbeatScheduler.scheduleAtFixedRate(() -> sendHeartbeats(heartbeat),
                0, heartbeatInterval, TimeUnit.MILLISECONDS);
    }

    public void stopSendingHeartbeats() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
        }
    }

    private void sendHeartbeats(HeartbeatMessage heartbeat) {
        nodeAddresses.forEach(nodeId -> {
            System.out.println(heartbeat + " para: " + nodeId);
            strategy.sendMessage(heartbeat, nodeId);
        });
    }


    public void addFailureListener(FailureListener listener) {
        listeners.add(listener);
    }

    private void startHeartbeatChecker() {
        heartbeatScheduler.scheduleWithFixedDelay(this::checkHeartbeatTimeouts,
                heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
    }


    private void checkHeartbeatTimeouts() {
        long now = System.currentTimeMillis();
        for (Map.Entry<Integer, Long> entry : lastHeartbeatReceivedTimes.entrySet()) {
            int nodeId = entry.getKey();
            long timeSinceLastHeartbeat = now - entry.getValue();
            long timeoutThreshold = 4000;

            if (failedServers.contains(nodeId)) {
                continue;
            }

            if (timeSinceLastHeartbeat >= timeoutThreshold) {
                System.out.println("Servidor " + nodeId + " considerado falho.");
                notifyNodeFailure(nodeId);
                failedServers.add(nodeId);
            }
        }
    }

    private void notifyNodeFailure(int failedId) {
        for (FailureListener listener : listeners) {
            listener.onNodeFailure(failedId);
        }
    }
    

}
