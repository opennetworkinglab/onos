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

package org.onosproject.packetthrottle;

/**
 * Constants for default values of configurable properties.
 */
public final class OsgiPropertyConstants {

    private OsgiPropertyConstants() {}

    public static final String PROP_PPS_ARP = "ppsArp";
    public static final int PPS_ARP_DEFAULT = 100;

    public static final String PROP_PPS_DHCP = "ppsDhcp";
    public static final int PPS_DHCP_DEFAULT = 100;

    public static final String PROP_PPS_NS = "ppsNs";
    public static final int PPS_NS_DEFAULT = 100;

    public static final String PROP_PPS_NA = "ppsNa";
    public static final int PPS_NA_DEFAULT = 100;

    public static final String PROP_PPS_DHCP6_DIRECT = "ppsDhcp6Direct";
    public static final int PPS_DHCP6_DIRECT_DEFAULT = 100;

    public static final String PROP_PPS_DHCP6_INDIRECT = "ppsDhcp6Indirect";
    public static final int PPS_DHCP6_INDIRECT_DEFAULT = 100;

    public static final String PROP_PPS_ICMP = "ppsIcmp";
    public static final int PPS_ICMP_DEFAULT = 100;

    public static final String PROP_PPS_ICMP6 = "ppsIcmp6";
    public static final int PPS_ICMP6_DEFAULT = 100;

    public static final String PROP_WIN_SIZE_ARP_MS = "winSizeArp";
    public static final int WIN_SIZE_ARP_MS_DEFAULT = 500;

    public static final String PROP_WIN_SIZE_DHCP_MS = "winSizeDhcp";
    public static final int WIN_SIZE_DHCP_MS_DEFAULT = 500;

    public static final String PROP_WIN_SIZE_NA_MS = "winSizeNa";
    public static final int WIN_SIZE_NA_MS_DEFAULT = 500;

    public static final String PROP_WIN_SIZE_NS_MS = "winSizeNs";
    public static final int WIN_SIZE_NS_MS_DEFAULT = 500;

    public static final String PROP_WIN_SIZE_DHCP6_DIRECT_MS = "winSizeDhcp6Direct";
    public static final int WIN_SIZE_DHCP6_DIRECT_MS_DEFAULT = 500;

    public static final String PROP_WIN_SIZE_DHCP6_INDIRECT_MS = "winSizeDhcp6Indirect";
    public static final int WIN_SIZE_DHCP6_INDIRECT_MS_DEFAULT = 500;

    public static final String PROP_WIN_SIZE_ICMP_MS = "winSizeIcmp";
    public static final int WIN_SIZE_ICMP_MS_DEFAULT = 500;

    public static final String PROP_WIN_SIZE_ICMP6_MS = "winSizeIcmp6";
    public static final int WIN_SIZE_ICMP6_MS_DEFAULT = 500;

    public static final String PROP_GUARD_TIME_ARP_SEC = "guardTimeArp";
    public static final int GUARD_TIME_ARP_SEC_DEFAULT = 10;

    public static final String PROP_GUARD_TIME_DHCP_SEC = "guardTimeDhcp";
    public static final int GUARD_TIME_DHCP_SEC_DEFAULT = 10;

    public static final String PROP_GUARD_TIME_NS_SEC = "guardTimeNs";
    public static final int GUARD_TIME_NS_SEC_DEFAULT = 10;

    public static final String PROP_GUARD_TIME_NA_SEC = "guardTimeNa";
    public static final int GUARD_TIME_NA_SEC_DEFAULT = 10;

    public static final String PROP_GUARD_TIME_DHCP6_DIRECT_SEC = "guardTimeDhcp6Direct";
    public static final int GUARD_TIME_DHCP6_DIRECT_SEC_DEFAULT = 10;

    public static final String PROP_GUARD_TIME_DHCP6_INDIRECT_SEC = "guardTimeDhcp6Indirect";
    public static final int GUARD_TIME_DHCP6_INDIRECT_SEC_DEFAULT = 10;

    public static final String PROP_GUARD_TIME_ICMP_SEC = "guardTimeIcmp";
    public static final int GUARD_TIME_ICMP_SEC_DEFAULT = 10;

    public static final String PROP_GUARD_TIME_ICMP6_SEC = "guardTimeIcmp6";
    public static final int GUARD_TIME_ICMP6_SEC_DEFAULT = 10;

    public static final String PROP_WIN_THRES_ARP = "winThresArp";
    public static final int WIN_THRES_ARP_DEFAULT = 10;

    public static final String PROP_WIN_THRES_DHCP = "winThresDhcp";
    public static final int WIN_THRES_DHCP_DEFAULT = 10;

    public static final String PROP_WIN_THRES_NS = "winThresNs";
    public static final int WIN_THRES_NS_DEFAULT = 10;

    public static final String PROP_WIN_THRES_NA = "winThresNa";
    public static final int WIN_THRES_NA_DEFAULT = 10;

    public static final String PROP_WIN_THRES_DHCP6_DIRECT = "winThresDhcp6Direct";
    public static final int WIN_THRES_DHCP6_DIRECT_DEFAULT = 10;

    public static final String PROP_WIN_THRES_DHCP6_INDIRECT = "winThresDhcp6Indirect";
    public static final int WIN_THRES_DHCP6_INDIRECT_DEFAULT = 10;

    public static final String PROP_WIN_THRES_ICMP = "winThresIcmp";
    public static final int WIN_THRES_ICMP_DEFAULT = 10;

    public static final String PROP_WIN_THRES_ICMP6 = "winThresIcmp6";
    public static final int WIN_THRES_ICMP6_DEFAULT = 10;

}
