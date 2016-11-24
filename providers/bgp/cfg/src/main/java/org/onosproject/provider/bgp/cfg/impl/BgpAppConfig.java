/*
 * Copyright 2015-present Open Networking Laboratory
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    protected final Logger log = LoggerFactory.getLogger(BgpAppConfig.class);
    public static final String ROUTER_ID = "routerId";
    public static final String LOCAL_AS = "localAs";
    public static final String MAX_SESSION = "maxSession";
    public static final String LS_CAPABILITY = "lsCapability";
    public static final String HOLD_TIME = "holdTime";
    public static final String LARGE_AS_CAPABILITY = "largeAsCapability";
    public static final String FLOW_SPEC_CAPABILITY = "flowSpecCapability";
    public static final String FLOW_SPEC_RPD_CAPABILITY = "flowSpecRpdCapability";

    public static final String BGP_PEER = "bgpPeer";
    public static final String PEER_IP = "peerIp";
    public static final String REMOTE_AS = "remoteAs";
    public static final String PEER_HOLD_TIME = "peerHoldTime";
    public static final String PEER_CONNECT_MODE = "connectMode";
    public static final String PEER_CONNECT_PASSIVE = "passive";
    public static final String PEER_CONNECT_ACTIVE = "active";

    static final int MAX_SHORT_AS_NUMBER = 65535;
    static final long MAX_LONG_AS_NUMBER = 4294967295L;

    static final int MIN_SESSION_NUMBER = 1;
    static final long MAX_SESSION_NUMBER = 21;

    static final int MIN_HOLDTIME = 0;
    static final long MAX_HOLDTIME = 65535;

    @Override
    public boolean isValid() {
        boolean fields = false;

        this.bgpController = DefaultServiceDirectory.getService(BgpController.class);
        bgpConfig = bgpController.getConfig();

        fields = hasOnlyFields(ROUTER_ID, LOCAL_AS, MAX_SESSION, LS_CAPABILITY,
                HOLD_TIME, LARGE_AS_CAPABILITY, FLOW_SPEC_CAPABILITY, FLOW_SPEC_RPD_CAPABILITY, BGP_PEER) &&
                isIpAddress(ROUTER_ID, MANDATORY) && isNumber(LOCAL_AS, MANDATORY) &&
                isNumber(MAX_SESSION, OPTIONAL, MIN_SESSION_NUMBER, MAX_SESSION_NUMBER)
                && isNumber(HOLD_TIME, OPTIONAL, MIN_HOLDTIME, MAX_HOLDTIME) &&
                isBoolean(LS_CAPABILITY, OPTIONAL) && isBoolean(LARGE_AS_CAPABILITY, OPTIONAL) &&
                isString(FLOW_SPEC_CAPABILITY, OPTIONAL) && isBoolean(FLOW_SPEC_RPD_CAPABILITY, OPTIONAL);

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
     * Returns flow spec route policy distribution capability support from the configuration.
     *
     * @return true if flow spec route policy distribution capability is set otherwise false
     */
    public boolean rpdCapability() {
        return Boolean.parseBoolean(get(FLOW_SPEC_RPD_CAPABILITY, null));
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
     * Returns flow specification capability support from the configuration.
     *
     * @return flow specification capability
     */
    public String flowSpecCapability() {
        return get(FLOW_SPEC_CAPABILITY, null);
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
     * Validates the flow specification capability.
     *
     * @return true if valid else false
     */
    public boolean validateFlowSpec() {
        if (flowSpecCapability() != null) {
            String flowSpec = flowSpecCapability();
            if ((!flowSpec.equals("IPV4")) && (!flowSpec.equals("VPNV4")) && (!flowSpec.equals("IPV4_VPNV4"))) {
                log.debug("Flow specification capabality is false");
                return false;
            }
        }
        log.debug("Flow specification capabality is true");
        return true;
    }

    /**
     * Validates the hold time value.
     *
     * @return true if valid else false
     */
    public boolean validateHoldTime() {
        if (holdTime() != 0) {
            short holdTime = holdTime();
            if ((holdTime == 1) || (holdTime == 2)) {
                return false;
            }
        }
        return true;
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

        if (!validateFlowSpec()) {
            return false;
        }

        if (!validateHoldTime()) {
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
     * @param remoteAs remote As number
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
        // TODO: router ID validation
        return true;
    }

    /**
     * Validates the Bgp peer holdTime.
     *
     * @param remoteAs remote As number
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
                log.debug("BGP peer configration false");
                return false;
            }
        }
        log.debug("BGP peer configration true");
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
