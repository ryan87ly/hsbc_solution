package com.hsbc;

import com.hsbc.objectpool.ObjectPool;

public class Transmission {
    public interface MessageMuncher {
        boolean on(Message m);
    }

    private final ObjectPool objectPool;

    public Transmission(ObjectPool objectPool) {
        this.objectPool = objectPool;
    }

    public void read(int howMany, MessageMuncher m) {
        // your code here
        objectPool.read(howMany, m);
    }

    public int write(Message m) {
        return (int) objectPool.release(m); // your code here
    }
}