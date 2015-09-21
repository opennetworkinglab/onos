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

package org.onosproject.bgpio.protocol.ver4;

import java.util.LinkedList;
import java.util.ListIterator;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BGPParseException;
import org.onosproject.bgpio.protocol.BGPMessageReader;
import org.onosproject.bgpio.protocol.BGPMessageWriter;
import org.onosproject.bgpio.protocol.BGPOpenMsg;
import org.onosproject.bgpio.protocol.BGPType;
import org.onosproject.bgpio.protocol.BGPVersion;
import org.onosproject.bgpio.types.BGPErrorType;
import org.onosproject.bgpio.types.BGPHeader;
import org.onosproject.bgpio.types.BGPValueType;
import org.onosproject.bgpio.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides BGP open message.
 */
public class BGPOpenMsgVer4 implements BGPOpenMsg {

    /*
       0                   1                   2                   3
       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+
       |    Version    |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |    My Autonomous System       |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |         Hold Time             |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                  BGP Identifier                             |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       | Opt Parm Len  |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |             Optional Parameters (variable)                  |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       OPEN Message Format
       REFERENCE : RFC 4271
    */

    protected static final Logger log = LoggerFactory.getLogger(BGPOpenMsgVer4.class);

    public static final byte PACKET_VERSION = 4;
    public static final int OPEN_MSG_MINIMUM_LENGTH = 10;
    public static final int MSG_HEADER_LENGTH = 19;
    public static final int MARKER_LENGTH  = 16;
    public static final int DEFAULT_HOLD_TIME = 120;
    public static final int OPT_PARA_TYPE_CAPABILITY = 2;
    public static final BGPType MSG_TYPE = BGPType.OPEN;
    public static final byte[] MARKER = new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff,
        (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
    public static final BGPHeader DEFAULT_OPEN_HEADER = new BGPHeader(MARKER,
        (short) OPEN_MSG_MINIMUM_LENGTH, (byte) 0X01);
    private BGPHeader bgpMsgHeader;
    private byte version;
    private short asNumber;
    private short holdTime;
    private int bgpId;
    private LinkedList<BGPValueType> capabilityTlv;

    public static final BGPOpenMsgVer4.Reader READER = new Reader();

    /**
     * reset variables.
     */
    public BGPOpenMsgVer4() {
        this.bgpMsgHeader = null;
        this.version = 0;
        this.holdTime = 0;
        this.asNumber = 0;
        this.bgpId = 0;
        this.capabilityTlv = null;
    }

    /**
     * Constructor to initialize all variables of BGP Open message.
     *
     * @param bgpMsgHeader
     *           BGP Header in open message
     * @param version
     *           BGP version in open message
     * @param holdTime
     *           hold time in open message
     * @param asNumber
     *           AS number in open message
     * @param bgpId
     *           BGP identifier in open message
     * @param capabilityTlv
     *           capabilities in open message
     */
    public BGPOpenMsgVer4(BGPHeader bgpMsgHeader, byte version, short asNumber, short holdTime,
             int bgpId, LinkedList<BGPValueType> capabilityTlv) {
        this.bgpMsgHeader = bgpMsgHeader;
        this.version = version;
        this.asNumber = asNumber;
        this.holdTime = holdTime;
        this.bgpId = bgpId;
        this.capabilityTlv = capabilityTlv;
    }

    @Override
    public BGPHeader getHeader() {
        return this.bgpMsgHeader;
    }

    @Override
    public BGPVersion getVersion() {
        return BGPVersion.BGP_4;
    }

    @Override
    public BGPType getType() {
        return MSG_TYPE;
    }

    @Override
    public short getHoldTime() {
        return this.holdTime;
    }

    @Override
    public short getAsNumber() {
        return this.asNumber;
    }

    @Override
    public int getBgpId() {
        return this.bgpId;
    }

    @Override
    public LinkedList<BGPValueType> getCapabilityTlv() {
        return this.capabilityTlv;
    }

    /**
     * Reader class for reading BGP open message from channel buffer.
     */
    public static class Reader implements BGPMessageReader<BGPOpenMsg> {

        @Override
        public BGPOpenMsg readFrom(ChannelBuffer cb, BGPHeader bgpHeader) throws BGPParseException {

            byte version;
            short holdTime;
            short asNumber;
            int bgpId;
            byte optParaLen = 0;
            byte optParaType;
            byte capParaLen = 0;
            LinkedList<BGPValueType> capabilityTlv = new LinkedList<>();

            if (cb.readableBytes() < OPEN_MSG_MINIMUM_LENGTH) {
                log.error("[readFrom] Invalid length: Packet size is less than the minimum length ");
                Validation.validateLen(BGPErrorType.OPEN_MESSAGE_ERROR, BGPErrorType.BAD_MESSAGE_LENGTH,
                        cb.readableBytes());
            }

            // Read version
            version = cb.readByte();
            if (version != PACKET_VERSION) {
                log.error("[readFrom] Invalid version: " + version);
                throw new BGPParseException(BGPErrorType.OPEN_MESSAGE_ERROR,
                        BGPErrorType.UNSUPPORTED_VERSION_NUMBER, null);
            }

            // Read AS number
            asNumber = cb.readShort();

            // Read Hold timer
            holdTime = cb.readShort();

            // Read BGP Identifier
            bgpId = cb.readInt();

            // Read optional parameter length
            optParaLen = cb.readByte();

            // Read Capabilities if optional parameter length is greater than 0
            if (optParaLen != 0) {
                // Read optional parameter type
                optParaType = cb.readByte();

                // Read optional parameter length
                capParaLen = cb.readByte();

                if (cb.readableBytes() < capParaLen) {
                    throw new BGPParseException(BGPErrorType.OPEN_MESSAGE_ERROR, (byte) 0, null);
                }

                ChannelBuffer capaCb = cb.readBytes(capParaLen);

                // Parse capabilities only if optional parameter type is 2
                if ((optParaType == OPT_PARA_TYPE_CAPABILITY) && (capParaLen != 0)) {
                    capabilityTlv = parseCapabilityTlv(capaCb);
                } else {
                    throw new BGPParseException(BGPErrorType.OPEN_MESSAGE_ERROR,
                        BGPErrorType.UNSUPPORTED_OPTIONAL_PARAMETER, null);
                }
            }
            return new BGPOpenMsgVer4(bgpHeader, version, asNumber, holdTime, bgpId, capabilityTlv);
        }
    }

    /**
     * Parsing capabilities.
     *
     * @param cb of type channel buffer
     * @return capabilityTlv of open message
     * @throws BGPParseException while parsing capabilities
     */
    protected static LinkedList<BGPValueType> parseCapabilityTlv(ChannelBuffer cb) throws BGPParseException {

        LinkedList<BGPValueType> capabilityTlv = new LinkedList<>();

        // TODO: Capability parsing
        return capabilityTlv;
    }

    /**
     * Builder class for BGP open message.
     */
    static class Builder implements BGPOpenMsg.Builder {

        private boolean isHeaderSet = false;
        private BGPHeader bgpMsgHeader;
        private boolean isHoldTimeSet = false;
        private short  holdTime;
        private boolean isAsNumSet = false;
        private short asNumber;
        private boolean isBgpIdSet = false;
        private int bgpId;

        LinkedList<BGPValueType> capabilityTlv = new LinkedList<>();

        @Override
        public BGPOpenMsg build() throws BGPParseException {
            BGPHeader bgpMsgHeader = this.isHeaderSet ? this.bgpMsgHeader : DEFAULT_OPEN_HEADER;
            short holdTime = this.isHoldTimeSet ? this.holdTime : DEFAULT_HOLD_TIME;

            if (!this.isAsNumSet) {
                throw new BGPParseException("BGP AS number is not set (mandatory)");
            }

            if (!this.isBgpIdSet) {
                throw new BGPParseException("BGPID  is not set (mandatory)");
            }

            // TODO: capabilities build

            return new BGPOpenMsgVer4(bgpMsgHeader, PACKET_VERSION, this.asNumber, holdTime, this.bgpId,
                       this.capabilityTlv);
        }

        @Override
        public BGPHeader getHeader() {
            return this.bgpMsgHeader;
        }

        @Override
        public Builder setHeader(BGPHeader bgpMsgHeader) {
            this.bgpMsgHeader = bgpMsgHeader;
            return this;
        }

        @Override
        public BGPVersion getVersion() {
            return BGPVersion.BGP_4;
        }

        @Override
        public BGPType getType() {
            return MSG_TYPE;
        }

        @Override
        public short getHoldTime() {
            return this.holdTime;
        }

        @Override
        public short getAsNumber() {
            return this.asNumber;
        }

        @Override
        public int getBgpId() {
            return this.bgpId;
        }

        @Override
        public LinkedList<BGPValueType> getCapabilityTlv() {
            return this.capabilityTlv;
        }

        @Override
        public Builder setHoldTime(short holdTime) {
            this.holdTime = holdTime;
            this.isHoldTimeSet = true;
            return this;
        }

        @Override
        public Builder setAsNumber(short asNumber) {
            this.asNumber = asNumber;
            this.isAsNumSet = true;
            return this;
        }

        @Override
        public Builder setBgpId(int bgpId) {
            this.bgpId = bgpId;
            this.isBgpIdSet = true;
            return this;
        }

        @Override
        public Builder setCapabilityTlv(LinkedList<BGPValueType> capabilityTlv) {
            this.capabilityTlv = capabilityTlv;
            return this;
        }
    }

    @Override
    public void writeTo(ChannelBuffer cb) {
        try {
            WRITER.write(cb, this);
        } catch (BGPParseException e) {
            log.debug("[writeTo] Error: " + e.toString());
        }
    }

    public static final Writer WRITER = new Writer();

    /**
     * Writer class for writing BGP open message to channel buffer.
     */
    public static class Writer implements BGPMessageWriter<BGPOpenMsgVer4> {

        @Override
        public void write(ChannelBuffer cb, BGPOpenMsgVer4 message) throws BGPParseException {

            int optParaLen = 0;

            int startIndex = cb.writerIndex();

            // write common header and get msg length index
            int msgLenIndex = message.bgpMsgHeader.write(cb);

            if (msgLenIndex <= 0) {
                throw new BGPParseException("Unable to write message header.");
            }

            // write version in 1-octet
            cb.writeByte(message.version);

            // TODO : Write AS number based on capabilities
            cb.writeShort(message.asNumber);

            // write HoldTime in next 2-octet
            cb.writeShort(message.holdTime);

            // write BGP Identifier in next 4-octet
            cb.writeInt(message.bgpId);

            // store the index of Optional parameter length
            int optParaLenIndex = cb.writerIndex();

            // set optional parameter length as 0
            cb.writeByte(0);

            // Pack capability TLV
            optParaLen = message.packCapabilityTlv(cb, message);

            if (optParaLen != 0) {
                // Update optional parameter length
                cb.setByte(optParaLenIndex, (byte) (optParaLen + 2)); //+2 for optional parameter type.
            }

            // write OPEN Object Length
            int length = cb.writerIndex() - startIndex;
            cb.setShort(msgLenIndex, (short) length);
            message.bgpMsgHeader.setLength((short) length);
        }
    }

    /**
     * returns length of capability tlvs.
     *
     * @param cb of type channel buffer
     * @param message of type BGPOpenMsgVer4
     * @return capParaLen of open message
     */
    protected int packCapabilityTlv(ChannelBuffer cb, BGPOpenMsgVer4 message) {
        int startIndex = cb.writerIndex();
        int capParaLen = 0;
        int capParaLenIndex = 0;

        LinkedList<BGPValueType> capabilityTlv = message.capabilityTlv;
        ListIterator<BGPValueType> listIterator = capabilityTlv.listIterator();

        if (listIterator.hasNext()) {
            // Set optional parameter type as 2
            cb.writeByte(OPT_PARA_TYPE_CAPABILITY);

            // Store the index of capability parameter length and update length at the end
            capParaLenIndex = cb.writerIndex();

            // Set capability parameter length as 0
            cb.writeByte(0);

            // Update the startIndex to know the length of capability tlv
            startIndex = cb.writerIndex();
        }

        while (listIterator.hasNext()) {
            BGPValueType tlv = listIterator.next();
            if (tlv == null) {
                log.debug("Warning: tlv is null from CapabilityTlv list");
                continue;
            }
            tlv.write(cb);
        }

        capParaLen = cb.writerIndex() - startIndex;

        if (capParaLen != 0) {
            // Update capability parameter length
            cb.setByte(capParaLenIndex, (byte) capParaLen);
        }
        return capParaLen;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
            .add("bgpMsgHeader", bgpMsgHeader)
            .add("version", version)
            .add("holdTime", holdTime)
            .add("asNumber", asNumber)
            .add("bgpId", bgpId)
            .add("capabilityTlv", capabilityTlv)
            .toString();
    }
}
