/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.api;

import org.onlab.packet.MacAddress;

/**
 * Provides constants used in KubevirtNetworking.
 */
public final class Constants {

    private Constants() {
    }

    public static final String KUBEVIRT_NETWORKING_APP_ID =
            "org.onosproject.kubevirtnetworking";
    public static final String DEFAULT_GATEWAY_MAC_STR = "fe:00:00:00:00:02";
    public static final MacAddress DEFAULT_GATEWAY_MAC =
                        MacAddress.valueOf(DEFAULT_GATEWAY_MAC_STR);

    // flow table index
    public static final int STAT_INBOUND_TABLE = 0;
    public static final int VTAP_INBOUND_TABLE = 1;
    public static final int STAT_FLAT_OUTBOUND_TABLE = 10;
    public static final int DHCP_TABLE = 5;
    public static final int VTAG_TABLE = 30;
    public static final int PRE_FLAT_TABLE = 31;
    public static final int FLAT_TABLE = 32;
    public static final int ARP_TABLE = 35;
    public static final int ACL_EGRESS_TABLE = 40;
    public static final int ACL_INGRESS_TABLE = 44;
    public static final int CT_TABLE = 45;
    public static final int ACL_RECIRC_TABLE = 43;
    public static final int JUMP_TABLE = 50;
    public static final int ROUTING_TABLE = 60;
    public static final int STAT_OUTBOUND_TABLE = 70;
    public static final int VTAP_OUTBOUND_TABLE = 71;
    public static final int FORWARDING_TABLE = 80;
    public static final int ERROR_TABLE = 100;

    // flow rule priority
    public static final int PRIORITY_SWITCHING_RULE = 30000;

    // CLI item length
    public static final int CLI_ID_LENGTH = 30;
    public static final int CLI_NAME_LENGTH = 30;
    public static final int CLI_IP_ADDRESSES_LENGTH = 50;
    public static final int CLI_IP_ADDRESS_LENGTH = 25;
    public static final int CLI_IP_ADDRESS_AVAILABILITY = 15;
    public static final int CLI_MAC_ADDRESS_LENGTH = 25;
    public static final int CLI_PORTS_LENGTH = 20;
    public static final int CLI_NAMESPACE_LENGTH = 15;
    public static final int CLI_STATUS_LENGTH = 15;
    public static final int CLI_PHASE_LENGTH = 15;
    public static final int CLI_TYPE_LENGTH = 15;
    public static final int CLI_TYPES_LENGTH = 30;
    public static final int CLI_SEG_ID_LENGTH = 10;
    public static final int CLI_LABELS_LENGTH = 30;
    public static final int CLI_CONTAINERS_LENGTH = 30;
    public static final int CLI_MARGIN_LENGTH = 2;
}
