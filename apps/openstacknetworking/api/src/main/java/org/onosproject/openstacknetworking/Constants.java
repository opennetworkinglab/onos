/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.openstacknetworking;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;

/**
 * Provides constants used in OpenStackSwitching.
 */
public final class Constants {

    private Constants() {
    }

    public static final String SWITCHING_APP_ID = "org.onosproject.openstackswitching";
    public static final String ROUTING_APP_ID = "org.onosproject.openstackrouting";

    public static final String DEVICE_OWNER_ROUTER_INTERFACE = "network:router_interface";
    public static final String DEVICE_OWNER_ROUTER_GATEWAY = "network:router_gateway";
    public static final String DEVICE_OWNER_FLOATING_IP = "network:floatingip";

    public static final String PORT_NAME_PREFIX_VM = "tap";
    public static final String PORT_NAME_PREFIX_TUNNEL = "vxlan";

    public static final String DEFAULT_GATEWAY_MAC_STR = "fe:00:00:00:00:02";
    public static final MacAddress DEFAULT_GATEWAY_MAC = MacAddress.valueOf(DEFAULT_GATEWAY_MAC_STR);
    // TODO make this configurable
    public static final MacAddress DEFAULT_EXTERNAL_ROUTER_MAC = MacAddress.valueOf("fe:00:00:00:00:01");

    public static final Ip4Address DNS_SERVER_IP = Ip4Address.valueOf("8.8.8.8");
    public static final IpPrefix IP_PREFIX_ANY = Ip4Prefix.valueOf("0.0.0.0/0");
    public static final int DHCP_INFINITE_LEASE = -1;

    public static final String NETWORK_ID = "networkId";
    public static final String SUBNET_ID = "subnetId";
    public static final String PORT_ID = "portId";
    public static final String VXLAN_ID = "vxlanId";
    public static final String TENANT_ID = "tenantId";
    public static final String GATEWAY_IP = "gatewayIp";
    public static final String CREATE_TIME = "createTime";

    public static final int SWITCHING_RULE_PRIORITY = 30000;
    public static final int TUNNELTAG_RULE_PRIORITY = 30000;
    public static final int ACL_RULE_PRIORITY = 30000;
    public static final int EW_ROUTING_RULE_PRIORITY = 28000;

    public static final int GATEWAY_ICMP_PRIORITY = 43000;
    public static final int ROUTING_RULE_PRIORITY = 25000;
    public static final int FLOATING_RULE_FOR_TRAFFIC_FROM_VM_PRIORITY = 42000;
    public static final int FLOATING_RULE_PRIORITY = 41000;
    public static final int PNAT_RULE_PRIORITY = 26000;
    public static final int PNAT_TIMEOUT = 120;
}