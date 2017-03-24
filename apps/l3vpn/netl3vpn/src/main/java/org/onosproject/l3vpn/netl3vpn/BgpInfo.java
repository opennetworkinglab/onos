/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.l3vpn.netl3vpn;

import java.util.HashMap;
import java.util.Map;

/**
 * Representation of BGP information which contains the protocol info and the
 * VPN name.
 */
public class BgpInfo {

    /**
     * Map of route protocol and the protocol info for the BGP info.
     */
    private Map<RouteProtocol, ProtocolInfo> protocolInfo;

    /**
     * VPN name, to which the BGP info belongs.
     */
    private String vpnName;

    /**
     * Constructs BGP info.
     */
    public BgpInfo() {
    }

    /**
     * Returns the map of protocol info associated with the BGP info.
     *
     * @return protocol info map.
     */
    public Map<RouteProtocol, ProtocolInfo> protocolInfo() {
        return protocolInfo;
    }

    /**
     * Sets the map of protocol info with route protocol as key value.
     *
     * @param protocolInfo protocol info map
     */
    public void protocolInfo(Map<RouteProtocol, ProtocolInfo> protocolInfo) {
        this.protocolInfo = protocolInfo;
    }

    /**
     * Adds a protocol info with route protocol as key to the map.
     *
     * @param route route protocol
     * @param info  protocol info
     */
    public void addProtocolInfo(RouteProtocol route, ProtocolInfo info) {
        if (protocolInfo == null) {
            protocolInfo = new HashMap<>();
        }
        protocolInfo.put(route, info);
    }

    /**
     * Returns the VPN name of the BGP info.
     *
     * @return VPN name
     */
    public String vpnName() {
        return vpnName;
    }

    /**
     * Sets the VPN name.
     *
     * @param vpnName VPN name
     */
    public void vpnName(String vpnName) {
        this.vpnName = vpnName;
    }

    @Override
    public String toString() {
        return "VPN name : " + vpnName;
    }
}

