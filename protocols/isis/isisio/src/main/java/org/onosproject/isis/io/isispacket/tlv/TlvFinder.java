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

/**
 * Representation of TLV Finder.
 */
public class TlvFinder extends TlvHeader {

    /**
     * Sets the value for TLV header.
     *
     * @param tlvHeader     tlvHeader
     * @param channelBuffer byteBuf
     * @return isisTlv
     */
    public static IsisTlv findTlv(TlvHeader tlvHeader, ChannelBuffer channelBuffer) {

        IsisTlv isisTlv = null;
        switch (TlvType.get(tlvHeader.tlvType())) {
            case AREAADDRESS:
                AreaAddressTlv areaAddressTlv = new AreaAddressTlv(tlvHeader);
                areaAddressTlv.readFrom(channelBuffer);
                isisTlv = areaAddressTlv;
                break;
            case AUTHENTICATION:
                //TODO
                break;
            case EXTENDEDISREACHABILITY:
                IsExtendedReachability isExtendedReachability =
                        new IsExtendedReachability(tlvHeader);
                isExtendedReachability.readFrom(channelBuffer);
                isisTlv = isExtendedReachability;
                break;
            case HOSTNAME:
                HostNameTlv hostNameTlv = new HostNameTlv(tlvHeader);
                hostNameTlv.readFrom(channelBuffer);
                isisTlv = hostNameTlv;
                break;
            case IDRPINFORMATION:
                IdrpInformationTlv idrpInformationTlv = new IdrpInformationTlv(tlvHeader);
                idrpInformationTlv.readFrom(channelBuffer);
                isisTlv = idrpInformationTlv;
                break;
            case IPEXTENDEDREACHABILITY:
                IpExtendedReachabilityTlv iperTlv = new IpExtendedReachabilityTlv(tlvHeader);
                iperTlv.readFrom(channelBuffer);
                isisTlv = iperTlv;
                break;
            case IPINTERFACEADDRESS:
                IpInterfaceAddressTlv ipTlv = new IpInterfaceAddressTlv(tlvHeader);
                ipTlv.readFrom(channelBuffer);
                isisTlv = ipTlv;
                break;
            case IPINTERNALREACHABILITY:
                IpInternalReachabilityTlv iprTlv = new IpInternalReachabilityTlv(tlvHeader);
                iprTlv.readFrom(channelBuffer);
                isisTlv = iprTlv;
                break;
            case ISALIAS:
                break;
            case PROTOCOLSUPPORTED:
                ProtocolSupportedTlv psTlv = new ProtocolSupportedTlv(tlvHeader);
                psTlv.readFrom(channelBuffer);
                isisTlv = psTlv;
                break;
            case ISREACHABILITY:
                IsReachabilityTlv isrTlv = new IsReachabilityTlv(tlvHeader);
                isrTlv.readFrom(channelBuffer);
                isisTlv = isrTlv;
                break;
            case ISNEIGHBORS:
                IsisNeighborTlv isisNeighborTlv = new IsisNeighborTlv(tlvHeader);
                isisNeighborTlv.readFrom(channelBuffer);
                isisTlv = isisNeighborTlv;
                break;
            case LSPENTRY:
                LspEntriesTlv lspEntriesTlv = new LspEntriesTlv(tlvHeader);
                lspEntriesTlv.readFrom(channelBuffer);
                isisTlv = lspEntriesTlv;
                break;
            case PADDING:
                PaddingTlv paddingTlv = new PaddingTlv(tlvHeader);
                paddingTlv.readFrom(channelBuffer);
                isisTlv = paddingTlv;
                break;
            case ADJACENCYSTATE:
                AdjacencyStateTlv adjacencyStateTlv = new AdjacencyStateTlv(tlvHeader);
                adjacencyStateTlv.readFrom(channelBuffer);
                isisTlv = adjacencyStateTlv;
                break;
            default:
                break;
        }
        return isisTlv;
    }
}