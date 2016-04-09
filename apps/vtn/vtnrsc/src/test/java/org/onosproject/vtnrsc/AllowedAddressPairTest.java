/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.vtnrsc;

import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for AllowedAddressPair class.
 */
public class AllowedAddressPairTest {

    final IpAddress ip1 = IpAddress.valueOf("192.168.0.1");
    final IpAddress ip2 = IpAddress.valueOf("192.168.0.2");
    final MacAddress mac1 = MacAddress.valueOf("fa:16:3e:76:83:88");
    final MacAddress mac2 = MacAddress.valueOf("aa:16:3e:76:83:88");

    /**
     * Checks that the AllowedAddressPair class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(AllowedAddressPair.class);
    }

    /**
     * Checks the operation of equals().
     */
    @Test
    public void testEquals() {
        AllowedAddressPair p1 = AllowedAddressPair
                .allowedAddressPair(ip1, mac1);
        AllowedAddressPair p2 = AllowedAddressPair
                .allowedAddressPair(ip1, mac1);
        AllowedAddressPair p3 = AllowedAddressPair
                .allowedAddressPair(ip2, mac2);
        new EqualsTester().addEqualityGroup(p1, p2).addEqualityGroup(p3)
                .testEquals();
    }

    /**
     * Checks the construction of a AllowedAddressPair object.
     */
    @Test
    public void testConstruction() {
        AllowedAddressPair allowedAddressPair = AllowedAddressPair
                .allowedAddressPair(ip1, mac1);
        assertThat(ip1, is(notNullValue()));
        assertThat(ip1, is(allowedAddressPair.ip()));
        assertThat(mac1, is(notNullValue()));
        assertThat(mac1, is(allowedAddressPair.mac()));
    }
}
