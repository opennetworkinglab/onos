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

    public static final String DEFAULT_GATEWAY_MAC_STR = "fe:00:00:00:00:02";
    public static final String DEFAULT_HOST_MAC_STR = "fe:00:00:00:00:08";
    public static final MacAddress DEFAULT_GATEWAY_MAC =
                        MacAddress.valueOf(DEFAULT_GATEWAY_MAC_STR);

    public static final String PORT_NAME_PREFIX_CONTAINER = "veth";

    public static final String ANNOTATION_NETWORK_ID = "networkId";
    public static final String ANNOTATION_PORT_ID = "portId";
    public static final String ANNOTATION_CREATE_TIME = "createTime";
    public static final String ANNOTATION_SEGMENT_ID = "segId";

    // flow priority
    public static final int PRIORITY_SNAT_RULE = 26000;
    public static final int PRIORITY_TUNNEL_TAG_RULE = 30000;
    public static final int PRIORITY_DHCP_RULE = 42000;
    public static final int PRIORITY_ADMIN_RULE = 32000;
    public static final int PRIORITY_ACL_RULE = 31000;
    public static final int PRIORITY_ACL_INGRESS_RULE = 30000;
    public static final int PRIORITY_CT_HOOK_RULE = 30500;
    public static final int PRIORITY_CT_RULE = 32000;
    public static final int PRIORITY_CT_DROP_RULE = 32500;
    public static final int PRIORITY_SWITCHING_RULE = 30000;
    public static final int PRIORITY_ARP_GATEWAY_RULE = 41000;
    public static final int PRIORITY_ARP_SUBNET_RULE = 40000;
    public static final int PRIORITY_ARP_CONTROL_RULE = 40000;
    public static final int PRIORITY_ARP_REPLY_RULE = 40000;
    public static final int PRIORITY_ARP_REQUEST_RULE = 40000;
    public static final int PRIORITY_ARP_FLOOD_RULE = 39000;
    public static final int PRIORITY_FORCED_ACL_RULE = 50000;
    public static final int PRIORITY_ICMP_PROBE_RULE = 50000;

    // flow table index
    public static final int STAT_INBOUND_TABLE = 0;
    public static final int VTAP_INBOUND_TABLE = 1;
    public static final int VTAP_INBOUND_MIRROR_TABLE = 2;
    public static final int STAT_FLAT_OUTBOUND_TABLE = 10;
    public static final int VTAP_FLAT_OUTBOUND_TABLE = 11;
    public static final int VTAP_FLAT_OUTBOUND_MIRROR_TABLE = 12;
    public static final int DHCP_TABLE = 5;
    public static final int VTAG_TABLE = 30;
    public static final int ARP_TABLE = 35;
    public static final int ACL_EGRESS_TABLE = 40;
    public static final int ACL_INGRESS_TABLE = 44;
    public static final int CT_TABLE = 45;
    public static final int ACL_RECIRC_TABLE = 43;
    public static final int JUMP_TABLE = 50;
    public static final int ROUTING_TABLE = 60;
    public static final int STAT_OUTBOUND_TABLE = 70;
    public static final int VTAP_OUTBOUND_TABLE = 71;
    public static final int VTAP_OUTBOUND_MIRROR_TABLE = 72;
    public static final int FORWARDING_TABLE = 80;
    public static final int ERROR_TABLE = 100;

    // group table index
    public static final int VTAP_INBOUND_GROUP_TABLE = 1;
    public static final int VTAP_FLAT_OUTBOUND_GROUP_TABLE = 2;
    public static final int VTAP_OUTBOUND_GROUP_TABLE = 3;
}
