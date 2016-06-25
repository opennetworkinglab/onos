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
package org.onosproject.routing;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.Ip6Prefix;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        Ip6Prefix prefix6 = Ip6Prefix.valueOf("1000::/64");
        Ip6Address nextHop6 = Ip6Address.valueOf("2000::1");
        RouteEntry routeEntry6 = new RouteEntry(prefix6, nextHop6);
        assertThat(routeEntry6.toString(),
                   is("RouteEntry{prefix=1000::/64, nextHop=2000::1}"));
    }

    /**
     * Tests invalid class constructor for null IPv4 prefix.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidConstructorNullIpv4Prefix() {
        Ip4Prefix prefix = null;
        Ip4Address nextHop = Ip4Address.valueOf("5.6.7.8");

        new RouteEntry(prefix, nextHop);
    }

    /**
     * Tests invalid class constructor for null IPv6 prefix.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidConstructorNullIpv6Prefix() {
        Ip6Prefix prefix = null;
        Ip6Address nextHop = Ip6Address.valueOf("2000::1");

        new RouteEntry(prefix, nextHop);
    }

    /**
     * Tests invalid class constructor for null IPv4 next-hop.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidConstructorNullIpv4NextHop() {
        Ip4Prefix prefix = Ip4Prefix.valueOf("1.2.3.0/24");
        Ip4Address nextHop = null;

        new RouteEntry(prefix, nextHop);
    }

    /**
     * Tests invalid class constructor for null IPv6 next-hop.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidConstructorNullIpv6NextHop() {
        Ip6Prefix prefix = Ip6Prefix.valueOf("1000::/64");
        Ip6Address nextHop = null;

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

        Ip6Prefix prefix6 = Ip6Prefix.valueOf("1000::/64");
        Ip6Address nextHop6 = Ip6Address.valueOf("2000::1");
        RouteEntry routeEntry6 = new RouteEntry(prefix6, nextHop6);
        assertThat(routeEntry6.prefix(), is(prefix6));
        assertThat(routeEntry6.nextHop(), is(nextHop6));
    }

    /**
     * Tests creating a binary string from IPv4 prefix.
     */
    @Test
    public void testCreateBinaryString() {
        Ip4Prefix prefix;

        prefix = Ip4Prefix.valueOf("0.0.0.0/0");
        assertThat(RouteEntry.createBinaryString(prefix), is("0"));

        prefix = Ip4Prefix.valueOf("192.168.166.0/22");
        assertThat(RouteEntry.createBinaryString(prefix),
                   is("0" + "1100000010101000101001"));

        prefix = Ip4Prefix.valueOf("192.168.166.0/23");
        assertThat(RouteEntry.createBinaryString(prefix),
                   is("0" + "11000000101010001010011"));

        prefix = Ip4Prefix.valueOf("192.168.166.0/24");
        assertThat(RouteEntry.createBinaryString(prefix),
                   is("0" + "110000001010100010100110"));

        prefix = Ip4Prefix.valueOf("130.162.10.1/25");
        assertThat(RouteEntry.createBinaryString(prefix),
                   is("0" + "1000001010100010000010100"));

        prefix = Ip4Prefix.valueOf("255.255.255.255/32");
        assertThat(RouteEntry.createBinaryString(prefix),
                   is("0" + "11111111111111111111111111111111"));

        Ip6Prefix prefix6;
        Pattern pattern;
        Matcher matcher;

        prefix6 = Ip6Prefix.valueOf("::/0");
        assertThat(RouteEntry.createBinaryString(prefix6), is("0"));

        prefix6 = Ip6Prefix.valueOf("2000::1000/112");
        pattern = Pattern.compile("0" + "00100{108}");
        matcher = pattern.matcher(RouteEntry.createBinaryString(prefix6));
        assertTrue(matcher.matches());

        prefix6 = Ip6Prefix.valueOf("2000::1000/116");
        pattern = Pattern.compile("0" + "00100{108}0001");
        matcher = pattern.matcher(RouteEntry.createBinaryString(prefix6));
        assertTrue(matcher.matches());

        prefix6 = Ip6Prefix.valueOf("2000::2000/116");
        pattern = Pattern.compile("0" + "00100{108}0010");
        matcher = pattern.matcher(RouteEntry.createBinaryString(prefix6));
        assertTrue(matcher.matches());

        prefix6 = Ip6Prefix.valueOf("2000::1234/128");
        pattern = Pattern.compile("0" + "00100{108}0001001000110100");
        matcher = pattern.matcher(RouteEntry.createBinaryString(prefix6));
        assertTrue(matcher.matches());
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

        Ip6Prefix prefix3 = Ip6Prefix.valueOf("1000::/64");
        Ip6Address nextHop3 = Ip6Address.valueOf("2000::2");
        RouteEntry routeEntry3 = new RouteEntry(prefix3, nextHop3);

        Ip6Prefix prefix4 = Ip6Prefix.valueOf("1000::/64");
        Ip6Address nextHop4 = Ip6Address.valueOf("2000::2");
        RouteEntry routeEntry4 = new RouteEntry(prefix4, nextHop4);

        assertThat(routeEntry3, is(routeEntry4));
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

        Ip6Prefix prefix4 = Ip6Prefix.valueOf("1000::/64");
        Ip6Address nextHop4 = Ip6Address.valueOf("2000::1");
        RouteEntry routeEntry4 = new RouteEntry(prefix4, nextHop4);

        Ip6Prefix prefix5 = Ip6Prefix.valueOf("1000::/65");
        Ip6Address nextHop5 = Ip6Address.valueOf("2000::1");
        RouteEntry routeEntry5 = new RouteEntry(prefix5, nextHop5);

        Ip6Prefix prefix6 = Ip6Prefix.valueOf("1000::/64");
        Ip6Address nextHop6 = Ip6Address.valueOf("2000::2");
        RouteEntry routeEntry6 = new RouteEntry(prefix6, nextHop6);

        assertThat(routeEntry4, Matchers.is(not(routeEntry5)));
        assertThat(routeEntry4, Matchers.is(not(routeEntry6)));
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

        Ip6Prefix prefix6 = Ip6Prefix.valueOf("1000::/64");
        Ip6Address nextHop6 = Ip6Address.valueOf("2000::1");
        RouteEntry routeEntry6 = new RouteEntry(prefix6, nextHop6);

        assertThat(routeEntry6.toString(),
                   is("RouteEntry{prefix=1000::/64, nextHop=2000::1}"));
    }
}
