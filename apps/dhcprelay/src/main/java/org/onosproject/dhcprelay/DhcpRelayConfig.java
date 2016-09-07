/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.dhcprelay;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.Config;

import static org.onosproject.net.config.Config.FieldPresence.MANDATORY;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
/**
 * DHCP Relay Config class.
 */
public class DhcpRelayConfig extends Config<ApplicationId> {

    private static final String DHCP_CONNECT_POINT = "dhcpserverConnectPoint";
    private static final String DHCP_SERVER_IP = "serverip";
    private static final String DHCP_SERVER_MAC = "servermac";

    @Override
    public boolean isValid() {

        return hasOnlyFields(DHCP_CONNECT_POINT, DHCP_SERVER_IP, DHCP_SERVER_MAC) &&
                isConnectPoint(DHCP_CONNECT_POINT, MANDATORY) &&
                isIpAddress(DHCP_SERVER_IP, MANDATORY) &&
                isMacAddress(DHCP_SERVER_MAC, MANDATORY);
    }

    /**
     * Returns the dhcp server connect point.
     *
     * @return dhcp server connect point
     */
    public ConnectPoint getDhcpServerConnectPoint() {
        return ConnectPoint.deviceConnectPoint(object.path(DHCP_CONNECT_POINT).asText());
    }

    /**
     * Returns the dhcp server ip.
     *
     * @return ip address or null if not set
     */
    public Ip4Address getDhcpServerIp() {
        String ip = get(DHCP_SERVER_IP, null);
        return ip != null ? Ip4Address.valueOf(ip) : null;
    }

    /**
     * Returns the dhcp server mac.
     *
     * @return server mac or null if not set
     */
    public MacAddress getDhcpServermac() {
        String mac = get(DHCP_SERVER_MAC, null);
        return mac != null ? MacAddress.valueOf(mac) : null;
    }
}
