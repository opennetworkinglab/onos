package org.onlab.onos.store.cluster.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.store.cluster.messaging.ClusterMessage;
import org.onlab.onos.store.cluster.messaging.HelloMessage;
import org.onlab.onos.store.cluster.messaging.MessageSubject;
import org.onlab.onos.store.cluster.messaging.SerializationService;
import org.onlab.packet.IpPrefix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

import static com.google.common.base.Preconditions.checkState;

/**
 * Factory for parsing messages sent between cluster members.
 */
@Component(immediate = true)
@Service
public class MessageSerializer implements SerializationService {

    private final Logger log = LoggerFactory.getLogger(getClass());

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
            String[] fields = new String(data).split(":");
            return new HelloMessage(new NodeId(fields[0]), IpPrefix.valueOf(fields[1]), Integer.parseInt(fields[2]));

        } catch (Exception e) {
            // TODO: recover from exceptions by forwarding stream to next marker
            log.warn("Unable to decode message due to: " + e);
        }
        return null;
    }

    @Override
    public void encode(ClusterMessage message, ByteBuffer buffer) {
        try {
            HelloMessage helloMessage = (HelloMessage) message;
            buffer.putLong(MARKER);
            buffer.putInt(message.subject().ordinal());

            String str = helloMessage.nodeId() + ":" + helloMessage.ipAddress() + ":" + helloMessage.tcpPort();
            byte[] data = str.getBytes();
            buffer.putInt(data.length + METADATA_LENGTH);
            buffer.put(data);

        } catch (Exception e) {
            // TODO: recover from exceptions by forwarding stream to next marker
            log.warn("Unable to encode message due to: " + e);
        }
    }

}
