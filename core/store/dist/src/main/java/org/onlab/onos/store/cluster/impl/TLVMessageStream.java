package org.onlab.onos.store.cluster.impl;

import org.onlab.nio.IOLoop;
import org.onlab.nio.MessageStream;
import org.onlab.onos.cluster.DefaultControllerNode;

import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

import static com.google.common.base.Preconditions.checkState;

/**
 * Stream for transferring TLV messages between cluster members.
 */
public class TLVMessageStream extends MessageStream<TLVMessage> {

    public static final int METADATA_LENGTH = 16; // 8 + 4 + 4

    private static final int LENGTH_OFFSET = 12;
    private static final long MARKER = 0xfeedcafecafefeedL;

    private DefaultControllerNode node;

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

    /**
     * Returns the node with which this stream is associated.
     *
     * @return controller node
     */
    DefaultControllerNode node() {
        return node;
    }

    /**
     * Sets the node with which this stream is affiliated.
     *
     * @param node controller node
     */
    void setNode(DefaultControllerNode node) {
        checkState(this.node == null, "Stream is already bound to a node");
        this.node = node;
    }

    @Override
    protected TLVMessage read(ByteBuffer buffer) {
        // Do we have enough bytes to read the header? If not, bail.
        if (buffer.remaining() < METADATA_LENGTH) {
            return null;
        }

        // Peek at the length and if we have enough to read the entire message
        // go ahead, otherwise bail.
        int length = buffer.getInt(buffer.position() + LENGTH_OFFSET);
        if (buffer.remaining() < length) {
            return null;
        }

        // At this point, we have enough data to read a complete message.
        long marker = buffer.getLong();
        checkState(marker == MARKER, "Incorrect message marker");

        int type = buffer.getInt();
        length = buffer.getInt();

        // TODO: add deserialization hook here
        byte[] data = new byte[length - METADATA_LENGTH];
        buffer.get(data);

        return new TLVMessage(type, data);
    }

    @Override
    protected void write(TLVMessage message, ByteBuffer buffer) {
        buffer.putLong(MARKER);
        buffer.putInt(message.type());
        buffer.putInt(message.length());

        // TODO: add serialization hook here
        buffer.put(message.data());
    }

}
