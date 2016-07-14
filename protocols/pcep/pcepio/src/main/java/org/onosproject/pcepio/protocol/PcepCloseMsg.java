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

package org.onosproject.pcepio.protocol;

import java.util.LinkedList;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.onosproject.pcepio.types.PcepValueType;

/**
 * Abstraction of an entity providing PCEP Close Message.
 */
public interface PcepCloseMsg extends PcepObject, PcepMessage {

    @Override
    PcepVersion getVersion();

    @Override
    PcepType getType();

    /**
     * Returns reason field in Close message.
     *
     * @return reason field
     */
    byte getReason();

    /**
     * Sets reason field in Close message with specified value.
     *
     * @param value of Reason field
     */
    void setReason(byte value);

    /**
     * Returns LinkedList of Optional Tlv in Close Message.
     *
     * @return list of optional tlv
     */
    LinkedList<PcepValueType> getOptionalTlv();

    /**
     * Sets LinkedList of Optional Tlvs in Close Message.
     *
     * @param llOptionalTlv LinkedList of type PcepValueType
     */
    void setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv);

    @Override
    void writeTo(ChannelBuffer channelBuffer) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build Close message.
     */
    interface Builder extends PcepMessage.Builder {

        @Override
        PcepCloseMsg build();

        @Override
        PcepVersion getVersion();

        @Override
        PcepType getType();

        /**
         * Returns Close Object header.
         *
         * @return Close Object header
         */
        PcepObjectHeader getCloseObjHeader();

        /**
         * Sets close object header and returns its builder.
         *
         * @param obj close object header
         * @return Builder by setting Close object header
         */
        Builder setCloseObjHeader(PcepObjectHeader obj);

        /**
         * Returns reason field in Close message.
         *
         * @return reason field in Close message
         */
        byte getReason();

        /**
         * Sets reason field and return its builder.
         *
         * @param value of Reason field
         * @return builder by setting reason field
         */
        Builder setReason(byte value);

        /**
         * Returns LinkedList of Optional Tlvs.
         *
         * @return list of optional tlv
         */
        LinkedList<PcepValueType> getOptionalTlv();

        /**
         * Sets LinkedList of Optional Tlvs in Close Message.
         *
         * @param llOptionalTlv list of optional tlv
         * @return Builder by setting Optional Tlvs
         */
        Builder setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv);

        /**
         * Sets P flag in Close object header and returns its builder.
         *
         * @param value boolean value to set P flag
         * @return Builder by setting P flag
         */
        Builder setPFlag(boolean value);

        /**
         * Sets I flag in Close object header and returns its builder.
         *
         * @param value boolean value to set I flag
         * @return Builder by setting I flag
         */
        Builder setIFlag(boolean value);
    }
}
