package org.onlab.onos.provider.of.link.impl;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.link.LinkProvider;
import org.onlab.onos.net.link.LinkProviderRegistry;
import org.onlab.onos.net.link.LinkProviderService;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.of.controller.Dpid;
import org.onlab.onos.of.controller.OpenFlowController;
import org.onlab.onos.of.controller.OpenFlowSwitchListener;
import org.onlab.onos.of.controller.PacketContext;
import org.onlab.onos.of.controller.PacketListener;
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

    private final InternalLinkProvider listener = new InternalLinkProvider();

    /**
     * Creates an OpenFlow link provider.
     */
    public OpenFlowLinkProvider() {
        super(new ProviderId("org.onlab.onos.provider.of.link"));
    }

    @Activate
    public void activate() {
        providerService = providerRegistry.register(this);
        controller.addListener(listener);
        controller.addPacketListener(0, listener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);
        controller.removeListener(listener);
        controller.removePacketListener(listener);
        providerService = null;
        log.info("Stopped");
    }


    private class InternalLinkProvider implements PacketListener, OpenFlowSwitchListener {

        @Override
        public void handlePacket(PacketContext pktCtx) {

        }

        @Override
        public void switchAdded(Dpid dpid) {
            // TODO Auto-generated method stub

        }

        @Override
        public void switchRemoved(Dpid dpid) {
            // TODO Auto-generated method stub

        }

        @Override
        public void portChanged(Dpid dpid, OFPortStatus status) {
            // TODO Auto-generated method stub

        }

    }

}
