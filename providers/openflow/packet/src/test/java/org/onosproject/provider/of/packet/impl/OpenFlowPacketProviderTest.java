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
package org.onosproject.provider.of.packet.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.packet.DefaultOutboundPacket;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketProvider;
import org.onosproject.net.packet.PacketProviderRegistry;
import org.onosproject.net.packet.PacketProviderService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.openflow.controller.DefaultOpenFlowPacketContext;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowClassifierListener;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowEventListener;
import org.onosproject.openflow.controller.OpenFlowMessageListener;
import org.onosproject.openflow.controller.OpenFlowPacketContext;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.OpenFlowSwitchListener;
import org.onosproject.openflow.controller.PacketListener;
import org.onosproject.openflow.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFMeterFeatures;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketInReason;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.ver10.OFFactoryVer10;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.*;


public class OpenFlowPacketProviderTest {

    private static final int PN1 = 100;
    private static final int PN2 = 200;
    private static final int PN3 = 300;
    private static final short VLANID = (short) 100;
    private static final int IN_PORT_PN = 1;

    private static final DeviceId DID = DeviceId.deviceId("of:1");
    private static final DeviceId DID_MISSING = DeviceId.deviceId("of:2");
    private static final DeviceId DID_WRONG = DeviceId.deviceId("test:1");
    private static final PortNumber P1 = PortNumber.portNumber(PN1);
    private static final PortNumber P2 = PortNumber.portNumber(PN2);
    private static final PortNumber P3 = PortNumber.portNumber(PN3);
    private static final PortNumber IN_PORT = PortNumber.portNumber(IN_PORT_PN);

    private static final Instruction INST1 = Instructions.createOutput(P1);
    private static final Instruction INST2 = Instructions.createOutput(P2);
    private static final Instruction INST3 = Instructions.createOutput(P3);
    private static final Instruction INST_ALL = Instructions.createOutput(PortNumber.ALL);

    private static final OFPortDesc PD1 = portDesc(PN1);
    private static final OFPortDesc PD2 = portDesc(PN2);
    private static final OFPortDesc PD_ALL = portDesc((int) PortNumber.ALL.toLong());

    private static final List<OFPortDesc> PLIST = Lists.newArrayList(PD1, PD2);
    private static final TrafficTreatment TR = treatment(INST1, INST2);
    private static final List<OFPortDesc> PLIST_ALL = Lists.newArrayList(PD_ALL);
    private static final TrafficTreatment TR_ALL = treatment(INST_ALL);
    private static final TrafficTreatment TR_MISSING = treatment(INST1, INST3);

    private static final byte[] ANY = new byte[] {0, 0, 0, 0};

    private final OpenFlowPacketProvider provider = new OpenFlowPacketProvider();
    private final TestPacketRegistry registry = new TestPacketRegistry();
    private final TestController controller = new TestController();

    private final TestOpenFlowSwitch sw = new TestOpenFlowSwitch();

    @Before
    public void startUp() {
        provider.providerRegistry = registry;
        provider.controller = controller;
        provider.activate();
        assertNotNull("listener should be registered", registry.listener);
    }

    @After
    public void teardown() {
        provider.deactivate();
        assertNull("listeners shouldn't be registered", registry.listener);
        provider.controller = null;
        provider.providerRegistry = null;
    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongScheme() {
        sw.setRole(RoleState.MASTER);
        OutboundPacket schemeFailPkt = outPacket(DID_WRONG, TR, null);
        provider.emit(schemeFailPkt);
        assertEquals("message sent incorrectly", 0, sw.sent.size());
    }

    @Test
    public void emit() {

        MacAddress mac1 = MacAddress.of("00:00:00:11:00:01");
        MacAddress mac2 = MacAddress.of("00:00:00:22:00:02");

        ARP arp = new ARP();
        arp.setSenderProtocolAddress(ANY)
        .setSenderHardwareAddress(mac1.getBytes())
        .setTargetHardwareAddress(mac2.getBytes())
        .setTargetProtocolAddress(ANY)
        .setHardwareType((short) 0)
        .setProtocolType((short) 0)
        .setHardwareAddressLength((byte) 6)
        .setProtocolAddressLength((byte) 4)
        .setOpCode((byte) 0);

        Ethernet eth = new Ethernet();
        eth.setVlanID(VLANID)
        .setEtherType(Ethernet.TYPE_ARP)
        .setSourceMACAddress("00:00:00:11:00:01")
        .setDestinationMACAddress("00:00:00:22:00:02")
        .setPayload(arp);

        //the should-be working setup.
        OutboundPacket passPkt = outPacket(DID, TR, eth);
        sw.setRole(RoleState.MASTER);
        provider.emit(passPkt);
        assertEquals("invalid switch", sw, controller.current);
        assertEquals("message not sent", PLIST.size(), sw.sent.size());
        sw.sent.clear();

        //Send with different IN_PORT
        OutboundPacket inPortPkt = outPacket(DID, TR_ALL, eth, IN_PORT);
        sw.setRole(RoleState.MASTER);
        provider.emit(inPortPkt);
        assertEquals("invalid switch", sw, controller.current);
        assertEquals("message not sent", PLIST_ALL.size(), sw.sent.size());
        OFMessage ofMessage = sw.sent.get(0);
        assertEquals("Wrong OF message type", OFType.PACKET_OUT, ofMessage.getType());
        OFPacketOut packetOut = (OFPacketOut) ofMessage;
        assertEquals("Wrong in port", OFPort.of(IN_PORT_PN), packetOut.getInPort());
        assertEquals("Unexpected number of actions", 1, packetOut.getActions().size());
        OFAction ofAction = packetOut.getActions().get(0);
        assertEquals("Packet out action should be type output", OFActionType.OUTPUT, ofAction.getType());
        OFActionOutput ofActionOutput = (OFActionOutput) ofAction;
        assertEquals("Output should be ALL", OFPort.ALL, ofActionOutput.getPort());
        sw.sent.clear();

        //wrong Role
        //sw.setRole(RoleState.SLAVE);
        //provider.emit(passPkt);
        //assertEquals("invalid switch", sw, controller.current);
        //assertEquals("message sent incorrectly", 0, sw.sent.size());

        //sw.setRole(RoleState.MASTER);

        //missing switch
        OutboundPacket swFailPkt = outPacket(DID_MISSING, TR, eth);
        provider.emit(swFailPkt);
        assertNull("invalid switch", controller.current);
        assertEquals("message sent incorrectly", 0, sw.sent.size());

        //to missing port
        //OutboundPacket portFailPkt = outPacket(DID, TR_MISSING, eth);
        //provider.emit(portFailPkt);
        //assertEquals("extra message sent", 1, sw.sent.size());

    }

    @Test
    public void handlePacket() {
        OFPacketIn pkt = sw.factory().buildPacketIn()
                .setBufferId(OFBufferId.NO_BUFFER)
                .setInPort(OFPort.NO_MASK)
                .setReason(OFPacketInReason.INVALID_TTL)
                .build();

        controller.processPacket(null, pkt);
        assertNotNull("message unprocessed", registry.ctx);

    }

    private static OFPortDesc portDesc(int port) {
        OFPortDesc.Builder builder = OFFactoryVer10.INSTANCE.buildPortDesc();
        builder.setPortNo(OFPort.of(port));

        return builder.build();
    }

    private static TrafficTreatment treatment(Instruction... insts) {
        TrafficTreatment.Builder builder = DefaultTrafficTreatment.builder();
        for (Instruction i : insts) {
            builder.add(i);
        }
        return builder.build();
    }

    private static OutboundPacket outPacket(
            DeviceId d, TrafficTreatment t, Ethernet e) {
        ByteBuffer buf = null;
        if (e != null) {
            buf = ByteBuffer.wrap(e.serialize());
        }
        return new DefaultOutboundPacket(d, t, buf);
    }

    private OutboundPacket outPacket(DeviceId d, TrafficTreatment t, Ethernet e,
                                     PortNumber inPort) {
        ByteBuffer buf = null;
        if (e != null) {
            buf = ByteBuffer.wrap(e.serialize());
        }
        return new DefaultOutboundPacket(d, t, buf, inPort);
    }


    private class TestPacketRegistry implements PacketProviderRegistry {

        PacketProvider listener = null;
        PacketContext ctx = null;

        @Override
        public PacketProviderService register(PacketProvider provider) {
            listener = provider;
            return new TestPacketProviderService();
        }

        @Override
        public void unregister(PacketProvider provider) {
            listener = null;
        }

        @Override
        public Set<ProviderId> getProviders() {
            return Sets.newHashSet(listener.id());
        }

        private class TestPacketProviderService implements PacketProviderService {

            @Override
            public PacketProvider provider() {
                return null;
            }

            @Override
            public void processPacket(PacketContext context) {
                ctx = context;
            }

        }
    }

    private class TestController implements OpenFlowController {

        PacketListener pktListener;
        OpenFlowSwitch current;

        @Override
        public Iterable<OpenFlowSwitch> getSwitches() {
            return null;
        }

        @Override
        public Iterable<OpenFlowSwitch> getMasterSwitches() {
            return null;
        }

        @Override
        public Iterable<OpenFlowSwitch> getEqualSwitches() {
            return null;
        }

        @Override
        public OpenFlowSwitch getSwitch(Dpid dpid) {
            if (dpid.equals(Dpid.dpid(DID.uri()))) {
                current = sw;
            } else {
                current = null;
            }
            return current;
        }

        @Override
        public OpenFlowSwitch getMasterSwitch(Dpid dpid) {
            return null;
        }

        @Override
        public OpenFlowSwitch getEqualSwitch(Dpid dpid) {
            return null;
        }

        @Override
        public void addListener(OpenFlowSwitchListener listener) {
        }

        @Override
        public void removeListener(OpenFlowSwitchListener listener) {
        }

        @Override
        public void addMessageListener(OpenFlowMessageListener listener) {

        }

        @Override
        public void removeMessageListener(OpenFlowMessageListener listener) {

        }

        @Override
        public void addPacketListener(int priority, PacketListener listener) {
            pktListener = listener;
        }

        @Override
        public void removePacketListener(PacketListener listener) {
        }

        @Override
        public void addEventListener(OpenFlowEventListener listener) {
        }

        @Override
        public void removeEventListener(OpenFlowEventListener listener) {
        }

        @Override
        public void write(Dpid dpid, OFMessage msg) {
        }

        @Override
        public CompletableFuture<OFMessage> writeResponse(Dpid dpid, OFMessage msg) {
            return null;
        }

        @Override
        public void processPacket(Dpid dpid, OFMessage msg) {
            OpenFlowPacketContext pktCtx =
                    DefaultOpenFlowPacketContext.
                    packetContextFromPacketIn(sw, (OFPacketIn) msg);
            pktListener.handlePacket(pktCtx);
        }

        @Override
        public void setRole(Dpid dpid, RoleState role) {
        }

        @Override
        public void removeClassifierListener(OpenFlowClassifierListener listener) {
        }

        @Override
        public void addClassifierListener(OpenFlowClassifierListener listener) {
        }

    }

    private class TestOpenFlowSwitch implements OpenFlowSwitch {

        RoleState state;
        List<OFMessage> sent = new ArrayList<>();
        OFFactory factory = OFFactoryVer10.INSTANCE;

        @Override
        public void sendMsg(OFMessage msg) {
            sent.add(msg);
        }

        @Override
        public void sendMsg(List<OFMessage> msgs) {
        }

        @Override
        public void handleMessage(OFMessage fromSwitch) {
        }

        @Override
        public void setRole(RoleState role) {
            state = role;
        }

        @Override
        public RoleState getRole() {
            return state;
        }

        @Override
        public List<OFPortDesc> getPorts() {
            return PLIST;
        }

        @Override
        public OFMeterFeatures getMeterFeatures() {
            return null;
        }

        @Override
        public OFFactory factory() {
            return factory;
        }

        @Override
        public String getStringId() {
            return null;
        }

        @Override
        public long getId() {
            return 0;
        }

        @Override
        public String manufacturerDescription() {
            return null;
        }

        @Override
        public String datapathDescription() {
            return null;
        }

        @Override
        public String hardwareDescription() {
            return null;
        }

        @Override
        public String softwareDescription() {
            return null;
        }

        @Override
        public String serialNumber() {
            return null;
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public void disconnectSwitch() {
        }

        @Override
        public void returnRoleReply(RoleState requested, RoleState reponse) {
        }

        @Override
        public Device.Type deviceType() {
            return Device.Type.SWITCH;
        }

        @Override
        public String channelId() {
            return "1.2.3.4:1";
        }
    }

}
