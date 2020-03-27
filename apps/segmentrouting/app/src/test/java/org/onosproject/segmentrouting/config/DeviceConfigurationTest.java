/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.segmentrouting.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.config.basics.InterfaceConfig;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.neighbour.NeighbourResolutionService;
import org.onosproject.segmentrouting.SegmentRoutingManager;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

public class DeviceConfigurationTest {
    private static final DeviceId DEV1 = DeviceId.deviceId("of:1");
    private static final String CONFIG_KEY = "segmentrouting";
    private static final PortNumber PORT1 = PortNumber.portNumber(1);
    private static final PortNumber PORT2 = PortNumber.portNumber(2);
    private static final ConnectPoint CP1 = new ConnectPoint(DEV1, PORT1);
    private static final ConnectPoint CP2 = new ConnectPoint(DEV1, PORT2);
    private static final MacAddress MAC1 = MacAddress.valueOf("00:11:22:33:44:55");
    private static final VlanId VLAN1 = VlanId.vlanId((short) 10);
    private static final VlanId VLAN2 = VlanId.vlanId((short) 20);
    private static final IpPrefix PREFIX1 = IpPrefix.valueOf("10.0.1.254/24");
    private static final IpPrefix PREFIX2 = IpPrefix.valueOf("10.0.2.254/24");
    private static final IpPrefix ROUTE1 = IpPrefix.valueOf("20.0.1.254/24");
    private static final InterfaceIpAddress INTF1_IP = new InterfaceIpAddress(PREFIX1.address(), PREFIX1);
    private static final InterfaceIpAddress INTF2_IP = new InterfaceIpAddress(PREFIX2.address(), PREFIX2);
    private static final List<InterfaceIpAddress> IP_LIST1 = Lists.newArrayList(INTF1_IP);
    private static final List<InterfaceIpAddress> IP_LIST2 = Lists.newArrayList(INTF2_IP);
    private static final Interface INTF1 = new Interface("mock-intf1", CP1, IP_LIST1, MAC1,
            null, VLAN1, null, null);
    private static final Interface INTF2 = new Interface("mock-intf2", CP2, IP_LIST2, MAC1,
            null, VLAN2, null, null);
    private static final Set<Interface> INTERFACES = Sets.newHashSet(INTF1, INTF2);

    private DeviceConfiguration devConfig;

    private NetworkConfigRegistry networkConfigService;

    @Before
    public void setUp() throws Exception {
        InterfaceService interfaceService;
        networkConfigService = null;
        NeighbourResolutionService neighbourResolutionService;
        SegmentRoutingManager srManager;

        // Mock device netcfg
        InputStream jsonStream = SegmentRoutingDeviceConfigTest.class.getResourceAsStream("/device.json");
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(jsonStream);
        SegmentRoutingDeviceConfig srDevConfig = new SegmentRoutingDeviceConfig();
        srDevConfig.init(DEV1, CONFIG_KEY, jsonNode, mapper, config -> { });
        BasicDeviceConfig basicDeviceConfig = new BasicDeviceConfig();
        basicDeviceConfig.init(DEV1, DEV1.toString(), JsonNodeFactory.instance.objectNode(), mapper, config -> { });
        BasicDeviceConfig purgeOnDisconnectConfig = basicDeviceConfig.purgeOnDisconnection(true);


        // Mock interface netcfg
        jsonStream = InterfaceConfig.class.getResourceAsStream("/interface1.json");
        jsonNode = mapper.readTree(jsonStream);
        InterfaceConfig interfaceConfig1 = new InterfaceConfig();
        interfaceConfig1.init(CP1, CONFIG_KEY, jsonNode, mapper, config -> { });
        jsonStream = InterfaceConfig.class.getResourceAsStream("/interface2.json");
        jsonNode = mapper.readTree(jsonStream);
        InterfaceConfig interfaceConfig2 = new InterfaceConfig();
        interfaceConfig2.init(CP1, CONFIG_KEY, jsonNode, mapper, config -> { });

        networkConfigService = createMock(NetworkConfigRegistry.class);
        expect(networkConfigService.getSubjects(DeviceId.class, SegmentRoutingDeviceConfig.class))
                .andReturn(Sets.newHashSet(DEV1)).anyTimes();
        expect(networkConfigService.getConfig(DEV1, SegmentRoutingDeviceConfig.class))
                .andReturn(srDevConfig).anyTimes();
        expect(networkConfigService.addConfig(DEV1, BasicDeviceConfig.class))
                .andReturn(basicDeviceConfig).anyTimes();
        expect(networkConfigService.getConfig(DEV1, BasicDeviceConfig.class))
                .andReturn(basicDeviceConfig).anyTimes();
        expect(networkConfigService.applyConfig(DEV1, BasicDeviceConfig.class, purgeOnDisconnectConfig.node()))
                .andReturn(purgeOnDisconnectConfig).anyTimes();
        expect(networkConfigService.getSubjects(ConnectPoint.class, InterfaceConfig.class))
                .andReturn(Sets.newHashSet(CP1, CP2)).anyTimes();
        expect(networkConfigService.getConfig(CP1, InterfaceConfig.class)).andReturn(interfaceConfig1).anyTimes();
        expect(networkConfigService.getConfig(CP2, InterfaceConfig.class)).andReturn(interfaceConfig2).anyTimes();
        expect(networkConfigService.applyConfig(eq(CP1), eq(InterfaceConfig.class), anyObject()))
                .andReturn(interfaceConfig1).anyTimes();
        expect(networkConfigService.applyConfig(eq(CP2), eq(InterfaceConfig.class), anyObject()))
                .andReturn(interfaceConfig2).anyTimes();
        expect(networkConfigService.getConfig(null, SegmentRoutingAppConfig.class)).andReturn(null).anyTimes();
        replay(networkConfigService);

        interfaceService = createMock(InterfaceService.class);
        expect(interfaceService.getInterfaces()).andReturn(INTERFACES).anyTimes();
        expect(interfaceService.getInterfacesByPort(CP1)).andReturn(Sets.newHashSet(INTF1)).anyTimes();
        expect(interfaceService.getInterfacesByPort(CP2)).andReturn(Sets.newHashSet(INTF2)).anyTimes();
        replay(interfaceService);

        neighbourResolutionService = createMock(NeighbourResolutionService.class);
        neighbourResolutionService.registerNeighbourHandler(anyObject(ConnectPoint.class), anyObject(), anyObject());
        expectLastCall().anyTimes();
        replay(neighbourResolutionService);

        srManager = new SegmentRoutingManager();
        srManager.interfaceService = interfaceService;
        srManager.cfgService = networkConfigService;
        srManager.neighbourResolutionService = neighbourResolutionService;

        devConfig = new DeviceConfiguration(srManager);
        devConfig.addSubnet(CP2, ROUTE1);
    }

    @Test
    public void getConfiguredSubnets() {
        Set<IpPrefix> expect = Sets.newHashSet(PREFIX1, PREFIX2);
        assertEquals(expect, devConfig.getConfiguredSubnets(DEV1));
    }

    @Test
    public void getSubnets() {
        Set<IpPrefix> expect = Sets.newHashSet(PREFIX1, PREFIX2, ROUTE1);
        assertEquals(expect, devConfig.getSubnets(DEV1));
    }

    @Test
    public void getPortSubnets() {
        assertEquals(Sets.newHashSet(PREFIX1), devConfig.getPortSubnets(DEV1, PORT1));
        assertEquals(Sets.newHashSet(PREFIX2), devConfig.getPortSubnets(DEV1, PORT2));
    }

    @Test
    public void inSameSubnet() {
        assertTrue(devConfig.inSameSubnet(DEV1, PREFIX1.address()));
        assertTrue(devConfig.inSameSubnet(DEV1, PREFIX2.address()));
        assertFalse(devConfig.inSameSubnet(DEV1, ROUTE1.address()));
    }

    @Test
    public void getPurgeOnDisconnect() {
        assertNotNull(networkConfigService.getConfig(DEV1, BasicDeviceConfig.class));
        assertTrue(networkConfigService.getConfig(DEV1, BasicDeviceConfig.class).purgeOnDisconnection());
    }
}