package org.onlab.onos.store.cluster.impl;

import de.javakaffee.kryoserializers.URISerializer;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.DefaultControllerNode;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultDevice;
import org.onlab.onos.net.DefaultLink;
import org.onlab.onos.net.DefaultPort;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Element;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.LinkKey;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.cluster.messaging.ClusterMessage;
import org.onlab.onos.store.cluster.messaging.EchoMessage;
import org.onlab.onos.store.cluster.messaging.GoodbyeMessage;
import org.onlab.onos.store.cluster.messaging.HelloMessage;
import org.onlab.onos.store.cluster.messaging.MessageSubject;
import org.onlab.onos.store.cluster.messaging.SerializationService;
import org.onlab.onos.store.serializers.ConnectPointSerializer;
import org.onlab.onos.store.serializers.DefaultLinkSerializer;
import org.onlab.onos.store.serializers.DefaultPortSerializer;
import org.onlab.onos.store.serializers.DeviceIdSerializer;
import org.onlab.onos.store.serializers.IpPrefixSerializer;
import org.onlab.onos.store.serializers.LinkKeySerializer;
import org.onlab.onos.store.serializers.NodeIdSerializer;
import org.onlab.onos.store.serializers.PortNumberSerializer;
import org.onlab.onos.store.serializers.ProviderIdSerializer;
import org.onlab.packet.IpPrefix;
import org.onlab.util.KryoPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import static com.google.common.base.Preconditions.checkState;

/**
 * Factory for parsing messages sent between cluster members.
 */
@Component(immediate = true)
@Service
public class MessageSerializer implements SerializationService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final int METADATA_LENGTH = 12; // 8 + 4
    private static final int LENGTH_OFFSET = 8;

    private static final long MARKER = 0xfeedcafebeaddeadL;

    private KryoPool serializerPool;

    @Activate
    public void activate() {
        setupKryoPool();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    /**
     * Sets up the common serialzers pool.
     */
    protected void setupKryoPool() {
        // FIXME Slice out types used in common to separate pool/namespace.
        serializerPool = KryoPool.newBuilder()
                .register(ArrayList.class,
                          HashMap.class,

                          ControllerNode.State.class,
                          Device.Type.class,

                          DefaultControllerNode.class,
                          DefaultDevice.class,
                          MastershipRole.class,
                          Port.class,
                          Element.class,

                          Link.Type.class,

                          MessageSubject.class,
                          HelloMessage.class,
                          GoodbyeMessage.class,
                          EchoMessage.class
                )
                .register(IpPrefix.class, new IpPrefixSerializer())
                .register(URI.class, new URISerializer())
                .register(NodeId.class, new NodeIdSerializer())
                .register(ProviderId.class, new ProviderIdSerializer())
                .register(DeviceId.class, new DeviceIdSerializer())
                .register(PortNumber.class, new PortNumberSerializer())
                .register(DefaultPort.class, new DefaultPortSerializer())
                .register(LinkKey.class, new LinkKeySerializer())
                .register(ConnectPoint.class, new ConnectPointSerializer())
                .register(DefaultLink.class, new DefaultLinkSerializer())
                .build()
                .populate(1);
    }


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
            length = buffer.getInt();

            // TODO: sanity checking for length
            byte[] data = new byte[length - METADATA_LENGTH];
            buffer.get(data);
            return (ClusterMessage) serializerPool.deserialize(data);

        } catch (Exception e) {
            // TODO: recover from exceptions by forwarding stream to next marker
            log.warn("Unable to decode message due to: " + e);
        }
        return null;
    }

    @Override
    public void encode(ClusterMessage message, ByteBuffer buffer) {
        try {
            byte[] data = serializerPool.serialize(message);
            buffer.putLong(MARKER);
            buffer.putInt(data.length + METADATA_LENGTH);
            buffer.put(data);

        } catch (Exception e) {
            // TODO: recover from exceptions by forwarding stream to next marker
            log.warn("Unable to encode message due to: " + e);
        }
    }

}
