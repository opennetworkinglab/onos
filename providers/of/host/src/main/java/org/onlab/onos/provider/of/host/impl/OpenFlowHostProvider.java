package org.onlab.onos.provider.of.host.impl;

import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.HostLocation;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.host.DefaultHostDescription;
import org.onlab.onos.net.host.HostDescription;
import org.onlab.onos.net.host.HostProvider;
import org.onlab.onos.net.host.HostProviderRegistry;
import org.onlab.onos.net.host.HostProviderService;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.of.controller.Dpid;
import org.onlab.onos.of.controller.OpenFlowController;
import org.onlab.onos.of.controller.OpenFlowPacketContext;
import org.onlab.onos.of.controller.PacketListener;
import org.onlab.packet.ARP;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IPAddress;
import org.onlab.packet.VLANID;
import org.slf4j.Logger;

import com.google.common.collect.Sets;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses an OpenFlow controller to detect network
 * end-station hosts.
 */
@Component(immediate = true)
public class OpenFlowHostProvider extends AbstractProvider implements HostProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenFlowController controller;

    private HostProviderService providerService;

    private final InternalHostProvider listener = new InternalHostProvider();

    /**
     * Creates an OpenFlow host provider.
     */
    public OpenFlowHostProvider() {
        super(new ProviderId("org.onlab.onos.provider.openflow"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        controller.addPacketListener(10, listener);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);
        controller.removePacketListener(listener);
        providerService = null;

        log.info("Stopped");
    }

    @Override
    public void triggerProbe(Host host) {
        log.info("Triggering probe on device {}", host);
    }

    private class InternalHostProvider implements PacketListener {

        @Override
        public void handlePacket(OpenFlowPacketContext pktCtx) {
            Ethernet eth = pktCtx.parsed();

            // potentially a new or moved host
            if (eth.getEtherType() == Ethernet.TYPE_ARP) {
                VLANID vlan = VLANID.vlanId(eth.getVlanID());
                HostId hid = HostId.hostId(
                        eth.getSourceMAC(), vlan);
                HostLocation hloc = new HostLocation(
                        DeviceId.deviceId(Dpid.uri(pktCtx.dpid())),
                        PortNumber.portNumber(pktCtx.inPort()),
                        System.currentTimeMillis());
                ARP arp = (ARP) eth.getPayload();
                Set<IPAddress> ips = Sets.newHashSet(IPAddress.valueOf(arp.getSenderProtocolAddress()));
                HostDescription hdescr = new DefaultHostDescription(
                        eth.getSourceMAC(),
                        vlan,
                        hloc,
                        ips);
                providerService.hostDetected(hid, hdescr);

            }
        }

    }
}
