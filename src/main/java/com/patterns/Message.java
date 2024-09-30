package com.patterns;

import java.io.Serializable;

public class Message implements Serializable {
    
    private MessageType type;
    private int generation;
    private int senderId; 
    private int leaderId;
    
    public Message(MessageType type){
        this.type = type;
    }
    public Message(MessageType type, int generation, int senderId, int leaderId) {
        this.type = type;
        this.generation = generation;
        this.senderId = senderId;
        this.leaderId = leaderId;
    }

    public Message() {
        
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public int getGeneration() {
        return generation;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getLeaderId() {
        return leaderId;
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", generation=" + generation +
                ", candidateId=" + senderId +
                ", leaderId=" + leaderId +
                '}';
    }
}
