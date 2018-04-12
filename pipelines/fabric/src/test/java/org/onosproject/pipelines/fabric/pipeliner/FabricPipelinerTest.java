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

package org.onosproject.pipelines.fabric.pipeliner;

import org.junit.Before;
import org.onlab.junit.TestUtils;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.group.GroupService;
import org.onosproject.pipelines.fabric.FabricConstants;
import org.onosproject.pipelines.fabric.FabricInterpreter;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

public abstract class FabricPipelinerTest {
    static final ApplicationId APP_ID = TestApplicationId.create("FabricPipelinerTest");
    static final DeviceId DEVICE_ID = DeviceId.deviceId("device:bmv2:11");
    static final int PRIORITY = 100;
    static final PortNumber PORT_1 = PortNumber.portNumber(1);
    static final PortNumber PORT_2 = PortNumber.portNumber(2);
    static final VlanId VLAN_100 = VlanId.vlanId("100");
    static final MacAddress HOST_MAC = MacAddress.valueOf("00:00:00:00:00:01");
    static final MacAddress ROUTER_MAC = MacAddress.valueOf("00:00:00:00:02:01");
    static final IpPrefix IPV4_UNICAST_ADDR = IpPrefix.valueOf("10.0.0.1/32");
    static final IpPrefix IPV4_MCAST_ADDR = IpPrefix.valueOf("224.0.0.1/32");
    static final IpPrefix IPV6_UNICAST_ADDR = IpPrefix.valueOf("2000::1/32");
    static final IpPrefix IPV6_MCAST_ADDR = IpPrefix.valueOf("ff00::1/32");
    static final MplsLabel MPLS_10 = MplsLabel.mplsLabel(10);
    static final Integer NEXT_ID_1 = 1;
    static final TrafficSelector VLAN_META = DefaultTrafficSelector.builder()
            .matchVlanId(VLAN_100)
            .build();

    // Forwarding types
    static final byte FWD_BRIDGING = 0;
    static final byte FWD_MPLS = 1;
    static final byte FWD_IPV4_UNICAST = 2;
    static final byte FWD_IPV4_MULTICAST = 3;
    static final byte FWD_IPV6_UNICAST = 4;
    static final byte FWD_IPV6_MULTICAST = 5;

    static final PiCriterion VLAN_VALID = PiCriterion.builder()
            .matchExact(FabricConstants.HDR_VLAN_TAG_IS_VALID, new byte[]{1})
            .build();
    static final PiCriterion VLAN_INVALID = PiCriterion.builder()
            .matchExact(FabricConstants.HDR_VLAN_TAG_IS_VALID, new byte[]{0})
            .build();

    FabricPipeliner pipeliner;
    FabricInterpreter interpreter;

    @Before
    public void setup() {
        pipeliner = new FabricPipeliner();

        GroupService mockGroupService = createNiceMock(GroupService.class);
        ServiceDirectory serviceDirectory = createNiceMock(ServiceDirectory.class);
        PipelinerContext pipelinerContext = createNiceMock(PipelinerContext.class);
        DriverHandler driverHandler = createNiceMock(DriverHandler.class);
        Driver mockDriver = createNiceMock(Driver.class);
        expect(mockDriver.getProperty("supportTableCounters")).andReturn("true").anyTimes();
        expect(mockDriver.getProperty("noHashedTable")).andReturn("false").anyTimes();
        expect(driverHandler.driver()).andReturn(mockDriver).anyTimes();
        expect(pipelinerContext.directory()).andReturn(serviceDirectory).anyTimes();
        expect(serviceDirectory.get(GroupService.class)).andReturn(mockGroupService).anyTimes();
        replay(serviceDirectory, pipelinerContext, driverHandler, mockDriver);
        TestUtils.setField(pipeliner, "handler", driverHandler);

        pipeliner.init(DEVICE_ID, pipelinerContext);
        interpreter = new FabricInterpreter();
    }
}
