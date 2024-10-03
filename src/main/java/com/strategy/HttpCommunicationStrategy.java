package com.strategy;

import com.patterns.Message;
import com.server.MessageHandler;
import com.server.OrderHandler;

import java.net.InetSocketAddress;

public class HttpCommunicationStrategy implements CommunicationStrategy {



    @Override
    public void sendMessage(com.message.Message message, int nodeId) {
        
    }

    @Override
    public void startListening(int port, MessageHandler handler, OrderHandler orderHandler) {

    }
}
