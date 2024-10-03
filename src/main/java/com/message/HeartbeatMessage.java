package com.message;

import java.io.Serializable;

public class HeartbeatMessage implements Message, Serializable {
    private static final long serialVersionUID = 1L;
    int generation;
    int senderId;
    int leaderId;
    
    public HeartbeatMessage(int generation, int senderId, int leaderId) {
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
}
