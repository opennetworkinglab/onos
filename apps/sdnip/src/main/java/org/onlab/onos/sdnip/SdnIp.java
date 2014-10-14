package org.onlab.onos.sdnip;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.onlab.onos.sdnip.config.SdnIpConfigReader;
import org.slf4j.Logger;

/**
 * Placeholder SDN-IP component.
 */
@Component(immediate = true)
public class SdnIp {

    private final Logger log = getLogger(getClass());

    private SdnIpConfigReader config;

    @Activate
    protected void activate() {
        log.debug("SDN-IP started");

        config = new SdnIpConfigReader();
        config.init();
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }
}
