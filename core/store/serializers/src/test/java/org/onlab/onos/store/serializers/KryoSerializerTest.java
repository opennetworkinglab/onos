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
import org.onlab.onos.net.Annotations;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DefaultAnnotations;
import org.onlab.onos.net.DefaultDevice;
import org.onlab.onos.net.DefaultLink;
import org.onlab.onos.net.DefaultPort;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.LinkKey;
import org.onlab.onos.net.MastershipRole;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.SparseAnnotations;
import org.onlab.onos.net.device.DeviceMastershipTerm;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.util.KryoPool;

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
    private static final Device DEV1 = new DefaultDevice(PID, DID1, Device.Type.SWITCH, MFR, HW, SW1, SN);
    private static final SparseAnnotations A1 = DefaultAnnotations.builder()
            .set("A1", "a1")
            .set("B1", "b1")
            .build();
    private static final SparseAnnotations A1_2 = DefaultAnnotations.builder()
            .remove("A1")
            .set("B3", "b3")
            .build();

    private static KryoPool kryos;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        kryos = KryoPool.newBuilder()
                .register(KryoPoolUtil.API)
                .register(ImmutableMap.class, new ImmutableMapSerializer())
                .register(ImmutableSet.class, new ImmutableSetSerializer())
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
    public final void testSerialization() {
        testSerialized(new ConnectPoint(DID1, P1));
        testSerialized(new DefaultLink(PID, CP1, CP2, Link.Type.DIRECT));
        testSerialized(new DefaultPort(DEV1, P1, true));
        testSerialized(new DefaultLink(PID, CP1, CP2, Link.Type.DIRECT, A1));
        testSerialized(new DefaultPort(DEV1, P1, true, A1_2));
        testSerialized(DID1);
        testSerialized(ImmutableMap.of(DID1, DEV1, DID2, DEV1));
        testSerialized(ImmutableMap.of(DID1, DEV1));
        testSerialized(ImmutableMap.of());
        testSerialized(ImmutableSet.of(DID1, DID2));
        testSerialized(ImmutableSet.of(DID1));
        testSerialized(ImmutableSet.of());
        testSerialized(IpPrefix.valueOf("192.168.0.1/24"));
        testSerialized(IpAddress.valueOf("192.168.0.1"));
        testSerialized(new LinkKey(CP1, CP2));
        testSerialized(new NodeId("SomeNodeIdentifier"));
        testSerialized(P1);
        testSerialized(PID);
        testSerialized(PIDA);
        testSerialized(new NodeId("bar"));
        testSerialized(DeviceMastershipTerm.of(new NodeId("foo"), 2));
        for (MastershipRole role : MastershipRole.values()) {
            testSerialized(role);
        }
    }

    @Test
    public final void testAnnotations() {
        // Annotations does not have equals defined, manually test equality
        final byte[] a1Bytes = kryos.serialize(A1);
        SparseAnnotations copiedA1 = kryos.deserialize(a1Bytes);
        assertAnnotationsEquals(copiedA1, A1);

        final byte[] a12Bytes = kryos.serialize(A1_2);
        SparseAnnotations copiedA12 = kryos.deserialize(a12Bytes);
        assertAnnotationsEquals(copiedA12, A1_2);
    }

    // code clone
    public static void assertAnnotationsEquals(Annotations actual, SparseAnnotations... annotations) {
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
