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

package org.onosproject.drivers.cisco;

import com.google.common.collect.Lists;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.junit.Test;
import org.onlab.packet.VlanId;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.device.DefaultDeviceInterfaceDescription;
import org.onosproject.net.device.DeviceInterfaceDescription;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests the parser for Netconf XML configurations and replies from Cisco devices.
 */
public class XmlParserCiscoTest {

    private static final String INTF_NAME_1 = "GigabitEthernet0/1";
    private static final String INTF_NAME_2 = "GigabitEthernet0/2";
    private static final String INTF_NAME_3 = "GigabitEthernet0/3";
    private static final String INTF_NAME_4 = "GigabitEthernet0/4";
    private static final String INTF_NAME_5 = "GigabitEthernet0/5";
    private static final VlanId ACCESS_VLAN = VlanId.vlanId((short) 100);
    private static final VlanId TRUNK_VLAN_1 = VlanId.vlanId((short) 200);
    private static final VlanId TRUNK_VLAN_2 = VlanId.vlanId((short) 201);
    private static final VlanId TRUNK_VLAN_3 = VlanId.vlanId((short) 300);
    private static final VlanId TRUNK_VLAN_4 = VlanId.vlanId((short) 301);
    private static final VlanId TRUNK_VLAN_5 = VlanId.vlanId((short) 302);
    private static final short NO_RATE_LIMIT = -1;
    private static final short RATE_LIMIT_1 = 75;
    private static final short RATE_LIMIT_2 = 50;
    private static final boolean NO_LIMIT = false;
    private static final boolean WITH_LIMIT = true;
    private static final String CONFIG_XML_FILE = "/testGetConfig.xml";

    @Test
    public void controllersConfig() {
        InputStream streamOrig = getClass().getResourceAsStream(CONFIG_XML_FILE);
        HierarchicalConfiguration cfgOrig = XmlConfigParser.loadXml(streamOrig);
        List<DeviceInterfaceDescription> actualIntfs =
                XmlParserCisco.getInterfacesFromConfig(cfgOrig);
        assertEquals("Interfaces were not retrieved from configuration",
                     getExpectedIntfs(), actualIntfs);
    }

    private List<DeviceInterfaceDescription> getExpectedIntfs() {
        List<DeviceInterfaceDescription> intfs = new ArrayList<>();
        intfs.add(new DefaultDeviceInterfaceDescription(INTF_NAME_1,
                                                        DeviceInterfaceDescription.Mode.NORMAL,
                                                        Lists.newArrayList(),
                                                        NO_LIMIT,
                                                        NO_RATE_LIMIT));

        List<VlanId> accessList = new ArrayList<>();
        accessList.add(ACCESS_VLAN);
        intfs.add(new DefaultDeviceInterfaceDescription(INTF_NAME_2,
                                                        DeviceInterfaceDescription.Mode.ACCESS,
                                                        accessList,
                                                        NO_LIMIT,
                                                        NO_RATE_LIMIT));

        List<VlanId> trunkList1 = new ArrayList<>();
        trunkList1.add(TRUNK_VLAN_1);
        trunkList1.add(TRUNK_VLAN_2);
        intfs.add(new DefaultDeviceInterfaceDescription(INTF_NAME_3,
                                                        DeviceInterfaceDescription.Mode.TRUNK,
                                                        trunkList1,
                                                        NO_LIMIT,
                                                        NO_RATE_LIMIT));

        intfs.add(new DefaultDeviceInterfaceDescription(INTF_NAME_4,
                                                        DeviceInterfaceDescription.Mode.NORMAL,
                                                        Lists.newArrayList(),
                                                        WITH_LIMIT,
                                                        RATE_LIMIT_1));

        List<VlanId> trunkList2 = new ArrayList<>();
        trunkList2.add(TRUNK_VLAN_3);
        trunkList2.add(TRUNK_VLAN_4);
        trunkList2.add(TRUNK_VLAN_5);
        intfs.add(new DefaultDeviceInterfaceDescription(INTF_NAME_5,
                                                        DeviceInterfaceDescription.Mode.TRUNK,
                                                        trunkList2,
                                                        WITH_LIMIT,
                                                        RATE_LIMIT_2));
        return intfs;
    }
}