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
import org.onosproject.pcepio.protocol.PcepIroObject;
import org.onosproject.pcepio.types.IPv4SubObject;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.onosproject.pcepio.types.PcepValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP iro object.
 */
public class PcepIroObjectVer1 implements PcepIroObject {

    /*
      0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |                                                               |
      //                      (Sub-objects)                           //
      |                                                               |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                     The IRO Object format

        Each IPV4 suboject

        0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        |L|    Type     |     Length    | IPv4 address (4 bytes)        |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
        | IPv4 address (continued)      | Prefix Length |      Resvd    |
        +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    protected static final Logger log = LoggerFactory.getLogger(PcepIroObjectVer1.class);

    public static final byte IRO_OBJ_TYPE = 1;
    public static final byte IRO_OBJ_CLASS = 10;
    public static final byte IRO_OBJECT_VERSION = 1;
    public static final short IRO_OBJ_MINIMUM_LENGTH = 12;
    public static final int OBJECT_HEADER_LENGTH = 4;
    public static final int YTYPE_SHIFT_VALUE = 0x7F;

    public static final PcepObjectHeader DEFAULT_IRO_OBJECT_HEADER = new PcepObjectHeader(IRO_OBJ_CLASS, IRO_OBJ_TYPE,
            PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED, IRO_OBJ_MINIMUM_LENGTH);

    private short iroObjType = 0;
    private byte yLength;
    private byte yPrefixLength;
    private byte yResvd;
    private PcepObjectHeader iroObjHeader;
    private LinkedList<PcepValueType> llSubObjects = new LinkedList<>();

    /**
     * Default constructor.
     */
    public PcepIroObjectVer1() {
        this.iroObjHeader = null;
        this.iroObjType = 0;
        this.yLength = 0;
    }

    /**
     * Constructor to initialize member variables.
     *
     * @param iroObjHeader IRO object header
     * @param llSubObjects list of sub-objects
     */
    public PcepIroObjectVer1(PcepObjectHeader iroObjHeader, LinkedList<PcepValueType> llSubObjects) {
        this.iroObjHeader = iroObjHeader;
        this.llSubObjects = llSubObjects;
    }

    /**
     * Returns object header.
     *
     * @return iroObjHeader IRO object header
     */
    public PcepObjectHeader getIroObjHeader() {
        return this.iroObjHeader;
    }

    /**
     * Sets IRO Object Header.
     *
     * @param obj IRO object header
     */
    public void setIroObjHeader(PcepObjectHeader obj) {
        this.iroObjHeader = obj;
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
     * Reads from channel buffer and return object of PcepIroObject.
     *
     * @param cb of type channel buffer
     * @return object of PcepIroObject
     * @throws PcepParseException while parsing from channel buffer
     */
    public static PcepIroObject read(ChannelBuffer cb) throws PcepParseException {

        PcepObjectHeader iroObjHeader;
        LinkedList<PcepValueType> llSubObjects;

        iroObjHeader = PcepObjectHeader.read(cb);

        //take only IroObject buffer.
        ChannelBuffer tempCb = cb.readBytes(iroObjHeader.getObjLen() - OBJECT_HEADER_LENGTH);
        llSubObjects = parseSubObjects(tempCb);
        return new PcepIroObjectVer1(iroObjHeader, llSubObjects);
    }

    /**
     * Returns linked list of sub objects.
     *
     * @param cb of type channel buffer
     * @return linked list of sub objects
     * @throws PcepParseException while parsing subobjects from channel buffer
     */
    protected static LinkedList<PcepValueType> parseSubObjects(ChannelBuffer cb) throws PcepParseException {

        LinkedList<PcepValueType> llSubObjects = new LinkedList<>();

        while (0 < cb.readableBytes()) {

            //check the Type of the Subobjects.
            byte yType = cb.readByte();
            yType = (byte) (yType & (YTYPE_SHIFT_VALUE));
            byte hLength = cb.readByte();

            PcepValueType subObj;
            switch (yType) {

            case IPv4SubObject.TYPE:
                subObj = IPv4SubObject.read(cb);
                break;

            default:
                throw new PcepParseException("Invalid sub object. Type: " + (int) yType);
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

        int objLenIndex = iroObjHeader.write(cb);

        if (objLenIndex <= 0) {
            throw new PcepParseException(" ObjectLength is " + objLenIndex);
        }

        ListIterator<PcepValueType> listIterator = llSubObjects.listIterator();
        while (listIterator.hasNext()) {
            listIterator.next().write(cb);
        }

        //Update object length now
        int length = cb.writerIndex() - objStartIndex;
        //will be helpful during print().
        iroObjHeader.setObjLen((short) length);
        // As per RFC the length of object should be
        // multiples of 4
        int pad = length % 4;
        if (pad != 0) {
            pad = 4 - pad;
            for (int i = 0; i < pad; i++) {
                cb.writeByte((byte) 0);
            }
            length = length + pad;
        }
        cb.setShort(objLenIndex, (short) length);
        objLenIndex = cb.writerIndex();
        return objLenIndex;
    }

    /**
     * Builder class for PCEP iro object.
     */
    public static class Builder implements PcepIroObject.Builder {

        private boolean bIsHeaderSet = false;

        private PcepObjectHeader iroObjHeader;
        LinkedList<PcepValueType> llSubObjects = new LinkedList<>();

        private boolean bIsPFlagSet = false;
        private boolean bPFlag;

        private boolean bIsIFlagSet = false;
        private boolean bIFlag;

        @Override
        public PcepIroObject build() {

            PcepObjectHeader iroObjHeader = this.bIsHeaderSet ? this.iroObjHeader : DEFAULT_IRO_OBJECT_HEADER;

            if (bIsPFlagSet) {
                iroObjHeader.setPFlag(bPFlag);
            }

            if (bIsIFlagSet) {
                iroObjHeader.setIFlag(bIFlag);
            }

            return new PcepIroObjectVer1(iroObjHeader, this.llSubObjects);
        }

        @Override
        public PcepObjectHeader getIroObjHeader() {
            return this.iroObjHeader;
        }

        @Override
        public Builder setIroObjHeader(PcepObjectHeader obj) {
            this.iroObjHeader = obj;
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
                .add("IroObjectHeader", iroObjHeader)
                .add("SubObjects", llSubObjects).toString();
    }
}
