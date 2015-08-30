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

package org.onosproject.pcepio.protocol;

import java.util.LinkedList;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;

/**
 * Abstraction of an entity providing PCEP TE Report Message.
 */
public interface PcepTEReportMsg extends PcepObject, PcepMessage {

    @Override
    PcepVersion getVersion();

    @Override
    PcepType getType();

    /**
     * Returns list of PCEP TE Objects.
     *
     * @return list of PCEP TE Objects
     */
    LinkedList<PcepTEObject> getTEReportList();

    /**
     * Sets list of Optional Tlvs in TE Report Message.
     *
     * @param llTEReportList list of optional Tlvs
     */
    void setTEReportList(LinkedList<PcepTEObject> llTEReportList);

    @Override
    void writeTo(ChannelBuffer channelBuffer) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build TE Report message.
     */
    interface Builder extends PcepMessage.Builder {

        @Override
        PcepTEReportMsg build();

        @Override
        PcepVersion getVersion();

        @Override
        PcepType getType();

        /**
         * Returns list of Optional Tlv in TE Report Message.
         *
         * @return list of Optional Tlv
         */
        LinkedList<PcepTEObject> getTEReportList();

        /**
         * Sets list of Optional Tlvs and returns its builder.
         *
         * @param llTEReportList list of Optional Tlvs
         * @return Builder object for TE report message
         */
        Builder setTEReportList(LinkedList<PcepTEObject> llTEReportList);
    }
}
