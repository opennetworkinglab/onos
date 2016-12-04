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
package org.onosproject.isis.io.isispacket.tlv.subtlv;

import org.easymock.EasyMock;
import org.jboss.netty.buffer.ChannelBuffer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.isis.io.isispacket.tlv.TlvHeader;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test class for SubTlvToBytes.
 */
public class SubTlvToBytesTest {
    private TlvHeader tlvHeader;
    private ChannelBuffer channelBuffer;
    private List<Byte> tlv;

    @Before
    public void setUp() throws Exception {
        tlvHeader = new TlvHeader();
        channelBuffer = EasyMock.createMock(ChannelBuffer.class);
    }

    @After
    public void tearDown() throws Exception {
        tlvHeader = null;
        channelBuffer = null;
    }

    @Test
    public void testTlvToBytes() throws Exception {
        tlvHeader.setTlvLength(4);
        tlvHeader.setTlvType(9);
        AdministrativeGroup administrativeGroup = new AdministrativeGroup(tlvHeader);
        tlv = SubTlvToBytes.tlvToBytes(administrativeGroup);
        assertThat(tlv, is(notNullValue()));

        tlvHeader = new TlvHeader();
        tlvHeader.setTlvLength(4);
        tlvHeader.setTlvType(5);
        TrafficEngineeringMetric trafficEngineeringMetric = new TrafficEngineeringMetric(tlvHeader);
        tlv = SubTlvToBytes.tlvToBytes(trafficEngineeringMetric);
        assertThat(tlv, is(notNullValue()));

        tlvHeader = new TlvHeader();
        tlvHeader.setTlvLength(4);
        tlvHeader.setTlvType(6);
        MaximumBandwidth maximumBandwidth = new MaximumBandwidth(tlvHeader);
        tlv = SubTlvToBytes.tlvToBytes(maximumBandwidth);
        assertThat(tlv, is(notNullValue()));

        tlvHeader = new TlvHeader();
        tlvHeader.setTlvLength(4);
        tlvHeader.setTlvType(7);
        MaximumReservableBandwidth maximumReservableBandwidth = new MaximumReservableBandwidth(tlvHeader);
        tlv = SubTlvToBytes.tlvToBytes(maximumReservableBandwidth);
        assertThat(tlv, is(notNullValue()));

        tlvHeader = new TlvHeader();
        tlvHeader.setTlvLength(4);
        tlvHeader.setTlvType(8);
        UnreservedBandwidth unreservedBandwidth = new UnreservedBandwidth(tlvHeader);
        tlv = SubTlvToBytes.tlvToBytes(unreservedBandwidth);
        assertThat(tlv, is(notNullValue()));
    }
}
