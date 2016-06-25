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
import org.onosproject.pcepio.protocol.PcInitiatedLspRequest;
import org.onosproject.pcepio.protocol.PcepAttribute;
import org.onosproject.pcepio.protocol.PcepEndPointsObject;
import org.onosproject.pcepio.protocol.PcepEroObject;
import org.onosproject.pcepio.protocol.PcepInitiateMsg;
import org.onosproject.pcepio.protocol.PcepLspObject;
import org.onosproject.pcepio.protocol.PcepMessageReader;
import org.onosproject.pcepio.protocol.PcepMessageWriter;
import org.onosproject.pcepio.protocol.PcepSrpObject;
import org.onosproject.pcepio.protocol.PcepType;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP initiate message.
 */
class PcepInitiateMsgVer1 implements PcepInitiateMsg {

    protected static final Logger log = LoggerFactory.getLogger(PcepInitiateMsgVer1.class);

    // Ref : PCE initiated tunnel setup draft-ietf-pce-pce-initiated-lsp-03, section 5.1
    /*      <PCInitiate Message>             ::= <Common Header>
     *                                           <PCE-initiated-lsp-list>
     *    Where:
     *      <PCE-initiated-lsp-list>          ::= <PCE-initiated-lsp-request>[<PCE-initiated-lsp-list>]
     *      <PCE-initiated-lsp-request>       ::= (<PCE-initiated-lsp-instantiation>|<PCE-initiated-lsp-deletion>)
     *      <PCE-initiated-lsp-instantiation> ::= <SRP>
     *                                            <LSP>
     *                                            <END-POINTS>
     *                                            <ERO>
     *                                            [<attribute-list>]
     *     <PCE-initiated-lsp-deletion>      ::= <SRP>
     *                                           <LSP>
     */

    static final byte PACKET_VERSION = 1;
    /* considering LspDelete Request PcInitiate msg will contain
     * common header
     * srp object
     * lsp object
     * so min length for this can be
     * PACKET_MINIMUM_LENGTH = CommonHeaderLen(4)+SrpObjectMinLen(12)+LspObjectMinLen(8)
     */
    public static final short PACKET_MINIMUM_LENGTH = 24;
    public static final short MINIMUM_COMMON_HEADER_LENGTH = 4;
    public static final PcepType MSG_TYPE = PcepType.INITIATE;
    private LinkedList<PcInitiatedLspRequest> llPcInitiatedLspRequestList;
    public static final PcepInitiateMsgVer1.Reader READER = new Reader();

    /**
     * Reader class for reading of Pcep initiate message from channel buffer.
     */
    static class Reader implements PcepMessageReader<PcepInitiateMsg> {

        LinkedList<PcInitiatedLspRequest> llPcInitiatedLspRequestList;

        @Override
        public PcepInitiateMsg readFrom(ChannelBuffer cb) throws PcepParseException {

            if (cb.readableBytes() < PACKET_MINIMUM_LENGTH) {
                return null;
            }

            llPcInitiatedLspRequestList = new LinkedList<>();

            byte version = cb.readByte();
            version = (byte) (version >> PcepMessageVer1.SHIFT_FLAG);
            if (version != PACKET_VERSION) {
                throw new PcepParseException("Wrong version. Expected=PcepVersion.PCEP_1(1), received=" + version);
            }
            byte type = cb.readByte();
            if (type != MSG_TYPE.getType()) {
                throw new PcepParseException("Wrong type. Expected=PcepType.INITIATE(12), recived=" + type);
            }
            short length = cb.readShort();

            if (length < PACKET_MINIMUM_LENGTH) {
                throw new PcepParseException("Wrong length. Initiate message length expected to be >= "
                        + PACKET_MINIMUM_LENGTH + ", but received=" + length);
            }

            log.debug("reading PcInitiate message of length " + length);

            // parse Start initiate/deletion list
            if (!parsePcInitiatedLspRequestList(cb)) {
                throw new PcepParseException("Parsing PCE-initiated-lsp-Request-list failed");
            }

            return new PcepInitiateMsgVer1(llPcInitiatedLspRequestList);
        }

        /**
         * To parse PcInitiatedLspRequestList from PcInitiate Message.
         *
         * @param cb of type channel buffer
         * @return true if parsing PcInitiatedLspRequestList is success, false otherwise
         * @throws PcepParseException while parsing from channel buffer
         */
        public boolean parsePcInitiatedLspRequestList(ChannelBuffer cb) throws PcepParseException {

            boolean isDelLspRequest = false;

            if (cb == null) {
                throw new PcepParseException("Channel buffer is empty");
            }

            while (0 < cb.readableBytes()) {
                PcInitiatedLspRequest pceInitLspReq = new PcInitiatedLspRequestVer1();

                //store SRP object
                PcepSrpObject srpObj;
                srpObj = PcepSrpObjectVer1.read(cb);
                pceInitLspReq.setSrpObject(srpObj);
                isDelLspRequest = srpObj.getRFlag();

                //store LSP object
                PcepLspObject lspObj;
                lspObj = PcepLspObjectVer1.read(cb);
                pceInitLspReq.setLspObject(lspObj);

                /* if R bit will be set then pcInitiate msg will contain only LSp and SRP objects
                 * so if R bit is not set then we should read for Ero and EndPoint objects also.
                 */
                if (!isDelLspRequest) {

                    //store EndPoint object
                    PcepEndPointsObject endPointObj;
                    endPointObj = PcepEndPointsObjectVer1.read(cb);
                    pceInitLspReq.setEndPointsObject(endPointObj);

                    //store ERO object
                    PcepEroObject eroObj;
                    eroObj = PcepEroObjectVer1.read(cb);
                    pceInitLspReq.setEroObject(eroObj);

                    if (cb.readableBytes() > MINIMUM_COMMON_HEADER_LENGTH) {
                        pceInitLspReq.setPcepAttribute(PcepAttributeVer1.read(cb));
                    }
                }
                llPcInitiatedLspRequestList.add(pceInitLspReq);
            }
            return true;
        }
    }

    /**
     * Constructor to initialize PcInitiatedLspRequest.
     *
     * @param llPcInitiatedLspRequestList list of PcInitiatedLspRequest
     */
    PcepInitiateMsgVer1(LinkedList<PcInitiatedLspRequest> llPcInitiatedLspRequestList) {

        if (llPcInitiatedLspRequestList == null) {
            throw new NullPointerException("PcInitiatedLspRequestList cannot be null.");
        }
        this.llPcInitiatedLspRequestList = llPcInitiatedLspRequestList;
    }

    /**
     * Builder class for PCEP initiate message.
     */
    static class Builder implements PcepInitiateMsg.Builder {

        // Pcep initiate message fields
        LinkedList<PcInitiatedLspRequest> llPcInitiatedLspRequestList;

        @Override
        public PcepVersion getVersion() {
            return PcepVersion.PCEP_1;
        }

        @Override
        public PcepType getType() {
            return PcepType.INITIATE;
        }

        @Override
        public PcepInitiateMsg build() {
            return new PcepInitiateMsgVer1(this.llPcInitiatedLspRequestList);
        }

        @Override
        public LinkedList<PcInitiatedLspRequest> getPcInitiatedLspRequestList() {
            return this.llPcInitiatedLspRequestList;
        }

        @Override
        public Builder setPcInitiatedLspRequestList(LinkedList<PcInitiatedLspRequest> ll) {
            this.llPcInitiatedLspRequestList = ll;
            return this;
        }
    }

    @Override
    public void writeTo(ChannelBuffer cb) throws PcepParseException {
        WRITER.write(cb, this);
    }

    static final Writer WRITER = new Writer();

    /**
     * Writer class for writing pcep initiate message to channel buffer.
     */
    static class Writer implements PcepMessageWriter<PcepInitiateMsgVer1> {

        @Override
        public void write(ChannelBuffer cb, PcepInitiateMsgVer1 message) throws PcepParseException {

            boolean isDelLspRequest = false;
            int startIndex = cb.writerIndex();
            // first 3 bits set to version
            cb.writeByte((byte) (PACKET_VERSION << PcepMessageVer1.SHIFT_FLAG));
            // message type 0xC
            cb.writeByte(MSG_TYPE.getType());
            // length is length of variable message, will be updated at the end
            // Store the position of message
            // length in buffer
            int msgLenIndex = cb.writerIndex();
            cb.writeShort(0);

            ListIterator<PcInitiatedLspRequest> listIterator = message.llPcInitiatedLspRequestList.listIterator();

            while (listIterator.hasNext()) {

                PcInitiatedLspRequest listReq = listIterator.next();

                //Srp Object is mandatory
                PcepSrpObject srpObj = listReq.getSrpObject();
                if (srpObj != null) {
                    isDelLspRequest = srpObj.getRFlag();
                    srpObj.write(cb);
                } else {
                    throw new PcepParseException("SRP Object is mandatory for PcInitiate message.");
                }

                //LSP Object is mandatory
                PcepLspObject lspObj = listReq.getLspObject();
                if (lspObj != null) {
                    lspObj.write(cb);
                } else {
                    throw new PcepParseException("LSP Object is mandatory for PcInitiate message.");
                }

                /* if R bit will be set then pcInitiate msg will contain only LSp and SRP objects
                 * so if R bit is not set then we should read for Ero and EndPoint objects also.
                 */

                if (!isDelLspRequest) {

                    //EndPoints object is mandatory
                    PcepEndPointsObject endPointObj = listReq.getEndPointsObject();
                    if (endPointObj != null) {
                        endPointObj.write(cb);
                    } else {
                        throw new PcepParseException("End points Object is mandatory for PcInitiate message.");
                    }

                    //Ero object is mandatory
                    PcepEroObject eroObj = listReq.getEroObject();
                    if (eroObj != null) {
                        eroObj.write(cb);
                    } else {
                        throw new PcepParseException("ERO Object is mandatory for PcInitiate message.");
                    }

                    //PcepAttribute is optional
                    PcepAttribute pcepAttribute = listReq.getPcepAttribute();
                    if (pcepAttribute != null) {
                        pcepAttribute.write(cb);
                    }
                }
            }

            // PCInitiate message length field
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
    public LinkedList<PcInitiatedLspRequest> getPcInitiatedLspRequestList() {
        return this.llPcInitiatedLspRequestList;
    }

    @Override
    public void setPcInitiatedLspRequestList(LinkedList<PcInitiatedLspRequest> ll) {
        this.llPcInitiatedLspRequestList = ll;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("PcInitiaitedLspRequestList", llPcInitiatedLspRequestList)
                .toString();
    }
}
