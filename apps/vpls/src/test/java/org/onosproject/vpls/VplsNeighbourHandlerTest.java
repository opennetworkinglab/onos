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
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.PortNumber;
import org.onosproject.net.host.HostService;
import org.onosproject.net.host.HostServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.vpls.config.VplsConfigurationService;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static junit.framework.TestCase.assertEquals;

/**
 * Tests the the {@link VplsNeighbourHandler} class.
 */
public class VplsNeighbourHandlerTest {
    private final ConnectPoint of1p1 =
            new ConnectPoint(DeviceId.deviceId("of:1"),
                             PortNumber.portNumber(1));
    private final ConnectPoint of4p1 =
            new ConnectPoint(DeviceId.deviceId("of:4"),
                             PortNumber.portNumber(1));
    private final ConnectPoint of3p1 =
            new ConnectPoint(DeviceId.deviceId("of:3"),
                             PortNumber.portNumber(1));
    private final ConnectPoint of4p2 =
            new ConnectPoint(DeviceId.deviceId("of:4"),
                             PortNumber.portNumber(2));
    private final ConnectPoint of2p1 =
            new ConnectPoint(DeviceId.deviceId("of:2"),
                             PortNumber.portNumber(1));

    private final VlanId vlan100 = VlanId.vlanId("100");
    private final VlanId vlan200 = VlanId.vlanId("200");
    private final VlanId vlan300 = VlanId.vlanId("300");

    private final Interface v100h1 =
            new Interface("v100h1", of1p1, null, null, vlan100);
    private final Interface v100h2 =
            new Interface("v100h2", of4p1, null, null, vlan100);
    private final Interface v200h1 =
            new Interface("v200h1", of4p2, null, null, vlan200);
    private final Interface v300h1 =
            new Interface("v300h1", of3p1, null, null, vlan300);
    private final Interface v200h2 =
            new Interface("v200h2", of2p1, null, null, vlan200);

    private final MacAddress mac1 = MacAddress.valueOf("00:00:00:00:00:01");
    private final MacAddress mac2 = MacAddress.valueOf("00:00:00:00:00:02");
    private final MacAddress mac3 = MacAddress.valueOf("00:00:00:00:00:03");
    private final MacAddress mac4 = MacAddress.valueOf("00:00:00:00:00:04");
    private final MacAddress mac5 = MacAddress.valueOf("00:00:00:00:00:05");

    private final ProviderId pid = new ProviderId("of", "foo");

    private final Host v100host1 = makeHost(mac1, vlan100, of1p1);
    private final Host v100host2 = makeHost(mac2, vlan100, of4p1);
    private final Host v200host1 = makeHost(mac3, vlan200, of4p2);
    private final Host v300host1 = makeHost(mac4, vlan300, of3p1);
    private final Host v200host2 = makeHost(mac5, vlan200, of2p1);

    private final Set<Host> availableHosts = ImmutableSet.of(v100host1,
                                                             v100host2,
                                                             v200host1,
                                                             v300host1,
                                                             v200host2);

    private final Set<Interface> avaliableInterfaces =
            ImmutableSet.of(v100h1, v100h2, v200h1, v200h2, v300h1);

    private VplsNeighbourHandler vplsNeighbourHandler;

    private HostService hostService;

    /**
     * Sets up 2 VPLS which contain 2 networks and 5 hosts.
     * net1 contains 3 hosts: v100h1, v200h1 and v300h1
     * net2 contains 2 hosts: v100h2, v200h2
     */
    @Before
    public void setUp() {
        vplsNeighbourHandler = new VplsNeighbourHandler();
        SetMultimap<String, Interface> vplsNetworks =
                HashMultimap.create();
        vplsNetworks.put("net1", v100h1);
        vplsNetworks.put("net1", v200h1);
        vplsNetworks.put("net1", v300h1);
        vplsNetworks.put("net2", v100h2);
        vplsNetworks.put("net2", v200h2);
        vplsNeighbourHandler.vplsConfigService =
                new TestVplsConfigService(vplsNetworks);
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
     * Sends request messages to all hosts in VPLS net1.
     * Request messages should be received from other hosts in net1.
     */
    @Test
    public void testNet1RequestMessage() {
        // Request from v100h1 (net1)
        // Should be received by v200h1 and v300h1
        TestMessageContext requestMessage =
                makeBroadcastRequestContext(v100host1);
        Set<Interface> expectInterfaces = ImmutableSet.of(v200h1, v300h1);
        vplsNeighbourHandler.handleRequest(requestMessage);
        assertEquals(expectInterfaces, requestMessage.forwardResults);

        // Request from v200h1 (net1)
        // Should be received by v100h1 and v300h1
        requestMessage = makeBroadcastRequestContext(v200host1);
        expectInterfaces = ImmutableSet.of(v100h1, v300h1);
        vplsNeighbourHandler.handleRequest(requestMessage);
        assertEquals(expectInterfaces, requestMessage.forwardResults);

        // Request from v300h1 (net1)
        // Should be received by v100h1 and v200h1
        requestMessage = makeBroadcastRequestContext(v300host1);
        expectInterfaces = ImmutableSet.of(v100h1, v200h1);
        vplsNeighbourHandler.handleRequest(requestMessage);
        assertEquals(expectInterfaces, requestMessage.forwardResults);
    }

    /**
     * Sends request messages to all hosts in VPLS net2.
     * Request messages should be received from other hosts in net2.
     */
    @Test
    public void testNet2RequestMessage() {
        // Request from v100h2
        // Should be received by v200h2
        TestMessageContext requestMessage =
                makeBroadcastRequestContext(v100host2);
        Set<Interface> expectInterfaces = ImmutableSet.of(v200h2);
        vplsNeighbourHandler.handleRequest(requestMessage);
        assertEquals(expectInterfaces, requestMessage.forwardResults);

        // Request from v200h2
        // Should be received by v100h2
        requestMessage = makeBroadcastRequestContext(v200host2);
        expectInterfaces = ImmutableSet.of(v100h2);
        vplsNeighbourHandler.handleRequest(requestMessage);
        assertEquals(expectInterfaces, requestMessage.forwardResults);
    }

    /**
     * Sends reply messages to hosts in VPLS net1.
     * Reply messages should be received by the host with MAC address equal to
     * the dstMac of the message context.
     */
    @Test
    public void testNet1ReplyMessage() {
        // Response from v100h1 (net1) to v200h1 (net1)
        // Should be received by v200h1
        TestMessageContext replyMessage =
                makeReplyContext(v100host1, v200host1);
        Set<Interface> expectInterfaces = ImmutableSet.of(v200h1);
        vplsNeighbourHandler.handleReply(replyMessage, hostService);
        assertEquals(expectInterfaces, replyMessage.forwardResults);

        // Response from v200h1 (net1) to v300h1 (net1)
        // Should be received by v300h1
        replyMessage = makeReplyContext(v200host1, v300host1);
        expectInterfaces = ImmutableSet.of(v300h1);
        vplsNeighbourHandler.handleReply(replyMessage, hostService);
        assertEquals(expectInterfaces, replyMessage.forwardResults);

        // Response from v300h1 (net1) to v100h1 (net1)
        // Should be received by v100h1
        replyMessage = makeReplyContext(v300host1, v100host1);
        expectInterfaces = ImmutableSet.of(v100h1);
        vplsNeighbourHandler.handleReply(replyMessage, hostService);
        assertEquals(expectInterfaces, replyMessage.forwardResults);
    }

    /**
     * Sends reply messages to hosts in VPLS net2.
     * Reply messages should be received by the host with MAC address equal to
     * the dstMac of the message context.
     */
    @Test
    public void testNet2ReplyMessage() {
        // Response from v100h2 (net2) to v200h2 (net2)
        // Should be received by v200h2
        TestMessageContext replyMessage =
                makeReplyContext(v100host2, v200host2);
        Set<Interface> expectInterfaces = ImmutableSet.of(v200h2);
        vplsNeighbourHandler.handleReply(replyMessage, hostService);
        assertEquals(expectInterfaces, replyMessage.forwardResults);

        // Response from v200h2 (net2) to v100h2 (net2)
        // Should be received by v100h2
        replyMessage = makeReplyContext(v200host2, v100host2);
        expectInterfaces = ImmutableSet.of(v100h2);
        vplsNeighbourHandler.handleReply(replyMessage, hostService);
        assertEquals(expectInterfaces, replyMessage.forwardResults);
    }

    /**
     * Sends wrong reply messages to hosts.
     * The source and the destination MAC addresses are not set on any host of the VPLS.
     * The reply messages won't be received by any hosts.
     */
    @Test
    public void testWrongReplyMessage() {
        // Response from v100h1 (net1) to v100h2 (net2)
        // forward results should be empty
        TestMessageContext replyMessage = makeReplyContext(v100host1, v100host2);
        Set<Interface> expectInterfaces = ImmutableSet.of();
        vplsNeighbourHandler.handleReply(replyMessage, hostService);
        assertEquals(expectInterfaces, replyMessage.forwardResults);

        // Response from v200h2 (net2) to v300h1 (net1)
        // forward results should be empty
        replyMessage = makeReplyContext(v200host2, v300host1);
        expectInterfaces = ImmutableSet.of();
        vplsNeighbourHandler.handleReply(replyMessage, hostService);
        assertEquals(expectInterfaces, replyMessage.forwardResults);
    }

    private Host makeHost(MacAddress mac, VlanId vlan, ConnectPoint cp) {
        return new DefaultHost(pid,
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

    private class TestVplsConfigService implements VplsConfigurationService {

        private final SetMultimap<String, Interface> vplsNetworks;

        public TestVplsConfigService(SetMultimap<String, Interface> networks) {
            this.vplsNetworks = networks;
        }

        @Override
        public void addVpls(String name, Set<String> ifaceNames) {
            if (!vplsNetworks.containsKey(name)) {
                ifaceNames.forEach(ifaceName -> {
                    avaliableInterfaces.forEach(intf -> {
                        if (intf.name().equals(ifaceName)) {
                            vplsNetworks.put(name, intf);
                        }
                    });
                });
            }
        }

        @Override
        public void removeVpls(String name) {
            if (vplsNetworks.containsKey(name)) {
                vplsNetworks.removeAll(name);
            }
        }

        @Override
        public void addInterfaceToVpls(String name, String ifaceName) {
            avaliableInterfaces.forEach(intf -> {
                if (intf.name().equals(ifaceName)) {
                    vplsNetworks.put(name, intf);
                }
            });
        }

        @Override
        public void removeInterfaceFromVpls(String ifaceName) {
            SetMultimap<String, Interface> toBeRemoved = HashMultimap.create();
            vplsNetworks.entries().forEach(e -> {
                if (e.getValue().name().equals(ifaceName)) {
                    toBeRemoved.put(e.getKey(), e.getValue());
                }
            });

            toBeRemoved.entries()
                    .forEach(e -> vplsNetworks.remove(e.getKey(),
                                                      e.getValue()));
        }

        @Override
        public void cleanVpls() {
            vplsNetworks.clear();
        }

        @Override
        public Set<String> getVplsAffectedByApi() {
            return null;
        }

        @Override
        public Set<Interface> getAllInterfaces() {
            return ImmutableSet.copyOf(vplsNetworks.values());
        }

        @Override
        public Set<Interface> getVplsInterfaces(String name) {
            return vplsNetworks.get(name)
                    .stream()
                    .collect(Collectors.toSet());
        }

        @Override
        public Set<String> getAllVpls() {
            return vplsNetworks.keySet();
        }

        @Override
        public Set<String> getOldVpls() {
            return null;
        }

        @Override
        public SetMultimap<String, Interface> getVplsNetworks() {
            return ImmutableSetMultimap.copyOf(vplsNetworks);
        }

        @Override
        public SetMultimap<String, Interface> getVplsNetwork(VlanId vlan,
                                                             ConnectPoint connectPoint) {
            String vplsNetworkName =
                    vplsNetworks.entries().stream()
                            .filter(e -> e.getValue().connectPoint().equals(connectPoint))
                            .filter(e -> e.getValue().vlan().equals(vlan))
                            .map(e -> e.getKey())
                            .findFirst()
                            .orElse(null);
            SetMultimap<String, Interface> result = HashMultimap.create();
            if (vplsNetworkName != null &&
                    vplsNetworks.containsKey(vplsNetworkName)) {
                vplsNetworks.get(vplsNetworkName)
                        .forEach(intf -> result.put(vplsNetworkName, intf));
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
            return avaliableInterfaces;
        }

        @Override
        public Interface getInterfaceByName(ConnectPoint connectPoint,
                                            String name) {
            return avaliableInterfaces.stream()
                    .filter(intf -> intf.name().equals(name))
                    .findFirst()
                    .orElse(null);

        }

        @Override
        public Set<Interface> getInterfacesByPort(ConnectPoint port) {
            return avaliableInterfaces.stream()
                    .filter(intf -> intf.connectPoint().equals(port))
                    .collect(Collectors.toSet());
        }

        @Override
        public Set<Interface> getInterfacesByIp(IpAddress ip) {
            return avaliableInterfaces.stream()
                    .filter(intf -> intf.ipAddressesList().contains(ip))
                    .collect(Collectors.toSet());
        }

        @Override
        public Set<Interface> getInterfacesByVlan(VlanId vlan) {
            return avaliableInterfaces.stream()
                    .filter(intf -> intf.vlan().equals(vlan))
                    .collect(Collectors.toSet());
        }

        @Override
        public Interface getMatchingInterface(IpAddress ip) {
            return avaliableInterfaces.stream()
                    .filter(intf -> intf.ipAddressesList().contains(ip))
                    .findFirst()
                    .orElse(null);
        }
    }
}
