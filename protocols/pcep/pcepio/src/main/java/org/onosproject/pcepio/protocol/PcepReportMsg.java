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

/**
 * Abstraction of an entity providing PCEP Report Message.
 */
public interface PcepReportMsg extends PcepObject, PcepMessage {

    @Override
    PcepVersion getVersion();

    @Override
    PcepType getType();

    /**
     * Returns PcepStateReport list.
     *
     * @return list of PcepStateReport
     */
    LinkedList<PcepStateReport> getStateReportList();

    /**
     * Sets StateReportList.
     *
     * @param llStateReportList list of PcepStateReport.
     */
    void setStateReportList(LinkedList<PcepStateReport> llStateReportList);

    @Override
    void writeTo(ChannelBuffer channelBuffer) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build Report message.
     */
    interface Builder extends PcepMessage.Builder {

        @Override
        PcepReportMsg build();

        @Override
        PcepVersion getVersion();

        @Override
        PcepType getType();

        /**
         * Returns StateReportList.
         *
         * @return StateReportList.
         */
        LinkedList<PcepStateReport> getStateReportList();

        /**
         * Sets list of PcepStateReport and returns builder.
         *
         * @param llStateReportList list of PcepStateReport.
         * @return Builder by setting list of PcepStateReport.
         */
        Builder setStateReportList(LinkedList<PcepStateReport> llStateReportList);
    }
}
