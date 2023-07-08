package com.hsbc.objectpool;

import com.hsbc.Message;
import com.hsbc.Transmission;
import com.lmax.disruptor.EventPoller;
import com.lmax.disruptor.RingBuffer;

import java.util.concurrent.atomic.AtomicLong;

public class RingBufferObjectPool implements ObjectPool {
    private final RingBuffer<Message> ringBuffer;
    private final ThreadLocal<AtomicLong> acquiredSequence;
    private final ThreadLocal<EventPoller<Message>> poller;
    private final ThreadLocal<MessageEventHandler> messageHandler;

    public RingBufferObjectPool(int bufferSize) {
        this.ringBuffer = RingBuffer.createMultiProducer(
                Message::new,
                bufferSize);
        acquiredSequence = ThreadLocal.withInitial(() -> new AtomicLong(-1));
        poller = ThreadLocal.withInitial(() -> {
            EventPoller<Message> messagePoller = this.ringBuffer.newPoller();
            this.ringBuffer.addGatingSequences(messagePoller.getSequence());
            return messagePoller;
        });
        messageHandler = ThreadLocal.withInitial(MessageEventHandler::new);

    }

    @Override
    public void read(int howMany, Transmission.MessageMuncher m) {
        MessageEventHandler handler = messageHandler.get();
        handler.prepareToRead(howMany, m);
        try {
            poller.get().poll(handler);
        } catch (Exception e) {
            throw new RingBufferOperationException(e);
        }
    }

    @Override
    public Message acquire() {
        long sequence = ringBuffer.next();
        acquiredSequence.get().set(sequence);
        return ringBuffer.get(sequence);
    }

    @Override
    public long release(Message m) {
        long sequence = acquiredSequence.get().get();
        if (sequence == -1) {
            throw new RingBufferOperationException("No acquired object");
        }
        if (ringBuffer.get(sequence) != m) {
            throw new RingBufferOperationException("Released object is not the same as acquired object");
        }
        ringBuffer.publish(sequence);
        acquiredSequence.get().set(-1);
        return (int) sequence;
    }

    private static class MessageEventHandler implements EventPoller.Handler<Message> {
        private int proceededCount;
        private int targetedMessageCount;
        private Transmission.MessageMuncher messageMuncher;

        public void prepareToRead(int count, Transmission.MessageMuncher messageMuncher) {
            this.proceededCount = 0;
            this.targetedMessageCount = count;
            this.messageMuncher = messageMuncher;
        }

        @Override
        public boolean onEvent(Message event, long sequence, boolean endOfBatch) {
            messageMuncher.on(event);
            proceededCount ++;
            return !(proceededCount >= targetedMessageCount || endOfBatch);
        }
    }
}
