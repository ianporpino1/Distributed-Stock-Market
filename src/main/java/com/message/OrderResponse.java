package com.message;

import java.io.Serializable;

public class OrderResponse implements Message, Serializable {
    @Override
    public int getSenderId() {
        return 0;
    }

   
}
