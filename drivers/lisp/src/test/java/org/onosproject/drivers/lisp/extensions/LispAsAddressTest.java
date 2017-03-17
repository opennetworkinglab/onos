/*
 * Copyright 2017-present Open Networking Laboratory
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
 * Unit tests for LispAsAddress extension class.
 */
public class LispAsAddressTest {

    private static final IpPrefix IP_ADDRESS_1 = IpPrefix.valueOf("1.2.3.4/24");
    private static final IpPrefix IP_ADDRESS_2 = IpPrefix.valueOf("5.6.7.8/24");

    private static final int AS_NUMBER_1 = 1;
    private static final int AS_NUMBER_2 = 2;

    private LispAsAddress address1;
    private LispAsAddress sameAsAddress1;
    private LispAsAddress address2;

    @Before
    public void setUp() {

        MappingAddress ma1 = MappingAddresses.ipv4MappingAddress(IP_ADDRESS_1);

        address1 = new LispAsAddress.Builder()
                                .withAsNumber(AS_NUMBER_1)
                                .withAddress(ma1)
                                .build();

        sameAsAddress1 = new LispAsAddress.Builder()
                                .withAsNumber(AS_NUMBER_1)
                                .withAddress(ma1)
                                .build();

        MappingAddress ma2 = MappingAddresses.ipv4MappingAddress(IP_ADDRESS_2);

        address2 = new LispAsAddress.Builder()
                                .withAsNumber(AS_NUMBER_2)
                                .withAddress(ma2)
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
        LispAsAddress address = address1;

        MappingAddress ma = MappingAddresses.ipv4MappingAddress(IP_ADDRESS_1);

        assertThat(address.getAsNumber(), is(AS_NUMBER_1));
        assertThat(address.getAddress(), is(ma));
    }

    @Test
    public void testSerialization() {
        LispAsAddress other = new LispAsAddress();
        other.deserialize(address1.serialize());

        new EqualsTester()
                .addEqualityGroup(address1, other)
                .testEquals();
    }
}
