/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onlab.packet;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutableBaseClass;

/**
 * Tests for class {@link IpPrefix}.
 */
public class IpPrefixTest {
    /**
     * Tests the immutability of {@link IpPrefix}.
     */
    @Test
    public void testImmutable() {
        assertThatClassIsImmutableBaseClass(IpPrefix.class);
    }

    /**
     * Tests the maximum mask length.
     */
    @Test
    public void testMaxMaskLength() {
        assertThat(IpPrefix.MAX_INET_MASK_LENGTH, is(32));
        assertThat(IpPrefix.MAX_INET6_MASK_LENGTH, is(128));
    }

    /**
     * Tests returning the IP version of the prefix.
     */
    @Test
    public void testVersion() {
        IpPrefix ipPrefix;

        // IPv4
        ipPrefix = IpPrefix.valueOf("0.0.0.0/0");
        assertThat(ipPrefix.version(), is(IpAddress.Version.INET));

        // IPv6
        ipPrefix = IpPrefix.valueOf("::/0");
        assertThat(ipPrefix.version(), is(IpAddress.Version.INET6));
    }

    /**
     * Tests whether the IP version of a prefix is IPv4.
     */
    @Test
    public void testIsIp4() {
        IpPrefix ipPrefix;

        // IPv4
        ipPrefix = IpPrefix.valueOf("0.0.0.0/0");
        assertTrue(ipPrefix.isIp4());

        // IPv6
        ipPrefix = IpPrefix.valueOf("::/0");
        assertFalse(ipPrefix.isIp4());
    }

    /**
     * Tests whether the IP version of a prefix is IPv6.
     */
    @Test
    public void testIsIp6() {
        IpPrefix ipPrefix;

        // IPv4
        ipPrefix = IpPrefix.valueOf("0.0.0.0/0");
        assertFalse(ipPrefix.isIp6());

        // IPv6
        ipPrefix = IpPrefix.valueOf("::/0");
        assertTrue(ipPrefix.isIp6());
    }

    /**
     * Tests if the prefix is a multicast prefix.
     */
    @Test
    public void testIsMulticast() {
        IpPrefix v4Unicast = IpPrefix.valueOf("10.0.0.1/16");
        IpPrefix v4Multicast = IpPrefix.valueOf("224.0.0.1/4");
        IpPrefix v4Overlap = IpPrefix.valueOf("192.0.0.0/2");
        IpPrefix v6Unicast = IpPrefix.valueOf("1000::1/8");
        IpPrefix v6Multicast = IpPrefix.valueOf("ff02::1/8");
        IpPrefix v6Overlap = IpPrefix.valueOf("ff00::1/4");
        assertFalse(v4Unicast.isMulticast());
        assertTrue(v4Multicast.isMulticast());
        assertFalse(v4Overlap.isMulticast());
        assertFalse(v6Unicast.isMulticast());
        assertTrue(v6Multicast.isMulticast());
        assertFalse(v6Overlap.isMulticast());
    }

    /**
     * Tests returning the IP address value and IP address prefix length of
     * an IPv4 prefix.
     */
    @Test
    public void testAddressAndPrefixLengthIPv4() {
        IpPrefix ipPrefix;

        ipPrefix = IpPrefix.valueOf("1.2.3.0/24");
        assertThat(ipPrefix.address(), equalTo(IpAddress.valueOf("1.2.3.0")));
        assertThat(ipPrefix.prefixLength(), is(24));

        ipPrefix = IpPrefix.valueOf("1.2.3.4/24");
        assertThat(ipPrefix.address(), equalTo(IpAddress.valueOf("1.2.3.0")));
        assertThat(ipPrefix.prefixLength(), is(24));

        ipPrefix = IpPrefix.valueOf("1.2.3.4/32");
        assertThat(ipPrefix.address(), equalTo(IpAddress.valueOf("1.2.3.4")));
        assertThat(ipPrefix.prefixLength(), is(32));

        ipPrefix = IpPrefix.valueOf("1.2.3.5/32");
        assertThat(ipPrefix.address(), equalTo(IpAddress.valueOf("1.2.3.5")));
        assertThat(ipPrefix.prefixLength(), is(32));

        ipPrefix = IpPrefix.valueOf("0.0.0.0/0");
        assertThat(ipPrefix.address(), equalTo(IpAddress.valueOf("0.0.0.0")));
        assertThat(ipPrefix.prefixLength(), is(0));

        ipPrefix = IpPrefix.valueOf("255.255.255.255/32");
        assertThat(ipPrefix.address(),
                   equalTo(IpAddress.valueOf("255.255.255.255")));
        assertThat(ipPrefix.prefixLength(), is(32));
    }

    /**
     * Tests returning the IP address value and IP address prefix length of
     * an IPv6 prefix.
     */
    @Test
    public void testAddressAndPrefixLengthIPv6() {
        IpPrefix ipPrefix;

        ipPrefix = IpPrefix.valueOf("1100::/8");
        assertThat(ipPrefix.address(), equalTo(IpAddress.valueOf("1100::")));
        assertThat(ipPrefix.prefixLength(), is(8));

        ipPrefix =
            IpPrefix.valueOf("1111:2222:3333:4444:5555:6666:7777:8885/8");
        assertThat(ipPrefix.address(), equalTo(IpAddress.valueOf("1100::")));
        assertThat(ipPrefix.prefixLength(), is(8));

        ipPrefix =
            IpPrefix.valueOf("1111:2222:3333:4444:5555:6666:7777:8800/120");
        assertThat(ipPrefix.address(),
                   equalTo(IpAddress.valueOf("1111:2222:3333:4444:5555:6666:7777:8800")));
        assertThat(ipPrefix.prefixLength(), is(120));

        ipPrefix =
            IpPrefix.valueOf("1111:2222:3333:4444:5555:6666:7777:8885/128");
        assertThat(ipPrefix.address(),
                   equalTo(IpAddress.valueOf("1111:2222:3333:4444:5555:6666:7777:8885")));
        assertThat(ipPrefix.prefixLength(), is(128));

        ipPrefix = IpPrefix.valueOf("::/0");
        assertThat(ipPrefix.address(), equalTo(IpAddress.valueOf("::")));
        assertThat(ipPrefix.prefixLength(), is(0));

        ipPrefix =
            IpPrefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128");
        assertThat(ipPrefix.address(),
                   equalTo(IpAddress.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")));
        assertThat(ipPrefix.prefixLength(), is(128));

        ipPrefix =
            IpPrefix.valueOf("1111:2222:3333:4444:5555:6666:7777:8885/64");
        assertThat(ipPrefix.address(),
                   equalTo(IpAddress.valueOf("1111:2222:3333:4444::")));
        assertThat(ipPrefix.prefixLength(), is(64));
    }

    /**
     * Tests getting the Ip4Prefix and Ip6Prefix view of the IP prefix.
     */
    @Test
    public void testGetIp4AndIp6PrefixView() {
        IpPrefix ipPrefix;
        Ip4Prefix ip4Prefix;
        Ip6Prefix ip6Prefix;

        // Pure IPv4 IpPrefix
        ipPrefix = IpPrefix.valueOf("1.2.3.0/24");
        ip4Prefix = ipPrefix.getIp4Prefix();
        ip6Prefix = ipPrefix.getIp6Prefix();
        assertThat(ip4Prefix.toString(), is("1.2.3.0/24"));
        assertNull(ip6Prefix);

        // IPv4 IpPrefix that is Ip4Prefix
        ipPrefix = Ip4Prefix.valueOf("1.2.3.0/24");
        ip4Prefix = ipPrefix.getIp4Prefix();
        ip6Prefix = ipPrefix.getIp6Prefix();
        assertThat(ip4Prefix.toString(), is("1.2.3.0/24"));
        assertNull(ip6Prefix);

        // Pure IPv6 IpPrefix
        ipPrefix = IpPrefix.valueOf("1111:2222::/64");
        ip4Prefix = ipPrefix.getIp4Prefix();
        ip6Prefix = ipPrefix.getIp6Prefix();
        assertNull(ip4Prefix);
        assertThat(ip6Prefix.toString(), is("1111:2222::/64"));

        // IPv6 IpPrefix that is Ip6Prefix
        ipPrefix = Ip6Prefix.valueOf("1111:2222::/64");
        ip4Prefix = ipPrefix.getIp4Prefix();
        ip6Prefix = ipPrefix.getIp6Prefix();
        assertNull(ip4Prefix);
        assertThat(ip6Prefix.toString(), is("1111:2222::/64"));
    }

    /**
     * Tests valueOf() converter for IPv4 integer value.
     */
    @Test
    public void testValueOfForIntegerIPv4() {
        IpPrefix ipPrefix;

        ipPrefix = IpPrefix.valueOf(0x01020304, 24);
        assertThat(ipPrefix.toString(), is("1.2.3.0/24"));

        ipPrefix = IpPrefix.valueOf(0x01020304, 32);
        assertThat(ipPrefix.toString(), is("1.2.3.4/32"));

        ipPrefix = IpPrefix.valueOf(0x01020305, 32);
        assertThat(ipPrefix.toString(), is("1.2.3.5/32"));

        ipPrefix = IpPrefix.valueOf(0, 0);
        assertThat(ipPrefix.toString(), is("0.0.0.0/0"));

        ipPrefix = IpPrefix.valueOf(0, 32);
        assertThat(ipPrefix.toString(), is("0.0.0.0/32"));

        ipPrefix = IpPrefix.valueOf(0xffffffff, 0);
        assertThat(ipPrefix.toString(), is("0.0.0.0/0"));

        ipPrefix = IpPrefix.valueOf(0xffffffff, 16);
        assertThat(ipPrefix.toString(), is("255.255.0.0/16"));

        ipPrefix = IpPrefix.valueOf(0xffffffff, 32);
        assertThat(ipPrefix.toString(), is("255.255.255.255/32"));
    }

    /**
     * Tests invalid valueOf() converter for IPv4 integer value and
     * negative prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfIntegerNegativePrefixLengthIPv4() {
        IpPrefix ipPrefix;

        ipPrefix = IpPrefix.valueOf(0x01020304, -1);
    }

    /**
     * Tests invalid valueOf() converter for IPv4 integer value and
     * too long prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfIntegerTooLongPrefixLengthIPv4() {
        IpPrefix ipPrefix;

        ipPrefix = IpPrefix.valueOf(0x01020304, 33);
    }

    /**
     * Tests valueOf() converter for IPv4 byte array.
     */
    @Test
    public void testValueOfByteArrayIPv4() {
        IpPrefix ipPrefix;
        byte[] value;

        value = new byte[] {1, 2, 3, 4};
        ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET, value, 24);
        assertThat(ipPrefix.toString(), is("1.2.3.0/24"));

        ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET, value, 32);
        assertThat(ipPrefix.toString(), is("1.2.3.4/32"));

        value = new byte[] {1, 2, 3, 5};
        ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET, value, 32);
        assertThat(ipPrefix.toString(), is("1.2.3.5/32"));

        value = new byte[] {0, 0, 0, 0};
        ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET, value, 0);
        assertThat(ipPrefix.toString(), is("0.0.0.0/0"));

        ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET, value, 32);
        assertThat(ipPrefix.toString(), is("0.0.0.0/32"));

        value = new byte[] {(byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff};
        ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET, value, 0);
        assertThat(ipPrefix.toString(), is("0.0.0.0/0"));

        ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET, value, 16);
        assertThat(ipPrefix.toString(), is("255.255.0.0/16"));

        ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET, value, 32);
        assertThat(ipPrefix.toString(), is("255.255.255.255/32"));
    }

    /**
     * Tests valueOf() converter for IPv6 byte array.
     */
    @Test
    public void testValueOfByteArrayIPv6() {
        IpPrefix ipPrefix;
        byte[] value;

        value = new byte[] {0x11, 0x11, 0x22, 0x22,
                            0x33, 0x33, 0x44, 0x44,
                            0x55, 0x55, 0x66, 0x66,
                            0x77, 0x77, (byte) 0x88, (byte) 0x88};
        ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET6, value, 120);
        assertThat(ipPrefix.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8800/120"));

        ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET6, value, 128);
        assertThat(ipPrefix.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8888/128"));

        value = new byte[] {0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00};
        ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET6, value, 0);
        assertThat(ipPrefix.toString(), is("::/0"));

        ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET6, value, 128);
        assertThat(ipPrefix.toString(), is("::/128"));

        value = new byte[] {(byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff};
        ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET6, value, 0);
        assertThat(ipPrefix.toString(), is("::/0"));

        ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET6, value, 64);
        assertThat(ipPrefix.toString(), is("ffff:ffff:ffff:ffff::/64"));

        ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET6, value, 128);
        assertThat(ipPrefix.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128"));
    }

    /**
     * Tests invalid valueOf() converter for a null array for IPv4.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidValueOfNullArrayIPv4() {
        IpPrefix ipPrefix;
        byte[] value;

        value = null;
        ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET, value, 24);
    }

    /**
     * Tests invalid valueOf() converter for a null array for IPv6.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidValueOfNullArrayIPv6() {
        IpPrefix ipPrefix;
        byte[] value;

        value = null;
        ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET6, value, 120);
    }

    /**
     * Tests invalid valueOf() converter for a short array for IPv4.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfShortArrayIPv4() {
        IpPrefix ipPrefix;
        byte[] value;

        value = new byte[] {1, 2, 3};
        ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET, value, 24);
    }

    /**
     * Tests invalid valueOf() converter for a short array for IPv6.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfShortArrayIPv6() {
        IpPrefix ipPrefix;
        byte[] value;

        value = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9};
        ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET6, value, 120);
    }

    /**
     * Tests invalid valueOf() converter for IPv4 byte array and
     * negative prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfByteArrayNegativePrefixLengthIPv4() {
        IpPrefix ipPrefix;
        byte[] value;

        value = new byte[] {1, 2, 3, 4};
        ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET, value, -1);
    }

    /**
     * Tests invalid valueOf() converter for IPv6 byte array and
     * negative prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfByteArrayNegativePrefixLengthIPv6() {
        IpPrefix ipPrefix;
        byte[] value;

        value = new byte[] {0x11, 0x11, 0x22, 0x22,
                            0x33, 0x33, 0x44, 0x44,
                            0x55, 0x55, 0x66, 0x66,
                            0x77, 0x77, (byte) 0x88, (byte) 0x88};
        ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET6, value, -1);
    }

    /**
     * Tests invalid valueOf() converter for IPv4 byte array and
     * too long prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfByteArrayTooLongPrefixLengthIPv4() {
        IpPrefix ipPrefix;
        byte[] value;

        value = new byte[] {1, 2, 3, 4};
        ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET, value, 33);
    }

    /**
     * Tests invalid valueOf() converter for IPv6 byte array and
     * too long prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfByteArrayTooLongPrefixLengthIPv6() {
        IpPrefix ipPrefix;
        byte[] value;

        value = new byte[] {0x11, 0x11, 0x22, 0x22,
                            0x33, 0x33, 0x44, 0x44,
                            0x55, 0x55, 0x66, 0x66,
                            0x77, 0x77, (byte) 0x88, (byte) 0x88};
        ipPrefix = IpPrefix.valueOf(IpAddress.Version.INET6, value, 129);
    }

    /**
     * Tests valueOf() converter for IPv4 address.
     */
    @Test
    public void testValueOfAddressIPv4() {
        IpAddress ipAddress;
        IpPrefix ipPrefix;

        ipAddress = IpAddress.valueOf("1.2.3.4");
        ipPrefix = IpPrefix.valueOf(ipAddress, 24);
        assertThat(ipPrefix.toString(), is("1.2.3.0/24"));

        ipPrefix = IpPrefix.valueOf(ipAddress, 32);
        assertThat(ipPrefix.toString(), is("1.2.3.4/32"));

        ipAddress = IpAddress.valueOf("1.2.3.5");
        ipPrefix = IpPrefix.valueOf(ipAddress, 32);
        assertThat(ipPrefix.toString(), is("1.2.3.5/32"));

        ipAddress = IpAddress.valueOf("0.0.0.0");
        ipPrefix = IpPrefix.valueOf(ipAddress, 0);
        assertThat(ipPrefix.toString(), is("0.0.0.0/0"));

        ipPrefix = IpPrefix.valueOf(ipAddress, 32);
        assertThat(ipPrefix.toString(), is("0.0.0.0/32"));

        ipAddress = IpAddress.valueOf("255.255.255.255");
        ipPrefix = IpPrefix.valueOf(ipAddress, 0);
        assertThat(ipPrefix.toString(), is("0.0.0.0/0"));

        ipPrefix = IpPrefix.valueOf(ipAddress, 16);
        assertThat(ipPrefix.toString(), is("255.255.0.0/16"));

        ipPrefix = IpPrefix.valueOf(ipAddress, 32);
        assertThat(ipPrefix.toString(), is("255.255.255.255/32"));
    }

    /**
     * Tests valueOf() converter for IPv6 address.
     */
    @Test
    public void testValueOfAddressIPv6() {
        IpAddress ipAddress;
        IpPrefix ipPrefix;

        ipAddress =
            IpAddress.valueOf("1111:2222:3333:4444:5555:6666:7777:8888");
        ipPrefix = IpPrefix.valueOf(ipAddress, 120);
        assertThat(ipPrefix.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8800/120"));

        ipPrefix = IpPrefix.valueOf(ipAddress, 128);
        assertThat(ipPrefix.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8888/128"));

        ipAddress = IpAddress.valueOf("::");
        ipPrefix = IpPrefix.valueOf(ipAddress, 0);
        assertThat(ipPrefix.toString(), is("::/0"));

        ipPrefix = IpPrefix.valueOf(ipAddress, 128);
        assertThat(ipPrefix.toString(), is("::/128"));

        ipAddress =
            IpAddress.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff");
        ipPrefix = IpPrefix.valueOf(ipAddress, 0);
        assertThat(ipPrefix.toString(), is("::/0"));

        ipPrefix = IpPrefix.valueOf(ipAddress, 64);
        assertThat(ipPrefix.toString(), is("ffff:ffff:ffff:ffff::/64"));

        ipPrefix = IpPrefix.valueOf(ipAddress, 128);
        assertThat(ipPrefix.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128"));
    }

    /**
     * Tests invalid valueOf() converter for a null IP address.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidValueOfNullAddress() {
        IpAddress ipAddress;
        IpPrefix ipPrefix;

        ipAddress = null;
        ipPrefix = IpPrefix.valueOf(ipAddress, 24);
    }

    /**
     * Tests invalid valueOf() converter for IPv4 address and
     * negative prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfAddressNegativePrefixLengthIPv4() {
        IpAddress ipAddress;
        IpPrefix ipPrefix;

        ipAddress = IpAddress.valueOf("1.2.3.4");
        ipPrefix = IpPrefix.valueOf(ipAddress, -1);
    }

    /**
     * Tests invalid valueOf() converter for IPv6 address and
     * negative prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfAddressNegativePrefixLengthIPv6() {
        IpAddress ipAddress;
        IpPrefix ipPrefix;

        ipAddress =
            IpAddress.valueOf("1111:2222:3333:4444:5555:6666:7777:8888");
        ipPrefix = IpPrefix.valueOf(ipAddress, -1);
    }

    /**
     * Tests invalid valueOf() converter for IPv4 address and
     * too long prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfAddressTooLongPrefixLengthIPv4() {
        IpAddress ipAddress;
        IpPrefix ipPrefix;

        ipAddress = IpAddress.valueOf("1.2.3.4");
        ipPrefix = IpPrefix.valueOf(ipAddress, 33);
    }

    /**
     * Tests invalid valueOf() converter for IPv6 address and
     * too long prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfAddressTooLongPrefixLengthIPv6() {
        IpAddress ipAddress;
        IpPrefix ipPrefix;

        ipAddress =
            IpAddress.valueOf("1111:2222:3333:4444:5555:6666:7777:8888");
        ipPrefix = IpPrefix.valueOf(ipAddress, 129);
    }

    /**
     * Tests valueOf() converter for IPv4 string.
     */
    @Test
    public void testValueOfStringIPv4() {
        IpPrefix ipPrefix;

        ipPrefix = IpPrefix.valueOf("1.2.3.4/24");
        assertThat(ipPrefix.toString(), is("1.2.3.0/24"));

        ipPrefix = IpPrefix.valueOf("1.2.3.4/32");
        assertThat(ipPrefix.toString(), is("1.2.3.4/32"));

        ipPrefix = IpPrefix.valueOf("1.2.3.5/32");
        assertThat(ipPrefix.toString(), is("1.2.3.5/32"));

        ipPrefix = IpPrefix.valueOf("0.0.0.0/0");
        assertThat(ipPrefix.toString(), is("0.0.0.0/0"));

        ipPrefix = IpPrefix.valueOf("0.0.0.0/32");
        assertThat(ipPrefix.toString(), is("0.0.0.0/32"));

        ipPrefix = IpPrefix.valueOf("255.255.255.255/0");
        assertThat(ipPrefix.toString(), is("0.0.0.0/0"));

        ipPrefix = IpPrefix.valueOf("255.255.255.255/16");
        assertThat(ipPrefix.toString(), is("255.255.0.0/16"));

        ipPrefix = IpPrefix.valueOf("255.255.255.255/32");
        assertThat(ipPrefix.toString(), is("255.255.255.255/32"));
    }

    /**
     * Tests valueOf() converter for IPv6 string.
     */
    @Test
    public void testValueOfStringIPv6() {
        IpPrefix ipPrefix;

        ipPrefix =
            IpPrefix.valueOf("1111:2222:3333:4444:5555:6666:7777:8888/120");
        assertThat(ipPrefix.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8800/120"));

        ipPrefix =
            IpPrefix.valueOf("1111:2222:3333:4444:5555:6666:7777:8888/128");
        assertThat(ipPrefix.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8888/128"));

        ipPrefix = IpPrefix.valueOf("::/0");
        assertThat(ipPrefix.toString(), is("::/0"));

        ipPrefix = IpPrefix.valueOf("::/128");
        assertThat(ipPrefix.toString(), is("::/128"));

        ipPrefix =
            IpPrefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/0");
        assertThat(ipPrefix.toString(), is("::/0"));

        ipPrefix =
            IpPrefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/64");
        assertThat(ipPrefix.toString(), is("ffff:ffff:ffff:ffff::/64"));

        ipPrefix =
            IpPrefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128");
        assertThat(ipPrefix.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128"));
    }

    /**
     * Tests invalid valueOf() converter for a null string.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidValueOfNullString() {
        IpPrefix ipPrefix;
        String fromString;

        fromString = null;
        ipPrefix = IpPrefix.valueOf(fromString);
    }

    /**
     * Tests invalid valueOf() converter for an empty string.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfEmptyString() {
        IpPrefix ipPrefix;
        String fromString;

        fromString = "";
        ipPrefix = IpPrefix.valueOf(fromString);
    }

    /**
     * Tests invalid valueOf() converter for an incorrect string.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfIncorrectString() {
        IpPrefix ipPrefix;
        String fromString;

        fromString = "NoSuchIpPrefix";
        ipPrefix = IpPrefix.valueOf(fromString);
    }

    /**
     * Tests invalid valueOf() converter for IPv4 string and
     * negative prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfStringNegativePrefixLengthIPv4() {
        IpPrefix ipPrefix;

        ipPrefix = IpPrefix.valueOf("1.2.3.4/-1");
    }

    /**
     * Tests invalid valueOf() converter for IPv6 string and
     * negative prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfStringNegativePrefixLengthIPv6() {
        IpPrefix ipPrefix;

        ipPrefix =
            IpPrefix.valueOf("1111:2222:3333:4444:5555:6666:7777:8888/-1");
    }

    /**
     * Tests invalid valueOf() converter for IPv4 string and
     * too long prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfStringTooLongPrefixLengthIPv4() {
        IpPrefix ipPrefix;

        ipPrefix = IpPrefix.valueOf("1.2.3.4/33");
    }

    /**
     * Tests invalid valueOf() converter for IPv6 string and
     * too long prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfStringTooLongPrefixLengthIPv6() {
        IpPrefix ipPrefix;

        ipPrefix =
            IpPrefix.valueOf("1111:2222:3333:4444:5555:6666:7777:8888/129");
    }

    /**
     * Tests IP prefix contains another IP prefix for IPv4.
     */
    @Test
    public void testContainsIpPrefixIPv4() {
        IpPrefix ipPrefix;

        ipPrefix = IpPrefix.valueOf("1.2.0.0/24");
        assertTrue(ipPrefix.contains(IpPrefix.valueOf("1.2.0.0/24")));
        assertTrue(ipPrefix.contains(IpPrefix.valueOf("1.2.0.0/32")));
        assertTrue(ipPrefix.contains(IpPrefix.valueOf("1.2.0.4/32")));
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("1.2.0.0/16")));
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("1.3.0.0/24")));
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("0.0.0.0/16")));
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("0.0.0.0/0")));
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("255.255.255.255/32")));

        ipPrefix = IpPrefix.valueOf("1.2.0.0/32");
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("1.2.0.0/24")));
        assertTrue(ipPrefix.contains(IpPrefix.valueOf("1.2.0.0/32")));
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("1.2.0.4/32")));
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("1.2.0.0/16")));
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("1.3.0.0/24")));
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("0.0.0.0/16")));
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("0.0.0.0/0")));
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("255.255.255.255/32")));

        ipPrefix = IpPrefix.valueOf("0.0.0.0/0");
        assertTrue(ipPrefix.contains(IpPrefix.valueOf("1.2.0.0/24")));
        assertTrue(ipPrefix.contains(IpPrefix.valueOf("1.2.0.0/32")));
        assertTrue(ipPrefix.contains(IpPrefix.valueOf("1.2.0.4/32")));
        assertTrue(ipPrefix.contains(IpPrefix.valueOf("1.2.0.0/16")));
        assertTrue(ipPrefix.contains(IpPrefix.valueOf("1.3.0.0/24")));
        assertTrue(ipPrefix.contains(IpPrefix.valueOf("0.0.0.0/16")));
        assertTrue(ipPrefix.contains(IpPrefix.valueOf("0.0.0.0/0")));
        assertTrue(ipPrefix.contains(IpPrefix.valueOf("255.255.255.255/32")));

        ipPrefix = IpPrefix.valueOf("255.255.255.255/32");
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("1.2.0.0/24")));
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("1.2.0.0/32")));
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("1.2.0.4/32")));
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("1.2.0.0/16")));
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("1.3.0.0/24")));
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("0.0.0.0/16")));
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("0.0.0.0/0")));
        assertTrue(ipPrefix.contains(IpPrefix.valueOf("255.255.255.255/32")));

        // Test when there is a mistmatch in the compared IP address families
        ipPrefix = IpPrefix.valueOf("0.0.0.0/0");
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("1111:2222:3333:4444::/120")));
        ipPrefix = IpPrefix.valueOf("255.255.255.255/32");
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128")));
    }

    /**
     * Tests IP prefix contains another IP prefix for IPv6.
     */
    @Test
    public void testContainsIpPrefixIPv6() {
        IpPrefix ipPrefix;

        ipPrefix = IpPrefix.valueOf("1111:2222:3333:4444::/120");
        assertTrue(ipPrefix.contains(
                IpPrefix.valueOf("1111:2222:3333:4444::/120")));
        assertTrue(ipPrefix.contains(
                IpPrefix.valueOf("1111:2222:3333:4444::/128")));
        assertTrue(ipPrefix.contains(
                IpPrefix.valueOf("1111:2222:3333:4444::1/128")));
        assertFalse(ipPrefix.contains(
                IpPrefix.valueOf("1111:2222:3333:4444::/64")));
        assertFalse(ipPrefix.contains(
                IpPrefix.valueOf("1111:2222:3333:4445::/120")));
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("::/64")));
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("::/0")));
        assertFalse(ipPrefix.contains(
                IpPrefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128")));

        ipPrefix = IpPrefix.valueOf("1111:2222:3333:4444::/128");
        assertFalse(ipPrefix.contains(
                IpPrefix.valueOf("1111:2222:3333:4444::/120")));
        assertTrue(ipPrefix.contains(
                IpPrefix.valueOf("1111:2222:3333:4444::/128")));
        assertFalse(ipPrefix.contains(
                IpPrefix.valueOf("1111:2222:3333:4444::1/128")));
        assertFalse(ipPrefix.contains(
                IpPrefix.valueOf("1111:2222:3333:4444::/64")));
        assertFalse(ipPrefix.contains(
                IpPrefix.valueOf("1111:2222:3333:4445::/120")));
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("::/64")));
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("::/0")));
        assertFalse(ipPrefix.contains(
                IpPrefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128")));

        ipPrefix = IpPrefix.valueOf("::/0");
        assertTrue(ipPrefix.contains(
                IpPrefix.valueOf("1111:2222:3333:4444::/120")));
        assertTrue(ipPrefix.contains(
                IpPrefix.valueOf("1111:2222:3333:4444::/128")));
        assertTrue(ipPrefix.contains(
                IpPrefix.valueOf("1111:2222:3333:4444::1/128")));
        assertTrue(ipPrefix.contains(
                IpPrefix.valueOf("1111:2222:3333:4444::/64")));
        assertTrue(ipPrefix.contains(
                IpPrefix.valueOf("1111:2222:3333:4445::/120")));
        assertTrue(ipPrefix.contains(IpPrefix.valueOf("::/64")));
        assertTrue(ipPrefix.contains(IpPrefix.valueOf("::/0")));
        assertTrue(ipPrefix.contains(
                IpPrefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128")));

        ipPrefix =
            IpPrefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128");
        assertFalse(ipPrefix.contains(
                IpPrefix.valueOf("1111:2222:3333:4444::/120")));
        assertFalse(ipPrefix.contains(
                IpPrefix.valueOf("1111:2222:3333:4444::/128")));
        assertFalse(ipPrefix.contains(
                IpPrefix.valueOf("1111:2222:3333:4444::1/128")));
        assertFalse(ipPrefix.contains(
                IpPrefix.valueOf("1111:2222:3333:4444::/64")));
        assertFalse(ipPrefix.contains(
                IpPrefix.valueOf("1111:2222:3333:4445::/120")));
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("::/64")));
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("::/0")));
        assertTrue(ipPrefix.contains(
                IpPrefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128")));

        // Test when there is a mistmatch in the compared IP address families
        ipPrefix = IpPrefix.valueOf("::/0");
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("1.2.0.0/24")));
        ipPrefix = IpPrefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128");
        assertFalse(ipPrefix.contains(IpPrefix.valueOf("255.255.255.255/32")));
    }

    /**
     * Tests IP prefix contains IP address for IPv4.
     */
    @Test
    public void testContainsIpAddressIPv4() {
        IpPrefix ipPrefix;

        ipPrefix = IpPrefix.valueOf("1.2.0.0/24");
        assertTrue(ipPrefix.contains(IpAddress.valueOf("1.2.0.0")));
        assertTrue(ipPrefix.contains(IpAddress.valueOf("1.2.0.4")));
        assertFalse(ipPrefix.contains(IpAddress.valueOf("1.3.0.0")));
        assertFalse(ipPrefix.contains(IpAddress.valueOf("0.0.0.0")));
        assertFalse(ipPrefix.contains(IpAddress.valueOf("255.255.255.255")));

        ipPrefix = IpPrefix.valueOf("1.2.0.0/32");
        assertTrue(ipPrefix.contains(IpAddress.valueOf("1.2.0.0")));
        assertFalse(ipPrefix.contains(IpAddress.valueOf("1.2.0.4")));
        assertFalse(ipPrefix.contains(IpAddress.valueOf("1.3.0.0")));
        assertFalse(ipPrefix.contains(IpAddress.valueOf("0.0.0.0")));
        assertFalse(ipPrefix.contains(IpAddress.valueOf("255.255.255.255")));

        ipPrefix = IpPrefix.valueOf("0.0.0.0/0");
        assertTrue(ipPrefix.contains(IpAddress.valueOf("1.2.0.0")));
        assertTrue(ipPrefix.contains(IpAddress.valueOf("1.2.0.4")));
        assertTrue(ipPrefix.contains(IpAddress.valueOf("1.3.0.0")));
        assertTrue(ipPrefix.contains(IpAddress.valueOf("0.0.0.0")));
        assertTrue(ipPrefix.contains(IpAddress.valueOf("255.255.255.255")));

        ipPrefix = IpPrefix.valueOf("255.255.255.255/32");
        assertFalse(ipPrefix.contains(IpAddress.valueOf("1.2.0.0")));
        assertFalse(ipPrefix.contains(IpAddress.valueOf("1.2.0.4")));
        assertFalse(ipPrefix.contains(IpAddress.valueOf("1.3.0.0")));
        assertFalse(ipPrefix.contains(IpAddress.valueOf("0.0.0.0")));
        assertTrue(ipPrefix.contains(IpAddress.valueOf("255.255.255.255")));

        // Test when there is a mistmatch in the compared IP address families
        ipPrefix = IpPrefix.valueOf("0.0.0.0/0");
        assertFalse(ipPrefix.contains(IpAddress.valueOf("1111:2222:3333:4444::")));
        ipPrefix = IpPrefix.valueOf("255.255.255.255/32");
        assertFalse(ipPrefix.contains(IpAddress.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")));
    }

    /**
     * Tests IP prefix contains IP address for IPv6.
     */
    @Test
    public void testContainsIpAddressIPv6() {
        IpPrefix ipPrefix;

        ipPrefix = IpPrefix.valueOf("1111:2222:3333:4444::/120");
        assertTrue(ipPrefix.contains(
                IpAddress.valueOf("1111:2222:3333:4444::")));
        assertTrue(ipPrefix.contains(
                IpAddress.valueOf("1111:2222:3333:4444::1")));
        assertFalse(ipPrefix.contains(
                IpAddress.valueOf("1111:2222:3333:4445::")));
        assertFalse(ipPrefix.contains(IpAddress.valueOf("::")));
        assertFalse(ipPrefix.contains(
                IpAddress.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")));

        ipPrefix = IpPrefix.valueOf("1111:2222:3333:4444::/128");
        assertTrue(ipPrefix.contains(
                IpAddress.valueOf("1111:2222:3333:4444::")));
        assertFalse(ipPrefix.contains(
                IpAddress.valueOf("1111:2222:3333:4444::1")));
        assertFalse(ipPrefix.contains(
                IpAddress.valueOf("1111:2222:3333:4445::")));
        assertFalse(ipPrefix.contains(IpAddress.valueOf("::")));
        assertFalse(ipPrefix.contains(
                IpAddress.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")));

        ipPrefix = IpPrefix.valueOf("::/0");
        assertTrue(ipPrefix.contains(
                IpAddress.valueOf("1111:2222:3333:4444::")));
        assertTrue(ipPrefix.contains(
                IpAddress.valueOf("1111:2222:3333:4444::1")));
        assertTrue(ipPrefix.contains(
                IpAddress.valueOf("1111:2222:3333:4445::")));
        assertTrue(ipPrefix.contains(IpAddress.valueOf("::")));
        assertTrue(ipPrefix.contains(
                IpAddress.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")));

        ipPrefix =
            IpPrefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128");
        assertFalse(ipPrefix.contains(
                IpAddress.valueOf("1111:2222:3333:4444::")));
        assertFalse(ipPrefix.contains(
                IpAddress.valueOf("1111:2222:3333:4444::1")));
        assertFalse(ipPrefix.contains(
                IpAddress.valueOf("1111:2222:3333:4445::")));
        assertFalse(ipPrefix.contains(IpAddress.valueOf("::")));
        assertTrue(ipPrefix.contains(
                IpAddress.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")));

        // Test when there is a mistmatch in the compared IP address families
        ipPrefix = IpPrefix.valueOf("::/0");
        assertFalse(ipPrefix.contains(IpAddress.valueOf("1.2.0.0")));
        ipPrefix = IpPrefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128");
        assertFalse(ipPrefix.contains(IpAddress.valueOf("255.255.255.255")));
    }

    /**
     * Tests equality of {@link IpPrefix} for IPv4.
     */
    @Test
    public void testEqualityIPv4() {
        new EqualsTester()
            .addEqualityGroup(IpPrefix.valueOf("1.2.0.0/24"),
                              IpPrefix.valueOf("1.2.0.0/24"),
                              IpPrefix.valueOf("1.2.0.4/24"))
            .addEqualityGroup(IpPrefix.valueOf("1.2.0.0/16"),
                              IpPrefix.valueOf("1.2.0.0/16"))
            .addEqualityGroup(IpPrefix.valueOf("1.2.0.0/32"),
                              IpPrefix.valueOf("1.2.0.0/32"))
            .addEqualityGroup(IpPrefix.valueOf("1.3.0.0/24"),
                              IpPrefix.valueOf("1.3.0.0/24"))
            .addEqualityGroup(IpPrefix.valueOf("0.0.0.0/0"),
                              IpPrefix.valueOf("0.0.0.0/0"))
            .addEqualityGroup(IpPrefix.valueOf("255.255.255.255/32"),
                              IpPrefix.valueOf("255.255.255.255/32"))
            .testEquals();
    }

    /**
     * Tests equality of {@link IpPrefix} for IPv6.
     */
    @Test
    public void testEqualityIPv6() {
        new EqualsTester()
            .addEqualityGroup(
                IpPrefix.valueOf("1111:2222:3333:4444::/120"),
                IpPrefix.valueOf("1111:2222:3333:4444::1/120"),
                IpPrefix.valueOf("1111:2222:3333:4444::/120"))
            .addEqualityGroup(
                IpPrefix.valueOf("1111:2222:3333:4444::/64"),
                IpPrefix.valueOf("1111:2222:3333:4444::/64"))
            .addEqualityGroup(
                IpPrefix.valueOf("1111:2222:3333:4444::/128"),
                IpPrefix.valueOf("1111:2222:3333:4444::/128"))
            .addEqualityGroup(
                IpPrefix.valueOf("1111:2222:3333:4445::/64"),
                IpPrefix.valueOf("1111:2222:3333:4445::/64"))
            .addEqualityGroup(
                IpPrefix.valueOf("::/0"),
                IpPrefix.valueOf("::/0"))
            .addEqualityGroup(
                IpPrefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128"),
                IpPrefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128"))
            .testEquals();
    }

    /**
     * Tests object string representation for IPv4.
     */
    @Test
    public void testToStringIPv4() {
        IpPrefix ipPrefix;

        ipPrefix = IpPrefix.valueOf("1.2.3.0/24");
        assertThat(ipPrefix.toString(), is("1.2.3.0/24"));

        ipPrefix = IpPrefix.valueOf("1.2.3.4/24");
        assertThat(ipPrefix.toString(), is("1.2.3.0/24"));

        ipPrefix = IpPrefix.valueOf("0.0.0.0/0");
        assertThat(ipPrefix.toString(), is("0.0.0.0/0"));

        ipPrefix = IpPrefix.valueOf("255.255.255.255/32");
        assertThat(ipPrefix.toString(), is("255.255.255.255/32"));
    }

    /**
     * Tests object string representation for IPv6.
     */
    @Test
    public void testToStringIPv6() {
        IpPrefix ipPrefix;

        ipPrefix = IpPrefix.valueOf("1100::/8");
        assertThat(ipPrefix.toString(), is("1100::/8"));

        ipPrefix = IpPrefix.valueOf("1111:2222:3333:4444:5555:6666:7777:8885/8");
        assertThat(ipPrefix.toString(), is("1100::/8"));

        ipPrefix = IpPrefix.valueOf("::/0");
        assertThat(ipPrefix.toString(), is("::/0"));

        ipPrefix = IpPrefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128");
        assertThat(ipPrefix.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128"));
    }
}
