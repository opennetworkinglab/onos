/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.controller.impl;

import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.bgp.controller.impl.BgpFlowSpecRib;
import org.onosproject.bgpio.protocol.flowspec.BgpFlowSpecDetails;
import org.onosproject.bgpio.protocol.flowspec.BgpFlowSpecPrefix;
import org.onosproject.bgpio.types.BgpFsOperatorValue;
import org.onosproject.bgpio.types.BgpFsPortNum;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.RouteDistinguisher;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Test cases for BGP Selection Algorithm.
 */
public class BgpFlowSpecRibTest {

    /**
     * Add flow specification to rib.
     */
    @Test
    public void bgpFlowSpecRibTests1() {
        List<BgpValueType> flowSpecComponents = new LinkedList<>();
        List<BgpFsOperatorValue> operatorValue = new ArrayList<>();
        BgpFlowSpecRib rib = new BgpFlowSpecRib();

        IpPrefix destinationPrefix = IpPrefix.valueOf(IpAddress.valueOf("10.0.1.1"), 32);
        IpPrefix sourcePrefix = IpPrefix.valueOf(IpAddress.valueOf("10.0.1.2"), 32);

        BgpFlowSpecPrefix prefix  = new BgpFlowSpecPrefix(destinationPrefix, sourcePrefix);

        byte[] port = new byte[] {(byte) 0x1 };
        operatorValue.add(new BgpFsOperatorValue((byte) 0x81, port));

        BgpFsPortNum portNum = new BgpFsPortNum(operatorValue);
        flowSpecComponents.add(portNum);

        BgpFlowSpecDetails flowSpec = new BgpFlowSpecDetails(flowSpecComponents);

        rib.add(prefix, flowSpec);

        boolean isPresent = rib.flowSpecTree().containsKey(prefix);
        assertThat(isPresent, is(true));
    }

    /**
     * Add and delete flow specification to rib.
     */
    @Test
    public void bgpFlowSpecRibTest2() {
        List<BgpValueType> flowSpecComponents = new LinkedList<>();
        List<BgpFsOperatorValue> operatorValue = new ArrayList<>();
        BgpFlowSpecRib rib = new BgpFlowSpecRib();

        IpPrefix destinationPrefix = IpPrefix.valueOf(IpAddress.valueOf("10.0.1.1"), 32);
        IpPrefix sourcePrefix = IpPrefix.valueOf(IpAddress.valueOf("10.0.1.2"), 32);

        BgpFlowSpecPrefix prefix  = new BgpFlowSpecPrefix(destinationPrefix, sourcePrefix);
        byte[] port = new byte[] {(byte) 0x1 };
        operatorValue.add(new BgpFsOperatorValue((byte) 0x81, port));

        BgpFsPortNum portNum = new BgpFsPortNum(operatorValue);
        flowSpecComponents.add(portNum);

        BgpFlowSpecDetails flowSpec = new BgpFlowSpecDetails(flowSpecComponents);

        rib.add(prefix, flowSpec);

        boolean isPresent = rib.flowSpecTree().containsKey(prefix);
        assertThat(isPresent, is(true));

        rib.delete(prefix);
        isPresent = rib.flowSpecTree().containsKey(prefix);
        assertThat(isPresent, is(false));

    }

    /**
     * Add and delete flow specification with a specific VPN to rib.
     */
    @Test
    public void bgpFlowSpecRibTest3() {
        List<BgpValueType> flowSpecComponents = new LinkedList<>();
        List<BgpFsOperatorValue> operatorValue = new ArrayList<>();
        RouteDistinguisher routeDistinguisher = new RouteDistinguisher(1);
        BgpFlowSpecRib rib = new BgpFlowSpecRib();

        IpPrefix destinationPrefix = IpPrefix.valueOf(IpAddress.valueOf("10.0.1.1"), 32);
        IpPrefix sourcePrefix = IpPrefix.valueOf(IpAddress.valueOf("10.0.1.2"), 32);

        BgpFlowSpecPrefix prefix  = new BgpFlowSpecPrefix(destinationPrefix, sourcePrefix);

        byte[] port = new byte[] {(byte) 0x1 };
        operatorValue.add(new BgpFsOperatorValue((byte) 0x81, port));

        BgpFsPortNum portNum = new BgpFsPortNum(operatorValue);
        flowSpecComponents.add(portNum);

        BgpFlowSpecDetails flowSpec = new BgpFlowSpecDetails(flowSpecComponents);

        rib.add(routeDistinguisher, prefix, flowSpec);

        boolean isPresent = rib.vpnFlowSpecTree().containsKey(routeDistinguisher);
        assertThat(isPresent, is(true));

        rib.delete(routeDistinguisher, prefix);
        isPresent = rib.vpnFlowSpecTree().containsKey(routeDistinguisher);
        assertThat(isPresent, is(false));
    }
}
