/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtn.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.onlab.packet.IpAddress;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeDescription;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.behaviour.DefaultBridgeDescription;
import org.onosproject.net.behaviour.DefaultTunnelDescription;
import org.onosproject.net.behaviour.InterfaceConfig;
import org.onosproject.net.behaviour.TunnelDescription;
import org.onosproject.net.behaviour.TunnelEndPoints;
import org.onosproject.net.behaviour.TunnelKeys;
import org.onosproject.net.driver.DriverHandler;

/**
 * Applies configuration to the device.
 */
public final class VtnConfig {

    public static final String DEFAULT_BRIDGE_NAME = "br-int";
    private static final String DEFAULT_TUNNEL = "vxlan-0.0.0.0";
    private static final Map<String, String> DEFAULT_TUNNEL_OPTIONS = new HashMap<String, String>() {
        {
            put("key", "flow");
            put("remote_ip", "flow");
            put("dst_port", "4790");
            put("in_nsi", "flow");
            put("in_nsp", "flow");
            put("out_nsi", "flow");
            put("out_nsp", "flow");
            put("in_nshc1", "flow");
            put("out_nshc1", "flow");
            put("in_nshc2", "flow");
            put("out_nshc2", "flow");
            put("in_nshc3", "flow");
            put("out_nshc3", "flow");
            put("in_nshc4", "flow");
            put("out_nshc4", "flow");
            put("exts", "gpe");
        }
    };
    /**
     * Constructs a vtn config object. Utility classes should not have a
     * public or default constructor, otherwise IDE will compile unsuccessfully. This
     * class should not be instantiated.
     */
    private VtnConfig() {
    }

    /**
     * Creates or update bridge in the controller device.
     *
     * @param handler DriverHandler
     * @param dpid datapath id
     * @param exPortName external port name
     */
    public static void applyBridgeConfig(DriverHandler handler, String dpid, String exPortName) {
        BridgeConfig bridgeConfig = handler.behaviour(BridgeConfig.class);
        BridgeDescription bridgeDesc = DefaultBridgeDescription.builder()
                .name(DEFAULT_BRIDGE_NAME)
                .failMode(BridgeDescription.FailMode.SECURE)
                .datapathId(dpid)
                .disableInBand()
                .enableLocalController()
                .build();

        bridgeConfig.addBridge(bridgeDesc);
        bridgeConfig.addPort(BridgeName.bridgeName(DEFAULT_BRIDGE_NAME), exPortName);
    }

    /**
     * Creates or update tunnel in the controller device.
     *
     * @param handler DriverHandler
     * @param srcIp the ipAddress of the local controller device
     */
    public static void applyTunnelConfig(DriverHandler handler, IpAddress srcIp) {
        DefaultAnnotations.Builder optionBuilder = DefaultAnnotations.builder();
        for (String key : DEFAULT_TUNNEL_OPTIONS.keySet()) {
            optionBuilder.set(key, DEFAULT_TUNNEL_OPTIONS.get(key));
        }

        InterfaceConfig interfaceConfig = handler.behaviour(InterfaceConfig.class);
        TunnelDescription tunnel = DefaultTunnelDescription.builder()
                .deviceId(DEFAULT_BRIDGE_NAME)
                .ifaceName(DEFAULT_TUNNEL)
                .type(TunnelDescription.Type.VXLAN)
                .local(TunnelEndPoints.ipTunnelEndpoint(srcIp))
                .remote(TunnelEndPoints.flowTunnelEndpoint())
                .key(TunnelKeys.flowTunnelKey())
                .otherConfigs(optionBuilder.build())
                .build();
        interfaceConfig.addTunnelMode(DEFAULT_TUNNEL, tunnel);
    }

    /**
     * Creates or update tunnel in the controller device.
     *
     * @param handler DriverHandler
     */
    public static void removeTunnelConfig(DriverHandler handler) {
        InterfaceConfig interfaceConfig = handler.behaviour(InterfaceConfig.class);
        interfaceConfig.removeTunnelMode(DEFAULT_TUNNEL);
    }

    /**
     * Gets ports in the controller device.
     *
     * @param handler DriverHandler
     * @return set of port numbers
     */
    public static Set<PortNumber> getPortNumbers(DriverHandler handler) {
        BridgeConfig bridgeConfig = handler.behaviour(BridgeConfig.class);
        return bridgeConfig.getPortNumbers();
    }

}
