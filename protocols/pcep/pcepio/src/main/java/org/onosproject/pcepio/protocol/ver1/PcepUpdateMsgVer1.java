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
import org.onosproject.pcepio.protocol.PcepMsgPath;
import org.onosproject.pcepio.protocol.PcepSrpObject;
import org.onosproject.pcepio.protocol.PcepType;
import org.onosproject.pcepio.protocol.PcepUpdateMsg;
import org.onosproject.pcepio.protocol.PcepUpdateRequest;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP update message.
 */

class PcepUpdateMsgVer1 implements PcepUpdateMsg {

    // Pcep version: 1

    /*    The format of the PCUpd message is as follows:
     *      <PCUpd Message>             ::= <Common Header>
     *                                       <update-request-list>
     *      Where:
     *        <update-request-list>     ::= <update-request>[<update-request-list>]
     *        <update-request>          ::= <SRP>
     *                                      <LSP>
     *                                      <path>
     *      Where:
     *        <path>                     ::= <ERO><attribute-list>
     *       Where:
     *        <attribute-list> is defined in [RFC5440] and extended by PCEP extensions.
     *       where:
     *        <attribute-list>            ::=[<LSPA>]
     *                                      [<BANDWIDTH>]
     *                                      [<metric-list>]
     *                                      [<IRO>]
     *        <metric-list>               ::=<METRIC>[<metric-list>]
     *
     *            0                   1                   2                   3
     *           0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     *          +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *          | Ver |  Flags  |  Message-Type |       Message-Length          |
     *          +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *          |                                                               |
     *          //                  UPDATE REQUEST LIST                        //
     *          |                                                               |
     *          +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *
     *          Reference:Internet-Draft-PCEP Extensions-for-Stateful-PCE-10
     */

    protected static final Logger log = LoggerFactory.getLogger(PcepUpdateMsgVer1.class);

    public static final byte PACKET_VERSION = 1;
    // UpdateMsgMinLength = SrpObjMinLentgh(12)+LspObjMinLength(8)+EroObjMinLength(12)+ CommonHeaderLength(4)
    public static final short PACKET_MINIMUM_LENGTH = 36;
    public static final PcepType MSG_TYPE = PcepType.UPDATE;
    //Update Request List
    private LinkedList<PcepUpdateRequest> llUpdateRequestList;

    public static final PcepUpdateMsgVer1.Reader READER = new Reader();

    /**
     * Reader reads UpdateMessage from the channel.
     */
    static class Reader implements PcepMessageReader<PcepUpdateMsg> {

        LinkedList<PcepUpdateRequest> llUpdateRequestList;

        @Override
        public PcepUpdateMsg readFrom(ChannelBuffer cb) throws PcepParseException {

            if (cb.readableBytes() < PACKET_MINIMUM_LENGTH) {
                throw new PcepParseException("Readable bytes is less than update message minimum length");
            }

            llUpdateRequestList = new LinkedList<>();

            // fixed value property version == 1
            byte version = cb.readByte();
            version = (byte) (version >> PcepMessageVer1.SHIFT_FLAG);
            if (version != PACKET_VERSION) {
                throw new PcepParseException("Wrong version. Expected=PcepVersion.PCEP_1(1), got=" + version);
            }
            // fixed value property type == 11
            byte type = cb.readByte();
            if (type != MSG_TYPE.getType()) {
                throw new PcepParseException("Wrong type. Expected=PcepType.UPDATE(11), got=" + type);
            }
            short length = cb.readShort();
            if (length < PACKET_MINIMUM_LENGTH) {
                throw new PcepParseException("Wrong length. Expected to be >= " + PACKET_MINIMUM_LENGTH + ", was: "
                        + length);
            }

            log.debug("reading update message of length " + length);

            // parse Update Request list
            if (!parseUpdateRequestList(cb)) {
                throw new PcepParseException("parsing Update Request List Failed.");
            }

            return new PcepUpdateMsgVer1(llUpdateRequestList);
        }

        /**
         * Parse update request list.
         *
         * @param cb of type channel buffer
         * @return true after parsing Update Request List
         * @throws PcepParseException while parsing update request list from channel buffer
         */
        public boolean parseUpdateRequestList(ChannelBuffer cb) throws PcepParseException {

            /*                     <update-request-list>
             * Where:
             *   <update-request-list>     ::= <update-request>[<update-request-list>]
             *   <update-request>          ::= <SRP>
             *                                 <LSP>
             *                                 <path>
             * Where:
             *   <path>                     ::= <ERO><attribute-list>
             * Where:
             * <attribute-list> is defined in [RFC5440] and extended by PCEP extensions.
             */

            while (0 < cb.readableBytes()) {

                PcepUpdateRequest pceUpdateReq = new PcepUpdateRequestVer1();

                //Read SRP Object and Store it.
                PcepSrpObject srpObj;
                srpObj = PcepSrpObjectVer1.read(cb);
                pceUpdateReq.setSrpObject(srpObj);

                //Read LSP object and Store it.
                PcepLspObject lspObj;
                lspObj = PcepLspObjectVer1.read(cb);
                pceUpdateReq.setLspObject(lspObj);

                // Read Msg Path and store it.
                PcepMsgPath msgPath = new PcepMsgPathVer1().read(cb);
                pceUpdateReq.setMsgPath(msgPath);

                llUpdateRequestList.add(pceUpdateReq);
            }
            return true;
        }
    }

    /**
     * Constructor to initialize llUpdateRequestList.
     *
     * @param llUpdateRequestList list of PcepUpdateRequest.
     */
    PcepUpdateMsgVer1(LinkedList<PcepUpdateRequest> llUpdateRequestList) {
        this.llUpdateRequestList = llUpdateRequestList;
    }

    /**
     * Builder class for PCPE update message.
     */
    static class Builder implements PcepUpdateMsg.Builder {

        // PCEP report message fields
        LinkedList<PcepUpdateRequest> llUpdateRequestList;

        @Override
        public PcepVersion getVersion() {
            return PcepVersion.PCEP_1;
        }

        @Override
        public PcepType getType() {
            return PcepType.UPDATE;
        }

        @Override
        public PcepUpdateMsg build() {
            return new PcepUpdateMsgVer1(this.llUpdateRequestList);
        }

        @Override
        public LinkedList<PcepUpdateRequest> getUpdateRequestList() {
            return this.llUpdateRequestList;
        }

        @Override
        public Builder setUpdateRequestList(LinkedList<PcepUpdateRequest> llUpdateRequestList) {
            this.llUpdateRequestList = llUpdateRequestList;
            return this;
        }

    }

    @Override
    public void writeTo(ChannelBuffer cb) throws PcepParseException {
        WRITER.write(cb, this);
    }

    static final Writer WRITER = new Writer();

    /**
     * Writer writes UpdateMessage to the channel buffer.
     */
    static class Writer implements PcepMessageWriter<PcepUpdateMsgVer1> {

        @Override
        public void write(ChannelBuffer cb, PcepUpdateMsgVer1 message) throws PcepParseException {

            int startIndex = cb.writerIndex();
            // first 3 bits set to version
            cb.writeByte((byte) (PACKET_VERSION << PcepMessageVer1.SHIFT_FLAG));
            // message type
            cb.writeByte(MSG_TYPE.getType());
            /* length is length of variable message, will be updated at the end
             * Store the position of message
             * length in buffer
             */
            int msgLenIndex = cb.writerIndex();

            cb.writeShort((short) 0);
            ListIterator<PcepUpdateRequest> listIterator = message.llUpdateRequestList.listIterator();

            while (listIterator.hasNext()) {

                PcepUpdateRequest updateReq = listIterator.next();

                //SRP object is mandatory
                PcepSrpObject srpObj = updateReq.getSrpObject();
                srpObj.write(cb);

                //LSP object is mandatory
                PcepLspObject lspObj = updateReq.getLspObject();
                lspObj.write(cb);

                //PATH object is mandatory
                PcepMsgPath msgPath = updateReq.getMsgPath();
                msgPath.write(cb);
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
    public LinkedList<PcepUpdateRequest> getUpdateRequestList() {
        return this.llUpdateRequestList;
    }

    @Override
    public void setUpdateRequestList(LinkedList<PcepUpdateRequest> llUpdateRequestList) {
        this.llUpdateRequestList = llUpdateRequestList;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("UpdateRequestList", llUpdateRequestList)
                .toString();
    }
}
