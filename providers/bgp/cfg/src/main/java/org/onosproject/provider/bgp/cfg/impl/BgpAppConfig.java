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
package org.onosproject.provider.bgp.cfg.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onosproject.bgp.controller.BgpCfg;
import org.onosproject.bgp.controller.BgpController;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;

import java.util.ArrayList;
import java.util.List;

import static org.onosproject.net.config.Config.FieldPresence.MANDATORY;
import static org.onosproject.net.config.Config.FieldPresence.OPTIONAL;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration object for BGP.
 */
public class BgpAppConfig extends Config<ApplicationId> {
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    BgpController bgpController;

    BgpCfg bgpConfig = null;

    public static final String ROUTER_ID = "routerId";
    public static final String LOCAL_AS = "localAs";
    public static final String MAX_SESSION = "maxSession";
    public static final String LS_CAPABILITY = "lsCapability";
    public static final String HOLD_TIME = "holdTime";
    public static final String LARGE_AS_CAPABILITY = "largeAsCapability";

    public static final String BGP_PEER = "bgpPeer";
    public static final String PEER_IP = "peerIp";
    public static final String REMOTE_AS = "remoteAs";
    public static final String PEER_HOLD_TIME = "peerHoldTime";
    public static final String PEER_CONNECT_MODE = "connectMode";
    public static final String PEER_CONNECT_PASSIVE = "passive";
    public static final String PEER_CONNECT_ACTIVE = "active";

    static final int MAX_SHORT_AS_NUMBER = 65535;
    static final long MAX_LONG_AS_NUMBER = 4294967295L;

    @Override
    public boolean isValid() {
        boolean fields = false;

        this.bgpController = DefaultServiceDirectory.getService(BgpController.class);
        bgpConfig = bgpController.getConfig();

        fields = hasOnlyFields(ROUTER_ID, LOCAL_AS, MAX_SESSION, LS_CAPABILITY,
                HOLD_TIME, LARGE_AS_CAPABILITY, BGP_PEER) &&
                isIpAddress(ROUTER_ID, MANDATORY) && isNumber(LOCAL_AS, MANDATORY) &&
                isNumber(MAX_SESSION, OPTIONAL, 20) && isNumber(HOLD_TIME, OPTIONAL, 180) &&
                isBoolean(LS_CAPABILITY, OPTIONAL) && isBoolean(LARGE_AS_CAPABILITY, OPTIONAL);

        if (!fields) {
            return fields;
        }

        return validateBgpConfiguration();
    }

    /**
     * Returns routerId from the configuration.
     *
     * @return routerId
     */
    public String routerId() {
        return get(ROUTER_ID, null);
    }

    /**
     * Returns localAs number from the configuration.
     *
     * @return local As number
     */
    public int localAs() {
        return Integer.parseInt(get(LOCAL_AS, null));
    }

    /**
     * Returns max session from the configuration.
     *
     * @return max session
     */
    public int maxSession() {
        return Integer.parseInt(get(MAX_SESSION, null));
    }

    /**
     * Returns BGP-LS capability support from the configuration.
     *
     * @return true if BGP-LS capability is set else false
     */
    public boolean lsCapability() {
        return Boolean.parseBoolean(get(LS_CAPABILITY, null));
    }

    /**
     * Returns largeAs capability support from the configuration.
     *
     * @return largeAs capability
     */
    public boolean largeAsCapability() {
        return Boolean.parseBoolean(get(LARGE_AS_CAPABILITY, null));
    }

    /**
     * Returns holdTime of the local node from the configuration.
     *
     * @return holdTime
     */
    public short holdTime() {
        return Short.parseShort(get(HOLD_TIME, null));
    }

    /**
     * Validates the Bgp local and peer configuration.
     *
     * @return true if valid else false
     */
    public boolean validateBgpConfiguration() {

        if (!validateLocalAs()) {
            return false;
        }

        if (!validateRouterId()) {
            return false;
        }

        if (!validateBgpPeers()) {
            return false;
        }

        return true;
    }

    /**
     * Validates the Bgp As number.
     *
     * @return true if valid else false
     */
    public boolean validateLocalAs() {

        long localAs = 0;
        localAs = localAs();

        if (bgpController.connectedPeerCount() != 0) {
            return false;
        }

        if (largeAsCapability()) {

            if (localAs == 0 || localAs >= MAX_LONG_AS_NUMBER) {
                return false;
            }
        } else {
            if (localAs == 0 || localAs >= MAX_SHORT_AS_NUMBER) {
                return false;
            }
        }

        return true;
    }

    /**
     * Validates the Bgp peer As number.
     *
     * @return true if valid else false
     */
    public boolean validateRemoteAs(long remoteAs) {
        if (largeAsCapability()) {

            if (remoteAs == 0 || remoteAs >= MAX_LONG_AS_NUMBER) {
                return false;
            }
        } else {
            if (remoteAs == 0 || remoteAs >= MAX_SHORT_AS_NUMBER) {
                return false;
            }
        }
        return true;
    }

    /**
     * Validates the Bgp Router ID configuration.
     *
     * @return true if valid else false
     */
    public boolean validateRouterId() {
        String routerId = routerId();
        if (bgpController.connectedPeerCount() != 0) {
            return false;
        }
        return true;
    }

    /**
     * Validates the Bgp peer holdTime.
     *
     * @return true if valid else false
     */
    public boolean validatePeerHoldTime(long remoteAs) {
        //TODO:Validate it later..
        return true;
    }

    /**
     * Validates the Bgp peer configuration.
     *
     * @return true if valid else false
     */
    public boolean validateBgpPeers() {
        List<BgpPeerConfig> nodes;
        String connectMode;

        nodes = bgpPeer();
        for (int i = 0; i < nodes.size(); i++) {
            connectMode = nodes.get(i).connectMode();
            if ((IpAddress.valueOf(nodes.get(i).hostname()) == null) ||
                    !validateRemoteAs(nodes.get(i).asNumber()) ||
                    !validatePeerHoldTime(nodes.get(i).holdTime()) ||
                    !(connectMode.equals(PEER_CONNECT_ACTIVE) || connectMode.equals(PEER_CONNECT_PASSIVE))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the set of nodes read from network config.
     *
     * @return list of BgpPeerConfig or null
     */
    public List<BgpPeerConfig> bgpPeer() {
        List<BgpPeerConfig> nodes = new ArrayList<BgpPeerConfig>();

        JsonNode jsonNodes = object.get(BGP_PEER);
        if (jsonNodes == null) {
            return null;
        }

        jsonNodes.forEach(jsonNode -> nodes.add(new BgpPeerConfig(
                jsonNode.path(PEER_IP).asText(),
                jsonNode.path(REMOTE_AS).asInt(),
                jsonNode.path(PEER_HOLD_TIME).asInt(),
                jsonNode.path(PEER_CONNECT_MODE).asText())));

        return nodes;
    }

    /**
     * Configuration for Bgp peer nodes.
     */
    public static class BgpPeerConfig {

        private final String hostname;
        private final int asNumber;
        private final short holdTime;
        private final String connectMode;

        public BgpPeerConfig(String hostname, int asNumber, int holdTime, String connectMode) {
            this.hostname = checkNotNull(hostname);
            this.asNumber = asNumber;
            this.holdTime = (short) holdTime;
            this.connectMode = connectMode;
        }

        /**
         * Returns hostname of the peer node.
         *
         * @return hostname
         */
        public String hostname() {
            return this.hostname;
        }

        /**
         * Returns asNumber if peer.
         *
         * @return asNumber
         */
        public int asNumber() {
            return this.asNumber;
        }

        /**
         * Returns holdTime of the peer node.
         *
         * @return holdTime
         */
        public short holdTime() {
            return this.holdTime;
        }

        /**
         * Returns connection mode for the peer node.
         *
         * @return active or passive connection
         */
        public String connectMode() {
            return this.connectMode;
        }
    }
}
