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
package org.onosproject.net.host;

import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests for class {@link InterfaceIpAddress}.
 */
public class InterfaceIpAddressTest {
    private static final IpAddress IP_ADDRESS = IpAddress.valueOf("1.2.3.4");
    private static final IpPrefix SUBNET_ADDRESS =
        IpPrefix.valueOf("1.2.0.0/16");
    private static final IpAddress BROADCAST_ADDRESS =
        IpAddress.valueOf("1.2.0.255");         // NOTE: non-default broadcast
    private static final IpAddress PEER_ADDRESS = IpAddress.valueOf("5.6.7.8");
    private static final IpAddress DEF_BROADCAST_ADDRESS =
        IpAddress.valueOf("1.2.255.255");         // NOTE: default broadcast
    private static final IpPrefix V6_SUBNET_ADDRESS =
        IpPrefix.valueOf("::/64");

    private static final IpAddress IP_ADDRESS2 = IpAddress.valueOf("10.2.3.4");
    private static final IpPrefix SUBNET_ADDRESS2 =
        IpPrefix.valueOf("10.2.0.0/16");
    private static final IpAddress BROADCAST_ADDRESS2 =
        IpAddress.valueOf("10.2.0.255");        // NOTE: non-default broadcast
    private static final IpAddress PEER_ADDRESS2 =
        IpAddress.valueOf("50.6.7.8");

    /**
     * Tests valid class copy constructor.
     */
    @Test
    public void testCopyConstructor() {
        InterfaceIpAddress fromAddr;
        InterfaceIpAddress toAddr;

        // Regular interface address with default broadcast address
        fromAddr = new InterfaceIpAddress(IP_ADDRESS, SUBNET_ADDRESS);
        toAddr = new InterfaceIpAddress(fromAddr);
        assertThat(toAddr.ipAddress(), is(fromAddr.ipAddress()));
        assertThat(toAddr.subnetAddress(), is(fromAddr.subnetAddress()));
        assertThat(toAddr.broadcastAddress(), is(fromAddr.broadcastAddress()));
        assertThat(toAddr.peerAddress(), is(fromAddr.peerAddress()));

        // Interface address with non-default broadcast address
        fromAddr = new InterfaceIpAddress(IP_ADDRESS, SUBNET_ADDRESS,
                                          BROADCAST_ADDRESS);
        toAddr = new InterfaceIpAddress(fromAddr);
        assertThat(toAddr.ipAddress(), is(fromAddr.ipAddress()));
        assertThat(toAddr.subnetAddress(), is(fromAddr.subnetAddress()));
        assertThat(toAddr.broadcastAddress(), is(fromAddr.broadcastAddress()));
        assertThat(toAddr.peerAddress(), is(fromAddr.peerAddress()));

        // Point-to-point address with peer IP address
        fromAddr = new InterfaceIpAddress(IP_ADDRESS, SUBNET_ADDRESS, null,
                                          PEER_ADDRESS);
        toAddr = new InterfaceIpAddress(fromAddr);
        assertThat(toAddr.ipAddress(), is(fromAddr.ipAddress()));
        assertThat(toAddr.subnetAddress(), is(fromAddr.subnetAddress()));
        assertThat(toAddr.broadcastAddress(), is(fromAddr.broadcastAddress()));
        assertThat(toAddr.peerAddress(), is(fromAddr.peerAddress()));
    }

    /**
     * Tests invalid class copy constructor for a null object to copy from.
     */
    @Test(expected = NullPointerException.class)
    public void testInvalidConstructorNullObject() {
        InterfaceIpAddress fromAddr = null;
        InterfaceIpAddress toAddr = new InterfaceIpAddress(fromAddr);
    }

    /**
     * Tests valid class constructor for regular interface address with
     * default broadcast address.
     */
    @Test
    public void testConstructorForDefaultBroadcastAddress() {
        InterfaceIpAddress addr =
            new InterfaceIpAddress(IP_ADDRESS, SUBNET_ADDRESS);
        assertThat(addr.ipAddress(), is(IP_ADDRESS));
        assertThat(addr.subnetAddress(), is(SUBNET_ADDRESS));
        assertThat(addr.broadcastAddress(), is(DEF_BROADCAST_ADDRESS));
        assertThat(addr.peerAddress(), nullValue());

        IpPrefix  subnetAddr = IpPrefix.valueOf("10.2.3.0/24");
        InterfaceIpAddress addr1 = new InterfaceIpAddress(IP_ADDRESS2, subnetAddr);
        assertThat(addr1.broadcastAddress().toString(), is("10.2.3.255"));

        IpAddress ipAddress = IpAddress.valueOf("2001::4");
        InterfaceIpAddress addr2 = new InterfaceIpAddress(ipAddress, V6_SUBNET_ADDRESS);
        assertThat(addr2.broadcastAddress(), is(nullValue()));
    }

    /**
     * Tests valid class constructor for interface address with
     * non-default broadcast address.
     */
    @Test
    public void testConstructorForNonDefaultBroadcastAddress() {
        InterfaceIpAddress addr =
            new InterfaceIpAddress(IP_ADDRESS, SUBNET_ADDRESS,
                                   BROADCAST_ADDRESS);

        assertThat(addr.ipAddress(), is(IP_ADDRESS));
        assertThat(addr.subnetAddress(), is(SUBNET_ADDRESS));
        assertThat(addr.broadcastAddress(), is(BROADCAST_ADDRESS));
        assertThat(addr.peerAddress(), nullValue());
    }

    /**
     * Tests valid class constructor for point-to-point interface address with
     * peer address.
     */
    @Test
    public void testConstructorForPointToPointAddress() {
        InterfaceIpAddress addr =
            new InterfaceIpAddress(IP_ADDRESS, SUBNET_ADDRESS, null,
                                   PEER_ADDRESS);

        assertThat(addr.ipAddress(), is(IP_ADDRESS));
        assertThat(addr.subnetAddress(), is(SUBNET_ADDRESS));
        assertThat(addr.broadcastAddress(), nullValue());
        assertThat(addr.peerAddress(), is(PEER_ADDRESS));
    }

    /**
     * Tests getting the fields of an interface address.
     */
    @Test
    public void testGetFields() {
        InterfaceIpAddress addr;

        // Regular interface address with default broadcast address
        addr = new InterfaceIpAddress(IP_ADDRESS, SUBNET_ADDRESS);
        assertThat(addr.ipAddress().toString(), is("1.2.3.4"));
        assertThat(addr.subnetAddress().toString(), is("1.2.0.0/16"));
        assertThat(addr.broadcastAddress(), is(DEF_BROADCAST_ADDRESS));
        assertThat(addr.peerAddress(), is(nullValue()));

        // Interface address with non-default broadcast address
        addr = new InterfaceIpAddress(IP_ADDRESS, SUBNET_ADDRESS,
                                      BROADCAST_ADDRESS);
        assertThat(addr.ipAddress().toString(), is("1.2.3.4"));
        assertThat(addr.subnetAddress().toString(), is("1.2.0.0/16"));
        assertThat(addr.broadcastAddress().toString(), is("1.2.0.255"));
        assertThat(addr.peerAddress(), is(nullValue()));

        // Point-to-point address with peer IP address
        addr = new InterfaceIpAddress(IP_ADDRESS, SUBNET_ADDRESS, null,
                                      PEER_ADDRESS);
        assertThat(addr.ipAddress().toString(), is("1.2.3.4"));
        assertThat(addr.subnetAddress().toString(), is("1.2.0.0/16"));
        assertThat(addr.broadcastAddress(), is(nullValue()));
        assertThat(addr.peerAddress().toString(), is("5.6.7.8"));
    }

    /**
     * Tests equality of {@link InterfaceIpAddress}.
     */
    @Test
    public void testEquality() {
        InterfaceIpAddress addr1, addr2;

        // Regular interface address with default broadcast address
        addr1 = new InterfaceIpAddress(IP_ADDRESS, SUBNET_ADDRESS);
        addr2 = new InterfaceIpAddress(IP_ADDRESS, SUBNET_ADDRESS);
        assertThat(addr1, is(addr2));

        // Interface address with non-default broadcast address
        addr1 = new InterfaceIpAddress(IP_ADDRESS, SUBNET_ADDRESS,
                                       BROADCAST_ADDRESS);
        addr2 = new InterfaceIpAddress(IP_ADDRESS, SUBNET_ADDRESS,
                                       BROADCAST_ADDRESS);
        assertThat(addr1, is(addr2));

        // Point-to-point address with peer IP address
        addr1 = new InterfaceIpAddress(IP_ADDRESS, SUBNET_ADDRESS, null,
                                       PEER_ADDRESS);
        addr2 = new InterfaceIpAddress(IP_ADDRESS, SUBNET_ADDRESS, null,
                                       PEER_ADDRESS);
        assertThat(addr1, is(addr2));
    }

    /**
     * Tests non-equality of {@link InterfaceIpAddress}.
     */
    @Test
    public void testNonEquality() {
        InterfaceIpAddress addr1, addr2, addr3, addr4;

        // Regular interface address with default broadcast address
        addr1 = new InterfaceIpAddress(IP_ADDRESS, SUBNET_ADDRESS);
        // Interface address with non-default broadcast address
        addr2 = new InterfaceIpAddress(IP_ADDRESS, SUBNET_ADDRESS,
                                       BROADCAST_ADDRESS);
        // Point-to-point address with peer IP address
        addr3 = new InterfaceIpAddress(IP_ADDRESS, SUBNET_ADDRESS, null,
                                       PEER_ADDRESS);

        // Test interface addresses with different properties:
        //  - default-broadcast vs non-default broadcast
        //   - regular vs point-to-point
        assertThat(addr1, is(not(addr2)));
        assertThat(addr1, is(not(addr3)));
        assertThat(addr2, is(not(addr3)));

        // Test regular interface address with default broadcast address
        addr4 = new InterfaceIpAddress(IP_ADDRESS2, SUBNET_ADDRESS);
        assertThat(addr1, is(not(addr4)));
        addr4 = new InterfaceIpAddress(IP_ADDRESS, SUBNET_ADDRESS2);
        assertThat(addr1, is(not(addr4)));

        // Test interface address with non-default broadcast address
        addr4 = new InterfaceIpAddress(IP_ADDRESS2, SUBNET_ADDRESS,
                                       BROADCAST_ADDRESS);
        assertThat(addr2, is(not(addr4)));
        addr4 = new InterfaceIpAddress(IP_ADDRESS, SUBNET_ADDRESS2,
                                       BROADCAST_ADDRESS);
        assertThat(addr2, is(not(addr4)));
        addr4 = new InterfaceIpAddress(IP_ADDRESS, SUBNET_ADDRESS,
                                       BROADCAST_ADDRESS2);
        assertThat(addr2, is(not(addr4)));

        // Test point-to-point address with peer IP address
        addr4 = new InterfaceIpAddress(IP_ADDRESS2, SUBNET_ADDRESS, null,
                                       PEER_ADDRESS);
        assertThat(addr3, is(not(addr4)));
        addr4 = new InterfaceIpAddress(IP_ADDRESS, SUBNET_ADDRESS2, null,
                                       PEER_ADDRESS);
        assertThat(addr3, is(not(addr4)));
        addr4 = new InterfaceIpAddress(IP_ADDRESS, SUBNET_ADDRESS, null,
                                       PEER_ADDRESS2);
        assertThat(addr3, is(not(addr4)));
    }

    /**
     * Tests invalid class copy constructor for a null object to copy from.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testIllegalConstructorArgument() {
        InterfaceIpAddress toAddr = new InterfaceIpAddress(IP_ADDRESS, V6_SUBNET_ADDRESS);
    }
}
