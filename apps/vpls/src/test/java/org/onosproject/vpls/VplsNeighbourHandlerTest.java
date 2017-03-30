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
import org.onosproject.incubator.net.neighbour.NeighbourHandlerRegistration;
import org.onosproject.incubator.net.neighbour.NeighbourMessageContext;
import org.onosproject.incubator.net.neighbour.NeighbourMessageHandler;
import org.onosproject.incubator.net.neighbour.NeighbourMessageType;
import org.onosproject.incubator.net.neighbour.NeighbourProtocol;
import org.onosproject.incubator.net.neighbour.NeighbourResolutionService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostService;
import org.onosproject.vpls.api.VplsData;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * Tests the the {@link VplsNeighbourHandler}.
 */
public class VplsNeighbourHandlerTest extends VplsTest {

    private static final String IFACES_NOT_EXPECTED =
            "The interfaces reached by the packet are not equal to the " +
                    "interfaces expected.";
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
        hostService = new TestHostService();
        vplsNeighbourHandler.vplsStore = new TestVplsStore();
        vplsNeighbourHandler.interfaceService = new TestInterfaceService();
        vplsNeighbourHandler.neighbourService = new TestNeighbourService();
        vplsNeighbourHandler.coreService = new TestCoreService();
        vplsNeighbourHandler.configService = new TestConfigService();

        // Init VPLS store
        VplsData vplsData = VplsData.of(VPLS1);
        vplsData.addInterfaces(ImmutableSet.of(V100H1, V200H1, V300H1));
        vplsNeighbourHandler.vplsStore.addVpls(vplsData);

        vplsData = VplsData.of(VPLS2);
        vplsData.addInterfaces(ImmutableSet.of(V100H2, V200H2));
        vplsNeighbourHandler.vplsStore.addVpls(vplsData);

        vplsData = VplsData.of(VPLS3);
        vplsData.addInterfaces(ImmutableSet.of(VNONEH1, VNONEH2));
        vplsNeighbourHandler.vplsStore.addVpls(vplsData);

        vplsData = VplsData.of(VPLS4);
        vplsData.addInterfaces(ImmutableSet.of(V400H1, VNONEH3));
        vplsNeighbourHandler.vplsStore.addVpls(vplsData);

        vplsNeighbourHandler.activate();

    }

    @After
    public void tearDown() {
        vplsNeighbourHandler.deactivate();
    }

    /**
     * Registers neighbour handler to all available interfaces.
     */
    @Test
    public void testConfigNeighbourHandler() {
        vplsNeighbourHandler.configNeighbourHandler();
        assertEquals(9, vplsNeighbourHandler.neighbourService.getHandlerRegistrations().size());
    }

    /**
     * Sends request messages to all hosts in VPLS 1.
     * Request messages should be received from other hosts in VPLS 1.
     */
    @Test
    public void vpls1RequestMessage() {
        // Request messages from v100h1 (VPLS 1) should be received by v200h1 and v300h1
        TestMessageContext requestMessage =
                makeBroadcastRequestContext(V100HOST1);
        Set<Interface> expectInterfaces = ImmutableSet.of(V200H1, V300H1);
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(requestMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, requestMessage.forwardResults);

        // Request messages from v200h1 (VPLS 1) should be received by v100h1 and v300h1
        requestMessage = makeBroadcastRequestContext(V200HOST1);
        expectInterfaces = ImmutableSet.of(V100H1, V300H1);
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(requestMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, requestMessage.forwardResults);

        // Request from v300h1 (VPLS 1) should be received by v100h1 and v200h1
        requestMessage = makeBroadcastRequestContext(V300HOST1);
        expectInterfaces = ImmutableSet.of(V100H1, V200H1);
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(requestMessage);
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
                makeBroadcastRequestContext(V100HOST2);
        Set<Interface> expectInterfaces = ImmutableSet.of(V200H2);
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(requestMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, requestMessage.forwardResults);

        // Request messages from v200h2 (VPLS 2) should be received by v100h2
        requestMessage = makeBroadcastRequestContext(V200HOST2);
        expectInterfaces = ImmutableSet.of(V100H2);
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(requestMessage);
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
        // Request messages from VNONEHOST1 (VPLS 3) should be received by VNONEHOST2
        TestMessageContext requestMessage =
                makeBroadcastRequestContext(VNONEHOST1);
        Set<Interface> expectInterfaces = ImmutableSet.of(VNONEH2);
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(requestMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, requestMessage.forwardResults);

        // Request messages from vNoneh2 (VPLS 3) should be received by vNoneh1
        requestMessage = makeBroadcastRequestContext(VNONEHOST2);
        expectInterfaces = ImmutableSet.of(VNONEH1);
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(requestMessage);
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
        // Request messages from V400HOST1 (VPLS 4) should be received by VNONEHOST3
        TestMessageContext requestMessage =
                makeBroadcastRequestContext(V400HOST1);
        Set<Interface> expectInterfaces = ImmutableSet.of(VNONEH3);
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(requestMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, requestMessage.forwardResults);

        // Request messages from VNONEHOST3 (VPLS 4) should be received by V400HOST1
        requestMessage = makeBroadcastRequestContext(VNONEHOST3);
        expectInterfaces = ImmutableSet.of(V400H1);
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(requestMessage);
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
                makeReplyContext(V100HOST1, V200HOST1);
        Set<Interface> expectInterfaces = ImmutableSet.of(V200H1);
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(replyMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, replyMessage.forwardResults);

        // Reply messages from v200h1 (VPLS 1) should be received by v300h1
        replyMessage = makeReplyContext(V200HOST1, V300HOST1);
        expectInterfaces = ImmutableSet.of(V300H1);
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(replyMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, replyMessage.forwardResults);

        // Reply messages from v300h1 (VPLS 1) should be received by v100h1
        replyMessage = makeReplyContext(V300HOST1, V100HOST1);
        expectInterfaces = ImmutableSet.of(V100H1);
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(replyMessage);
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
                makeReplyContext(V100HOST2, V200HOST2);
        Set<Interface> expectInterfaces = ImmutableSet.of(V200H2);
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(replyMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, replyMessage.forwardResults);

        // Reply messages from v200h2 (VPLS 2) should be received by v100h2
        replyMessage = makeReplyContext(V200HOST2, V100HOST2);
        expectInterfaces = ImmutableSet.of(V100H2);
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(replyMessage);
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
                makeReplyContext(VNONEHOST1, VNONEHOST2);
        Set<Interface> expectInterfaces = ImmutableSet.of(VNONEH2);
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(replyMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, replyMessage.forwardResults);

        // Reply messages from vNoneh2 (VPLS 3) should be received by vNoneh1
        replyMessage = makeReplyContext(VNONEHOST2, VNONEHOST1);
        expectInterfaces = ImmutableSet.of(VNONEH1);
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(replyMessage);
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
                makeReplyContext(V400HOST1, VNONEHOST3);
        Set<Interface> expectInterfaces = ImmutableSet.of(VNONEH3);
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(replyMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, replyMessage.forwardResults);

        // Reply messages from vNoneh3 (VPLS 4) should be received by v400h1
        replyMessage = makeReplyContext(VNONEHOST3, V400HOST1);
        expectInterfaces = ImmutableSet.of(V400H1);
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(replyMessage);
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
        TestMessageContext replyMessage = makeReplyContext(V100HOST1, V100HOST2);
        Set<Interface> expectInterfaces = ImmutableSet.of();
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(replyMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, replyMessage.forwardResults);

        // Reply message from v200h2 (VPLS 2) to v300h1 (VPLS 1).
        // Forward results should be empty
        replyMessage = makeReplyContext(V200HOST2, V300HOST1);
        expectInterfaces = ImmutableSet.of();
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(replyMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, replyMessage.forwardResults);

        // Reply message from vNoneh1 (VPLS 3) to v400h1 (VPLS 4).
        // Forward results should be empty
        replyMessage = makeReplyContext(VNONEHOST1, V400HOST1);
        expectInterfaces = ImmutableSet.of();
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(replyMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, replyMessage.forwardResults);

        // Reply message from vNoneh3 (VPLS 4) to vNoneH2 (VPLS 3).
        // Forward results should be empty
        replyMessage = makeReplyContext(VNONEHOST3, VNONEHOST2);
        expectInterfaces = ImmutableSet.of();
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(replyMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, replyMessage.forwardResults);
    }

    /**
     * Sends reply and request message from a host which not related to any VPLS.
     */
    @Test
    public void testVplsNotfound() {
        TestMessageContext replyMessage = makeReplyContext(V300HOST2, V100HOST1);
        Set<Interface> expectInterfaces = ImmutableSet.of();
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(replyMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, replyMessage.forwardResults);
        assertTrue(replyMessage.dropped());

        TestMessageContext requestMessage = makeBroadcastRequestContext(V300HOST2);
        ((TestNeighbourService) vplsNeighbourHandler.neighbourService).sendNeighourMessage(requestMessage);
        assertEquals(IFACES_NOT_EXPECTED, expectInterfaces, requestMessage.forwardResults);
        assertTrue(requestMessage.dropped());
    }

    /**
     * Generates broadcast request message context by given source host.
     *
     * @param host the source host
     * @return the request message context
     */
    private TestMessageContext makeBroadcastRequestContext(Host host) {
        return new TestMessageContext(host.location(),
                                      host.mac(),
                                      MacAddress.BROADCAST,
                                      host.vlan(),
                                      NeighbourMessageType.REQUEST);
    }

    /**
     * Generates reply message context by given source and destination host.
     *
     * @param src the source host
     * @param dst the destination host
     * @return the reply message context
     */
    private TestMessageContext makeReplyContext(Host src, Host dst) {
        return new TestMessageContext(src.location(),
                                      src.mac(),
                                      dst.mac(),
                                      src.vlan(),
                                      NeighbourMessageType.REPLY);
    }

    /**
     * Test message context.
     */
    private class TestMessageContext implements NeighbourMessageContext {
        private final NeighbourMessageType type;
        private final MacAddress srcMac;
        private final MacAddress dstMac;
        private final ConnectPoint inPort;
        private final VlanId vlanId;
        private boolean dropped = false;
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
            this.dropped = false;
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

        /**
         * Records all forward network interface information.
         * @param outIntf output interface
         */
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
            this.dropped = true;
        }

        @Override
        public Ethernet packet() {
            return null;
        }

        @Override
        public NeighbourProtocol protocol() {
            return null;
        }

        public boolean dropped() {
            return dropped;
        }
    }

    /**
     * Test neighbour service; records all registrations between neighbour
     * message handler and interfaces.
     */
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

        /**
         * Sends neighbour message context to all handler which related to the
         * context.
         *
         * @param context the neighbour message context
         */
        public void sendNeighourMessage(NeighbourMessageContext context) {
            ConnectPoint connectPoint = context.inPort();
            VlanId vlanId = context.vlan();
            Collection<NeighbourHandlerRegistration> registrations = handlerRegs.get(connectPoint);
            registrations.forEach(reg -> {
                if (reg.intf().vlan().equals(vlanId)) {
                    reg.handler().handleMessage(context, hostService);
                }
            });
        }

        /**
         * Test handler registration.
         */
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
}
