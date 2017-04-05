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

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepEndPointsObject;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP Endpoints Object.
 */
public class PcepEndPointsObjectVer1 implements PcepEndPointsObject {

    /*
     * RFC : 5440 , section : 7.6
     * An End point is defined as follows:
    END-POINTS Object-Class is 4.

    END-POINTS Object-Type is 1 for IPv4 and 2 for IPv6.
    0                   1                   2                   3
       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      | Object-Class  |   OT  |Res|P|I|   Object Length (bytes)       |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |                     Source IPv4 address                       |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |                  Destination IPv4 address                     |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

     */
    protected static final Logger log = LoggerFactory.getLogger(PcepEndPointsObjectVer1.class);

    static final byte END_POINTS_OBJ_TYPE = 1;
    static final byte END_POINTS_OBJ_CLASS = 4;
    static final byte END_POINTS_OBJECT_VERSION = 1;
    static final short END_POINTS_OBJ_MINIMUM_LENGTH = 12;
    static byte endPointObjType;

    static final PcepObjectHeader DEFAULT_END_POINTS_OBJECT_HEADER = new PcepObjectHeader(END_POINTS_OBJ_CLASS,
            END_POINTS_OBJ_TYPE, PcepObjectHeader.REQ_OBJ_OPTIONAL_PROCESS, PcepObjectHeader.RSP_OBJ_PROCESSED,
            END_POINTS_OBJ_MINIMUM_LENGTH);

    private PcepObjectHeader endPointsObjHeader;
    int sourceIpAddress;
    int destIpAddress;

    /**
     * Constructor to initialize all variables.
     *
     * @param endPointsObjHeader end points object header
     * @param sourceIpAddress source IP address
     * @param destIpAddress destination IP address
     */
    public PcepEndPointsObjectVer1(PcepObjectHeader endPointsObjHeader, int sourceIpAddress, int destIpAddress) {

        this.endPointsObjHeader = endPointsObjHeader;
        this.sourceIpAddress = sourceIpAddress;
        this.destIpAddress = destIpAddress;
    }

    /**
     * Sets End Points Object Header.
     *
     * @param obj of PcepObjectHeader
     */
    public void setEndPointsObjHeader(PcepObjectHeader obj) {
        this.endPointsObjHeader = obj;
    }

    @Override
    public void setSourceIpAddress(int sourceIpAddress) {
        this.sourceIpAddress = sourceIpAddress;
    }

    @Override
    public void setDestIpAddress(int destIpAddress) {
        this.destIpAddress = destIpAddress;
    }

    @Override
    public int getSourceIpAddress() {
        return this.sourceIpAddress;
    }

    @Override
    public int getDestIpAddress() {
        return this.destIpAddress;
    }

    /**
     * Reads from channel buffer and returns object of PcepEndPointsObject.
     *
     * @param cb of channel buffer
     * @return object of PcepEndPointsObject
     * @throws PcepParseException while parsing channel buffer
     */
    public static PcepEndPointsObject read(ChannelBuffer cb) throws PcepParseException {

        PcepObjectHeader endPointsObjHeader;
        int sourceIpAddress;
        int destIpAddress;

        endPointsObjHeader = PcepObjectHeader.read(cb);
        if (endPointsObjHeader.getObjType() == END_POINTS_OBJ_TYPE
                && endPointsObjHeader.getObjClass() == END_POINTS_OBJ_CLASS) {
            sourceIpAddress = cb.readInt();
            destIpAddress = cb.readInt();
        } else {
            throw new PcepParseException("Expected PcepEndPointsObject.");
        }
        return new PcepEndPointsObjectVer1(endPointsObjHeader, sourceIpAddress, destIpAddress);
    }

    @Override
    public int write(ChannelBuffer cb) throws PcepParseException {

        int objStartIndex = cb.writerIndex();
        //write common header
        int objLenIndex = endPointsObjHeader.write(cb);

        //write source IPv4 IP
        cb.writeInt(sourceIpAddress);
        //write destination IPv4 IP
        cb.writeInt(destIpAddress);

        int length = cb.writerIndex() - objStartIndex;
        //now write EndPoints Object Length
        cb.setShort(objLenIndex, (short) length);
        //will be helpful during print().
        endPointsObjHeader.setObjLen((short) length);

        return cb.writerIndex();

    }

    /**
     * Builder class for PCEP end points objects.
     */
    public static class Builder implements PcepEndPointsObject.Builder {

        private boolean bIsHeaderSet = false;
        private boolean bIsSourceIpAddressset = false;
        private boolean bIsDestIpAddressset = false;
        private PcepObjectHeader endpointsObjHeader;
        private int sourceIpAddress;
        private int destIpAddress;

        private boolean bIsPFlagSet = false;
        private boolean bPFlag;

        private boolean bIsIFlagSet = false;
        private boolean bIFlag;

        @Override
        public PcepEndPointsObject build() throws PcepParseException {

            PcepObjectHeader endpointsObjHeader = this.bIsHeaderSet ? this.endpointsObjHeader
                    : DEFAULT_END_POINTS_OBJECT_HEADER;

            if (bIsPFlagSet) {
                endpointsObjHeader.setPFlag(bPFlag);
            }

            if (bIsIFlagSet) {
                endpointsObjHeader.setIFlag(bIFlag);
            }

            if (!this.bIsSourceIpAddressset) {
                throw new PcepParseException("SourceIpAddress not set while building EndPoints object");
            }

            if (!this.bIsDestIpAddressset) {
                throw new PcepParseException("DestIpAddress not set while building EndPoints object");
            }

            return new PcepEndPointsObjectVer1(endpointsObjHeader, this.sourceIpAddress, this.destIpAddress);
        }

        @Override
        public PcepObjectHeader getEndPointsObjHeader() {
            return this.endpointsObjHeader;
        }

        @Override
        public Builder setEndPointsObjHeader(PcepObjectHeader obj) {
            this.endpointsObjHeader = obj;
            this.bIsHeaderSet = true;
            return this;
        }

        @Override
        public int getSourceIpAddress() {
            return this.sourceIpAddress;
        }

        @Override
        public Builder setSourceIpAddress(int sourceIpAddress) {
            this.sourceIpAddress = sourceIpAddress;
            this.bIsSourceIpAddressset = true;
            return this;
        }

        @Override
        public int getDestIpAddress() {
            return this.destIpAddress;
        }

        @Override
        public Builder setDestIpAddress(int destIpAddress) {
            this.destIpAddress = destIpAddress;
            this.bIsDestIpAddressset = true;
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
                .add("sourceIpAddress", sourceIpAddress)
                .add("destIpAddress", destIpAddress).toString();
    }

}
