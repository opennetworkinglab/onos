package org.onlab.onos.provider.of.link.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.link.LinkProvider;
import org.onlab.onos.net.link.LinkProviderBroker;
import org.onlab.onos.net.link.LinkProviderService;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Device provider which uses an OpenFlow controller to detect network
 * infrastructure links.
 */
@Component
public class OpenFlowLinkProvider extends AbstractProvider implements LinkProvider {

    private final Logger log = LoggerFactory.getLogger(OpenFlowLinkProvider.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderBroker providerBroker;

    private LinkProviderService providerService;

//    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
//    protected OpenFlowController controller;

    /**
     * Creates an OpenFlow link provider.
     */
    public OpenFlowLinkProvider() {
        super(new ProviderId("org.onlab.onos.provider.of.link"));
    }

    @Activate
    public void activate() {
        providerService = providerBroker.register(this);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        providerBroker.unregister(this);
        providerService = null;
        log.info("Stopped");
    }

}
