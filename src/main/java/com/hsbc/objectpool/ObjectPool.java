package com.hsbc.objectpool;

import com.hsbc.Message;
import com.hsbc.Transmission;

public interface ObjectPool {
    void read(int howMany, Transmission.MessageMuncher m);
    Message acquire();
    long release(Message m);
}
