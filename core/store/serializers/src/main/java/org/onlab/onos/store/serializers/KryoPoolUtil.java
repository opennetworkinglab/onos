package org.onlab.onos.store.serializers;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.DefaultControllerNode;
import org.onlab.onos.cluster.MastershipTerm;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultAnnotations;
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
import org.onlab.onos.net.device.DefaultDeviceDescription;
import org.onlab.onos.net.device.DefaultPortDescription;
import org.onlab.onos.net.link.DefaultLinkDescription;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.Timestamp;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.util.KryoPool;

public final class KryoPoolUtil {

    /**
     * KryoPool which can serialize ON.lab misc classes.
     */
    public static final KryoPool MISC = KryoPool.newBuilder()
            .register(IpPrefix.class, new IpPrefixSerializer())
            .register(IpAddress.class, new IpAddressSerializer())
            .build();

    // TODO: Populate other classes
    /**
     * KryoPool which can serialize API bundle classes.
     */
    public static final KryoPool API = KryoPool.newBuilder()
            .register(MISC)
            .register(
                    //
                    ArrayList.class,
                    Arrays.asList().getClass(),
                    HashMap.class,
                    //
                    ControllerNode.State.class,
                    Device.Type.class,
                    DefaultAnnotations.class,
                    DefaultControllerNode.class,
                    DefaultDevice.class,
                    DefaultDeviceDescription.class,
                    DefaultLinkDescription.class,
                    MastershipRole.class,
                    Port.class,
                    DefaultPortDescription.class,
                    Element.class,
                    Link.Type.class,
                    Timestamp.class

                    )
            .register(URI.class, new URISerializer())
            .register(NodeId.class, new NodeIdSerializer())
            .register(ProviderId.class, new ProviderIdSerializer())
            .register(DeviceId.class, new DeviceIdSerializer())
            .register(PortNumber.class, new PortNumberSerializer())
            .register(DefaultPort.class, new DefaultPortSerializer())
            .register(LinkKey.class, new LinkKeySerializer())
            .register(ConnectPoint.class, new ConnectPointSerializer())
            .register(DefaultLink.class, new DefaultLinkSerializer())
            .register(MastershipTerm.class, new MastershipTermSerializer())
            .register(MastershipRole.class, new MastershipRoleSerializer())

            .build();


    // not to be instantiated
    private KryoPoolUtil() {}
}
