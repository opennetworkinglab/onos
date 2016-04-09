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
import org.onosproject.pcepio.protocol.PcepLabelRange;
import org.onosproject.pcepio.protocol.PcepLabelRangeResvMsg;
import org.onosproject.pcepio.protocol.PcepMessageReader;
import org.onosproject.pcepio.protocol.PcepMessageWriter;
import org.onosproject.pcepio.protocol.PcepType;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP label range reserve message.
 */
class PcepLabelRangeResvMsgVer1 implements PcepLabelRangeResvMsg {

    // Pcep version: 1

    /*
       The format of a PCLRResv message is as follows:

               PCLRResv Message>::= <Common Header>
                                    <label-range>
          Where:

               <label-range> ::= <SRP>
                                 <labelrange-list>

          Where
               <labelrange-list>::=<LABEL-RANGE>[<labelrange-list>]
     */
    protected static final Logger log = LoggerFactory.getLogger(PcepLabelRangeResvMsgVer1.class);

    public static final byte PACKET_VERSION = 1;
    // LabelRangeResvMsgMinLength = COMMON-HEADER(4)+SrpObjMinLentgh(12)+LABEL-RANGE-MIN-LENGTH(12)
    public static final int PACKET_MINIMUM_LENGTH = 28;
    public static final PcepType MSG_TYPE = PcepType.LABEL_RANGE_RESERV;
    //<label-range>
    PcepLabelRange labelRange;

    public static final PcepLabelRangeResvMsgVer1.Reader READER = new Reader();

    /**
     * Reader reads LabelRangeResv Message from the channel.
     */
    static class Reader implements PcepMessageReader<PcepLabelRangeResvMsg> {

        @Override
        public PcepLabelRangeResvMsg readFrom(ChannelBuffer cb) throws PcepParseException {

            if (cb.readableBytes() < PACKET_MINIMUM_LENGTH) {
                throw new PcepParseException("Channel buffer has less readable bytes than Packet minimum length.");
            }
            // fixed value property version == 1
            byte version = cb.readByte();
            version = (byte) (version >> PcepMessageVer1.SHIFT_FLAG);
            if (version != PACKET_VERSION) {
                throw new PcepParseException("Wrong version. Expected=PcepVersion.PCEP_1(1), got=" + version);
            }
            // fixed value property type == 15
            byte type = cb.readByte();
            if (type != MSG_TYPE.getType()) {
                throw new PcepParseException("Wrong type. Expected=PcepType.LABEL_RANGE_RESERV(15), got=" + type);
            }
            short length = cb.readShort();
            if (length < PACKET_MINIMUM_LENGTH) {
                throw new PcepParseException("Wrong length.Expected to be >= " + PACKET_MINIMUM_LENGTH + ", is: "
                        + length);
            }
            // parse <label-range>
            PcepLabelRange labelRange = PcepLabelRangeVer1.read(cb);
            return new PcepLabelRangeResvMsgVer1(labelRange);
        }
    }

    /**
     * Constructor to initialize PCEP label range.
     *
     * @param labelRange PCEP label range
     */
    PcepLabelRangeResvMsgVer1(PcepLabelRange labelRange) {
        this.labelRange = labelRange;
    }

    /**
     * Builder class for PCEP label range reserve message.
     */
    static class Builder implements PcepLabelRangeResvMsg.Builder {

        PcepLabelRange labelRange = new PcepLabelRangeVer1();

        @Override
        public PcepVersion getVersion() {
            return PcepVersion.PCEP_1;
        }

        @Override
        public PcepType getType() {
            return PcepType.LABEL_RANGE_RESERV;
        }

        @Override
        public PcepLabelRangeResvMsg build() {
            return new PcepLabelRangeResvMsgVer1(this.labelRange);
        }

        @Override
        public PcepLabelRange getLabelRange() {
            return this.labelRange;
        }

        @Override
        public Builder setLabelRange(PcepLabelRange labelRange) {
            this.labelRange = labelRange;
            return this;
        }
    }

    @Override
    public void writeTo(ChannelBuffer cb) throws PcepParseException {
        WRITER.write(cb, this);
    }

    static final Writer WRITER = new Writer();

    /**
     * Writer writes LabelRangeResv Message to the channel.
     */
    static class Writer implements PcepMessageWriter<PcepLabelRangeResvMsgVer1> {

        @Override
        public void write(ChannelBuffer cb, PcepLabelRangeResvMsgVer1 message) throws PcepParseException {

            int startIndex = cb.writerIndex();
            // first 3 bits set to version
            cb.writeByte((byte) (PACKET_VERSION << PcepMessageVer1.SHIFT_FLAG));
            // message type
            cb.writeByte(MSG_TYPE.getType());
            // Length will be set after calculating length, but currently set it as 0.
            int msgLenIndex = cb.writerIndex();

            cb.writeShort((short) 0);
            //write Label Range
            message.labelRange.write(cb);

            // update message length field
            int length = cb.writerIndex() - startIndex;
            cb.setShort(msgLenIndex, (short) length);
        }
    }

    @Override
    public PcepVersion getVersion() {
        return PcepVersion.PCEP_1;
    }

    @Override
    public PcepType getType() {
        return MSG_TYPE;
    }

    @Override
    public PcepLabelRange getLabelRange() {
        return this.labelRange;
    }

    @Override
    public void setLabelRange(PcepLabelRange lr) {
        this.labelRange = lr;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("labelRange", labelRange)
                .toString();
    }
}
