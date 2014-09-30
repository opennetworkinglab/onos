package org.onlab.onos.store.cluster.messaging;

import org.onlab.nio.IOLoop;
import org.onlab.nio.MessageStream;
import org.onlab.onos.cluster.DefaultControllerNode;

import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

import static com.google.common.base.Preconditions.checkState;

/**
 * Stream for transferring messages between two cluster members.
 */
public class ClusterMessageStream extends MessageStream<ClusterMessage> {

    private static final int COMM_BUFFER_SIZE = 32 * 1024;
    private static final int COMM_IDLE_TIME = 500;

    private DefaultControllerNode node;
    private SerializationService serializationService;

    /**
     * Creates a message stream associated with the specified IO loop and
     * backed by the given byte channel.
     *
     * @param serializationService service for encoding/decoding messages
     * @param loop                 IO loop
     * @param byteChannel          backing byte channel
     */
    public ClusterMessageStream(SerializationService serializationService,
                                IOLoop<ClusterMessage, ?> loop,
                                ByteChannel byteChannel) {
        super(loop, byteChannel, COMM_BUFFER_SIZE, COMM_IDLE_TIME);
        this.serializationService = serializationService;
    }

    /**
     * Returns the node with which this stream is associated.
     *
     * @return controller node
     */
    public DefaultControllerNode node() {
        return node;
    }

    /**
     * Sets the node with which this stream is affiliated.
     *
     * @param node controller node
     */
    public void setNode(DefaultControllerNode node) {
        checkState(this.node == null, "Stream is already bound to a node");
        this.node = node;
    }

    @Override
    protected ClusterMessage read(ByteBuffer buffer) {
        return serializationService.decode(buffer);
    }

    @Override
    protected void write(ClusterMessage message, ByteBuffer buffer) {
        serializationService.encode(message, buffer);
    }

}
