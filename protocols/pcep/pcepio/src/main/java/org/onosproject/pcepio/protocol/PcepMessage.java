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
 * Abstraction of an entity providing PCEP Messages.
 */
public interface PcepMessage extends PcepObject {

    @Override
    PcepVersion getVersion();

    /**
     * Returns Type of PCEP Message.
     *
     * @return Type of PCEP Message
     */
    PcepType getType();

    @Override
    void writeTo(ChannelBuffer channelBuffer) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build PCEP Message.
     */
    interface Builder {

        /**
         * Builds PCEP Message.
         *
         * @return PCEP Message
         * @throws PcepParseException when build fails to create PCEP message
         */
        PcepMessage build() throws PcepParseException;

        /**
         * Returns Version of PCEP Message.
         *
         * @return Version of PCEP Message
         */
        PcepVersion getVersion();

        /**
         * Returns Type of PCEP Message.
         *
         * @return Type of PCEP Message
         */
        PcepType getType();
    }
}
