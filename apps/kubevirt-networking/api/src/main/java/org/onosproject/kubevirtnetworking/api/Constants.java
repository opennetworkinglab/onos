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

    public static final String TENANT_TO_TUNNEL_PREFIX = "i-to-t-";
    public static final String TUNNEL_TO_TENANT_PREFIX = "t-to-i-";

    // provider and VLAN integration bridge flow table index
    public static final int STAT_INBOUND_TABLE = 0;
    public static final int VTAP_INBOUND_TABLE = 1;
    public static final int DHCP_TABLE = 5;
    public static final int GW_ENTRY_TABLE = 31;
    public static final int GW_DROP_TABLE = 32;
    public static final int ARP_TABLE = 35;
    public static final int ACL_EGRESS_TABLE = 40;
    public static final int ACL_RECIRC_TABLE = 43;
    public static final int ACL_INGRESS_TABLE = 44;
    public static final int ACL_CT_TABLE = 45;
    public static final int STAT_OUTBOUND_TABLE = 70;
    public static final int VTAP_OUTBOUND_TABLE = 71;
    public static final int FORWARDING_TABLE = 80;
    public static final int ERROR_TABLE = 100;

    // tenant (VXLAN/GRE/GENEVE) integration bridge flow table index
    public static final int TENANT_INBOUND_TABLE = 0;
    public static final int TENANT_DHCP_TABLE = 5;
    public static final int TENANT_ARP_TABLE = 30;
    public static final int TENANT_ICMP_TABLE = 35;
    public static final int TENANT_ACL_EGRESS_TABLE = 40;
    public static final int TENANT_ACL_RECIRC_TABLE = 43;
    public static final int TENANT_ACL_INGRESS_TABLE = 44;
    public static final int TENANT_ACL_CT_TABLE = 45;
    public static final int TENANT_FORWARDING_TABLE = 80;

    // tunnel bridge flow table index
    public static final int TUNNEL_DEFAULT_TABLE = 0;

    // flow rule priority
    public static final int PRIORITY_ICMP_RULE = 43000;
    public static final int PRIORITY_FORWARDING_RULE = 30000;
    public static final int PRIORITY_FLOATING_GATEWAY_TUN_BRIDGE_RULE = 32000;
    public static final int PRIORITY_LB_GATEWAY_TUN_BRIDGE_RULE = 33000;
    public static final int PRIORITY_DHCP_RULE = 42000;
    public static final int PRIORITY_ARP_GATEWAY_RULE = 41000;
    public static final int PRIORITY_ARP_DEFAULT_RULE = 40000;
    public static final int PRIORITY_TUNNEL_RULE = 31000;

    public static final int PRIORITY_IP_INGRESS_RULE = 19000;
    public static final int PRIORITY_IP_EGRESS_RULE = 18000;
    public static final int PRIORITY_ACL_RULE = 31000;
    public static final int PRIORITY_ACL_INGRESS_RULE = 30000;
    public static final int PRIORITY_CT_HOOK_RULE = 30500;
    public static final int PRIORITY_CT_RULE = 32000;
    public static final int PRIORITY_CT_DROP_RULE = 32500;

    // CLI item length
    public static final int CLI_ID_LENGTH = 30;
    public static final int CLI_NAME_LENGTH = 30;
    public static final int CLI_LONG_NAME_LENGTH = 50;
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
    public static final int CLI_FLAG_LENGTH = 10;
    public static final int CLI_NUMBER_LENGTH = 10;
    public static final int CLI_MARGIN_LENGTH = 2;

    public static final int PRIORITY_STATEFUL_SNAT_RULE = 40500;
    public static final int PRIORITY_FLOATING_IP_UPSTREAM_RULE = 40800;
    public static final int PRIORITY_FLOATING_IP_DOWNSTREAM_RULE = 40700;

    public static final int PRIORITY_INTERNAL_ROUTING_RULE = 41000;
    public static final int PRIORITY_LB_RULE = 41500;
    public static final int PRIORITY_LB_FIP_RULE = 41500;
}
