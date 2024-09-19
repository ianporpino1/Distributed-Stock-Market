package com.service;

import com.model.Order;
import com.model.OrderBook;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OrderBookService {
    private final ConcurrentHashMap<String, OrderBook> orderBooks;

    public OrderBookService() {
        this.orderBooks = new ConcurrentHashMap<>();
    }

    private OrderBook getOrderBook(String symbol) {
        return orderBooks.computeIfAbsent(symbol, k -> new OrderBook());
    }

    void addOrder(Order order) {
        OrderBook orderBook = getOrderBook(order.getSymbol());
        orderBook.addOrder(order);
        System.out.println("ORDERS: " + orderBook.getBuyOrders());
    }
    
}

