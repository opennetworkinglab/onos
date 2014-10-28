/*
 * Copyright 2014 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
