package org.onlab.onos.net.proxyarp;

import org.onlab.onos.net.packet.PacketContext;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpPrefix;

/**
 * Service for processing arp requests on behalf of applications.
 */
// TODO: move to the peer host package
public interface ProxyArpService {

    /**
     * Returns whether this particular ip address is known to the system.
     *
     * @param addr a ip address
     * @return true if know, false otherwise
     */
    boolean known(IpPrefix addr);

    /**
     * Sends a reply for a given request. If the host is not known then the arp
     * will be flooded at all edge ports.
     *
     * @param eth an arp request
     */
    void reply(Ethernet eth);

    /**
     * Forwards an ARP request to its destination. Floods at the edge the ARP request if the
     * destination is not known.
     *
     * @param eth an ethernet frame containing an ARP request.
     */
    void forward(Ethernet eth);

    /**
     * Handles a arp packet.
     * Replies to arp requests and forwards request to the  right place.
     * @param context the packet context to handle
     * @return true if handled, false otherwise.
     */
    boolean handleArp(PacketContext context);

}
