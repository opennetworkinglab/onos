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

import org.junit.Ignore;
import org.junit.Test;
import org.onlab.packet.Ethernet;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for Forwarding class.
 */
public class ForwardingFunctionTypeTest {
    private static final ApplicationId APP_ID = TestApplicationId.create("ForwardingFunctionTypeTest");
    private static final VlanId VLAN_100 = VlanId.vlanId((short) 100);
    private static final MacAddress MAC_ADDR = MacAddress.valueOf("00:00:00:00:00:01");
    private static final MacAddress MAC_NONE = MacAddress.NONE;
    private static final IpPrefix IPV4_UNICAST_ADDR = IpPrefix.valueOf("10.0.0.1/32");
    private static final IpPrefix IPV4_MCAST_ADDR = IpPrefix.valueOf("224.0.0.1/32");
    private static final IpPrefix IPV6_UNICAST_ADDR = IpPrefix.valueOf("2000::1/32");
    private static final IpPrefix IPV6_MCAST_ADDR = IpPrefix.valueOf("ff00::1/32");
    private static final MplsLabel MPLS_10 = MplsLabel.mplsLabel(10);
    private TrafficSelector selector;

    /**
     * Match Vlan + EthDst.
     */
    @Test
    public void testL2Unicast() {
        selector = DefaultTrafficSelector.builder()
                .matchVlanId(VLAN_100)
                .matchEthDst(MAC_ADDR)
                .build();
        testFft(selector, ForwardingFunctionType.L2_UNICAST);
    }

    @Test
    public void testL2Broadcast() {
        selector = DefaultTrafficSelector.builder()
                .matchVlanId(VLAN_100)
                .build();
        testFft(selector, ForwardingFunctionType.L2_BROADCAST);
    }

    @Test
    public void testL2BroadcastWithMacNone() {
        selector = DefaultTrafficSelector.builder()
                .matchVlanId(VLAN_100)
                .matchEthDst(MAC_NONE)
                .build();
        testFft(selector, ForwardingFunctionType.L2_BROADCAST);
    }

    @Test
    public void testIpv4Unicast() {
        selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(IPV4_UNICAST_ADDR)
                .build();
        testFft(selector, ForwardingFunctionType.IPV4_ROUTING);
    }

    @Test
    @Ignore
    public void testIpv4Multicast() {
        selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV4)
                .matchIPDst(IPV4_MCAST_ADDR)
                .build();
        testFft(selector, ForwardingFunctionType.IPV4_ROUTING);
    }

    @Test
    @Ignore
    public void testIpv6Unicast() {
        selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV6)
                .matchIPDst(IPV6_UNICAST_ADDR)
                .build();
        testFft(selector, ForwardingFunctionType.IPV6_ROUTING);
    }

    @Test
    @Ignore
    public void testIpv6Multicast() {
        selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.TYPE_IPV6)
                .matchIPDst(IPV6_MCAST_ADDR)
                .build();
        testFft(selector, ForwardingFunctionType.IPV4_ROUTING);
    }

    @Test
    public void testMplsUnicast() {
        selector = DefaultTrafficSelector.builder()
                .matchEthType(Ethernet.MPLS_UNICAST)
                .matchMplsLabel(MPLS_10)
                .matchMplsBos(true)
                .build();
        testFft(selector, ForwardingFunctionType.MPLS_SEGMENT_ROUTING);
    }

    private void testFft(TrafficSelector selector, ForwardingFunctionType expectedFft) {
        ForwardingObjective fwd = DefaultForwardingObjective.builder()
                .withSelector(selector)
                .withFlag(ForwardingObjective.Flag.SPECIFIC)
                .nextStep(0)
                .fromApp(APP_ID)
                .add();
        assertEquals(expectedFft,
                     ForwardingFunctionType.getForwardingFunctionType(fwd));
    }
}
