package org.onlab.onos.sdnip.config;

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
