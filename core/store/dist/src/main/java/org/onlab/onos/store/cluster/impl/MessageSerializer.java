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
import java.util.ArrayList;
import java.util.HashMap;

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

                          MessageSubject.class
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
    public Object decode(byte[] data) {
        return serializerPool.deserialize(data);
    }

    @Override
    public byte[] encode(Object payload) {
        return serializerPool.serialize(payload);
    }
}
