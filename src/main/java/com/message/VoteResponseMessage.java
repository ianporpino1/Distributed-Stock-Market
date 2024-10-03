package com.message;

import java.io.Serializable;


public class VoteResponseMessage implements Message, Serializable {
    private static final long serialVersionUID = 1L;
    private final int generation;
    private final boolean voteGranted;
    private final int senderId;
    
    public VoteResponseMessage(int generation, boolean voteGranted, int senderId) {
        this.generation = generation;
        this.voteGranted = voteGranted;
        this.senderId = senderId;
    }
    
    public int getGeneration() {
        return generation;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }
    
    @Override
    public int getSenderId() {
        return senderId;
    }
    
    
}
