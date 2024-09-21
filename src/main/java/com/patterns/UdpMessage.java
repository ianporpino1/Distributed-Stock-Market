package com.patterns;

import java.io.Serializable;

public class UdpMessage implements Serializable {
    

    private MessageType type;
    private int generation;          
    private int candidateId; 
    private int leaderId;

    public UdpMessage(MessageType type, int generation, int candidateId, int leaderId) {
        this.type = type;
        this.generation = generation;
        this.candidateId = candidateId;
        this.leaderId = leaderId;
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

    public void setGeneration(int generation) {
        this.generation = generation;
    }

    public int getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(int candidateId) {
        this.candidateId = candidateId;
    }

    public int getLeaderId() {
        return leaderId;
    }

    public void setLeaderId(int leaderId) {
        this.leaderId = leaderId;
    }
    

    @Override
    public String toString() {
        return "UdpMessage{" +
                "type=" + type +
                ", generation=" + generation +
                ", candidateId=" + candidateId +
                ", leaderId=" + leaderId +
                '}';
    }
}
