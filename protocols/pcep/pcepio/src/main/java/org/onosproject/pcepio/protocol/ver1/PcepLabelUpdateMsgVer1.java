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
import org.onosproject.pcepio.protocol.PcepLabelUpdate;
import org.onosproject.pcepio.protocol.PcepLabelUpdateMsg;
import org.onosproject.pcepio.protocol.PcepMessageReader;
import org.onosproject.pcepio.protocol.PcepMessageWriter;
import org.onosproject.pcepio.protocol.PcepType;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP lable update message.
 */
class PcepLabelUpdateMsgVer1 implements PcepLabelUpdateMsg {

    // Pcep version: 1

    /*
      The format of the PCLabelUpd message:

                  <PCLabelUpd Message>         ::=     <Common Header>
                                                       <pce-label-update-list>
                Where:

                <pce-label-update-list>        ::=     <pce-label-update>
                                                       [<pce-label-update-list>]
                <pce-label-update>             ::=     (<pce-label-download>|<pce-label-map>)

                Where:
                <pce-label-download>           ::=      <SRP>
                                                        <LSP>
                                                        <label-list>

                <pce-label-map>                 ::=     <SRP>
                                                        <LABEL>
                                                        <FEC>

                <label-list >                   ::=     <LABEL>
                                                        [<label-list>]

     */
    protected static final Logger log = LoggerFactory.getLogger(PcepLabelUpdateMsgVer1.class);

    public static final byte PACKET_VERSION = 1;

    //LabelUpdateMsgMinLength = COMMON-HEADER(4)+SrpObjMinLentgh(12)+LabelObjectMinLength(12)+FECType1Object(8)
    public static final int PACKET_MINIMUM_LENGTH = 36;
    public static final PcepType MSG_TYPE = PcepType.LABEL_UPDATE;
    //pce-label-update-list
    private LinkedList<PcepLabelUpdate> llPcLabelUpdateList;

    static final PcepLabelUpdateMsgVer1.Reader READER = new Reader();

    /**
     * Reader reads LabelUpdate Message from the channel.
     */
    static class Reader implements PcepMessageReader<PcepLabelUpdateMsg> {

        @Override
        public PcepLabelUpdateMsg readFrom(ChannelBuffer cb) throws PcepParseException {

            if (cb.readableBytes() < PACKET_MINIMUM_LENGTH) {
                throw new PcepParseException("Readable bytes are less than Packet minimum length.");
            }

            // fixed value property version == 1
            byte version = cb.readByte();
            version = (byte) (version >> PcepMessageVer1.SHIFT_FLAG);
            if (version != PACKET_VERSION) {
                throw new PcepParseException("Wrong version.Expected=PcepVersion.PCEP_1(1), got=" + version);
            }
            // fixed value property type == 13
            byte type = cb.readByte();
            if (type != MSG_TYPE.getType()) {
                throw new PcepParseException("Wrong type. Expected=PcepType.LABEL_UPDATE(13), got=" + type);
            }
            short length = cb.readShort();
            if (length < PACKET_MINIMUM_LENGTH) {
                throw new PcepParseException("Wrong length. Expected to be >= " + PACKET_MINIMUM_LENGTH + ", is: "
                        + length);
            }
            // parse <pce-label-download> / <pce-label-map>
            LinkedList<PcepLabelUpdate> llPcLabelUpdateList = parsePcLabelUpdateList(cb);
            return new PcepLabelUpdateMsgVer1(llPcLabelUpdateList);
        }

        /**
         * Returns list of PCEP Label Update object.
         *
         * @param cb of type channel buffer
         * @return llPcLabelUpdateList list of PCEP label update object
         * @throws PcepParseException when fails to parse list of PCEP label update object
         */
        public LinkedList<PcepLabelUpdate> parsePcLabelUpdateList(ChannelBuffer cb) throws PcepParseException {

            LinkedList<PcepLabelUpdate> llPcLabelUpdateList;
            llPcLabelUpdateList = new LinkedList<>();

            while (0 < cb.readableBytes()) {
                llPcLabelUpdateList.add(PcepLabelUpdateVer1.read(cb));
            }
            return llPcLabelUpdateList;
        }
    }

    /**
     * Constructor to initialize PCEP Label Update List.
     *
     * @param llPcLabelUpdateList list of PCEP Label Update object
     */
    PcepLabelUpdateMsgVer1(LinkedList<PcepLabelUpdate> llPcLabelUpdateList) {
        this.llPcLabelUpdateList = llPcLabelUpdateList;
    }

    /**
     * Builder class for PCEP label update message.
     */
    static class Builder implements PcepLabelUpdateMsg.Builder {

        LinkedList<PcepLabelUpdate> llPcLabelUpdateList;

        @Override
        public PcepVersion getVersion() {
            return PcepVersion.PCEP_1;
        }

        @Override
        public PcepType getType() {
            return PcepType.LABEL_UPDATE;
        }

        @Override
        public PcepLabelUpdateMsg build() {
            return new PcepLabelUpdateMsgVer1(this.llPcLabelUpdateList);
        }

        @Override
        public LinkedList<PcepLabelUpdate> getPcLabelUpdateList() {
            return this.llPcLabelUpdateList;
        }

        @Override
        public Builder setPcLabelUpdateList(LinkedList<PcepLabelUpdate> ll) {
            this.llPcLabelUpdateList = ll;
            return this;
        }
    }

    @Override
    public void writeTo(ChannelBuffer cb) throws PcepParseException {
        WRITER.write(cb, this);
    }

    static final Writer WRITER = new Writer();

    /**
     * Writer writes LabelUpdate Message to the channel.
     */
    static class Writer implements PcepMessageWriter<PcepLabelUpdateMsgVer1> {

        @Override
        public void write(ChannelBuffer cb, PcepLabelUpdateMsgVer1 message) throws PcepParseException {

            int startIndex = cb.writerIndex();

            // first 3 bits set to version
            cb.writeByte((byte) (PACKET_VERSION << PcepMessageVer1.SHIFT_FLAG));

            // message type
            cb.writeByte(MSG_TYPE.getType());

            // Length will be set after calculating length, but currently set it as 0.
            int msgLenIndex = cb.writerIndex();

            cb.writeShort((short) 0);
            ListIterator<PcepLabelUpdate> listIterator = message.llPcLabelUpdateList.listIterator();

            while (listIterator.hasNext()) {
                PcepLabelUpdate labelUpdate = listIterator.next();
                labelUpdate.write(cb);
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
    public LinkedList<PcepLabelUpdate> getPcLabelUpdateList() {
        return this.llPcLabelUpdateList;
    }

    @Override
    public void setPcLabelUpdateList(LinkedList<PcepLabelUpdate> ll) {
        this.llPcLabelUpdateList = ll;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("PcLabelUpdateList", llPcLabelUpdateList)
                .toString();
    }
}
