/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.provider.of.link.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.link.LinkProvider;
import org.onosproject.net.link.LinkProviderRegistry;
import org.onosproject.net.link.LinkProviderService;
import org.onosproject.net.provider.AbstractProviderService;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowPacketContext;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.OpenFlowSwitchListener;
import org.onosproject.openflow.controller.OpenflowControllerAdapter;
import org.onosproject.openflow.controller.PacketListener;
import org.onosproject.openflow.controller.RoleState;
import org.onlab.packet.Ethernet;
import org.onlab.packet.ONLabLddp;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortConfig;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortReason;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.ver10.OFFactoryVer10;
import org.projectfloodlight.openflow.types.OFPort;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class OpenFlowLinkProviderTest {


    private static final DeviceId DID1 = DeviceId.deviceId("of:0000000000000001");
    private static final DeviceId DID2 = DeviceId.deviceId("of:0000000000000002");

    private static final Dpid DPID2 = Dpid.dpid(DID2.uri());
    private static final Dpid DPID1 = Dpid.dpid(DID1.uri());

    private static final OFPortDesc PD1 = portDesc(1, true);
    private static final OFPortDesc PD2 = portDesc(2, true);
    private static final OFPortDesc PD3 = portDesc(1, true);
    private static final OFPortDesc PD4 = portDesc(2, true);

    private static final List<OFPortDesc> PLIST1 = Lists.newArrayList(PD1, PD2);
    private static final List<OFPortDesc> PLIST2 = Lists.newArrayList(PD3, PD4);

    private static final TestOpenFlowSwitch SW1 = new TestOpenFlowSwitch(DPID1, PLIST1);
    private static final TestOpenFlowSwitch SW2 = new TestOpenFlowSwitch(DPID2, PLIST2);

    private final OpenFlowLinkProvider provider = new OpenFlowLinkProvider();
    private final TestLinkRegistry linkService = new TestLinkRegistry();
    private final TestController controller = new TestController();

    private TestLinkProviderService providerService;
    private TestPacketContext pktCtx;

    @Before
    public void setUp() {
        pktCtx = new TestPacketContext(DPID2);
        provider.providerRegistry = linkService;
        controller.switchMap.put(DPID1, SW1);
        controller.switchMap.put(DPID2, SW2);
        provider.controller = controller;
        provider.activate();
    }

    @Test
    public void basics() {
        assertNotNull("registration expected", providerService);
        assertEquals("incorrect provider", provider, providerService.provider());
    }

    @Test
    public void switchAdd() {
        controller.listener.switchAdded(DPID1);
        assertFalse("Device not added", provider.discoverers.isEmpty());
    }

    @Test
    public void switchRemove() {
        controller.listener.switchAdded(DPID1);
        controller.listener.switchRemoved(DPID1);

        assertTrue("Discoverer is not gone", provider.discoverers.isEmpty());
        assertTrue("Device is not gone.", vanishedDpid(DPID1));
    }

    @Test
    public void portUp() {
        controller.listener.switchAdded(DPID1);
        controller.listener.portChanged(DPID1, portStatus(true, 3));

        assertTrue("Port not added to discoverer",
                provider.discoverers.get(DPID1).ports.containsKey(3));
    }

    @Test
    public void portDown() {
        controller.listener.switchAdded(DPID1);
        controller.listener.portChanged(DPID1, portStatus(false, 1));

        assertFalse("Port added to discoverer",
                provider.discoverers.get(DPID1).ports.containsKey(1));
        assertTrue("Port is not gone.", vanishedPort((long) 1));
    }

    @Test
    public void portUnknown() {
        controller.listener.switchAdded(DPID1);
        controller.listener.portChanged(DPID2, portStatus(false, 1));

        assertNull("DPID exists",
                provider.discoverers.get(DPID2));
    }

    @Test
    public void unknownPktCtx() {
        controller.pktListener.handlePacket(pktCtx);

        assertFalse("Context should still be free", pktCtx.isHandled());
    }

    @Test
    public void knownPktCtx() {
        controller.listener.switchAdded(DPID1);
        controller.listener.switchAdded(DPID2);

        controller.pktListener.handlePacket(pktCtx);

        assertTrue("Link not detected", detectedLink(DPID1, DPID2));

    }


    @After
    public void tearDown() {
        provider.deactivate();
        provider.providerRegistry = null;
        provider.controller = null;
    }

    private OFPortStatus portStatus(boolean up, int port) {
        OFPortDesc desc = portDesc(port, up);
        OFPortStatus status = OFFactoryVer10.INSTANCE.buildPortStatus()
                .setDesc(desc)
                .setReason(up ? OFPortReason.ADD : OFPortReason.DELETE).build();
        return status;

    }

    private static OFPortDesc portDesc(int port, boolean up) {
        OFPortDesc.Builder builder = OFFactoryVer10.INSTANCE.buildPortDesc();
        builder.setPortNo(OFPort.of(port));
        if (!up) {
            builder.setConfig(Collections.singleton(OFPortConfig.PORT_DOWN));
        }
        return builder.build();
    }

    private boolean vanishedDpid(Dpid... dpids) {
        for (int i = 0; i < dpids.length; i++) {
            if (!providerService.vanishedDpid.contains(dpids[i])) {
                return false;
            }
        }
        return true;
    }

    private boolean vanishedPort(Long... ports) {
        for (int i = 0; i < ports.length; i++) {
            if (!providerService.vanishedPort.contains(ports[i])) {
                return false;
            }
        }
        return true;
    }

    private boolean detectedLink(Dpid src, Dpid dst) {
        for (Dpid key : providerService.discoveredLinks.keySet()) {
            if (key.equals(src)) {
                return providerService.discoveredLinks.get(src).equals(dst);
            }
        }
        return false;
    }


    private class TestLinkRegistry implements LinkProviderRegistry {

        @Override
        public LinkProviderService register(LinkProvider provider) {
            providerService = new TestLinkProviderService(provider);
            return providerService;
        }

        @Override
        public void unregister(LinkProvider provider) {
        }

        @Override
        public Set<ProviderId> getProviders() {
            return null;
        }

    }

    private class TestLinkProviderService
    extends AbstractProviderService<LinkProvider>
    implements LinkProviderService {

        List<Dpid> vanishedDpid = Lists.newLinkedList();
        List<Long> vanishedPort = Lists.newLinkedList();
        Map<Dpid, Dpid> discoveredLinks = Maps.newHashMap();

        protected TestLinkProviderService(LinkProvider provider) {
            super(provider);
        }

        @Override
        public void linkDetected(LinkDescription linkDescription) {
            Dpid sDpid = Dpid.dpid(linkDescription.src().deviceId().uri());
            Dpid dDpid = Dpid.dpid(linkDescription.dst().deviceId().uri());
            discoveredLinks.put(sDpid, dDpid);
        }

        @Override
        public void linkVanished(LinkDescription linkDescription) {
        }

        @Override
        public void linksVanished(ConnectPoint connectPoint) {
            vanishedPort.add(connectPoint.port().toLong());

        }

        @Override
        public void linksVanished(DeviceId deviceId) {
            vanishedDpid.add(Dpid.dpid(deviceId.uri()));
        }


    }

    private class TestController extends OpenflowControllerAdapter {
        PacketListener pktListener;
        OpenFlowSwitchListener listener;
        Map<Dpid, OpenFlowSwitch> switchMap = new HashMap<Dpid, OpenFlowSwitch>();

        @Override
        public void addPacketListener(int priority, PacketListener listener) {
            pktListener = listener;
        }

        @Override
        public void removePacketListener(PacketListener listener) {
            pktListener = null;
        }

        @Override
        public void addListener(OpenFlowSwitchListener listener) {
            this.listener = listener;
        }

        @Override
        public void removeListener(OpenFlowSwitchListener listener) {
            this.listener = null;
        }

        @Override
        public void processPacket(Dpid dpid, OFMessage msg) {
            OpenFlowPacketContext ctx = new TestPacketContext(dpid);
            pktListener.handlePacket(ctx);
        }

        @Override
        public Iterable<OpenFlowSwitch> getSwitches() {
            return Collections.emptyList();
        }

        @Override
        public OpenFlowSwitch getSwitch(Dpid dpid) {
            return switchMap.get(dpid);
        }
    }



    private class TestPacketContext implements OpenFlowPacketContext {

        protected Dpid swid;
        protected boolean blocked = false;

        public TestPacketContext(Dpid dpid) {
            swid = dpid;
        }

        @Override
        public boolean block() {
            blocked = true;
            return blocked;
        }

        @Override
        public void send() {
        }

        @Override
        public void build(OFPort outPort) {
        }

        @Override
        public void build(Ethernet ethFrame, OFPort outPort) {
        }

        @Override
        public Ethernet parsed() {
            return null;
        }

        @Override
        public byte[] unparsed() {
            ONLabLddp lldp = new ONLabLddp();
            lldp.setSwitch(DPID1.value());

            Ethernet ethPacket = new Ethernet();
            ethPacket.setEtherType(Ethernet.TYPE_LLDP);
            ethPacket.setDestinationMACAddress(ONLabLddp.LLDP_NICIRA);
            ethPacket.setPayload(lldp);
            ethPacket.setPad(true);


            lldp.setPort(PD1.getPortNo().getPortNumber());
            ethPacket.setSourceMACAddress(PD1.getHwAddr().getBytes());
            return ethPacket.serialize();

        }

        @Override
        public Dpid dpid() {
            return swid;
        }

        @Override
        public Integer inPort() {
            return PD3.getPortNo().getPortNumber();
        }

        @Override
        public boolean isHandled() {
            return blocked;
        }

        @Override
        public boolean isBuffered() {
            return false;
        }

    }

    private static class TestOpenFlowSwitch implements OpenFlowSwitch {

        private final List<OFPortDesc> ports;
        private final Dpid dpid;

        public TestOpenFlowSwitch(Dpid dpid, List<OFPortDesc> ports) {
            this.ports = ports;
            this.dpid = dpid;
        }

        RoleState state;
        List<OFMessage> sent = new ArrayList<OFMessage>();
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
            return dpid.value();
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
        public boolean isOptical() {
            return false;
        }

        @Override
        public void returnRoleReply(RoleState requested, RoleState reponse) {
        }

        @Override
        public String channelId() {
            return "1.2.3.4:1";
        }


    }
}
