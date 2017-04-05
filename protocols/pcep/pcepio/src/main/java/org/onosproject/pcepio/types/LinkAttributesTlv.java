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
package org.onosproject.pcepio.types;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides LinkAttributesTlv.
 */
public class LinkAttributesTlv implements PcepValueType {

    /*
     * Reference :draft-dhodylee-pce-pcep-ls-01, section 9.2.8.2.
     *  0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |           Type=[TBD27]        |             Length            |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                                                               |
     //              Link Attributes Sub-TLVs (variable)            //
     |                                                               |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    protected static final Logger log = LoggerFactory.getLogger(LinkAttributesTlv.class);

    public static final short TYPE = (short) 65286;
    short hLength;

    public static final int TLV_HEADER_LENGTH = 4;

    // LinkDescriptors Sub-TLVs (variable)
    private List<PcepValueType> llLinkAttributesSubTLVs;

    /**
     * Constructor to initialize Link Attributes Sub TLVs.
     *
     * @param llLinkAttributesSubTLVs linked list of PcepValueType
     */
    public LinkAttributesTlv(List<PcepValueType> llLinkAttributesSubTLVs) {
        this.llLinkAttributesSubTLVs = llLinkAttributesSubTLVs;
    }

    /**
     * Returns object of TE Link Attributes TLV.
     *
     * @param llLinkAttributesSubTLVs linked list of Link Attribute of Sub TLV
     * @return object of LinkAttributesTlv
     */
    public static LinkAttributesTlv of(final List<PcepValueType> llLinkAttributesSubTLVs) {
        return new LinkAttributesTlv(llLinkAttributesSubTLVs);
    }

    /**
     * Returns linked list of Link Attribute of Sub TLV.
     *
     * @return llLinkAttributesSubTLVs linked list of Link Attribute of Sub TLV
     */
    public List<PcepValueType> getllLinkAttributesSubTLVs() {
        return llLinkAttributesSubTLVs;
    }

    @Override
    public PcepVersion getVersion() {
        return PcepVersion.PCEP_1;
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public short getLength() {
        return hLength;
    }

    @Override
    public int hashCode() {
        return Objects.hash(llLinkAttributesSubTLVs.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        /*
         * Here we have a list of Tlv so to compare each sub tlv between the object
         * we have to take a list iterator so one by one we can get each sub tlv object
         * and can compare them.
         * it may be possible that the size of 2 lists is not equal so we have to first check
         * the size, if both are same then we should check for the subtlv objects otherwise
         * we should return false.
         */
        if (obj instanceof LinkAttributesTlv) {
            int countObjSubTlv = 0;
            int countOtherSubTlv = 0;
            boolean isCommonSubTlv = true;
            LinkAttributesTlv other = (LinkAttributesTlv) obj;
            Iterator<PcepValueType> objListIterator = ((LinkAttributesTlv) obj).llLinkAttributesSubTLVs.iterator();
            countObjSubTlv = ((LinkAttributesTlv) obj).llLinkAttributesSubTLVs.size();
            countOtherSubTlv = other.llLinkAttributesSubTLVs.size();
            if (countObjSubTlv != countOtherSubTlv) {
                return false;
            } else {
                while (objListIterator.hasNext() && isCommonSubTlv) {
                    PcepValueType subTlv = objListIterator.next();
                    isCommonSubTlv = Objects.equals(llLinkAttributesSubTLVs.contains(subTlv),
                            other.llLinkAttributesSubTLVs.contains(subTlv));
                }
                return isCommonSubTlv;
            }
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int tlvStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        int tlvLenIndex = c.writerIndex();
        hLength = 0;
        c.writeShort(hLength);

        ListIterator<PcepValueType> listIterator = llLinkAttributesSubTLVs.listIterator();

        while (listIterator.hasNext()) {
            PcepValueType tlv = listIterator.next();

            if (tlv == null) {
                log.debug("TLV is null from subTlv list");
                continue;
            }
            tlv.write(c);

            // need to take care of padding
            int pad = tlv.getLength() % 4;

            if (0 != pad) {
                pad = 4 - pad;
                for (int i = 0; i < pad; ++i) {
                    c.writeByte((byte) 0);
                }
            }
        }

        hLength = (short) (c.writerIndex() - tlvStartIndex);
        c.setShort(tlvLenIndex, (hLength - TLV_HEADER_LENGTH));

        return c.writerIndex() - tlvStartIndex;
    }

    /**
     * Reads channel buffer and returns object of TE Link Attributes TLV.
     *
     * @param c input channel buffer
     * @param hLength length
     * @return object of LinkAttributesTlv
     * @throws PcepParseException if mandatory fields are missing
     */
    public static PcepValueType read(ChannelBuffer c, short hLength) throws PcepParseException {

        // Node Descriptor Sub-TLVs (variable)
        List<PcepValueType> llLinkAttributesSubTLVs = new LinkedList<>();

        ChannelBuffer tempCb = c.readBytes(hLength);

        while (TLV_HEADER_LENGTH <= tempCb.readableBytes()) {

            PcepValueType tlv;
            short hType = tempCb.readShort();
            int iValue = 0;
            short length = tempCb.readShort();
            switch (hType) {

            case IPv4RouterIdOfLocalNodeSubTlv.TYPE:
                iValue = tempCb.readInt();
                tlv = new IPv4RouterIdOfLocalNodeSubTlv(iValue);
                break;
            case IPv6RouterIdofLocalNodeSubTlv.TYPE:
                byte[] ipv6LValue = new byte[IPv6RouterIdofLocalNodeSubTlv.VALUE_LENGTH];
                tempCb.readBytes(ipv6LValue, 0, IPv6RouterIdofLocalNodeSubTlv.VALUE_LENGTH);
                tlv = new IPv6RouterIdofLocalNodeSubTlv(ipv6LValue);
                break;
            case IPv4RouterIdOfRemoteNodeSubTlv.TYPE:
                iValue = tempCb.readInt();
                tlv = new IPv4RouterIdOfRemoteNodeSubTlv(iValue);
                break;
            case IPv6RouterIdofRemoteNodeSubTlv.TYPE:
                byte[] ipv6RValue = new byte[IPv6RouterIdofRemoteNodeSubTlv.VALUE_LENGTH];
                tempCb.readBytes(ipv6RValue, 0, IPv6RouterIdofRemoteNodeSubTlv.VALUE_LENGTH);
                tlv = new IPv6RouterIdofRemoteNodeSubTlv(ipv6RValue);
                break;
            case LinkLocalRemoteIdentifiersSubTlv.TYPE:
                tlv = LinkLocalRemoteIdentifiersSubTlv.read(tempCb);
                break;
            case AdministrativeGroupSubTlv.TYPE:
                iValue = tempCb.readInt();
                tlv = new AdministrativeGroupSubTlv(iValue);
                break;
            case MaximumLinkBandwidthSubTlv.TYPE:
                iValue = tempCb.readInt();
                tlv = new MaximumLinkBandwidthSubTlv(iValue);
                break;
            case MaximumReservableLinkBandwidthSubTlv.TYPE:
                iValue = tempCb.readInt();
                tlv = new MaximumReservableLinkBandwidthSubTlv(iValue);
                break;
            case UnreservedBandwidthSubTlv.TYPE:
                iValue = tempCb.readInt();
                tlv = new UnreservedBandwidthSubTlv(iValue);
                break;
            case TEDefaultMetricSubTlv.TYPE:
                iValue = tempCb.readInt();
                tlv = new TEDefaultMetricSubTlv(iValue);
                break;
            case LinkProtectionTypeSubTlv.TYPE:
                tlv = LinkProtectionTypeSubTlv.read(tempCb);
                break;
            case MplsProtocolMaskSubTlv.TYPE:
                byte cValue = tempCb.readByte();
                tlv = new MplsProtocolMaskSubTlv(cValue);
                break;
            case IgpMetricSubTlv.TYPE:
                tlv = IgpMetricSubTlv.read(tempCb, length);
                break;
            case SharedRiskLinkGroupSubTlv.TYPE:
                tlv = SharedRiskLinkGroupSubTlv.read(tempCb, length);
                break;
            case OpaqueLinkAttributeSubTlv.TYPE:
                tlv = OpaqueLinkAttributeSubTlv.read(tempCb, length);
                break;
            case LinkNameAttributeSubTlv.TYPE:
                tlv = LinkNameAttributeSubTlv.read(tempCb, length);
                break;
            default:
                throw new PcepParseException("Unsupported Sub TLV type :" + hType);
            }

            // Check for the padding
            int pad = length % 4;
            if (0 < pad) {
                pad = 4 - pad;
                if (pad <= tempCb.readableBytes()) {
                    tempCb.skipBytes(pad);
                }
            }
            llLinkAttributesSubTLVs.add(tlv);
        }

        if (0 < tempCb.readableBytes()) {

            throw new PcepParseException("Sub Tlv parsing error. Extra bytes received.");
        }

        return new LinkAttributesTlv(llLinkAttributesSubTLVs);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", hLength)
                .add("LinkAttributesSubTLVs", llLinkAttributesSubTLVs)
                .toString();
    }
}
