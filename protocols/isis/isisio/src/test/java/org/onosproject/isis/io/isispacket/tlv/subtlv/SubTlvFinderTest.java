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
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.isis.io.isispacket.tlv.TlvHeader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Unit test class for SubTlvFinder.
 */
public class SubTlvFinderTest {
    private final byte[] packet1 = {0, 0, 0, 1};
    private TlvHeader tlvHeader;
    private ChannelBuffer channelBuffer;
    private TrafficEngineeringSubTlv tlv;

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
    public void testFindSubTlv() throws Exception {

        tlvHeader.setTlvLength(4);
        tlvHeader.setTlvType(SubTlvType.ADMINISTRATIVEGROUP.value());
        AdministrativeGroup administrativeGroup = new AdministrativeGroup(tlvHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(packet1);
        tlv = SubTlvFinder.findSubTlv(tlvHeader, channelBuffer);
        assertThat(tlv, is(notNullValue()));

        tlvHeader = new TlvHeader();
        tlvHeader.setTlvLength(4);
        tlvHeader.setTlvType(SubTlvType.TRAFFICENGINEERINGMETRIC.value());
        TrafficEngineeringMetric trafficEngineeringMetric = new TrafficEngineeringMetric(tlvHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(packet1);
        tlv = SubTlvFinder.findSubTlv(tlvHeader, channelBuffer);
        assertThat(tlv, is(notNullValue()));

        tlvHeader = new TlvHeader();
        tlvHeader.setTlvLength(4);
        tlvHeader.setTlvType(SubTlvType.MAXIMUMBANDWIDTH.value());
        MaximumBandwidth maximumBandwidth = new MaximumBandwidth(tlvHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(packet1);
        tlv = SubTlvFinder.findSubTlv(tlvHeader, channelBuffer);
        assertThat(tlv, is(notNullValue()));

        tlvHeader = new TlvHeader();
        tlvHeader.setTlvLength(4);
        tlvHeader.setTlvType(SubTlvType.MAXIMUMRESERVABLEBANDWIDTH.value());
        MaximumReservableBandwidth maximumReservableBandwidth = new MaximumReservableBandwidth(tlvHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(packet1);
        tlv = SubTlvFinder.findSubTlv(tlvHeader, channelBuffer);
        assertThat(tlv, is(notNullValue()));

        tlvHeader = new TlvHeader();
        tlvHeader.setTlvLength(4);
        tlvHeader.setTlvType(SubTlvType.UNRESERVEDBANDWIDTH.value());
        UnreservedBandwidth unreservedBandwidth = new UnreservedBandwidth(tlvHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(packet1);
        tlv = SubTlvFinder.findSubTlv(tlvHeader, channelBuffer);
        assertThat(tlv, is(notNullValue()));

        tlvHeader = new TlvHeader();
        tlvHeader.setTlvLength(4);
        tlvHeader.setTlvType(SubTlvType.INTERFACEADDRESS.value());
        InterfaceIpAddress ipInterfaceAddressTlv = new InterfaceIpAddress(tlvHeader);
        channelBuffer = ChannelBuffers.copiedBuffer(packet1);
        tlv = SubTlvFinder.findSubTlv(tlvHeader, channelBuffer);
        assertThat(tlv, is(notNullValue()));
    }
}