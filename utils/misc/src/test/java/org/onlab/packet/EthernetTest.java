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

import java.nio.ByteBuffer;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the Ethernet class.
 */
public class EthernetTest {

    private MacAddress dstMac;
    private MacAddress srcMac;
    private short ethertype = 6;
    private short vlan = 5;
    private short qinqVlan = 55;

    private Deserializer<Ethernet> deserializer;

    private byte[] byteHeader;
    private byte[] vlanByteHeader;
    private byte[] qinq8100ByteHeader;
    private byte[] qinq88a8ByteHeader;

    private static byte[] qinqHeaderExpected = {
                (byte) 0x88, (byte) 0x88, (byte) 0x88, (byte) 0x88,
                (byte) 0x88, (byte) 0x88, (byte) 0xaa, (byte) 0xaa,
                (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa,
                (byte) 0x88, (byte) 0xa8, (byte) 0x00, (byte) 0x37,
                (byte) 0x81, (byte) 0x00, (byte) 0x00, (byte) 0x05,
                (byte) 0x00, (byte) 0x06 };

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

        // Create Ethernet byte array with a QinQ header with TPID 0x8100
        bb = ByteBuffer.allocate(Ethernet.ETHERNET_HEADER_LENGTH + Ethernet.VLAN_HEADER_LENGTH
                                 + Ethernet.VLAN_HEADER_LENGTH);
        bb.put(dstMacBytes);
        bb.put(srcMacBytes);
        bb.putShort(Ethernet.TYPE_VLAN);
        bb.putShort(vlan);
        bb.putShort(Ethernet.TYPE_VLAN);
        bb.putShort(vlan);
        bb.putShort(ethertype);

        qinq8100ByteHeader = bb.array();

        // Create Ethernet byte array with a QinQ header with TPID 0x88a8
        bb = ByteBuffer.allocate(Ethernet.ETHERNET_HEADER_LENGTH + Ethernet.VLAN_HEADER_LENGTH
                                 + Ethernet.VLAN_HEADER_LENGTH);
        bb.put(dstMacBytes);
        bb.put(srcMacBytes);
        bb.putShort(Ethernet.TYPE_QINQ);
        bb.putShort(qinqVlan);
        bb.putShort(Ethernet.TYPE_VLAN);
        bb.putShort(vlan);
        bb.putShort(ethertype);

        qinq88a8ByteHeader = bb.array();

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

    @Test
    public void testDeserializeWithQinQ() throws Exception {
        Ethernet eth = deserializer.deserialize(qinq8100ByteHeader, 0, qinq8100ByteHeader.length);

        assertEquals(dstMac, eth.getDestinationMAC());
        assertEquals(srcMac, eth.getSourceMAC());
        assertEquals(vlan, eth.getVlanID());
        assertEquals(vlan, eth.getQinQVID());
        assertEquals(ethertype, eth.getEtherType());

        eth = deserializer.deserialize(qinq88a8ByteHeader, 0, qinq88a8ByteHeader.length);

        assertEquals(dstMac, eth.getDestinationMAC());
        assertEquals(srcMac, eth.getSourceMAC());
        assertEquals(vlan, eth.getVlanID());
        assertEquals(qinqVlan, eth.getQinQVID());
        assertEquals(ethertype, eth.getEtherType());
    }

    @Test
    public void testSerializeWithQinQ() throws Exception {
        Ethernet eth = new Ethernet();
        eth.setDestinationMACAddress(dstMac);
        eth.setSourceMACAddress(srcMac);
        eth.setVlanID(vlan);
        eth.setQinQVID(qinqVlan);
        eth.setEtherType(ethertype);

        byte[] encoded = eth.serialize();

        assertEquals(Arrays.toString(encoded), Arrays.toString(qinqHeaderExpected));
    }

}
