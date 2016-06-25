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
package org.onosproject.ospf.protocol.lsa;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit test class for Tlv Header.
 */
public class TlvHeaderTest {

    private TlvHeader tlvHeader;
    private byte[] result;


    @Before
    public void setUp() throws Exception {
        tlvHeader = new TlvHeader();
    }

    @After
    public void tearDown() throws Exception {
        tlvHeader = null;
        result = null;
    }

    /**
     * Tests to string method.
     */
    @Test
    public void testToString() throws Exception {
        assertThat(tlvHeader.toString(), is(notNullValue()));
    }

    /**
     * Tests tlvLength() getter method.
     */
    @Test
    public void testGetTlvLength() throws Exception {
        tlvHeader.setTlvLength(2);
        assertThat(tlvHeader.tlvLength(), is(2));
    }

    /**
     * Tests tlvLength() setter method.
     */
    @Test
    public void testSetTlvLength() throws Exception {
        tlvHeader.setTlvLength(2);
        assertThat(tlvHeader.tlvLength(), is(2));
    }

    /**
     * Tests tlvType() getter method.
     */
    @Test
    public void testGetTlvType() throws Exception {
        tlvHeader.setTlvType(2);
        assertThat(tlvHeader.tlvType(), is(2));
    }

    /**
     * Tests tlvType() setter method.
     */
    @Test
    public void testSetTlvType() throws Exception {
        tlvHeader.setTlvType(2);
        assertThat(tlvHeader.tlvType(), is(2));
    }

    /**
     * Tests getTlvHeaderAsByteArray() method.
     */
    @Test
    public void testGetTlvHeaderAsByteArray() throws Exception {
        result = tlvHeader.getTlvHeaderAsByteArray();
        assertThat(result, is(notNullValue()));
    }
}