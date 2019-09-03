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
package org.onosproject.openstacknode.api;

/**
 * Provides constants used in OpenStack node services.
 */
public final class Constants {

    private Constants() {
    }

    public static final String INTEGRATION_BRIDGE = "br-int";
    public static final String TUNNEL_BRIDGE = "br-tun";
    public static final String ROUTER_BRIDGE = "br-router";
    public static final String VXLAN_TUNNEL = "vxlan";
    public static final String GRE_TUNNEL = "gre";
    public static final String GENEVE_TUNNEL = "geneve";
    public static final String PATCH_INTG_BRIDGE = "patch-intg";
    public static final String PATCH_ROUT_BRIDGE = "patch-rout";
    public static final String GATEWAY = "GATEWAY";
    public static final String CONTROLLER = "CONTROLLER";
    public static final String HOST_NAME = "hostname";
    public static final String TYPE = "type";
    public static final String MANAGEMENT_IP = "managementIp";
    public static final String DATA_IP = "dataIp";
    public static final String VLAN_INTF_NAME = "vlanPort";
    public static final String UPLINK_PORT = "uplinkPort";

    public static final String BRIDGE_PREFIX = "br-";
    public static final String INTEGRATION_TO_PHYSICAL_PREFIX = "int-to-";
    public static final String PHYSICAL_TO_INTEGRATION_SUFFIX = "-to-int";

    public static final String FLAT = "flat";
    public static final String VLAN = "vlan";
    public static final String VXLAN = "vxlan";
    public static final String GRE = "gre";
    public static final String GENEVE = "geneve";
}