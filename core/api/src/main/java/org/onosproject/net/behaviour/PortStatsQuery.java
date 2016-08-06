package org.onosproject.net.behaviour;

import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.driver.HandlerBehaviour;

import java.util.Collection;

/**
 * Query port stats through device specific protocol and return Collection<PortStatistics>.
 * The port stats info is to be installed into ONOS core.
 */
public interface PortStatsQuery extends HandlerBehaviour {

    /**
     * Retrieve the set of port stats from a device.
     * @return a set of port stats.
     */
    Collection<PortStatistics> getPortStatistics();
}
