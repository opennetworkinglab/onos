package org.onlab.onos.sdnip.config;

import java.util.Map;

import org.onlab.packet.IpAddress;

/**
 * Provides information about the layer 3 properties of the network.
 * This is based on IP addresses configured on ports in the network.
 */
public interface SdnIpConfigService {

    /**
     * Gets the list of virtual external-facing interfaces.
     *
     * @return the map of interface names to interface objects
     */
    //public Map<String, Interface> getInterfaces();

    /**
     * Gets the list of BGP speakers inside the SDN network.
     *
     * @return the map of BGP speaker names to BGP speaker objects
     */
    public Map<String, BgpSpeaker> getBgpSpeakers();

    /**
     * Gets the list of configured BGP peers.
     *
     * @return the map from peer IP address to BgpPeer object
     */
    public Map<IpAddress, BgpPeer> getBgpPeers();

    /**
     * Gets the Interface object for the interface that packets
     * to dstIpAddress will be sent out of. Returns null if dstIpAddress is not
     * in a directly connected network, or if no interfaces are configured.
     *
     * @param dstIpAddress destination IP address that we want to match to
     *                     an outgoing interface
     * @return the Interface object if one is found, otherwise null
     */
    //public Interface getOutgoingInterface(IpAddress dstIpAddress);

}
