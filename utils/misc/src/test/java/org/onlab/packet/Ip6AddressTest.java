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
package org.onlab.packet;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Tests for class {@link Ip6Address}.
 */
public class Ip6AddressTest {
    /**
     * Tests the immutability of {@link Ip6Address}.
     */
    @Test
    public void testImmutable() {
        assertThatClassIsImmutable(Ip6Address.class);
    }

    /**
     * Tests the length of the address in bytes (octets).
     */
    @Test
    public void testAddrBytelen() {
        assertThat(Ip6Address.BYTE_LENGTH, is(16));
    }

    /**
     * Tests the length of the address in bits.
     */
    @Test
    public void testAddrBitlen() {
        assertThat(Ip6Address.BIT_LENGTH, is(128));
    }

    /**
     * Tests default class constructor.
     */
    @Test
    public void testDefaultConstructor() {
        Ip6Address ip6Address = new Ip6Address();
        assertThat(ip6Address.toString(), is("::"));
    }

    /**
     * Tests valid class copy constructor.
     */
    @Test
    public void testCopyConstructor() {
        Ip6Address fromAddr =
            new Ip6Address("1111:2222:3333:4444:5555:6666:7777:8888");
        Ip6Address ip6Address = new Ip6Address(fromAddr);
        assertThat(ip6Address.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8888"));

        fromAddr = new Ip6Address("::");
        ip6Address = new Ip6Address(fromAddr);
        assertThat(ip6Address.toString(), is("::"));

        fromAddr = new Ip6Address("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff");
        ip6Address = new Ip6Address(fromAddr);
        assertThat(ip6Address.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));
    }

    /**
     * Tests invalid class copy constructor for a null object to copy from.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidConstructorNullObject() {
        Ip6Address fromAddr = null;
        Ip6Address ip6Address = new Ip6Address(fromAddr);
    }

    /**
     * Tests valid class constructor for integer values.
     */
    @Test
    public void testConstructorForInteger() {
        Ip6Address ip6Address =
            new Ip6Address(0x1111222233334444L, 0x5555666677778888L);
        assertThat(ip6Address.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8888"));

        ip6Address = new Ip6Address(0L, 0L);
        assertThat(ip6Address.toString(), is("::"));

        ip6Address = new Ip6Address(-1L, -1L);
        assertThat(ip6Address.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));
    }

    /**
     * Tests valid class constructor for an array value.
     */
    @Test
    public void testConstructorForArray() {
        final byte[] value1 = new byte[] {0x11, 0x11, 0x22, 0x22,
                                          0x33, 0x33, 0x44, 0x44,
                                          0x55, 0x55, 0x66, 0x66,
                                          0x77, 0x77,
                                          (byte) 0x88, (byte) 0x88};
        Ip6Address ip6Address = new Ip6Address(value1);
        assertThat(ip6Address.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8888"));

        final byte[] value2 = new byte[] {0x00, 0x00, 0x00, 0x00,
                                          0x00, 0x00, 0x00, 0x00,
                                          0x00, 0x00, 0x00, 0x00,
                                          0x00, 0x00, 0x00, 0x00};
        ip6Address = new Ip6Address(value2);
        assertThat(ip6Address.toString(), is("::"));

        final byte[] value3 = new byte[] {(byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff};
        ip6Address = new Ip6Address(value3);
        assertThat(ip6Address.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));
    }

    /**
     * Tests valid class constructor for an array value and an offset.
     */
    @Test
    public void testConstructorForArrayAndOffset() {
        final byte[] value1 = new byte[] {11, 22, 33,           // Preamble
                                          0x11, 0x11, 0x22, 0x22,
                                          0x33, 0x33, 0x44, 0x44,
                                          0x55, 0x55, 0x66, 0x66,
                                          0x77, 0x77,
                                          (byte) 0x88, (byte) 0x88,
                                          44, 55};              // Extra bytes
        Ip6Address ip6Address = new Ip6Address(value1, 3);
        assertThat(ip6Address.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8888"));

        final byte[] value2 = new byte[] {11, 22,               // Preamble
                                          0x00, 0x00, 0x00, 0x00,
                                          0x00, 0x00, 0x00, 0x00,
                                          0x00, 0x00, 0x00, 0x00,
                                          0x00, 0x00, 0x00, 0x00,
                                          33};                  // Extra bytes
        ip6Address = new Ip6Address(value2, 2);
        assertThat(ip6Address.toString(), is("::"));

        final byte[] value3 = new byte[] {11, 22,               // Preamble
                                          (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff,
                                          33};                  // Extra bytes
        ip6Address = new Ip6Address(value3, 2);
        assertThat(ip6Address.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));
    }

    /**
     * Tests invalid class constructor for a null array.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidConstructorNullArray() {
        final byte[] fromArray = null;
        Ip6Address ip6Address = new Ip6Address(fromArray);
    }

    /**
     * Tests invalid class constructor for an array that is too short.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConstructorShortArray() {
        final byte[] fromArray = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9};
        Ip6Address ip6Address = new Ip6Address(fromArray);
    }

    /**
     * Tests invalid class constructor for an array and an invalid offset.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConstructorArrayInvalidOffset() {
        final byte[] value1 = new byte[] {11, 22, 33,           // Preamble
                                          0x11, 0x11, 0x22, 0x22,
                                          0x33, 0x33, 0x44, 0x44,
                                          0x55, 0x55, 0x66, 0x66,
                                          0x77, 0x77,
                                          (byte) 0x88, (byte) 0x88,
                                          44, 55};              // Extra bytes
        Ip6Address ip6Address = new Ip6Address(value1, 6);
    }

    /**
     * Tests valid class constructor for a string.
     */
    @Test
    public void testConstructorForString() {
        Ip6Address ip6Address =
            new Ip6Address("1111:2222:3333:4444:5555:6666:7777:8888");
        assertThat(ip6Address.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8888"));

        ip6Address = new Ip6Address("::");
        assertThat(ip6Address.toString(), is("::"));

        ip6Address = new Ip6Address("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff");
        assertThat(ip6Address.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));
    }

    /**
     * Tests invalid class constructor for a null string.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidConstructorNullString() {
        String fromString = null;
        Ip6Address ip6Address = new Ip6Address(fromString);
    }

    /**
     * Tests invalid class constructor for an empty string.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConstructors() {
        // Check constructor for invalid ID: empty string
        Ip6Address ip6Address = new Ip6Address("");
    }

    /**
     * Tests returning the address as a byte array.
     */
    @Test
    public void testAddressToOctets() {
        final byte[] value1 = new byte[] {0x11, 0x11, 0x22, 0x22,
                                          0x33, 0x33, 0x44, 0x44,
                                          0x55, 0x55, 0x66, 0x66,
                                          0x77, 0x77,
                                          (byte) 0x88, (byte) 0x88};
        Ip6Address ip6Address =
            new Ip6Address("1111:2222:3333:4444:5555:6666:7777:8888");
        assertThat(ip6Address.toOctets(), is(value1));

        final byte[] value2 = new byte[] {0x00, 0x00, 0x00, 0x00,
                                          0x00, 0x00, 0x00, 0x00,
                                          0x00, 0x00, 0x00, 0x00,
                                          0x00, 0x00, 0x00, 0x00};
        ip6Address = new Ip6Address("::");
        assertThat(ip6Address.toOctets(), is(value2));

        final byte[] value3 = new byte[] {(byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff};
        ip6Address = new Ip6Address("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff");
        assertThat(ip6Address.toOctets(), is(value3));
    }

    /**
     * Tests making a mask prefix for a given prefix length.
     */
    @Test
    public void testMakeMaskPrefix() {
        Ip6Address ip6Address = Ip6Address.makeMaskPrefix(8);
        assertThat(ip6Address.toString(), is("ff00::"));

        ip6Address = Ip6Address.makeMaskPrefix(120);
        assertThat(ip6Address.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ff00"));

        ip6Address = Ip6Address.makeMaskPrefix(0);
        assertThat(ip6Address.toString(), is("::"));

        ip6Address = Ip6Address.makeMaskPrefix(128);
        assertThat(ip6Address.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));

        ip6Address = Ip6Address.makeMaskPrefix(64);
        assertThat(ip6Address.toString(), is("ffff:ffff:ffff:ffff::"));
    }

    /**
     * Tests making of a masked address.
     */
    @Test
    public void testMakeMaskedAddress() {
        Ip6Address ip6Address =
            new Ip6Address("1111:2222:3333:4444:5555:6666:7777:8885");
        Ip6Address ip6AddressMasked =
            Ip6Address.makeMaskedAddress(ip6Address, 8);
        assertThat(ip6AddressMasked.toString(), is("1100::"));

        ip6AddressMasked = Ip6Address.makeMaskedAddress(ip6Address, 120);
        assertThat(ip6AddressMasked.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8800"));

        ip6AddressMasked = Ip6Address.makeMaskedAddress(ip6Address, 0);
        assertThat(ip6AddressMasked.toString(), is("::"));

        ip6AddressMasked = Ip6Address.makeMaskedAddress(ip6Address, 128);
        assertThat(ip6AddressMasked.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8885"));

        ip6AddressMasked = Ip6Address.makeMaskedAddress(ip6Address, 64);
        assertThat(ip6AddressMasked.toString(), is("1111:2222:3333:4444::"));
    }

    /**
     * Tests getting the value of an address.
     */
    @Test
    public void testGetValue() {
        Ip6Address ip6Address =
            new Ip6Address("1111:2222:3333:4444:5555:6666:7777:8888");
        assertThat(ip6Address.getValueHigh(), is(0x1111222233334444L));
        assertThat(ip6Address.getValueLow(), is(0x5555666677778888L));

        ip6Address = new Ip6Address(0, 0);
        assertThat(ip6Address.getValueHigh(), is(0L));
        assertThat(ip6Address.getValueLow(), is(0L));

        ip6Address = new Ip6Address(-1L, -1L);
        assertThat(ip6Address.getValueHigh(), is(-1L));
        assertThat(ip6Address.getValueLow(), is(-1L));
    }

    /**
     * Tests equality of {@link Ip6Address}.
     */
    @Test
    public void testEquality() {
        Ip6Address addr1 =
            new Ip6Address("1111:2222:3333:4444:5555:6666:7777:8888");
        Ip6Address addr2 =
            new Ip6Address("1111:2222:3333:4444:5555:6666:7777:8888");
        assertThat(addr1, is(addr2));

        addr1 = new Ip6Address("::");
        addr2 = new Ip6Address("::");
        assertThat(addr1, is(addr2));

        addr1 = new Ip6Address("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff");
        addr2 = new Ip6Address("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff");
        assertThat(addr1, is(addr2));
    }

    /**
     * Tests non-equality of {@link Ip6Address}.
     */
    @Test
    public void testNonEquality() {
        Ip6Address addr1 =
            new Ip6Address("1111:2222:3333:4444:5555:6666:7777:8888");
        Ip6Address addr2 =
            new Ip6Address("1111:2222:3333:4444:5555:6666:7777:888A");
        Ip6Address addr3 = new Ip6Address("::");
        Ip6Address addr4 =
            new Ip6Address("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff");
        assertThat(addr1, is(not(addr2)));
        assertThat(addr3, is(not(addr2)));
        assertThat(addr4, is(not(addr2)));
    }

    /**
     * Tests comparison of {@link Ip6Address}.
     */
    @Test
    public void testComparison() {
        Ip6Address addr1 =
            new Ip6Address("1111:2222:3333:4444:5555:6666:7777:8888");
        Ip6Address addr2 =
            new Ip6Address("1111:2222:3333:4444:5555:6666:7777:8888");
        Ip6Address addr3 =
            new Ip6Address("1111:2222:3333:4444:5555:6666:7777:8887");
        Ip6Address addr4 =
            new Ip6Address("1111:2222:3333:4444:5555:6666:7777:8889");
        assertTrue(addr1.compareTo(addr2) == 0);
        assertTrue(addr1.compareTo(addr3) > 0);
        assertTrue(addr1.compareTo(addr4) < 0);

        addr1 = new Ip6Address("ffff:2222:3333:4444:5555:6666:7777:8888");
        addr2 = new Ip6Address("ffff:2222:3333:4444:5555:6666:7777:8888");
        addr3 = new Ip6Address("ffff:2222:3333:4444:5555:6666:7777:8887");
        addr4 = new Ip6Address("ffff:2222:3333:4444:5555:6666:7777:8889");
        assertTrue(addr1.compareTo(addr2) == 0);
        assertTrue(addr1.compareTo(addr3) > 0);
        assertTrue(addr1.compareTo(addr4) < 0);

        addr1 = new Ip6Address("ffff:2222:3333:4444:5555:6666:7777:8888");
        addr2 = new Ip6Address("ffff:2222:3333:4444:5555:6666:7777:8888");
        addr3 = new Ip6Address("ffff:2222:3333:4443:5555:6666:7777:8888");
        addr4 = new Ip6Address("ffff:2222:3333:4445:5555:6666:7777:8888");
        assertTrue(addr1.compareTo(addr2) == 0);
        assertTrue(addr1.compareTo(addr3) > 0);
        assertTrue(addr1.compareTo(addr4) < 0);
    }

    /**
     * Tests object string representation.
     */
    @Test
    public void testToString() {
        Ip6Address ip6Address =
            new Ip6Address("1111:2222:3333:4444:5555:6666:7777:8888");
        assertThat(ip6Address.toString(),
                   is("1111:2222:3333:4444:5555:6666:7777:8888"));

        ip6Address = new Ip6Address("1111::8888");
        assertThat(ip6Address.toString(), is("1111::8888"));

        ip6Address = new Ip6Address("1111::");
        assertThat(ip6Address.toString(), is("1111::"));

        ip6Address = new Ip6Address("::8888");
        assertThat(ip6Address.toString(), is("::8888"));

        ip6Address = new Ip6Address("::");
        assertThat(ip6Address.toString(), is("::"));

        ip6Address = new Ip6Address("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff");
        assertThat(ip6Address.toString(),
                   is("ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff"));
    }
}
