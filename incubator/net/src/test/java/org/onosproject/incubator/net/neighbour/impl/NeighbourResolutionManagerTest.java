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

package org.onosproject.incubator.net.neighbour.impl;

import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ComponentContextAdapter;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.neighbour.NeighbourHandlerRegistration;
import org.onosproject.incubator.net.neighbour.NeighbourMessageContext;
import org.onosproject.incubator.net.neighbour.NeighbourMessageHandler;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.host.HostService;
import org.onosproject.net.packet.DefaultInboundPacket;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.InboundPacket;
import org.onosproject.net.packet.PacketContextAdapter;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.packet.PacketServiceAdapter;

import java.util.Collection;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.incubator.net.neighbour.impl.DefaultNeighbourMessageContext.createContext;
import static org.onosproject.incubator.net.neighbour.impl.NeighbourTestUtils.createArpRequest;
import static org.onosproject.incubator.net.neighbour.impl.NeighbourTestUtils.intf;

/**
 * Unit tests for the NeighbourResolutionManager.
 */
public class NeighbourResolutionManagerTest {

    private NeighbourResolutionManager neighbourManager;

    private PacketService packetService;
    private PacketProcessor packetProcessor;

    private static final NeighbourMessageHandler HANDLER = new TestNeighbourHandler();

    private static final ConnectPoint CP1 = ConnectPoint.deviceConnectPoint("of:0000000000000001/1");
    private static final ConnectPoint CP2 = ConnectPoint.deviceConnectPoint("of:0000000000000001/2");

    private static final MacAddress MAC1 = MacAddress.valueOf(1);
    private static final IpAddress IP1 = IpAddress.valueOf(1);
    private static final IpAddress IP2 = IpAddress.valueOf(2);

    private static final Interface INTF1 = intf(CP1, IP1, MAC1, VlanId.NONE);

    private static final ApplicationId APP_ID = TestApplicationId.create("app");

    @Before
    public void setUp() throws Exception {
        neighbourManager = new NeighbourResolutionManager();

        packetService = createMock(PacketService.class);
        packetService.requestPackets(anyObject(TrafficSelector.class),
                anyObject(PacketPriority.class), anyObject(ApplicationId.class));
        expectLastCall().anyTimes();
        packetService.addProcessor(anyObject(PacketProcessor.class), anyInt());
        expectLastCall().andDelegateTo(new TestPacketService()).once();
        packetService.cancelPackets(anyObject(TrafficSelector.class),
                anyObject(PacketPriority.class), anyObject(ApplicationId.class));
        expectLastCall().anyTimes();
        replay(packetService);

        neighbourManager.packetService = packetService;

        CoreService coreService = createNiceMock(CoreService.class);
        replay(coreService);
        neighbourManager.coreService = coreService;

        neighbourManager.componentConfigService = new ComponentConfigAdapter();

        neighbourManager.activate(new ComponentContextAdapter());
    }

    @Test
    public void testRegistration() throws Exception {
        neighbourManager.registerNeighbourHandler(CP1, HANDLER, APP_ID);

        assertTrue(verifyRegistration(CP1, HANDLER, APP_ID));
    }

    @Test
    public void testUnregister() {
        // Register a handler and verify the registration is there
        neighbourManager.registerNeighbourHandler(CP1, HANDLER, APP_ID);
        assertTrue(verifyRegistration(CP1, HANDLER, APP_ID));

        // Unregister the handler but supply a different connect point
        neighbourManager.unregisterNeighbourHandler(CP2, HANDLER, APP_ID);

        // Verify the original registration is still there on the original
        // connect point
        assertTrue(verifyRegistration(CP1, HANDLER, APP_ID));

        assertTrue(verifyNoRegistration(CP2));

        // Unregister the handler from the original connect point
        neighbourManager.unregisterNeighbourHandler(CP1, HANDLER, APP_ID);

        // Verify that it is gone
        assertTrue(verifyNoRegistration(CP1));
    }

    @Test
    public void testRegisterInterface() {
        neighbourManager.registerNeighbourHandler(INTF1, HANDLER, APP_ID);

        assertTrue(verifyRegistration(INTF1, HANDLER, APP_ID));
    }

    @Test
    public void testUnregisterInterface() {
        // Register a handler for an interface and verify it is there
        neighbourManager.registerNeighbourHandler(INTF1, HANDLER, APP_ID);
        assertTrue(verifyRegistration(INTF1, HANDLER, APP_ID));

        // Unregister the handler but use the connect point rather than the interface
        neighbourManager.unregisterNeighbourHandler(CP1, HANDLER, APP_ID);

        // Verify the interface registration is still there
        assertTrue(verifyRegistration(INTF1, HANDLER, APP_ID));

        // Unregister the handler from the interface
        neighbourManager.unregisterNeighbourHandler(INTF1, HANDLER, APP_ID);

        // Verify the registration is gone
        assertTrue(verifyNoRegistration(INTF1));
    }

    @Test
    public void testUnregisterByAppId() {
        // Register some handlers and verify they are there
        neighbourManager.registerNeighbourHandler(CP1, HANDLER, APP_ID);
        neighbourManager.registerNeighbourHandler(CP2, HANDLER, APP_ID);

        assertEquals(2, neighbourManager.getHandlerRegistrations().size());

        // Unregister all handlers for the given app ID
        neighbourManager.unregisterNeighbourHandlers(APP_ID);

        // Verify the handlers are gone
        assertEquals(0, neighbourManager.getHandlerRegistrations().size());
    }

    @Test
    public void testPacketDistribution() {
        Ethernet arpRequest = createArpRequest(IP1);

        NeighbourMessageHandler handler = createMock(NeighbourMessageHandler.class);
        handler.handleMessage(eq(createContext(arpRequest, CP1, null)), anyObject(HostService.class));
        expectLastCall().once();
        replay(handler);
        neighbourManager.registerNeighbourHandler(CP1, handler, APP_ID);

        // Incoming packet on the connect point where the handler is registered
        packetProcessor.process(context(arpRequest, CP1));

        // Send a packet from a different connect point that should not be
        // delivered to the handler
        packetProcessor.process(context(arpRequest, CP2));

        verify(handler);
    }

    @Test
    public void testPacketDistributionToInterface() {
        Ethernet arpRequest = createArpRequest(IP1);

        NeighbourMessageHandler handler = createMock(NeighbourMessageHandler.class);
        handler.handleMessage(eq(createContext(arpRequest, CP1, null)), anyObject(HostService.class));
        expectLastCall().once();
        replay(handler);
        neighbourManager.registerNeighbourHandler(INTF1, handler, APP_ID);

        // Incoming packet matching the interface where the handler is registered
        packetProcessor.process(context(arpRequest, CP1));

        verify(handler);

        reset(handler);
        replay(handler);

        // Incoming packet on same connect point but not matching the interface
        packetProcessor.process(context(createArpRequest(IP2), CP1));

        verify(handler);
    }

    /**
     * Verifies that there is one registration for the given connect point and
     * that the registration matches the given handler and appId.
     *
     * @param cp connect point to verify registration for
     * @param handler neighbour message handler
     * @param appId application ID
     * @return true if the registration is the only registration present for
     * this connect point, otherwise false
     */
    private boolean verifyRegistration(ConnectPoint cp, NeighbourMessageHandler handler, ApplicationId appId) {
        Collection<NeighbourHandlerRegistration> registrations =
                neighbourManager.getHandlerRegistrations().get(cp);

        if (registrations == null) {
            return false;
        }

        if (registrations.size() != 1) {
            return false;
        }

        NeighbourHandlerRegistration reg = registrations.stream().findFirst().get();

        return reg.appId().equals(appId) &&
                reg.handler().equals(handler);
    }

    /**
     * Verifies that there is one registration for the given interface and
     * that the registration matches the given handler and appId.
     *
     * @param intf interface to verify registration for
     * @param handler neighbour message handler
     * @param appId application ID
     * @return true if the registration is the only registration present for
     * this interface, otherwise false
     */
    private boolean verifyRegistration(Interface intf, NeighbourMessageHandler handler, ApplicationId appId) {
        return verifyRegistration(intf.connectPoint(), handler, appId);
    }

    /**
     * Verifies that there are no registrations for the given connect point.
     *
     * @param cp connect point
     * @return true if there are no registrations for this connect point,
     * otherwise false
     */
    private boolean verifyNoRegistration(ConnectPoint cp) {
        return neighbourManager.getHandlerRegistrations().get(cp) == null;
    }

    /**
     * Verifies that there are no registrations for the given interface.
     *
     * @param intf interface
     * @return true if there are no registrations for this interface,
     * otherwise false
     */
    private boolean verifyNoRegistration(Interface intf) {
        return verifyNoRegistration(intf.connectPoint());
    }

    /**
     * Creates a packet context for the given packet coming in the given port.
     *
     * @param packet packet to wrap in a packet context
     * @param inPort input port of the packet
     * @return packet context
     */
    private static PacketContext context(Ethernet packet, ConnectPoint inPort) {
        InboundPacket inboundPacket = new DefaultInboundPacket(inPort, packet, null);
        OutboundPacket outboundPacket = new DefaultOutboundPacket(null, null, null);
        return new PacketContextAdapter(0, inboundPacket, outboundPacket, false);
    }

    private class TestPacketService extends PacketServiceAdapter {

        @Override
        public void addProcessor(PacketProcessor processor, int priority) {
            NeighbourResolutionManagerTest.this.packetProcessor = processor;
        }
    }

    private static class TestNeighbourHandler implements NeighbourMessageHandler {

        @Override
        public void handleMessage(NeighbourMessageContext context, HostService hostService) {

        }
    }

}
