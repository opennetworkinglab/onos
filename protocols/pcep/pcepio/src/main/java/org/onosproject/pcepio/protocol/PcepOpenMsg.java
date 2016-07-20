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

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;

/**
 * Abstraction of an entity providing PCEP Open Message.
 */
public interface PcepOpenMsg extends PcepObject, PcepMessage {

    @Override
    PcepVersion getVersion();

    @Override
    PcepType getType();

    /**
     * Sets OpenObject in Open Message with Specified Obj.
     *
     * @param obj OpenObject
     */
    void setPcepOpenObject(PcepOpenObject obj);

    /**
     * Returns OpenObject in Open Message.
     *
     * @return OpenObject in Open Message
     */
    PcepOpenObject getPcepOpenObject();

    @Override
    void writeTo(ChannelBuffer channelBuffer) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build Open message.
     */
    interface Builder extends PcepMessage.Builder {

        @Override
        PcepOpenMsg build() throws PcepParseException;

        /**
         * Sets Open Object in Open Message and return its builder.
         *
         * @param obj Open Object
         * @return builder by setting Open Object
         */
        Builder setPcepOpenObj(PcepOpenObject obj);

        /**
         * Returns OpenObject in Open Message.
         *
         * @return OpenObject in Open Message
         */
        PcepOpenObject getPcepOpenObj();
    }
}
