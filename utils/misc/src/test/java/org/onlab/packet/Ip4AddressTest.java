/*
 * Copyright 2014-present Open Networking Laboratory
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

import com.google.common.net.InetAddresses;
import com.google.common.testing.EqualsTester;
import org.junit.Test;

import java.net.InetAddress;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Tests for class {@link Ip4Address}.
 */
public class Ip4AddressTest {
    /**
     * Tests the immutability of {@link Ip4Address}.
     */
    @Test
    public void testImmutable() {
        assertThatClassIsImmutable(Ip4Address.class);
    }

    /**
     * Tests the IPv4 address version constant.
     */
    @Test
    public void testAddressVersion() {
        assertThat(Ip4Address.VERSION, is(IpAddress.Version.INET));
    }

    /**
     * Tests the length of the address in bytes (octets).
     */
    @Test
    public void testAddrByteLength() {
        assertThat(Ip4Address.BYTE_LENGTH, is(4));
    }

    /**
     * Tests the length of the address in bits.
     */
    @Test
    public void testAddrBitLength() {
        assertThat(Ip4Address.BIT_LENGTH, is(32));
    }

    /**
     * Tests returning the IP address version.
     */
    @Test
    public void testVersion() {
        Ip4Address ipAddress;

        // IPv4
        ipAddress = Ip4Address.valueOf("0.0.0.0");
        assertThat(ipAddress.version(), is(IpAddress.Version.INET));
    }

    /**
     * Tests returning an IPv4 address as a byte array.
     */
    @Test
    public void testAddressToOctetsIPv4() {
        Ip4Address ipAddress;
        byte[] value;

        value = new byte[] {1, 2, 3, 4};
        ipAddress = Ip4Address.valueOf("1.2.3.4");
        assertThat(ipAddress.toOctets(), is(value));

        value = new byte[] {0, 0, 0, 0};
        ipAddress = Ip4Address.valueOf("0.0.0.0");
        assertThat(ipAddress.toOctets(), is(value));

        value = new byte[] {(byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff};
        ipAddress = Ip4Address.valueOf("255.255.255.255");
        assertThat(ipAddress.toOctets(), is(value));
    }

    /**
     * Tests returning an IPv4 address as an integer.
     */
    @Test
    public void testToInt() {
        Ip4Address ipAddress;

        ipAddress = Ip4Address.valueOf("1.2.3.4");
        assertThat(ipAddress.toInt(), is(0x01020304));

        ipAddress = Ip4Address.valueOf("0.0.0.0");
        assertThat(ipAddress.toInt(), is(0));

        ipAddress = Ip4Address.valueOf("255.255.255.255");
        assertThat(ipAddress.toInt(), is(-1));
    }

    /**
     * Tests valueOf() converter for IPv4 integer value.
     */
    @Test
    public void testValueOfForIntegerIPv4() {
        Ip4Address ipAddress;

        ipAddress = Ip4Address.valueOf(0x01020304);
        assertThat(ipAddress.toString(), is("1.2.3.4"));

        ipAddress = Ip4Address.valueOf(0);
        assertThat(ipAddress.toString(), is("0.0.0.0"));

        ipAddress = Ip4Address.valueOf(0xffffffff);
        assertThat(ipAddress.toString(), is("255.255.255.255"));
    }

    /**
     * Tests valueOf() converter for IPv4 byte array.
     */
    @Test
    public void testValueOfByteArrayIPv4() {
        Ip4Address ipAddress;
        byte[] value;

        value = new byte[] {1, 2, 3, 4};
        ipAddress = Ip4Address.valueOf(value);
        assertThat(ipAddress.toString(), is("1.2.3.4"));

        value = new byte[] {0, 0, 0, 0};
        ipAddress = Ip4Address.valueOf(value);
        assertThat(ipAddress.toString(), is("0.0.0.0"));

        value = new byte[] {(byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff};
        ipAddress = Ip4Address.valueOf(value);
        assertThat(ipAddress.toString(), is("255.255.255.255"));
    }

    /**
     * Tests invalid valueOf() converter for a null array for IPv4.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidValueOfNullArrayIPv4() {
        Ip4Address ipAddress;
        byte[] value;

        value = null;
        ipAddress = Ip4Address.valueOf(value);
    }

    /**
     * Tests invalid valueOf() converger for an array that is too short for
     * IPv4.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfShortArrayIPv4() {
        Ip4Address ipAddress;
        byte[] value;

        value = new byte[] {1, 2, 3};
        ipAddress = Ip4Address.valueOf(value);
    }

    /**
     * Tests valueOf() converter for IPv4 byte array and an offset.
     */
    @Test
    public void testValueOfByteArrayOffsetIPv4() {
        Ip4Address ipAddress;
        byte[] value;

        value = new byte[] {11, 22, 33,                 // Preamble
                            1, 2, 3, 4,
                            44, 55};                    // Extra bytes
        ipAddress = Ip4Address.valueOf(value, 3);
        assertThat(ipAddress.toString(), is("1.2.3.4"));

        value = new byte[] {11, 22,                     // Preamble
                            0, 0, 0, 0,
                            33};                        // Extra bytes
        ipAddress = Ip4Address.valueOf(value, 2);
        assertThat(ipAddress.toString(), is("0.0.0.0"));

        value = new byte[] {11, 22,                     // Preamble
                            (byte) 0xff, (byte) 0xff,
                            (byte) 0xff, (byte) 0xff,
                            33};                        // Extra bytes
        ipAddress = Ip4Address.valueOf(value, 2);
        assertThat(ipAddress.toString(), is("255.255.255.255"));
    }

    /**
     * Tests invalid valueOf() converger for an array and an invalid offset
     * for IPv4.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfArrayInvalidOffsetIPv4() {
        Ip4Address ipAddress;
        byte[] value;

        value = new byte[] {11, 22, 33,                 // Preamble
                            1, 2, 3, 4,
                            44, 55};                    // Extra bytes
        ipAddress = Ip4Address.valueOf(value, 6);
    }

    /**
     * Tests valueOf() converter for IPv4 InetAddress.
     */
    @Test
    public void testValueOfInetAddressIPv4() {
        Ip4Address ipAddress;
        InetAddress inetAddress;

        inetAddress = InetAddresses.forString("1.2.3.4");
        ipAddress = Ip4Address.valueOf(inetAddress);
        assertThat(ipAddress.toString(), is("1.2.3.4"));

        inetAddress = InetAddresses.forString("0.0.0.0");
        ipAddress = Ip4Address.valueOf(inetAddress);
        assertThat(ipAddress.toString(), is("0.0.0.0"));

        inetAddress = InetAddresses.forString("255.255.255.255");
        ipAddress = Ip4Address.valueOf(inetAddress);
        assertThat(ipAddress.toString(), is("255.255.255.255"));
    }

    /**
     * Tests valueOf() converter for IPv4 string.
     */
    @Test
    public void testValueOfStringIPv4() {
        Ip4Address ipAddress;

        ipAddress = Ip4Address.valueOf("1.2.3.4");
        assertThat(ipAddress.toString(), is("1.2.3.4"));

        ipAddress = Ip4Address.valueOf("0.0.0.0");
        assertThat(ipAddress.toString(), is("0.0.0.0"));

        ipAddress = Ip4Address.valueOf("255.255.255.255");
        assertThat(ipAddress.toString(), is("255.255.255.255"));
    }

    /**
     * Tests invalid valueOf() converter for a null string.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidValueOfNullString() {
        Ip4Address ipAddress;

        String fromString = null;
        ipAddress = Ip4Address.valueOf(fromString);
    }

    /**
     * Tests invalid valueOf() converter for an empty string.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfEmptyString() {
        Ip4Address ipAddress;

        String fromString = "";
        ipAddress = Ip4Address.valueOf(fromString);
    }

    /**
     * Tests invalid valueOf() converter for an incorrect string.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidValueOfIncorrectString() {
        Ip4Address ipAddress;

        String fromString = "NoSuchIpAddress";
        ipAddress = Ip4Address.valueOf(fromString);
    }

    /**
     * Tests making a mask prefix for a given prefix length for IPv4.
     */
    @Test
    public void testMakeMaskPrefixIPv4() {
        Ip4Address ipAddress;

        ipAddress = Ip4Address.makeMaskPrefix(25);
        assertThat(ipAddress.toString(), is("255.255.255.128"));

        ipAddress = Ip4Address.makeMaskPrefix(0);
        assertThat(ipAddress.toString(), is("0.0.0.0"));

        ipAddress = Ip4Address.makeMaskPrefix(32);
        assertThat(ipAddress.toString(), is("255.255.255.255"));
    }

    /**
     * Tests making a mask prefix for an invalid prefix length for IPv4:
     * negative prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMakeNegativeMaskPrefixIPv4() {
        Ip4Address ipAddress;

        ipAddress = Ip4Address.makeMaskPrefix(-1);
    }

    /**
     * Tests making a mask prefix for an invalid prefix length for IPv4:
     * too long prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMakeTooLongMaskPrefixIPv4() {
        Ip4Address ipAddress;

        ipAddress = Ip4Address.makeMaskPrefix(33);
    }

    /**
     * Tests making of a masked address for IPv4.
     */
    @Test
    public void testMakeMaskedAddressIPv4() {
        Ip4Address ipAddress = Ip4Address.valueOf("1.2.3.5");
        Ip4Address ipAddressMasked;

        ipAddressMasked = Ip4Address.makeMaskedAddress(ipAddress, 24);
        assertThat(ipAddressMasked.toString(), is("1.2.3.0"));

        ipAddressMasked = Ip4Address.makeMaskedAddress(ipAddress, 0);
        assertThat(ipAddressMasked.toString(), is("0.0.0.0"));

        ipAddressMasked = Ip4Address.makeMaskedAddress(ipAddress, 32);
        assertThat(ipAddressMasked.toString(), is("1.2.3.5"));
    }

    /**
     * Tests making of a masked address for invalid prefix length for IPv4:
     * negative prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMakeNegativeMaskedAddressIPv4() {
        Ip4Address ipAddress = Ip4Address.valueOf("1.2.3.5");
        Ip4Address ipAddressMasked;

        ipAddressMasked = Ip4Address.makeMaskedAddress(ipAddress, -1);
    }

    /**
     * Tests making of a masked address for an invalid prefix length for IPv4:
     * too long prefix length.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidMakeTooLongMaskedAddressIPv4() {
        Ip4Address ipAddress = Ip4Address.valueOf("1.2.3.5");
        Ip4Address ipAddressMasked;

        ipAddressMasked = Ip4Address.makeMaskedAddress(ipAddress, 33);
    }

    /**
     * Tests comparison of {@link Ip4Address} for IPv4.
     */
    @Test
    public void testComparisonIPv4() {
        Ip4Address addr1, addr2, addr3, addr4;

        addr1 = Ip4Address.valueOf("1.2.3.4");
        addr2 = Ip4Address.valueOf("1.2.3.4");
        addr3 = Ip4Address.valueOf("1.2.3.3");
        addr4 = Ip4Address.valueOf("1.2.3.5");
        assertTrue(addr1.compareTo(addr2) == 0);
        assertTrue(addr1.compareTo(addr3) > 0);
        assertTrue(addr1.compareTo(addr4) < 0);

        addr1 = Ip4Address.valueOf("255.2.3.4");
        addr2 = Ip4Address.valueOf("255.2.3.4");
        addr3 = Ip4Address.valueOf("255.2.3.3");
        addr4 = Ip4Address.valueOf("255.2.3.5");
        assertTrue(addr1.compareTo(addr2) == 0);
        assertTrue(addr1.compareTo(addr3) > 0);
        assertTrue(addr1.compareTo(addr4) < 0);
    }

    /**
     * Tests equality of {@link Ip4Address} for IPv4.
     */
    @Test
    public void testEqualityIPv4() {
        new EqualsTester()
            .addEqualityGroup(Ip4Address.valueOf("1.2.3.4"),
                              Ip4Address.valueOf("1.2.3.4"))
            .addEqualityGroup(Ip4Address.valueOf("1.2.3.5"),
                              Ip4Address.valueOf("1.2.3.5"))
            .addEqualityGroup(Ip4Address.valueOf("0.0.0.0"),
                              Ip4Address.valueOf("0.0.0.0"))
            .addEqualityGroup(Ip4Address.valueOf("255.255.255.255"),
                              Ip4Address.valueOf("255.255.255.255"))
            .testEquals();
    }

    /**
     * Tests object string representation for IPv4.
     */
    @Test
    public void testToStringIPv4() {
        Ip4Address ipAddress;

        ipAddress = Ip4Address.valueOf("1.2.3.4");
        assertThat(ipAddress.toString(), is("1.2.3.4"));

        ipAddress = Ip4Address.valueOf("0.0.0.0");
        assertThat(ipAddress.toString(), is("0.0.0.0"));

        ipAddress = Ip4Address.valueOf("255.255.255.255");
        assertThat(ipAddress.toString(), is("255.255.255.255"));
    }
}
