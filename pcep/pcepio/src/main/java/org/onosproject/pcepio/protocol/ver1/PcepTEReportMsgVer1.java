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
import org.onosproject.pcepio.protocol.PcepMessageReader;
import org.onosproject.pcepio.protocol.PcepMessageWriter;
import org.onosproject.pcepio.protocol.PcepTEObject;
import org.onosproject.pcepio.protocol.PcepTEReportMsg;
import org.onosproject.pcepio.protocol.PcepType;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides  PCEP TE Report Message.
 */
class PcepTEReportMsgVer1 implements PcepTEReportMsg {

    /*
     * Ref : draft-dhodylee-pce-pcep-te-data-extn-02, section 8.1

        <TERpt Message>  ::=  <Common Header>
                              <te-report-list>
    Where:
        <te-report-list> ::=  <TE>[<te-report-list>]
     */

    private static final Logger log = LoggerFactory.getLogger(PcepTEReportMsgVer1.class);
    //PACKET_MINIMUM_LENGTH = CommonHeaderLen(4)+TEObjMinLen(12)
    public static final int PACKET_MINIMUM_LENGTH = 16;
    public static final PcepType MSG_TYPE = PcepType.TE_REPORT;
    // <te-report-list>
    private LinkedList<PcepTEObject> teReportList;

    public static final PcepTEReportMsgVer1.Reader READER = new Reader();

    /**
     * Reader class for reading PCPE te report message form channel buffer.
     */
    static class Reader implements PcepMessageReader<PcepTEReportMsg> {

        LinkedList<PcepTEObject> teReportList;

        @Override
        public PcepTEReportMsg readFrom(ChannelBuffer cb) throws PcepParseException {

            if (cb.readableBytes() < PACKET_MINIMUM_LENGTH) {
                return null;
            }

            teReportList = new LinkedList<>();

            byte version = cb.readByte();
            version = (byte) (version >> PcepMessageVer1.SHIFT_FLAG);
            if (version != PcepMessageVer1.PACKET_VERSION) {
                throw new PcepParseException("Wrong version. Expected=PcepVersion.PCEP_1(1), got=" + version);
            }

            byte type = cb.readByte();
            if (type != MSG_TYPE.getType()) {
                throw new PcepParseException("Wrong type. Expected=PcepType.TE_REPORT(14), got=" + type);
            }

            short length = cb.readShort();
            if (length < PACKET_MINIMUM_LENGTH) {
                throw new PcepParseException(
                        "Wrong length. Expected to be >= " + PACKET_MINIMUM_LENGTH + ", is: " + length);
            }

            // Parse state report list
            parseTEReportList(cb);

            return new PcepTEReportMsgVer1(teReportList);
        }

        /**
         * Parse te-report-list.
         *
         * @param cb input Channel Buffer
         * @throws PcepParseException when fails to parse TE Report list.
         */
        public void parseTEReportList(ChannelBuffer cb) throws PcepParseException {
            // <te-report-list> ::= <TE>[<te-report-list>]

            while (0 < cb.readableBytes()) {
                //store TE objectS
                if (!teReportList.add(PcepTEObjectVer1.read(cb))) {
                    throw new PcepParseException("Failed to add TE object to TE report list");
                }
            }
        }
    }

    /**
     * Constructor to initialize TE Report List.
     *
     * @param teReportList list of PCEP TE Object
     */
    PcepTEReportMsgVer1(LinkedList<PcepTEObject> teReportList) {
        this.teReportList = teReportList;
    }

    /**
     * Builder class for PCEP te report message.
     */
    static class Builder implements PcepTEReportMsg.Builder {
        // PCEP TE Report message fields
        LinkedList<PcepTEObject> teReportList;

        @Override
        public PcepVersion getVersion() {
            return PcepVersion.PCEP_1;
        }

        @Override
        public PcepType getType() {
            return PcepType.TE_REPORT;
        }

        @Override
        public PcepTEReportMsg build() {
            return new PcepTEReportMsgVer1(this.teReportList);
        }

        @Override
        public LinkedList<PcepTEObject> getTEReportList() {
            return this.teReportList;
        }

        @Override
        public Builder setTEReportList(LinkedList<PcepTEObject> ll) {
            this.teReportList = ll;
            return this;
        }
    }

    @Override
    public void writeTo(ChannelBuffer bb) throws PcepParseException {
        WRITER.write(bb, this);
    }

    static final Writer WRITER = new Writer();

    /**
     * Writer class for writing PCEP te report message to channel buffer.
     */
    static class Writer implements PcepMessageWriter<PcepTEReportMsgVer1> {

        @Override
        public void write(ChannelBuffer bb, PcepTEReportMsgVer1 message) throws PcepParseException {

            int startIndex = bb.writerIndex();

            // first 3 bits set to version
            bb.writeByte((byte) (PcepMessageVer1.PACKET_VERSION << PcepMessageVer1.SHIFT_FLAG));

            // message type
            bb.writeByte(MSG_TYPE.getType());

            // Length of the message will be updated at the end
            // First write with 0s
            int msgLenIndex = bb.writerIndex();
            bb.writeShort((short) 0);

            ListIterator<PcepTEObject> listIterator = message.teReportList.listIterator();

            while (listIterator.hasNext()) {
                PcepTEObject teObj = listIterator.next();
                teObj.write(bb);
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
    public LinkedList<PcepTEObject> getTEReportList() {
        return this.teReportList;
    }

    @Override
    public void setTEReportList(LinkedList<PcepTEObject> ll) {
        this.teReportList = ll;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("TeReportList", teReportList)
                .toString();
    }
}
