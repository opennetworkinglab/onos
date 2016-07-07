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

    public static final String APP_ID = "org.onosproject.openstackswitching";

    public static final String PORTNAME_PREFIX_VM = "tap";
    public static final String PORTNAME_PREFIX_ROUTER = "qr-";
    public static final String PORTNAME_PREFIX_TUNNEL = "vxlan";

    public static final MacAddress GATEWAY_MAC = MacAddress.valueOf("1f:1f:1f:1f:1f:1f");

    // TODO: Please change these valuses following the way vrouter is implemented
    public static final MacAddress GW_EXT_INT_MAC = MacAddress.valueOf("56:e6:30:a6:8c:e5");
    public static final MacAddress PHY_ROUTER_MAC = MacAddress.valueOf("00:00:00:00:01:01");

    public static final Ip4Address DNS_SERVER_IP = Ip4Address.valueOf("8.8.8.8");
    public static final IpPrefix IP_PREFIX_ANY = Ip4Prefix.valueOf("0.0.0.0/0");
    public static final int DHCP_INFINITE_LEASE = -1;

    public static final String NETWORK_ID = "networkId";
    public static final String PORT_ID = "portId";
    public static final String VXLAN_ID = "vxlanId";
    public static final String TENANT_ID = "tenantId";
    public static final String GATEWAY_IP = "gatewayIp";
    public static final String CREATE_TIME = "createTime";

    public static final int SWITCHING_RULE_PRIORITY = 30000;
    public static final int TUNNELTAG_RULE_PRIORITY = 30000;
    public static final int ACL_RULE_PRIORITY = 30000;

}