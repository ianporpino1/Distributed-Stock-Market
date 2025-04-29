package com.message;


public class HeartbeatMessage implements Message {
    int generation;
    int senderId;
    int leaderId;
    
    public HeartbeatMessage(int senderId, int generation, int leaderId) {
        this.generation = generation;
        this.senderId = senderId;
        this.leaderId = leaderId;
    }
    
    public int getLeaderId(){
        return leaderId;
    }
    
    @Override
    public int getSenderId() {
        return senderId;
    }
    
    public int getGeneration() {
        return generation;
    }

    @Override
    public String toString() {
        return String.format("1|HEARTBEAT|senderId=%d;generation=%d;leaderId=%d", senderId,generation, leaderId);
    }

    public static HeartbeatMessage fromString(String message) {
        String[] parts = message.split("\\|");
        String[] dataParts = parts[2].split(";");
        
        int senderId = Integer.parseInt(dataParts[0].split("=")[1]);
        int generation = Integer.parseInt(dataParts[1].split("=")[1]);
        int leaderId = Integer.parseInt(dataParts[2].split("=")[1]);

        return new HeartbeatMessage(senderId, generation, leaderId);
    }
}
