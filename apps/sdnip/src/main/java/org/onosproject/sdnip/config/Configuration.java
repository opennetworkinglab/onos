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
package org.onosproject.sdnip.config;

import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Contains the configuration data for SDN-IP that has been read from a
 * JSON-formatted configuration file.
 */
public class Configuration {
    // We call the BGP routers in our SDN network the BGP speakers, and call
    // the BGP routers outside our SDN network the BGP peers.
    private List<BgpSpeaker> bgpSpeakers;
    private List<BgpPeer> peers;

    /**
     * Default constructor.
     */
    public Configuration() {
    }

    /**
     * Gets a list of bgpSpeakers in the system, represented by
     * {@link BgpSpeaker} objects.
     *
     * @return the list of BGP speakers
     */
    public List<BgpSpeaker> getBgpSpeakers() {
        return Collections.unmodifiableList(bgpSpeakers);
    }

    /**
     * Sets a list of bgpSpeakers in the system.
     *
     * @param bgpSpeakers the list of BGP speakers
     */
    @JsonProperty("bgpSpeakers")
    public void setBgpSpeakers(List<BgpSpeaker> bgpSpeakers) {
        this.bgpSpeakers = bgpSpeakers;
    }

    /**
     * Gets a list of BGP peers we are configured to peer with. Peers are
     * represented by {@link BgpPeer} objects.
     *
     * @return the list of BGP peers
     */
    public List<BgpPeer> getPeers() {
        return Collections.unmodifiableList(peers);
    }

    /**
     * Sets a list of BGP peers we are configured to peer with.
     *
     * @param peers the list of BGP peers
     */
    @JsonProperty("bgpPeers")
    public void setPeers(List<BgpPeer> peers) {
        this.peers = peers;
    }

}
