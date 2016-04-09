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
 * Abstraction of an entity providing PCEP Update Message.
 */
public interface PcepUpdateMsg extends PcepObject, PcepMessage {

    @Override
    PcepVersion getVersion();

    @Override
    PcepType getType();

    /**
     * Returns the update request list for PCEP Update Message.
     *
     * @return list of Update Requests
     */
    LinkedList<PcepUpdateRequest> getUpdateRequestList();

    /**
     * Sets the update request list for PCEP update message.
     *
     * @param llUpdateRequestList is a list of PCEP Update Requests
     */
    void setUpdateRequestList(LinkedList<PcepUpdateRequest> llUpdateRequestList);

    @Override
    void writeTo(ChannelBuffer channelBuffer) throws PcepParseException;

    /**
     * Builder interface with Get and Set Functions to build the PCEP update Message.
     */
    interface Builder extends PcepMessage.Builder {

        @Override
        PcepUpdateMsg build();

        @Override
        PcepVersion getVersion();

        @Override
        PcepType getType();

        /**
         * Returns the update request list for the PCEP update message.
         *
         * @return list of Update Requests
         */
        LinkedList<PcepUpdateRequest> getUpdateRequestList();

        /**
         * Sets the  update request list for the PCEP update message.
         *
         * @param llUpdateRequestList list of Update requests
         * @return builder by setting list llUpdateRequestList of PcepUpdateRequest.
         */
        Builder setUpdateRequestList(LinkedList<PcepUpdateRequest> llUpdateRequestList);
    }
}
