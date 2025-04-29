package com.message;

public class VoteResponseMessage implements Message{
    
    private final int generation;
    private final boolean voteGranted;
    private final int senderId;
    
    public VoteResponseMessage(int senderId, int generation, boolean voteGranted) {
        this.generation = generation;
        this.voteGranted = voteGranted;
        this.senderId = senderId;
    }

    @Override
    public String toString() {
        String data = String.format("senderId=%d;generation=%d;vote_granted=%s",
                senderId,
                generation,
                voteGranted ? "true" : "false"
        );

        return String.format("1|VOTE_RESPONSE|%s", data);
    }

    public static VoteResponseMessage fromString(String message) {
        String[] parts = message.split("\\|");

        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid message format.");
        }

        String[] dataParts = parts[2].split(";");

        int senderId = Integer.parseInt(dataParts[0].split("=")[1]);
        int generation = Integer.parseInt(dataParts[1].split("=")[1]);
        boolean voteGranted = Boolean.parseBoolean(dataParts[2].split("=")[1]);

        return new VoteResponseMessage(senderId, generation, voteGranted);
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
