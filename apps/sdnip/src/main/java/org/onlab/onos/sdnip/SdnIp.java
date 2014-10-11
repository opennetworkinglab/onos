package org.onlab.onos.sdnip;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.slf4j.Logger;

/**
 * Placeholder SDN-IP component.
 */
@Component(immediate = true)
public class SdnIp {

    private final Logger log = getLogger(getClass());

    @Activate
    protected void activate() {
        log.debug("SDN-IP started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }
}
