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
 * Unit tests for LispSrcDstAddress extension class.
 */
public class LispSrcDstAddressTest {

    private static final IpPrefix IP_ADDRESS_1 = IpPrefix.valueOf("1.2.3.4/24");
    private static final IpPrefix IP_ADDRESS_2 = IpPrefix.valueOf("5.6.7.8/24");

    private static final byte MASK_LENGTH_1 = 0x01;
    private static final byte MASK_LENGTH_2 = 0x02;

    private LispSrcDstAddress address1;
    private LispSrcDstAddress sameAsAddress1;
    private LispSrcDstAddress address2;

    @Before
    public void setUp() {

        MappingAddress ma1 = MappingAddresses.ipv4MappingAddress(IP_ADDRESS_1);

        address1 = new LispSrcDstAddress.Builder()
                                    .withSrcMaskLength(MASK_LENGTH_1)
                                    .withDstMaskLength(MASK_LENGTH_1)
                                    .withSrcPrefix(ma1)
                                    .withDstPrefix(ma1)
                                    .build();

        sameAsAddress1 = new LispSrcDstAddress.Builder()
                                    .withSrcMaskLength(MASK_LENGTH_1)
                                    .withDstMaskLength(MASK_LENGTH_1)
                                    .withSrcPrefix(ma1)
                                    .withDstPrefix(ma1)
                                    .build();

        MappingAddress ma2 = MappingAddresses.ipv4MappingAddress(IP_ADDRESS_2);

        address2 = new LispSrcDstAddress.Builder()
                                    .withSrcMaskLength(MASK_LENGTH_2)
                                    .withDstMaskLength(MASK_LENGTH_2)
                                    .withSrcPrefix(ma2)
                                    .withDstPrefix(ma2)
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
        LispSrcDstAddress address = address1;

        MappingAddress ma = MappingAddresses.ipv4MappingAddress(IP_ADDRESS_1);

        assertThat(address.getSrcMaskLength(), is(MASK_LENGTH_1));
        assertThat(address.getDstMaskLength(), is(MASK_LENGTH_1));
        assertThat(address.getSrcPrefix(), is(ma));
        assertThat(address.getDstPrefix(), is(ma));
    }

    @Test
    public void testSerialization() {
        LispSrcDstAddress other = new LispSrcDstAddress();
        other.deserialize(address1.serialize());

        new EqualsTester()
                .addEqualityGroup(address1, other)
                .testEquals();
    }
}
