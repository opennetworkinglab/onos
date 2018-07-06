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
package org.onosproject.openstacknetworking.api;

import com.google.common.collect.ImmutableMap;
import org.onlab.packet.MacAddress;

import java.util.Map;

/**
 * Provides constants used in OpenStackSwitching.
 */
public final class Constants {

    private Constants() {
    }

    public static final String OPENSTACK_NETWORKING_APP_ID = "org.onosproject.openstacknetworking";

    public static final String ARP_BROADCAST_MODE = "broadcast";
    public static final String ARP_PROXY_MODE = "proxy";

    public static final String DEFAULT_GATEWAY_MAC_STR = "fe:00:00:00:00:02";
    public static final String DEFAULT_ARP_MODE_STR = ARP_PROXY_MODE;
    public static final MacAddress DEFAULT_GATEWAY_MAC = MacAddress.valueOf(DEFAULT_GATEWAY_MAC_STR);
    public static final MacAddress DEFAULT_EXTERNAL_ROUTER_MAC = MacAddress.valueOf("fe:00:00:00:00:01");

    private static final String CAVIUM_PCI_VENDOR_INFO = "177d:9712";
    private static final String PORT_NAME_PREFIX_CAVIUM = "enp";
    public static final String PCI_VENDOR_INFO = "pci_vendor_info";
    public static final String DIRECT = "direct";
    public static final String PCISLOT = "pci_slot";

    private static final Map<String, String> PORT_NAME_PREFIX_MAP = createPortNamePrefixMap();
    private static Map<String, String> createPortNamePrefixMap() {
        //Additional pci vendor information will be added
        return ImmutableMap.of(CAVIUM_PCI_VENDOR_INFO, PORT_NAME_PREFIX_CAVIUM);
    }

    public static final int PRIORITY_TUNNEL_TAG_RULE = 30000;
    public static final int PRIORITY_FLOATING_INTERNAL = 42000;
    public static final int PRIORITY_FLOATING_EXTERNAL = 41000;
    public static final int PRIORITY_STATEFUL_SNAT_RULE = 40500;
    public static final int PRIORITY_ICMP_RULE = 43000;
    public static final int PRIORITY_INTERNAL_ROUTING_RULE = 28000;
    public static final int PRIORITY_EXTERNAL_ROUTING_RULE = 25000;
    public static final int PRIORITY_EXTERNAL_FLOATING_ROUTING_RULE = 27000;
    public static final int PRIORITY_SNAT_RULE = 26000;
    public static final int PRIORITY_SWITCHING_RULE = 30000;
    public static final int PRIORITY_FLAT_JUMP_UPSTREAM_RULE = 41000;
    public static final int PRIORITY_FLAT_JUMP_DOWNSTREAM_RULE = 41000;
    public static final int PRIORITY_FLAT_UPSTREAM_RULE = 41000;
    public static final int PRIORITY_FLAT_DOWNSTREAM_RULE = 42000;
    public static final int PRIORITY_DHCP_RULE = 42000;
    public static final int PRIORITY_ADMIN_RULE = 32000;
    public static final int PRIORITY_ACL_RULE = 31000;
    public static final int PRIORITY_CT_HOOK_RULE = 30500;
    public static final int PRIORITY_CT_RULE = 32000;
    public static final int PRIORITY_CT_DROP_RULE = 32500;
    public static final int PRIORITY_ARP_GATEWAY_RULE = 41000;
    public static final int PRIORITY_ARP_SUBNET_RULE = 40000;
    public static final int PRIORITY_ARP_CONTROL_RULE = 40000;
    public static final int PRIORITY_ARP_REPLY_RULE = 40000;
    public static final int PRIORITY_ARP_REQUEST_RULE = 40000;

    // flow table index
    public static final int STAT_INBOUND_TABLE = 0;
    public static final int VTAP_INBOUND_TABLE = 1;
    public static final int VTAP_INBOUND_MIRROR_TABLE = 2;
    public static final int STAT_FLAT_OUTBOUND_TABLE = 10;
    public static final int VTAP_FLAT_OUTBOUND_TABLE = 11;
    public static final int VTAP_FLAT_OUTBOUND_MIRROR_TABLE = 12;
    public static final int DHCP_ARP_TABLE = 5;
    public static final int FLAT_TABLE = 20;
    public static final int VTAG_TABLE = 30;
    public static final int ACL_TABLE = 40;
    public static final int CT_TABLE = 41;
    public static final int JUMP_TABLE = 50;
    public static final int ROUTING_TABLE = 60;
    public static final int STAT_OUTBOUND_TABLE = 70;
    public static final int VTAP_OUTBOUND_TABLE = 71;
    public static final int VTAP_OUTBOUND_MIRROR_TABLE = 72;
    public static final int FORWARDING_TABLE = 80;
    public static final int GW_COMMON_TABLE = 0;
    public static final int ERROR_TABLE = 100;

    // group table index
    public static final int VTAP_INBOUND_GROUP_TABLE = 1;
    public static final int VTAP_FLAT_OUTBOUND_GROUP_TABLE = 2;
    public static final int VTAP_OUTBOUND_GROUP_TABLE = 3;

    public static Map<String, String> portNamePrefixMap() {
        return PORT_NAME_PREFIX_MAP;
    }
}