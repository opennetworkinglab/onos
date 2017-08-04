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
 * Tests for class {@link Ip4Prefix}.
 */
public class Ip4PrefixTest {
    /**
     * Tests the immutability of {@link Ip4Prefix}.
     */
    @Test
    public void testImmutable() {
        assertThatClassIsImmutable(Ip4Prefix.class);
    }

    /**
     * Tests the IPv4 prefix address version constant.
     */
    @Test
    public void testAddressVersion() {
        assertThat(Ip4Prefix.VERSION, is(IpAddress.Version.INET));
    }

    /**
     * Tests the maximum mask length.
     */
    @Test
    public void testMaxMaskLength() {
        assertThat(Ip4Prefix.MAX_MASK_LENGTH, is(32));
    }

    /**
     * Tests returning the IP version of the prefix.
     */
    @Test
    public void testVersion() {
        Ip4Prefix ipPrefix;

        // IPv4
        ipPrefix = Ip4Prefix.valueOf("0.0.0.0/0");
        assertThat(ipPrefix.version(), is(IpAddress.Version.INET));
    }

    /**
     * Tests returning the IP address value and IP address prefix length of
     * an IPv4 prefix.
     */
    @Test
    public void testAddressAndPrefixLengthIPv4() {
        Ip4Prefix ipPrefix;

        ipPrefix = Ip4Prefix.valueOf("1.2.3.0/24");
        assertThat(ipPrefix.address(), equalTo(Ip4Address.valueOf("1.2.3.0")));
        assertThat(ipPrefix.prefixLength(), is(24));

        ipPrefix = Ip4Prefix.valueOf("1.2.3.4/24");
        assertThat(ipPrefix.address(), equalTo(Ip4Address.valueOf("1.2.3.0")));
        assertThat(ipPrefix.prefixLength(), is(24));

        ipPrefix = Ip4Prefix.valueOf("1.2.3.4/32");
        assertThat(ipPrefix.address(), equalTo(Ip4Address.valueOf("1.2.3.4")));
        assertThat(ipPrefix.prefixLength(), is(32));

        ipPrefix = Ip4Prefix.valueOf("1.2.3.5/32");
        assertThat(ipPrefix.address(), equalTo(Ip4Address.valueOf("1.2.3.5")));
        assertThat(ipPrefix.prefixLength(), is(32));

        ipPrefix = Ip4Prefix.valueOf("0.0.0.0/0");
        assertThat(ipPrefix.address(), equalTo(Ip4Address.valueOf("0.0.0.0")));
        assertThat(ipPrefix.prefixLength(), is(0));

        ipPrefix = Ip4Prefix.valueOf("255.255.255.255/32");
        assertThat(ipPrefix.address(),
                   equalTo(Ip4Address.valueOf("255.255.255.255")));
        assertThat(ipPrefix.prefixLength(), is(32));
    }

    /**
     * Tests valueOf() converter for IPv4 integer value.
     */
    @Test
    public void testValueOfForIntegerIPv4() {
        Ip4Prefix ipPrefix;

        ipPrefix = Ip4Prefix.valueOf(0x01020304, 24);
        assertThat(ipPrefix.toString(), is("1.2.3.0/24"));

        ipPrefix = Ip4Prefix.valueOf(0x01020304, 32);
        assertThat(ipPrefix.toString(), is("1.2.3.4/32"));

        ipPrefix = Ip4Prefix.valueOf(0x01020305, 32);
        assertThat(ipPrefix.toString(), is("1.2.3.5/32"));

        ipPrefix = Ip4Prefix.valueOf(0, 0);
        assertThat(ipPrefix.toString(), is("0.0.0.0/0"));

        ipPrefix = Ip4Prefix.valueOf(0, 32);
        assertThat(ipPrefix.toString(), is("0.0.0.0/32"));

        ipPrefix = Ip4Prefix.valueOf(0xffffffff, 0);
        assertThat(ipPrefix.toString(), is("0.0.0.0/0"));

        ipPrefix = Ip4Prefix.valueOf(0xffffffff, 16);
        assertThat(ipPrefix.toString(), is("255.255.0.0/16"));

        ipPrefix = Ip4Prefix.valueOf(0xffffffff, 32);
        assertThat(ipPrefix.toString(), is("255.255.255.255/32"));
    }

    /**
     * Tests invalid valueOf() converter for IPv4 integer value and
     * negative prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfIntegerNegativePrefixLengthIPv4() {
        Ip4Prefix ipPrefix;

        ipPrefix = Ip4Prefix.valueOf(0x01020304, -1);
    }

    /**
     * Tests invalid valueOf() converter for IPv4 integer value and
     * too long prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfIntegerTooLongPrefixLengthIPv4() {
        Ip4Prefix ipPrefix;

        ipPrefix = Ip4Prefix.valueOf(0x01020304, 33);
    }

    /**
     * Tests valueOf() converter for IPv4 byte array.
     */
    @Test
    public void testValueOfByteArrayIPv4() {
        Ip4Prefix ipPrefix;
        byte[] value;

        value = new byte[] {1, 2, 3, 4};
        ipPrefix = Ip4Prefix.valueOf(value, 24);
        assertThat(ipPrefix.toString(), is("1.2.3.0/24"));

        ipPrefix = Ip4Prefix.valueOf(value, 32);
        assertThat(ipPrefix.toString(), is("1.2.3.4/32"));

        value = new byte[] {1, 2, 3, 5};
        ipPrefix = Ip4Prefix.valueOf(value, 32);
        assertThat(ipPrefix.toString(), is("1.2.3.5/32"));

        value = new byte[] {0, 0, 0, 0};
        ipPrefix = Ip4Prefix.valueOf(value, 0);
        assertThat(ipPrefix.toString(), is("0.0.0.0/0"));

        ipPrefix = Ip4Prefix.valueOf(value, 32);
        assertThat(ipPrefix.toString(), is("0.0.0.0/32"));

        value = new byte[] {(byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff};
        ipPrefix = Ip4Prefix.valueOf(value, 0);
        assertThat(ipPrefix.toString(), is("0.0.0.0/0"));

        ipPrefix = Ip4Prefix.valueOf(value, 16);
        assertThat(ipPrefix.toString(), is("255.255.0.0/16"));

        ipPrefix = Ip4Prefix.valueOf(value, 32);
        assertThat(ipPrefix.toString(), is("255.255.255.255/32"));
    }

    /**
     * Tests invalid valueOf() converter for a null array for IPv4.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidValueOfNullArrayIPv4() {
        Ip4Prefix ipPrefix;
        byte[] value;

        value = null;
        ipPrefix = Ip4Prefix.valueOf(value, 24);
    }

    /**
     * Tests invalid valueOf() converter for a short array for IPv4.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfShortArrayIPv4() {
        Ip4Prefix ipPrefix;
        byte[] value;

        value = new byte[] {1, 2, 3};
        ipPrefix = Ip4Prefix.valueOf(value, 24);
    }

    /**
     * Tests invalid valueOf() converter for IPv4 byte array and
     * negative prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfByteArrayNegativePrefixLengthIPv4() {
        Ip4Prefix ipPrefix;
        byte[] value;

        value = new byte[] {1, 2, 3, 4};
        ipPrefix = Ip4Prefix.valueOf(value, -1);
    }

    /**
     * Tests invalid valueOf() converter for IPv4 byte array and
     * too long prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfByteArrayTooLongPrefixLengthIPv4() {
        Ip4Prefix ipPrefix;
        byte[] value;

        value = new byte[] {1, 2, 3, 4};
        ipPrefix = Ip4Prefix.valueOf(value, 33);
    }

    /**
     * Tests valueOf() converter for IPv4 address.
     */
    @Test
    public void testValueOfAddressIPv4() {
        Ip4Address ipAddress;
        Ip4Prefix ipPrefix;

        ipAddress = Ip4Address.valueOf("1.2.3.4");
        ipPrefix = Ip4Prefix.valueOf(ipAddress, 24);
        assertThat(ipPrefix.toString(), is("1.2.3.0/24"));

        ipPrefix = Ip4Prefix.valueOf(ipAddress, 32);
        assertThat(ipPrefix.toString(), is("1.2.3.4/32"));

        ipAddress = Ip4Address.valueOf("1.2.3.5");
        ipPrefix = Ip4Prefix.valueOf(ipAddress, 32);
        assertThat(ipPrefix.toString(), is("1.2.3.5/32"));

        ipAddress = Ip4Address.valueOf("0.0.0.0");
        ipPrefix = Ip4Prefix.valueOf(ipAddress, 0);
        assertThat(ipPrefix.toString(), is("0.0.0.0/0"));

        ipPrefix = Ip4Prefix.valueOf(ipAddress, 32);
        assertThat(ipPrefix.toString(), is("0.0.0.0/32"));

        ipAddress = Ip4Address.valueOf("255.255.255.255");
        ipPrefix = Ip4Prefix.valueOf(ipAddress, 0);
        assertThat(ipPrefix.toString(), is("0.0.0.0/0"));

        ipPrefix = Ip4Prefix.valueOf(ipAddress, 16);
        assertThat(ipPrefix.toString(), is("255.255.0.0/16"));

        ipPrefix = Ip4Prefix.valueOf(ipAddress, 32);
        assertThat(ipPrefix.toString(), is("255.255.255.255/32"));
    }

    /**
     * Tests invalid valueOf() converter for a null IP address.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidValueOfNullAddress() {
        Ip4Address ipAddress;
        Ip4Prefix ipPrefix;

        ipAddress = null;
        ipPrefix = Ip4Prefix.valueOf(ipAddress, 24);
    }

    /**
     * Tests invalid valueOf() converter for IPv4 address and
     * negative prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfAddressNegativePrefixLengthIPv4() {
        Ip4Address ipAddress;
        Ip4Prefix ipPrefix;

        ipAddress = Ip4Address.valueOf("1.2.3.4");
        ipPrefix = Ip4Prefix.valueOf(ipAddress, -1);
    }

    /**
     * Tests invalid valueOf() converter for IPv4 address and
     * too long prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfAddressTooLongPrefixLengthIPv4() {
        Ip4Address ipAddress;
        Ip4Prefix ipPrefix;

        ipAddress = Ip4Address.valueOf("1.2.3.4");
        ipPrefix = Ip4Prefix.valueOf(ipAddress, 33);
    }

    /**
     * Tests valueOf() converter for IPv4 string.
     */
    @Test
    public void testValueOfStringIPv4() {
        Ip4Prefix ipPrefix;

        ipPrefix = Ip4Prefix.valueOf("1.2.3.4/24");
        assertThat(ipPrefix.toString(), is("1.2.3.0/24"));

        ipPrefix = Ip4Prefix.valueOf("1.2.3.4/32");
        assertThat(ipPrefix.toString(), is("1.2.3.4/32"));

        ipPrefix = Ip4Prefix.valueOf("1.2.3.5/32");
        assertThat(ipPrefix.toString(), is("1.2.3.5/32"));

        ipPrefix = Ip4Prefix.valueOf("0.0.0.0/0");
        assertThat(ipPrefix.toString(), is("0.0.0.0/0"));

        ipPrefix = Ip4Prefix.valueOf("0.0.0.0/32");
        assertThat(ipPrefix.toString(), is("0.0.0.0/32"));

        ipPrefix = Ip4Prefix.valueOf("255.255.255.255/0");
        assertThat(ipPrefix.toString(), is("0.0.0.0/0"));

        ipPrefix = Ip4Prefix.valueOf("255.255.255.255/16");
        assertThat(ipPrefix.toString(), is("255.255.0.0/16"));

        ipPrefix = Ip4Prefix.valueOf("255.255.255.255/32");
        assertThat(ipPrefix.toString(), is("255.255.255.255/32"));
    }

    /**
     * Tests invalid valueOf() converter for a null string.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidValueOfNullString() {
        Ip4Prefix ipPrefix;
        String fromString;

        fromString = null;
        ipPrefix = Ip4Prefix.valueOf(fromString);
    }

    /**
     * Tests invalid valueOf() converter for an empty string.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfEmptyString() {
        Ip4Prefix ipPrefix;
        String fromString;

        fromString = "";
        ipPrefix = Ip4Prefix.valueOf(fromString);
    }

    /**
     * Tests invalid valueOf() converter for an incorrect string.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfIncorrectString() {
        Ip4Prefix ipPrefix;
        String fromString;

        fromString = "NoSuchIpPrefix";
        ipPrefix = Ip4Prefix.valueOf(fromString);
    }

    /**
     * Tests invalid valueOf() converter for IPv4 string and
     * negative prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfStringNegativePrefixLengthIPv4() {
        Ip4Prefix ipPrefix;

        ipPrefix = Ip4Prefix.valueOf("1.2.3.4/-1");
    }

    /**
     * Tests invalid valueOf() converter for IPv4 string and
     * too long prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfStringTooLongPrefixLengthIPv4() {
        Ip4Prefix ipPrefix;

        ipPrefix = Ip4Prefix.valueOf("1.2.3.4/33");
    }

    /**
     * Tests IP prefix contains another IP prefix for IPv4.
     */
    @Test
    public void testContainsIpPrefixIPv4() {
        Ip4Prefix ipPrefix;

        ipPrefix = Ip4Prefix.valueOf("1.2.0.0/24");
        assertTrue(ipPrefix.contains(Ip4Prefix.valueOf("1.2.0.0/24")));
        assertTrue(ipPrefix.contains(Ip4Prefix.valueOf("1.2.0.0/32")));
        assertTrue(ipPrefix.contains(Ip4Prefix.valueOf("1.2.0.4/32")));
        assertFalse(ipPrefix.contains(Ip4Prefix.valueOf("1.2.0.0/16")));
        assertFalse(ipPrefix.contains(Ip4Prefix.valueOf("1.3.0.0/24")));
        assertFalse(ipPrefix.contains(Ip4Prefix.valueOf("0.0.0.0/16")));
        assertFalse(ipPrefix.contains(Ip4Prefix.valueOf("0.0.0.0/0")));
        assertFalse(ipPrefix.contains(Ip4Prefix.valueOf("255.255.255.255/32")));

        ipPrefix = Ip4Prefix.valueOf("1.2.0.0/32");
        assertFalse(ipPrefix.contains(Ip4Prefix.valueOf("1.2.0.0/24")));
        assertTrue(ipPrefix.contains(Ip4Prefix.valueOf("1.2.0.0/32")));
        assertFalse(ipPrefix.contains(Ip4Prefix.valueOf("1.2.0.4/32")));
        assertFalse(ipPrefix.contains(Ip4Prefix.valueOf("1.2.0.0/16")));
        assertFalse(ipPrefix.contains(Ip4Prefix.valueOf("1.3.0.0/24")));
        assertFalse(ipPrefix.contains(Ip4Prefix.valueOf("0.0.0.0/16")));
        assertFalse(ipPrefix.contains(Ip4Prefix.valueOf("0.0.0.0/0")));
        assertFalse(ipPrefix.contains(Ip4Prefix.valueOf("255.255.255.255/32")));

        ipPrefix = Ip4Prefix.valueOf("0.0.0.0/0");
        assertTrue(ipPrefix.contains(Ip4Prefix.valueOf("1.2.0.0/24")));
        assertTrue(ipPrefix.contains(Ip4Prefix.valueOf("1.2.0.0/32")));
        assertTrue(ipPrefix.contains(Ip4Prefix.valueOf("1.2.0.4/32")));
        assertTrue(ipPrefix.contains(Ip4Prefix.valueOf("1.2.0.0/16")));
        assertTrue(ipPrefix.contains(Ip4Prefix.valueOf("1.3.0.0/24")));
        assertTrue(ipPrefix.contains(Ip4Prefix.valueOf("0.0.0.0/16")));
        assertTrue(ipPrefix.contains(Ip4Prefix.valueOf("0.0.0.0/0")));
        assertTrue(ipPrefix.contains(Ip4Prefix.valueOf("255.255.255.255/32")));

        ipPrefix = Ip4Prefix.valueOf("255.255.255.255/32");
        assertFalse(ipPrefix.contains(Ip4Prefix.valueOf("1.2.0.0/24")));
        assertFalse(ipPrefix.contains(Ip4Prefix.valueOf("1.2.0.0/32")));
        assertFalse(ipPrefix.contains(Ip4Prefix.valueOf("1.2.0.4/32")));
        assertFalse(ipPrefix.contains(Ip4Prefix.valueOf("1.2.0.0/16")));
        assertFalse(ipPrefix.contains(Ip4Prefix.valueOf("1.3.0.0/24")));
        assertFalse(ipPrefix.contains(Ip4Prefix.valueOf("0.0.0.0/16")));
        assertFalse(ipPrefix.contains(Ip4Prefix.valueOf("0.0.0.0/0")));
        assertTrue(ipPrefix.contains(Ip4Prefix.valueOf("255.255.255.255/32")));
    }

    /**
     * Tests IP prefix contains IP address for IPv4.
     */
    @Test
    public void testContainsIpAddressIPv4() {
        Ip4Prefix ipPrefix;

        ipPrefix = Ip4Prefix.valueOf("1.2.0.0/24");
        assertTrue(ipPrefix.contains(Ip4Address.valueOf("1.2.0.0")));
        assertTrue(ipPrefix.contains(Ip4Address.valueOf("1.2.0.4")));
        assertFalse(ipPrefix.contains(Ip4Address.valueOf("1.3.0.0")));
        assertFalse(ipPrefix.contains(Ip4Address.valueOf("0.0.0.0")));
        assertFalse(ipPrefix.contains(Ip4Address.valueOf("255.255.255.255")));

        ipPrefix = Ip4Prefix.valueOf("1.2.0.0/32");
        assertTrue(ipPrefix.contains(Ip4Address.valueOf("1.2.0.0")));
        assertFalse(ipPrefix.contains(Ip4Address.valueOf("1.2.0.4")));
        assertFalse(ipPrefix.contains(Ip4Address.valueOf("1.3.0.0")));
        assertFalse(ipPrefix.contains(Ip4Address.valueOf("0.0.0.0")));
        assertFalse(ipPrefix.contains(Ip4Address.valueOf("255.255.255.255")));

        ipPrefix = Ip4Prefix.valueOf("0.0.0.0/0");
        assertTrue(ipPrefix.contains(Ip4Address.valueOf("1.2.0.0")));
        assertTrue(ipPrefix.contains(Ip4Address.valueOf("1.2.0.4")));
        assertTrue(ipPrefix.contains(Ip4Address.valueOf("1.3.0.0")));
        assertTrue(ipPrefix.contains(Ip4Address.valueOf("0.0.0.0")));
        assertTrue(ipPrefix.contains(Ip4Address.valueOf("255.255.255.255")));

        ipPrefix = Ip4Prefix.valueOf("255.255.255.255/32");
        assertFalse(ipPrefix.contains(Ip4Address.valueOf("1.2.0.0")));
        assertFalse(ipPrefix.contains(Ip4Address.valueOf("1.2.0.4")));
        assertFalse(ipPrefix.contains(Ip4Address.valueOf("1.3.0.0")));
        assertFalse(ipPrefix.contains(Ip4Address.valueOf("0.0.0.0")));
        assertTrue(ipPrefix.contains(Ip4Address.valueOf("255.255.255.255")));
    }

    /**
     * Tests equality of {@link Ip4Prefix} for IPv4.
     */
    @Test
    public void testEqualityIPv4() {
        new EqualsTester()
            .addEqualityGroup(Ip4Prefix.valueOf("1.2.0.0/24"),
                              Ip4Prefix.valueOf("1.2.0.0/24"),
                              Ip4Prefix.valueOf("1.2.0.4/24"))
            .addEqualityGroup(Ip4Prefix.valueOf("1.2.0.0/16"),
                              Ip4Prefix.valueOf("1.2.0.0/16"))
            .addEqualityGroup(Ip4Prefix.valueOf("1.2.0.0/32"),
                              Ip4Prefix.valueOf("1.2.0.0/32"))
            .addEqualityGroup(Ip4Prefix.valueOf("1.3.0.0/24"),
                              Ip4Prefix.valueOf("1.3.0.0/24"))
            .addEqualityGroup(Ip4Prefix.valueOf("0.0.0.0/0"),
                              Ip4Prefix.valueOf("0.0.0.0/0"))
            .addEqualityGroup(Ip4Prefix.valueOf("255.255.255.255/32"),
                              Ip4Prefix.valueOf("255.255.255.255/32"))
            .testEquals();
    }

    /**
     * Tests object string representation for IPv4.
     */
    @Test
    public void testToStringIPv4() {
        Ip4Prefix ipPrefix;

        ipPrefix = Ip4Prefix.valueOf("1.2.3.0/24");
        assertThat(ipPrefix.toString(), is("1.2.3.0/24"));

        ipPrefix = Ip4Prefix.valueOf("1.2.3.4/24");
        assertThat(ipPrefix.toString(), is("1.2.3.0/24"));

        ipPrefix = Ip4Prefix.valueOf("0.0.0.0/0");
        assertThat(ipPrefix.toString(), is("0.0.0.0/0"));

        ipPrefix = Ip4Prefix.valueOf("255.255.255.255/32");
        assertThat(ipPrefix.toString(), is("255.255.255.255/32"));
    }
}
