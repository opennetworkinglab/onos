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

package org.onosproject.routing.bgp;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.Ip6Prefix;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for the RouteEntry class.
 */
public class RouteEntryTest {

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

        assertThat(routeEntry1, Matchers.is(Matchers.not(routeEntry2)));
        assertThat(routeEntry1, Matchers.is(Matchers.not(routeEntry3)));

        Ip6Prefix prefix4 = Ip6Prefix.valueOf("1000::/64");
        Ip6Address nextHop4 = Ip6Address.valueOf("2000::1");
        RouteEntry routeEntry4 = new RouteEntry(prefix4, nextHop4);

        Ip6Prefix prefix5 = Ip6Prefix.valueOf("1000::/65");
        Ip6Address nextHop5 = Ip6Address.valueOf("2000::1");
        RouteEntry routeEntry5 = new RouteEntry(prefix5, nextHop5);

        Ip6Prefix prefix6 = Ip6Prefix.valueOf("1000::/64");
        Ip6Address nextHop6 = Ip6Address.valueOf("2000::2");
        RouteEntry routeEntry6 = new RouteEntry(prefix6, nextHop6);

        assertThat(routeEntry4, Matchers.is(Matchers.not(routeEntry5)));
        assertThat(routeEntry4, Matchers.is(Matchers.not(routeEntry6)));
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
