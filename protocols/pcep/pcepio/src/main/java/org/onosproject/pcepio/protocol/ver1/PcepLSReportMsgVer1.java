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

package org.onosproject.pcepio.protocol.ver1;

import java.util.List;
import java.util.LinkedList;
import java.util.ListIterator;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepMessageReader;
import org.onosproject.pcepio.protocol.PcepMessageWriter;
import org.onosproject.pcepio.protocol.PcepLSObject;
import org.onosproject.pcepio.protocol.PcepLSReportMsg;
import org.onosproject.pcepio.protocol.PcepType;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides  PCEP LS (link-state) Report Message.
 */
class PcepLSReportMsgVer1 implements PcepLSReportMsg {

    /*
     * Ref : draft-dhodylee-pce-pcep-ls-01, section 8.1

        <LSRpt Message>  ::=  <Common Header>
                              <ls-report-list>
    Where:
        <ls-report-list> ::=  <LS>[<ls-report-list>]
     */

    private static final Logger log = LoggerFactory.getLogger(PcepLSReportMsgVer1.class);
    //PACKET_MINIMUM_LENGTH = CommonHeaderLen(4)+LSObjMinLen(12)
    public static final int PACKET_MINIMUM_LENGTH = 16;
    public static final PcepType MSG_TYPE = PcepType.LS_REPORT;
    // <ls-report-list>
    private List<PcepLSObject> lsReportList;

    public static final PcepLSReportMsgVer1.Reader READER = new Reader();

    /**
     * Reader class for reading PCEP LS-Report message form channel buffer.
     */
    static class Reader implements PcepMessageReader<PcepLSReportMsg> {

        List<PcepLSObject> lsReportList;

        @Override
        public PcepLSReportMsg readFrom(ChannelBuffer cb) throws PcepParseException {

            if (cb.readableBytes() < PACKET_MINIMUM_LENGTH) {
                return null;
            }

            lsReportList = new LinkedList<>();

            byte version = cb.readByte();
            version = (byte) (version >> PcepMessageVer1.SHIFT_FLAG);
            if (version != PcepMessageVer1.PACKET_VERSION) {
                throw new PcepParseException("Wrong version. Expected=PcepVersion.PCEP_1(1), got=" + version);
            }

            byte type = cb.readByte();
            if (type != MSG_TYPE.getType()) {
                throw new PcepParseException("Wrong type. Expected=PcepType.LS_REPORT(224), got=" + type);
            }

            short length = cb.readShort();
            if (length < PACKET_MINIMUM_LENGTH) {
                throw new PcepParseException(
                        "Wrong length. Expected to be >= " + PACKET_MINIMUM_LENGTH + ", is: " + length);
            }

            // Parse <ls-report-list>
            parseLSReportList(cb);

            return new PcepLSReportMsgVer1(lsReportList);
        }

        /**
         * Parse ls-report-list.
         *
         * @param cb input Channel Buffer
         * @throws PcepParseException when fails to parse LS-Report list.
         */
        public void parseLSReportList(ChannelBuffer cb) throws PcepParseException {
            // <ls-report-list> ::= <LS>[<ls-report-list>]

            while (0 < cb.readableBytes()) {
                //store LS objects
                if (!lsReportList.add(PcepLSObjectVer1.read(cb))) {
                    throw new PcepParseException("Failed to add LS object to LS-Report list");
                }
            }
        }
    }

    /**
     * Constructor to initialize LS-Report list.
     *
     * @param lsReportList list of PCEP LS Object
     */
    PcepLSReportMsgVer1(List<PcepLSObject> lsReportList) {
        this.lsReportList = lsReportList;
    }

    /**
     * Builder class for PCEP LS-Report message.
     */
    static class Builder implements PcepLSReportMsg.Builder {
        // PCEP LS Report message fields
        List<PcepLSObject> lsReportList;

        @Override
        public PcepVersion getVersion() {
            return PcepVersion.PCEP_1;
        }

        @Override
        public PcepType getType() {
            return PcepType.LS_REPORT;
        }

        @Override
        public PcepLSReportMsg build() {
            return new PcepLSReportMsgVer1(this.lsReportList);
        }

        @Override
        public List<PcepLSObject> getLSReportList() {
            return this.lsReportList;
        }

        @Override
        public Builder setLSReportList(List<PcepLSObject> ll) {
            this.lsReportList = ll;
            return this;
        }
    }

    @Override
    public void writeTo(ChannelBuffer bb) throws PcepParseException {
        WRITER.write(bb, this);
    }

    static final Writer WRITER = new Writer();

    /**
     * Writer class for writing PCEP LS-Report message to channel buffer.
     */
    static class Writer implements PcepMessageWriter<PcepLSReportMsgVer1> {

        @Override
        public void write(ChannelBuffer bb, PcepLSReportMsgVer1 message) throws PcepParseException {

            int startIndex = bb.writerIndex();

            // first 3 bits set to version
            bb.writeByte((byte) (PcepMessageVer1.PACKET_VERSION << PcepMessageVer1.SHIFT_FLAG));

            // message type
            bb.writeByte(MSG_TYPE.getType());

            // Length of the message will be updated at the end
            // First write with 0s
            int msgLenIndex = bb.writerIndex();
            bb.writeShort((short) 0);

            ListIterator<PcepLSObject> listIterator = message.lsReportList.listIterator();

            while (listIterator.hasNext()) {
                PcepLSObject lsObj = listIterator.next();
                lsObj.write(bb);
            }

            // update message length field
            int length = bb.writerIndex() - startIndex;
            bb.setShort(msgLenIndex, (short) length);
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
    public List<PcepLSObject> getLSReportList() {
        return this.lsReportList;
    }

    @Override
    public void setLSReportList(List<PcepLSObject> ll) {
        this.lsReportList = ll;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("LSReportList", lsReportList)
                .toString();
    }
}
