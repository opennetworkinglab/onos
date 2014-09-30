package org.onlab.onos.store.cluster.messaging.impl;

import org.onlab.onos.store.cluster.messaging.ClusterMessage;
import org.onlab.onos.store.cluster.messaging.MessageSubject;
import org.onlab.onos.store.cluster.messaging.SerializationService;

import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.checkState;

/**
 * Factory for parsing messages sent between cluster members.
 */
public class MessageSerializer implements SerializationService {

    private static final int METADATA_LENGTH = 16; // 8 + 4 + 4
    private static final int LENGTH_OFFSET = 12;

    private static final long MARKER = 0xfeedcafebeaddeadL;

    @Override
    public ClusterMessage decode(ByteBuffer buffer) {
        try {
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

            int subjectOrdinal = buffer.getInt();
            MessageSubject subject = MessageSubject.values()[subjectOrdinal];
            length = buffer.getInt();

            // TODO: sanity checking for length
            byte[] data = new byte[length - METADATA_LENGTH];
            buffer.get(data);

            // TODO: add deserialization hook here; for now this hack
            return null; // actually deserialize

        } catch (Exception e) {
            // TODO: recover from exceptions by forwarding stream to next marker
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void encode(ClusterMessage message, ByteBuffer buffer) {
        try {
            int i = 0;
            // Type based lookup for proper encoder
        } catch (Exception e) {
            // TODO: recover from exceptions by forwarding stream to next marker
            e.printStackTrace();
        }
    }

}
