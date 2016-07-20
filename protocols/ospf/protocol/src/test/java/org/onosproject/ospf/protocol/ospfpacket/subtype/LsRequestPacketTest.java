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
package org.onosproject.ospf.protocol.ospfpacket.subtype;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit test class for LsRequestPacket.
 */
public class LsRequestPacketTest {

    private LsRequestPacket lsrPacket;
    private int result;

    @Before
    public void setUp() throws Exception {
        lsrPacket = new LsRequestPacket();
    }

    @After
    public void tearDown() throws Exception {
        lsrPacket = null;
    }

    /**
     * Tests lsType() getter method.
     */
    @Test
    public void testGetLsType() throws Exception {
        lsrPacket.setLsType(1);
        assertThat(lsrPacket.lsType(), is(1));
    }

    /**
     * Tests lsType() setter method.
     */
    @Test
    public void testSetLsType() throws Exception {
        lsrPacket.setLsType(1);
        assertThat(lsrPacket.lsType(), is(1));
    }

    /**
     * Tests linkStateId() getter method.
     */
    @Test
    public void testGetLinkStateId() throws Exception {
        lsrPacket.setLinkStateId("1.1.1.1");
        assertThat(lsrPacket.linkStateId(), is("1.1.1.1"));
    }

    /**
     * Tests linkStateId() setter method.
     */
    @Test
    public void testSetLinkStateId() throws Exception {
        lsrPacket.setLinkStateId("1.1.1.1");
        assertThat(lsrPacket.linkStateId(), is("1.1.1.1"));
    }

    /**
     * Tests ownRouterId() getter method.
     */
    @Test
    public void testGetOwnRouterId() throws Exception {
        lsrPacket.setOwnRouterId("1.1.1.1");
        assertThat(lsrPacket.ownRouterId(), is("1.1.1.1"));
    }

    /**
     * Tests ownRouterId() setter method.
     */
    @Test
    public void testSetOwnRouterId() throws Exception {
        lsrPacket.setOwnRouterId("1.1.1.1");
        assertThat(lsrPacket.ownRouterId(), is("1.1.1.1"));
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(lsrPacket.toString(), is(notNullValue()));
    }

    /**
     * Tests equals() method.
     */
    @Test
    public void testEquals() throws Exception {
        assertThat(lsrPacket.equals(new LsRequestPacket()), is(false));
    }

    /**
     * Tests hashCode() method.
     */
    @Test
    public void testHashCode() throws Exception {
        result = lsrPacket.hashCode();
        assertThat(result, is(notNullValue()));
    }
}