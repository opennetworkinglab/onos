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
import org.onlab.packet.pim.PIMAddrUnicast;
import org.onlab.packet.pim.PIMHello;
import org.onlab.packet.pim.PIMJoinPrune;

import static junit.framework.Assert.assertTrue;

public final class PIMTest {

    public static final String SADDR = "10.2.1.2";
    public static final String PIMADDR = "224.0.0.13";
    public static final String PIMUADDR = "10.23.3.5";

    public static final String SADDR1 = "10.1.1.1/32";
    public static final String SADDR2 = "10.1.2.1/32";
    public static final String GADDR1 = "232.1.1.1/32";
    public static final String GADDR2 = "232.1.2.1/32";

    public static final String CPSTR1 = "of:deadbeefball/8";
    public static final String CPSTR2 = "of:deadbeefcafe/3";
    public static final String CPSTR3 = "of:2badcafef00d/3";

    private Deserializer<PIM> deserializer;

    private PIM pimHello;
    private PIMHello hello;

    private PIM pimJoinPrune;
    private PIMJoinPrune joinPrune;

    /**
     * Create PIM Hello and Join/Prune packets to be used in testing.
     *
     * @throws Exception if packet creation fails
     */
    @Before
    public void setUp() throws Exception {

        // Create a PIM Hello
        pimHello = new PIM();
        pimHello.setVersion((byte) 2);
        pimHello.setPIMType((byte) PIM.TYPE_HELLO);
        pimHello.setChecksum((short) 0);

        hello = new PIMHello();
        hello.createDefaultOptions();
        pimHello.setPayload(hello);
        hello.setParent(pimHello);

        // Create PIM Join Prune
        pimJoinPrune = new PIM();
        pimJoinPrune.setVersion((byte) 2);
        pimJoinPrune.setPIMType((byte) PIM.TYPE_JOIN_PRUNE_REQUEST);
        pimJoinPrune.setChecksum((short) 0);

        joinPrune = new PIMJoinPrune();
        joinPrune.setUpstreamAddr(new PIMAddrUnicast(SADDR));
        joinPrune.addJoin(GADDR1, SADDR1);
        joinPrune.addJoin(GADDR2, SADDR2);
        joinPrune.addPrune(GADDR1, SADDR2);
        joinPrune.addPrune(GADDR2, SADDR1);

        pimJoinPrune.setPayload(joinPrune);
        joinPrune.setParent(pimJoinPrune);

        deserializer = PIM.deserializer();
    }

    /**
     * Make sure our deserializer throws an exception if we recieve bad input.
     *
     * @throws Exception if we are given bad input.
     */
    @Test
    public void testDeserializeBadInput() throws Exception {
        PacketTestUtils.testDeserializeBadInput(deserializer);
    }

    /**
     * Verify we throw an exception if we receive a truncated Join/Prune message.
     *
     * @throws Exception if we receive a truncated Join/Prune message.
     */
    @Test
    public void testDeserializeTruncated() throws Exception {
        byte[] bits = pimJoinPrune.serialize();
        PacketTestUtils.testDeserializeTruncated(deserializer, bits);
    }

    /**
     * Verify that we correctly deserialize hello messages.
     *
     * @throws Exception if our input is bad or truncated.
     */
    @Test
    public void testDeserializeHello() throws Exception {
        byte[] data = pimHello.serialize();
        PIM pim = deserializer.deserialize(data, 0, data.length);
        assertTrue(pim.equals(pimHello));
    }

    /**
     * Verify that we correctly deserialize Join/Prune messages.
     *
     * @throws Exception if our input is bad or truncated.
     */
    @Test
    public void testDeserializeJoinPrune() throws Exception {
        byte[] data = pimJoinPrune.serialize();
        PIM pim = deserializer.deserialize(data, 0, data.length);
        assertTrue(pim.equals(pimJoinPrune));
    }

}