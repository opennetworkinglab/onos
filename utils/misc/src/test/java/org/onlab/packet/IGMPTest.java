/*
 * Copyright 2015-present Open Networking Foundation
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

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Unit tests for IGMP class.
 */
public class IGMPTest {
    private Deserializer<IGMP> deserializer;

    private IGMP igmpQuery;
    private IGMP igmpMembership;

    private Ip4Address gaddr1;
    private Ip4Address gaddr2;
    private Ip4Address saddr1;
    private Ip4Address saddr2;

    @Before
    public void setUp() throws Exception {
        gaddr1 = Ip4Address.valueOf(0xe1010101);
        gaddr2 = Ip4Address.valueOf(0xe2020202);
        saddr1 = Ip4Address.valueOf(0x0a010101);
        saddr2 = Ip4Address.valueOf(0x0b020202);

        deserializer = IGMP.deserializer();

        // Create an IGMP Query object
        igmpQuery = new IGMP.IGMPv3();
        igmpQuery.setIgmpType(IGMP.TYPE_IGMPV3_MEMBERSHIP_QUERY);
        igmpQuery.setMaxRespCode((byte) 0x7f);
        IGMPQuery q = new IGMPQuery(gaddr1, (byte) 0x7f);
        q.addSource(saddr1);
        q.addSource(saddr2);
        q.setSbit(false);
        igmpQuery.groups.add(q);

        // Create an IGMP Membership Object
        igmpMembership = new IGMP.IGMPv3();
        igmpMembership.setIgmpType(IGMP.TYPE_IGMPV3_MEMBERSHIP_REPORT);
        IGMPMembership g1 = new IGMPMembership(gaddr1);
        g1.addSource(saddr1);
        g1.addSource(saddr2);
        igmpMembership.groups.add(g1);
        IGMPMembership g2 = new IGMPMembership(gaddr2);
        g2.addSource(saddr1);
        g2.addSource(saddr2);
        igmpMembership.groups.add(g2);
    }

    @Test
    public void testDeserializeBadInput() throws Exception {
        PacketTestUtils.testDeserializeBadInput(deserializer);
    }

    @Test
    public void testDeserializeTruncated() throws Exception {
        byte[] bits = igmpQuery.serialize();
        PacketTestUtils.testDeserializeTruncated(deserializer, bits);

        bits = igmpMembership.serialize();
        PacketTestUtils.testDeserializeTruncated(deserializer, bits);
    }

    @Test
    public void testDeserializeQuery() throws Exception {
        byte[] data = igmpQuery.serialize();
        IGMP igmp = deserializer.deserialize(data, 0, data.length);
        assertTrue(igmp.equals(igmpQuery));
    }

    @Test
    public void testDeserializeMembership() throws Exception {
        byte[] data = igmpMembership.serialize();
        IGMP igmp = deserializer.deserialize(data, 0, data.length);
        assertTrue(igmp.equals(igmpMembership));
    }

    @Test
    public void testIGMPv2() throws Exception {
        IGMP igmp = new IGMP.IGMPv2();
        igmp.setIgmpType((byte) 0x11);
        igmp.setMaxRespCode((byte) 0x64);
        igmp.addGroup(new IGMPQuery(IpAddress.valueOf(0), 0));

        byte[] data = igmp.serialize();
        assertEquals("Packet length is not 8 bytes", data.length, IGMP.IGMPv2.HEADER_LENGTH);
        IGMP deserialized = deserializer.deserialize(data, 0, data.length);
        assertTrue(igmp.equals(deserialized));
    }

    /**
     * Tests toString.
     */
    @Test
    public void testToStringIgmp() throws Exception {
        // TODO: add toString unit test
    }
}
