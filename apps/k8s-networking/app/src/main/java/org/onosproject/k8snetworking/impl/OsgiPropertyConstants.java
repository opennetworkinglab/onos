/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.impl;

/**
 * Name/Value constants for properties.
 */
public final class OsgiPropertyConstants {

    private OsgiPropertyConstants() {
    }

    static final String GATEWAY_MAC = "gatewayMac";
    static final String GATEWAY_MAC_DEFAULT = "fe:00:00:00:00:02";

    static final String ARP_MODE = "arpMode";
    static final String ARP_MODE_DEFAULT = "proxy";

    static final String DHCP_SERVER_MAC = "dhcpServerMac";
    static final String DHCP_SERVER_MAC_DEFAULT = "fe:00:00:00:00:02";

    static final String SERVICE_IP_NAT_MODE = "serviceIpNatMode";
    static final String SERVICE_IP_NAT_MODE_DEFAULT = "stateless";
    static final String SERVICE_CIDR = "serviceCidr";
    static final String SERVICE_IP_CIDR_DEFAULT = "10.96.0.0/12";
}
