package com.server;

import java.net.InetSocketAddress;

public interface OrderHandler {
    void handleOrder(String orderMessage, InetSocketAddress sender);
}
