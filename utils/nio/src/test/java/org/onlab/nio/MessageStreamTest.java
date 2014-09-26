package org.onlab.nio;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests of the message message stream implementation.
 */
public class MessageStreamTest {

    private static final int LENGTH = 16;

    private static final TestMessage TM1 = new TestMessage(LENGTH);
    private static final TestMessage TM2 = new TestMessage(LENGTH);
    private static final TestMessage TM3 = new TestMessage(LENGTH);
    private static final TestMessage TM4 = new TestMessage(LENGTH);

    private static final int BIG_SIZE = 32 * 1024;
    private static final TestMessage BIG_MESSAGE = new TestMessage(BIG_SIZE);

    private static enum WritePending {
        ON, OFF;

        public boolean on() {
            return this == ON;
        }
    }

    private static enum FlushRequired {
        ON, OFF;

        public boolean on() {
            return this == ON;
        }
    }

    private FakeIOLoop loop;
    private TestByteChannel channel;
    private TestMessageStream buffer;
    private TestKey key;

    @Before
    public void setUp() throws IOException {
        loop = new FakeIOLoop();
        channel = new TestByteChannel();
        key = new TestKey(channel);
        buffer = loop.createStream(channel);
        buffer.setKey(key);
    }

    @After
    public void tearDown() {
        loop.shutdown();
        buffer.close();
    }

    // Check state of the message buffer
    private void assertState(WritePending wp, FlushRequired fr,
                             int read, int written) {
        assertEquals(wp.on(), buffer.isWritePending());
//        assertEquals(fr.on(), buffer.requiresFlush());
        assertEquals(read, channel.readBytes);
        assertEquals(written, channel.writtenBytes);
    }

    @Test
    public void endOfStream() throws IOException {
        channel.close();
        List<TestMessage> messages = buffer.read();
        assertNull(messages);
    }

    @Test
    public void bufferGrowth() throws IOException {
        // Create a buffer for big messages and test the growth.
        buffer = new TestMessageStream(BIG_SIZE, channel, loop);
        buffer.write(BIG_MESSAGE);
        buffer.write(BIG_MESSAGE);
        buffer.write(BIG_MESSAGE);
        buffer.write(BIG_MESSAGE);
        buffer.write(BIG_MESSAGE);
    }

    @Test
    public void discardBeforeKey() {
        // Create a buffer that does not yet have the key set and discard it.
        buffer = loop.createStream(channel);
        assertNull(buffer.key());
        buffer.close();
        // There is not key, so nothing to check; we just expect no problem.
    }

    @Test
    public void bufferedRead() throws IOException {
        channel.bytesToRead = LENGTH + 4;
        List<TestMessage> messages = buffer.read();
        assertEquals(1, messages.size());
        assertState(WritePending.OFF, FlushRequired.OFF, LENGTH + 4, 0);

        channel.bytesToRead = LENGTH - 4;
        messages = buffer.read();
        assertEquals(1, messages.size());
        assertState(WritePending.OFF, FlushRequired.OFF, LENGTH * 2, 0);
    }

    @Test
    public void bufferedWrite() throws IOException {
        assertState(WritePending.OFF, FlushRequired.OFF, 0, 0);

        // First write is immediate...
        buffer.write(TM1);
        assertState(WritePending.OFF, FlushRequired.OFF, 0, LENGTH);

        // Second and third get buffered...
        buffer.write(TM2);
        assertState(WritePending.OFF, FlushRequired.ON, 0, LENGTH);
        buffer.write(TM3);
        assertState(WritePending.OFF, FlushRequired.ON, 0, LENGTH);

        // Reset write, which will flush if needed; the next write is again buffered
        buffer.flushIfWriteNotPending();
        assertState(WritePending.OFF, FlushRequired.OFF, 0, LENGTH * 3);
        buffer.write(TM4);
        assertState(WritePending.OFF, FlushRequired.ON, 0, LENGTH * 3);

        // Select reset, which will flush if needed; the next write is again buffered
        buffer.flushIfPossible();
        assertState(WritePending.OFF, FlushRequired.OFF, 0, LENGTH * 4);
        buffer.write(TM1);
        assertState(WritePending.OFF, FlushRequired.ON, 0, LENGTH * 4);
        buffer.flush();
        assertState(WritePending.OFF, FlushRequired.ON, 0, LENGTH * 4);
    }

    @Test
    public void bufferedWriteList() throws IOException {
        assertState(WritePending.OFF, FlushRequired.OFF, 0, 0);

        // First write is immediate...
        List<TestMessage> messages = new ArrayList<TestMessage>();
        messages.add(TM1);
        messages.add(TM2);
        messages.add(TM3);
        messages.add(TM4);

        buffer.write(messages);
        assertState(WritePending.OFF, FlushRequired.OFF, 0, LENGTH * 4);

        buffer.write(messages);
        assertState(WritePending.OFF, FlushRequired.ON, 0, LENGTH * 4);

        buffer.flushIfPossible();
        assertState(WritePending.OFF, FlushRequired.OFF, 0, LENGTH * 8);
    }

    @Test
    public void bufferedPartialWrite() throws IOException {
        assertState(WritePending.OFF, FlushRequired.OFF, 0, 0);

        // First write is immediate...
        buffer.write(TM1);
        assertState(WritePending.OFF, FlushRequired.OFF, 0, LENGTH);

        // Tell test channel to accept only half.
        channel.bytesToWrite = LENGTH / 2;

        // Second and third get buffered...
        buffer.write(TM2);
        assertState(WritePending.OFF, FlushRequired.ON, 0, LENGTH);
        buffer.flushIfPossible();
        assertState(WritePending.ON, FlushRequired.ON, 0, LENGTH + LENGTH / 2);
    }

    @Test
    public void bufferedPartialWrite2() throws IOException {
        assertState(WritePending.OFF, FlushRequired.OFF, 0, 0);

        // First write is immediate...
        buffer.write(TM1);
        assertState(WritePending.OFF, FlushRequired.OFF, 0, LENGTH);

        // Tell test channel to accept only half.
        channel.bytesToWrite = LENGTH / 2;

        // Second and third get buffered...
        buffer.write(TM2);
        assertState(WritePending.OFF, FlushRequired.ON, 0, LENGTH);
        buffer.flushIfWriteNotPending();
        assertState(WritePending.ON, FlushRequired.ON, 0, LENGTH + LENGTH / 2);
    }

    @Test
    public void bufferedReadWrite() throws IOException {
        channel.bytesToRead = LENGTH + 4;
        List<TestMessage> messages = buffer.read();
        assertEquals(1, messages.size());
        assertState(WritePending.OFF, FlushRequired.OFF, LENGTH + 4, 0);

        buffer.write(TM1);
        assertState(WritePending.OFF, FlushRequired.OFF, LENGTH + 4, LENGTH);

        channel.bytesToRead = LENGTH - 4;
        messages = buffer.read();
        assertEquals(1, messages.size());
        assertState(WritePending.OFF, FlushRequired.OFF, LENGTH * 2, LENGTH);
    }

    // Fake IO driver loop
    private static class FakeIOLoop extends IOLoop<TestMessage, TestMessageStream> {

        public FakeIOLoop() throws IOException {
            super(500);
        }

        @Override
        protected TestMessageStream createStream(ByteChannel channel) {
            return new TestMessageStream(LENGTH, channel, this);
        }

        @Override
        protected void processMessages(List<TestMessage> messages,
                                       MessageStream<TestMessage> stream) {
        }

    }

    // Byte channel test fixture
    private static class TestByteChannel extends SelectableChannel implements ByteChannel {

        private static final int BUFFER_LENGTH = 1024;
        byte[] bytes = new byte[BUFFER_LENGTH];
        int bytesToWrite = BUFFER_LENGTH;
        int bytesToRead = BUFFER_LENGTH;
        int writtenBytes = 0;
        int readBytes = 0;

        @Override
        public int read(ByteBuffer dst) throws IOException {
            int l = Math.min(dst.remaining(), bytesToRead);
            if (bytesToRead > 0) {
                readBytes += l;
                dst.put(bytes, 0, l);
            }
            return l;
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            int l = Math.min(src.remaining(), bytesToWrite);
            writtenBytes += l;
            src.get(bytes, 0, l);
            return l;
        }

        @Override
        public Object blockingLock() {
            return null;
        }

        @Override
        public SelectableChannel configureBlocking(boolean arg0) throws IOException {
            return null;
        }

        @Override
        public boolean isBlocking() {
            return false;
        }

        @Override
        public boolean isRegistered() {
            return false;
        }

        @Override
        public SelectionKey keyFor(Selector arg0) {
            return null;
        }

        @Override
        public SelectorProvider provider() {
            return null;
        }

        @Override
        public SelectionKey register(Selector arg0, int arg1, Object arg2)
                throws ClosedChannelException {
            return null;
        }

        @Override
        public int validOps() {
            return 0;
        }

        @Override
        protected void implCloseChannel() throws IOException {
            bytesToRead = -1;
        }

    }

    // Selection key text fixture
    private static class TestKey extends SelectionKey {

        private SelectableChannel channel;

        public TestKey(TestByteChannel channel) {
            this.channel = channel;
        }

        @Override
        public void cancel() {
        }

        @Override
        public SelectableChannel channel() {
            return channel;
        }

        @Override
        public int interestOps() {
            return 0;
        }

        @Override
        public SelectionKey interestOps(int ops) {
            return null;
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public int readyOps() {
            return 0;
        }

        @Override
        public Selector selector() {
            return null;
        }
    }

}
