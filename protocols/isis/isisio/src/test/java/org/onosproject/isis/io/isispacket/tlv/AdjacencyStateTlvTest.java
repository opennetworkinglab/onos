/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.isis.io.isispacket.tlv;

import org.easymock.EasyMock;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test class for AdjacencyStateTlv.
 */
public class AdjacencyStateTlvTest {

    private final String neighborSystemId = "2929.2929.2929";
    private final byte[] tlv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private AdjacencyStateTlv adjacencyStateTlv;
    private TlvHeader tlvHeader;
    private int result;
    private byte result2;
    private String result1;
    private ChannelBuffer channelBuffer;
    private byte[] result3;

    @Before
    public void setUp() throws Exception {
        tlvHeader = new TlvHeader();
        adjacencyStateTlv = new AdjacencyStateTlv(tlvHeader);
        channelBuffer = EasyMock.createMock(ChannelBuffer.class);
    }

    @After
    public void tearDown() throws Exception {
        tlvHeader = null;
        adjacencyStateTlv = null;
        channelBuffer = null;
    }

    /**
     * Tests localCircuitId() getter method.
     */
    @Test
    public void testLocalCircuitId() throws Exception {
        adjacencyStateTlv.setLocalCircuitId(1);
        result = adjacencyStateTlv.localCircuitId();
        assertThat(result, is(1));
    }

    /**
     * Tests localCircuitId() setter method.
     */
    @Test
    public void testSetLocalCircuitId() throws Exception {
        adjacencyStateTlv.setLocalCircuitId(1);
        result = adjacencyStateTlv.localCircuitId();
        assertThat(result, is(1));
    }

    /**
     * Tests neighborSystemId() getter method.
     */
    @Test
    public void testNeighborSystemId() throws Exception {
        adjacencyStateTlv.setNeighborSystemId(neighborSystemId);
        result1 = adjacencyStateTlv.neighborSystemId();
        assertThat(result1, is(neighborSystemId));
    }

    /**
     * Tests neighborSystemId() setter method.
     */
    @Test
    public void testSetNeighborSystemId() throws Exception {
        adjacencyStateTlv.setNeighborSystemId(neighborSystemId);
        result1 = adjacencyStateTlv.neighborSystemId();
        assertThat(result1, is(neighborSystemId));
    }

    /**
     * Tests neighborLocalCircuitId() getter method.
     */
    @Test
    public void testNeighborLocalCircuitId() throws Exception {
        adjacencyStateTlv.setNeighborLocalCircuitId(1);
        result = adjacencyStateTlv.neighborLocalCircuitId();
        assertThat(result, is(1));
    }

    /**
     * Tests neighborLocalCircuitId() setter method.
     */
    @Test
    public void testSetNeighborLocalCircuitId() throws Exception {
        adjacencyStateTlv.setNeighborLocalCircuitId(1);
        result = adjacencyStateTlv.neighborLocalCircuitId();
        assertThat(result, is(1));
    }

    /**
     * Tests adjacencyType() getter method.
     */
    @Test
    public void testAdjacencyType() throws Exception {
        adjacencyStateTlv.setAdjacencyType((byte) 1);
        result2 = adjacencyStateTlv.adjacencyType();
        assertThat(result2, is((byte) 1));
    }

    /**
     * Tests adjacencyType() setter method.
     */
    @Test
    public void testSetAdjacencyType() throws Exception {
        adjacencyStateTlv.setAdjacencyType((byte) 1);
        result2 = adjacencyStateTlv.adjacencyType();
        assertThat(result2, is((byte) 1));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        adjacencyStateTlv.readFrom(channelBuffer);
        assertThat(adjacencyStateTlv.adjacencyType(), is((byte) 0));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        adjacencyStateTlv.readFrom(channelBuffer);
        result3 = adjacencyStateTlv.asBytes();
        assertThat(result3, is(notNullValue()));
    }

    /**
     * Tests toString() method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(adjacencyStateTlv.toString(), is(notNullValue()));
    }
}