package com.service;

import com.model.Order;

public class MatchingEngine {
    public OrderBookService orderBookService;
    
    public MatchingEngine(OrderBookService orderBookService) {
        this.orderBookService = orderBookService;
    }
    
    public void processOrder(Order order) {
        orderBookService.addOrder(order);
    }
    
}
