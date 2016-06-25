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
package org.onosproject.isis.io.isispacket;

import org.easymock.EasyMock;
import org.jboss.netty.buffer.ChannelBuffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onosproject.isis.controller.IsisPduType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test class for IsisHeader.
 */
public class IsisHeaderTest {

    private IsisHeader isisHeader;
    private MacAddress macAddress = MacAddress.valueOf("a4:23:05:00:00:00");
    private int result;
    private MacAddress result1;
    private byte result2;
    private IsisPduType result3;
    private ChannelBuffer channelBuffer;

    @Before
    public void setUp() throws Exception {
        isisHeader = new IsisHeader();
        channelBuffer = EasyMock.createMock(ChannelBuffer.class);
    }

    @After
    public void tearDown() throws Exception {
        isisHeader = null;
    }

    /**
     * Tests interfaceIndex() getter method.
     */
    @Test
    public void testInterfaceIndex() throws Exception {
        isisHeader.setInterfaceIndex(1);
        result = isisHeader.interfaceIndex();
        assertThat(result, is(1));
    }

    /**
     * Tests interfaceIndex() setter method.
     */
    @Test
    public void testSetInterfaceIndex() throws Exception {
        isisHeader.setInterfaceIndex(1);
        result = isisHeader.interfaceIndex();
        assertThat(result, is(1));
    }

    /**
     * Tests interfaceMac() getter method.
     */
    @Test
    public void testInterfaceMac() throws Exception {
        isisHeader.setInterfaceMac(macAddress);
        result1 = isisHeader.interfaceMac();
        assertThat(result1, is(macAddress));
    }

    /**
     * Tests sourceMac() getter method.
     */
    @Test
    public void testSourceMac() throws Exception {
        isisHeader.setSourceMac(macAddress);
        result1 = isisHeader.sourceMac();
        assertThat(result1, is(macAddress));
    }

    /**
     * Tests sourceMac() setter method.
     */
    @Test
    public void testSetSourceMac() throws Exception {
        isisHeader.setSourceMac(macAddress);
        result1 = isisHeader.sourceMac();
        assertThat(result1, is(macAddress));
    }

    /**
     * Tests interfaceMac() setter method.
     */
    @Test
    public void testSetInterfaceMac() throws Exception {
        isisHeader.setSourceMac(macAddress);
        result1 = isisHeader.sourceMac();
        assertThat(result1, is(macAddress));
    }

    /**
     * Tests version2() getter method.
     */
    @Test
    public void testVersion2() throws Exception {
        isisHeader.setVersion2((byte) 1);
        result2 = isisHeader.version2();
        assertThat(result2, is((byte) 1));
    }

    /**
     * Tests version2() setter method.
     */
    @Test
    public void testSetVersion2() throws Exception {
        isisHeader.setVersion2((byte) 1);
        result2 = isisHeader.version2();
        assertThat(result2, is((byte) 1));
    }

    /**
     * Tests maximumAreaAddresses() getter method.
     */
    @Test
    public void testMaximumAreaAddresses() throws Exception {
        isisHeader.setMaximumAreaAddresses((byte) 1);
        result2 = isisHeader.maximumAreaAddresses();
        assertThat(result2, is((byte) 1));
    }

    /**
     * Tests maximumAreaAddresses() setter method.
     */
    @Test
    public void testSetMaximumAreaAddresses() throws Exception {
        isisHeader.setMaximumAreaAddresses((byte) 1);
        result2 = isisHeader.maximumAreaAddresses();
        assertThat(result2, is((byte) 1));
    }

    /**
     * Tests reserved() getter method.
     */
    @Test
    public void testReserved() throws Exception {
        isisHeader.setReserved((byte) 1);
        result2 = isisHeader.reserved();
        assertThat(result2, is((byte) 1));
    }

    /**
     * Tests reserved() setter method.
     */
    @Test
    public void testSetReserved() throws Exception {
        isisHeader.setReserved((byte) 1);
        result2 = isisHeader.reserved();
        assertThat(result2, is((byte) 1));
    }

    /**
     * Tests version() getter method.
     */
    @Test
    public void testVersion() throws Exception {
        isisHeader.setVersion((byte) 1);
        result2 = isisHeader.version();
        assertThat(result2, is((byte) 1));
    }

    /**
     * Tests version() setter method.
     */
    @Test
    public void testSetVersion() throws Exception {
        isisHeader.setVersion((byte) 1);
        result2 = isisHeader.version();
        assertThat(result2, is((byte) 1));
    }

    /**
     * Tests idLength() getter method.
     */
    @Test
    public void testIdLength() throws Exception {
        isisHeader.setIdLength((byte) 1);
        result2 = isisHeader.idLength();
        assertThat(result2, is((byte) 1));
    }

    /**
     * Tests idLength() setter method.
     */
    @Test
    public void testSetIdLength() throws Exception {
        isisHeader.setIdLength((byte) 1);
        result2 = isisHeader.idLength();
        assertThat(result2, is((byte) 1));
    }

    /**
     * Tests pduType() getter method.
     */
    @Test
    public void testPduType() throws Exception {
        isisHeader.setIsisPduType(1);
        result = isisHeader.pduType();
        assertThat(result, is(1));
    }

    /**
     * Tests pduType() setter method.
     */
    @Test
    public void testSetIsisPduType() throws Exception {
        isisHeader.setIsisPduType(1);
        result = isisHeader.pduType();
        assertThat(result, is(1));
    }

    /**
     * Tests pduHeaderLength() getter method.
     */
    @Test
    public void testPduHeaderLength() throws Exception {
        isisHeader.setPduHeaderLength((byte) 1);
        result2 = isisHeader.pduHeaderLength();
        assertThat(result2, is((byte) 1));
    }

    /**
     * Tests pduHeaderLength() setter method.
     */
    @Test
    public void testSetPduHeaderLength() throws Exception {
        isisHeader.setPduHeaderLength((byte) 1);
        result2 = isisHeader.pduHeaderLength();
        assertThat(result2, is((byte) 1));
    }

    /**
     * Tests irpDiscriminator() getter method.
     */
    @Test
    public void testIrpDiscriminator() throws Exception {
        isisHeader.setIrpDiscriminator((byte) 1);
        result2 = isisHeader.irpDiscriminator();
        assertThat(result2, is((byte) 1));
    }

    /**
     * Tests irpDiscriminator() setter method.
     */
    @Test
    public void testSetIrpDiscriminator() throws Exception {
        isisHeader.setIrpDiscriminator((byte) 1);
        result2 = isisHeader.irpDiscriminator();
        assertThat(result2, is((byte) 1));
    }

    /**
     * Tests irpDiscriminator() setter method.
     */
    @Test
    public void testIsisPduType() throws Exception {
        isisHeader.setIsisPduType(IsisPduType.L1HELLOPDU.value());
        result3 = isisHeader.isisPduType();
        assertThat(result3, is(IsisPduType.L1HELLOPDU));
    }

    /**
     * Tests readFrom() method.
     */
    @Test
    public void testReadFrom() throws Exception {
        isisHeader.readFrom(channelBuffer);
        assertThat(isisHeader, is(notNullValue()));
    }

    /**
     * Tests asBytes() method.
     */
    @Test
    public void testAsBytes() throws Exception {
        isisHeader.asBytes();
        assertThat(isisHeader, is(notNullValue()));
    }

    /**
     * Tests populateHeader() method.
     */
    @Test
    public void testPopulateHeader() throws Exception {
        isisHeader.populateHeader(new IsisHeader());
        assertThat(isisHeader, is(notNullValue()));
    }
}