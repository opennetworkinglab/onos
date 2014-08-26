package org.onlab.onos.provider.of.host.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.host.HostProvider;
import org.onlab.onos.net.host.HostProviderBroker;
import org.onlab.onos.net.host.HostProviderService;
import org.onlab.onos.net.provider.AbstractProvider;
import org.onlab.onos.net.provider.ProviderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Device provider which uses an OpenFlow controller to detect network
 * end-station hosts.
 */
@Component
public class OpenFlowHostProvider extends AbstractProvider implements HostProvider {

    private final Logger log = LoggerFactory.getLogger(OpenFlowHostProvider.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostProviderBroker providerBroker;

    private HostProviderService providerService;

//    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
//    protected OpenFlowController controller;

    /**
     * Creates an OpenFlow host provider.
     */
    public OpenFlowHostProvider() {
        super(new ProviderId("org.onlab.onos.provider.of.host"));
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

    @Override
    public void triggerProbe(Host host) {
        log.info("Triggering probe on device {}", host);
    }
}
