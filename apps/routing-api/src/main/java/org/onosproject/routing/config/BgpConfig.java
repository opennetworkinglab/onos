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

package org.onosproject.routing.config;

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

    public static final String SPEAKERS = "bgpSpeakers";
    public static final String CONNECT_POINT = "connectPoint";
    public static final String PEERS = "peers";

    // TODO add methods for updating config

    /**
     * Gets the set of configured BGP speakers.
     *
     * @return BGP speakers
     */
    public Set<BgpSpeakerConfig> bgpSpeakers() {
        Set<BgpSpeakerConfig> speakers = Sets.newHashSet();

        JsonNode speakersNode = object.get(SPEAKERS);
        speakersNode.forEach(jsonNode -> {
            Set<IpAddress> listenAddresses = Sets.newHashSet();
            jsonNode.path(PEERS).forEach(addressNode ->
                    listenAddresses.add(IpAddress.valueOf(addressNode.asText()))
            );
            speakers.add(new BgpSpeakerConfig(
                    ConnectPoint.deviceConnectPoint(jsonNode.path(CONNECT_POINT).asText()),
                    listenAddresses));
        });

        return speakers;
    }

    /**
     * Configuration for a BGP speaker.
     */
    public static class BgpSpeakerConfig {

        private ConnectPoint connectPoint;
        private Set<IpAddress> peers;

        public BgpSpeakerConfig(ConnectPoint connectPoint, Set<IpAddress> peers) {
            this.connectPoint = checkNotNull(connectPoint);
            this.peers = checkNotNull(peers);
        }

        public ConnectPoint connectPoint() {
            return connectPoint;
        }

        public Set<IpAddress> peers() {
            return peers;
        }
    }
}
