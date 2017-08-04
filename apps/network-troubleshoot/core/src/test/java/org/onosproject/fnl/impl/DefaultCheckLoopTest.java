/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.fnl.impl;

import com.google.common.collect.ImmutableSet;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ChassisId;
import org.onlab.packet.IPv4;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.fnl.intf.NetworkAnomaly;
import org.onosproject.fnl.intf.NetworkDiagnostic;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.onlab.packet.EthType.EtherType.IPV4;
import static org.onlab.packet.TpPort.tpPort;
import static org.onosproject.net.DefaultAnnotations.EMPTY;
import static org.onosproject.net.Link.State.ACTIVE;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.flow.FlowEntry.FlowEntryState.ADDED;
import static org.onosproject.net.provider.ProviderId.NONE;

/**
 * Unit Tests for DefaultCheckLoop class.
 */
public class DefaultCheckLoopTest {

    private NetworkDiagnostic defaultCheckLoop;

    private DeviceService ds;
    private HostService hs;
    private FlowRuleService frs;
    private LinkService ls;

    private List<Device> devices;
    private List<Host> hosts;
    private Map<DeviceId, Set<Link>> egressLinks;
    private Map<DeviceId, List<FlowEntry>> flowEntries;


    private static final DeviceId DEVICE_ID_A = DeviceId.deviceId("of:000000000000000A");
    private static final DeviceId DEVICE_ID_B = DeviceId.deviceId("of:000000000000000B");
    private static final DeviceId DEVICE_ID_C = DeviceId.deviceId("of:000000000000000C");
    private static final DeviceId DEVICE_ID_D = DeviceId.deviceId("of:000000000000000D");
    private static final DeviceId DEVICE_ID_E = DeviceId.deviceId("of:E000000000000000");
    private static final DeviceId DEVICE_ID_F = DeviceId.deviceId("of:F000000000000000");
    private static final DeviceId DEVICE_ID_G = DeviceId.deviceId("of:A000000000000000");

    private static final String HOSTID_EXAMPLE = "12:34:56:78:9A:BC/1355";
    private static final long FLOWRULE_COOKIE_EXAMPLE = 708;
    private static final int FLOWRULE_PRIORITY_EXAMPLE = 738;


    @Before
    public void setUp() {
        ds = EasyMock.createMock(DeviceService.class);
        hs = EasyMock.createMock(HostService.class);
        frs = EasyMock.createMock(FlowRuleService.class);
        ls = EasyMock.createMock(LinkService.class);

        defaultCheckLoop = new DefaultCheckLoop(ds, hs, frs, ls);
    }

    @After
    public void tearDown() {
        // do nothing
    }

    @Test
    public void testFindLoops() {
        produceTopoDevices();
        produceTopoHosts();
        produceTopoLinks();
        produceFlowEntries();

        initMock();

        Set<NetworkAnomaly> loops = defaultCheckLoop.findAnomalies();
        assertThat(loops, hasSize(1));
    }

    private void initMock() {
        expect(ds.getDevices()).andReturn(devices).anyTimes();
        replay(ds);

        expect(hs.getHosts()).andReturn(hosts).anyTimes();
        replay(hs);

        // --- init flow rule service ---

        expect(frs.getFlowEntries(DEVICE_ID_A))
                .andReturn(flowEntries.get(DEVICE_ID_A)).anyTimes();
        expect(frs.getFlowEntries(DEVICE_ID_B))
                .andReturn(flowEntries.get(DEVICE_ID_B)).anyTimes();
        expect(frs.getFlowEntries(DEVICE_ID_C))
                .andReturn(flowEntries.get(DEVICE_ID_C)).anyTimes();
        expect(frs.getFlowEntries(DEVICE_ID_D))
                .andReturn(flowEntries.get(DEVICE_ID_D)).anyTimes();
        expect(frs.getFlowEntries(DEVICE_ID_E))
                .andReturn(flowEntries.get(DEVICE_ID_E)).anyTimes();
        expect(frs.getFlowEntries(DEVICE_ID_F))
                .andReturn(flowEntries.get(DEVICE_ID_F)).anyTimes();
        expect(frs.getFlowEntries(DEVICE_ID_G))
                .andReturn(flowEntries.get(DEVICE_ID_G)).anyTimes();
        replay(frs);

        // --- init link service ---

        expect(ls.getDeviceEgressLinks(DEVICE_ID_A))
                .andReturn(egressLinks.get(DEVICE_ID_A)).anyTimes();
        expect(ls.getDeviceEgressLinks(DEVICE_ID_B))
                .andReturn(egressLinks.get(DEVICE_ID_B)).anyTimes();
        expect(ls.getDeviceEgressLinks(DEVICE_ID_C))
                .andReturn(egressLinks.get(DEVICE_ID_C)).anyTimes();
        expect(ls.getDeviceEgressLinks(DEVICE_ID_D))
                .andReturn(egressLinks.get(DEVICE_ID_D)).anyTimes();
        expect(ls.getDeviceEgressLinks(DEVICE_ID_E))
                .andReturn(egressLinks.get(DEVICE_ID_E)).anyTimes();
        expect(ls.getDeviceEgressLinks(DEVICE_ID_F))
                .andReturn(egressLinks.get(DEVICE_ID_F)).anyTimes();
        expect(ls.getDeviceEgressLinks(DEVICE_ID_G))
                .andReturn(egressLinks.get(DEVICE_ID_G)).anyTimes();
        replay(ls);
    }

    private void produceTopoDevices() {
        devices = new ArrayList<>();
        devices.add(produceOneDevice(DEVICE_ID_A));
        devices.add(produceOneDevice(DEVICE_ID_B));
        devices.add(produceOneDevice(DEVICE_ID_C));
        devices.add(produceOneDevice(DEVICE_ID_D));
        devices.add(produceOneDevice(DEVICE_ID_E));
        devices.add(produceOneDevice(DEVICE_ID_F));
        devices.add(produceOneDevice(DEVICE_ID_G));
    }

    private Device produceOneDevice(DeviceId dpid) {
        return new DefaultDevice(NONE, dpid,
                Device.Type.SWITCH, "", "", "", "", new ChassisId(),
                EMPTY);
    }

    private void produceTopoHosts() {
        hosts = new ArrayList<>();
        hosts.add(produceOneHost(DEVICE_ID_A, 3));
        hosts.add(produceOneHost(DEVICE_ID_B, 3));
        hosts.add(produceOneHost(DEVICE_ID_D, 3));
        hosts.add(produceOneHost(DEVICE_ID_F, 2));
        hosts.add(produceOneHost(DEVICE_ID_F, 3));
        hosts.add(produceOneHost(DEVICE_ID_G, 1));
        hosts.add(produceOneHost(DEVICE_ID_G, 3));
    }

    private Host produceOneHost(DeviceId dpid, int port) {
        return new DefaultHost(NONE, HostId.hostId(HOSTID_EXAMPLE),
                MacAddress.valueOf(0), VlanId.vlanId(),
                new HostLocation(dpid, portNumber(port), 1),
                ImmutableSet.of(), EMPTY);
    }

    private void produceTopoLinks() {
        egressLinks = new HashMap<>();

        Set<Link> el = new HashSet<>();
        el.add(produceOneEgressLink(DEVICE_ID_A, 1, DEVICE_ID_B, 1));
        el.add(produceOneEgressLink(DEVICE_ID_A, 2, DEVICE_ID_D, 2));
        egressLinks.put(DEVICE_ID_A, el);

        el = new HashSet<>();
        el.add(produceOneEgressLink(DEVICE_ID_B, 1, DEVICE_ID_A, 1));
        el.add(produceOneEgressLink(DEVICE_ID_B, 2, DEVICE_ID_C, 2));
        egressLinks.put(DEVICE_ID_B, el);

        el = new HashSet<>();
        el.add(produceOneEgressLink(DEVICE_ID_C, 1, DEVICE_ID_D, 1));
        el.add(produceOneEgressLink(DEVICE_ID_C, 2, DEVICE_ID_B, 2));
        el.add(produceOneEgressLink(DEVICE_ID_C, 3, DEVICE_ID_E, 3));
        egressLinks.put(DEVICE_ID_C, el);

        el = new HashSet<>();
        el.add(produceOneEgressLink(DEVICE_ID_D, 1, DEVICE_ID_C, 1));
        el.add(produceOneEgressLink(DEVICE_ID_D, 2, DEVICE_ID_A, 2));
        egressLinks.put(DEVICE_ID_D, el);

        el = new HashSet<>();
        el.add(produceOneEgressLink(DEVICE_ID_E, 1, DEVICE_ID_F, 1));
        el.add(produceOneEgressLink(DEVICE_ID_E, 2, DEVICE_ID_G, 2));
        el.add(produceOneEgressLink(DEVICE_ID_E, 3, DEVICE_ID_C, 3));
        egressLinks.put(DEVICE_ID_E, el);

        el = new HashSet<>();
        el.add(produceOneEgressLink(DEVICE_ID_F, 1, DEVICE_ID_E, 1));
        egressLinks.put(DEVICE_ID_F, el);

        el = new HashSet<>();
        el.add(produceOneEgressLink(DEVICE_ID_G, 2, DEVICE_ID_E, 2));
        egressLinks.put(DEVICE_ID_G, el);
    }

    private Link produceOneEgressLink(DeviceId srcId, int srcPort, DeviceId dstId, int dstPort) {
        return DefaultLink.builder()
                .providerId(NONE)
                .src(new ConnectPoint(srcId, portNumber(srcPort)))
                .dst(new ConnectPoint(dstId, portNumber(dstPort)))
                .type(DIRECT)
                .state(ACTIVE)
                .build();
    }

    private void produceFlowEntries() {
        flowEntries = new HashMap<>();

        List<FlowEntry> fe = new ArrayList<>();
        fe.add(produceOneFlowEntry(DEVICE_ID_A, 2));
        flowEntries.put(DEVICE_ID_A, fe);

        fe = new ArrayList<>();
        fe.add(produceOneFlowEntry(DEVICE_ID_D, 1));
        flowEntries.put(DEVICE_ID_D, fe);

        fe = new ArrayList<>();
        fe.add(produceOneFlowEntry(DEVICE_ID_C, 2));
        flowEntries.put(DEVICE_ID_C, fe);

        fe = new ArrayList<>();
        fe.add(produceOneFlowEntry(DEVICE_ID_B, 1));
        flowEntries.put(DEVICE_ID_B, fe);

        flowEntries.put(DEVICE_ID_E, new ArrayList<>());
        flowEntries.put(DEVICE_ID_F, new ArrayList<>());
        flowEntries.put(DEVICE_ID_G, new ArrayList<>());

    }

    private FlowEntry produceOneFlowEntry(DeviceId dpid, int outPort) {

        TrafficSelector.Builder sb = DefaultTrafficSelector.builder()
                .matchEthType(IPV4.ethType().toShort())
                .matchIPDst(IpPrefix.valueOf("10.0.0.0/8"))
                .matchIPProtocol(IPv4.PROTOCOL_TCP)
                .matchTcpDst(tpPort(22));

        TrafficTreatment.Builder tb = DefaultTrafficTreatment.builder()
                .setOutput(portNumber(outPort));

        return new DefaultFlowEntry(DefaultFlowRule.builder()
                .withPriority(FLOWRULE_PRIORITY_EXAMPLE).forDevice(dpid).forTable(0)
                .withCookie(FLOWRULE_COOKIE_EXAMPLE)
                .withSelector(sb.build()).withTreatment(tb.build())
                .makePermanent().build(), ADDED);
    }
}
