package com.hsbc.objectpool;

import com.hsbc.Message;
import com.hsbc.Transmission;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.function.Executable;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RingBufferObjectPoolTest {
    private RingBufferObjectPool ringBufferObjectPool;

    @Test
    void acquireAndReleaseSuccess() {
        ringBufferObjectPool = new RingBufferObjectPool(4);

        Message messageAcquired = ringBufferObjectPool.acquire();
        assertNotNull(messageAcquired);
        assertEquals(0L, ringBufferObjectPool.release(messageAcquired));
    }

    @Test
    void releaseWithoutAcquire_exceptionIsThrown() {
        ringBufferObjectPool = new RingBufferObjectPool(4);

        Message m = new Message();
        Throwable t = assertThrows(RingBufferOperationException.class, () -> ringBufferObjectPool.release(m));
        assertEquals("No acquired object", t.getMessage());
    }

    @Test
    void releaseDifferentObjectFromAcquire_exceptionIsThrown() {
        ringBufferObjectPool = new RingBufferObjectPool(4);

        Message messageAcquired = ringBufferObjectPool.acquire();
        Message messageToRelease = new Message();
        Throwable t = assertThrows(RingBufferOperationException.class, () -> ringBufferObjectPool.release(messageToRelease));
        assertEquals("Released object is not the same as acquired object", t.getMessage());
    }

    @Test
    void consumerReadsWhenNoMessageIsPublished() {
        ringBufferObjectPool = new RingBufferObjectPool(4);
        Transmission.MessageMuncher messageMuncher = mock(Transmission.MessageMuncher.class);

        ringBufferObjectPool.read(1, messageMuncher);
        verify(messageMuncher, never()).on(any());
    }

    @Test
    void publishAndReadMessage() {
        ringBufferObjectPool = new RingBufferObjectPool(4);
        Transmission.MessageMuncher messageMuncher = mock(Transmission.MessageMuncher.class);
        ringBufferObjectPool.read(0, messageMuncher);
        verify(messageMuncher, never()).on(any());

        publishMessage(1);
        ringBufferObjectPool.read(1, messageMuncher);
        verify(messageMuncher, times(1)).on(any());
        verify(messageMuncher, times(1)).on(argThat(m -> m.getE() == 1));
        reset(messageMuncher);

        publishMessage(2);
        ringBufferObjectPool.read(1, messageMuncher);
        verify(messageMuncher, times(1)).on(any());
        verify(messageMuncher, times(1)).on(argThat(m -> m.getE() == 2));
        reset(messageMuncher);
    }

    @Test
    void consumerReadsMoreThanAvailableMessages() {
        ringBufferObjectPool = new RingBufferObjectPool(4);
        Transmission.MessageMuncher messageMuncher = mock(Transmission.MessageMuncher.class);
        ringBufferObjectPool.read(0, messageMuncher);
        verify(messageMuncher, never()).on(any());

        publishMessage(1);
        publishMessage(2);
        ringBufferObjectPool.read(3, messageMuncher);
        verify(messageMuncher, times(2)).on(any());
        verify(messageMuncher, times(1)).on(argThat(m -> m.getE() == 1));
        verify(messageMuncher, times(1)).on(argThat(m -> m.getE() == 2));
    }

    @Test
    @Timeout(value = 10)
    void singleProducerAndSingleConsumer_runningOnDifferentThreads() throws InterruptedException {
        ringBufferObjectPool = new RingBufferObjectPool(4);
        CapturedConsumer consumer = new CapturedConsumer(0);
        Thread consumerThread = new Thread(consumer);
        consumerThread.start();

        while(!consumer.isStarted()) {
            Thread.sleep(1);
        }

        Thread producerThread = new Thread(() -> publishMessages(1, 1000));
        producerThread.start();
        producerThread.join();

        // Sleep a while to wait for consumers to finish
        Thread.sleep(100);
        consumer.stop();

        assertEquals(999, consumer.capturedIntegers.size());
        for (int i = 1; i < 1000; i ++) {
            assertTrue(consumer.capturedIntegers.contains(i));
        }
    }

    @Test
    @Timeout(value = 10)
    void multipleProducersAndMultipleConsumers_runningOnDifferentThreads() throws InterruptedException {
        ringBufferObjectPool = new RingBufferObjectPool(2048);
        CapturedConsumer[] consumers = createConsumers(3, 0);

        Thread producer1Thread = new Thread(() -> publishMessages(1, 1000));
        producer1Thread.start();
        Thread producer2Thread = new Thread(() -> publishMessages(1000, 2000));
        producer2Thread.start();
        producer1Thread.join();
        producer2Thread.join();

        // Sleep a while to wait for consumers to finish
        Thread.sleep(100);

        for (CapturedConsumer consumer : consumers) {
            consumer.stop();
        }

        for (CapturedConsumer consumer : consumers) {
            assertEquals(1999, consumer.capturedIntegers.size());
            for (int i = 1; i < 2000; i++) {
                assertTrue(consumer.capturedIntegers.contains(i));
            }
        }
    }

    @Test
    @Timeout(value = 10)
    void slowConsumersCauseBackpressureOnProducers() throws Throwable {
        ringBufferObjectPool = new RingBufferObjectPool(1);
        // Apply 1000ms delay on consumer process
        CapturedConsumer consumer = createConsumers(1, 1000)[0];

        // Before object pool is full, no backpressure on producer
        Thread producer1Thread = new Thread(() -> publishMessage(1));
        producer1Thread.start();
        long publishTimeBeforePoolIsFull = measureRunTime(producer1Thread::join);
        assertTrue(publishTimeBeforePoolIsFull < 200);

        // After object pool is full, there is backpressure on producer, causing publishing delay
        Thread producer2Thread = new Thread(() -> publishMessage(1));
        producer2Thread.start();
        long publishTimeAfterPoolIsFull = measureRunTime(producer2Thread::join);
        assertTrue(publishTimeAfterPoolIsFull > 950);

        Thread.sleep(1000);
        consumer.stop();
        assertEquals(2, consumer.capturedIntegers.size());
    }

    private long measureRunTime(Executable executable) throws Throwable {
        long startTime = System.currentTimeMillis();
        executable.execute();
        long entTime = System.currentTimeMillis();
        return entTime - startTime;
    }

    private CapturedConsumer[] createConsumers(int consumerCount, int processDelayMS) throws InterruptedException {
        CapturedConsumer[] consumers = new CapturedConsumer[consumerCount];
        for (int i = 0; i < consumerCount; i++) {
            CapturedConsumer consumer = new CapturedConsumer(processDelayMS);
            consumers[i] = consumer;
            Thread consumerThread = new Thread(consumer);
            consumerThread.start();

            while(!consumer.isStarted()) {
                Thread.sleep(1);
            }
        }
        return consumers;
    }

    private void publishMessages(int from, int to) {
        for (int i = from; i < to; i++) {
            publishMessage(i);
        }
    }

    private void publishMessage(int e) {
        Message acquiredMessage = ringBufferObjectPool.acquire();
        acquiredMessage.setE(e);
        acquiredMessage.setC("String" + e);
        ringBufferObjectPool.release(acquiredMessage);
    }

    private class CapturedConsumer implements Transmission.MessageMuncher, Runnable {
        private final List<Integer> capturedIntegers;
        private int processDelayMs;
        private volatile boolean stopped;
        private volatile boolean started;

        CapturedConsumer(int processDelayMs) {
            this.capturedIntegers = new LinkedList<>();
            this.processDelayMs = processDelayMs;
            this.stopped = false;
            this.started = false;
        }

        @Override
        public boolean on(Message m) {
            capturedIntegers.add(m.getE());
            if (processDelayMs > 0) {
                try {
                    Thread.sleep(processDelayMs);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return true;
        }

        @Override
        public void run() {
            while(!stopped) {
                ringBufferObjectPool.read(10, this);
                started = true;
            }
        }

        void stop() {
            stopped = true;
        }

        boolean isStarted() {
            return started;
        }
    }


}