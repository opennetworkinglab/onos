/*
 * Copyright 2015-present Open Networking Laboratory
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

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the Ethernet class.
 */
public class EthernetTest {

    private MacAddress dstMac;
    private MacAddress srcMac;
    private short ethertype = 6;
    private short vlan = 5;

    private Deserializer<Ethernet> deserializer;

    private byte[] byteHeader;
    private byte[] vlanByteHeader;

    @Before
    public void setUp() {
        deserializer = Ethernet.deserializer();

        byte[] dstMacBytes = {
                (byte) 0x88, (byte) 0x88, (byte) 0x88, (byte) 0x88, (byte) 0x88,
                (byte) 0x88 };
        dstMac = MacAddress.valueOf(dstMacBytes);
        byte[] srcMacBytes = {
                (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa,
                (byte) 0xaa };
        srcMac = MacAddress.valueOf(srcMacBytes);

        // Create Ethernet byte array with no VLAN header
        ByteBuffer bb = ByteBuffer.allocate(Ethernet.ETHERNET_HEADER_LENGTH);
        bb.put(dstMacBytes);
        bb.put(srcMacBytes);
        bb.putShort(ethertype);

        byteHeader = bb.array();

        // Create Ethernet byte array with a VLAN header
        bb = ByteBuffer.allocate(Ethernet.ETHERNET_HEADER_LENGTH + Ethernet.VLAN_HEADER_LENGTH);
        bb.put(dstMacBytes);
        bb.put(srcMacBytes);
        bb.putShort(Ethernet.TYPE_VLAN);
        bb.putShort(vlan);
        bb.putShort(ethertype);

        vlanByteHeader = bb.array();
    }

    @Test
    public void testDeserializeBadInput() throws Exception {
        PacketTestUtils.testDeserializeBadInput(deserializer);
    }

    @Test
    public void testDeserializeTruncated() throws DeserializationException {
        PacketTestUtils.testDeserializeTruncated(deserializer, vlanByteHeader);
    }

    @Test
    public void testDeserializeNoVlan() throws Exception {
        Ethernet eth = deserializer.deserialize(byteHeader, 0, byteHeader.length);

        assertEquals(dstMac, eth.getDestinationMAC());
        assertEquals(srcMac, eth.getSourceMAC());
        assertEquals(Ethernet.VLAN_UNTAGGED, eth.getVlanID());
        assertEquals(ethertype, eth.getEtherType());
    }

    @Test
    public void testDeserializeWithVlan() throws Exception {
        Ethernet eth = deserializer.deserialize(vlanByteHeader, 0, vlanByteHeader.length);

        assertEquals(dstMac, eth.getDestinationMAC());
        assertEquals(srcMac, eth.getSourceMAC());
        assertEquals(vlan, eth.getVlanID());
        assertEquals(ethertype, eth.getEtherType());
    }

}
