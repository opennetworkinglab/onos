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
import org.onosproject.pcepio.protocol.PcepErrorObject;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.onosproject.pcepio.types.PcepValueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP Error Object.
 */
public class PcepErrorObjectVer1 implements PcepErrorObject {

    /*
    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    | Object-Class  |   OT  |Res|P|I|   Object Length (bytes)       |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |   Reserved    |      Flags    |   Error-Type  |  Error-value  |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                                                               |
    //                         Optional TLVs                       //
    |                                                               |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */
    protected static final Logger log = LoggerFactory.getLogger(PcepErrorObjectVer1.class);

    public static final byte ERROR_OBJ_TYPE = 1;
    public static final byte ERROR_OBJ_CLASS = 13;
    public static final byte ERROR_OBJECT_VERSION = 1;
    //ERROR_OBJ_MINIMUM_LENGTH = CommonHeaderLen(4)+ErrorObjectHeaderLen(4)
    public static final short ERROR_OBJ_MINIMUM_LENGTH = 8;
    public static final int OBJECT_HEADER_LENGTH = 4;

    public static final PcepObjectHeader DEFAULT_ERROR_OBJECT_HEADER = new PcepObjectHeader(ERROR_OBJ_CLASS,
            ERROR_OBJ_TYPE, PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED,
            ERROR_OBJ_MINIMUM_LENGTH);

    private PcepObjectHeader errorObjHeader;
    private byte errorType;
    private byte errorValue;
    private LinkedList<PcepValueType> optionalTlv; // Optional TLV

    /**
     * Constructor to initialize variables.
     *
     * @param errorObjHeader ERROR Object header
     * @param errorType Error Type
     * @param errorValue Error Value
     * @param optionalTlv list of optional TLV
     */

    public PcepErrorObjectVer1(PcepObjectHeader errorObjHeader, byte errorType, byte errorValue,
            LinkedList<PcepValueType> optionalTlv) {
        this.errorObjHeader = errorObjHeader;
        this.errorType = errorType;
        this.errorValue = errorValue;
        this.optionalTlv = optionalTlv;
    }

    /**
     * sets Object Header.
     *
     * @param obj Error-Object header
     */
    public void setLspObjHeader(PcepObjectHeader obj) {
        this.errorObjHeader = obj;
    }

    @Override
    public void setErrorType(byte errorType) {
        this.errorType = errorType;
    }

    @Override
    public void setErrorValue(byte errorValue) {
        this.errorValue = errorValue;
    }

    /**
     * returns object header.
     *
     * @return errorObjHeader Error-Object header
     */
    public PcepObjectHeader getErrorObjHeader() {
        return this.errorObjHeader;
    }

    @Override
    public int getErrorType() {
        return this.errorType;
    }

    @Override
    public byte getErrorValue() {
        return this.errorValue;
    }

    @Override
    public LinkedList<PcepValueType> getOptionalTlv() {
        return this.optionalTlv;
    }

    @Override
    public void setOptionalTlv(LinkedList<PcepValueType> optionalTlv) {
        this.optionalTlv = optionalTlv;
    }

    /**
     * Reads from channel buffer and returns object of PcepErrorObject.
     *
     * @param cb of channel buffer.
     * @return object of PCEP-ERROR-OBJECT
     */
    public static PcepErrorObject read(ChannelBuffer cb) {

        PcepObjectHeader errorObjHeader;
        byte errorType;
        byte errorValue;
        LinkedList<PcepValueType> optionalTlv;

        errorObjHeader = PcepObjectHeader.read(cb);

        //take only ErrorObject buffer.
        ChannelBuffer tempCb = cb.readBytes(errorObjHeader.getObjLen() - OBJECT_HEADER_LENGTH);
        tempCb.readByte(); //ignore Reserved
        tempCb.readByte(); //ignore Flags
        errorType = tempCb.readByte();
        errorValue = tempCb.readByte();

        optionalTlv = parseOptionalTlv(tempCb);

        return new PcepErrorObjectVer1(errorObjHeader, errorType, errorValue, optionalTlv);
    }

    /**
     * returns Linked list of optional tlvs.
     *
     * @param cb channel buffer.
     * @return Linked list of optional tlvs
     */
    protected static LinkedList<PcepValueType> parseOptionalTlv(ChannelBuffer cb) {

        LinkedList<PcepValueType> llOutOptionalTlv = new LinkedList<>();

        byte[] yTemp = new byte[cb.readableBytes()];
        cb.readBytes(yTemp);

        return llOutOptionalTlv;
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {

        //write Object header
        int objStartIndex = cb.writerIndex();

        int objLenIndex = errorObjHeader.write(cb);

        if (objLenIndex <= 0) {
            throw new PcepParseException("While writing Error Object Header.");
        }

        //write Reserved
        cb.writeByte(0);
        //write Flags
        cb.writeByte(0);
        //write ErrorType and ErrorValue
        cb.writeByte(this.errorType);
        cb.writeByte(this.errorValue);

        // Add optional TLV
        packOptionalTlv(cb);

        //Update object length now
        int length = cb.writerIndex() - objStartIndex;
        //will be helpful during print().
        errorObjHeader.setObjLen((short) length);
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
        return length;
    }

    /**
     * Pack the Optional tlvs.
     *
     * @param cb channel buffer.
     * @return writer index.
     */
    protected int packOptionalTlv(ChannelBuffer cb) {

        ListIterator<PcepValueType> listIterator = optionalTlv.listIterator();
        int startIndex = cb.writerIndex();
        while (listIterator.hasNext()) {
            PcepValueType tlv = listIterator.next();

            if (tlv == null) {
                log.debug("TLV is null from OptionalTlv list");
                continue;
            }
            tlv.write(cb);
        }

        return cb.writerIndex() - startIndex;
    }

    /**
     * Builder class for PCEP error object.
     */
    public static class Builder implements PcepErrorObject.Builder {

        private boolean bIsHeaderSet = false;

        private PcepObjectHeader errorObjHeader;
        private byte errorType;
        private byte errorValue;

        private boolean bIsPFlagSet = false;
        private boolean bPFlag;

        private boolean bIsIFlagSet = false;
        private boolean bIFlag;

        private LinkedList<PcepValueType> optionalTlv = new LinkedList<>();

        @Override
        public PcepErrorObject build() {

            PcepObjectHeader errorObjHeader = this.bIsHeaderSet ? this.errorObjHeader : DEFAULT_ERROR_OBJECT_HEADER;

            if (bIsPFlagSet) {
                errorObjHeader.setPFlag(bPFlag);
            }

            if (bIsIFlagSet) {
                errorObjHeader.setIFlag(bIFlag);
            }

            return new PcepErrorObjectVer1(errorObjHeader, errorType, errorValue, optionalTlv);
        }

        @Override
        public PcepObjectHeader getErrorObjHeader() {
            return this.errorObjHeader;
        }

        @Override
        public Builder setErrorObjHeader(PcepObjectHeader obj) {
            this.errorObjHeader = obj;
            this.bIsHeaderSet = true;
            return this;
        }

        @Override
        public int getErrorType() {
            return this.errorType;
        }

        @Override
        public Builder setErrorType(byte value) {
            this.errorType = value;
            return this;
        }

        @Override
        public byte getErrorValue() {
            return this.errorValue;
        }

        @Override
        public Builder setErrorValue(byte value) {
            this.errorValue = value;
            return this;
        }

        @Override
        public Builder setOptionalTlv(LinkedList<PcepValueType> optionalTlv) {
            this.optionalTlv = optionalTlv;
            return this;
        }

        @Override
        public LinkedList<PcepValueType> getOptionalTlv() {
            return this.optionalTlv;
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
                .add("ObjectHeader", errorObjHeader).add("ErrorType", errorType)
                .add("ErrorValue", errorValue).add("OptionalTlv", optionalTlv).toString();
    }
}
