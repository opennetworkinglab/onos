/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.host;

import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.NetTestTools;

import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for port addresses class.
 */
public class PortAddressesTest {

    PortAddresses addresses1;
    PortAddresses sameAsAddresses1;
    PortAddresses addresses2;
    PortAddresses addresses3;

    private static final ConnectPoint CONNECT_POINT1 =
            NetTestTools.connectPoint("cp1", 1);
    private static final IpAddress IP_ADDRESS1 = IpAddress.valueOf("1.2.3.4");
    private static final IpPrefix SUBNET_ADDRESS1 =
            IpPrefix.valueOf("1.2.0.0/16");
    private static final InterfaceIpAddress INTERFACE_ADDRESS_1 =
            new InterfaceIpAddress(IP_ADDRESS1, SUBNET_ADDRESS1);

    private static final ConnectPoint CONNECT_POINT2 =
            NetTestTools.connectPoint("cp2", 1);
    private static final IpAddress IP_ADDRESS2 = IpAddress.valueOf("1.2.3.5");
    private static final IpPrefix SUBNET_ADDRESS2 =
            IpPrefix.valueOf("1.3.0.0/16");
    private static final InterfaceIpAddress INTERFACE_ADDRESS_2 =
            new InterfaceIpAddress(IP_ADDRESS2, SUBNET_ADDRESS2);

    Set<InterfaceIpAddress> ipAddresses;


    /**
     * Initializes local data used by all test cases.
     */
    @Before
    public void setUpAddresses() {
        ipAddresses = ImmutableSet.of(INTERFACE_ADDRESS_1,
                INTERFACE_ADDRESS_2);
        addresses1 = new PortAddresses(CONNECT_POINT1, ipAddresses,
                MacAddress.BROADCAST, VlanId.NONE);
        sameAsAddresses1 = new PortAddresses(CONNECT_POINT1, ipAddresses,
                MacAddress.BROADCAST, VlanId.NONE);
        addresses2 = new PortAddresses(CONNECT_POINT2, ipAddresses,
                MacAddress.BROADCAST, VlanId.NONE);
        addresses3 = new PortAddresses(CONNECT_POINT2, ipAddresses,
                MacAddress.ZERO, VlanId.NONE);
    }

    /**
     * Checks that the PortAddresses class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(PortAddresses.class);
    }

    /**
     * Checks the operation of the equals(), hash() and toString()
     * methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(addresses1, sameAsAddresses1)
                .addEqualityGroup(addresses2)
                .addEqualityGroup(addresses3)
                .testEquals();
    }

    /**
     * Tests that object are created correctly.
     */
    @Test
    public void testConstruction() {
        assertThat(addresses1.mac(), is(MacAddress.BROADCAST));
        assertThat(addresses1.connectPoint(), is(CONNECT_POINT1));
        assertThat(addresses1.ipAddresses(), is(ipAddresses));
        assertThat(addresses1.vlan(), is(VlanId.NONE));
    }
}
