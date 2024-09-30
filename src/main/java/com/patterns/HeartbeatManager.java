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
    private ScheduledExecutorService heartbeatChecker = Executors.newScheduledThreadPool(1); //provavelmente tera de ser 2 threads
    private Map<Integer, Long> lastHeartbeatReceivedTimes = new ConcurrentHashMap<>();// uma para enviar heartbeats para o gateway e outra p receber
    public int heartbeatInterval = 2000;                                              // heartbeats do lider
    
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

    private void sendHeartbeats() { //provalvelmente vai ser modificado
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
        else{
            stopSendingHeartbeats();
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
    
    //talvez add listener onLeaderDetected, talvez nem precise, pq quem chama esse metodo eh o server
    public void handleHeartbeat(Message message) {
        if (message.getGeneration() >= serverState.getCurrentGeneration()) {
            if (serverState.getServerRole() != ServerRole.FOLLOWER){
                serverState.setServerRole(ServerRole.FOLLOWER);
                System.out.println("Node " + serverId + " reconhece o líder " + message.getLeaderId() + " no termo " + serverState.getCurrentGeneration());
            } 
            serverState.setLeaderId(message.getLeaderId());
            serverState.setCurrentGeneration(message.getGeneration());
            lastHeartbeatReceivedTimes.put(message.getSenderId(), System.currentTimeMillis());
        }
    }

}
