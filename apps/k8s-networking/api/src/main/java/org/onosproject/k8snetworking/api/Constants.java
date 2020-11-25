/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.k8snetworking.api;

import org.onlab.packet.MacAddress;

/**
 * Provides constants used in kubernetes network switching and routing.
 */
public final class Constants {

    private Constants() {
    }

    public static final String K8S_NETWORKING_APP_ID = "org.onosproject.k8snetworking";

    public static final String ARP_BROADCAST_MODE = "broadcast";
    public static final String ARP_PROXY_MODE = "proxy";

    public static final String NAT_STATEFUL = "stateful";
    public static final String NAT_STATELESS = "stateless";

    public static final String DEFAULT_GATEWAY_MAC_STR = "fe:00:00:00:00:02";
    public static final String DEFAULT_ARP_MODE_STR = ARP_PROXY_MODE;
    public static final String DEFAULT_HOST_MAC_STR = "fe:00:00:00:00:08";
    public static final String DEFAULT_SERVICE_IP_NAT_MODE_STR = NAT_STATELESS;
    public static final String CONTROLLER_MAC_STR = "fe:00:00:00:00:10";
    public static final String SERVICE_FAKE_MAC_STR = "fe:00:00:00:00:20";
    public static final String NODE_FAKE_IP_STR = "172.172.172.172";
    public static final String NODE_FAKE_MAC_STR = "fe:00:00:00:00:80";

    public static final MacAddress DEFAULT_GATEWAY_MAC =
                        MacAddress.valueOf(DEFAULT_GATEWAY_MAC_STR);

    public static final String SHIFTED_IP_CIDR = "172.10.0.0/16";
    public static final String SHIFTED_IP_PREFIX = "172.10";
    public static final String SHIFTED_LOCAL_IP_PREFIX = "172.11";
    public static final String NODE_IP_PREFIX = "182";

    public static final String SRC = "src";
    public static final String DST = "dst";
    public static final String A_CLASS = "a";
    public static final String B_CLASS = "b";

    public static final String DEFAULT_SERVICE_IP_CIDR = "10.96.0.0/12";
    public static final String DEFAULT_SERVICE_IP_NONE = "none";

    public static final String NORMAL_PORT_NAME_PREFIX_CONTAINER = "veth";
    public static final String PT_PORT_NAME_PREFIX_CONTAINER = "tap";
    public static final int NORMAL_PORT_PREFIX_LENGTH = 4;
    public static final int PT_PORT_PREFIX_LENGTH = 3;

    public static final String ANNOTATION_NETWORK_ID = "networkId";
    public static final String ANNOTATION_PORT_ID = "portId";
    public static final String ANNOTATION_CREATE_TIME = "createTime";
    public static final String ANNOTATION_SEGMENT_ID = "segId";

    // network type
    public static final String VXLAN = "VXLAN";
    public static final String GRE = "GRE";
    public static final String GENEVE = "GENEVE";

    public static final long DEFAULT_METADATA_MASK = 0xffffffffffffffffL;
    public static final int DEFAULT_NAMESPACE_HASH = 0xffffffff;

    public static final int DEFAULT_SEGMENT_ID = 100;

    public static final int HOST_PREFIX = 32;

    // flow priority
    public static final int PRIORITY_SNAT_RULE = 26000;
    public static final int PRIORITY_TUNNEL_TAG_RULE = 30000;
    public static final int PRIORITY_TRANSLATION_RULE = 30000;
    public static final int PRIORITY_CT_HOOK_RULE = 30500;
    public static final int PRIORITY_INTER_ROUTING_RULE = 29000;
    public static final int PRIORITY_CT_RULE = 32000;
    public static final int PRIORITY_CT_DROP_RULE = 32500;
    public static final int PRIORITY_NAT_RULE = 30000;
    public static final int PRIORITY_GATEWAY_RULE = 31000;
    public static final int PRIORITY_INTER_NODE_RULE = 33000;
    public static final int PRIORITY_LOCAL_BRIDGE_RULE = 32000;
    public static final int PRIORITY_SWITCHING_RULE = 30000;
    public static final int PRIORITY_CIDR_RULE = 30000;
    public static final int PRIORITY_NAMESPACE_RULE = 31000;
    public static final int PRIORITY_STATEFUL_SNAT_RULE = 41000;
    public static final int PRIORITY_EXTERNAL_ROUTING_RULE = 25000;
    public static final int PRIORITY_ARP_CONTROL_RULE = 40000;
    public static final int PRIORITY_ARP_REPLY_RULE = 40000;
    public static final int PRIORITY_ARP_POD_RULE = 39000;
    public static final int PRIORITY_ARP_FLOOD_RULE = 39000;
    public static final int PRIORITY_FORCED_ACL_RULE = 50000;
    public static final int PRIORITY_ICMP_PROBE_RULE = 50000;
    public static final int PRIORITY_NODE_PORT_RULE = 42000;
    public static final int PRIORITY_ROUTER_RULE = 10000;
    public static final int PRIORITY_DEFAULT_RULE = 0;

    // flow table index
    public static final int STAT_INGRESS_TABLE = 0;
    public static final int VTAP_INGRESS_TABLE = 1;
    public static final int VTAP_INGRESS_MIRROR_TABLE = 2;
    public static final int VTAG_TABLE = 30;
    public static final int ARP_TABLE = 35;
    public static final int JUMP_TABLE = 40;
    public static final int NAMESPACE_TABLE = 49;
    public static final int GROUPING_TABLE = 50;
    public static final int NAT_TABLE = 51;
    public static final int SERVICE_TABLE = 52;
    public static final int POD_TABLE = 53;
    public static final int ACL_TABLE = 55;
    public static final int ACL_INGRESS_WHITE_TABLE = 56;
    public static final int ACL_INGRESS_BLACK_TABLE = 57;
    public static final int ACL_EGRESS_WHITE_TABLE = 58;
    public static final int ACL_EGRESS_BLACK_TABLE = 59;
    public static final int ROUTING_TABLE = 60;
    public static final int STAT_EGRESS_TABLE = 70;
    public static final int VTAP_EGRESS_TABLE = 71;
    public static final int VTAP_EGRESS_MIRROR_TABLE = 72;
    public static final int FORWARDING_TABLE = 80;
    public static final int ERROR_TABLE = 100;

    public static final int EXT_ENTRY_TABLE = 0;
    public static final int POD_RESOLUTION_TABLE = 11;

    public static final int ROUTER_ENTRY_TABLE = 0;
    public static final int EXT_RESOLUTION_TABLE = 11;

    public static final int LOCAL_ENTRY_TABLE = 0;

    public static final int TUN_ENTRY_TABLE = 0;

    // CLI item length
    public static final int CLI_ID_LENGTH = 30;
    public static final int CLI_NAME_LENGTH = 30;
    public static final int CLI_IP_ADDRESSES_LENGTH = 50;
    public static final int CLI_IP_ADDRESS_LENGTH = 25;
    public static final int CLI_MAC_ADDRESS_LENGTH = 25;
    public static final int CLI_PORTS_LENGTH = 20;
    public static final int CLI_NAMESPACE_LENGTH = 15;
    public static final int CLI_PHASE_LENGTH = 15;
    public static final int CLI_TYPE_LENGTH = 15;
    public static final int CLI_TYPES_LENGTH = 30;
    public static final int CLI_SEG_ID_LENGTH = 10;
    public static final int CLI_LABELS_LENGTH = 30;
    public static final int CLI_CONTAINERS_LENGTH = 30;

    public static final int CLI_MARGIN_LENGTH = 2;
}
