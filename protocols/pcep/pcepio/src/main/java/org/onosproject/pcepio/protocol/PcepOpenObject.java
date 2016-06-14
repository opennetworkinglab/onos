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
 * Abstraction of an entity providing PCEP Open Object.
 */
public interface PcepOpenObject {

    /**
     * Returns Open object header.
     *
     * @return Open object header
     */
    PcepObjectHeader getOpenObjHeader();

    /**
     * Sets Open object header in Open Object.
     *
     * @param obj Open object header
     */
    void setOpenObjHeader(PcepObjectHeader obj);

    /**
     * Returns version of Open Object.
     *
     * @return Version of Open Object
     */
    PcepVersion getVersion();

    /**
     * Returns KeepAlive Time in Open Object.
     *
     * @return KeepAlive Time in Open Object
     */
    byte getKeepAliveTime();

    /**
     * Sets KeepAlive Time in Open Object with specified value.
     *
     * @param value KeepAlive Time
     */
    void setKeepAliveTime(byte value);

    /**
     * Returns Dead Time in Open Object.
     *
     * @return Dead Time in Open Object
     */
    byte getDeadTime();

    /**
     * Sets Dead Time in Open Object with specified value.
     *
     * @param value Dead Time
     */
    void setDeadTime(byte value);

    /**
     * Returns SessionId in Open Object.
     *
     * @return SessionId in Open Object
     */
    byte getSessionId();

    /**
     * Sets SessionId in Open Object with specified value.
     *
     * @param value SessionId
     */
    void setSessionId(byte value);

    /**
     * Returns list of Optional Tlvs in Open Object.
     *
     * @return list of Optional Tlvs
     */
    LinkedList<PcepValueType> getOptionalTlv();

    /**
     * Sets list of Optional Tlvs in Open Object.
     *
     * @param llOptionalTlv list of Optional Tlvs
     */
    void setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv);

    /**
     * Writes the Open into channel buffer.
     *
     * @param bb channel buffer
     * @return Returns the writerIndex of this buffer
     * @throws PcepParseException while writing Open Object into Channel Buffer.
     */
    int write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build Open object.
     */
    interface Builder {

        /**
         * Builds Open Object.
         *
         * @return Open Object
         * @throws PcepParseException while building PCEP-Open object
         */
        PcepOpenObject build() throws PcepParseException;

        /**
         * Returns Open object header.
         *
         * @return Open object header
         */
        PcepObjectHeader getOpenObjHeader();

        /**
         * Sets Open object header and returns its builder.
         *
         * @param obj Open object header
         * @return Builder by setting Open object header
         */
        Builder setOpenObjHeader(PcepObjectHeader obj);

        /**
         * Returns KeepAlive Time in Open Object.
         *
         * @return KeepAlive Time in Open Object
         */
        byte getKeepAliveTime();

        /**
         * Sets KeepAlive Time and returns its builder.
         *
         * @param value KeepAlive Time
         * @return Builder by setting KeepAlive Time
         */
        Builder setKeepAliveTime(byte value);

        /**
         * Returns Dead Time in Open Object.
         *
         * @return Dead Time in Open Object
         */
        byte getDeadTime();

        /**
         * Sets Dead Time and returns its builder.
         *
         * @param value Dead Time
         * @return Builder by setting Dead Time
         */
        Builder setDeadTime(byte value);

        /**
         * Returns SessionId in Open Object.
         *
         * @return SessionId in Open Object
         */
        byte getSessionId();

        /**
         * Sets SessionId and returns its builder.
         *
         * @param value SessionId
         * @return Builder by setting SessionId
         */
        Builder setSessionId(byte value);

        /**
         * Returns list of Optional Tlvs in Open Object.
         *
         * @return list of Optional Tlvs in Open Object
         */
        LinkedList<PcepValueType> getOptionalTlv();

        /**
         * Sets list of Optional Tlvs and return its Builder.
         *
         * @param llOptionalTlv list of Optional Tlvs
         * @return builder by setting list of Optional Tlvs
         */
        Builder setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv);

        /**
         * Sets P flag in Open object header and returns its builder.
         *
         * @param value boolean value to set P flag
         * @return Builder by setting P flag
         */
        Builder setPFlag(boolean value);

        /**
         * Sets I flag in Open object header and returns its builder.
         *
         * @param value boolean value to set I flag
         * @return Builder by setting I flag
         */
        Builder setIFlag(boolean value);
    }
}
