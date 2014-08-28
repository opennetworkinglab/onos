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
import org.onlab.onos.of.controller.OpenFlowController;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses an OpenFlow controller to detect network
 * infrastructure links.
 */
@Component(immediate = true)
public class OpenFlowLinkProvider extends AbstractProvider implements LinkProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkProviderBroker providerBroker;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenFlowController controller;

    private LinkProviderService providerService;

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
