package org.onlab.onos.provider.of.host.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.host.HostDescription;
import org.onlab.onos.net.host.HostProvider;
import org.onlab.onos.net.host.HostProviderRegistry;
import org.onlab.onos.net.host.HostProviderService;
import org.onlab.onos.net.provider.AbstractProviderService;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.net.topology.Topology;
import org.onlab.onos.net.topology.TopologyServiceAdapter;
import org.onlab.onos.of.controller.Dpid;
import org.onlab.onos.of.controller.OpenFlowPacketContext;
import org.onlab.onos.of.controller.OpenflowControllerAdapter;
import org.onlab.onos.of.controller.PacketListener;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.types.OFPort;

public class OpenFlowHostProviderTest {

    private static final Integer INPORT = 10;
    private static final Dpid DPID1 = new Dpid(100);
    private static final Dpid DPID2 = new Dpid(200);
    private static final Dpid DPID3 = new Dpid(300);

    private static final VlanId VLAN = VlanId.vlanId();
    private static final MacAddress MAC = MacAddress.valueOf("00:00:11:00:00:01");
    private static final MacAddress BCMAC = MacAddress.valueOf("ff:ff:ff:ff:ff:ff");
    private static final byte[] IP = new byte[]{10, 0, 0, 1};

    private final OpenFlowHostProvider provider = new OpenFlowHostProvider();
    private final TestHostRegistry hostService = new TestHostRegistry();
    private final TestController controller = new TestController();
    private final TestTopologyService topoService = new TestTopologyService();
    private TestHostProviderService providerService;

    @Before
    public void setUp() {
        provider.providerRegistry = hostService;
        provider.controller = controller;
        provider.topologyService = topoService;
        provider.activate();
    }

    @Test
    public void basics() {
        assertNotNull("registration expected", providerService);
        assertEquals("incorrect provider", provider, providerService.provider());
    }

    @Test
    public void events() {
        // new host
        controller.processPacket(DPID1, null);
        assertNotNull("new host expected", providerService.added);
        assertNull("host motion unexpected", providerService.moved);

        // the host moved to new switch
        controller.processPacket(DPID2, null);
        assertNotNull("host motion expected", providerService.moved);

        // the host was misheard on a spine
        controller.processPacket(DPID3, null);
        assertNull("host misheard on spine switch", providerService.spine);
    }

    @After
    public void tearDown() {
        provider.deactivate();
        provider.providerRegistry = null;
        provider.controller = null;
    }

    private class TestHostRegistry implements HostProviderRegistry {

        @Override
        public HostProviderService register(HostProvider provider) {
            providerService = new TestHostProviderService(provider);
            return providerService;
        }

        @Override
        public void unregister(HostProvider provider) {
        }

        @Override
        public Set<ProviderId> getProviders() {
            return null;
        }

    }

    private class TestHostProviderService
    extends AbstractProviderService<HostProvider>
    implements HostProviderService {

        Dpid added = null;
        Dpid moved = null;
        Dpid spine = null;

        protected TestHostProviderService(HostProvider provider) {
            super(provider);
        }

        @Override
        public void hostDetected(HostId hostId, HostDescription hostDescription) {
            Dpid descr = Dpid.dpid(hostDescription.location().deviceId().uri());
            if (added == null) {
                added = descr;
            } else if ((moved == null) && !descr.equals(added)) {
                moved = descr;
            } else {
                spine = descr;
            }
        }

        @Override
        public void hostVanished(HostId hostId) {
        }

    }

    private class TestController extends OpenflowControllerAdapter {
        PacketListener pktListener;

        @Override
        public void addPacketListener(int priority, PacketListener listener) {
            pktListener = listener;
        }

        @Override
        public void processPacket(Dpid dpid, OFMessage msg) {
            OpenFlowPacketContext ctx = new TestPacketContext(dpid);
            pktListener.handlePacket(ctx);
        }
    }

    private class TestTopologyService extends TopologyServiceAdapter {
        @Override
        public boolean isInfrastructure(Topology topology,
                ConnectPoint connectPoint) {
            //simulate DPID3 as an infrastructure switch
            if (Dpid.dpid(connectPoint.deviceId().uri()).equals(DPID3)) {
                return true;
            }
            return false;
        }
    }

    private class TestPacketContext implements OpenFlowPacketContext {

        protected Dpid swid;

        public TestPacketContext(Dpid dpid) {
            swid = dpid;
        }

        @Override
        public boolean block() {
            return false;
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
            // just things we (and serializers) need
            ARP arp = new ARP();
            arp.setSenderProtocolAddress(IP)
            .setSenderHardwareAddress(MAC.toBytes())
            .setTargetHardwareAddress(BCMAC.toBytes())
            .setTargetProtocolAddress(IP);

            Ethernet eth = new Ethernet();
            eth.setEtherType(Ethernet.TYPE_ARP)
            .setVlanID(VLAN.toShort())
            .setSourceMACAddress(MAC.toBytes())
            .setDestinationMACAddress(BCMAC.getAddress())
            .setPayload(arp);

            return eth;
        }

        @Override
        public byte[] unparsed() {
            return null;
        }

        @Override
        public Dpid dpid() {
            return swid;
        }

        @Override
        public Integer inPort() {
            return INPORT;
        }

        @Override
        public boolean isHandled() {
            return false;
        }

    }
}
