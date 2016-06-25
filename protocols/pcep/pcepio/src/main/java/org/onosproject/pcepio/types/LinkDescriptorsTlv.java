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
 * Provides TE Link Descriptors TLV.
 */
public class LinkDescriptorsTlv implements PcepValueType {

    /*
     * Reference: draft-dhodylee-pce-pcep-ls-01, section 9.2.6.
     *   0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |           Type=[TBD14]        |             Length            |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                                                               |
     //              Link Descriptor Sub-TLVs (variable)            //
     |                                                               |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

     */

    protected static final Logger log = LoggerFactory.getLogger(LinkDescriptorsTlv.class);

    public static final short TYPE = (short) 65284;
    public short hLength;

    public static final int TLV_HEADER_LENGTH = 4;

    // LinkDescriptors Sub-TLVs (variable)
    private List<PcepValueType> llLinkDescriptorsSubTLVs;

    /**
     * Constructor to initialize llLinkDescriptorsSubTLVs.
     *
     * @param llLinkDescriptorsSubTLVs of PcepValueType
     */
    public LinkDescriptorsTlv(List<PcepValueType> llLinkDescriptorsSubTLVs) {
        this.llLinkDescriptorsSubTLVs = llLinkDescriptorsSubTLVs;
    }

    /**
     * Returns object of LinkDescriptorsTlv.
     *
     * @param llLinkDescriptorsSubTLVs of PcepValueType
     * @return object of LinkDescriptorsTlv
     */
    public static LinkDescriptorsTlv of(final List<PcepValueType> llLinkDescriptorsSubTLVs) {
        return new LinkDescriptorsTlv(llLinkDescriptorsSubTLVs);
    }

    /**
     * Returns linked list of Link Attribute of Sub TLV.
     *
     * @return llLinkDescriptorsSubTLVs linked list of Link Attribute of Sub TLV
     */
    public List<PcepValueType> getllLinkDescriptorsSubTLVs() {
        return llLinkDescriptorsSubTLVs;
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
        return Objects.hash(llLinkDescriptorsSubTLVs.hashCode());
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
        if (obj instanceof LinkDescriptorsTlv) {
            int countObjSubTlv = 0;
            int countOtherSubTlv = 0;
            boolean isCommonSubTlv = true;
            LinkDescriptorsTlv other = (LinkDescriptorsTlv) obj;
            Iterator<PcepValueType> objListIterator = ((LinkDescriptorsTlv) obj).llLinkDescriptorsSubTLVs.iterator();
            countObjSubTlv = ((LinkDescriptorsTlv) obj).llLinkDescriptorsSubTLVs.size();
            countOtherSubTlv = other.llLinkDescriptorsSubTLVs.size();
            if (countObjSubTlv != countOtherSubTlv) {
                return false;
            } else {
                while (objListIterator.hasNext() && isCommonSubTlv) {
                    PcepValueType subTlv = objListIterator.next();
                    isCommonSubTlv = Objects.equals(llLinkDescriptorsSubTLVs.contains(subTlv),
                            other.llLinkDescriptorsSubTLVs.contains(subTlv));
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

        ListIterator<PcepValueType> listIterator = llLinkDescriptorsSubTLVs.listIterator();

        while (listIterator.hasNext()) {
            PcepValueType tlv = listIterator.next();

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
     * Reads channel buffer and returns object of LinkDescriptorsTlv.
     *
     * @param c input channel buffer
     * @param length length
     * @return object of LinkDescriptorsTlv
     * @throws PcepParseException if mandatory fields are missing
     */
    public static PcepValueType read(ChannelBuffer c, short length) throws PcepParseException {

        // Node Descriptor Sub-TLVs (variable)
        List<PcepValueType> llLinkDescriptorsSubTLVs = new LinkedList<>();

        ChannelBuffer tempCb = c.readBytes(length);

        while (TLV_HEADER_LENGTH <= tempCb.readableBytes()) {

            PcepValueType tlv;
            short hType = tempCb.readShort();
            int iValue = 0;
            short hLength = tempCb.readShort();
            log.debug("sub Tlv Length" + hLength);
            switch (hType) {

            case LinkLocalRemoteIdentifiersSubTlv.TYPE:
                tlv = LinkLocalRemoteIdentifiersSubTlv.read(tempCb);
                break;
            case IPv4InterfaceAddressSubTlv.TYPE:
                iValue = tempCb.readInt();
                tlv = new IPv4InterfaceAddressSubTlv(iValue);
                break;
            case IPv4NeighborAddressSubTlv.TYPE:
                iValue = tempCb.readInt();
                tlv = new IPv4NeighborAddressSubTlv(iValue);
                break;
            case IPv6InterfaceAddressSubTlv.TYPE:
                byte[] ipv6Value = new byte[IPv6InterfaceAddressSubTlv.VALUE_LENGTH];
                tempCb.readBytes(ipv6Value, 0, IPv6InterfaceAddressSubTlv.VALUE_LENGTH);
                tlv = new IPv6InterfaceAddressSubTlv(ipv6Value);
                break;
            case IPv6NeighborAddressSubTlv.TYPE:
                byte[] ipv6NeighborAdd = new byte[IPv6NeighborAddressSubTlv.VALUE_LENGTH];
                tempCb.readBytes(ipv6NeighborAdd, 0, IPv6NeighborAddressSubTlv.VALUE_LENGTH);
                tlv = new IPv6NeighborAddressSubTlv(ipv6NeighborAdd);
                break;
            default:
                throw new PcepParseException("Unsupported Sub TLV type:" + hType);
            }

            // Check for the padding
            int pad = hLength % 4;
            if (0 < pad) {
                pad = 4 - pad;
                if (pad <= tempCb.readableBytes()) {
                    tempCb.skipBytes(pad);
                }
            }
            llLinkDescriptorsSubTLVs.add(tlv);

        }

        if (0 < tempCb.readableBytes()) {

            throw new PcepParseException("Sub Tlv parsing error. Extra bytes received.");
        }
        return new LinkDescriptorsTlv(llLinkDescriptorsSubTLVs);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", hLength)
                .add("LinkDescriptorsSubTLVs", llLinkDescriptorsSubTLVs)
                .toString();
    }
}
