package com.hsbc;

import com.hsbc.objectpool.ObjectPool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransmissionTest {
    private Transmission transmission;
    private ObjectPool objectPool;

    @BeforeEach
    void setup() {
        objectPool = mock(ObjectPool.class);
        transmission = new Transmission(objectPool);
    }

    @Test
    void read() {
        Transmission.MessageMuncher m = mock(Transmission.MessageMuncher.class);
        transmission.read(100, m);
        verify(objectPool, times(1)).read(eq(100), eq(m));
    }

    @Test
    void write() {
        Message m = new Message();
        m.setE(1);
        when(objectPool.release(eq(m))).thenReturn(1L);
        assertEquals(1L, transmission.write(m));
        verify(objectPool, times(1)).release(eq(m));
    }
}