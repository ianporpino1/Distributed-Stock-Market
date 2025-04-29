package com.message;


public class RequestVoteMessage implements Message {
    int generation;
    int candidateId;
    int leaderId;

    public RequestVoteMessage(int generation, int candidateId, int leaderId) {
        this.generation = generation;
        this.candidateId = candidateId;
        this.leaderId = leaderId;
    }

    @Override
    public String toString() {
        return String.format("1|VOTE_REQUEST|generation=%d;candidateId=%d;leaderId=%d", generation, candidateId, leaderId);
    }

    public static RequestVoteMessage fromString(String message) {
        String[] parts = message.split("\\|");

        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid message format.");
        }

        String[] dataParts = parts[2].split(";");

        int generation = Integer.parseInt(dataParts[0].split("=")[1]);
        int candidateId = Integer.parseInt(dataParts[1].split("=")[1]);
        int leaderId = Integer.parseInt(dataParts[2].split("=")[1]);

        return new RequestVoteMessage(generation, candidateId, leaderId);
    }

    @Override
    public int getSenderId() {
        return candidateId;
    }

    
    public int getGeneration() {
        return generation;
    }
}
