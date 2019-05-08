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

package org.onosproject.provider.host.impl;

/**
 * Name/Value constants for properties.
 */

public final class OsgiPropertyConstants {

    private OsgiPropertyConstants() {
    }

    public static final String HOST_REMOVAL_ENABLED = "hostRemovalEnabled:Boolean=true";
    public static final boolean HOST_REMOVAL_ENABLED_DEFAULT = true;

    public static final String REQUEST_ARP = "requestArp";
    public static final boolean REQUEST_ARP_DEFAULT = true;

    public static final String REQUEST_NDP = "requestIpv6ND";
    public static final boolean REQUEST_NDP_DEFAULT = false;

    public static final String REQUEST_NDP_RS_RA = "requestIpv6NdpRsRa";
    public static final boolean REQUEST_NDP_RS_RA_DEFAULT = false;

    public static final String USE_DHCP = "useDhcp";
    public static final boolean USE_DHCP_DEFAULT = false;

    public static final String USE_DHCP6 = "useDhcp6";
    public static final boolean USE_DHCP6_DEFAULT = false;

    public static final String REQUEST_INTERCEPTS_ENABLED = "requestInterceptsEnabled";
    public static final boolean REQUEST_INTERCEPTS_ENABLED_DEFAULT = true;

    public static final String MULTIHOMING_ENABLED = "multihomingEnabled";
    public static final boolean MULTIHOMING_ENABLED_DEFAULT = false;
}
