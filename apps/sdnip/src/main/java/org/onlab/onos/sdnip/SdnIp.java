package org.onlab.onos.sdnip;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.sdnip.config.SdnIpConfigReader;
import org.slf4j.Logger;

/**
 * Placeholder SDN-IP component.
 */
@Component(immediate = true)
public class SdnIp {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    private SdnIpConfigReader config;
    private PeerConnectivity peerConnectivity;

    @Activate
    protected void activate() {
        log.debug("SDN-IP started");

        config = new SdnIpConfigReader();
        config.init();

        InterfaceService interfaceService = new HostServiceBasedInterfaceService(hostService);

        peerConnectivity = new PeerConnectivity(config, interfaceService, intentService);
        peerConnectivity.start();

    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }
}
