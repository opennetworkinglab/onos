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
     * Tests the length of the address in bytes (octets).
     */
    @Test
    public void testAddrBytelen() {
        assertThat(Ip4Address.BYTE_LENGTH, is(4));
    }

    /**
     * Tests the length of the address in bits.
     */
    @Test
    public void testAddrBitlen() {
        assertThat(Ip4Address.BIT_LENGTH, is(32));
    }

    /**
     * Tests default class constructor.
     */
    @Test
    public void testDefaultConstructor() {
        Ip4Address ip4Address = new Ip4Address();
        assertThat(ip4Address.toString(), is("0.0.0.0"));
    }

    /**
     * Tests valid class copy constructor.
     */
    @Test
    public void testCopyConstructor() {
        Ip4Address fromAddr = new Ip4Address("1.2.3.4");
        Ip4Address ip4Address = new Ip4Address(fromAddr);
        assertThat(ip4Address.toString(), is("1.2.3.4"));

        fromAddr = new Ip4Address("0.0.0.0");
        ip4Address = new Ip4Address(fromAddr);
        assertThat(ip4Address.toString(), is("0.0.0.0"));

        fromAddr = new Ip4Address("255.255.255.255");
        ip4Address = new Ip4Address(fromAddr);
        assertThat(ip4Address.toString(), is("255.255.255.255"));
    }

    /**
     * Tests invalid class copy constructor for a null object to copy from.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidConstructorNullObject() {
        Ip4Address fromAddr = null;
        Ip4Address ip4Address = new Ip4Address(fromAddr);
    }

    /**
     * Tests valid class constructor for an integer value.
     */
    @Test
    public void testConstructorForInteger() {
        Ip4Address ip4Address = new Ip4Address(0x01020304);
        assertThat(ip4Address.toString(), is("1.2.3.4"));

        ip4Address = new Ip4Address(0);
        assertThat(ip4Address.toString(), is("0.0.0.0"));

        ip4Address = new Ip4Address(0xffffffff);
        assertThat(ip4Address.toString(), is("255.255.255.255"));
    }

    /**
     * Tests valid class constructor for an array value.
     */
    @Test
    public void testConstructorForArray() {
        final byte[] value1 = new byte[] {1, 2, 3, 4};
        Ip4Address ip4Address = new Ip4Address(value1);
        assertThat(ip4Address.toString(), is("1.2.3.4"));

        final byte[] value2 = new byte[] {0, 0, 0, 0};
        ip4Address = new Ip4Address(value2);
        assertThat(ip4Address.toString(), is("0.0.0.0"));

        final byte[] value3 = new byte[] {(byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff};
        ip4Address = new Ip4Address(value3);
        assertThat(ip4Address.toString(), is("255.255.255.255"));
    }

    /**
     * Tests valid class constructor for an array value and an offset.
     */
    @Test
    public void testConstructorForArrayAndOffset() {
        final byte[] value1 = new byte[] {11, 22, 33,   // Preamble
                                          1, 2, 3, 4,
                                          44, 55};      // Extra bytes
        Ip4Address ip4Address = new Ip4Address(value1, 3);
        assertThat(ip4Address.toString(), is("1.2.3.4"));

        final byte[] value2 = new byte[] {11, 22,       // Preamble
                                          0, 0, 0, 0,
                                          33};          // Extra bytes
        ip4Address = new Ip4Address(value2, 2);
        assertThat(ip4Address.toString(), is("0.0.0.0"));

        final byte[] value3 = new byte[] {11, 22,       // Preamble
                                          (byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff,
                                          33};          // Extra bytes
        ip4Address = new Ip4Address(value3, 2);
        assertThat(ip4Address.toString(), is("255.255.255.255"));
    }

    /**
     * Tests invalid class constructor for a null array.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidConstructorNullArray() {
        final byte[] fromArray = null;
        Ip4Address ip4Address = new Ip4Address(fromArray);
    }

    /**
     * Tests invalid class constructor for an array that is too short.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConstructorShortArray() {
        final byte[] fromArray = new byte[] {1, 2, 3};
        Ip4Address ip4Address = new Ip4Address(fromArray);
    }

    /**
     * Tests invalid class constructor for an array and an invalid offset.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConstructorArrayInvalidOffset() {
        final byte[] value1 = new byte[] {11, 22, 33,   // Preamble
                                          1, 2, 3, 4,
                                          44, 55};      // Extra bytes
        Ip4Address ip4Address = new Ip4Address(value1, 6);
    }

    /**
     * Tests valid class constructor for a string.
     */
    @Test
    public void testConstructorForString() {
        Ip4Address ip4Address = new Ip4Address("1.2.3.4");
        assertThat(ip4Address.toString(), is("1.2.3.4"));

        ip4Address = new Ip4Address("0.0.0.0");
        assertThat(ip4Address.toString(), is("0.0.0.0"));

        ip4Address = new Ip4Address("255.255.255.255");
        assertThat(ip4Address.toString(), is("255.255.255.255"));
    }

    /**
     * Tests invalid class constructor for a null string.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidConstructorNullString() {
        String fromString = null;
        Ip4Address ip4Address = new Ip4Address(fromString);
    }

    /**
     * Tests invalid class constructor for an empty string.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConstructors() {
        // Check constructor for invalid ID: empty string
        Ip4Address ip4Address = new Ip4Address("");
    }

    /**
     * Tests returning the address as a byte array.
     */
    @Test
    public void testAddressToOctets() {
        final byte[] value1 = new byte[] {1, 2, 3, 4};
        Ip4Address ip4Address = new Ip4Address("1.2.3.4");
        assertThat(ip4Address.toOctets(), is(value1));

        final byte[] value2 = new byte[] {0, 0, 0, 0};
        ip4Address = new Ip4Address("0.0.0.0");
        assertThat(ip4Address.toOctets(), is(value2));

        final byte[] value3 = new byte[] {(byte) 0xff, (byte) 0xff,
                                          (byte) 0xff, (byte) 0xff};
        ip4Address = new Ip4Address("255.255.255.255");
        assertThat(ip4Address.toOctets(), is(value3));
    }

    /**
     * Tests making a mask prefix for a given prefix length.
     */
    @Test
    public void testMakeMaskPrefix() {
        Ip4Address ip4Address = Ip4Address.makeMaskPrefix(25);
        assertThat(ip4Address.toString(), is("255.255.255.128"));

        ip4Address = Ip4Address.makeMaskPrefix(0);
        assertThat(ip4Address.toString(), is("0.0.0.0"));

        ip4Address = Ip4Address.makeMaskPrefix(32);
        assertThat(ip4Address.toString(), is("255.255.255.255"));
    }

    /**
     * Tests making of a masked address.
     */
    @Test
    public void testMakeMaskedAddress() {
        Ip4Address ip4Address = new Ip4Address("1.2.3.5");
        Ip4Address ip4AddressMasked =
            Ip4Address.makeMaskedAddress(ip4Address, 24);
        assertThat(ip4AddressMasked.toString(), is("1.2.3.0"));

        ip4AddressMasked = Ip4Address.makeMaskedAddress(ip4Address, 0);
        assertThat(ip4AddressMasked.toString(), is("0.0.0.0"));

        ip4AddressMasked = Ip4Address.makeMaskedAddress(ip4Address, 32);
        assertThat(ip4AddressMasked.toString(), is("1.2.3.5"));
    }

    /**
     * Tests getting the value of an address.
     */
    @Test
    public void testGetValue() {
        Ip4Address ip4Address = new Ip4Address("1.2.3.4");
        assertThat(ip4Address.getValue(), is(0x01020304));

        ip4Address = new Ip4Address("0.0.0.0");
        assertThat(ip4Address.getValue(), is(0));

        ip4Address = new Ip4Address("255.255.255.255");
        assertThat(ip4Address.getValue(), is(-1));
    }

    /**
     * Tests equality of {@link Ip4Address}.
     */
    @Test
    public void testEquality() {
        Ip4Address addr1 = new Ip4Address("1.2.3.4");
        Ip4Address addr2 = new Ip4Address("1.2.3.4");
        assertThat(addr1, is(addr2));

        addr1 = new Ip4Address("0.0.0.0");
        addr2 = new Ip4Address("0.0.0.0");
        assertThat(addr1, is(addr2));

        addr1 = new Ip4Address("255.255.255.255");
        addr2 = new Ip4Address("255.255.255.255");
        assertThat(addr1, is(addr2));
    }

    /**
     * Tests non-equality of {@link Ip4Address}.
     */
    @Test
    public void testNonEquality() {
        Ip4Address addr1 = new Ip4Address("1.2.3.4");
        Ip4Address addr2 = new Ip4Address("1.2.3.5");
        Ip4Address addr3 = new Ip4Address("0.0.0.0");
        Ip4Address addr4 = new Ip4Address("255.255.255.255");
        assertThat(addr1, is(not(addr2)));
        assertThat(addr3, is(not(addr2)));
        assertThat(addr4, is(not(addr2)));
    }

    /**
     * Tests comparison of {@link Ip4Address}.
     */
    @Test
    public void testComparison() {
        Ip4Address addr1 = new Ip4Address("1.2.3.4");
        Ip4Address addr2 = new Ip4Address("1.2.3.4");
        Ip4Address addr3 = new Ip4Address("1.2.3.3");
        Ip4Address addr4 = new Ip4Address("1.2.3.5");
        assertTrue(addr1.compareTo(addr2) == 0);
        assertTrue(addr1.compareTo(addr3) > 0);
        assertTrue(addr1.compareTo(addr4) < 0);

        addr1 = new Ip4Address("255.2.3.4");
        addr2 = new Ip4Address("255.2.3.4");
        addr3 = new Ip4Address("255.2.3.3");
        addr4 = new Ip4Address("255.2.3.5");
        assertTrue(addr1.compareTo(addr2) == 0);
        assertTrue(addr1.compareTo(addr3) > 0);
        assertTrue(addr1.compareTo(addr4) < 0);
    }

    /**
     * Tests object string representation.
     */
    @Test
    public void testToString() {
        Ip4Address ip4Address = new Ip4Address("1.2.3.4");
        assertThat(ip4Address.toString(), is("1.2.3.4"));

        ip4Address = new Ip4Address("0.0.0.0");
        assertThat(ip4Address.toString(), is("0.0.0.0"));

        ip4Address = new Ip4Address("255.255.255.255");
        assertThat(ip4Address.toString(), is("255.255.255.255"));
    }
}
