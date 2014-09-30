package org.onlab.onos.net.proxyarp;

import org.onlab.packet.Ethernet;
import org.onlab.packet.IpPrefix;

/**
 * Service for processing arp requests on behalf of applications.
 */
public interface ProxyArpService {

    /**
     * Returns whether this particular ip address is known to the system.
     *
     * @param addr
     *            a ip address
     * @return true if know, false otherwise
     */
    boolean known(IpPrefix addr);

    /**
     * Sends a reply for a given request. If the host is not known then the arp
     * will be flooded at all edge ports.
     *
     * @param request
     *            an arp request
     */
    void reply(Ethernet request);

}
