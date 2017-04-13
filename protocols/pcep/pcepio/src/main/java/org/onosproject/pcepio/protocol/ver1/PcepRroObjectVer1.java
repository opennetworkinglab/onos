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

import java.util.LinkedList;
import java.util.ListIterator;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepRroObject;
import org.onosproject.pcepio.types.IPv4SubObject;
import org.onosproject.pcepio.types.IPv6SubObject;
import org.onosproject.pcepio.types.LabelSubObject;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.onosproject.pcepio.types.PcepValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP RRO object.
 */
public class PcepRroObjectVer1 implements PcepRroObject {

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

            Each subobject has its own Length
            field.  The length contains the total length of the subobject in
            bytes, including the Type and Length fields.  The length MUST always
            be a multiple of 4, and at least 4.

            An empty RRO with no subobjects is considered illegal.
            Three kinds of subobjects are currently defined.

           Subobject 1: IPv4 address

            0                   1                   2                   3
            0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           |      Type     |     Length    | IPv4 address (4 bytes)        |
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           | IPv4 address (continued)      | Prefix Length |      Flags    |
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

           Subobject 2: IPv6 address

            0                   1                   2                   3
            0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           |      Type     |     Length    | IPv6 address (16 bytes)       |
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           | IPv6 address (continued)                                      |
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           | IPv6 address (continued)                                      |
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           | IPv6 address (continued)                                      |
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           | IPv6 address (continued)      | Prefix Length |      Flags    |
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

           Subobject 3, Label

            0                   1                   2                   3
            0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           |     Type      |     Length    |    Flags      |   C-Type      |
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           |       Contents of Label Object                                |
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

     */
    protected static final Logger log = LoggerFactory.getLogger(PcepRroObjectVer1.class);

    public static final byte RRO_OBJ_TYPE = 1;
    public static final byte RRO_OBJ_CLASS = 8;
    public static final byte RRO_OBJECT_VERSION = 1;
    public static final short RRO_OBJ_MINIMUM_LENGTH = 12;
    public static final int OBJECT_HEADER_LENGTH = 4;
    public static final int YTYPE_SHIFT_VALUE = 0x7F;

    static final PcepObjectHeader DEFAULT_RRO_OBJECT_HEADER = new PcepObjectHeader(RRO_OBJ_CLASS, RRO_OBJ_TYPE,
            PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED, RRO_OBJ_MINIMUM_LENGTH);

    private short rroObjType = 0;
    private byte length;
    private byte prefixLength;
    private byte resvd;
    PcepObjectHeader rroObjHeader;
    private LinkedList<PcepValueType> llSubObjects = new LinkedList<>();

    /**
     * Reset variables.
     */
    public PcepRroObjectVer1() {
        this.rroObjHeader = null;
        this.rroObjType = 0;
        this.length = 0;
    }

    /**
     * constructor to initialize parameters for RRO object.
     *
     * @param rroObjHeader RRO object header
     * @param llSubObjects list of sub objects
     */
    public PcepRroObjectVer1(PcepObjectHeader rroObjHeader, LinkedList<PcepValueType> llSubObjects) {
        this.rroObjHeader = rroObjHeader;
        this.llSubObjects = llSubObjects;
    }

    /**
     * Returns PCEP RRO Object Header.
     *
     * @return rroObjHeader RRO Object header
     */
    public PcepObjectHeader getRroObjHeader() {
        return this.rroObjHeader;
    }

    /**
     * Sets PCEP RRO Object Header.
     *
     * @param obj Object header
     */
    public void setRroObjHeader(PcepObjectHeader obj) {
        this.rroObjHeader = obj;
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
     * Reads the channel buffer and returns object of PcepRroObject.
     *
     * @param cb of type channel buffer
     * @return object of PcepRroObject
     * @throws PcepParseException when fails to read from channel buffer
     */
    public static PcepRroObject read(ChannelBuffer cb) throws PcepParseException {

        PcepObjectHeader rroObjHeader;
        LinkedList<PcepValueType> llSubObjects;
        rroObjHeader = PcepObjectHeader.read(cb);

        //take only RroObject buffer.
        ChannelBuffer tempCb = cb.readBytes(rroObjHeader.getObjLen() - OBJECT_HEADER_LENGTH);
        llSubObjects = parseSubObjects(tempCb);

        return new PcepRroObjectVer1(rroObjHeader, llSubObjects);
    }

    /**
     * Returns list of sub objects.
     *
     * @param cb of type channel buffer
     * @return list of sub objects
     * @throws PcepParseException when fails to parse list of sub objects
     */
    protected static LinkedList<PcepValueType> parseSubObjects(ChannelBuffer cb) throws PcepParseException {

        LinkedList<PcepValueType> llSubObjects = new LinkedList<>();

        while (0 < cb.readableBytes()) {

            //check the Type of the Sub objects
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
            case LabelSubObject.TYPE:
                subObj = LabelSubObject.read(cb);
                break;
            default:
                throw new PcepParseException(" Unexpected sub object. Type: " + (int) yType);
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

        return llSubObjects;
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {
        //write Object header
        int objStartIndex = cb.writerIndex();

        int objLenIndex = rroObjHeader.write(cb);

        if (objLenIndex <= 0) {
            throw new PcepParseException(" object Length Index" + objLenIndex);
        }

        ListIterator<PcepValueType> listIterator = llSubObjects.listIterator();

        while (listIterator.hasNext()) {
            listIterator.next().write(cb);
        }

        //Update object length now
        int length = cb.writerIndex() - objStartIndex;
        cb.setShort(objLenIndex, (short) length);
        //will be helpful during print().
        rroObjHeader.setObjLen((short) length);

        //As per RFC the length of object should be multiples of 4
        int pad = length % 4;

        if (0 != pad) {
            pad = 4 - pad;
            for (int i = 0; i < pad; i++) {
                cb.writeByte((byte) 0);
            }
        }
        objLenIndex = cb.writerIndex();
        return objLenIndex;
    }

    /**
     * Builder class for PCEP RRO object.
     */
    public static class Builder implements PcepRroObject.Builder {
        private boolean bIsHeaderSet = false;

        private PcepObjectHeader rroObjHeader;
        LinkedList<PcepValueType> llSubObjects = new LinkedList<>();

        private boolean bIsPFlagSet = false;
        private boolean bPFlag;

        private boolean bIsIFlagSet = false;
        private boolean bIFlag;

        @Override
        public PcepRroObject build() {

            PcepObjectHeader rroObjHeader = this.bIsHeaderSet ? this.rroObjHeader : DEFAULT_RRO_OBJECT_HEADER;

            if (bIsPFlagSet) {
                rroObjHeader.setPFlag(bPFlag);
            }

            if (bIsIFlagSet) {
                rroObjHeader.setIFlag(bIFlag);
            }
            return new PcepRroObjectVer1(rroObjHeader, this.llSubObjects);
        }

        @Override
        public PcepObjectHeader getRroObjHeader() {
            return this.rroObjHeader;
        }

        @Override
        public Builder setRroObjHeader(PcepObjectHeader obj) {
            this.rroObjHeader = obj;
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
                .add("SubObjects", llSubObjects)
                .toString();
    }
}
