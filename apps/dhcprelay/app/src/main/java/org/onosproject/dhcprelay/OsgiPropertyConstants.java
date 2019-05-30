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

package org.onosproject.dhcprelay;

public final class OsgiPropertyConstants {
    private OsgiPropertyConstants() {
    }

    static final String ARP_ENABLED = "arpEnabled";
    static final boolean ARP_ENABLED_DEFAULT = true;

    static final String DHCP_POLL_INTERVAL = "dhcpPollInterval";
    static final int DHCP_POLL_INTERVAL_DEFAULT = 86400;

    static final String DHCP_FPM_ENABLED = "dhcpFpmEnabled";
    static final boolean DHCP_FPM_ENABLED_DEFAULT = false;

    static final String DHCP_PROBE_INTERVAL = "dhcpHostRelearnProbeInterval";
    static final int DHCP_PROBE_INTERVAL_DEFAULT = 500;

    static final String DHCP_PROBE_COUNT = "dhcpHostRelearnProbeCount";
    static final int DHCP_PROBE_COUNT_DEFAULT = 3;

    static final String LEARN_ROUTE_FROM_LEASE_QUERY = "learnRouteFromLeasequery";
    static final boolean LEARN_ROUTE_FROM_LEASE_QUERY_DEFAULT = false;
}
