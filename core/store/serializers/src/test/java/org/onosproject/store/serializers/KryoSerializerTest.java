/*
 * Copyright 2014-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.store.serializers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onlab.packet.VlanId;
import org.onlab.util.Bandwidth;
import org.onlab.util.Frequency;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.GroupId;
import org.onosproject.mastership.MastershipTerm;
import org.onosproject.net.Annotations;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.GridType;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.MarkerResource;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.oldbatch.FlowRuleBatchEntry;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceConsumerId;
import org.onosproject.net.resource.Resources;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.intent.constraint.AnnotationConstraint;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.intent.constraint.LatencyConstraint;
import org.onosproject.net.intent.constraint.LinkTypeConstraint;
import org.onosproject.net.intent.constraint.ObstacleConstraint;
import org.onosproject.net.intent.constraint.WaypointConstraint;
import org.onlab.packet.ChassisId;
import org.onlab.packet.IpAddress;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ip6Prefix;
import org.onlab.packet.MacAddress;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.time.Duration;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;

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
    private static final VlanId VLAN1 = VlanId.vlanId((short) 100);

    private StoreSerializer serializer;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        serializer = StoreSerializer.using(KryoNamespaces.API);
    }

    @After
    public void tearDown() throws Exception {
    }

    private byte[] serialize(Object object) {
        return serialize(object, serializer);
    }

    private byte[] serialize(Object object, StoreSerializer serializer) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        serializer.encode(object, buffer);
        buffer.flip();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    private <T> T deserialize(byte[] bytes, StoreSerializer serializer) {
        return serializer.decode(bytes);
    }

    private <T> void testBytesEqual(T expected, T actual) {
        byte[] expectedBytes = serialize(expected);
        byte[] actualBytes = serialize(actual);
        assertArrayEquals(expectedBytes, actualBytes);
    }

    private <T> void testSerializedEquals(T original) {
        ByteBuffer buffer = ByteBuffer.allocate(1 * 1024 * 1024);
        serializer.encode(original, buffer);
        buffer.flip();
        T copy = serializer.decode(buffer);

        T copy2 = serializer.decode(serializer.encode(original));

        new EqualsTester()
            .addEqualityGroup(original, copy, copy2)
            .testEquals();
    }

    private <T> void testSerializable(T original) {
        byte[] bs = serializer.encode(original);
        T copy = serializer.decode(bs);
        assertNotNull(copy);
    }

    public static class Versioned1 {
        private int value1;
    }

    public static class Versioned2 {
        private int value1;
        private int value2;
        private int value3;
    }

    public static class Versioned3 {
        private int value1;
        private int value3;
    }

    @Test
    public void testVersioned() {
        StoreSerializer serializer1 = StoreSerializer.using(KryoNamespace.newBuilder()
                .register(KryoNamespaces.BASIC)
                .register(Versioned1.class)
                .setCompatible(true)
                .build());

        StoreSerializer serializer2 = StoreSerializer.using(KryoNamespace.newBuilder()
                .register(KryoNamespaces.BASIC)
                .register(Versioned2.class)
                .setCompatible(true)
                .build());

        StoreSerializer serializer3 = StoreSerializer.using(KryoNamespace.newBuilder()
                .register(KryoNamespaces.BASIC)
                .register(Versioned3.class)
                .setCompatible(true)
                .build());

        Versioned1 versioned1 = new Versioned1();
        versioned1.value1 = 1;

        Versioned2 versioned2 = new Versioned2();
        versioned2.value1 = 1;
        versioned2.value2 = 2;
        versioned2.value3 = 3;

        Versioned3 versioned3 = new Versioned3();
        versioned3.value1 = 1;
        versioned3.value3 = 3;

        Versioned2 versioned1Upgrade = deserialize(serialize(versioned1, serializer1), serializer2);
        assertEquals(versioned1.value1, versioned1Upgrade.value1);

        Versioned1 versioned2Downgrade = deserialize(serialize(versioned2, serializer2), serializer1);
        assertEquals(versioned2.value1, versioned2Downgrade.value1);

        Versioned3 versioned2Upgrade = deserialize(serialize(versioned2, serializer2), serializer3);
        assertEquals(versioned2.value1, versioned2Upgrade.value1);
        assertEquals(versioned2.value3, versioned2Upgrade.value3);

        Versioned2 versioned3Downgrade = deserialize(serialize(versioned3, serializer3), serializer2);
        assertEquals(versioned3.value1, versioned3Downgrade.value1);
    }

    @Test
    public void testConnectPoint() {
        testSerializedEquals(new ConnectPoint(DID1, P1));
    }

    @Test
    public void testDefaultLink() {
        testSerializedEquals(DefaultLink.builder()
                                     .providerId(PID)
                                     .src(CP1)
                                     .dst(CP2)
                                     .type(Link.Type.DIRECT)
                                     .build());
        testSerializedEquals(DefaultLink.builder()
                                     .providerId(PID)
                                     .src(CP1)
                                     .dst(CP2)
                                     .type(Link.Type.DIRECT)
                                     .annotations(A1)
                                     .build());
    }

    @Test
    public void testDefaultPort() {
        testSerializedEquals(new DefaultPort(DEV1, P1, true));
        testSerializedEquals(new DefaultPort(DEV1, P1, true, A1_2));
    }

    @Test
    public void testDeviceId() {
        testSerializedEquals(DID1);
    }

    @Test
    public void testImmutableMap() {
        testSerializedEquals(ImmutableMap.of(DID1, DEV1, DID2, DEV1));
        testSerializedEquals(ImmutableMap.of(DID1, DEV1));
        testSerializedEquals(ImmutableMap.of());
    }

    @Test
    public void testImmutableSet() {
        testSerializedEquals(ImmutableSet.of(DID1, DID2));
        testSerializedEquals(ImmutableSet.of(DID1));
        testSerializedEquals(ImmutableSet.of());
    }

    @Test
    public void testImmutableList() {
        testBytesEqual(ImmutableList.of(DID1, DID2), ImmutableList.of(DID1, DID2, DID1, DID2).subList(0, 2));
        testSerializedEquals(ImmutableList.of(DID1, DID2));
        testSerializedEquals(ImmutableList.of(DID1));
        testSerializedEquals(ImmutableList.of());
    }

    @Test
    public void testFlowRuleBatchEntry() {
        final FlowRule rule1 =
                DefaultFlowRule.builder()
                        .forDevice(DID1)
                        .withSelector(DefaultTrafficSelector.emptySelector())
                        .withTreatment(DefaultTrafficTreatment.emptyTreatment())
                        .withPriority(0)
                        .fromApp(new DefaultApplicationId(1, "1"))
                        .makeTemporary(1)
                        .build();

        final FlowRuleBatchEntry entry1 =
                new FlowRuleBatchEntry(FlowRuleBatchEntry.FlowRuleOperation.ADD, rule1);
        final FlowRuleBatchEntry entry2 =
                new FlowRuleBatchEntry(FlowRuleBatchEntry.FlowRuleOperation.ADD, rule1, 100L);

        testSerializedEquals(entry1);
        testSerializedEquals(entry2);
    }

    @Test
    public void testIpPrefix() {
        testSerializedEquals(IpPrefix.valueOf("192.168.0.1/24"));
    }

    @Test
    public void testIp4Prefix() {
        testSerializedEquals(Ip4Prefix.valueOf("192.168.0.1/24"));
    }

    @Test
    public void testIp6Prefix() {
        testSerializedEquals(Ip6Prefix.valueOf("1111:2222::/120"));
    }

    @Test
    public void testIpAddress() {
        testSerializedEquals(IpAddress.valueOf("192.168.0.1"));
    }

    @Test
    public void testIp4Address() {
        testSerializedEquals(Ip4Address.valueOf("192.168.0.1"));
    }

    @Test
    public void testIp6Address() {
        testSerializedEquals(Ip6Address.valueOf("1111:2222::"));
    }

    @Test
    public void testMacAddress() {
        testSerializedEquals(MacAddress.valueOf("12:34:56:78:90:ab"));
    }

    @Test
    public void testLinkKey() {
        testSerializedEquals(LinkKey.linkKey(CP1, CP2));
    }

    @Test
    public void testNodeId() {
        testSerializedEquals(new NodeId("SomeNodeIdentifier"));
    }

    @Test
    public void testPortNumber() {
        testSerializedEquals(P1);
    }

    @Test
    public void testProviderId() {
        testSerializedEquals(PID);
        testSerializedEquals(PIDA);
    }

    @Test
    public void testMastershipTerm() {
        testSerializedEquals(MastershipTerm.of(new NodeId("foo"), 2));
        testSerializedEquals(MastershipTerm.of(null, 0));
    }

    @Test
    public void testHostLocation() {
        testSerializedEquals(new HostLocation(CP1, 1234L));
    }

    @Test
    public void testFlowId() {
        testSerializedEquals(FlowId.valueOf(0x12345678L));
    }

    @Test
    public void testRoleInfo() {
        testSerializedEquals(new RoleInfo(new NodeId("master"),
                            asList(new NodeId("stby1"), new NodeId("stby2"))));
    }

    @Test
    public void testOchSignal() {
        testSerializedEquals(org.onosproject.net.Lambda.ochSignal(
                GridType.DWDM, ChannelSpacing.CHL_100GHZ, 1, 1
        ));
    }

    @Test
    public void testResource() {
        testSerializedEquals(Resources.discrete(DID1, P1, VLAN1).resource());
    }

    @Test
    public void testResourceId() {
        testSerializedEquals(Resources.discrete(DID1, P1).id());
    }

    @Test
    public void testResourceAllocation() {
        testSerializedEquals(new ResourceAllocation(
                Resources.discrete(DID1, P1, VLAN1).resource(),
                ResourceConsumerId.of(30L, IntentId.class)));
    }

    @Test
    public void testFrequency() {
        testSerializedEquals(Frequency.ofGHz(100));
    }

    @Test
    public void testBandwidth() {
        testSerializedEquals(Bandwidth.mbps(1000));
        testSerializedEquals(Bandwidth.mbps(1000.0));
    }

    @Test
    public void testBandwidthConstraint() {
        testSerializable(new BandwidthConstraint(Bandwidth.bps(1000.0)));
    }

    @Test
    public void testLinkTypeConstraint() {
        testSerializable(new LinkTypeConstraint(true, Link.Type.DIRECT));
    }

    @Test
    public void testLatencyConstraint() {
        testSerializable(new LatencyConstraint(Duration.ofSeconds(10)));
    }

    @Test
    public void testWaypointConstraint() {
        testSerializable(new WaypointConstraint(deviceId("of:1"), deviceId("of:2")));
    }

    @Test
    public void testObstacleConstraint() {
        testSerializable(new ObstacleConstraint(deviceId("of:1"), deviceId("of:2")));
    }

    @Test
    public void testArraysAsList() {
        testSerializedEquals(Arrays.asList(1, 2, 3));
    }

    @Test
    public void testAnnotationConstraint() {
        testSerializable(new AnnotationConstraint("distance", 100.0));
    }

    @Test
    public void testGroupId() {
        testSerializedEquals(new GroupId(99));
    }

    @Test
    public void testEmptySet() {
        testSerializedEquals(Collections.emptySet());
    }

    @Test
    public void testMarkerResource() {
        testSerializedEquals(MarkerResource.marker("testString"));
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
    private static void assertAnnotationsEquals(Annotations actual, SparseAnnotations... annotations) {
        SparseAnnotations expected = DefaultAnnotations.builder().build();
        for (SparseAnnotations a : annotations) {
            expected = DefaultAnnotations.union(expected, a);
        }
        assertEquals(expected.keys(), actual.keys());
        for (String key : expected.keys()) {
            assertEquals(expected.value(key), actual.value(key));
        }
    }

    @Test
    public void testBitSet() {
        BitSet bs = new BitSet(32);
        bs.set(2);
        bs.set(8);
        bs.set(12);
        bs.set(18);
        bs.set(25);
        bs.set(511);

        testSerializedEquals(bs);
    }

}
