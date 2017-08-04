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
package org.onosproject.lisp.ctl.impl.tree;

import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for IpConcurrentRadixTree.
 */
public class IpConcurrentRadixTreeTest {

    private static final String IPV4_ADDRESS_1 = "10.1.2.1/32";
    private static final String IPV4_ADDRESS_2 = "10.1.2.2/32";
    private static final String IPV4_ADDRESS_3 = "10.1.2.0/24";
    private static final String IPV4_ADDRESS_4 = "10.1.0.0/16";
    private static final String IPV4_ADDRESS_5 = "10.1.2.3/32";
    private static final String IPV4_ADDRESS_6 = "10.1.3.0/24";

    private static final String IPV6_ADDRESS_1 = "1000:2000:3000:4000:5000:6000:7000:8000/128";
    private static final String IPV6_ADDRESS_2 = "1000:2000:3000:4000:5000:6000:7000:9000/128";
    private static final String IPV6_ADDRESS_3 = "1000:2000:3000:4000:5000:6000:7000:0000/112";
    private static final String IPV6_ADDRESS_4 = "1000:2000:3000:4000:5000:6000:0000:0000/96";
    private static final String IPV6_ADDRESS_5 = "1000:2000:3000:4000:5000:0000:0000:0000/80";
    private static final String IPV6_ADDRESS_6 = "1000:2000:3000:4000:0000:0000:0000:0000/64";
    private static final String IPV6_ADDRESS_7 = "1000:2000:3000:4000:5000:6000:7000:A000/128";
    private static final String IPV6_ADDRESS_8 = "1000:2000:3000:4000:5000:6000:8000:0000/112";
    private static final String IPV6_ADDRESS_9 = "1000:2000:3000:4000:5000:8000:0000:0000/96";
    private static final String IPV6_ADDRESS_10 = "1000:2000:3000:4000:8000:0000:0000:0000/80";

    private final IpPrefix ipv4PrefixKey1 = IpPrefix.valueOf(IPV4_ADDRESS_1);
    private final IpPrefix ipv4PrefixKey2 = IpPrefix.valueOf(IPV4_ADDRESS_2);
    private final IpPrefix ipv4PrefixKey3 = IpPrefix.valueOf(IPV4_ADDRESS_3);
    private final IpPrefix ipv4PrefixKey4 = IpPrefix.valueOf(IPV4_ADDRESS_4);
    private final IpPrefix ipv4PrefixKey5 = IpPrefix.valueOf(IPV4_ADDRESS_5);
    private final IpPrefix ipv4PrefixKey6 = IpPrefix.valueOf(IPV4_ADDRESS_6);

    private final IpPrefix ipv6PrefixKey1 = IpPrefix.valueOf(IPV6_ADDRESS_1);
    private final IpPrefix ipv6PrefixKey2 = IpPrefix.valueOf(IPV6_ADDRESS_2);
    private final IpPrefix ipv6PrefixKey3 = IpPrefix.valueOf(IPV6_ADDRESS_3);
    private final IpPrefix ipv6PrefixKey4 = IpPrefix.valueOf(IPV6_ADDRESS_4);
    private final IpPrefix ipv6PrefixKey5 = IpPrefix.valueOf(IPV6_ADDRESS_5);
    private final IpPrefix ipv6PrefixKey6 = IpPrefix.valueOf(IPV6_ADDRESS_6);
    private final IpPrefix ipv6PrefixKey7 = IpPrefix.valueOf(IPV6_ADDRESS_7);
    private final IpPrefix ipv6PrefixKey8 = IpPrefix.valueOf(IPV6_ADDRESS_8);
    private final IpPrefix ipv6PrefixKey9 = IpPrefix.valueOf(IPV6_ADDRESS_9);
    private final IpPrefix ipv6PrefixKey10 = IpPrefix.valueOf(IPV6_ADDRESS_10);

    private static final int IPV4_PREFIX_VALUE_1 = 1;
    private static final int IPV4_PREFIX_VALUE_2 = 2;
    private static final int IPV4_PREFIX_VALUE_3 = 3;
    private static final int IPV4_PREFIX_VALUE_4 = 4;
    private static final int IPV4_PREFIX_VALUE_5 = 5;

    private static final int IPV6_PREFIX_VALUE_1 = 11;
    private static final int IPV6_PREFIX_VALUE_2 = 12;
    private static final int IPV6_PREFIX_VALUE_3 = 13;
    private static final int IPV6_PREFIX_VALUE_4 = 14;
    private static final int IPV6_PREFIX_VALUE_5 = 15;
    private static final int IPV6_PREFIX_VALUE_6 = 16;
    private static final int IPV6_PREFIX_VALUE_7 = 17;

    private final IpConcurrentRadixTree<Integer> radixTree = new IpConcurrentRadixTree<>();

    @Before
    public void setUp() {
        radixTree.put(ipv4PrefixKey1, IPV4_PREFIX_VALUE_1);
        radixTree.put(ipv4PrefixKey2, IPV4_PREFIX_VALUE_2);
        radixTree.put(ipv4PrefixKey3, IPV4_PREFIX_VALUE_3);
        radixTree.put(ipv4PrefixKey4, IPV4_PREFIX_VALUE_4);

        radixTree.put(ipv6PrefixKey1, IPV6_PREFIX_VALUE_1);
        radixTree.put(ipv6PrefixKey2, IPV6_PREFIX_VALUE_2);
        radixTree.put(ipv6PrefixKey3, IPV6_PREFIX_VALUE_3);
        radixTree.put(ipv6PrefixKey4, IPV6_PREFIX_VALUE_4);
        radixTree.put(ipv6PrefixKey5, IPV6_PREFIX_VALUE_5);
        radixTree.put(ipv6PrefixKey6, IPV6_PREFIX_VALUE_6);
    }

    @Test
    public void testPut() {
        radixTree.put(ipv4PrefixKey5, IPV4_PREFIX_VALUE_5);
        assertThat("Incorrect size of radix tree for IPv4 maps",
                radixTree.size(IpAddress.Version.INET), is(5));
        assertThat("IPv4 prefix key has not been inserted correctly",
                radixTree.getValueForExactAddress(ipv4PrefixKey5), is(5));

        radixTree.put(ipv6PrefixKey7, IPV6_PREFIX_VALUE_7);
        assertThat("Incorrect size of radix tree for IPv6 maps",
                radixTree.size(IpAddress.Version.INET6), is(7));
        assertThat("IPv6 prefix key has not been inserted correctly",
                radixTree.getValueForExactAddress(ipv6PrefixKey7), is(17));

        radixTree.put(ipv4PrefixKey1, 8801);
        assertThat("Incorrect size of radix tree for IPv4 maps",
                radixTree.size(IpAddress.Version.INET), is(5));
        assertThat("IPv4 prefix key has not been inserted correctly",
                radixTree.getValueForExactAddress(ipv4PrefixKey1), is(8801));
    }

    @Test
    public void testRemove() {
        radixTree.remove(ipv4PrefixKey1);
        assertThat("Incorrect size of radix tree for IPv4 maps",
                radixTree.size(IpAddress.Version.INET), is(3));
        assertNull("There should not be any value associated with the key ipv4PrefixKey1",
                radixTree.getValueForExactAddress(ipv4PrefixKey1));

        radixTree.remove(ipv6PrefixKey1);
        assertThat("Incorrect size of radix tree for IPv6 maps",
                radixTree.size(IpAddress.Version.INET6), is(5));
        assertNull("There should not be any value associated with the key ipv6PrefixKey1",
                radixTree.getValueForExactAddress(ipv6PrefixKey1));
    }

    @Test
    public void testGetValueForExactAddress() {
        assertThat("IPv4 prefix has not been inserted correctly",
                radixTree.getValueForExactAddress(ipv4PrefixKey1), is(1));
        assertThat("IPv4 prefix has not been inserted correctly",
                radixTree.getValueForExactAddress(ipv4PrefixKey2), is(2));
        assertThat("IPv4 prefix has not been inserted correctly",
                radixTree.getValueForExactAddress(ipv4PrefixKey3), is(3));
        assertThat("IPv4 prefix has not been inserted correctly",
                radixTree.getValueForExactAddress(ipv4PrefixKey4), is(4));

        assertThat("IPv6 prefix has not been inserted correctly",
                radixTree.getValueForExactAddress(ipv6PrefixKey1), is(11));
        assertThat("IPv6 prefix has not been inserted correctly",
                radixTree.getValueForExactAddress(ipv6PrefixKey2), is(12));
        assertThat("IPv6 prefix has not been inserted correctly",
                radixTree.getValueForExactAddress(ipv6PrefixKey3), is(13));
        assertThat("IPv6 prefix has not been inserted correctly",
                radixTree.getValueForExactAddress(ipv6PrefixKey4), is(14));
        assertThat("IPv6 prefix has not been inserted correctly",
                radixTree.getValueForExactAddress(ipv6PrefixKey5), is(15));
        assertThat("IPv6 prefix has not been inserted correctly",
                radixTree.getValueForExactAddress(ipv6PrefixKey6), is(16));
    }

    @Test
    public void testGetValuesForAddressesStartingWith() {
        assertTrue("IPv4 prefix has not been inserted correctly",
                radixTree.getValuesForAddressesStartingWith(ipv4PrefixKey1).contains(1));

        assertTrue("IPv4 prefix has not been inserted correctly",
                radixTree.getValuesForAddressesStartingWith(ipv4PrefixKey3).contains(1));
        assertTrue("IPv4 prefix has not been inserted correctly",
                radixTree.getValuesForAddressesStartingWith(ipv4PrefixKey3).contains(2));
        assertTrue("IPv4 prefix has not been inserted correctly",
                radixTree.getValuesForAddressesStartingWith(ipv4PrefixKey3).contains(3));

        assertTrue("IPv4 prefix has not been inserted correctly",
                radixTree.getValuesForAddressesStartingWith(ipv4PrefixKey4).contains(1));
        assertTrue("IPv4 prefix has not been inserted correctly",
                radixTree.getValuesForAddressesStartingWith(ipv4PrefixKey4).contains(2));
        assertTrue("IPv4 prefix has not been inserted correctly",
                radixTree.getValuesForAddressesStartingWith(ipv4PrefixKey4).contains(3));
        assertTrue("IPv4 prefix has not been inserted correctly",
                radixTree.getValuesForAddressesStartingWith(ipv4PrefixKey4).contains(4));

        assertTrue("IPv6 prefix has not been inserted correctly",
                radixTree.getValuesForAddressesStartingWith(ipv6PrefixKey1).contains(11));
        assertTrue("IPv6 prefix has not been inserted correctly",
                radixTree.getValuesForAddressesStartingWith(ipv6PrefixKey3).contains(11));
        assertTrue("IPv6 prefix has not been inserted correctly",
                radixTree.getValuesForAddressesStartingWith(ipv6PrefixKey3).contains(12));
        assertTrue("IPv6 prefix has not been inserted correctly",
                radixTree.getValuesForAddressesStartingWith(ipv6PrefixKey3).contains(13));

        assertTrue("IPv6 prefix has not been inserted correctly",
                radixTree.getValuesForAddressesStartingWith(ipv6PrefixKey6).contains(11));
        assertTrue("IPv6 prefix has not been inserted correctly",
                radixTree.getValuesForAddressesStartingWith(ipv6PrefixKey6).contains(12));
        assertTrue("IPv6 prefix has not been inserted correctly",
                radixTree.getValuesForAddressesStartingWith(ipv6PrefixKey6).contains(13));
        assertTrue("IPv6 prefix has not been inserted correctly",
                radixTree.getValuesForAddressesStartingWith(ipv6PrefixKey6).contains(14));
        assertTrue("IPv6 prefix has not been inserted correctly",
                radixTree.getValuesForAddressesStartingWith(ipv6PrefixKey6).contains(15));
        assertTrue("IPv6 prefix has not been inserted correctly",
                radixTree.getValuesForAddressesStartingWith(ipv6PrefixKey6).contains(16));
    }

    @Test
    public void testGetValueForClosestParentAddress() {
        assertThat("Failed to fetch a value that closest associated with IPv4 prefix",
                radixTree.getValueForClosestParentAddress(ipv4PrefixKey5), is(3));

        assertThat("Failed to fetch a value that closest associated with IPv4 prefix",
                radixTree.getValueForClosestParentAddress(ipv4PrefixKey6), is(4));

        assertThat("Failed to fetch a value that closest associated with IPv6 prefix",
                radixTree.getValueForClosestParentAddress(ipv6PrefixKey7), is(13));

        assertThat("Failed to fetch a value that closest associated with IPv6 prefix",
                radixTree.getValueForClosestParentAddress(ipv6PrefixKey8), is(14));

        assertThat("Failed to fetch a value that closest associated with IPv6 prefix",
                radixTree.getValueForClosestParentAddress(ipv6PrefixKey9), is(15));

        assertThat("Failed to fetch a value that closest associated with IPv6 prefix",
                radixTree.getValueForClosestParentAddress(ipv6PrefixKey10), is(16));
    }

    @Test
    public void testSize() {
        assertThat("Incorrect size of radix tree for IPv4 maps",
                radixTree.size(IpAddress.Version.INET), is(4));
        assertThat("Incorrect size of radix tree for IPv6 maps",
                radixTree.size(IpAddress.Version.INET6), is(6));
    }

    @Test
    public void testClear() {
        radixTree.clear();
        assertThat("Incorrect size of radix tree for IPv4 maps",
                radixTree.size(IpAddress.Version.INET), is(0));
        assertThat("Incorrect size of radix tree for IPv6 maps",
                radixTree.size(IpAddress.Version.INET6), is(0));
    }
}
