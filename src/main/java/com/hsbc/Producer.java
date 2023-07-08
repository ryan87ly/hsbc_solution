package com.hsbc;

import com.hsbc.objectpool.ObjectPool;

public class Producer {
    private final Transmission transmission;
    private final ObjectPool objectPool;

    public Producer(Transmission transmission, ObjectPool objectPool) {
        this.transmission = transmission;
        this.objectPool = objectPool;
    }

    public void publish(Message m) {
        Message pooledMessage = objectPool.acquire();
        pooledMessage.setA(m.getA());
        pooledMessage.setB(m.getB());
        pooledMessage.setC(m.getC());
        pooledMessage.setD(m.getD());
        pooledMessage.setE(m.getE());
        pooledMessage.setF(m.isF());
        transmission.write(pooledMessage);
    }
}
