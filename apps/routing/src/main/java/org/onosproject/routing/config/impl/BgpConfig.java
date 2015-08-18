/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.routing.config.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import org.onosproject.net.ConnectPoint;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration object for BGP config.
 */
public class BgpConfig extends Config<ApplicationId> {

    public static final String PEERS = "bgpPeers";
    public static final String SPEAKERS = "bgpSpeakers";
    public static final String CONNECT_POINT = "connectPoint";
    public static final String IP_ADDRESS = "ipAddress";
    public static final String LISTEN_ADDRESSES = "listenAddresses";

    /**
     * Gets the set of configured BGP peers.
     *
     * @return BGP peers
     */
    public Set<BgpPeerConfig> bgpPeers() {
        Set<BgpPeerConfig> peers = Sets.newHashSet();

        JsonNode peersNode = node.get(PEERS);
        peersNode.forEach(jsonNode -> peers.add(
                new BgpPeerConfig(ConnectPoint.deviceConnectPoint(jsonNode.path(CONNECT_POINT).asText()),
                        IpAddress.valueOf(jsonNode.path(IP_ADDRESS).asText()))));

        return peers;
    }

    /**
     * Gets the set of configured BGP speakers.
     *
     * @return BGP speakers
     */
    public Set<BgpSpeakerConfig> bgpSpeakers() {
        Set<BgpSpeakerConfig> speakers = Sets.newHashSet();

        JsonNode speakersNode = node.get(SPEAKERS);
        speakersNode.forEach(jsonNode -> {
            Set<IpAddress> listenAddresses = Sets.newHashSet();
            jsonNode.path(LISTEN_ADDRESSES).forEach(addressNode ->
                    listenAddresses.add(IpAddress.valueOf(addressNode.asText()))
            );
            speakers.add(new BgpSpeakerConfig(
                    ConnectPoint.deviceConnectPoint(jsonNode.path(CONNECT_POINT).asText()),
                    listenAddresses));
        });

        return speakers;
    }

    /**
     * Configuration for a BGP peer.
     */
    public class BgpPeerConfig {
        private ConnectPoint connectPoint;
        private IpAddress ipAddress;

        public BgpPeerConfig(ConnectPoint connectPoint, IpAddress ipAddress) {
            this.connectPoint = connectPoint;
            this.ipAddress = ipAddress;
        }

        public ConnectPoint connectPoint() {
            return connectPoint;
        }

        public IpAddress ipAddress() {
            return ipAddress;
        }

    }

    /**
     * Configuration for a BGP speaker.
     */
    public class BgpSpeakerConfig {

        private ConnectPoint connectPoint;
        private Set<IpAddress> listenAddresses;

        public BgpSpeakerConfig(ConnectPoint connectPoint, Set<IpAddress> listenAddresses) {
            this.connectPoint = checkNotNull(connectPoint);
            this.listenAddresses = checkNotNull(listenAddresses);
        }

        public ConnectPoint connectPoint() {
            return connectPoint;
        }

        public Set<IpAddress> listenAddresses() {
            return listenAddresses;
        }
    }
}
