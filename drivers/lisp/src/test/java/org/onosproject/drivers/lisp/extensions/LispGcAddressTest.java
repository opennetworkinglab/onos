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
 * Unit tests for LispGcAddress extension class.
 */
public class LispGcAddressTest {

    private static final IpPrefix IP_ADDRESS_1 = IpPrefix.valueOf("1.2.3.4/24");
    private static final IpPrefix IP_ADDRESS_2 = IpPrefix.valueOf("5.6.7.8/24");

    private static final boolean NORTH_VALUE_1 = true;
    private static final boolean EAST_VALUE_1 = false;
    private static final short UNIQUE_SHORT_VALUE_1 = 1;
    private static final byte UNIQUE_BYTE_VALUE_1 = 1;
    private static final int UNIQUE_INT_VALUE_1 = 1;

    private static final boolean NORTH_VALUE_2 = false;
    private static final boolean EAST_VALUE_2 = true;
    private static final short UNIQUE_SHORT_VALUE_2 = 2;
    private static final byte UNIQUE_BYTE_VALUE_2 = 2;
    private static final int UNIQUE_INT_VALUE_2 = 2;

    private LispGcAddress address1;
    private LispGcAddress sameAsAddress1;
    private LispGcAddress address2;

    @Before
    public void setUp() {

        MappingAddress ma1 = MappingAddresses.ipv4MappingAddress(IP_ADDRESS_1);

        address1 = new LispGcAddress.Builder()
                                    .withIsNorth(NORTH_VALUE_1)
                                    .withLatitudeDegree(UNIQUE_SHORT_VALUE_1)
                                    .withLatitudeMinute(UNIQUE_BYTE_VALUE_1)
                                    .withLatitudeSecond(UNIQUE_BYTE_VALUE_1)
                                    .withIsEast(EAST_VALUE_1)
                                    .withLongitudeDegree(UNIQUE_SHORT_VALUE_1)
                                    .withLongitudeMinute(UNIQUE_BYTE_VALUE_1)
                                    .withLongitudeSecond(UNIQUE_BYTE_VALUE_1)
                                    .withAltitude(UNIQUE_INT_VALUE_1)
                                    .withAddress(ma1)
                                    .build();

        sameAsAddress1 = new LispGcAddress.Builder()
                                    .withIsNorth(NORTH_VALUE_1)
                                    .withLatitudeDegree(UNIQUE_SHORT_VALUE_1)
                                    .withLatitudeMinute(UNIQUE_BYTE_VALUE_1)
                                    .withLatitudeSecond(UNIQUE_BYTE_VALUE_1)
                                    .withIsEast(EAST_VALUE_1)
                                    .withLongitudeDegree(UNIQUE_SHORT_VALUE_1)
                                    .withLongitudeMinute(UNIQUE_BYTE_VALUE_1)
                                    .withLongitudeSecond(UNIQUE_BYTE_VALUE_1)
                                    .withAltitude(UNIQUE_INT_VALUE_1)
                                    .withAddress(ma1)
                                    .build();

        MappingAddress ma2 = MappingAddresses.ipv4MappingAddress(IP_ADDRESS_2);

        address2 = new LispGcAddress.Builder()
                                    .withIsNorth(NORTH_VALUE_2)
                                    .withLatitudeDegree(UNIQUE_SHORT_VALUE_2)
                                    .withLatitudeMinute(UNIQUE_BYTE_VALUE_2)
                                    .withLatitudeSecond(UNIQUE_BYTE_VALUE_2)
                                    .withIsEast(EAST_VALUE_2)
                                    .withLongitudeDegree(UNIQUE_SHORT_VALUE_2)
                                    .withLongitudeMinute(UNIQUE_BYTE_VALUE_2)
                                    .withLongitudeSecond(UNIQUE_BYTE_VALUE_2)
                                    .withAltitude(UNIQUE_INT_VALUE_2)
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
        LispGcAddress address = address1;

        MappingAddress ma = MappingAddresses.ipv4MappingAddress(IP_ADDRESS_1);

        assertThat(address.isNorth(), is(NORTH_VALUE_1));
        assertThat(address.getLatitudeDegree(), is(UNIQUE_SHORT_VALUE_1));
        assertThat(address.getLatitudeMinute(), is(UNIQUE_BYTE_VALUE_1));
        assertThat(address.getLatitudeSecond(), is(UNIQUE_BYTE_VALUE_1));
        assertThat(address.isEast(), is(EAST_VALUE_1));
        assertThat(address.getLongitudeDegree(), is(UNIQUE_SHORT_VALUE_1));
        assertThat(address.getLongitudeMinute(), is(UNIQUE_BYTE_VALUE_1));
        assertThat(address.getLongitudeSecond(), is(UNIQUE_BYTE_VALUE_1));
        assertThat(address.getAltitude(), is(UNIQUE_INT_VALUE_1));
        assertThat(address.getAddress(), is(ma));
    }

    @Test
    public void testSerialization() {
        LispGcAddress other = new LispGcAddress();
        other.deserialize(address1.serialize());

        new EqualsTester()
                .addEqualityGroup(address1, other)
                .testEquals();
    }
}
