package org.onosproject.drivers.cisco;

import com.google.common.collect.Lists;
import org.onosproject.net.behaviour.PortStatsQuery;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Retrieve port stats from Cisco CSR1000v router via netconf.
 */
public class PortStatsQueryCsr1000vImpl extends AbstractHandlerBehaviour
        implements PortStatsQuery {

    private final Logger log = getLogger(getClass());

    public Collection<PortStatistics> getPortStatistics() {
        List<PortStatistics> ps = Lists.newArrayList();
        return ps;
    }
}
