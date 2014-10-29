/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onlab.onos.sdnip;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;

/**
 * Unit tests for the RouteEntry class.
 */
public class RouteEntryTest {
    /**
     * Tests valid class constructor.
     */
    @Test
    public void testConstructor() {
        IpPrefix prefix = IpPrefix.valueOf("1.2.3.0/24");
        IpAddress nextHop = IpAddress.valueOf("5.6.7.8");

        RouteEntry routeEntry = new RouteEntry(prefix, nextHop);
        assertThat(routeEntry.toString(),
                   is("RouteEntry{prefix=1.2.3.0/24, nextHop=5.6.7.8}"));
    }

    /**
     * Tests invalid class constructor for null IPv4 prefix.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidConstructorNullPrefix() {
        IpPrefix prefix = null;
        IpAddress nextHop = IpAddress.valueOf("5.6.7.8");

        new RouteEntry(prefix, nextHop);
    }

    /**
     * Tests invalid class constructor for null IPv4 next-hop.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidConstructorNullNextHop() {
        IpPrefix prefix = IpPrefix.valueOf("1.2.3.0/24");
        IpAddress nextHop = null;

        new RouteEntry(prefix, nextHop);
    }

    /**
     * Tests getting the fields of a route entry.
     */
    @Test
    public void testGetFields() {
        IpPrefix prefix = IpPrefix.valueOf("1.2.3.0/24");
        IpAddress nextHop = IpAddress.valueOf("5.6.7.8");

        RouteEntry routeEntry = new RouteEntry(prefix, nextHop);
        assertThat(routeEntry.prefix(), is(prefix));
        assertThat(routeEntry.nextHop(), is(nextHop));
    }

    /**
     * Tests creating a binary string from IPv4 prefix.
     */
    @Test
    public void testCreateBinaryString() {
        IpPrefix prefix;

        prefix = IpPrefix.valueOf("0.0.0.0/0");
        assertThat(RouteEntry.createBinaryString(prefix), is(""));

        prefix = IpPrefix.valueOf("192.168.166.0/22");
        assertThat(RouteEntry.createBinaryString(prefix),
                   is("1100000010101000101001"));

        prefix = IpPrefix.valueOf("192.168.166.0/23");
        assertThat(RouteEntry.createBinaryString(prefix),
                   is("11000000101010001010011"));

        prefix = IpPrefix.valueOf("192.168.166.0/24");
        assertThat(RouteEntry.createBinaryString(prefix),
                   is("110000001010100010100110"));

        prefix = IpPrefix.valueOf("130.162.10.1/25");
        assertThat(RouteEntry.createBinaryString(prefix),
                   is("1000001010100010000010100"));

        prefix = IpPrefix.valueOf("255.255.255.255/32");
        assertThat(RouteEntry.createBinaryString(prefix),
                   is("11111111111111111111111111111111"));
    }

    /**
     * Tests equality of {@link RouteEntry}.
     */
    @Test
    public void testEquality() {
        IpPrefix prefix1 = IpPrefix.valueOf("1.2.3.0/24");
        IpAddress nextHop1 = IpAddress.valueOf("5.6.7.8");
        RouteEntry routeEntry1 = new RouteEntry(prefix1, nextHop1);

        IpPrefix prefix2 = IpPrefix.valueOf("1.2.3.0/24");
        IpAddress nextHop2 = IpAddress.valueOf("5.6.7.8");
        RouteEntry routeEntry2 = new RouteEntry(prefix2, nextHop2);

        assertThat(routeEntry1, is(routeEntry2));
    }

    /**
     * Tests non-equality of {@link RouteEntry}.
     */
    @Test
    public void testNonEquality() {
        IpPrefix prefix1 = IpPrefix.valueOf("1.2.3.0/24");
        IpAddress nextHop1 = IpAddress.valueOf("5.6.7.8");
        RouteEntry routeEntry1 = new RouteEntry(prefix1, nextHop1);

        IpPrefix prefix2 = IpPrefix.valueOf("1.2.3.0/25");        // Different
        IpAddress nextHop2 = IpAddress.valueOf("5.6.7.8");
        RouteEntry routeEntry2 = new RouteEntry(prefix2, nextHop2);

        IpPrefix prefix3 = IpPrefix.valueOf("1.2.3.0/24");
        IpAddress nextHop3 = IpAddress.valueOf("5.6.7.9");        // Different
        RouteEntry routeEntry3 = new RouteEntry(prefix3, nextHop3);

        assertThat(routeEntry1, is(not(routeEntry2)));
        assertThat(routeEntry1, is(not(routeEntry3)));
    }

    /**
     * Tests object string representation.
     */
    @Test
    public void testToString() {
        IpPrefix prefix = IpPrefix.valueOf("1.2.3.0/24");
        IpAddress nextHop = IpAddress.valueOf("5.6.7.8");
        RouteEntry routeEntry = new RouteEntry(prefix, nextHop);

        assertThat(routeEntry.toString(),
                   is("RouteEntry{prefix=1.2.3.0/24, nextHop=5.6.7.8}"));
    }
}
