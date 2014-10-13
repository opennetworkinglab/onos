package org.onlab.onos.store.host.impl;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.net.host.HostClockService;
import org.onlab.onos.store.Timestamp;
import org.onlab.onos.store.impl.WallClockTimestamp;
import org.onlab.packet.MacAddress;
import org.slf4j.Logger;

/**
 * HostClockService to issue Timestamps based on local wallclock time.
 */
@Component(immediate = true)
@Service
public class HostClockManager implements HostClockService {

    private final Logger log = getLogger(getClass());

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public Timestamp getTimestamp(MacAddress hostMac) {
        return new WallClockTimestamp();
    }
}
