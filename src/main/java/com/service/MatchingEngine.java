package com.service;

import com.model.Order;

public class MatchingEngine {
    public OrderBookService orderBookService;
    
    public MatchingEngine(OrderBookService orderBookService) {
        this.orderBookService = orderBookService;
    }
    
    public void processOrder(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("A ordem não pode ser nula.");
        }
        if (order.getQuantity() <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
        if (order.getPrice() <= 0) {
            throw new IllegalArgumentException("O preço deve ser maior que zero.");
        }
        
        orderBookService.addOrder(order);
    }
    
}
