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
package org.onosproject.drivers.lisp.extensions;

import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpPrefix;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.addresses.MappingAddresses;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for LispNatAddress extension class.
 */
public class LispNatAddressTest {

    private static final IpPrefix IP_ADDRESS_1 = IpPrefix.valueOf("1.2.3.4/24");
    private static final IpPrefix IP_ADDRESS_2 = IpPrefix.valueOf("5.6.7.8/24");

    private static final IpPrefix RTR_ADDRESS_11 = IpPrefix.valueOf("10.1.1.1/24");
    private static final IpPrefix RTR_ADDRESS_12 = IpPrefix.valueOf("10.1.1.2/24");

    private static final IpPrefix RTR_ADDRESS_21 = IpPrefix.valueOf("10.1.2.1/24");
    private static final IpPrefix RTR_ADDRESS_22 = IpPrefix.valueOf("10.1.2.2/24");

    private static final short MS_UDP_PORT_NUMBER = 80;
    private static final short ETR_UDP_PORT_NUMBER = 100;

    private LispNatAddress address1;
    private LispNatAddress sameAsAddress1;
    private LispNatAddress address2;

    @Before
    public void setUp() {

        MappingAddress ma1 = MappingAddresses.ipv4MappingAddress(IP_ADDRESS_1);

        MappingAddress rtr11 = MappingAddresses.ipv4MappingAddress(RTR_ADDRESS_11);
        MappingAddress rtr12 = MappingAddresses.ipv4MappingAddress(RTR_ADDRESS_12);

        List<MappingAddress> rtrRlocs1 = ImmutableList.of(rtr11, rtr12);

        address1 = new LispNatAddress.Builder()
                                        .withMsUdpPortNumber(MS_UDP_PORT_NUMBER)
                                        .withEtrUdpPortNumber(ETR_UDP_PORT_NUMBER)
                                        .withGlobalEtrRlocAddress(ma1)
                                        .withMsRlocAddress(ma1)
                                        .withPrivateEtrRlocAddress(ma1)
                                        .withRtrRlocAddresses(rtrRlocs1)
                                        .build();

        sameAsAddress1 = new LispNatAddress.Builder()
                                        .withMsUdpPortNumber(MS_UDP_PORT_NUMBER)
                                        .withEtrUdpPortNumber(ETR_UDP_PORT_NUMBER)
                                        .withGlobalEtrRlocAddress(ma1)
                                        .withMsRlocAddress(ma1)
                                        .withPrivateEtrRlocAddress(ma1)
                                        .withRtrRlocAddresses(rtrRlocs1)
                                        .build();

        MappingAddress ma2 = MappingAddresses.ipv4MappingAddress(IP_ADDRESS_2);

        MappingAddress rtr21 = MappingAddresses.ipv4MappingAddress(RTR_ADDRESS_21);
        MappingAddress rtr22 = MappingAddresses.ipv4MappingAddress(RTR_ADDRESS_22);

        List<MappingAddress> rtrRlocs2 = ImmutableList.of(rtr21, rtr22);


        address2 = new LispNatAddress.Builder()
                                        .withMsUdpPortNumber(MS_UDP_PORT_NUMBER)
                                        .withEtrUdpPortNumber(ETR_UDP_PORT_NUMBER)
                                        .withGlobalEtrRlocAddress(ma2)
                                        .withMsRlocAddress(ma2)
                                        .withPrivateEtrRlocAddress(ma2)
                                        .withRtrRlocAddresses(rtrRlocs2)
                                        .build();
    }

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(address1, sameAsAddress1)
                .addEqualityGroup(address2).testEquals();
    }

    @Test
    public void testConstruction() {
        LispNatAddress address = address1;

        MappingAddress ma = MappingAddresses.ipv4MappingAddress(IP_ADDRESS_1);

        assertThat(address.getMsUdpPortNumber(), is(MS_UDP_PORT_NUMBER));
        assertThat(address.getEtrUdpPortNumber(), is(ETR_UDP_PORT_NUMBER));
        assertThat(address.getGlobalEtrRlocAddress(), is(ma));
        assertThat(address.getMsRlocAddress(), is(ma));
        assertThat(address.getPrivateEtrRlocAddress(), is(ma));
    }

    @Test
    public void testSerialization() {
        LispNatAddress other = new LispNatAddress();
        other.deserialize(address1.serialize());

        new EqualsTester()
                .addEqualityGroup(address1, other)
                .testEquals();
    }
}
