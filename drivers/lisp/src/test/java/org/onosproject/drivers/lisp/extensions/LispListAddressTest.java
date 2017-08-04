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

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpPrefix;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.addresses.MappingAddresses;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for LispListAddress extension class.
 */
public class LispListAddressTest {

    private static final IpPrefix IPV4_ADDRESS_1 = IpPrefix.valueOf("1.2.3.4/24");
    private static final IpPrefix IPV4_ADDRESS_2 = IpPrefix.valueOf("5.6.7.8/24");

    private static final IpPrefix IPV6_ADDRESS_1 = IpPrefix.valueOf("1111:2222:3333:4444:5555:6666:7777:8886/96");
    private static final IpPrefix IPV6_ADDRESS_2 = IpPrefix.valueOf("2222:3333:4444:5555:6666:7777:8888:9999/96");

    private LispListAddress address1;
    private LispListAddress sameAsAddress1;
    private LispListAddress address2;

    @Before
    public void setUp() {

        MappingAddress ipv4Addr1 = MappingAddresses.ipv4MappingAddress(IPV4_ADDRESS_1);
        MappingAddress ipv6Addr1 = MappingAddresses.ipv6MappingAddress(IPV6_ADDRESS_1);

        address1 = new LispListAddress.Builder()
                            .withIpv4(ipv4Addr1)
                            .withIpv6(ipv6Addr1)
                            .build();

        sameAsAddress1 = new LispListAddress.Builder()
                                .withIpv4(ipv4Addr1)
                                .withIpv6(ipv6Addr1)
                                .build();

        MappingAddress ipv4Addr2 = MappingAddresses.ipv4MappingAddress(IPV4_ADDRESS_2);
        MappingAddress ipv6Addr2 = MappingAddresses.ipv6MappingAddress(IPV6_ADDRESS_2);

        address2 = new LispListAddress.Builder()
                            .withIpv4(ipv4Addr2)
                            .withIpv6(ipv6Addr2)
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
        LispListAddress address = address1;

        MappingAddress ipv4 = MappingAddresses.ipv4MappingAddress(IPV4_ADDRESS_1);
        MappingAddress ipv6 = MappingAddresses.ipv6MappingAddress(IPV6_ADDRESS_1);

        assertThat(address.getIpv4(), is(ipv4));
        assertThat(address.getIpv6(), is(ipv6));
    }

    @Test
    public void testSerialization() {
        LispListAddress other = new LispListAddress();
        other.deserialize(address1.serialize());

        new EqualsTester()
                .addEqualityGroup(address1, other)
                .testEquals();
    }
}
