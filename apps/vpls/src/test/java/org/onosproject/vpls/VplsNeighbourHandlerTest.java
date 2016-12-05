/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.vpls;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceListener;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.incubator.net.neighbour.NeighbourHandlerRegistration;
import org.onosproject.incubator.net.neighbour.NeighbourMessageContext;
import org.onosproject.incubator.net.neighbour.NeighbourMessageHandler;
import org.onosproject.incubator.net.neighbour.NeighbourMessageType;
import org.onosproject.incubator.net.neighbour.NeighbourProtocol;
import org.onosproject.incubator.net.neighbour.NeighbourResolutionService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DeviceId;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.HostServiceAdapter;
import org.onosproject.net.provider.ProviderId;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertEquals;

/**
 * Tests the the {@link VplsNeighbourHandler} class.
 */
public class VplsNeighbourHandlerTest {

    private static final String IFACES_NOT_EXPECTED =
            "The interfaces reached by the packet are not equal to the " +
                    "interfaces expected.";

    private static final DeviceId DID1 = getDeviceId(1);
    private static final DeviceId DID2 = getDeviceId(2);
    private static final DeviceId DID3 = getDeviceId(3);
    private static final DeviceId DID4 = getDeviceId(4);
    private static final DeviceId DID5 = getDeviceId(5);

    private static final PortNumber P1 = PortNumber.portNumber(1);
    private static final PortNumber P2 = PortNumber.portNumber(2);
    private static final PortNumber P3 = PortNumber.portNumber(3);

    private static final ConnectPoint OF1P1 = new ConnectPoint(DID1, P1);
    private static final ConnectPoint OF2P1 = new ConnectPoint(DID2, P1);
    private static final ConnectPoint OF3P1 = new ConnectPoint(DID3, P1);
    private static final ConnectPoint OF4P1 = new ConnectPoint(DID4, P1);
    private static final ConnectPoint OF4P2 = new ConnectPoint(DID4, P2);
    private static final ConnectPoint OF4P3 = new ConnectPoint(DID4, P3);
    private static final ConnectPoint OF5P1 = new ConnectPoint(DID5, P1);
    private static final ConnectPoint OF5P2 = new ConnectPoint(DID5, P2);
    private static final ConnectPoint OF5P3 = new ConnectPoint(DID5, P3);

    private static final String VPLS1 = "vpls1";
    private static final String VPLS2 = "vpls2";
    private static final String VPLS3 = "vpls3";
    private static final String VPLS4 = "vpls4";

    private static final VlanId VLAN100 = VlanId.vlanId("100");
    private static final VlanId VLAN200 = VlanId.vlanId("200");
    private static final VlanId VLAN300 = VlanId.vlanId("300");
    private static final VlanId VLAN400 = VlanId.vlanId("400");
    private static final VlanId VLAN_NONE = VlanId.NONE;

    private static final Interface V100H1 =
            new Interface("v100h1", OF1P1, null, null, VLAN100);
    private static final Interface V100H2 =
            new Interface("v100h2", OF4P1, null, null, VLAN100);
    private static final Interface V200H1 =
            new Interface("v200h1", OF4P2, null, null, VLAN200);
    private static final Interface V200H2 =
            new Interface("v200h2", OF2P1, null, null, VLAN200);
    private static final Interface V300H1 =
            new Interface("v300h1", OF3P1, null, null, VLAN300);
    private static final Interface V400H1 =
            new Interface("v400h1", OF5P1, null, null, VLAN400);
    private static final Interface VNONEH1 =
            new Interface("vNoneh1", OF5P2, null, null, VLAN_NONE);
    private static final Interface VNONEH2 =
            new Interface("vNoneh2", OF5P3, null, null, VLAN_NONE);
    private static final Interface VNONEH3 =
            new Interface("vNoneh3", OF4P3, null, null, VLAN_NONE);

    private static final MacAddress MAC1 = MacAddress.valueOf("00:00:00:00:00:01");
    private static final MacAddress MAC2 = MacAddress.valueOf("00:00:00:00:00:02");
    private static final MacAddress MAC3 = MacAddress.valueOf("00:00:00:00:00:03");
    private static final MacAddress MAC4 = MacAddress.valueOf("00:00:00:00:00:04");
    private static final MacAddress MAC5 = MacAddress.valueOf("00:00:00:00:00:05");
    private static final MacAddress MAC6 = MacAddress.valueOf("00:00:00:00:00:06");
    private static final MacAddress MAC7 = MacAddress.valueOf("00:00:00:00:00:07");
    private static final MacAddress MAC8 = MacAddress.valueOf("00:00:00:00:00:08");
    private static final MacAddress MAC9 = MacAddress.valueOf("00:00:00:00:00:09");

    private static final ProviderId PID = new ProviderId("of", "foo");

    private final Host v100Host1 = makeHost(MAC1, VLAN100, OF1P1);
    private final Host v100Host2 = makeHost(MAC2, VLAN100, OF4P1);
    private final Host v200Host1 = makeHost(MAC3, VLAN200, OF4P2);
    private final Host v200Host2 = makeHost(MAC5, VLAN200, OF2P1);
    private final Host v300Host1 = makeHost(MAC4, VLAN300, OF3P1);
    private final Host v400Host1 = makeHost(MAC6, VLAN400, OF5P1);
    private final Host vNoneHost1 = makeHost(MAC7, VLAN_NONE, OF5P2);
    private final Host vNoneHost2 = makeHost(MAC8, VLAN_NONE, OF5P3);
    private final Host vNoneHost3 = makeHost(MAC9, VLAN_NONE, OF4P3);

    private final Set<Host> availableHosts = ImmutableSet.of(v100Host1,
                                                             v100Host2,
                                                             v200Host1,
                                                             v300Host1,
                                                             v200Host2,
                                                             v400Host1,
                                                             vNoneHost1,
                                                             vNoneHost2,
                                                             vNoneHost3);

    private final Set<Interface> availableInterfaces =
            ImmutableSet.of(V100H1, V100H2, V200H1, V200H2, V300H1,
                            V400H1, VNONEH1, VNONEH2, VNONEH3);

    private VplsNeighbourHandler vplsNeighbourHandler;

    private HostService hostService;

    /**
     * Sets up 4 VPLS.
     * VPLS 1 contains 3 hosts: v100h1, v200h1 and v300h1
     * VPLS 2 contains 2 hosts: v100h2, v200h2
     * VPLS 3 contains 2 hosts: vNoneh1, vNoneh2
     * VPLS 4 contains 2 hosts: v400h1, vNoneh3
     */
    @Before
    public void setUp() {
        vplsNeighbourHandler = new VplsNeighbourHandler();
        SetMultimap<String, Interface> ifacesByVpls =
                HashMultimap.create();
        ifacesByVpls.put(VPLS1, V100H1);
        ifacesByVpls.put(VPLS1, V200H1);
        ifacesByVpls.put(VPLS1, V300H1);
        ifacesByVpls.put(VPLS2, V100H2);
        ifacesByVpls.put(VPLS2, V200H2);
        ifacesByVpls.put(VPLS3, VNONEH1);
        ifacesByVpls.put(VPLS3, VNONEH2);
        ifacesByVpls.put(VPLS4, V400H1);
        ifacesByVpls.put(VPLS4, VNONEH3);
        HashMap<String, EncapsulationType> encap = Maps.newHashMap();
        vplsNeighbourHandler.vplsConfigService =
                new TestVplsConfigService(ifacesByVpls, encap);
        vplsNeighbourHandler.interfaceService =
                new TestInterfaceService();
        vplsNeighbourHandler.neighbourService =
                new TestNeighbourService();
        hostService = new TestHostService();
    }

    @After
    public void tearDown() {
    }

    /**
     * Sends request messages to all hosts in VPLS 1.
     * Request messages should be received from other hosts in VPLS 1.
     */
    @Test
    public void vpls1RequestMessage() {
        // Request messages from v100h1 (VPLS 1) should be received by v200h1 and v300h1
        TestMessageContext requestMessage =
                makeBroadcastRequestContext(v100Host1);
        Set<Interface> expectInterfaces = ImmutableSet.of(V200H1, V300H1);
        vplsNeighbourHandler.handleRequest(requestMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, requestMessage.forwardResults);

        // Request messages from v200h1 (VPLS 1) should be received by v100h1 and v300h1
        requestMessage = makeBroadcastRequestContext(v200Host1);
        expectInterfaces = ImmutableSet.of(V100H1, V300H1);
        vplsNeighbourHandler.handleRequest(requestMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, requestMessage.forwardResults);

        // Request from v300h1 (VPLS 1) should be received by v100h1 and v200h1
        requestMessage = makeBroadcastRequestContext(v300Host1);
        expectInterfaces = ImmutableSet.of(V100H1, V200H1);
        vplsNeighbourHandler.handleRequest(requestMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, requestMessage.forwardResults);
    }

    /**
     * Sends request messages to all hosts in VPLS 2.
     * Request messages should be received from other hosts in VPLS 2.
     */
    @Test
    public void vpls2RequestMessage() {
        // Request messages from v100h2 (VPLS 2) should be received by v200h2
        TestMessageContext requestMessage =
                makeBroadcastRequestContext(v100Host2);
        Set<Interface> expectInterfaces = ImmutableSet.of(V200H2);
        vplsNeighbourHandler.handleRequest(requestMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, requestMessage.forwardResults);

        // Request messages from v200h2 (VPLS 2) should be received by v100h2
        requestMessage = makeBroadcastRequestContext(v200Host2);
        expectInterfaces = ImmutableSet.of(V100H2);
        vplsNeighbourHandler.handleRequest(requestMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, requestMessage.forwardResults);
    }

    /**
     * Tests correct connection between untagged interfaces.
     *
     * Sends request messages to all hosts in VPLS 3.
     * Request messages should be received from other hosts in VPLS 3.
     */
    @Test
    public void vpls3RequestMessage() {
        // Request messages from vNoneHost1 (VPLS 3) should be received by vNoneHost2
        TestMessageContext requestMessage =
                makeBroadcastRequestContext(vNoneHost1);
        Set<Interface> expectInterfaces = ImmutableSet.of(VNONEH2);
        vplsNeighbourHandler.handleRequest(requestMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, requestMessage.forwardResults);

        // Request messages from vNoneh2 (VPLS 3) should be received by vNoneh1
        requestMessage = makeBroadcastRequestContext(vNoneHost2);
        expectInterfaces = ImmutableSet.of(VNONEH1);
        vplsNeighbourHandler.handleRequest(requestMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, requestMessage.forwardResults);
    }

    /**
     * Tests correct connection between tagged and untagged interfaces.
     *
     * Sends request messages to all hosts in VPLS 4.
     * Request messages should be received from other hosts in VPLS 4.
     */
    @Test
    public void vpls4RequestMessage() {
        // Request messages from v400Host1 (VPLS 4) should be received by vNoneHost3
        TestMessageContext requestMessage =
                makeBroadcastRequestContext(v400Host1);
        Set<Interface> expectInterfaces = ImmutableSet.of(VNONEH3);
        vplsNeighbourHandler.handleRequest(requestMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, requestMessage.forwardResults);

        // Request messages from vNoneHost3 (VPLS 4) should be received by v400Host1
        requestMessage = makeBroadcastRequestContext(vNoneHost3);
        expectInterfaces = ImmutableSet.of(V400H1);
        vplsNeighbourHandler.handleRequest(requestMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, requestMessage.forwardResults);
    }

    /**
     * Sends reply messages to hosts in VPLS 1.
     * Reply messages should be received by the host with MAC address equal to
     * the dstMac of the message context.
     */
    @Test
    public void vpls1ReplyMessage() {
        // Reply messages from v100h1 (VPLS 1) should be received by v200h1
        TestMessageContext replyMessage =
                makeReplyContext(v100Host1, v200Host1);
        Set<Interface> expectInterfaces = ImmutableSet.of(V200H1);
        vplsNeighbourHandler.handleReply(replyMessage, hostService);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, replyMessage.forwardResults);

        // Reply messages from v200h1 (VPLS 1) should be received by v300h1
        replyMessage = makeReplyContext(v200Host1, v300Host1);
        expectInterfaces = ImmutableSet.of(V300H1);
        vplsNeighbourHandler.handleReply(replyMessage, hostService);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, replyMessage.forwardResults);

        // Reply messages from v300h1 (VPLS 1) should be received by v100h1
        replyMessage = makeReplyContext(v300Host1, v100Host1);
        expectInterfaces = ImmutableSet.of(V100H1);
        vplsNeighbourHandler.handleReply(replyMessage, hostService);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, replyMessage.forwardResults);
    }

    /**
     * Sends reply messages to hosts in VPLS 2.
     * Reply messages should be received by the host with MAC address equal to
     * the dstMac of the message context.
     */
    @Test
    public void vpls2ReplyMessage() {
        // Reply messages from v100h2 (VPLS 2) should be received by v200h2
        TestMessageContext replyMessage =
                makeReplyContext(v100Host2, v200Host2);
        Set<Interface> expectInterfaces = ImmutableSet.of(V200H2);
        vplsNeighbourHandler.handleReply(replyMessage, hostService);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, replyMessage.forwardResults);

        // Reply messages from v200h2 (VPLS 2) should be received by v100h2
        replyMessage = makeReplyContext(v200Host2, v100Host2);
        expectInterfaces = ImmutableSet.of(V100H2);
        vplsNeighbourHandler.handleReply(replyMessage, hostService);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, replyMessage.forwardResults);
    }

    /**
     * Sends reply messages to hosts in VPLS 3.
     * Reply messages should be received by the host with MAC address equal to
     * the dstMac of the message context.
     */
    @Test
    public void vpls3ReplyMessage() {
        // Reply messages from vNoneh1 (VPLS 3) should be received by vNoneh2
        TestMessageContext replyMessage =
                makeReplyContext(vNoneHost1, vNoneHost2);
        Set<Interface> expectInterfaces = ImmutableSet.of(VNONEH2);
        vplsNeighbourHandler.handleReply(replyMessage, hostService);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, replyMessage.forwardResults);

        // Reply messages from vNoneh2 (VPLS 3) should be received by vNoneh1
        replyMessage = makeReplyContext(vNoneHost2, vNoneHost1);
        expectInterfaces = ImmutableSet.of(VNONEH1);
        vplsNeighbourHandler.handleReply(replyMessage, hostService);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, replyMessage.forwardResults);
    }

    /**
     * Sends reply messages to hosts in VPLS 4.
     * Reply messages should be received by the host with MAC address equal to
     * the dstMac of the message context.
     */
    @Test
    public void vpls4ReplyMessage() {
        // Reply messages from v400h1 (VPLS 4) should be received by vNoneh3
        TestMessageContext replyMessage =
                makeReplyContext(v400Host1, vNoneHost3);
        Set<Interface> expectInterfaces = ImmutableSet.of(VNONEH3);
        vplsNeighbourHandler.handleReply(replyMessage, hostService);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, replyMessage.forwardResults);

        // Reply messages from vNoneh3 (VPLS 4) should be received by v400h1
        replyMessage = makeReplyContext(vNoneHost3, v400Host1);
        expectInterfaces = ImmutableSet.of(V400H1);
        vplsNeighbourHandler.handleReply(replyMessage, hostService);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, replyMessage.forwardResults);
    }

    /**
     * Sends wrong reply messages to hosts.
     * The source and the destination MAC addresses are not set on any host of the VPLS.
     * The reply messages will not be received by any hosts.
     */
    @Test
    public void wrongReplyMessage() {
        // Reply message from v100h1 (VPLS 1) to v100h2 (VPLS 2).
        // Forward results should be empty
        TestMessageContext replyMessage = makeReplyContext(v100Host1, v100Host2);
        Set<Interface> expectInterfaces = ImmutableSet.of();
        vplsNeighbourHandler.handleReply(replyMessage, hostService);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, replyMessage.forwardResults);

        // Reply message from v200h2 (VPLS 2) to v300h1 (VPLS 1).
        // Forward results should be empty
        replyMessage = makeReplyContext(v200Host2, v300Host1);
        expectInterfaces = ImmutableSet.of();
        vplsNeighbourHandler.handleReply(replyMessage, hostService);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, replyMessage.forwardResults);

        // Reply message from vNoneh1 (VPLS 3) to v400h1 (VPLS 4).
        // Forward results should be empty
        replyMessage = makeReplyContext(vNoneHost1, v400Host1);
        expectInterfaces = ImmutableSet.of();
        vplsNeighbourHandler.handleReply(replyMessage, hostService);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, replyMessage.forwardResults);

        // Reply message from vNoneh3 (VPLS 4) to vNoneH2 (VPLS 3).
        // Forward results should be empty
        replyMessage = makeReplyContext(vNoneHost3, vNoneHost2);
        expectInterfaces = ImmutableSet.of();
        vplsNeighbourHandler.handleReply(replyMessage, hostService);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, replyMessage.forwardResults);
    }

    /**
     * Returns the device Id of the ith device.
     *
     * @param i the device to get the Id of
     * @return the device Id
     */
    private static DeviceId getDeviceId(int i) {
        return DeviceId.deviceId("" + i);
    }

    private Host makeHost(MacAddress mac, VlanId vlan, ConnectPoint cp) {
        return new DefaultHost(PID,
                               HostId.hostId(mac, vlan),
                               mac,
                               vlan,
                               new HostLocation(cp, 0),
                               Sets.newHashSet());
    }

    private TestMessageContext makeBroadcastRequestContext(Host host) {
        return new TestMessageContext(host.location(),
                                      host.mac(),
                                      MacAddress.BROADCAST,
                                      host.vlan(),
                                      NeighbourMessageType.REQUEST);
    }

    private TestMessageContext makeReplyContext(Host src, Host dst) {
        return new TestMessageContext(src.location(),
                                      src.mac(),
                                      dst.mac(),
                                      src.vlan(),
                                      NeighbourMessageType.REPLY);
    }

    private class TestMessageContext implements NeighbourMessageContext {


        private final NeighbourMessageType type;
        private final MacAddress srcMac;
        private final MacAddress dstMac;
        private final ConnectPoint inPort;
        private final VlanId vlanId;

        public Set<Interface> forwardResults;

        /**
         * Creates new neighbour message context for test.
         *
         * @param inPort the input port
         * @param srcMac the source Mac
         * @param dstMac the destination Mac
         * @param vlanId the VLAN Id
         * @param type the message context type
         */
        public TestMessageContext(
                ConnectPoint inPort,
                MacAddress srcMac,
                MacAddress dstMac,
                VlanId vlanId,
                NeighbourMessageType type) {

            this.inPort = inPort;
            this.srcMac = srcMac;
            this.dstMac = dstMac;
            this.vlanId = vlanId;
            this.type = type;

            this.forwardResults = Sets.newHashSet();
        }

        @Override
        public ConnectPoint inPort() {
            return inPort;
        }

        @Override
        public NeighbourMessageType type() {
            return type;
        }

        @Override
        public VlanId vlan() {
            return vlanId;
        }

        @Override
        public MacAddress srcMac() {
            return srcMac;
        }

        @Override
        public MacAddress dstMac() {
            return dstMac;
        }

        @Override
        public IpAddress target() {
            return null;
        }

        @Override
        public IpAddress sender() {
            return null;
        }

        @Override
        public void forward(ConnectPoint outPort) {
        }

        @Override
        public void forward(Interface outIntf) {
            forwardResults.add(outIntf);
        }

        @Override
        public void reply(MacAddress targetMac) {
        }

        @Override
        public void flood() {
        }

        @Override
        public void drop() {
        }

        @Override
        public Ethernet packet() {
            return null;
        }

        @Override
        public NeighbourProtocol protocol() {
            return null;
        }
    }

    private class TestVplsConfigService extends VplsConfigServiceAdapter {

        private final SetMultimap<String, Interface> ifacesByVplsName;

        public TestVplsConfigService(SetMultimap<String, Interface> ifacesByVplsName,
                                     HashMap<String, EncapsulationType> encapByVplsName) {
            this.ifacesByVplsName = ifacesByVplsName;
        }

        @Override
        public void addVpls(String vplsName, Set<String> ifaceNames, String encap) {
            if (!ifacesByVplsName.containsKey(vplsName)) {
                ifaceNames.forEach(ifaceName -> {
                    availableInterfaces.forEach(iface -> {
                        if (iface.name().equals(ifaceName)) {
                            ifacesByVplsName.put(vplsName, iface);
                        }
                    });
                });
            }
        }

        @Override
        public void removeVpls(String vplsName) {
            if (ifacesByVplsName.containsKey(vplsName)) {
                ifacesByVplsName.removeAll(vplsName);
            }
        }

        @Override
        public void addIface(String vplsName, String ifaceName) {
            availableInterfaces.forEach(intf -> {
                if (intf.name().equals(ifaceName)) {
                    ifacesByVplsName.put(vplsName, intf);
                }
            });
        }

        @Override
        public void removeIface(String ifaceName) {
            SetMultimap<String, Interface> toBeRemoved = HashMultimap.create();
            ifacesByVplsName.entries().forEach(e -> {
                if (e.getValue().name().equals(ifaceName)) {
                    toBeRemoved.put(e.getKey(), e.getValue());
                }
            });
            toBeRemoved.entries()
                    .forEach(e -> ifacesByVplsName.remove(e.getKey(),
                                                          e.getValue()));
        }

        @Override
        public void cleanVplsConfig() {
            ifacesByVplsName.clear();
        }

        @Override
        public Set<Interface> allIfaces() {
            return ImmutableSet.copyOf(ifacesByVplsName.values());
        }

        @Override
        public Set<Interface> ifaces(String name) {
            return ifacesByVplsName.get(name)
                    .stream()
                    .collect(Collectors.toSet());
        }

        @Override
        public Set<String> vplsNames() {
            return ifacesByVplsName.keySet();
        }

        @Override
        public SetMultimap<String, Interface> ifacesByVplsName() {
            return ImmutableSetMultimap.copyOf(ifacesByVplsName);
        }

        @Override
        public SetMultimap<String, Interface> ifacesByVplsName(VlanId vlan,
                                                               ConnectPoint connectPoint) {
            String vplsName =
                    ifacesByVplsName.entries().stream()
                            .filter(e -> e.getValue().connectPoint().equals(connectPoint))
                            .filter(e -> e.getValue().vlan().equals(vlan))
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElse(null);
            SetMultimap<String, Interface> result = HashMultimap.create();
            if (vplsName != null &&
                    ifacesByVplsName.containsKey(vplsName)) {
                ifacesByVplsName.get(vplsName)
                        .forEach(intf -> result.put(vplsName, intf));
                return result;
            }
            return null;
        }
    }

    class TestHostService extends HostServiceAdapter {
        @Override
        public Set<Host> getHostsByMac(MacAddress mac) {
            return availableHosts.stream()
                    .filter(host -> host.mac().equals(mac))
                    .collect(Collectors.toSet());
        }

        @Override
        public Iterable<Host> getHosts() {
            return availableHosts;
        }

        @Override
        public Set<Host> getHostsByVlan(VlanId vlanId) {
            return availableHosts.stream()
                    .filter(host -> host.vlan().equals(vlanId))
                    .collect(Collectors.toSet());
        }

        @Override
        public int getHostCount() {
            return availableHosts.size();
        }

        @Override
        public Host getHost(HostId hostId) {
            return availableHosts.stream()
                    .filter(host -> host.id().equals(hostId))
                    .findFirst()
                    .orElse(null);
        }
    }

    private class TestNeighbourService implements NeighbourResolutionService {
        private SetMultimap<ConnectPoint, NeighbourHandlerRegistration> handlerRegs;

        public TestNeighbourService() {
            handlerRegs = HashMultimap.create();
        }

        @Override
        public void registerNeighbourHandler(ConnectPoint connectPoint,
                                             NeighbourMessageHandler handler,
                                             ApplicationId appId) {
            Interface intf =
                    new Interface(null, connectPoint, null, null, null);

            NeighbourHandlerRegistration reg =
                    new HandlerRegistration(handler, intf, appId);

            handlerRegs.put(connectPoint, reg);
        }

        @Override
        public void registerNeighbourHandler(Interface intf,
                                             NeighbourMessageHandler handler,
                                             ApplicationId appId) {
            NeighbourHandlerRegistration reg =
                    new HandlerRegistration(handler, intf, appId);
            handlerRegs.put(intf.connectPoint(), reg);
        }

        @Override
        public void unregisterNeighbourHandler(ConnectPoint connectPoint,
                                               NeighbourMessageHandler handler,
                                               ApplicationId appId) {
            handlerRegs.removeAll(connectPoint);
        }

        @Override
        public void unregisterNeighbourHandler(Interface intf,
                                               NeighbourMessageHandler handler,
                                               ApplicationId appId) {
            handlerRegs.removeAll(intf.connectPoint());
        }

        @Override
        public void unregisterNeighbourHandlers(ApplicationId appId) {
            handlerRegs.clear();
        }

        @Override
        public Map<ConnectPoint, Collection<NeighbourHandlerRegistration>> getHandlerRegistrations() {
            return handlerRegs.asMap();
        }

        private class HandlerRegistration implements NeighbourHandlerRegistration {
            private final Interface intf;
            private final NeighbourMessageHandler handler;
            private final ApplicationId appId;

            /**
             * Creates a new registration handler.
             *
             * @param handler the neighbour message handler
             * @param intf the interface
             */
            public HandlerRegistration(NeighbourMessageHandler handler,
                                       Interface intf,
                                       ApplicationId appId) {
                this.intf = intf;
                this.handler = handler;
                this.appId = appId;
            }

            @Override
            public Interface intf() {
                return intf;
            }

            @Override
            public NeighbourMessageHandler handler() {
                return handler;
            }

            @Override
            public ApplicationId appId() {
                return appId;
            }
        }
    }

    class TestInterfaceService implements InterfaceService {

        @Override
        public void addListener(InterfaceListener listener) {
        }

        @Override
        public void removeListener(InterfaceListener listener) {
        }

        @Override
        public Set<Interface> getInterfaces() {
            return availableInterfaces;
        }

        @Override
        public Interface getInterfaceByName(ConnectPoint connectPoint,
                                            String name) {
            return availableInterfaces.stream()
                    .filter(intf -> intf.name().equals(name))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public Set<Interface> getInterfacesByPort(ConnectPoint port) {
            return availableInterfaces.stream()
                    .filter(intf -> intf.connectPoint().equals(port))
                    .collect(Collectors.toSet());
        }

        @Override
        public Set<Interface> getInterfacesByIp(IpAddress ip) {
            return availableInterfaces.stream()
                    .filter(intf -> intf.ipAddressesList().contains(ip))
                    .collect(Collectors.toSet());
        }

        @Override
        public Set<Interface> getInterfacesByVlan(VlanId vlan) {
            return availableInterfaces.stream()
                    .filter(intf -> intf.vlan().equals(vlan))
                    .collect(Collectors.toSet());
        }

        @Override
        public Interface getMatchingInterface(IpAddress ip) {
            return availableInterfaces.stream()
                    .filter(intf -> intf.ipAddressesList().contains(ip))
                    .findFirst()
                    .orElse(null);
        }
    }
}
