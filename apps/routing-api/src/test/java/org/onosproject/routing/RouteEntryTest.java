/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.routing;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for the RouteEntry class.
 */
public class RouteEntryTest {
    /**
     * Tests valid class constructor.
     */
    @Test
    public void testConstructor() {
        Ip4Prefix prefix = Ip4Prefix.valueOf("1.2.3.0/24");
        Ip4Address nextHop = Ip4Address.valueOf("5.6.7.8");

        RouteEntry routeEntry = new RouteEntry(prefix, nextHop);
        assertThat(routeEntry.toString(),
                   is("RouteEntry{prefix=1.2.3.0/24, nextHop=5.6.7.8}"));
    }

    /**
     * Tests invalid class constructor for null IPv4 prefix.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidConstructorNullPrefix() {
        Ip4Prefix prefix = null;
        Ip4Address nextHop = Ip4Address.valueOf("5.6.7.8");

        new RouteEntry(prefix, nextHop);
    }

    /**
     * Tests invalid class constructor for null IPv4 next-hop.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidConstructorNullNextHop() {
        Ip4Prefix prefix = Ip4Prefix.valueOf("1.2.3.0/24");
        Ip4Address nextHop = null;

        new RouteEntry(prefix, nextHop);
    }

    /**
     * Tests getting the fields of a route entry.
     */
    @Test
    public void testGetFields() {
        Ip4Prefix prefix = Ip4Prefix.valueOf("1.2.3.0/24");
        Ip4Address nextHop = Ip4Address.valueOf("5.6.7.8");

        RouteEntry routeEntry = new RouteEntry(prefix, nextHop);
        assertThat(routeEntry.prefix(), is(prefix));
        assertThat(routeEntry.nextHop(), is(nextHop));
    }

    /**
     * Tests creating a binary string from IPv4 prefix.
     */
    @Test
    public void testCreateBinaryString() {
        Ip4Prefix prefix;

        prefix = Ip4Prefix.valueOf("0.0.0.0/0");
        assertThat(RouteEntry.createBinaryString(prefix), is(""));

        prefix = Ip4Prefix.valueOf("192.168.166.0/22");
        assertThat(RouteEntry.createBinaryString(prefix),
                   is("1100000010101000101001"));

        prefix = Ip4Prefix.valueOf("192.168.166.0/23");
        assertThat(RouteEntry.createBinaryString(prefix),
                   is("11000000101010001010011"));

        prefix = Ip4Prefix.valueOf("192.168.166.0/24");
        assertThat(RouteEntry.createBinaryString(prefix),
                   is("110000001010100010100110"));

        prefix = Ip4Prefix.valueOf("130.162.10.1/25");
        assertThat(RouteEntry.createBinaryString(prefix),
                   is("1000001010100010000010100"));

        prefix = Ip4Prefix.valueOf("255.255.255.255/32");
        assertThat(RouteEntry.createBinaryString(prefix),
                   is("11111111111111111111111111111111"));
    }

    /**
     * Tests equality of {@link RouteEntry}.
     */
    @Test
    public void testEquality() {
        Ip4Prefix prefix1 = Ip4Prefix.valueOf("1.2.3.0/24");
        Ip4Address nextHop1 = Ip4Address.valueOf("5.6.7.8");
        RouteEntry routeEntry1 = new RouteEntry(prefix1, nextHop1);

        Ip4Prefix prefix2 = Ip4Prefix.valueOf("1.2.3.0/24");
        Ip4Address nextHop2 = Ip4Address.valueOf("5.6.7.8");
        RouteEntry routeEntry2 = new RouteEntry(prefix2, nextHop2);

        assertThat(routeEntry1, is(routeEntry2));
    }

    /**
     * Tests non-equality of {@link RouteEntry}.
     */
    @Test
    public void testNonEquality() {
        Ip4Prefix prefix1 = Ip4Prefix.valueOf("1.2.3.0/24");
        Ip4Address nextHop1 = Ip4Address.valueOf("5.6.7.8");
        RouteEntry routeEntry1 = new RouteEntry(prefix1, nextHop1);

        Ip4Prefix prefix2 = Ip4Prefix.valueOf("1.2.3.0/25");      // Different
        Ip4Address nextHop2 = Ip4Address.valueOf("5.6.7.8");
        RouteEntry routeEntry2 = new RouteEntry(prefix2, nextHop2);

        Ip4Prefix prefix3 = Ip4Prefix.valueOf("1.2.3.0/24");
        Ip4Address nextHop3 = Ip4Address.valueOf("5.6.7.9");      // Different
        RouteEntry routeEntry3 = new RouteEntry(prefix3, nextHop3);

        assertThat(routeEntry1, Matchers.is(not(routeEntry2)));
        assertThat(routeEntry1, Matchers.is(not(routeEntry3)));
    }

    /**
     * Tests object string representation.
     */
    @Test
    public void testToString() {
        Ip4Prefix prefix = Ip4Prefix.valueOf("1.2.3.0/24");
        Ip4Address nextHop = Ip4Address.valueOf("5.6.7.8");
        RouteEntry routeEntry = new RouteEntry(prefix, nextHop);

        assertThat(routeEntry.toString(),
                   is("RouteEntry{prefix=1.2.3.0/24, nextHop=5.6.7.8}"));
    }
}
