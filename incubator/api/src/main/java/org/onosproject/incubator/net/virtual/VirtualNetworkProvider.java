package org.onosproject.incubator.net.virtual;

import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.provider.Provider;

/**
 * Entity capable of providing traffic isolation constructs for use in
 * implementation of virtual devices and virtual links.
 */
public interface VirtualNetworkProvider extends Provider {

    /**
     * Creates a network tunnel for all traffic from the specified source
     * connection point to the indicated destination connection point.
     *
     * @param networkId virtual network identifier
     * @param src       source connection point
     * @param dst       destination connection point
     * @return new tunnel's id
     */
    TunnelId createTunnel(NetworkId networkId, ConnectPoint src, ConnectPoint dst);

    /**
     * Destroys the specified network tunnel.
     *
     * @param networkId virtual network identifier
     * @param tunnelId  tunnel identifier
     */
    void destroyTunnel(NetworkId networkId, TunnelId tunnelId);

}
