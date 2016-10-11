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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.edge.EdgePortServiceAdapter;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketService;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.onosproject.incubator.net.neighbour.impl.DefaultNeighbourMessageContext.createContext;
import static org.onosproject.incubator.net.neighbour.impl.NeighbourTestUtils.createArpRequest;
import static org.onosproject.incubator.net.neighbour.impl.NeighbourTestUtils.intf;

/**
 * Unit tests for DefaultNeighbourMessageActions.
 */
public class DefaultNeighbourMessageActionsTest {

    private static final ConnectPoint CP1 = ConnectPoint.deviceConnectPoint("of:0000000000000001/1");
    private static final ConnectPoint CP2 = ConnectPoint.deviceConnectPoint("of:0000000000000001/2");
    private static final ConnectPoint CP3 = ConnectPoint.deviceConnectPoint("of:0000000000000002/1");

    private static final List<ConnectPoint> EDGE_PORTS =
            ImmutableList.<ConnectPoint>builder()
                    .add(CP1)
                    .add(CP2)
                    .add(CP3)
                    .build();

    private static final MacAddress MAC1 = MacAddress.valueOf(1);
    private static final MacAddress MAC2 = MacAddress.valueOf(2);
    private static final IpAddress IP1 = IpAddress.valueOf(1);
    private static final IpAddress IP2 = IpAddress.valueOf(2);

    private static final VlanId VLAN1 = VlanId.vlanId((short) 1);

    private static final Interface INTF1 = intf(CP1, IP1, MAC1, VLAN1);
    private static final Interface INTF2 = intf(CP2, IP2, MAC2, VLAN1);

    private DefaultNeighbourMessageActions actions;
    private PacketService packetService;

    @Before
    public void setUp() throws Exception {
        packetService = createMock(PacketService.class);
        actions = new DefaultNeighbourMessageActions(packetService, new TestEdgeService());
    }

    @Test
    public void reply() throws Exception {
        Ethernet request = createArpRequest(IP1);

        Ip4Address ip4Address = INTF1.ipAddressesList().get(0).ipAddress().getIp4Address();
        Ethernet response = ARP.buildArpReply(ip4Address, MAC2, request);

        packetService.emit(outbound(response, CP1));
        expectLastCall().once();
        replay(packetService);

        actions.reply(createContext(request, CP1, null), MAC2);

        verify(packetService);
    }

    @Test
    public void forwardToConnectPoint() {
        Ethernet request = createArpRequest(IP1);

        packetService.emit(outbound(request, CP2));
        expectLastCall().once();
        replay(packetService);

        actions.forward(createContext(request, CP1, null), CP2);

        verify(packetService);
    }

    @Test
    public void forwardToInterface() {
        Ethernet request = createArpRequest(IP1);

        Ethernet forwardedRequest = (Ethernet) request.clone();
        forwardedRequest.setSourceMACAddress(INTF2.mac());
        forwardedRequest.setVlanID(INTF2.vlan().toShort());

        packetService.emit(outbound(forwardedRequest, CP2));
        expectLastCall().once();
        replay(packetService);

        actions.forward(createContext(request, CP1, null), INTF2);

        verify(packetService);
    }

    @Test
    public void flood() {
        Ethernet request = createArpRequest(IP1);

        // Expect the packet to be emitted out all ports apart from the in port
        Sets.difference(Sets.newLinkedHashSet(EDGE_PORTS), Collections.singleton(CP1))
                .forEach(cp -> {
                    packetService.emit(outbound(request, cp));
                    expectLastCall().once();
                });
        replay(packetService);

        actions.flood(createContext(request, CP1, null));

        verify(packetService);
    }

    private static OutboundPacket outbound(Ethernet packet, ConnectPoint outPort) {
        TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(outPort.port()).build();
        return new DefaultOutboundPacket(outPort.deviceId(),
                treatment, ByteBuffer.wrap(packet.serialize()));
    }

    private class TestEdgeService extends EdgePortServiceAdapter {

        @Override
        public boolean isEdgePoint(ConnectPoint point) {
            return true;
        }

        @Override
        public Iterable<ConnectPoint> getEdgePoints() {
            return EDGE_PORTS;
        }

    }

}
