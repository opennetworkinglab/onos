/*
 * Copyright 2015 Open Networking Laboratory
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
import java.util.ListIterator;
import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides TELinkAttributesTlv.
 */
public class TELinkAttributesTlv implements PcepValueType {

    /*
     * Reference :PCEP Extension for Transporting TE Data draft-dhodylee-pce-pcep-te-data-extn-02
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

    protected static final Logger log = LoggerFactory.getLogger(TELinkAttributesTlv.class);

    public static final short TYPE = 1897; //TODD:change this TBD27
    public short hLength;

    public static final int TLV_HEADER_LENGTH = 4;

    // LinkDescriptors Sub-TLVs (variable)
    private LinkedList<PcepValueType> llLinkAttributesSubTLVs;

    /**
     * Constructor to initialize Link Attributes Sub TLVs.
     *
     * @param llLinkAttributesSubTLVs linked list of PcepValueType
     */
    public TELinkAttributesTlv(LinkedList<PcepValueType> llLinkAttributesSubTLVs) {
        this.llLinkAttributesSubTLVs = llLinkAttributesSubTLVs;
    }

    /**
     * Returns object of TE Link Attributes TLV.
     *
     * @param llLinkAttributesSubTLVs linked list of Link Attribute of Sub TLV
     * @return object of TELinkAttributesTlv
     */
    public static TELinkAttributesTlv of(final LinkedList<PcepValueType> llLinkAttributesSubTLVs) {
        return new TELinkAttributesTlv(llLinkAttributesSubTLVs);
    }

    /**
     * Returns linked list of Link Attribute of Sub TLV.
     *
     * @return llLinkAttributesSubTLVs linked list of Link Attribute of Sub TLV
     */
    public LinkedList<PcepValueType> getllLinkAttributesSubTLVs() {
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
        if (obj instanceof TELinkAttributesTlv) {
            int countObjSubTlv = 0;
            int countOtherSubTlv = 0;
            boolean isCommonSubTlv = true;
            TELinkAttributesTlv other = (TELinkAttributesTlv) obj;
            Iterator<PcepValueType> objListIterator = ((TELinkAttributesTlv) obj).llLinkAttributesSubTLVs.iterator();
            countObjSubTlv = ((TELinkAttributesTlv) obj).llLinkAttributesSubTLVs.size();
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
     * @return object of TELinkAttributesTlv
     * @throws PcepParseException if mandatory fields are missing
     */
    public static PcepValueType read(ChannelBuffer c, short hLength) throws PcepParseException {

        // Node Descriptor Sub-TLVs (variable)
        LinkedList<PcepValueType> llLinkAttributesSubTLVs = new LinkedList<>();

        ChannelBuffer tempCb = c.readBytes(hLength);

        while (TLV_HEADER_LENGTH <= tempCb.readableBytes()) {

            PcepValueType tlv;
            short hType = tempCb.readShort();
            int iValue = 0;
            short length = tempCb.readShort();
            switch (hType) {

            case IPv4TERouterIdOfLocalNodeTlv.TYPE:
                iValue = tempCb.readInt();
                tlv = new IPv4TERouterIdOfLocalNodeTlv(iValue);
                break;
            case IPv6TERouterIdofLocalNodeTlv.TYPE:
                byte[] ipv6LValue = new byte[IPv6TERouterIdofLocalNodeTlv.VALUE_LENGTH];
                tempCb.readBytes(ipv6LValue, 0, IPv6TERouterIdofLocalNodeTlv.VALUE_LENGTH);
                tlv = new IPv6TERouterIdofLocalNodeTlv(ipv6LValue);
                break;
            case IPv4TERouterIdOfRemoteNodeTlv.TYPE:
                iValue = tempCb.readInt();
                tlv = new IPv4TERouterIdOfRemoteNodeTlv(iValue);
                break;
            case IPv6TERouterIdofRemoteNodeTlv.TYPE:
                byte[] ipv6RValue = new byte[IPv6TERouterIdofRemoteNodeTlv.VALUE_LENGTH];
                tempCb.readBytes(ipv6RValue, 0, IPv6TERouterIdofRemoteNodeTlv.VALUE_LENGTH);
                tlv = new IPv6TERouterIdofRemoteNodeTlv(ipv6RValue);
                break;
            case LinkLocalRemoteIdentifiersTlv.TYPE:
                tlv = LinkLocalRemoteIdentifiersTlv.read(tempCb);
                break;
            case AdministrativeGroupTlv.TYPE:
                iValue = tempCb.readInt();
                tlv = new AdministrativeGroupTlv(iValue);
                break;
            case MaximumLinkBandwidthTlv.TYPE:
                iValue = tempCb.readInt();
                tlv = new MaximumLinkBandwidthTlv(iValue);
                break;
            case MaximumReservableLinkBandwidthTlv.TYPE:
                iValue = tempCb.readInt();
                tlv = new MaximumReservableLinkBandwidthTlv(iValue);
                break;
            case UnreservedBandwidthTlv.TYPE:
                iValue = tempCb.readInt();
                tlv = new UnreservedBandwidthTlv(iValue);
                break;
            case TEDefaultMetricTlv.TYPE:
                iValue = tempCb.readInt();
                tlv = new TEDefaultMetricTlv(iValue);
                break;
            case LinkProtectionTypeTlv.TYPE:
                tlv = LinkProtectionTypeTlv.read(tempCb);
                break;
            case MPLSProtocolMaskTlv.TYPE:
                byte cValue = tempCb.readByte();
                tlv = new MPLSProtocolMaskTlv(cValue);
                break;
            case IGPMetricTlv.TYPE:
                tlv = IGPMetricTlv.read(tempCb, length);
                break;
            case SharedRiskLinkGroupTlv.TYPE:
                tlv = SharedRiskLinkGroupTlv.read(tempCb, length);
                break;
            case OpaqueLinkAttributeTlv.TYPE:
                tlv = OpaqueLinkAttributeTlv.read(tempCb, length);
                break;
            case LinkNameTlv.TYPE:
                tlv = LinkNameTlv.read(tempCb, length);
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

        return new TELinkAttributesTlv(llLinkAttributesSubTLVs);
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
