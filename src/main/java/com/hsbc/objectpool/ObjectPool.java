package com.hsbc.objectpool;

import com.hsbc.Message;
import com.hsbc.Transmission;

public interface ObjectPool {
    /**
     * Read up to {@code @param howMany } messages from message queue and revoke callbacks on {@code @param m }
     */
    void read(int howMany, Transmission.MessageMuncher m);

    /**
     * Acquire reusable Message instance from object pool
     * @return Message instance
     */
    Message acquire();

    /**
     * Release Message to object pool, make it ready to use.
     * It will block the calling thread if the message queue(object pool) is full.
     * @return A unique sequence id
     */
    long release(Message m);
}
