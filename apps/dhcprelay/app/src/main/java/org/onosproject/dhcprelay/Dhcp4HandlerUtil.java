/*
 * Copyright 2017-present Open Networking Foundation
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
 *
 */
package org.onosproject.dhcprelay;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.VlanId;
import org.onlab.util.HexString;
import org.onosproject.dhcprelay.api.DhcpServerInfo;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intf.Interface;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Set;

public final class Dhcp4HandlerUtil {
    private static final Logger log = LoggerFactory.getLogger(Dhcp4HandlerUtil.class);

    private Dhcp4HandlerUtil() {
    }

    /**
     * Returns the first v4 interface ip out of a set of interfaces or null.
     *
     * @param intfs set of interfaces
     * @return Ip4Address / null if not present
     */
    public static Ip4Address getRelayAgentIPv4Address(Set<Interface> intfs) {
        for (Interface intf : intfs) {
            for (InterfaceIpAddress ip : intf.ipAddressesList()) {
                Ip4Address relayAgentIp = ip.ipAddress().getIp4Address();
                if (relayAgentIp != null) {
                    return relayAgentIp;
                }
            }
        }
        return null;
    }

    /**
     * Determind if an Interface contains a vlan id.
     *
     * @param iface the Interface
     * @param vlanId the vlan id
     * @return true if the Interface contains the vlan id
     */
    public static boolean interfaceContainsVlan(Interface iface, VlanId vlanId) {
        if (vlanId.equals(VlanId.NONE)) {
            // untagged packet, check if vlan untagged or vlan native is not NONE
            return !iface.vlanUntagged().equals(VlanId.NONE) ||
                    !iface.vlanNative().equals(VlanId.NONE);
        }
        // tagged packet, check if the interface contains the vlan
        return iface.vlanTagged().contains(vlanId);
   }

    /**
     * Check if a given server info has v6 ipaddress.
     *
     * @param serverInfo server info to check
     * @return true if server info has v6 ip address; false otherwise
     */
    public static boolean isServerIpEmpty(DhcpServerInfo serverInfo) {
        if (!serverInfo.getDhcpServerIp4().isPresent()) {
            log.warn("DhcpServerIp not available, use default DhcpServerIp {}",
                    HexString.toHexString(serverInfo.getDhcpServerIp4().get().toOctets()));
            return true;
        }
        return false;
    }
}

