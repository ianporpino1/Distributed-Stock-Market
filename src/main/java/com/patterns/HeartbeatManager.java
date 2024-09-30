package com.patterns;

import com.server.FailureListener;
import com.strategy.CommunicationStrategy;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class HeartbeatManager {
    private final Map<Integer, InetSocketAddress> nodeAddresses;
    private CommunicationStrategy strategy;
    private ScheduledExecutorService heartbeatScheduler = Executors.newScheduledThreadPool(2); //provavelmente tera de ser 2 threads
    public Map<Integer, Long> lastHeartbeatReceivedTimes = new ConcurrentHashMap<>();// uma para enviar heartbeats para o gateway e outra p receber
    public int heartbeatInterval = 2000;                                              // heartbeats do lider
    
    private ScheduledFuture<?> heartbeatTask;

    private final List<FailureListener> listeners = new ArrayList<>();
    
    public final Set<Integer> failedServers = ConcurrentHashMap.newKeySet(); // Conjunto para servidores falhos
    

    public HeartbeatManager(Map<Integer, InetSocketAddress> nodeAddresses,
                            CommunicationStrategy strategy) {
        this.nodeAddresses = nodeAddresses;
        this.strategy = strategy;

        startHeartbeatChecker();
    }

    public void startSendingHeartbeats(Message heartbeat) {
        heartbeatTask = heartbeatScheduler.scheduleAtFixedRate(() -> sendHeartbeats(heartbeat),
                0, heartbeatInterval, TimeUnit.MILLISECONDS);
    }

    public void stopSendingHeartbeats() {
        if (heartbeatTask != null) {
            heartbeatTask.cancel(false);
        }
    }

    private void sendHeartbeats(Message heartbeat) {
        nodeAddresses.forEach((otherNodeId, address) -> {
            System.out.println(heartbeat + " para: " + otherNodeId);
            strategy.sendRequest(new Request(heartbeat), address);
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
