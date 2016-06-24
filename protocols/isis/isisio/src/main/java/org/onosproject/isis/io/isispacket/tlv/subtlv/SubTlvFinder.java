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

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.isis.io.isispacket.tlv.TlvHeader;

/**
 * Representation of sub tlv finder.
 */
public final class SubTlvFinder {
    /**
     * Creates an instance.
     */
    private SubTlvFinder() {

    }

    /**
     * Sets the value for TLV header and to find sub TLV and populate.
     *
     * @param tlvHeader     tlvHeader
     * @param channelBuffer byteBuf
     * @return subTlv  traffic engineering sub tlv
     */
    public static TrafficEngineeringSubTlv findSubTlv(TlvHeader tlvHeader, ChannelBuffer channelBuffer) {

        TrafficEngineeringSubTlv subTlv = null;

        switch (SubTlvType.get(tlvHeader.tlvType())) {
            case ADMINISTRATIVEGROUP:
                AdministrativeGroup administrativeGroup = new AdministrativeGroup(tlvHeader);
                administrativeGroup.readFrom(channelBuffer);
                subTlv = administrativeGroup;
                break;
            case MAXIMUMBANDWIDTH:
                MaximumBandwidth maximumBandwidth = new MaximumBandwidth(tlvHeader);
                maximumBandwidth.readFrom(channelBuffer);
                subTlv = maximumBandwidth;
                break;
            case MAXIMUMRESERVABLEBANDWIDTH:
                MaximumReservableBandwidth maxResBandwidth = new MaximumReservableBandwidth(tlvHeader);
                maxResBandwidth.readFrom(channelBuffer);
                subTlv = maxResBandwidth;
                break;
            case TRAFFICENGINEERINGMETRIC:
                TrafficEngineeringMetric teMetric = new TrafficEngineeringMetric(tlvHeader);
                teMetric.readFrom(channelBuffer);
                subTlv = teMetric;
                break;
            case UNRESERVEDBANDWIDTH:
                UnreservedBandwidth unreservedBandwidth = new UnreservedBandwidth(tlvHeader);
                unreservedBandwidth.readFrom(channelBuffer);
                subTlv = unreservedBandwidth;
                break;
            case INTERFACEADDRESS:
                InterfaceIpAddress ipInterfaceAddressTlv = new InterfaceIpAddress(tlvHeader);
                ipInterfaceAddressTlv.readFrom(channelBuffer);
                subTlv = ipInterfaceAddressTlv;
                break;
            case NEIGHBORADDRESS:
                NeighborIpAddress ipNeighborAddressTlv = new NeighborIpAddress(tlvHeader);
                ipNeighborAddressTlv.readFrom(channelBuffer);
                subTlv = ipNeighborAddressTlv;
                break;
            default:
                //TODO
                break;
        }
        return subTlv;
    }
}