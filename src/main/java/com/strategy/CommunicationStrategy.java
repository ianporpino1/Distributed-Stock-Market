package com.strategy;


import com.patterns.Request;
import com.server.MessageHandler;
import com.server.OrderHandler;

import java.net.InetSocketAddress;

public interface CommunicationStrategy {
    void sendRequest(Request request, InetSocketAddress recipient);
    void startListening(int port, MessageHandler handler, OrderHandler orderHandler);
}

