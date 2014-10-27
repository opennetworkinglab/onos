/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.onlab.packet;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
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
     * Tests default class constructor.
     */
    @Test
    public void testDefaultConstructor() {
        Ip4Prefix ip4prefix = new Ip4Prefix();
        assertThat(ip4prefix.toString(), is("0.0.0.0/0"));
    }

    /**
     * Tests valid class copy constructor.
     */
    @Test
    public void testCopyConstructor() {
        Ip4Prefix fromAddr = new Ip4Prefix("1.2.3.0/24");
        Ip4Prefix ip4prefix = new Ip4Prefix(fromAddr);
        assertThat(ip4prefix.toString(), is("1.2.3.0/24"));

        fromAddr = new Ip4Prefix("0.0.0.0/0");
        ip4prefix = new Ip4Prefix(fromAddr);
        assertThat(ip4prefix.toString(), is("0.0.0.0/0"));

        fromAddr = new Ip4Prefix("255.255.255.255/32");
        ip4prefix = new Ip4Prefix(fromAddr);
        assertThat(ip4prefix.toString(), is("255.255.255.255/32"));
    }

    /**
     * Tests invalid class copy constructor for a null object to copy from.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidConstructorNullObject() {
        Ip4Prefix fromAddr = null;
        Ip4Prefix ip4prefix = new Ip4Prefix(fromAddr);
    }

    /**
     * Tests valid class constructor for an address and prefix length.
     */
    @Test
    public void testConstructorForAddressAndPrefixLength() {
        Ip4Prefix ip4prefix =
            new Ip4Prefix(new Ip4Address("1.2.3.0"), (short) 24);
        assertThat(ip4prefix.toString(), is("1.2.3.0/24"));

        ip4prefix = new Ip4Prefix(new Ip4Address("1.2.3.4"), (short) 24);
        assertThat(ip4prefix.toString(), is("1.2.3.0/24"));

        ip4prefix = new Ip4Prefix(new Ip4Address("1.2.3.5"), (short) 32);
        assertThat(ip4prefix.toString(), is("1.2.3.5/32"));

        ip4prefix = new Ip4Prefix(new Ip4Address("0.0.0.0"), (short) 0);
        assertThat(ip4prefix.toString(), is("0.0.0.0/0"));

        ip4prefix =
            new Ip4Prefix(new Ip4Address("255.255.255.255"), (short) 32);
        assertThat(ip4prefix.toString(), is("255.255.255.255/32"));
    }

    /**
     * Tests valid class constructor for a string.
     */
    @Test
    public void testConstructorForString() {
        Ip4Prefix ip4prefix = new Ip4Prefix("1.2.3.0/24");
        assertThat(ip4prefix.toString(), is("1.2.3.0/24"));

        ip4prefix = new Ip4Prefix("1.2.3.4/24");
        assertThat(ip4prefix.toString(), is("1.2.3.0/24"));

        ip4prefix = new Ip4Prefix("1.2.3.5/32");
        assertThat(ip4prefix.toString(), is("1.2.3.5/32"));

        ip4prefix = new Ip4Prefix("0.0.0.0/0");
        assertThat(ip4prefix.toString(), is("0.0.0.0/0"));

        ip4prefix = new Ip4Prefix("255.255.255.255/32");
        assertThat(ip4prefix.toString(), is("255.255.255.255/32"));
    }

    /**
     * Tests invalid class constructor for a null string.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidConstructorNullString() {
        String fromString = null;
        Ip4Prefix ip4prefix = new Ip4Prefix(fromString);
    }

    /**
     * Tests invalid class constructor for an empty string.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testInvalidConstructors() {
        // Check constructor for invalid ID: empty string
        Ip4Prefix ip4prefix = new Ip4Prefix("");
    }

    /**
     * Tests getting the value of an address.
     */
    @Test
    public void testGetValue() {
        Ip4Prefix ip4prefix = new Ip4Prefix("1.2.3.0/24");
        assertThat(ip4prefix.getAddress(), equalTo(new Ip4Address("1.2.3.0")));
        assertThat(ip4prefix.getPrefixLen(), is((short) 24));

        ip4prefix = new Ip4Prefix("0.0.0.0/0");
        assertThat(ip4prefix.getAddress(), equalTo(new Ip4Address("0.0.0.0")));
        assertThat(ip4prefix.getPrefixLen(), is((short) 0));

        ip4prefix = new Ip4Prefix("255.255.255.255/32");
        assertThat(ip4prefix.getAddress(),
                   equalTo(new Ip4Address("255.255.255.255")));
        assertThat(ip4prefix.getPrefixLen(), is((short) 32));
    }

    /**
     * Tests equality of {@link Ip4Address}.
     */
    @Test
    public void testEquality() {
        Ip4Prefix addr1net = new Ip4Prefix("1.2.3.0/24");
        Ip4Prefix addr2net = new Ip4Prefix("1.2.3.0/24");
        assertThat(addr1net, is(addr2net));

        addr1net = new Ip4Prefix("1.2.3.0/24");
        addr2net = new Ip4Prefix("1.2.3.4/24");
        assertThat(addr1net, is(addr2net));

        addr1net = new Ip4Prefix("0.0.0.0/0");
        addr2net = new Ip4Prefix("0.0.0.0/0");
        assertThat(addr1net, is(addr2net));

        addr1net = new Ip4Prefix("255.255.255.255/32");
        addr2net = new Ip4Prefix("255.255.255.255/32");
        assertThat(addr1net, is(addr2net));
    }

    /**
     * Tests non-equality of {@link Ip4Address}.
     */
    @Test
    public void testNonEquality() {
        Ip4Prefix addr1net = new Ip4Prefix("1.2.0.0/16");
        Ip4Prefix addr2net = new Ip4Prefix("1.3.0.0/16");
        Ip4Prefix addr3net = new Ip4Prefix("1.3.0.0/24");
        Ip4Prefix addr4net = new Ip4Prefix("0.0.0.0/0");
        Ip4Prefix addr5net = new Ip4Prefix("255.255.255.255/32");
        assertThat(addr1net, is(not(addr2net)));
        assertThat(addr3net, is(not(addr2net)));
        assertThat(addr4net, is(not(addr2net)));
        assertThat(addr5net, is(not(addr2net)));
    }

    /**
     * Tests object string representation.
     */
    @Test
    public void testToString() {
        Ip4Prefix ip4prefix = new Ip4Prefix("1.2.3.0/24");
        assertThat(ip4prefix.toString(), is("1.2.3.0/24"));

        ip4prefix = new Ip4Prefix("1.2.3.4/24");
        assertThat(ip4prefix.toString(), is("1.2.3.0/24"));

        ip4prefix = new Ip4Prefix("0.0.0.0/0");
        assertThat(ip4prefix.toString(), is("0.0.0.0/0"));

        ip4prefix = new Ip4Prefix("255.255.255.255/32");
        assertThat(ip4prefix.toString(), is("255.255.255.255/32"));
    }
}
