package org.onlab.onos.provider.of.packet.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.DefaultTrafficTreatment;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.flow.instructions.Instruction;
import org.onlab.onos.net.flow.instructions.Instructions;
import org.onlab.onos.net.packet.DefaultOutboundPacket;
import org.onlab.onos.net.packet.OutboundPacket;
import org.onlab.onos.net.packet.PacketContext;
import org.onlab.onos.net.packet.PacketProvider;
import org.onlab.onos.net.packet.PacketProviderRegistry;
import org.onlab.onos.net.packet.PacketProviderService;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.openflow.controller.DefaultOpenFlowPacketContext;
import org.onlab.onos.openflow.controller.Dpid;
import org.onlab.onos.openflow.controller.OpenFlowController;
import org.onlab.onos.openflow.controller.OpenFlowEventListener;
import org.onlab.onos.openflow.controller.OpenFlowPacketContext;
import org.onlab.onos.openflow.controller.OpenFlowSwitch;
import org.onlab.onos.openflow.controller.OpenFlowSwitchListener;
import org.onlab.onos.openflow.controller.PacketListener;
import org.onlab.onos.openflow.controller.RoleState;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpAddress;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketInReason;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.ver10.OFFactoryVer10;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;


public class OpenFlowPacketProviderTest {

    private static final int PN1 = 100;
    private static final int PN2 = 200;
    private static final int PN3 = 300;
    private static final short VLANID = (short) 100;

    private static final DeviceId DID = DeviceId.deviceId("of:1");
    private static final DeviceId DID_MISSING = DeviceId.deviceId("of:2");
    private static final DeviceId DID_WRONG = DeviceId.deviceId("test:1");
    private static final PortNumber P1 = PortNumber.portNumber(PN1);
    private static final PortNumber P2 = PortNumber.portNumber(PN2);
    private static final PortNumber P3 = PortNumber.portNumber(PN3);

    private static final Instruction INST1 = Instructions.createOutput(P1);
    private static final Instruction INST2 = Instructions.createOutput(P2);
    private static final Instruction INST3 = Instructions.createOutput(P3);

    private static final OFPortDesc PD1 = portDesc(PN1);
    private static final OFPortDesc PD2 = portDesc(PN2);

    private static final List<OFPortDesc> PLIST = Lists.newArrayList(PD1, PD2);
    private static final TrafficTreatment TR = treatment(INST1, INST2);
    private static final TrafficTreatment TR_MISSING = treatment(INST1, INST3);

    private final OpenFlowPacketProvider provider = new OpenFlowPacketProvider();
    private final TestPacketRegistry registry = new TestPacketRegistry();
    private final TestController controller = new TestController();

    private final TestOpenFlowSwitch sw = new TestOpenFlowSwitch(PLIST);

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
        arp.setSenderProtocolAddress(IpAddress.ANY)
        .setSenderHardwareAddress(mac1.getBytes())
        .setTargetHardwareAddress(mac2.getBytes())
        .setTargetProtocolAddress(IpAddress.ANY)
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

        //wrong Role
        sw.setRole(RoleState.SLAVE);
        provider.emit(passPkt);
        assertEquals("invalid switch", sw, controller.current);
        assertEquals("message sent incorrectly", 0, sw.sent.size());

        sw.setRole(RoleState.MASTER);

        //missing switch
        OutboundPacket swFailPkt = outPacket(DID_MISSING, TR, eth);
        provider.emit(swFailPkt);
        assertNull("invalid switch", controller.current);
        assertEquals("message sent incorrectly", 0, sw.sent.size());

        //to missing port
        OutboundPacket portFailPkt = outPacket(DID, TR_MISSING, eth);
        provider.emit(portFailPkt);
        assertEquals("extra message sent", 1, sw.sent.size());

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

    private static TrafficTreatment treatment(Instruction ... insts) {
        TrafficTreatment.Builder builder = new DefaultTrafficTreatment.Builder();
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
        public void processPacket(Dpid dpid, OFMessage msg) {
            OpenFlowPacketContext pktCtx =
                    DefaultOpenFlowPacketContext.
                    packetContextFromPacketIn(sw, (OFPacketIn) msg);
            pktListener.handlePacket(pktCtx);
        }

        @Override
        public void setRole(Dpid dpid, RoleState role) {
        }

    }

    private class TestOpenFlowSwitch implements OpenFlowSwitch {

        List<OFPortDesc> ports;
        RoleState state;
        List<OFMessage> sent = new ArrayList<OFMessage>();
        OFFactory factory = OFFactoryVer10.INSTANCE;

        TestOpenFlowSwitch(List<OFPortDesc> p) {
            ports = p;
        }

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
            return ports;
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
        public String manfacturerDescription() {
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
        public void disconnectSwitch() {
        }

    }

}
