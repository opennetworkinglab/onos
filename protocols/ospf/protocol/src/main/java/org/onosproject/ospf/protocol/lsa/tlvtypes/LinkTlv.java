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
package org.onosproject.ospf.protocol.lsa.tlvtypes;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.Bytes;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.ospf.protocol.lsa.TlvHeader;
import org.onosproject.ospf.protocol.lsa.linksubtype.AdministrativeGroup;
import org.onosproject.ospf.protocol.lsa.linksubtype.LinkId;
import org.onosproject.ospf.protocol.lsa.linksubtype.LinkSubType;
import org.onosproject.ospf.protocol.lsa.linksubtype.LinkSubTypes;
import org.onosproject.ospf.protocol.lsa.linksubtype.LinkType;
import org.onosproject.ospf.protocol.lsa.linksubtype.LocalInterfaceIpAddress;
import org.onosproject.ospf.protocol.lsa.linksubtype.MaximumBandwidth;
import org.onosproject.ospf.protocol.lsa.linksubtype.MaximumReservableBandwidth;
import org.onosproject.ospf.protocol.lsa.linksubtype.RemoteInterfaceIpAddress;
import org.onosproject.ospf.protocol.lsa.linksubtype.TrafficEngineeringMetric;
import org.onosproject.ospf.protocol.lsa.linksubtype.UnknownLinkSubType;
import org.onosproject.ospf.protocol.lsa.linksubtype.UnreservedBandwidth;
import org.onosproject.ospf.protocol.lsa.types.TopLevelTlv;
import org.onosproject.ospf.protocol.util.OspfUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of an OSPF Opaque link tlv.
 */
public class LinkTlv extends TlvHeader implements TopLevelTlv {
    private List<LinkSubType> subTlv = new ArrayList<>();

    /**
     * Creates an instance of link tlv.
     *
     * @param header tlv header
     */
    public LinkTlv(TlvHeader header) {
        this.setTlvType(header.tlvType());
        this.setTlvLength(header.tlvLength());
    }

    /**
     * Gets sub tlv lists.
     *
     * @return sub tlv lists
     */
    public List<LinkSubType> subTlvList() {
        return this.subTlv;
    }

    /**
     * Reads bytes from channel buffer .
     *
     * @param channelBuffer channel buffer instance
     * @throws Exception might throws exception while parsing packet
     */
    public void readFrom(ChannelBuffer channelBuffer) throws Exception {
        while (channelBuffer.readableBytes() > 0) {
            TlvHeader tlvHeader = new TlvHeader();
            tlvHeader.setTlvType(channelBuffer.readUnsignedShort());
            tlvHeader.setTlvLength(channelBuffer.readUnsignedShort());

            if (LinkSubTypes.LINK_TYPE.value() == tlvHeader.tlvType()) {
                LinkType linktype = new LinkType(tlvHeader);
                linktype.readFrom(channelBuffer.readBytes(tlvHeader.tlvLength()));
                subTlv.add(linktype);
                if (tlvHeader.tlvLength() < OspfUtil.FOUR_BYTES) {
                    int readerIndex = channelBuffer.readerIndex() + (OspfUtil.FOUR_BYTES - tlvHeader.tlvLength());
                    channelBuffer.readerIndex(readerIndex);
                }
            } else if (LinkSubTypes.LINK_ID.value() == tlvHeader.tlvType()) {
                LinkId linkId = new LinkId(tlvHeader);
                linkId.readFrom(channelBuffer.readBytes(tlvHeader.tlvLength()));
                subTlv.add(linkId);
            } else if (LinkSubTypes.LOCAL_INTERFACE_IP_ADDRESS.value() == tlvHeader.tlvType()) {
                LocalInterfaceIpAddress localInterfaceIpAddress = new LocalInterfaceIpAddress(tlvHeader);
                localInterfaceIpAddress.readFrom(channelBuffer.readBytes(tlvHeader.tlvLength()));
                subTlv.add(localInterfaceIpAddress);
            } else if (LinkSubTypes.REMOTE_INTERFACE_IP_ADDRESS.value() == tlvHeader.tlvType()) {
                RemoteInterfaceIpAddress remoteInterfaceIpAddress = new RemoteInterfaceIpAddress(tlvHeader);
                remoteInterfaceIpAddress.readFrom(channelBuffer.readBytes(tlvHeader.tlvLength()));
                subTlv.add(remoteInterfaceIpAddress);
            } else if (LinkSubTypes.TRAFFIC_ENGINEERING_METRIC.value() == tlvHeader.tlvType()) {
                TrafficEngineeringMetric trafficEngineeringMetric = new TrafficEngineeringMetric(tlvHeader);
                trafficEngineeringMetric.readFrom(channelBuffer.readBytes(tlvHeader.tlvLength()));
                subTlv.add(trafficEngineeringMetric);
            } else if (LinkSubTypes.MAXIMUM_BANDWIDTH.value() == tlvHeader.tlvType()) {
                MaximumBandwidth maximumBandwidth = new MaximumBandwidth(tlvHeader);
                maximumBandwidth.readFrom(channelBuffer.readBytes(tlvHeader.tlvLength()));
                subTlv.add(maximumBandwidth);
            } else if (LinkSubTypes.MAXIMUM_RESERVABLE_BANDWIDTH.value() == tlvHeader.tlvType()) {
                MaximumReservableBandwidth maximumReservableBandwidth = new MaximumReservableBandwidth(tlvHeader);
                maximumReservableBandwidth.readFrom(channelBuffer.readBytes(tlvHeader.tlvLength()));
                subTlv.add(maximumReservableBandwidth);
            } else if (LinkSubTypes.UNRESERVED_BANDWIDTH.value() == tlvHeader.tlvType()) {
                UnreservedBandwidth unreservedBandwidth = new UnreservedBandwidth(tlvHeader);
                unreservedBandwidth.readFrom(channelBuffer.readBytes(tlvHeader.tlvLength()));
                subTlv.add(unreservedBandwidth);
            } else if (LinkSubTypes.ADMINISTRATIVE_GROUP.value() == tlvHeader.tlvType()) {
                AdministrativeGroup administrativeGroup = new AdministrativeGroup(tlvHeader);
                administrativeGroup.readFrom(channelBuffer.readBytes(tlvHeader.tlvLength()));
                subTlv.add(administrativeGroup);
            } else {
                UnknownLinkSubType unknownLinkSubType = new UnknownLinkSubType(tlvHeader);
                unknownLinkSubType.readFrom(channelBuffer.readBytes(tlvHeader.tlvLength()));
                subTlv.add(unknownLinkSubType);
            }
        }
    }

    /**
     * Gets link tlv as byte array.
     *
     * @return link tlv as byte array
     * @throws Exception might throws exception while parsing buffer
     */
    public byte[] asBytes() throws Exception {
        byte[] lsaMessage = null;

        byte[] tlvHeader = getTlvHeaderAsByteArray();
        byte[] tlvBody = getTlvBodyAsByteArray();
        lsaMessage = Bytes.concat(tlvHeader, tlvBody);

        return lsaMessage;
    }

    /**
     * Gets tlv body as byte array.
     *
     * @return tlv body as byte array
     * @throws Exception might throws exception while parsing buffer
     */
    public byte[] getTlvBodyAsByteArray() throws Exception {

        List<Byte> bodyLst = new ArrayList<>();
        for (LinkSubType tlv : subTlv) {
            //Check the type of tlv and build bytes accordingly
            if (tlv instanceof LinkType) {
                LinkType linkType = (LinkType) tlv;
                bodyLst.addAll(Bytes.asList(linkType.asBytes()));
            } else if (tlv instanceof LinkId) {
                LinkId linkId = (LinkId) tlv;
                bodyLst.addAll(Bytes.asList(linkId.asBytes()));
            } else if (tlv instanceof LocalInterfaceIpAddress) {
                LocalInterfaceIpAddress localInterfaceIpAddress = (LocalInterfaceIpAddress) tlv;
                bodyLst.addAll(Bytes.asList(localInterfaceIpAddress.asBytes()));
            } else if (tlv instanceof RemoteInterfaceIpAddress) {
                RemoteInterfaceIpAddress remoteInterfaceIpAddress = (RemoteInterfaceIpAddress) tlv;
                bodyLst.addAll(Bytes.asList(remoteInterfaceIpAddress.asBytes()));
            } else if (tlv instanceof TrafficEngineeringMetric) {
                TrafficEngineeringMetric trafficEngineeringMetric = (TrafficEngineeringMetric) tlv;
                bodyLst.addAll(Bytes.asList(trafficEngineeringMetric.asBytes()));
            } else if (tlv instanceof MaximumBandwidth) {
                MaximumBandwidth maximumBandwidth = (MaximumBandwidth) tlv;
                bodyLst.addAll(Bytes.asList(maximumBandwidth.asBytes()));
            } else if (tlv instanceof MaximumReservableBandwidth) {
                MaximumReservableBandwidth maximumReservableBandwidth = (MaximumReservableBandwidth) tlv;
                bodyLst.addAll(Bytes.asList(maximumReservableBandwidth.asBytes()));
            } else if (tlv instanceof UnreservedBandwidth) {
                UnreservedBandwidth unreservedBandwidth = (UnreservedBandwidth) tlv;
                bodyLst.addAll(Bytes.asList(unreservedBandwidth.asBytes()));
            } else if (tlv instanceof AdministrativeGroup) {
                AdministrativeGroup administrativeGroup = (AdministrativeGroup) tlv;
                bodyLst.addAll(Bytes.asList(administrativeGroup.asBytes()));
            } else {
                UnknownLinkSubType unknownLinkSubType = (UnknownLinkSubType) tlv;
                bodyLst.addAll(Bytes.asList(unknownLinkSubType.asBytes()));
            }
        }
        return Bytes.toArray(bodyLst);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("subTlv", subTlv)
                .toString();
    }
}