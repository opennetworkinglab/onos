package org.onlab.nio;

import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

/**
 * Fixed-length message transfer buffer.
 */
public class TestMessageStream extends MessageStream<TestMessage> {

    private static final String E_WRONG_LEN = "Illegal message length: ";

    private final int length;

    /**
     * Create a new buffer for transferring messages of the specified length.
     *
     * @param length message length
     * @param ch     backing channel
     * @param loop   driver loop
     */
    public TestMessageStream(int length, ByteChannel ch,
                             IOLoop<TestMessage, ?> loop) {
        super(loop, ch, 64 * 1024, 500);
        this.length = length;
    }

    @Override
    protected TestMessage read(ByteBuffer rb) {
        if (rb.remaining() < length) {
            return null;
        }
        TestMessage message = new TestMessage(length);
        rb.get(message.data());
        return message;
    }

    /**
     * {@inheritDoc}
     * <p/>
     * This implementation enforces the message length against the buffer
     * supported length.
     *
     * @throws IllegalArgumentException if message size does not match the
     *                                  supported buffer size
     */
    @Override
    protected void write(TestMessage message, ByteBuffer wb) {
        if (message.length() != length) {
            throw new IllegalArgumentException(E_WRONG_LEN + message.length());
        }
        wb.put(message.data());
    }

}
