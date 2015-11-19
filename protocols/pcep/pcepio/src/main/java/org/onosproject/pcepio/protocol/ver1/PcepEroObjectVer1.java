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

package org.onosproject.pcepio.protocol.ver1;

import java.util.LinkedList;
import java.util.ListIterator;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepEroObject;
import org.onosproject.pcepio.types.AutonomousSystemTlv;
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
    public static final int YTYPE_SHIFT_VALUE = 0x7F;

    static final PcepObjectHeader DEFAULT_ERO_OBJECT_HEADER = new PcepObjectHeader(ERO_OBJ_CLASS, ERO_OBJ_TYPE,
            PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED, ERO_OBJ_MINIMUM_LENGTH);

    private PcepObjectHeader eroObjHeader;
    private LinkedList<PcepValueType> llSubObjects = new LinkedList<>();

    /**
     * reset variables.
     */
    public PcepEroObjectVer1() {
        this.eroObjHeader = null;
        this.llSubObjects = null;
    }

    /**
     * Constructor to initialize parameters of ERO object.
     *
     * @param eroObjHeader ERO object header
     * @param llSubObjects list of sub objects.
     */
    public PcepEroObjectVer1(PcepObjectHeader eroObjHeader, LinkedList<PcepValueType> llSubObjects) {

        this.eroObjHeader = eroObjHeader;
        this.llSubObjects = llSubObjects;
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
        return this.llSubObjects;
    }

    @Override
    public void setSubObjects(LinkedList<PcepValueType> llSubObjects) {
        this.llSubObjects = llSubObjects;
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
        LinkedList<PcepValueType> llSubObjects = new LinkedList<>();

        eroObjHeader = PcepObjectHeader.read(cb);

        if (eroObjHeader.getObjClass() != PcepEroObjectVer1.ERO_OBJ_CLASS) {
            log.debug("ErrorType:" + PcepErrorDetailInfo.ERROR_TYPE_6 + " ErrorValue:"
                    + PcepErrorDetailInfo.ERROR_VALUE_9);
            throw new PcepParseException(PcepErrorDetailInfo.ERROR_TYPE_6, PcepErrorDetailInfo.ERROR_VALUE_9);
        }

        if (eroObjHeader.getObjLen() > OBJECT_HEADER_LENGTH) {
            ChannelBuffer tempCb = cb.readBytes(eroObjHeader.getObjLen() - OBJECT_HEADER_LENGTH);
            llSubObjects = parseSubObjects(tempCb);
        }
        return new PcepEroObjectVer1(eroObjHeader, llSubObjects);
    }

    /**
     * Parse list of Sub Objects.
     *
     * @param cb channel buffer
     * @return list of Sub Objects
     * @throws PcepParseException when fails to parse sub object list
     */
    protected static LinkedList<PcepValueType> parseSubObjects(ChannelBuffer cb) throws PcepParseException {

        LinkedList<PcepValueType> llSubObjects = new LinkedList<>();

        while (0 < cb.readableBytes()) {

            //check the Type of the TLV
            byte yType = cb.readByte();
            yType = (byte) (yType & (YTYPE_SHIFT_VALUE));
            byte hLength = cb.readByte();

            PcepValueType subObj;

            switch (yType) {

            case IPv4SubObject.TYPE:
                subObj = IPv4SubObject.read(cb);
                break;
            case IPv6SubObject.TYPE:
                byte[] ipv6Value = new byte[IPv6SubObject.VALUE_LENGTH];
                cb.readBytes(ipv6Value, 0, IPv6SubObject.VALUE_LENGTH);
                subObj = new IPv6SubObject(ipv6Value);
                break;
            case AutonomousSystemTlv.TYPE:
                subObj = AutonomousSystemTlv.read(cb);
                break;
            case PathKeySubObject.TYPE:
                subObj = PathKeySubObject.read(cb);
                break;
            case SrEroSubObject.TYPE:
                subObj = SrEroSubObject.read(cb);
                break;
            default:
                throw new PcepParseException("Unexpected sub object. Type: " + (int) yType);
            }
            // Check for the padding
            int pad = hLength % 4;
            if (0 < pad) {
                pad = 4 - pad;
                if (pad <= cb.readableBytes()) {
                    cb.skipBytes(pad);
                }
            }

            llSubObjects.add(subObj);
        }
        if (0 < cb.readableBytes()) {
            throw new PcepParseException("Subobject parsing error. Extra bytes received.");
        }
        return llSubObjects;
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {

        //write Object header
        int objStartIndex = cb.writerIndex();

        int objLenIndex = eroObjHeader.write(cb);

        if (objLenIndex <= 0) {
            throw new PcepParseException("Failed to write ERO object header. Index " + objLenIndex);
        }

        ListIterator<PcepValueType> listIterator = llSubObjects.listIterator();

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
        LinkedList<PcepValueType> llSubObjects = new LinkedList<>();

        @Override
        public PcepEroObject build() {

            PcepObjectHeader eroObjHeader = this.bIsHeaderSet ? this.eroObjHeader : DEFAULT_ERO_OBJECT_HEADER;

            if (bIsPFlagSet) {
                eroObjHeader.setPFlag(bPFlag);
            }

            if (bIsIFlagSet) {
                eroObjHeader.setIFlag(bIFlag);
            }

            return new PcepEroObjectVer1(eroObjHeader, this.llSubObjects);
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
            return this.llSubObjects;
        }

        @Override
        public Builder setSubObjects(LinkedList<PcepValueType> llSubObjects) {
            this.llSubObjects = llSubObjects;
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
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("EroObjHeader", eroObjHeader).add("SubObjects", llSubObjects)
                .toString();
    }
}
