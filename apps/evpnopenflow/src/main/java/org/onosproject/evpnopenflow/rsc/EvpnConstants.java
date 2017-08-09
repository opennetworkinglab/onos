/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.evpnopenflow.rsc;

/**
 * Provides constants used in EVPN openflow application.
 */
public final class EvpnConstants {
    private EvpnConstants() {
    }

    public static final String APP_ID = "org.onosproject.evpnopenflow";
    public static final String EVPN_OPENFLOW_START = "evpn-openflow app is " +
            "started";
    public static final String EVPN_OPENFLOW_STOP = "evpn-openflow app is " +
            "stopped";
    public static final String EVPN_VPN_PORT_START = "EVPN port started";
    public static final String EVPN_VPN_PORT_STOP = "EVPN port stopped";
    public static final String EVPN_VPN_INSTANCE_START = "EVPN instance " +
            "started";
    public static final String EVPN_VPN_INSTANCE_STOP = "EVPN instance " +
            "stopped";
    public static final String HOST_DETECT = "Host detected {}";
    public static final String HOST_VANISHED = "Host vanished {}";
    public static final String IFACEID = "ifaceid";
    public static final String IFACEID_OF_HOST_IS_NULL =
            "The ifaceId of host is null";
    public static final String CANT_FIND_VPN_PORT = "Can't find vpnport {}";
    public static final String CANT_FIND_VPN_INSTANCE = "EVPN instance {} is " +
            "not exist";
    public static final String CANT_FIND_CONTROLLER_DEVICE = "Can't find " +
            "controller of device: {}";
    public static final String GET_PRIVATE_LABEL = "Get private label {}";
    public static final String RELEASE_LABEL_FAILED = "Release resoure label " +
            "{} failed";
    public static final String VPN_PORT_UNBIND = "On EVPN port unbind";
    public static final String VPN_PORT_BIND = "On EVPN port bind";
    public static final String SLASH = "/";
    public static final String COMMA = ",";
    public static final String VPN_INSTANCE_TARGET = "VpnService";
    public static final String VPN_PORT_TARGET = "VpnBinding";
    public static final String BASEPORT = "Port";
    public static final String VPN_AF_TARGET = "VpnAfConfig";
    public static final String BGP_PEERING = "BGPPeering";
    public static final String DATA_PLANE_TUNNEL = "DataplaneTunnel";
    public static final String VPN_PORT_STORE = "evpn-port-store";
    public static final String BASE_PORT_STORE = "evpn-baseport-store";
    public static final String VPN_INSTANCE_STORE =
            "evpn-instance-store";
    public static final String VPN_PORT_ID_NOT_NULL = "EVPN port ID cannot be" +
            " null";
    public static final String VPN_PORT_NOT_NULL = "EVPN port cannot be null";
    public static final String RESPONSE_NOT_NULL = "JsonNode can not be null";
    public static final String LISTENER_NOT_NULL = "Listener cannot be null";
    public static final String EVENT_NOT_NULL = "Event cannot be null";
    public static final String DELETE = "delete";
    public static final String SET = "set";
    public static final String UPDATE = "update";
    public static final String VPN_PORT_ID = "EVPN port ID is  {} ";
    public static final String VPN_PORT_CREATION_FAILED = "The EVPN port " +
            "creation is failed whose identifier is {} ";
    public static final String VPN_PORT_IS_NOT_EXIST = "The EVPN port is not " +
            "exist whose identifier is {}";
    public static final String VPN_PORT_UPDATE_FAILED = "The EVPN port update" +
            " is failed whose identifier is {}";
    public static final String VPN_PORT_DELETE_FAILED =
            "The EVPN port delete is failed whose identifier is {}";
    public static final String INTERFACE_ID = "interface_id";
    public static final String ID = "id";
    public static final String VPN_INSTANCE = "service_id";
    public static final String VPN_INSTANCE_ID_NOT_NULL = "EVPN instance ID " +
            "cannot be null";
    public static final String VPN_INSTANCE_NOT_NULL = "EVPN instance cannot " +
            "be null";
    public static final String JSON_NOT_NULL = "JsonNode can not be null";
    public static final String INSTANCE_ID = "EVPN instance ID is  {} ";
    public static final String VPN_INSTANCE_CREATION_FAILED = "The " +
            "EVPN instance creation is failed whose identifier is {} ";
    public static final String VPN_INSTANCE_IS_NOT_EXIST = "The EVPN instance" +
            " is not exist whose identifier is {}";
    public static final String VPN_INSTANCE_UPDATE_FAILED = "The EVPN " +
            "instance update is failed whose identifier is {}";
    public static final String VPN_INSTANCE_DELETE_FAILED = "The EVPN " +
            "instance delete is failed whose identifier is {}";
    public static final String VPN_INSTANCE_NAME = "name";
    public static final String DESCRIPTION = "description";
    public static final String ROUTE_DISTINGUISHERS = "route_distinguishers";
    public static final String IPV4_FAMILY = "ipv4_family";
    static final String ID_CANNOT_BE_NULL = "ID cannot be null";
    static final String INSTANCE_NAME_CANNOT_BE_NULL = "Instance name cannot " +
            "be null";
    static final String DESCRIPTION_CANNOT_BE_NULL = "Description cannot be " +
            "null";
    static final String RD_CANNOT_BE_NULL = "RouteDistinguisher cannot be null";
    static final String RT_CANNOT_BE_NULL = "RouteTarget cannot be null";
    static final String VPNINSTANCE_NAME = "vpnInstanceName";
    static final String ROUTE_DISTINGUISHER = "routeDistinguisher";
    static final String VPN_INSTANCE_ID_CANNOT_BE_NULL = "EVPN instance ID " +
            "cannot be null";
    static final String VPN_INSTANCE_ID = "vpnInstanceId";
    public static final String FORMAT_VPN_INSTANCE = "Id=%s, description=%s,"
            + " name=%s, routeDistinguisher=%s, routeTarget=%s";
    public static final String FORMAT_VPN_PORT = "   EVPN port id=%-32s, " +
            "EVPN instance id=%-18s";
    public static final String FORMAT_PRIVATE_ROUTE = "   %-18s %-15s %-10s";
    public static final String FORMAT_PUBLIC_ROUTE = "   %-18s %-18s %-10s";
    public static final String SWITCH_CHANNEL_ID = "channelId";
    public static final String NOT_MASTER_FOR_SPECIFIC_DEVICE = "The local " +
            "controller is not master for the specified deviceId";
    public static final String VPN_AF_CONFIG_STORE =
            "evpn-vpn-af-config-store";
    public static final String EVPN_VPN_AF_CONFIG_START = "EVPN af config" +
            " started";
    public static final String EVPN_VPN_AF_CONFIG_STOP = "EVPN af config" +
            " stopped";
    static final String RT_TYPE_CANNOT_BE_NULL = "Route target type " +
            "cannot be null";
    public static final String VPN_AF_CONFIG_NOT_NULL = "EVPN af config be " +
            "null";
    public static final String ROUTE_TARGET_VALUE = "Route target value is {} ";
    public static final String VPN_AF_CONFIG_CREATION_FAILED = "The " +
            "EVPN af config creation is failed whose route target is {} ";
    public static final String VPN_AF_CONFIG_UPDATE_FAILED = "The EVPN af " +
            "config update is failed whose identifier is {}";
    public static final String VPN_AF_CONFIG_IS_NOT_EXIST = "The EVPN AF " +
            "config is not exist whose identifier is {}";
    public static final String ROUTE_TARGET_CANNOT_NOT_NULL = "Route target " +
            "value cannot be null";
    public static final String ROUTE_TARGET_DELETE_FAILED = "The route target" +
            " delete is failed whose route target value is {}";
    static final String EXPORT_RT_CANNOT_BE_NULL = "export route " +
            "target set cannot be null";
    static final String IMPORT_RT_CANNOT_BE_NULL = "import route " +
            "target set cannot be null";
    static final String CONFIG_RT_CANNOT_BE_NULL = "import route " +
            "target set cannot be null";
    public static final String EXPORT_EXTCOMMUNITY = "export_extcommunity";
    public static final String IMPORT_EXTCOMMUNITY = "import_extcommunity";
    public static final String BOTH = "both";
    public static final String INVALID_ROUTE_TARGET_TYPE
            = "Invalid route target type has received";
    public static final String INVALID_EVENT_RECEIVED
            = "Invalid event is received while processing network " +
            "configuration event";
    public static final String NETWORK_CONFIG_EVENT_IS_RECEIVED
            = "Event is received from network configuration {}";
    public static final int ARP_PRIORITY = 0xffff;
    public static final short ARP_RESPONSE = 0x2;
    public static final String INVALID_TARGET_RECEIVED
            = "Invalid target type has received";
    public static final String INVALID_ACTION_VPN_AF_CONFIG
            = "Invalid action is received while processing VPN af" +
            " configuration";
    public static final String EXPORT_ROUTE_POLICY = "export_route_policy";
    public static final String IMPORT_ROUTE_POLICY = "import_route_policy";
    public static final String VRF_RT_TYPE = "vrf_rt_type";
    public static final String VRF_RT_VALUE = "vrf_rt_value";
    public static final String BGP_EVPN_ROUTE_UPDATE_START
            = "bgp evpn route update start {}";
    public static final String MPLS_OUT_FLOWS = "mpls out flows --> {}";
    public static final String BGP_EVPN_ROUTE_DELETE_START
            = "bgp route delete start {}";
    public static final String ROUTE_ADD_ARP_RULES = "Route ARP Rules-->ADD";
    public static final String ROUTE_REMOVE_ARP_RULES
            = "Route ARP Rules-->REMOVE";
    public static final String TUNNEL_DST = "tunnelDst";
    public static final String FAILED_TO_SET_TUNNEL_DST
            = "Failed to get extension instruction to set tunnel dst {}";
    public static final String VXLAN = "vxlan";
    public static final String CANNOT_FIND_TUNNEL_PORT_DEVICE =
            "Can't find tunnel port in device {}";
}
