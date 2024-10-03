package com.message;

import java.io.Serializable;

public class RequestVoteMessage implements Serializable, Message {
    private static final long serialVersionUID = 1L;
    int generation;
    int candidateId;
    int leaderId;

    public RequestVoteMessage(int generation, int candidateId, int leaderId) {
        this.generation = generation;
        this.candidateId = candidateId;
        this.leaderId = leaderId;
    }


    @Override
    public int getSenderId() {
        return candidateId;
    }

    
    public int getGeneration() {
        return generation;
    }
}
