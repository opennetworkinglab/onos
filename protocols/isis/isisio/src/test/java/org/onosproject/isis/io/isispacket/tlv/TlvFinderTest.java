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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Unit test class for TlvFinder.
 */
public class TlvFinderTest {

    private final byte[] tlv = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private final byte[] tlv1 = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private TlvFinder tlvFinder;
    private TlvHeader tlvHeader;
    private ChannelBuffer channelBuffer;
    private IsisTlv isisTlv;

    @Before
    public void setUp() throws Exception {
        tlvFinder = new TlvFinder();
        tlvHeader = new TlvHeader();
    }

    @After
    public void tearDown() throws Exception {
        tlvFinder = null;
        isisTlv = null;
    }

    /**
     * Tests IsisTlv() getter method.
     */
    @Test
    public void testIsisTlv() throws Exception {
        channelBuffer = ChannelBuffers.copiedBuffer(tlv);

        tlvHeader.setTlvType(TlvType.AREAADDRESS.value());
        isisTlv = tlvFinder.findTlv(tlvHeader, channelBuffer);
        assertThat(isisTlv, instanceOf(AreaAddressTlv.class));

        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        tlvHeader.setTlvType(TlvType.HOSTNAME.value());
        isisTlv = tlvFinder.findTlv(tlvHeader, channelBuffer);
        assertThat(isisTlv, instanceOf(HostNameTlv.class));

        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        tlvHeader.setTlvType(TlvType.IDRPINFORMATION.value());
        isisTlv = tlvFinder.findTlv(tlvHeader, channelBuffer);
        assertThat(isisTlv, instanceOf(IdrpInformationTlv.class));

        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        tlvHeader.setTlvType(TlvType.IPEXTENDEDREACHABILITY.value());
        isisTlv = tlvFinder.findTlv(tlvHeader, channelBuffer);
        assertThat(isisTlv, instanceOf(IpExtendedReachabilityTlv.class));

        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        tlvHeader.setTlvType(TlvType.IPINTERFACEADDRESS.value());
        isisTlv = tlvFinder.findTlv(tlvHeader, channelBuffer);
        assertThat(isisTlv, instanceOf(IpInterfaceAddressTlv.class));

        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        tlvHeader.setTlvType(TlvType.IPINTERNALREACHABILITY.value());
        isisTlv = tlvFinder.findTlv(tlvHeader, channelBuffer);
        assertThat(isisTlv, instanceOf(IpInternalReachabilityTlv.class));

        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        tlvHeader.setTlvType(TlvType.PROTOCOLSUPPORTED.value());
        isisTlv = tlvFinder.findTlv(tlvHeader, channelBuffer);
        assertThat(isisTlv, instanceOf(ProtocolSupportedTlv.class));

        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        tlvHeader.setTlvType(TlvType.ISREACHABILITY.value());
        isisTlv = tlvFinder.findTlv(tlvHeader, channelBuffer);
        assertThat(isisTlv, instanceOf(IsReachabilityTlv.class));

        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        tlvHeader.setTlvType(TlvType.ISNEIGHBORS.value());
        isisTlv = tlvFinder.findTlv(tlvHeader, channelBuffer);
        assertThat(isisTlv, instanceOf(IsisNeighborTlv.class));

        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        tlvHeader.setTlvType(TlvType.LSPENTRY.value());
        isisTlv = tlvFinder.findTlv(tlvHeader, channelBuffer);
        assertThat(isisTlv, instanceOf(LspEntriesTlv.class));

        channelBuffer = ChannelBuffers.copiedBuffer(tlv);
        tlvHeader.setTlvType(TlvType.PADDING.value());
        isisTlv = tlvFinder.findTlv(tlvHeader, channelBuffer);
        assertThat(isisTlv, instanceOf(PaddingTlv.class));

        channelBuffer = ChannelBuffers.copiedBuffer(tlv1);
        tlvHeader.setTlvType(TlvType.ADJACENCYSTATE.value());
        isisTlv = tlvFinder.findTlv(tlvHeader, channelBuffer);
        assertThat(isisTlv, instanceOf(AdjacencyStateTlv.class));
    }
}