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

package org.onosproject.drivers.cisco;

import com.google.common.collect.Lists;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onlab.packet.VlanId;
import org.onosproject.net.device.DefaultDeviceInterfaceDescription;
import org.onosproject.net.device.DeviceInterfaceDescription;

import java.util.Arrays;
import java.util.List;

/**
 * Parser for Netconf XML configurations and replies from Cisco devices.
 */
public final class XmlParserCisco {

    private static final String TRUNK_MODE_KEY =
            "ConfigIf-Configuration.switchport.mode.trunk";
    private static final String ACCESS_KEY =
            "ConfigIf-Configuration.switchport.access.vlan.VLANIDVLANPortAccessMode";
    private static final String TRUNK_VLAN_KEY =
            "ConfigIf-Configuration.switchport.trunk.allowed.vlan.VLANIDsAllowedVLANsPortTrunkingMode";
    private static final String RATE_LIMIT_KEY =
            "ConfigIf-Configuration.srr-queue.bandwidth.limit.EnterBandwidthLimitInterfaceAsPercentage";
    private static final short NO_LIMIT = -1;

    private XmlParserCisco() {
        // Not to be called.
    }

    /**
     * Parses device configuration and returns the descriptions of the device
     * interfaces.
     *
     * @param cfg an hierarchical configuration
     * @return list of interface descriptions for the device
     */

    public static List<DeviceInterfaceDescription> getInterfacesFromConfig(
            HierarchicalConfiguration cfg) {
        List<DeviceInterfaceDescription> intfs = Lists.newArrayList();
        List<HierarchicalConfiguration> subtrees =
                cfg.configurationsAt("data.xml-config-data.Device-Configuration.interface");
        for (HierarchicalConfiguration intfConfig :subtrees) {
            String intfName = getInterfaceName(intfConfig);
            DeviceInterfaceDescription.Mode intfMode = getInterfaceMode(intfConfig);
            List<VlanId> intfVlans = getInterfaceVlans(intfConfig, intfMode);
            short intfLimit = getInterfaceLimit(intfConfig);
            boolean intfLimited = (intfLimit == NO_LIMIT ?  false : true);
            DeviceInterfaceDescription intf =
                    new DefaultDeviceInterfaceDescription(intfName,
                                                          intfMode,
                                                          intfVlans,
                                                          intfLimited,
                                                          intfLimit);
            intfs.add(intf);
        }
        return intfs;
    }

    private static String getInterfaceName(HierarchicalConfiguration intfConfig) {
        return intfConfig.getString("Param");
    }

    private static DeviceInterfaceDescription.Mode
    getInterfaceMode(HierarchicalConfiguration intfConfig) {
        if (intfConfig.containsKey(TRUNK_MODE_KEY)) {
            return DeviceInterfaceDescription.Mode.TRUNK;
        } else if (intfConfig.containsKey(ACCESS_KEY)) {
            return DeviceInterfaceDescription.Mode.ACCESS;
        } else {
            return DeviceInterfaceDescription.Mode.NORMAL;
        }
    }

    private static List<VlanId> getInterfaceVlans(
            HierarchicalConfiguration intfConfig,
            DeviceInterfaceDescription.Mode mode) {
        List<VlanId> vlans = Lists.newArrayList();
        if (mode == DeviceInterfaceDescription.Mode.ACCESS) {
            vlans.add(getVlanForAccess(intfConfig));
        } else if (mode == DeviceInterfaceDescription.Mode.TRUNK) {
            vlans.addAll(getVlansForTrunk(intfConfig));
        }
        return vlans;
    }

    private static VlanId getVlanForAccess(HierarchicalConfiguration intfConfig) {
        if (intfConfig.containsKey(ACCESS_KEY)) {
            return VlanId.vlanId(intfConfig.getString(ACCESS_KEY));
        }
        return null;
    }

    private static List<VlanId> getVlansForTrunk(HierarchicalConfiguration intfConfig) {
        if (intfConfig.containsKey(TRUNK_VLAN_KEY)) {
            return parseVlans(intfConfig.getStringArray(TRUNK_VLAN_KEY));
        }
        return null;
    }

    private static List<VlanId> parseVlans(String[] vlansString) {
        List<VlanId> vlans = Lists.newArrayList();
        List<String> items = Arrays.asList(vlansString);
        for (String item: items) {
            int index = item.indexOf("-");
            if (index == -1) {
                // Not a range o values
                vlans.add(VlanId.vlanId(item));
            } else {
                // A range of values separated with "-"
                short lowerVlan = Short.parseShort(item.substring(0, index));
                short higherVlan = Short.parseShort(item.substring(index + 1));
                for (short i = lowerVlan; i <= higherVlan; i++) {
                    vlans.add(VlanId.vlanId(i));
                }
            }
        }
        return vlans;
    }

    private static short getInterfaceLimit(HierarchicalConfiguration intfConfig) {
        if (intfConfig.containsKey(RATE_LIMIT_KEY)) {
            return intfConfig.getShort(RATE_LIMIT_KEY);
        }
        return NO_LIMIT;
    }
}
