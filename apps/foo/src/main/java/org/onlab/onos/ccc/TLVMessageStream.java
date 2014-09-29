package org.onlab.onos.ccc;

import org.onlab.nio.IOLoop;
import org.onlab.nio.MessageStream;

import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

import static com.google.common.base.Preconditions.checkState;

/**
 * Stream for transferring TLV messages between cluster members.
 */
public class TLVMessageStream extends MessageStream<TLVMessage> {

    private static final long MARKER = 0xfeedcafecafefeedL;

    /**
     * Creates a message stream associated with the specified IO loop and
     * backed by the given byte channel.
     *
     * @param loop          IO loop
     * @param byteChannel   backing byte channel
     * @param bufferSize    size of the backing byte buffers
     * @param maxIdleMillis maximum number of millis the stream can be idle
     */
    protected TLVMessageStream(IOLoop<TLVMessage, ?> loop, ByteChannel byteChannel,
                               int bufferSize, int maxIdleMillis) {
        super(loop, byteChannel, bufferSize, maxIdleMillis);
    }

    @Override
    protected TLVMessage read(ByteBuffer buffer) {
        long marker = buffer.getLong();
        checkState(marker == MARKER, "Incorrect message marker");

        int type = buffer.getInt();
        int length = buffer.getInt();

        // TODO: add deserialization hook here

        return new TLVMessage(type, length, null);
    }

    @Override
    protected void write(TLVMessage message, ByteBuffer buffer) {
        buffer.putLong(MARKER);
        buffer.putInt(message.type());
        buffer.putInt(message.length());

        // TODO: add serialization hook here
    }
}
