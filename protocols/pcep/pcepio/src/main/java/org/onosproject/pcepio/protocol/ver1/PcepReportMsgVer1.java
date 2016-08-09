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
import org.onosproject.pcepio.protocol.PcepLspObject;
import org.onosproject.pcepio.protocol.PcepMessageReader;
import org.onosproject.pcepio.protocol.PcepMessageWriter;
import org.onosproject.pcepio.protocol.PcepReportMsg;
import org.onosproject.pcepio.protocol.PcepSrpObject;
import org.onosproject.pcepio.protocol.PcepStateReport;
import org.onosproject.pcepio.protocol.PcepType;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP report message.
 */
class PcepReportMsgVer1 implements PcepReportMsg {

    // Pcep version: 1

    /*
     * The format of the PCRpt message is as follows:
     *   <PCRpt Message>        ::= <Common Header>
     *                              <state-report-list>
     *Where:
     *   <state-report-list>    ::= <state-report>[<state-report-list>]
     *   <state-report>         ::= [<SRP>]
     *                              <LSP>
     *                              <path>
     * Where:
     *   <path>                 ::= <ERO><attribute-list>[<RRO>]
     *   Where:
     *   <attribute-list> is defined in [RFC5440] and extended by PCEP extensions.
     *    where:
     *    <attribute-list>      ::=[<LSPA>]
     *                             [<BANDWIDTH>]
     *                             [<metric-list>]
     *                             [<IRO>]
     *    <metric-list>       ::=<METRIC>[<metric-list>]
     */
    protected static final Logger log = LoggerFactory.getLogger(PcepReportMsgVer1.class);

    public static final byte PACKET_VERSION = 1;
    //PACKET_MINIMUM_LENGTH = CommonHeaderLen(4)+LspObjMinLen(8)
    public static final int PACKET_MINIMUM_LENGTH = 12;
    public static final PcepType MSG_TYPE = PcepType.REPORT;
    public static final byte REPORT_OBJ_TYPE = 1;
    //Optional TLV
    private LinkedList<PcepStateReport> llStateReportList;

    public static final PcepReportMsgVer1.Reader READER = new Reader();

    /**
     * Reader class for reading PCEP report message from channel buffer.
     */
    static class Reader implements PcepMessageReader<PcepReportMsg> {

        LinkedList<PcepStateReport> llStateReportList;

        @Override
        public PcepReportMsg readFrom(ChannelBuffer cb) throws PcepParseException {

            if (cb.readableBytes() < PACKET_MINIMUM_LENGTH) {
                throw new PcepParseException("Received packet size " + cb.readableBytes()
                        + " is less than the expected size: " + PACKET_MINIMUM_LENGTH);
            }
            llStateReportList = new LinkedList<>();
            byte version = cb.readByte();
            version = (byte) (version >> PcepMessageVer1.SHIFT_FLAG);

            if (version != PACKET_VERSION) {
                throw new PcepParseException(" Invalid version: " + version);
            }

            byte type = cb.readByte();

            if (type != MSG_TYPE.getType()) {
                throw new PcepParseException("Unexpected type: " + type);
            }

            short length = cb.readShort();

            if (length < PACKET_MINIMUM_LENGTH) {
                throw new PcepParseException("Wrong length. Expected to be >= " + PACKET_MINIMUM_LENGTH + ", was: "
                        + length);
            }
            // parse state report list
            parseStateReportList(cb);
            return new PcepReportMsgVer1(llStateReportList);
        }

        // Parse State Report list
        public void parseStateReportList(ChannelBuffer cb) throws PcepParseException {

            /*
                                <state-report-list>
            Where:
                    <state-report-list>     ::= <state-report>[<state-report-list>]
                    <state-report>          ::=  [<SRP>]
                                                  <LSP>
                                                  <path>
            Where:
                    <path>                  ::= <ERO><attribute-list>[<RRO>]
            Where:
                    <attribute-list> is defined in [RFC5440] and extended by PCEP extensions.

             */

            while (0 < cb.readableBytes()) {

                PcepStateReport pcestateReq = new PcepStateReportVer1();

                /*
                 * SRP is optional
                 * Check whether SRP Object is available, if yes store it.
                 * First read common object header and check the Object Class whether it is SRP or LSP
                 * If it is LSP then store only LSP. So, SRP is optional. then read path and store.
                 * If it is SRP then store SRP and then read LSP, path and store them.
                 */

                //mark the reader index to reset
                cb.markReaderIndex();
                PcepObjectHeader tempObjHeader = PcepObjectHeader.read(cb);

                byte yObjectClass = tempObjHeader.getObjClass();
                byte yObjectType = tempObjHeader.getObjType();

                //reset reader index
                cb.resetReaderIndex();
                //If SRP present then store it.
                if ((PcepSrpObjectVer1.SRP_OBJ_CLASS == yObjectClass)
                        && (PcepSrpObjectVer1.SRP_OBJ_TYPE == yObjectType)) {
                    PcepSrpObject srpObj;
                    srpObj = PcepSrpObjectVer1.read(cb);
                    pcestateReq.setSrpObject(srpObj);
                }

                //store LSP object
                PcepLspObject lspObj;
                lspObj = PcepLspObjectVer1.read(cb);
                pcestateReq.setLspObject(lspObj);

                if (cb.readableBytes() > 0) {

                    //mark the reader index to reset
                    cb.markReaderIndex();
                    tempObjHeader = PcepObjectHeader.read(cb);

                    yObjectClass = tempObjHeader.getObjClass();
                    yObjectType = tempObjHeader.getObjType();

                    //reset reader index
                    cb.resetReaderIndex();

                    if ((PcepEroObjectVer1.ERO_OBJ_CLASS == yObjectClass)
                            && (PcepEroObjectVer1.ERO_OBJ_TYPE == yObjectType)) {
                        // store path
                        PcepStateReport.PcepMsgPath msgPath = new PcepStateReportVer1().new PcepMsgPath().read(cb);
                        pcestateReq.setMsgPath(msgPath);
                    }
                }

                llStateReportList.add(pcestateReq);
            }
        }
    }

    /**
     * Constructor to initialize State Report List.
     *
     * @param llStateReportList list of type Pcep state report
     */
    PcepReportMsgVer1(LinkedList<PcepStateReport> llStateReportList) {
        this.llStateReportList = llStateReportList;
    }

    /**
     * Builder class for PCEP Report message.
     */
    static class Builder implements PcepReportMsg.Builder {
        // Pcep report message fields
        LinkedList<PcepStateReport> llStateReportList;

        @Override
        public PcepVersion getVersion() {
            return PcepVersion.PCEP_1;
        }

        @Override
        public PcepType getType() {
            return PcepType.REPORT;
        }

        @Override
        public PcepReportMsg build() {
            return new PcepReportMsgVer1(this.llStateReportList);
        }

        @Override
        public LinkedList<PcepStateReport> getStateReportList() {
            return this.llStateReportList;
        }

        @Override
        public Builder setStateReportList(LinkedList<PcepStateReport> ll) {
            this.llStateReportList = ll;
            return this;
        }
    }

    @Override
    public void writeTo(ChannelBuffer cb) throws PcepParseException {
        WRITER.write(cb, this);
    }

    static final Writer WRITER = new Writer();

    /**
     * Writer class for writing PCEP report message to channel buffer.
     */
    static class Writer implements PcepMessageWriter<PcepReportMsgVer1> {

        @Override
        public void write(ChannelBuffer cb, PcepReportMsgVer1 message) throws PcepParseException {

            int startIndex = cb.writerIndex();

            // first 3 bits set to version
            cb.writeByte((byte) (PACKET_VERSION << PcepMessageVer1.SHIFT_FLAG));

            // message type
            cb.writeByte(MSG_TYPE.getType());

            // length is length of variable message, will be updated at the end
            // Store the position of message
            // length in buffer
            int msgLenIndex = cb.writerIndex();

            cb.writeShort((short) 0);
            ListIterator<PcepStateReport> listIterator = message.llStateReportList.listIterator();

            while (listIterator.hasNext()) {

                PcepStateReport stateRpt = listIterator.next();
                PcepSrpObject srpObj = stateRpt.getSrpObject();

                //SRP object is optional
                if (srpObj != null) {
                    srpObj.write(cb);
                }

                //LSP object is mandatory
                PcepLspObject lspObj = stateRpt.getLspObject();
                if (lspObj == null) {
                    throw new PcepParseException("LSP Object is mandatory object for PcRpt message.");
                } else {
                    lspObj.write(cb);
                }

                //path is mandatory
                PcepStateReport.PcepMsgPath msgPath = stateRpt.getMsgPath();
                if (msgPath == null) {
                    throw new PcepParseException("Message path is mandatory object for PcRpt message.");
                } else {
                    msgPath.write(cb);
                }
            }

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
    public LinkedList<PcepStateReport> getStateReportList() {
        return this.llStateReportList;
    }

    @Override
    public void setStateReportList(LinkedList<PcepStateReport> ll) {
        this.llStateReportList = ll;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("StateReportList", llStateReportList)
                .toString();
    }
}
