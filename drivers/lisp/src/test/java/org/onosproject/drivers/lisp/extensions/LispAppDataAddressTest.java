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
 * Unit tests for LispAppDataAddress extension class.
 */
public class LispAppDataAddressTest {

    private static final IpPrefix IP_ADDRESS_1 = IpPrefix.valueOf("1.2.3.4/24");
    private static final IpPrefix IP_ADDRESS_2 = IpPrefix.valueOf("5.6.7.8/24");

    private static final byte PROTOCOL_VALUE_1 = 0x01;
    private static final int IP_TOS_VALUE_1 = 10;
    private static final short LOCAL_PORT_LOW_VALUE = 1;
    private static final short LOCAL_PORT_HIGH_VALUE = 255;
    private static final short REMOTE_PORT_LOW_VALUE = 2;
    private static final short REMOTE_PORT_HIGH_VALUE = 254;

    private static final byte PROTOCOL_VALUE_2 = 0x02;
    private static final int IP_TOS_VALUE_2 = 20;

    private LispAppDataAddress address1;
    private LispAppDataAddress sameAsAddress1;
    private LispAppDataAddress address2;

    @Before
    public void setUp() {

        MappingAddress ma1 = MappingAddresses.ipv4MappingAddress(IP_ADDRESS_1);

        address1 = new LispAppDataAddress.Builder()
                                            .withProtocol(PROTOCOL_VALUE_1)
                                            .withIpTos(IP_TOS_VALUE_1)
                                            .withLocalPortLow(LOCAL_PORT_LOW_VALUE)
                                            .withLocalPortHigh(LOCAL_PORT_HIGH_VALUE)
                                            .withRemotePortLow(REMOTE_PORT_LOW_VALUE)
                                            .withRemotePortHigh(REMOTE_PORT_HIGH_VALUE)
                                            .withAddress(ma1)
                                            .build();

        sameAsAddress1 = new LispAppDataAddress.Builder()
                                            .withProtocol(PROTOCOL_VALUE_1)
                                            .withIpTos(IP_TOS_VALUE_1)
                                            .withLocalPortLow(LOCAL_PORT_LOW_VALUE)
                                            .withLocalPortHigh(LOCAL_PORT_HIGH_VALUE)
                                            .withRemotePortLow(REMOTE_PORT_LOW_VALUE)
                                            .withRemotePortHigh(REMOTE_PORT_HIGH_VALUE)
                                            .withAddress(ma1)
                                            .build();

        MappingAddress ma2 = MappingAddresses.ipv4MappingAddress(IP_ADDRESS_2);

        address2 = new LispAppDataAddress.Builder()
                                            .withProtocol(PROTOCOL_VALUE_2)
                                            .withIpTos(IP_TOS_VALUE_2)
                                            .withLocalPortLow(LOCAL_PORT_LOW_VALUE)
                                            .withLocalPortHigh(LOCAL_PORT_HIGH_VALUE)
                                            .withRemotePortLow(REMOTE_PORT_LOW_VALUE)
                                            .withRemotePortHigh(REMOTE_PORT_HIGH_VALUE)
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
        LispAppDataAddress address = address1;

        MappingAddress ma = MappingAddresses.ipv4MappingAddress(IP_ADDRESS_1);

        assertThat(address.getProtocol(), is(PROTOCOL_VALUE_1));
        assertThat(address.getIpTos(), is(IP_TOS_VALUE_1));
        assertThat(address.getLocalPortLow(), is(LOCAL_PORT_LOW_VALUE));
        assertThat(address.getLocalPortHigh(), is(LOCAL_PORT_HIGH_VALUE));
        assertThat(address.getRemotePortLow(), is(REMOTE_PORT_LOW_VALUE));
        assertThat(address.getRemotePortHigh(), is(REMOTE_PORT_HIGH_VALUE));
        assertThat(address.getAddress(), is(ma));
    }

    @Test
    public void testSerialization() {
        LispAppDataAddress other = new LispAppDataAddress();
        other.deserialize(address1.serialize());

        new EqualsTester()
                .addEqualityGroup(address1, other)
                .testEquals();
    }
}
