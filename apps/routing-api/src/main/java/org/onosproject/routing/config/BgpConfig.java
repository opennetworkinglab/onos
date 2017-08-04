/*
 * Copyright 2015-present Open Networking Foundation
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.onlab.packet.IpAddress;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.Config;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration object for BGP config.
 */
public class BgpConfig extends Config<ApplicationId> {

    public static final String SPEAKERS = "bgpSpeakers";
    public static final String CONNECT_POINT = "connectPoint";
    public static final String NAME = "name";
    public static final String PEERS = "peers";
    public static final String VLAN = "vlan";

    /**
     * Gets the set of configured BGP speakers.
     *
     * @return BGP speakers
     */
    public Set<BgpSpeakerConfig> bgpSpeakers() {
        Set<BgpSpeakerConfig> speakers = Sets.newHashSet();

        JsonNode speakersNode = object.get(SPEAKERS);

        if (speakersNode == null) {
            return speakers;
        }

        speakersNode.forEach(jsonNode -> {
            Set<IpAddress> listenAddresses = Sets.newHashSet();
            jsonNode.path(PEERS).forEach(addressNode ->
                            listenAddresses.add(IpAddress.valueOf(addressNode.asText()))
            );

            Optional<String> name;
            if (jsonNode.get(NAME) == null) {
                name = Optional.empty();
            } else {
                name = Optional.of(jsonNode.get(NAME).asText());
            }

            VlanId vlan = getVlan(jsonNode);

            speakers.add(new BgpSpeakerConfig(name,
                                              vlan,
                                              ConnectPoint.deviceConnectPoint(jsonNode.path(CONNECT_POINT).asText()),
                                              listenAddresses));
        });

        return speakers;
    }

    /*
     * If configured, it retrieves a VLAN Id from a BGP speaker node
     */
    private VlanId getVlan(JsonNode node) {
        VlanId vlan = VlanId.NONE;
        if (!node.path(VLAN).isMissingNode()) {
            vlan = VlanId.vlanId(node.path(VLAN).asText());
        }
        return vlan;
    }

    /**
     * Examines whether a name of BGP speaker exists in configuration.
     *
     * @param name the name of BGP speaker being search
     * @return the BGP speaker
     */
    public BgpSpeakerConfig getSpeakerWithName(String name) {
        for (BgpConfig.BgpSpeakerConfig speaker : bgpSpeakers()) {
            if (speaker.name().filter(name::equals).isPresent()) {
                return speaker;
            }
        }
        return null;
    }

    /**
     * Adds a BGP speaker to the configuration.
     *
     * @param speaker the BGP speaker configuration entry
     */
    public void addSpeaker(BgpSpeakerConfig speaker) {
        // Create the new speaker node and set the parameters
        ObjectNode speakerNode = JsonNodeFactory.instance.objectNode();
        speakerNode.put(NAME, speaker.name().get());
        speakerNode.put(VLAN, speaker.vlan().toString());
        speakerNode.put(CONNECT_POINT, speaker.connectPoint().elementId().toString()
                + "/" + speaker.connectPoint().port().toString());

        ArrayNode peersNode = speakerNode.putArray(PEERS);
        for (IpAddress peerAddress: speaker.peers()) {
            peersNode.add(peerAddress.toString());
        }

        // Add the new BGP speaker to the existing node array
        ArrayNode speakersArray = bgpSpeakers().isEmpty() ?
                initBgpSpeakersConfiguration() : (ArrayNode) object.get(SPEAKERS);
        speakersArray.add(speakerNode);
    }

    /**
     * Removes BGP speaker from configuration.
     *
     * @param speakerName BGP speaker name
     */
    public void removeSpeaker(String speakerName) {
        ArrayNode speakersArray = (ArrayNode) object.get(SPEAKERS);

        for (int i = 0; i < speakersArray.size(); i++) {
            if (speakersArray.get(i).hasNonNull(NAME) &&
                    speakersArray.get(i).get(NAME).asText().equals(speakerName)) {
                speakersArray.remove(i);
                return;
            }
        }
    }

    /**
     * Adds peering address to BGP speaker.
     *
     * @param speakerName name of BGP speaker
     * @param peerAddress peering address to be added
     */
    public void addPeerToSpeaker(String speakerName, IpAddress peerAddress) {
        JsonNode speakersNode = object.get(SPEAKERS);
        speakersNode.forEach(jsonNode -> {
            if (jsonNode.hasNonNull(NAME) &&
                    jsonNode.get(NAME).asText().equals(speakerName)) {
                ArrayNode peersNode = (ArrayNode) jsonNode.get(PEERS);
                for (int i = 0; i < peersNode.size(); i++) {
                    if (peersNode.get(i).asText().equals(peerAddress.toString())) {
                        return; // Peer already exists.
                    }
                }
                peersNode.add(peerAddress.toString());
            }
        });
    }

    /**
     * Finds BGP speaker peering with a given external peer.
     *
     * @param peerAddress BGP peer address
     * @return BGP speaker
     */
    public BgpSpeakerConfig getSpeakerFromPeer(IpAddress peerAddress) {
        for (BgpConfig.BgpSpeakerConfig speaker : bgpSpeakers()) {
            if (speaker.peers().contains(peerAddress)) {
                return speaker;
            }
        }
        return null;
    }

    /**
     * Removes peering address from BGP speaker.
     *
     * @param speaker BGP speaker configuration entries
     * @param peerAddress peering address to be removed
     */
    public void removePeerFromSpeaker(BgpSpeakerConfig speaker, IpAddress peerAddress) {
        JsonNode speakersNode = object.get(SPEAKERS);
        speakersNode.forEach(jsonNode -> {
            if (jsonNode.hasNonNull(NAME) &&
                    jsonNode.get(NAME).asText().equals(speaker.name().get())) {
                ArrayNode peersNode = (ArrayNode) jsonNode.get(PEERS);
                for (int i = 0; i < peersNode.size(); i++) {
                    if (peersNode.get(i).asText().equals(peerAddress.toString())) {
                        peersNode.remove(i);
                        return;
                    }
                }
            }
        });
    }

    /**
     * Creates empty configuration for BGP speakers.
     *
     * @return empty array of BGP speakers
     */
    private ArrayNode initBgpSpeakersConfiguration() {
        return object.putArray(SPEAKERS);
    }

    /**
     * Configuration for a BGP speaker.
     */
    public static class BgpSpeakerConfig {

        private Optional<String> name;
        private VlanId vlanId;
        private ConnectPoint connectPoint;
        private Set<IpAddress> peers;

        public BgpSpeakerConfig(Optional<String> name,
                                VlanId vlanId,
                                ConnectPoint connectPoint,
                                Set<IpAddress> peers) {
            this.name = checkNotNull(name);
            this.vlanId = checkNotNull(vlanId);
            this.connectPoint = checkNotNull(connectPoint);
            this.peers = checkNotNull(peers);
        }

        public Optional<String> name() {
            return name;
        }

        public VlanId vlan() {
            return vlanId;
        }

        public ConnectPoint connectPoint() {
            return connectPoint;
        }

        public Set<IpAddress> peers() {
            return peers;
        }

        /**
         * Examines if BGP peer is connected.
         *
         * @param peer IP address of peer
         * @return result of search
         */
        public boolean isConnectedToPeer(IpAddress peer) {
            for (final IpAddress entry : peers()) {
                if (entry.equals(peer)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof BgpSpeakerConfig) {
                final BgpSpeakerConfig that = (BgpSpeakerConfig) obj;
                return Objects.equals(this.name, that.name) &&
                        Objects.equals(this.vlanId, that.vlanId) &&
                        Objects.equals(this.connectPoint, that.connectPoint) &&
                        Objects.equals(this.peers, that.peers);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, vlanId, connectPoint, peers);
        }
    }
}
