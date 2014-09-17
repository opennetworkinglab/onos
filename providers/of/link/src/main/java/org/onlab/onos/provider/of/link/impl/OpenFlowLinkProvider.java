package org.onlab.onos.provider.of.link.impl;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.link.LinkProvider;
import org.onlab.onos.net.link.LinkProviderRegistry;
import org.onlab.onos.net.link.LinkProviderService;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.of.controller.Dpid;
import org.onlab.onos.of.controller.OpenFlowController;
import org.onlab.onos.of.controller.OpenFlowPacketContext;
import org.onlab.onos.of.controller.OpenFlowSwitch;
import org.onlab.onos.of.controller.OpenFlowSwitchListener;
import org.onlab.onos.of.controller.PacketListener;
import org.projectfloodlight.openflow.protocol.OFPortConfig;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortState;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.slf4j.Logger;

/**
 * Provider which uses an OpenFlow controller to detect network
 * infrastructure links.
 */
@Component(immediate = true)
public class OpenFlowLinkProvider extends AbstractProvider implements LinkProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenFlowController controller;

    private LinkProviderService providerService;

    private final boolean useBDDP = true;

    private final InternalLinkProvider listener = new InternalLinkProvider();

    private final Map<Dpid, LinkDiscovery> discoverers = new ConcurrentHashMap<>();

    /**
     * Creates an OpenFlow link provider.
     */
    public OpenFlowLinkProvider() {
        super(new ProviderId("org.onlab.onos.provider.openflow"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        controller.addListener(listener);
        controller.addPacketListener(0, listener);
        for (OpenFlowSwitch sw : controller.getSwitches()) {
            listener.switchAdded(new Dpid(sw.getId()));
        }
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        for (LinkDiscovery ld : discoverers.values()) {
            ld.stop();
        }
        providerRegistry.unregister(this);
        controller.removeListener(listener);
        controller.removePacketListener(listener);
        providerService = null;

        log.info("Stopped");
    }


    private class InternalLinkProvider implements PacketListener, OpenFlowSwitchListener {


        @Override
        public void handlePacket(OpenFlowPacketContext pktCtx) {
            LinkDiscovery ld = discoverers.get(pktCtx.dpid());
            if (ld == null) {
                return;
            }
            if (ld.handleLLDP(pktCtx.unparsed(), pktCtx.inPort())) {
                pktCtx.block();
            }

        }

        @Override
        public void switchAdded(Dpid dpid) {
            discoverers.put(dpid, new LinkDiscovery(controller.getSwitch(dpid),
                    controller, providerService, useBDDP));

        }

        @Override
        public void switchRemoved(Dpid dpid) {
            LinkDiscovery ld = discoverers.remove(dpid);
            if (ld != null) {
                ld.removeAllPorts();
            }
            providerService.linksVanished(
                    DeviceId.deviceId("of:" + Long.toHexString(dpid.value())));
        }

        @Override
        public void portChanged(Dpid dpid, OFPortStatus status) {
            LinkDiscovery ld = discoverers.get(dpid);
            if (ld == null) {
                return;
            }
            final OFPortDesc port = status.getDesc();
            final boolean enabled = !port.getState().contains(OFPortState.LINK_DOWN) &&
                    !port.getConfig().contains(OFPortConfig.PORT_DOWN);
            if (enabled) {
                ld.addPort(port);
            } else {
                /*
                 * remove port calls linkVanished
                 */
                ld.removePort(port);
            }

        }

    }

}
