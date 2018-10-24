/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.openstacknetworking.impl;

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

    static final String USE_STATEFUL_SNAT = "useStatefulSnat";
    static final boolean USE_STATEFUL_SNAT_DEFAULT = false;

    static final String USE_SECURITY_GROUP = "useSecurityGroup";
    static final boolean USE_SECURITY_GROUP_DEFAULT = false;

    static final String DHCP_SERVER_MAC = "dhcpServerMac";
    static final String DHCP_SERVER_MAC_DEFAULT = "fe:00:00:00:00:02";
}
