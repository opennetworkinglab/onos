/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.pcepio.protocol.ver1;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepEroObject;
import org.onosproject.pcepio.types.AutonomousSystemNumberSubObject;
import org.onosproject.pcepio.types.IPv4SubObject;
import org.onosproject.pcepio.types.IPv6SubObject;
import org.onosproject.pcepio.types.PathKeySubObject;
import org.onosproject.pcepio.types.PcepErrorDetailInfo;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.onosproject.pcepio.types.PcepValueType;
import org.onosproject.pcepio.types.SrEroSubObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP Ero Object.
 */
public class PcepEroObjectVer1 implements PcepEroObject {
    /*
     * rfc3209
      0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     | Object-Class  |   OT  |Res|P|I|   Object Length (bytes)       |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                                                               |
     //                        (Subobjects)                          //
     |                                                               |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

     If a Path message contains multiple EXPLICIT_ROUTE objects, only the
     first object is meaningful.  Subsequent EXPLICIT_ROUTE objects MAY be
     ignored and SHOULD NOT be propagated.

     In current implementation, only strict hops are supported. So,
     empty ERO with no sub-objects is considered illegal.

     Subobjects:
      0                   1
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-------------//----------------+
     |L|    Type     |     Length    | (Subobject contents)          |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-------------//----------------+

      L

         The L bit is an attribute of the subobject.  The L bit is set
         if the subobject represents a loose hop in the explicit route.
         If the bit is not set, the subobject represents a strict hop in
         the explicit route.

      Type

         The Type indicates the type of contents of the subobject.


      Subobject 1: IPv4 address

      0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |L|    Type     |     Length    | IPv4 address (4 bytes)        |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     | IPv4 address (continued)      | Prefix Length |      Resvd    |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

     Subobject 2:  IPv6 Prefix

      0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |L|    Type     |     Length    | IPv6 address (16 bytes)       |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     | IPv6 address (continued)                                      |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     | IPv6 address (continued)                                      |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     | IPv6 address (continued)                                      |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     | IPv6 address (continued)      | Prefix Length |      Resvd    |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

     Subobject 3:  Autonomous System Number

     The contents of an Autonomous System (AS) number subobject are a 2-
     octet AS number.  The abstract node represented by this subobject is
     the set of nodes belonging to the autonomous system.

     The length of the AS number subobject is 4 octets.

     Subobject 4: PATH_KEY_32_BIT_SUB_OBJ_TYPE:

      Pathkey subobject(RFC 5520):
      0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |L|    Type     |     Length    |           Path-Key            |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                         PCE ID (4 bytes)                      |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

     Subobject 5: SR_ERO_SUB_OBJ_TYPE:

       SR-ERO subobject: (draft-ietf-pce-segment-routing-00)
      0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |L|    Type     |     Length    |  ST   |     Flags     |F|S|C|M|
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                              SID                              |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     //                        NAI (variable)                       //
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    protected static final Logger log = LoggerFactory.getLogger(PcepEroObjectVer1.class);

    public static final byte ERO_OBJ_TYPE = 1;
    public static final byte ERO_OBJ_CLASS = 7;
    public static final byte ERO_OBJECT_VERSION = 1;
    public static final short ERO_OBJ_MINIMUM_LENGTH = 12;
    public static final byte IPV4_TYPE = 1;
    public static final byte PATH_KEY_32_BIT_SUB_OBJ_TYPE = 64;
    public static final int LABEL_SUB_OBJ_TYPE = 3;
    public static final int SR_ERO_SUB_OBJ_TYPE = 96;
    public static final int OBJECT_HEADER_LENGTH = 4;
    public static final int TYPE_SHIFT_VALUE = 0x7F;

    public static final PcepObjectHeader DEFAULT_ERO_OBJECT_HEADER = new PcepObjectHeader(ERO_OBJ_CLASS, ERO_OBJ_TYPE,
            PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED, ERO_OBJ_MINIMUM_LENGTH);

    private PcepObjectHeader eroObjHeader;
    private LinkedList<PcepValueType> subObjectList = new LinkedList<>();

    /**
     * reset variables.
     */
    public PcepEroObjectVer1() {
        this.eroObjHeader = null;
        this.subObjectList = null;
    }

    /**
     * Constructor to initialize parameters of ERO object.
     *
     * @param eroObjHeader ERO object header
     * @param subObjectList list of sub objects.
     */
    public PcepEroObjectVer1(PcepObjectHeader eroObjHeader, LinkedList<PcepValueType> subObjectList) {

        this.eroObjHeader = eroObjHeader;
        this.subObjectList = subObjectList;
    }

    /**
     * Returns ERO object header.
     *
     * @return eroObjHeader ERO object header
     */
    public PcepObjectHeader getEroObjHeader() {
        return this.eroObjHeader;
    }

    /**
     * Sets Object Header.
     *
     * @param obj ERO object header
     */
    public void setEroObjHeader(PcepObjectHeader obj) {
        this.eroObjHeader = obj;
    }

    @Override
    public LinkedList<PcepValueType> getSubObjects() {
        return this.subObjectList;
    }

    @Override
    public void setSubObjects(LinkedList<PcepValueType> subObjectList) {
        this.subObjectList = subObjectList;
    }

    /**
     * Reads from channel buffer and returns object of PcepEroObject.
     *
     * @param cb channel buffer.
     * @return  object of PcepEroObject
     * @throws PcepParseException when ERO object is not present in channel buffer
     */
    public static PcepEroObject read(ChannelBuffer cb) throws PcepParseException {

        PcepObjectHeader eroObjHeader;
        LinkedList<PcepValueType> subObjectList = new LinkedList<>();

        eroObjHeader = PcepObjectHeader.read(cb);

        if (eroObjHeader.getObjClass() != PcepEroObjectVer1.ERO_OBJ_CLASS) {
            log.debug("ErrorType:" + PcepErrorDetailInfo.ERROR_TYPE_6 + " ErrorValue:"
                    + PcepErrorDetailInfo.ERROR_VALUE_9);
            throw new PcepParseException(PcepErrorDetailInfo.ERROR_TYPE_6, PcepErrorDetailInfo.ERROR_VALUE_9);
        }

        if (eroObjHeader.getObjLen() > OBJECT_HEADER_LENGTH) {
            ChannelBuffer tempCb = cb.readBytes(eroObjHeader.getObjLen() - OBJECT_HEADER_LENGTH);
            subObjectList = parseSubObjects(tempCb);
        }
        return new PcepEroObjectVer1(eroObjHeader, subObjectList);
    }

    /**
     * Parse list of Sub Objects.
     *
     * @param cb channel buffer
     * @return list of Sub Objects
     * @throws PcepParseException when fails to parse sub object list
     */
    protected static LinkedList<PcepValueType> parseSubObjects(ChannelBuffer cb) throws PcepParseException {

        LinkedList<PcepValueType> subObjectList = new LinkedList<>();

        while (0 < cb.readableBytes()) {

            //check the Type of the TLV
            short type = cb.readByte();
            type = (short) (type & (TYPE_SHIFT_VALUE));
            byte hLength = cb.readByte();

            PcepValueType subObj;

            switch (type) {

            case IPv4SubObject.TYPE:
                subObj = IPv4SubObject.read(cb);
                break;
            case IPv6SubObject.TYPE:
                byte[] ipv6Value = new byte[IPv6SubObject.VALUE_LENGTH];
                cb.readBytes(ipv6Value, 0, IPv6SubObject.VALUE_LENGTH);
                subObj = new IPv6SubObject(ipv6Value);
                break;
            case AutonomousSystemNumberSubObject.TYPE:
                subObj = AutonomousSystemNumberSubObject.read(cb);
                break;
            case PathKeySubObject.TYPE:
                subObj = PathKeySubObject.read(cb);
                break;
            case SrEroSubObject.TYPE:
                subObj = SrEroSubObject.read(cb);
                break;
            default:
                throw new PcepParseException("Unexpected sub object. Type: " + (int) type);
            }
            // Check for the padding
            int pad = hLength % 4;
            if (0 < pad) {
                pad = 4 - pad;
                if (pad <= cb.readableBytes()) {
                    cb.skipBytes(pad);
                }
            }

            subObjectList.add(subObj);
        }
        if (0 < cb.readableBytes()) {
            throw new PcepParseException("Subobject parsing error. Extra bytes received.");
        }
        return subObjectList;
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {

        //write Object header
        int objStartIndex = cb.writerIndex();

        int objLenIndex = eroObjHeader.write(cb);

        if (objLenIndex <= 0) {
            throw new PcepParseException("Failed to write ERO object header. Index " + objLenIndex);
        }

        ListIterator<PcepValueType> listIterator = subObjectList.listIterator();

        while (listIterator.hasNext()) {
            listIterator.next().write(cb);
        }

        //Update object length now
        int length = cb.writerIndex() - objStartIndex;
        cb.setShort(objLenIndex, (short) length);
        //will be helpful during print().
        eroObjHeader.setObjLen((short) length);

        //As per RFC the length of object should be multiples of 4
        int pad = length % 4;

        if (pad != 0) {
            pad = 4 - pad;
            for (int i = 0; i < pad; i++) {
                cb.writeByte((byte) 0);
            }
            length = length + pad;
        }

        objLenIndex = cb.writerIndex();
        return objLenIndex;
    }

    /**
     * Builder class for PCEP ERO object.
     */
    public static class Builder implements PcepEroObject.Builder {

        private boolean bIsHeaderSet = false;

        private boolean bIsPFlagSet = false;
        private boolean bPFlag;

        private boolean bIsIFlagSet = false;
        private boolean bIFlag;

        private PcepObjectHeader eroObjHeader;
        LinkedList<PcepValueType> subObjectList = new LinkedList<>();

        @Override
        public PcepEroObject build() {

            PcepObjectHeader eroObjHeader = this.bIsHeaderSet ? this.eroObjHeader : DEFAULT_ERO_OBJECT_HEADER;

            if (bIsPFlagSet) {
                eroObjHeader.setPFlag(bPFlag);
            }

            if (bIsIFlagSet) {
                eroObjHeader.setIFlag(bIFlag);
            }

            return new PcepEroObjectVer1(eroObjHeader, this.subObjectList);
        }

        @Override
        public PcepObjectHeader getEroObjHeader() {
            return this.eroObjHeader;
        }

        @Override
        public Builder setEroObjHeader(PcepObjectHeader obj) {
            this.eroObjHeader = obj;
            this.bIsHeaderSet = true;
            return this;
        }

        @Override
        public LinkedList<PcepValueType> getSubObjects() {
            return this.subObjectList;
        }

        @Override
        public Builder setSubObjects(LinkedList<PcepValueType> subObjectList) {
            this.subObjectList = subObjectList;
            return this;
        }

        @Override
        public Builder setPFlag(boolean value) {
            this.bPFlag = value;
            this.bIsPFlagSet = true;
            return this;
        }

        @Override
        public Builder setIFlag(boolean value) {
            this.bIFlag = value;
            this.bIsIFlagSet = true;
            return this;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(eroObjHeader, subObjectList);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).omitNullValues()
                .add("EroObjHeader", eroObjHeader)
                .add("SubObjects", subObjectList)
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof PcepEroObjectVer1) {
            int countObjSubTlv = 0;
            int countOtherSubTlv = 0;
            boolean isCommonSubTlv = true;
            PcepEroObjectVer1 other = (PcepEroObjectVer1) obj;
            Iterator<PcepValueType> objListIterator = other.subObjectList.iterator();
            countOtherSubTlv = other.subObjectList.size();
            countObjSubTlv = subObjectList.size();
            if (countObjSubTlv != countOtherSubTlv) {
                return false;
            } else {
                while (objListIterator.hasNext() && isCommonSubTlv) {
                    PcepValueType subTlv = objListIterator.next();
                    if (subObjectList.contains(subTlv)) {
                        isCommonSubTlv = Objects.equals(subObjectList.get(subObjectList.indexOf(subTlv)),
                                         other.subObjectList.get(other.subObjectList.indexOf(subTlv)));
                    } else {
                        isCommonSubTlv = false;
                    }
                }
                return isCommonSubTlv && Objects.equals(eroObjHeader, other.eroObjHeader);
            }
        }
        return false;
    }
}
