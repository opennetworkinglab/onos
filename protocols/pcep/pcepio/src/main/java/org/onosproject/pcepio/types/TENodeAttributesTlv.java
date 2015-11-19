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
 * Provides TE Node Attributes Tlv.
 */
public class TENodeAttributesTlv implements PcepValueType {
    /*
     * Reference :PCEP Extension for Transporting TE Data draft-dhodylee-pce-pcep-te-data-extn-02
     *
      0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |           Type=[TBD20]        |             Length            |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                                                               |
     //              Node Attributes Sub-TLVs (variable)            //
     |                                                               |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+


     */

    protected static final Logger log = LoggerFactory.getLogger(TENodeAttributesTlv.class);

    public static final short TYPE = 1267; //TODD:change this TBD20
    public short hLength;

    public static final int TLV_HEADER_LENGTH = 4;
    // LinkDescriptors Sub-TLVs (variable)
    private LinkedList<PcepValueType> llNodeAttributesSubTLVs;

    /**
     * Constructor to initialize llNodeAttributesSubTLVs.
     *
     * @param llNodeAttributesSubTLVs linked list of Node Attributes Sub-TLVs
     */
    public TENodeAttributesTlv(LinkedList<PcepValueType> llNodeAttributesSubTLVs) {
        this.llNodeAttributesSubTLVs = llNodeAttributesSubTLVs;
    }

    /**
     * Returns object of TENodeAttributesTlv.
     *
     * @param llNodeAttributesSubTLVs LinkedList of PcepValueType
     * @return object of TENodeAttributesTlv
     */
    public static TENodeAttributesTlv of(LinkedList<PcepValueType> llNodeAttributesSubTLVs) {
        return new TENodeAttributesTlv(llNodeAttributesSubTLVs);
    }

    /**
     * Returns Node Attributes Sub-TLVs.
     *
     * @return llNodeAttributesSubTLVs linked list of Node Attributes Sub-TLVs
     */
    public LinkedList<PcepValueType> getllNodeAttributesSubTLVs() {
        return llNodeAttributesSubTLVs;
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
        return Objects.hash(llNodeAttributesSubTLVs.hashCode());
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
        if (obj instanceof TENodeAttributesTlv) {
            int countObjSubTlv = 0;
            int countOtherSubTlv = 0;
            boolean isCommonSubTlv = true;
            TENodeAttributesTlv other = (TENodeAttributesTlv) obj;
            Iterator<PcepValueType> objListIterator = ((TENodeAttributesTlv) obj).llNodeAttributesSubTLVs.iterator();
            countObjSubTlv = ((TENodeAttributesTlv) obj).llNodeAttributesSubTLVs.size();
            countOtherSubTlv = other.llNodeAttributesSubTLVs.size();
            if (countObjSubTlv != countOtherSubTlv) {
                return false;
            } else {
                while (objListIterator.hasNext() && isCommonSubTlv) {
                    PcepValueType subTlv = objListIterator.next();
                    isCommonSubTlv = Objects.equals(llNodeAttributesSubTLVs.contains(subTlv),
                            other.llNodeAttributesSubTLVs.contains(subTlv));
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

        ListIterator<PcepValueType> listIterator = llNodeAttributesSubTLVs.listIterator();

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
     * Reads the channel buffer and returns object of TENodeAttributesTlv.
     *
     * @param c input channel buffer
     * @param hLength length
     * @return object of TENodeAttributesTlv
     * @throws PcepParseException if mandatory fields are missing
     */
    public static PcepValueType read(ChannelBuffer c, short hLength) throws PcepParseException {

        // Node Descriptor Sub-TLVs (variable)
        LinkedList<PcepValueType> llNodeAttributesSubTLVs = new LinkedList<>();

        ChannelBuffer tempCb = c.readBytes(hLength);

        while (TLV_HEADER_LENGTH <= tempCb.readableBytes()) {
            PcepValueType tlv;
            short hType = tempCb.readShort();
            int iValue = 0;
            short length = tempCb.readShort();
            switch (hType) {

            case NodeFlagBitsTlv.TYPE:
                byte cValue = tempCb.readByte();
                tlv = new NodeFlagBitsTlv(cValue);
                break;
            case OpaqueNodeAttributeTlv.TYPE:
                tlv = OpaqueNodeAttributeTlv.read(tempCb, length);
                break;
            case NodeNameTlv.TYPE:
                tlv = NodeNameTlv.read(tempCb, length);
                break;
            case ISISAreaIdentifierTlv.TYPE:
                tlv = ISISAreaIdentifierTlv.read(tempCb, length);
                break;
            case IPv4TERouterIdOfLocalNodeTlv.TYPE:
                iValue = tempCb.readInt();
                tlv = new IPv4TERouterIdOfLocalNodeTlv(iValue);
                break;
            case IPv6TERouterIdofLocalNodeTlv.TYPE:
                byte[] ipv6Value = new byte[IPv6TERouterIdofLocalNodeTlv.VALUE_LENGTH];
                tempCb.readBytes(ipv6Value, 0, IPv6TERouterIdofLocalNodeTlv.VALUE_LENGTH);
                tlv = new IPv6TERouterIdofLocalNodeTlv(ipv6Value);
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

            llNodeAttributesSubTLVs.add(tlv);
        }

        if (0 < tempCb.readableBytes()) {

            throw new PcepParseException("Sub Tlv parsing error. Extra bytes received.");
        }
        return new TENodeAttributesTlv(llNodeAttributesSubTLVs);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", hLength)
                .add("NodeAttributesSubTLVs", llNodeAttributesSubTLVs)
                .toString();
    }
}
