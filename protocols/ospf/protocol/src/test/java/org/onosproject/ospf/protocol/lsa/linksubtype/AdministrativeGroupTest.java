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
package org.onosproject.ospf.protocol.lsa.linksubtype;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.ospf.protocol.lsa.TlvHeader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test class for AdministrativeGroup.
 */
public class AdministrativeGroupTest {

    private final byte[] packet = {0, 0, 0, 1};
    private AdministrativeGroup administrativeGroup;
    private ChannelBuffer channelBuffer;
    private TlvHeader tlvHeader;
    private byte[] result;

    @Before
    public void setUp() throws Exception {
        administrativeGroup = new AdministrativeGroup(new TlvHeader());
    }

    @After
    public void tearDown() throws Exception {
        administrativeGroup = null;
        channelBuffer = null;
        tlvHeader = null;
    }

    /**
     * Tests administrativeGroup() getter method.
     */
    @Test
    public void testGetAdministrativeGroup() throws Exception {
        administrativeGroup.setAdministrativeGroup(1);
        assertThat(administrativeGroup.administrativeGroup(), is(1));
    }

    /**
     * Tests administrativeGroup() setter method.
     */
    @Test
    public void testSetAdministrativeGroup() throws Exception {
        administrativeGroup.setAdministrativeGroup(1);
        assertThat(administrativeGroup.administrativeGroup(), is(1));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        tlvHeader = new TlvHeader();
        tlvHeader.setTlvType(9);
        tlvHeader.setTlvLength(4);
        administrativeGroup = new AdministrativeGroup(tlvHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(packet);
        administrativeGroup.readFrom(channelBuffer);
        assertThat(administrativeGroup.administrativeGroup(), is(notNullValue()));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        result = administrativeGroup.asBytes();
        assertThat(result, is(notNullValue()));
    }


    /**
     * Tests getLinkSubTypeTlvBodyAsByteArray() method.
     */
    @Test
    public void testGetLinkSubTypeTlvBodyAsByteArray() throws Exception {
        result = administrativeGroup.getLinkSubTypeTlvBodyAsByteArray();
        assertThat(result, is(notNullValue()));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(administrativeGroup.toString(), is(notNullValue()));
    }
}