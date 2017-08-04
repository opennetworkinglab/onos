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

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Tests for class {@link Ip6Prefix}.
 */
public class Ip6PrefixTest {
    /**
     * Tests the immutability of {@link Ip6Prefix}.
     */
    @Test
    public void testImmutable() {
        assertThatClassIsImmutable(Ip6Prefix.class);
    }

    /**
     * Tests the IPv4 prefix address version constant.
     */
    @Test
    public void testAddressVersion() {
        assertThat(Ip6Prefix.VERSION, is(IpAddress.Version.INET6));
    }

    /**
     * Tests the maximum mask length.
     */
    @Test
    public void testMaxMaskLength() {
        assertThat(Ip6Prefix.MAX_MASK_LENGTH, is(128));
    }

    /**
     * Tests returning the IP version of the prefix.
     */
    @Test
    public void testVersion() {
        Ip6Prefix ipPrefix;

        // IPv6
        ipPrefix = Ip6Prefix.valueOf("::/0");
        assertThat(ipPrefix.version(), is(IpAddress.Version.INET6));
    }

    /**
     * Tests returning the IP address value and IP address prefix length of
     * an IPv6 prefix.
     */
    @Test
    public void testAddressAndPrefixLengthIPv6() {
        Ip6Prefix ipPrefix;

        ipPrefix = Ip6Prefix.valueOf("1100::/8");
        assertThat(ipPrefix.address(), equalTo(Ip6Address.valueOf("1100::")));
        assertThat(ipPrefix.prefixLength(), is(8));

        ipPrefix =
            Ip6Prefix.valueOf("1111:2222:3333:4444:5555:6666:7777:8885/8");
        assertThat(ipPrefix.address(), equalTo(Ip6Address.valueOf("1100::")));
        assertThat(ipPrefix.prefixLength(), is(8));

        ipPrefix =
            Ip6Prefix.valueOf("1111:2222:3333:4444:5555:6666:7777:8800/120");
        assertThat(ipPrefix.address(),
                   equalTo(Ip6Address.valueOf("1111:2222:3333:4444:5555:6666:7777:8800")));
        assertThat(ipPrefix.prefixLength(), is(120));

        ipPrefix =
            Ip6Prefix.valueOf("1111:2222:3333:4444:5555:6666:7777:8885/128");
        assertThat(ipPrefix.address(),
                   equalTo(Ip6Address.valueOf("1111:2222:3333:4444:5555:6666:7777:8885")));
        assertThat(ipPrefix.prefixLength(), is(128));

        ipPrefix = Ip6Prefix.valueOf("::/0");
        assertThat(ipPrefix.address(), equalTo(Ip6Address.valueOf("::")));
        assertThat(ipPrefix.prefixLength(), is(0));

        ipPrefix =
            Ip6Prefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128");
        assertThat(ipPrefix.address(),
                   equalTo(Ip6Address.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")));
        assertThat(ipPrefix.prefixLength(), is(128));

        ipPrefix =
            Ip6Prefix.valueOf("1111:2222:3333:4444:5555:6666:7777:8885/64");
        assertThat(ipPrefix.address(),
                   equalTo(Ip6Address.valueOf("1111:2222:3333:4444::")));
        assertThat(ipPrefix.prefixLength(), is(64));
    }

    /**
     * Tests valueOf() converter for IPv6 byte array.
     */
    @Test
    public void testValueOfByteArrayIPv6() {
        Ip6Prefix ipPrefix;
        byte[] value;

        value = new byte[] {0x11, 0x11, 0x22, 0x22,
                            0x33, 0x33, 0x44, 0x44,
                            0x55, 0x55, 0x66, 0x66,
                            0x77, 0x77, (byte) 0x88, (byte) 0x88};
        ipPrefix = Ip6Prefix.valueOf(value, 120);
        assertThat(ipPrefix.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8800/120"));

        ipPrefix = Ip6Prefix.valueOf(value, 128);
        assertThat(ipPrefix.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8888/128"));

        value = new byte[] {0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00,
                            0x00, 0x00, 0x00, 0x00};
        ipPrefix = Ip6Prefix.valueOf(value, 0);
        assertThat(ipPrefix.toString(), is("::/0"));

        ipPrefix = Ip6Prefix.valueOf(value, 128);
        assertThat(ipPrefix.toString(), is("::/128"));

        value = new byte[] {(byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff};
        ipPrefix = Ip6Prefix.valueOf(value, 0);
        assertThat(ipPrefix.toString(), is("::/0"));

        ipPrefix = Ip6Prefix.valueOf(value, 64);
        assertThat(ipPrefix.toString(), is("ffff:ffff:ffff:ffff::/64"));

        ipPrefix = Ip6Prefix.valueOf(value, 128);
        assertThat(ipPrefix.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128"));
    }

    /**
     * Tests invalid valueOf() converter for a null array for IPv6.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidValueOfNullArrayIPv6() {
        Ip6Prefix ipPrefix;
        byte[] value;

        value = null;
        ipPrefix = Ip6Prefix.valueOf(value, 120);
    }

    /**
     * Tests invalid valueOf() converter for a short array for IPv6.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfShortArrayIPv6() {
        Ip6Prefix ipPrefix;
        byte[] value;

        value = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9};
        ipPrefix = Ip6Prefix.valueOf(value, 120);
    }

    /**
     * Tests invalid valueOf() converter for IPv6 byte array and
     * negative prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfByteArrayNegativePrefixLengthIPv6() {
        Ip6Prefix ipPrefix;
        byte[] value;

        value = new byte[] {0x11, 0x11, 0x22, 0x22,
                            0x33, 0x33, 0x44, 0x44,
                            0x55, 0x55, 0x66, 0x66,
                            0x77, 0x77, (byte) 0x88, (byte) 0x88};
        ipPrefix = Ip6Prefix.valueOf(value, -1);
    }

    /**
     * Tests invalid valueOf() converter for IPv6 byte array and
     * too long prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfByteArrayTooLongPrefixLengthIPv6() {
        Ip6Prefix ipPrefix;
        byte[] value;

        value = new byte[] {0x11, 0x11, 0x22, 0x22,
                            0x33, 0x33, 0x44, 0x44,
                            0x55, 0x55, 0x66, 0x66,
                            0x77, 0x77, (byte) 0x88, (byte) 0x88};
        ipPrefix = Ip6Prefix.valueOf(value, 129);
    }

    /**
     * Tests valueOf() converter for IPv6 address.
     */
    @Test
    public void testValueOfAddressIPv6() {
        Ip6Address ipAddress;
        Ip6Prefix ipPrefix;

        ipAddress =
            Ip6Address.valueOf("1111:2222:3333:4444:5555:6666:7777:8888");
        ipPrefix = Ip6Prefix.valueOf(ipAddress, 120);
        assertThat(ipPrefix.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8800/120"));

        ipPrefix = Ip6Prefix.valueOf(ipAddress, 128);
        assertThat(ipPrefix.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8888/128"));

        ipAddress = Ip6Address.valueOf("::");
        ipPrefix = Ip6Prefix.valueOf(ipAddress, 0);
        assertThat(ipPrefix.toString(), is("::/0"));

        ipPrefix = Ip6Prefix.valueOf(ipAddress, 128);
        assertThat(ipPrefix.toString(), is("::/128"));

        ipAddress =
            Ip6Address.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff");
        ipPrefix = Ip6Prefix.valueOf(ipAddress, 0);
        assertThat(ipPrefix.toString(), is("::/0"));

        ipPrefix = Ip6Prefix.valueOf(ipAddress, 64);
        assertThat(ipPrefix.toString(), is("ffff:ffff:ffff:ffff::/64"));

        ipPrefix = Ip6Prefix.valueOf(ipAddress, 128);
        assertThat(ipPrefix.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128"));
    }

    /**
     * Tests invalid valueOf() converter for a null IP address.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidValueOfNullAddress() {
        Ip6Address ipAddress;
        Ip6Prefix ipPrefix;

        ipAddress = null;
        ipPrefix = Ip6Prefix.valueOf(ipAddress, 24);
    }

    /**
     * Tests invalid valueOf() converter for IPv6 address and
     * negative prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfAddressNegativePrefixLengthIPv6() {
        Ip6Address ipAddress;
        Ip6Prefix ipPrefix;

        ipAddress =
            Ip6Address.valueOf("1111:2222:3333:4444:5555:6666:7777:8888");
        ipPrefix = Ip6Prefix.valueOf(ipAddress, -1);
    }

    /**
     * Tests invalid valueOf() converter for IPv6 address and
     * too long prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfAddressTooLongPrefixLengthIPv6() {
        Ip6Address ipAddress;
        Ip6Prefix ipPrefix;

        ipAddress =
            Ip6Address.valueOf("1111:2222:3333:4444:5555:6666:7777:8888");
        ipPrefix = Ip6Prefix.valueOf(ipAddress, 129);
    }

    /**
     * Tests valueOf() converter for IPv6 string.
     */
    @Test
    public void testValueOfStringIPv6() {
        Ip6Prefix ipPrefix;

        ipPrefix =
            Ip6Prefix.valueOf("1111:2222:3333:4444:5555:6666:7777:8888/120");
        assertThat(ipPrefix.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8800/120"));

        ipPrefix =
            Ip6Prefix.valueOf("1111:2222:3333:4444:5555:6666:7777:8888/128");
        assertThat(ipPrefix.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8888/128"));

        ipPrefix = Ip6Prefix.valueOf("::/0");
        assertThat(ipPrefix.toString(), is("::/0"));

        ipPrefix = Ip6Prefix.valueOf("::/128");
        assertThat(ipPrefix.toString(), is("::/128"));

        ipPrefix =
            Ip6Prefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/0");
        assertThat(ipPrefix.toString(), is("::/0"));

        ipPrefix =
            Ip6Prefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/64");
        assertThat(ipPrefix.toString(), is("ffff:ffff:ffff:ffff::/64"));

        ipPrefix =
            Ip6Prefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128");
        assertThat(ipPrefix.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128"));
    }

    /**
     * Tests invalid valueOf() converter for a null string.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidValueOfNullString() {
        Ip6Prefix ipPrefix;
        String fromString;

        fromString = null;
        ipPrefix = Ip6Prefix.valueOf(fromString);
    }

    /**
     * Tests invalid valueOf() converter for an empty string.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfEmptyString() {
        Ip6Prefix ipPrefix;
        String fromString;

        fromString = "";
        ipPrefix = Ip6Prefix.valueOf(fromString);
    }

    /**
     * Tests invalid valueOf() converter for an incorrect string.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfIncorrectString() {
        Ip6Prefix ipPrefix;
        String fromString;

        fromString = "NoSuchIpPrefix";
        ipPrefix = Ip6Prefix.valueOf(fromString);
    }

    /**
     * Tests invalid valueOf() converter for IPv6 string and
     * negative prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfStringNegativePrefixLengthIPv6() {
        Ip6Prefix ipPrefix;

        ipPrefix =
            Ip6Prefix.valueOf("1111:2222:3333:4444:5555:6666:7777:8888/-1");
    }

    /**
     * Tests invalid valueOf() converter for IPv6 string and
     * too long prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfStringTooLongPrefixLengthIPv6() {
        Ip6Prefix ipPrefix;

        ipPrefix =
            Ip6Prefix.valueOf("1111:2222:3333:4444:5555:6666:7777:8888/129");
    }

    /**
     * Tests IP prefix contains another IP prefix for IPv6.
     */
    @Test
    public void testContainsIpPrefixIPv6() {
        Ip6Prefix ipPrefix;

        ipPrefix = Ip6Prefix.valueOf("1111:2222:3333:4444::/120");
        assertTrue(ipPrefix.contains(
                Ip6Prefix.valueOf("1111:2222:3333:4444::/120")));
        assertTrue(ipPrefix.contains(
                Ip6Prefix.valueOf("1111:2222:3333:4444::/128")));
        assertTrue(ipPrefix.contains(
                Ip6Prefix.valueOf("1111:2222:3333:4444::1/128")));
        assertFalse(ipPrefix.contains(
                Ip6Prefix.valueOf("1111:2222:3333:4444::/64")));
        assertFalse(ipPrefix.contains(
                Ip6Prefix.valueOf("1111:2222:3333:4445::/120")));
        assertFalse(ipPrefix.contains(Ip6Prefix.valueOf("::/64")));
        assertFalse(ipPrefix.contains(Ip6Prefix.valueOf("::/0")));
        assertFalse(ipPrefix.contains(
                Ip6Prefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128")));

        ipPrefix = Ip6Prefix.valueOf("1111:2222:3333:4444::/128");
        assertFalse(ipPrefix.contains(
                Ip6Prefix.valueOf("1111:2222:3333:4444::/120")));
        assertTrue(ipPrefix.contains(
                Ip6Prefix.valueOf("1111:2222:3333:4444::/128")));
        assertFalse(ipPrefix.contains(
                Ip6Prefix.valueOf("1111:2222:3333:4444::1/128")));
        assertFalse(ipPrefix.contains(
                Ip6Prefix.valueOf("1111:2222:3333:4444::/64")));
        assertFalse(ipPrefix.contains(
                Ip6Prefix.valueOf("1111:2222:3333:4445::/120")));
        assertFalse(ipPrefix.contains(Ip6Prefix.valueOf("::/64")));
        assertFalse(ipPrefix.contains(Ip6Prefix.valueOf("::/0")));
        assertFalse(ipPrefix.contains(
                Ip6Prefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128")));

        ipPrefix = Ip6Prefix.valueOf("::/0");
        assertTrue(ipPrefix.contains(
                Ip6Prefix.valueOf("1111:2222:3333:4444::/120")));
        assertTrue(ipPrefix.contains(
                Ip6Prefix.valueOf("1111:2222:3333:4444::/128")));
        assertTrue(ipPrefix.contains(
                Ip6Prefix.valueOf("1111:2222:3333:4444::1/128")));
        assertTrue(ipPrefix.contains(
                Ip6Prefix.valueOf("1111:2222:3333:4444::/64")));
        assertTrue(ipPrefix.contains(
                Ip6Prefix.valueOf("1111:2222:3333:4445::/120")));
        assertTrue(ipPrefix.contains(Ip6Prefix.valueOf("::/64")));
        assertTrue(ipPrefix.contains(Ip6Prefix.valueOf("::/0")));
        assertTrue(ipPrefix.contains(
                Ip6Prefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128")));

        ipPrefix =
            Ip6Prefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128");
        assertFalse(ipPrefix.contains(
                Ip6Prefix.valueOf("1111:2222:3333:4444::/120")));
        assertFalse(ipPrefix.contains(
                Ip6Prefix.valueOf("1111:2222:3333:4444::/128")));
        assertFalse(ipPrefix.contains(
                Ip6Prefix.valueOf("1111:2222:3333:4444::1/128")));
        assertFalse(ipPrefix.contains(
                Ip6Prefix.valueOf("1111:2222:3333:4444::/64")));
        assertFalse(ipPrefix.contains(
                Ip6Prefix.valueOf("1111:2222:3333:4445::/120")));
        assertFalse(ipPrefix.contains(Ip6Prefix.valueOf("::/64")));
        assertFalse(ipPrefix.contains(Ip6Prefix.valueOf("::/0")));
        assertTrue(ipPrefix.contains(
                Ip6Prefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128")));
    }

    /**
     * Tests IP prefix contains IP address for IPv6.
     */
    @Test
    public void testContainsIpAddressIPv6() {
        Ip6Prefix ipPrefix;

        ipPrefix = Ip6Prefix.valueOf("1111:2222:3333:4444::/120");
        assertTrue(ipPrefix.contains(
                Ip6Address.valueOf("1111:2222:3333:4444::")));
        assertTrue(ipPrefix.contains(
                Ip6Address.valueOf("1111:2222:3333:4444::1")));
        assertFalse(ipPrefix.contains(
                Ip6Address.valueOf("1111:2222:3333:4445::")));
        assertFalse(ipPrefix.contains(Ip6Address.valueOf("::")));
        assertFalse(ipPrefix.contains(
                Ip6Address.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")));

        ipPrefix = Ip6Prefix.valueOf("1111:2222:3333:4444::/128");
        assertTrue(ipPrefix.contains(
                Ip6Address.valueOf("1111:2222:3333:4444::")));
        assertFalse(ipPrefix.contains(
                Ip6Address.valueOf("1111:2222:3333:4444::1")));
        assertFalse(ipPrefix.contains(
                Ip6Address.valueOf("1111:2222:3333:4445::")));
        assertFalse(ipPrefix.contains(Ip6Address.valueOf("::")));
        assertFalse(ipPrefix.contains(
                Ip6Address.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")));

        ipPrefix = Ip6Prefix.valueOf("::/0");
        assertTrue(ipPrefix.contains(
                Ip6Address.valueOf("1111:2222:3333:4444::")));
        assertTrue(ipPrefix.contains(
                Ip6Address.valueOf("1111:2222:3333:4444::1")));
        assertTrue(ipPrefix.contains(
                Ip6Address.valueOf("1111:2222:3333:4445::")));
        assertTrue(ipPrefix.contains(Ip6Address.valueOf("::")));
        assertTrue(ipPrefix.contains(
                Ip6Address.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")));

        ipPrefix =
            Ip6Prefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128");
        assertFalse(ipPrefix.contains(
                Ip6Address.valueOf("1111:2222:3333:4444::")));
        assertFalse(ipPrefix.contains(
                Ip6Address.valueOf("1111:2222:3333:4444::1")));
        assertFalse(ipPrefix.contains(
                Ip6Address.valueOf("1111:2222:3333:4445::")));
        assertFalse(ipPrefix.contains(Ip6Address.valueOf("::")));
        assertTrue(ipPrefix.contains(
                Ip6Address.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff")));
    }

    /**
     * Tests equality of {@link Ip6Prefix} for IPv6.
     */
    @Test
    public void testEqualityIPv6() {
        new EqualsTester()
            .addEqualityGroup(
                Ip6Prefix.valueOf("1111:2222:3333:4444::/120"),
                Ip6Prefix.valueOf("1111:2222:3333:4444::1/120"),
                Ip6Prefix.valueOf("1111:2222:3333:4444::/120"))
            .addEqualityGroup(
                Ip6Prefix.valueOf("1111:2222:3333:4444::/64"),
                Ip6Prefix.valueOf("1111:2222:3333:4444::/64"))
            .addEqualityGroup(
                Ip6Prefix.valueOf("1111:2222:3333:4444::/128"),
                Ip6Prefix.valueOf("1111:2222:3333:4444::/128"))
            .addEqualityGroup(
                Ip6Prefix.valueOf("1111:2222:3333:4445::/64"),
                Ip6Prefix.valueOf("1111:2222:3333:4445::/64"))
            .addEqualityGroup(
                Ip6Prefix.valueOf("::/0"),
                Ip6Prefix.valueOf("::/0"))
            .addEqualityGroup(
                Ip6Prefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128"),
                Ip6Prefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128"))
            .testEquals();
    }

    /**
     * Tests object string representation for IPv6.
     */
    @Test
    public void testToStringIPv6() {
        Ip6Prefix ipPrefix;

        ipPrefix = Ip6Prefix.valueOf("1100::/8");
        assertThat(ipPrefix.toString(), is("1100::/8"));

        ipPrefix = Ip6Prefix.valueOf("1111:2222:3333:4444:5555:6666:7777:8885/8");
        assertThat(ipPrefix.toString(), is("1100::/8"));

        ipPrefix = Ip6Prefix.valueOf("::/0");
        assertThat(ipPrefix.toString(), is("::/0"));

        ipPrefix = Ip6Prefix.valueOf("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128");
        assertThat(ipPrefix.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128"));
    }
}
