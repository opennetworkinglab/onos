package org.onlab.onos.store.serializers;

import static org.junit.Assert.assertEquals;
import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.PortNumber.portNumber;

import java.nio.ByteBuffer;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.mastership.MastershipTerm;
import org.onlab.onos.net.Annotations;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultAnnotations;
import org.onlab.onos.net.DefaultDevice;
import org.onlab.onos.net.DefaultLink;
import org.onlab.onos.net.DefaultPort;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.HostLocation;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.LinkKey;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.SparseAnnotations;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.packet.ChassisId;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;

public class KryoSerializerTest {

    private static final ProviderId PID = new ProviderId("of", "foo");
    private static final ProviderId PIDA = new ProviderId("of", "foo", true);
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
    private static final ChassisId CID = new ChassisId();
    private static final Device DEV1 = new DefaultDevice(PID, DID1, Device.Type.SWITCH, MFR, HW,
                                                         SW1, SN, CID);
    private static final SparseAnnotations A1 = DefaultAnnotations.builder()
            .set("A1", "a1")
            .set("B1", "b1")
            .build();
    private static final SparseAnnotations A1_2 = DefaultAnnotations.builder()
            .remove("A1")
            .set("B3", "b3")
            .build();

    private KryoSerializer serializer;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        serializer = new KryoSerializer() {

            @Override
            protected void setupKryoPool() {
                serializerPool = KryoNamespace.newBuilder()
                        .register(KryoNamespaces.API)
                        .build()
                        .populate(1);
            }
        };
    }

    @After
    public void tearDown() throws Exception {
    }

    private <T> void testSerialized(T original) {
        ByteBuffer buffer = ByteBuffer.allocate(1 * 1024 * 1024);
        serializer.encode(original, buffer);
        buffer.flip();
        T copy = serializer.decode(buffer);

        T copy2 = serializer.decode(serializer.encode(original));

        new EqualsTester()
            .addEqualityGroup(original, copy, copy2)
            .testEquals();
    }


    @Test
    public void testConnectPoint() {
        testSerialized(new ConnectPoint(DID1, P1));
    }

    @Test
    public void testDefaultLink() {
        testSerialized(new DefaultLink(PID, CP1, CP2, Link.Type.DIRECT));
        testSerialized(new DefaultLink(PID, CP1, CP2, Link.Type.DIRECT, A1));
    }

    @Test
    public void testDefaultPort() {
        testSerialized(new DefaultPort(DEV1, P1, true));
        testSerialized(new DefaultPort(DEV1, P1, true, A1_2));
    }

    @Test
    public void testDeviceId() {
        testSerialized(DID1);
    }

    @Test
    public void testImmutableMap() {
        testSerialized(ImmutableMap.of(DID1, DEV1, DID2, DEV1));
        testSerialized(ImmutableMap.of(DID1, DEV1));
        testSerialized(ImmutableMap.of());
    }

    @Test
    public void testImmutableSet() {
        testSerialized(ImmutableSet.of(DID1, DID2));
        testSerialized(ImmutableSet.of(DID1));
        testSerialized(ImmutableSet.of());
    }

    @Test
    public void testImmutableList() {
        testSerialized(ImmutableList.of(DID1, DID2));
        testSerialized(ImmutableList.of(DID1));
        testSerialized(ImmutableList.of());
    }

    @Test
    public void testIpPrefix() {
        testSerialized(IpPrefix.valueOf("192.168.0.1/24"));
    }

    @Test
    public void testIpAddress() {
        testSerialized(IpAddress.valueOf("192.168.0.1"));
    }

    @Test
    public void testMacAddress() {
        testSerialized(MacAddress.valueOf("12:34:56:78:90:ab"));
    }

    @Test
    public void testLinkKey() {
        testSerialized(LinkKey.linkKey(CP1, CP2));
    }

    @Test
    public void testNodeId() {
        testSerialized(new NodeId("SomeNodeIdentifier"));
    }

    @Test
    public void testPortNumber() {
        testSerialized(P1);
    }

    @Test
    public void testProviderId() {
        testSerialized(PID);
        testSerialized(PIDA);
    }

    @Test
    public void testMastershipTerm() {
        testSerialized(MastershipTerm.of(new NodeId("foo"), 2));
    }

    @Test
    public void testHostLocation() {
        testSerialized(new HostLocation(CP1, 1234L));
    }

    @Test
    public void testAnnotations() {
        // Annotations does not have equals defined, manually test equality
        final byte[] a1Bytes = serializer.encode(A1);
        SparseAnnotations copiedA1 = serializer.decode(a1Bytes);
        assertAnnotationsEquals(copiedA1, A1);

        final byte[] a12Bytes = serializer.encode(A1_2);
        SparseAnnotations copiedA12 = serializer.decode(a12Bytes);
        assertAnnotationsEquals(copiedA12, A1_2);
    }

    // code clone
    protected static void assertAnnotationsEquals(Annotations actual, SparseAnnotations... annotations) {
        SparseAnnotations expected = DefaultAnnotations.builder().build();
        for (SparseAnnotations a : annotations) {
            expected = DefaultAnnotations.union(expected, a);
        }
        assertEquals(expected.keys(), actual.keys());
        for (String key : expected.keys()) {
            assertEquals(expected.value(key), actual.value(key));
        }
    }

}
