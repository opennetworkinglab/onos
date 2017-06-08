package org.onosproject.utils;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onlab.packet.Ip4Address;

import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.*;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.virtual.*;
import org.onosproject.net.*;

import org.onosproject.net.flow.*;
import org.onosproject.net.group.*;
import org.onosproject.net.intent.IntentTestsMocks;
import org.onosproject.net.key.DeviceKey;
import org.onosproject.net.key.DeviceKeyId;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.region.DefaultRegion;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionId;
import org.onosproject.net.statistic.DefaultLoad;
import org.onosproject.net.statistic.FlowEntryWithLoad;
import org.onosproject.net.statistic.Load;
import org.onosproject.net.topology.*;
import org.onosproject.ui.model.topo.UiTopoLayout;
import org.onosproject.ui.model.topo.UiTopoLayoutId;
import org.onosproject.net.group.DefaultGroupTest;


import java.util.Optional;


import static org.junit.Assert.*;

import static org.onosproject.app.DefaultApplicationDescriptionTest.*;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.did;
import static org.onosproject.net.topology.ClusterId.clusterId;
import static org.onosproject.utils.Comparators.APP_COMPARATOR;
import static org.onosproject.utils.Comparators.APP_ID_COMPARATOR;
import static org.onosproject.utils.Comparators.CLUSTER_COMPARATOR;
import static org.onosproject.utils.Comparators.DEVICE_KEY_COMPARATOR;
import static org.onosproject.utils.Comparators.ELEMENT_COMPARATOR;
import static org.onosproject.utils.Comparators.ELEMENT_ID_COMPARATOR;
import static org.onosproject.utils.Comparators.FLOWENTRY_WITHLOAD_COMPARATOR;
import static org.onosproject.utils.Comparators.FLOW_RULE_COMPARATOR;
import static org.onosproject.utils.Comparators.GROUP_COMPARATOR;
import static org.onosproject.utils.Comparators.INTERFACES_COMPARATOR;
import static org.onosproject.utils.Comparators.LAYOUT_COMPARATOR;
import static org.onosproject.utils.Comparators.NODE_COMPARATOR;
import static org.onosproject.utils.Comparators.PORT_COMPARATOR;
import static org.onosproject.utils.Comparators.REGION_COMPARATOR;
import static org.onosproject.utils.Comparators.TENANT_ID_COMPARATOR;
import static org.onosproject.utils.Comparators.VIRTUAL_DEVICE_COMPARATOR;
import static org.onosproject.utils.Comparators.VIRTUAL_NETWORK_COMPARATOR;
import static org.onosproject.utils.Comparators.VIRTUAL_PORT_COMPARATOR;


public class ComparatorsTest {
    private final ProviderId PID = new ProviderId("of", "foo");
    private final DeviceId DID = deviceId("of:foo");
    private final String MFR = "whitebox";
    private final String HW = "1.1.x";
    private final String HW1 = "2.2.x";
    private final String SW = "3.9.1";
    private final String SW1 = "4.0.0";
    private final String SN = "43311-12345";
    private final ChassisId CID = new ChassisId();
    private final ConnectPoint cp =
            new ConnectPoint(DeviceId.deviceId("of:00001"), PortNumber.portNumber(100));
    private final GroupBucket testBucket =
            DefaultGroupBucket.createSelectGroupBucket(
                    DefaultTrafficTreatment.emptyTreatment());
    private final GroupBuckets groupBuckets =
            new GroupBuckets(ImmutableList.of(testBucket));
    private final GroupDescription groupDesc1 =
            new DefaultGroupDescription(did("1"),
                    GroupDescription.Type.ALL,
                    groupBuckets);
    private final DefaultFlowEntry fEntry = new DefaultFlowEntry(fRule(10, 10),
            FlowEntry.FlowEntryState.ADDED, 5, 5, 5);
    Ip4Address ipAddress;

    @Test
    public void testAppIdComparator() {
        assertEquals(0, APP_ID_COMPARATOR.compare(appID(1, "a"), appID(1, "a")));
        assertTrue(APP_ID_COMPARATOR.compare(appID(2, "a"), appID(0, "a")) > 0);
        assertTrue(APP_ID_COMPARATOR.compare(appID(1, "b"), appID(3, "x")) < 0);
    }

    private ApplicationId appID(int id, String name) {
        return new DefaultApplicationId(id, name);
    }

    @Test
    public void testAppComparator() {
        assertEquals(0, APP_COMPARATOR.compare(app(1, "foo"), app(1, "foo")));
        assertEquals(0, (APP_COMPARATOR.compare(app(2, "foo"), app(2, "bar"))));
        assertNotEquals(0, APP_COMPARATOR.compare(app(1, "foo"), app(2, "foo")));
        assertNotEquals(0, APP_COMPARATOR.compare(app(1, "bar"), app(2, "foo")));
    }

    private Application app(int id, String name) {
        return new DefaultApplication(new DefaultApplicationId(id, name), VER, TITLE, DESC, ORIGIN,
                CATEGORY, URL, README, ICON, ROLE,
                PERMS, Optional.of(FURL), FEATURES, APPS);
    }

    @Test
    public void testElementIdComparator() {
        ElementId elementid1 = new ElementId() {
        };
        ElementId elementid2 = elementid1;
        ElementId elementid3 = new ElementId() {
        };
        while (elementid1 == elementid3) {
            elementid3 = new ElementId() {
            };
        }
        assertTrue(0 == ELEMENT_ID_COMPARATOR.compare(elementid1, elementid2));
        assertFalse(0 == ELEMENT_ID_COMPARATOR.compare(elementid3, elementid1));
    }

    @Test
    public void testFlowRuleComparator() {
        assertEquals(0, FLOW_RULE_COMPARATOR.compare(fRule(100, 10), fRule(100, 10)));
        assertEquals(-8, FLOW_RULE_COMPARATOR.compare(fRule(100, 10), fRule(100, 2)));
        assertEquals(90, FLOW_RULE_COMPARATOR.compare(fRule(100, 10), fRule(10, 10)));
        assertEquals(20, FLOW_RULE_COMPARATOR.compare(fRule(40, 10), fRule(20, 2)));

    }

    private FlowRule fRule(int tableID, int priority) {
        return DefaultFlowRule.builder()
                .forDevice(did("id" + Integer.toString(10)))
                .withPriority(priority)
                .forTable(tableID)
                .fromApp(APP_ID)
                .makeTemporary(44)
                .build();
    }

    @Test
    public void testFlowEntryWithLoad() {
        //Rate = (current-previous)/interval
        assertEquals(0, FLOWENTRY_WITHLOAD_COMPARATOR.compare(FWLoad(20, 10, 1), FWLoad(20, 10, 1)));
        assertEquals(0, FLOWENTRY_WITHLOAD_COMPARATOR.compare(FWLoad(50, 30, 2), FWLoad(100, 50, 5)));
        assertEquals(-1, FLOWENTRY_WITHLOAD_COMPARATOR.compare(FWLoad(200, 100, 4), FWLoad(300, 200, 10)));
    }

    private FlowEntryWithLoad FWLoad(long current, long previous, long interval) {
        return new FlowEntryWithLoad(cp, fEntry, new DefaultLoad(current, previous, interval));
    }

    @Test
    public void testElementComparator() {
        assertEquals(0, ELEMENT_COMPARATOR.compare(element("of", "foo", "of:foo"), element("of", "foo", "of:foo")));
        assertEquals(0, ELEMENT_COMPARATOR.compare(element("of", "bar", "of:bar"), element("of", "foo", "of:bar")));
        assertNotEquals(0, ELEMENT_COMPARATOR.compare(element("of", "foo", "of:foo"), element("of", "foo", "of:bar")));
    }

    private Element element(String scheme, String provID, String devID) {
        return new DefaultDevice((new ProviderId(scheme, provID)), deviceId(devID), null, MFR, HW1, SW1, SN, CID);
    }

    @Test
    public void testGroupComparator() {
        assertEquals(0, GROUP_COMPARATOR.compare(group(10), group(10)));
        assertEquals(-1, GROUP_COMPARATOR.compare(group(25), group(100)));
        assertEquals(1, GROUP_COMPARATOR.compare(group(20), group(10)));
    }

    private Group group(int id) {
        return new DefaultGroup(new GroupId(id), groupDesc1);
    }

    @Test
    public void testPortComparator() {
        assertEquals(0, PORT_COMPARATOR.compare(portTest(100), portTest(100)));
        assertNotEquals(0, PORT_COMPARATOR.compare(portTest(100), portTest(200)));
    }

    private Port portTest(long portNumber) {
        return new DefaultPort(null, PortNumber.portNumber(portNumber), true, Port.Type.COPPER, 100);
    }

    @Test
    public void testTopologyClusterTest() {
        assertEquals(0, CLUSTER_COMPARATOR.compare(cluster(3, 2, 1, "of:1"), cluster(3, 2, 1, "of:1")));
        assertNotEquals(0, CLUSTER_COMPARATOR.compare(cluster(5, 2, 1, "of:1"), cluster(3, 2, 1, "of:1")));
    }

    private TopologyCluster cluster(int id, int dc, int lc, String root) {
        return new DefaultTopologyCluster(clusterId(id), dc, lc,
                new DefaultTopologyVertex(deviceId(root)));
    }

    @Test
    public void testControllerNode() {
        assertEquals(0, NODE_COMPARATOR.compare(node("testId"), node("testId")));
        assertTrue(NODE_COMPARATOR.compare(node("abc"), node("xyz")) < 0);
        assertTrue(NODE_COMPARATOR.compare(node("xyz"), node("abc")) > 0);
    }

    private ControllerNode node(String id) {
        return new DefaultControllerNode(NodeId.nodeId(id), ipAddress, 9876);
    }

    @Test
    public void testInterfaceComparator() {
        assertEquals(0, INTERFACES_COMPARATOR.compare(intface("of:0000000000000001", 100), intface("of:0000000000000001", 100)));
        assertTrue(INTERFACES_COMPARATOR.compare(intface("of:0000000000000001", 2), intface("of:0000000000000001", 100)) < 0);
        assertTrue(INTERFACES_COMPARATOR.compare(intface("of:0000000000000001", 2), intface("of:0000000000000002", 2)) < 0);
    }

    private Interface intface(String deviceID, long port) {
        return new Interface("testInterface", connectPoint1(deviceID, port), null, null, null);
    }

    final ConnectPoint connectPoint1(String deviceID, long portNumber) {
        return new ConnectPoint(DeviceId.deviceId(deviceID), PortNumber.portNumber(portNumber));
    }

    @Test
    public void testDeviceKeyComparator() {
        assertEquals(0, DEVICE_KEY_COMPARATOR.compare(testDK("ID1", "label", "name"), testDK("ID1", "label", "name")));
        assertEquals(0, DEVICE_KEY_COMPARATOR.compare(testDK("ID2", "label", "name"), testDK("ID2", "label", "name")));
        assertNotEquals(0, DEVICE_KEY_COMPARATOR.compare(testDK("ID1", "label", "name"), testDK("ID2", "label", "name")));
    }

    private DeviceKey testDK(String ID, String testLabel, String testName) {
        return DeviceKey.createDeviceKeyUsingCommunityName(DeviceKeyId.deviceKeyId(ID), testLabel, testName);
    }

    @Test
    public void testRegionComparator() {
        assertEquals(0, REGION_COMPARATOR.compare(region("id1"), region("id1")));
        assertNotEquals(0, REGION_COMPARATOR.compare(region("id1"), region("id2")));
    }

    private Region region(String id) {
        return new DefaultRegion(RegionId.regionId(id), "name", Region.Type.METRO, DefaultAnnotations.EMPTY, null);
    }

    @Test
    public void testTopographicLayoutComparator() {
        assertEquals(0, LAYOUT_COMPARATOR.compare(layout("test"), layout("test")));
        assertNotEquals(0, LAYOUT_COMPARATOR.compare(layout("same"), layout("different")));
    }

    private UiTopoLayout layout(String id) {
        return new UiTopoLayout(UiTopoLayoutId.layoutId(id));
    }

    @Test
    public void testTenantIdComparator() {
        assertEquals(0, TENANT_ID_COMPARATOR.compare(id("1"), id("1")));
        assertEquals(0, TENANT_ID_COMPARATOR.compare(id("tenant1"), id("tenant1")));
        assertNotEquals(0, TENANT_ID_COMPARATOR.compare(id("tenant1"), id("tenant2")));
        assertTrue(TENANT_ID_COMPARATOR.compare(id("1"), id("9")) < 0);
        assertTrue(TENANT_ID_COMPARATOR.compare(id("Tenant5"), id("Tenant0")) > 0);
    }

    private TenantId id(String id) {
        return TenantId.tenantId(id);
    }

    @Test
    public void testVirtualNetworkComparator() {
        assertNotEquals(0, VIRTUAL_NETWORK_COMPARATOR.compare(network(10, "tenantID"), network(10, "tenantID1")));
        assertNotEquals(0, VIRTUAL_NETWORK_COMPARATOR.compare(network(10, "tenantID"), network(15, "tenantID1")));
        assertEquals(0, VIRTUAL_NETWORK_COMPARATOR.compare(network(15, "tenantID1"), network(10, "tenantID1")));
        assertNotEquals(0, VIRTUAL_NETWORK_COMPARATOR.compare(network(15, "tenantID"), network(10, "tenantID1")));
    }

    private VirtualNetwork network(int networkID, String tenantID) {
        return new DefaultVirtualNetwork(NetworkId.networkId(networkID), TenantId.tenantId(tenantID));
    }

    @Test
    public void testVirtualDeviceComparator() {
        assertEquals(0, VIRTUAL_DEVICE_COMPARATOR.compare(VD(0, "of:foo"), VD(0, "of:foo")));
        assertEquals(0, VIRTUAL_DEVICE_COMPARATOR.compare(VD(3, "of:foo"), VD(0, "of:foo")));
        assertNotEquals(0, VIRTUAL_DEVICE_COMPARATOR.compare(VD(0, "of:bar"), VD(0, "of:foo")));
        assertNotEquals(0, VIRTUAL_DEVICE_COMPARATOR.compare(VD(3, "of:bar"), VD(0, "of:foo")));
    }

    private VirtualDevice VD(int netID, String devID) {
        return new DefaultVirtualDevice(NetworkId.networkId(netID), DeviceId.deviceId(devID));
    }

    @Test
    public void testVirtualPortComparator() {
        assertEquals(0, VIRTUAL_PORT_COMPARATOR.compare(vPort(2), vPort(2)));
        assertEquals(4, VIRTUAL_PORT_COMPARATOR.compare(vPort(900), vPort(5)));
        assertEquals(-8, VIRTUAL_PORT_COMPARATOR.compare(vPort(0), vPort(8)));
    }

    private VirtualPort vPort(int portNumber) {
        return new DefaultVirtualPort(NetworkId.networkId(20), new DefaultDevice(PID, DID, null, MFR, HW, SW, SN, CID),
                PortNumber.portNumber(portNumber), new ConnectPoint(DID, PortNumber.portNumber(900)));
    }
}











