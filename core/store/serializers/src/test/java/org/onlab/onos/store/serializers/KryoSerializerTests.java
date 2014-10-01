package org.onlab.onos.store.serializers;

import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.PortNumber.portNumber;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onlab.onos.cluster.MastershipTerm;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultDevice;
import org.onlab.onos.net.DefaultLink;
import org.onlab.onos.net.DefaultPort;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.LinkKey;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.packet.IpPrefix;
import org.onlab.util.KryoPool;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;

import de.javakaffee.kryoserializers.URISerializer;

public class KryoSerializerTests {
    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final DeviceId DID1 = deviceId("of:foo");
    private static final DeviceId DID2 = deviceId("of:bar");
    private static final PortNumber P1 = portNumber(1);
    private static final PortNumber P2 = portNumber(2);
    private static final ConnectPoint CP1 = new ConnectPoint(DID1, P1);
    private static final ConnectPoint CP2 = new ConnectPoint(DID2, P2);
    private static final String MFR = "whitebox";
    private static final String HW = "1.1.x";
    private static final String SW1 = "3.8.1";
    private static final String SW2 = "3.9.5";
    private static final String SN = "43311-12345";
    private static final Device DEV1 = new DefaultDevice(PID, DID1, Device.Type.SWITCH, MFR, HW, SW1, SN);

    private static KryoPool kryos;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        kryos = KryoPool.newBuilder()
                .register(
                        ArrayList.class,
                        HashMap.class
                        )
                .register(
                        Device.Type.class,
                        Link.Type.class

//                      ControllerNode.State.class,
//                        DefaultControllerNode.class,
//                        MastershipRole.class,
//                        Port.class,
//                        Element.class,
                        )
                .register(ConnectPoint.class, new ConnectPointSerializer())
                .register(DefaultLink.class, new DefaultLinkSerializer())
                .register(DefaultPort.class, new DefaultPortSerializer())
                .register(DeviceId.class, new DeviceIdSerializer())
                .register(ImmutableMap.class, new ImmutableMapSerializer())
                .register(ImmutableSet.class, new ImmutableSetSerializer())
                .register(IpPrefix.class, new IpPrefixSerializer())
                .register(LinkKey.class, new LinkKeySerializer())
                .register(NodeId.class, new NodeIdSerializer())
                .register(PortNumber.class, new PortNumberSerializer())
                .register(ProviderId.class, new ProviderIdSerializer())

                .register(DefaultDevice.class)

                .register(URI.class, new URISerializer())

                .register(MastershipRole.class, new MastershipRoleSerializer())
                .register(MastershipTerm.class, new MastershipTermSerializer())
                .build();
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
        // removing Kryo instance to use fresh Kryo on each tests
        kryos.getKryo();
    }

    private static <T> void testSerialized(T original) {
        ByteBuffer buffer = ByteBuffer.allocate(1 * 1024 * 1024);
        kryos.serialize(original, buffer);
        buffer.flip();
        T copy = kryos.deserialize(buffer);

        new EqualsTester()
            .addEqualityGroup(original, copy)
            .testEquals();
    }


    @Test
    public final void test() {
        testSerialized(new ConnectPoint(DID1, P1));
        testSerialized(new DefaultLink(PID, CP1, CP2, Link.Type.DIRECT));
        testSerialized(new DefaultPort(DEV1, P1, true));
        testSerialized(DID1);
        testSerialized(ImmutableMap.of(DID1, DEV1, DID2, DEV1));
        testSerialized(ImmutableMap.of(DID1, DEV1));
        testSerialized(ImmutableMap.of());
        testSerialized(ImmutableSet.of(DID1, DID2));
        testSerialized(ImmutableSet.of(DID1));
        testSerialized(ImmutableSet.of());
        testSerialized(IpPrefix.valueOf("192.168.0.1/24"));
        testSerialized(new LinkKey(CP1, CP2));
        testSerialized(new NodeId("SomeNodeIdentifier"));
        testSerialized(P1);
        testSerialized(PID);
    }

}
