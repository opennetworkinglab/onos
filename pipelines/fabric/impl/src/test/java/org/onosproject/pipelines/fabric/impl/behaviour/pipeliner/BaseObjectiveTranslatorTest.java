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

package org.onosproject.pipelines.fabric.impl.behaviour.pipeliner;

import org.junit.Test;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.pipelines.fabric.impl.behaviour.FabricCapabilities;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

public class BaseObjectiveTranslatorTest {
    static final ApplicationId APP_ID = TestApplicationId.create("FabricPipelinerTest");
    static final ApplicationId XCONNECT_APP_ID = TestApplicationId.create("FabricPipelinerTest.xconnect");
    static final DeviceId DEVICE_ID = DeviceId.deviceId("device:bmv2:11");
    static final int PRIORITY = 100;
    static final PortNumber PORT_1 = PortNumber.portNumber(1);
    static final PortNumber PORT_2 = PortNumber.portNumber(2);
    static final VlanId VLAN_100 = VlanId.vlanId("100");
    static final VlanId VLAN_200 = VlanId.vlanId("200");
    static final MacAddress HOST_MAC = MacAddress.valueOf("00:00:00:00:00:01");
    static final MacAddress ROUTER_MAC = MacAddress.valueOf("00:00:00:00:02:01");
    static final MacAddress SPINE1_MAC = MacAddress.valueOf("00:00:00:00:03:01");
    static final MacAddress SPINE2_MAC = MacAddress.valueOf("00:00:00:00:03:02");
    static final IpPrefix IPV4_UNICAST_ADDR = IpPrefix.valueOf("10.0.0.1/32");
    static final IpPrefix IPV4_MCAST_ADDR = IpPrefix.valueOf("224.0.0.1/32");
    static final IpPrefix IPV6_UNICAST_ADDR = IpPrefix.valueOf("2000::1/32");
    static final IpPrefix IPV6_MCAST_ADDR = IpPrefix.valueOf("ff00::1/32");
    static final MplsLabel MPLS_10 = MplsLabel.mplsLabel(10);
    static final Integer NEXT_ID_1 = 1;
    static final TrafficSelector VLAN_META = DefaultTrafficSelector.builder()
            .matchVlanId(VLAN_100)
            .build();

    FabricCapabilities capabilitiesHashed;
    FabricCapabilities capabilitiesSimple;

    void doSetup() {
        this.capabilitiesHashed = createNiceMock(FabricCapabilities.class);
        this.capabilitiesSimple = createNiceMock(FabricCapabilities.class);
        expect(capabilitiesHashed.hasHashedTable()).andReturn(true).anyTimes();
        expect(capabilitiesHashed.supportDoubleVlanTerm()).andReturn(true).anyTimes();
        expect(capabilitiesSimple.hasHashedTable()).andReturn(false).anyTimes();
        expect(capabilitiesSimple.supportDoubleVlanTerm()).andReturn(true).anyTimes();
        replay(capabilitiesHashed);
        replay(capabilitiesSimple);
    }

    @Test
    public void fakeTest() {
        // Needed otherwise Bazel complains about a test class without test cases.
        assert true;
    }
}
