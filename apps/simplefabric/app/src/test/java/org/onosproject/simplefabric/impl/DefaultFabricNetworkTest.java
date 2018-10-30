/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.simplefabric.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.intf.Interface;
import org.onosproject.simplefabric.api.FabricNetwork;

import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.assertEquals;

/**
 * Unit tests for the default fabric network class.
 */
public final class DefaultFabricNetworkTest {

    private static final String NAME_1 = "network1";
    private static final String NAME_2 = "network2";

    private static final String INTF_NAMES_1_1 = "h11";
    private static final String INTF_NAMES_1_2 = "h12";
    private static final String INTF_NAMES_2_1 = "h21";
    private static final String INTF_NAMES_2_2 = "h22";

    private static final Set<String> INTF_NAME_SET_1 =
                            ImmutableSet.of(INTF_NAMES_1_1, INTF_NAMES_1_2);
    private static final Set<String> INTF_NAME_SET_2 =
                            ImmutableSet.of(INTF_NAMES_2_1, INTF_NAMES_2_2);

    private static final EncapsulationType ENCAP_TYPE_1 = EncapsulationType.NONE;
    private static final EncapsulationType ENCAP_TYPE_2 = EncapsulationType.NONE;

    private static final boolean IS_FORWARD_1 = false;
    private static final boolean IS_FORWARD_2 = true;
    private static final boolean IS_BROADCAST_1 = false;
    private static final boolean IS_BROADCAST_2 = true;

    private FabricNetwork fabricNetwork1;
    private FabricNetwork sameAsFabricNetwork1;
    private FabricNetwork fabricNetwork2;

    private static Interface createInterface(int index) {

        String name = "INTF_NAME_" + index;
        ConnectPoint cp = ConnectPoint.fromString("of:0011223344556677/" + index);
        InterfaceIpAddress intfIp1 = InterfaceIpAddress.valueOf("10.10.10." + index + "/32");
        InterfaceIpAddress intfIp2 = InterfaceIpAddress.valueOf("20.20.20." + index + "/32");
        List<InterfaceIpAddress> intfIps = ImmutableList.of(intfIp1, intfIp2);
        MacAddress mac = MacAddress.valueOf("00:00:00:00:00:00");
        VlanId vlanId = VlanId.NONE;

        return new Interface(name, cp, intfIps, mac, vlanId);
    }

    /**
     * Initial setup for this unit test.
     */
    @Before
    public void setUp() {

        fabricNetwork1 = DefaultFabricNetwork.builder()
                                .name(NAME_1)
                                .interfaceNames(INTF_NAME_SET_1)
                                .encapsulation(ENCAP_TYPE_1)
                                .forward(IS_FORWARD_1)
                                .broadcast(IS_BROADCAST_1)
                                .build();

        sameAsFabricNetwork1 = DefaultFabricNetwork.builder()
                                .name(NAME_1)
                                .interfaceNames(INTF_NAME_SET_1)
                                .encapsulation(ENCAP_TYPE_1)
                                .forward(IS_FORWARD_1)
                                .broadcast(IS_BROADCAST_1)
                                .build();

        fabricNetwork2 = DefaultFabricNetwork.builder()
                                .name(NAME_2)
                                .interfaceNames(INTF_NAME_SET_2)
                                .encapsulation(ENCAP_TYPE_2)
                                .forward(IS_FORWARD_2)
                                .broadcast(IS_BROADCAST_2)
                                .build();
    }

    /**
     * Tests object equality.
     */
    @Test
    public void testEquality() {
        new EqualsTester().addEqualityGroup(fabricNetwork1, sameAsFabricNetwork1)
                .addEqualityGroup(fabricNetwork2)
                .testEquals();
    }

    /**
     * Test object construction.
     */
    @Test
    public void testConstruction() {
        FabricNetwork network = fabricNetwork1;

        assertEquals(network.name(), NAME_1);
        assertEquals(network.interfaceNames(), INTF_NAME_SET_1);
        assertEquals(network.encapsulation(), ENCAP_TYPE_1);
        assertEquals(network.isForward(), IS_FORWARD_1);
        assertEquals(network.isBroadcast(), IS_BROADCAST_1);
    }
}
