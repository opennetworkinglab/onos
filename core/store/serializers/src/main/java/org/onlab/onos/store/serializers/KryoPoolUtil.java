package org.onlab.onos.store.serializers;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.DefaultControllerNode;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.mastership.MastershipTerm;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultAnnotations;
import org.onlab.onos.net.DefaultDevice;
import org.onlab.onos.net.DefaultLink;
import org.onlab.onos.net.DefaultPort;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Element;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.HostLocation;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.LinkKey;
import org.onlab.onos.net.Port;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.device.DefaultDeviceDescription;
import org.onlab.onos.net.device.DefaultPortDescription;
import org.onlab.onos.net.host.DefaultHostDescription;
import org.onlab.onos.net.host.HostDescription;
import org.onlab.onos.net.link.DefaultLinkDescription;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.Timestamp;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoPool;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public final class KryoPoolUtil {

    /**
     * KryoPool which can serialize ON.lab misc classes.
     */
    public static final KryoPool MISC = KryoPool.newBuilder()
            .register(IpPrefix.class, new IpPrefixSerializer())
            .register(IpAddress.class, new IpAddressSerializer())
            .register(MacAddress.class, new MacAddressSerializer())
            .register(VlanId.class)
            .build();

    // TODO: Populate other classes
    /**
     * KryoPool which can serialize API bundle classes.
     */
    public static final KryoPool API = KryoPool.newBuilder()
            .register(MISC)
            .register(ImmutableMap.class, new ImmutableMapSerializer())
            .register(ImmutableList.class, new ImmutableListSerializer())
            .register(ImmutableSet.class, new ImmutableSetSerializer())
            .register(
                    //
                    ArrayList.class,
                    Arrays.asList().getClass(),
                    HashMap.class,
                    //
                    //
                    ControllerNode.State.class,
                    Device.Type.class,
                    DefaultAnnotations.class,
                    DefaultControllerNode.class,
                    DefaultDevice.class,
                    DefaultDeviceDescription.class,
                    DefaultLinkDescription.class,
                    Port.class,
                    DefaultPortDescription.class,
                    Element.class,
                    Link.Type.class,
                    Timestamp.class,
                    HostId.class,
                    HostDescription.class,
                    DefaultHostDescription.class
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
            .register(HostLocation.class, new HostLocationSerializer())

            .build();


    // not to be instantiated
    private KryoPoolUtil() {}
}
