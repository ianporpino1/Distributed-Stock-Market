package com.server;

import com.patterns.Message;

import java.net.InetSocketAddress;

public interface MessageHandler {
    void handleMessage(Message message, InetSocketAddress sender);
    
}
