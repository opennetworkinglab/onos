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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for LispTeAddress extension class.
 */
public class LispTeAddressTest {

    private static final IpPrefix IP_ADDRESS_1 = IpPrefix.valueOf("1.2.3.4/24");
    private static final IpPrefix IP_ADDRESS_2 = IpPrefix.valueOf("5.6.7.8/24");

    private static final boolean IS_LOOKUP_1 = false;
    private static final boolean IS_RLOC_PROBE_1 = true;
    private static final boolean IS_STRICT_1 = false;

    private static final boolean IS_LOOKUP_2 = true;
    private static final boolean IS_RLOC_PROBE_2 = false;
    private static final boolean IS_STRICT_2 = true;

    private LispTeAddress address1;
    private LispTeAddress sameAsAddress1;
    private LispTeAddress address2;

    @Before
    public void setUp() {

        MappingAddress ma1 = MappingAddresses.ipv4MappingAddress(IP_ADDRESS_1);
        MappingAddress ma2 = MappingAddresses.ipv4MappingAddress(IP_ADDRESS_2);

        LispTeAddress.TeRecord tr1 = new LispTeAddress.TeRecord.Builder()
                                                .withIsLookup(IS_LOOKUP_1)
                                                .withIsRlocProbe(IS_RLOC_PROBE_1)
                                                .withIsStrict(IS_STRICT_1)
                                                .withRtrRlocAddress(ma1)
                                                .build();

        LispTeAddress.TeRecord tr2 = new LispTeAddress.TeRecord.Builder()
                                                .withIsLookup(IS_LOOKUP_2)
                                                .withIsRlocProbe(IS_RLOC_PROBE_2)
                                                .withIsStrict(IS_STRICT_2)
                                                .withRtrRlocAddress(ma2)
                                                .build();

        address1 = new LispTeAddress.Builder()
                                        .withTeRecords(ImmutableList.of(tr1, tr2))
                                        .build();

        sameAsAddress1 = new LispTeAddress.Builder()
                                        .withTeRecords(ImmutableList.of(tr1, tr2))
                                        .build();

        address2 = new LispTeAddress.Builder()
                                        .withTeRecords(ImmutableList.of(tr2, tr1))
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
        LispTeAddress address = address1;

        MappingAddress ma = MappingAddresses.ipv4MappingAddress(IP_ADDRESS_1);

        assertThat(address.getTeRecords().get(0).isLookup(), is(IS_LOOKUP_1));
        assertThat(address.getTeRecords().get(0).isRlocProbe(), is(IS_RLOC_PROBE_1));
        assertThat(address.getTeRecords().get(0).isStrict(), is(IS_STRICT_1));
        assertThat(address.getTeRecords().get(0).getAddress(), is(ma));
    }

    @Test
    public void testSerialization() {
        LispTeAddress other = new LispTeAddress();
        other.deserialize(address1.serialize());

        new EqualsTester()
                .addEqualityGroup(address1, other)
                .testEquals();
    }
}
