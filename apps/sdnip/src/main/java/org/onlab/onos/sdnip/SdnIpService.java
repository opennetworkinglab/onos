package org.onlab.onos.sdnip;

import java.util.Collection;

import org.onlab.onos.sdnip.bgp.BgpRouteEntry;

/**
 * Service interface exported by SDN-IP.
 */
public interface SdnIpService {
    /**
     * Gets the BGP routes.
     *
     * @return the BGP routes
     */
    public Collection<BgpRouteEntry> getBgpRoutes();

    /**
     * Gets all the routes known to SDN-IP.
     *
     * @return the SDN-IP routes
     */
    public Collection<RouteEntry> getRoutes();
}
