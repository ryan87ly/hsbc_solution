package com.hsbc;

import com.hsbc.objectpool.ObjectPool;
import com.hsbc.objectpool.RingBufferObjectPool;
import com.hsbc.output.ConsoleOutput;
import com.hsbc.output.Output;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IntegrationTest {
    private Transmission transmission;
    private ObjectPool objectPool;
    private CapturedOutput output;
    private Message[] messagesForTest;

    @BeforeEach
    void setup() {
        objectPool = new RingBufferObjectPool(32);
        transmission = new Transmission(objectPool);
        output = new CapturedOutput(new ConsoleOutput());
        messagesForTest = new Message[] {
            createMessage(Message.Type.QUOTE, "b1", "c1", 1L, 1, true),
            createMessage(Message.Type.TRADE, "b2", "c2", 2L, 2, true),
            createMessage(Message.Type.REFERENCE, "b3", "c3", 3L, 3, true)
        };
    }

    @Test
    void publishAndConsume() throws InterruptedException {
        Consumer consumer = new Consumer(transmission, output);
        Thread consumerThread = new Thread(() -> {
            while(true) {
                consumer.run();
            }
        });
        consumerThread.start();
        Thread.sleep(100);

        Producer producer = new Producer(transmission, objectPool);
        producer.publish(messagesForTest[0]);
        producer.publish(messagesForTest[1]);
        producer.publish(messagesForTest[2]);

        // Sleep a while for the consumers to process messages
        Thread.sleep(100);
        assertEquals(3, output.capturedMessages.size());
        assertEquals(messagesForTest[0], output.capturedMessages.get(0));
        assertEquals(messagesForTest[1], output.capturedMessages.get(1));
        assertEquals(messagesForTest[2], output.capturedMessages.get(2));

        consumerThread.interrupt();
    }

    @Test
    void multipleConsumers() throws InterruptedException {
        CapturedOutput consumer1Output = new CapturedOutput(new ConsoleOutput());
        Consumer consumer1 = new Consumer(transmission, consumer1Output);
        Thread consumer1Thread = new Thread(() -> {
            while(true) {
                consumer1.run();
            }
        });
        consumer1Thread.start();
        CapturedOutput consumer2Output = new CapturedOutput(new ConsoleOutput());
        Consumer consumer2 = new Consumer(transmission, consumer2Output);
        Thread consumer2Thread = new Thread(() -> {
            while(true) {
                consumer2.run();
            }
        });
        consumer2Thread.start();
        Thread.sleep(100);

        Producer producer = new Producer(transmission, objectPool);
        producer.publish(messagesForTest[0]);
        producer.publish(messagesForTest[1]);
        producer.publish(messagesForTest[2]);

        // Sleep a while for the consumers to process messages
        Thread.sleep(100);
        assertEquals(3, consumer1Output.capturedMessages.size());
        assertEquals(messagesForTest[0], consumer1Output.capturedMessages.get(0));
        assertEquals(messagesForTest[1], consumer1Output.capturedMessages.get(1));
        assertEquals(messagesForTest[2], consumer1Output.capturedMessages.get(2));

        assertEquals(3, consumer2Output.capturedMessages.size());
        assertEquals(messagesForTest[0], consumer2Output.capturedMessages.get(0));
        assertEquals(messagesForTest[1], consumer2Output.capturedMessages.get(1));
        assertEquals(messagesForTest[2], consumer2Output.capturedMessages.get(2));
    }

    private static Message createMessage(Message.Type a, String b, String c, long d, int e, boolean f) {
        Message m = new Message();
        m.setA(a);
        m.setB(b);
        m.setC(c);
        m.setD(d);
        m.setE(e);
        m.setF(f);
        return m;
    }

    private class CapturedOutput implements Output {
        private final Output base;
        private final List<Message> capturedMessages;

        private CapturedOutput(Output base) {
            this.base = base;
            this.capturedMessages = new ArrayList<>();
        }

        @Override
        public void print(Object msg) {
            base.print(msg);
            capturedMessages.add((Message) ((Message) msg).clone());
        }
    }

}
