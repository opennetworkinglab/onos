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
 * Abstraction of an entity providing PCEP Label Update Message.
 */
public interface PcepLabelUpdateMsg extends PcepObject, PcepMessage {

    @Override
    PcepVersion getVersion();

    @Override
    PcepType getType();

    /**
     * Returns list of PcLabelUpdateList.
     *
     * @return list of PcLabelUpdateList.
     */
    LinkedList<PcepLabelUpdate> getPcLabelUpdateList();

    /**
     * Sets list of PcLabelUpdateList.
     *
     * @param llPcLabelUpdateList list of PcLabelUpdateList
     */
    void setPcLabelUpdateList(LinkedList<PcepLabelUpdate> llPcLabelUpdateList);

    @Override
    void writeTo(ChannelBuffer channelBuffer) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build Label Update message.
     */
    interface Builder extends PcepMessage.Builder {

        @Override
        PcepLabelUpdateMsg build();

        @Override
        PcepVersion getVersion();

        @Override
        PcepType getType();

        /**
         * Returns list of PcLabelUpdateList.
         *
         * @return list of PcLabelUpdateList.
         */
        LinkedList<PcepLabelUpdate> getPcLabelUpdateList();

        /**
         * Sets list of PcLabelUpdateList.
         *
         * @param llPcLabelUpdateList list of PcLabelUpdateList.
         * @return Builder by setting list of PcLabelUpdateList.
         */
        Builder setPcLabelUpdateList(LinkedList<PcepLabelUpdate> llPcLabelUpdateList);
    }
}
