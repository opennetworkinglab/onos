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
 * Abstraction of an entity providing PCEP RP Object.
 */
public interface PcepRPObject {

    /**
     * Returns RequestId Number in RP Object.
     *
     * @return RequestId Number in RP Object
     */
    int getRequestIdNum();

    /**
     * Sets RequestId Number with specified value.
     *
     * @param value RequestId Number
     */
    void setRequestIdNum(int value);

    /**
     * Returns O flag in RP Object.
     *
     * @return O flag in RP Object
     */
    boolean getOFlag();

    /**
     * Sets O flag with specified value.
     *
     * @param value O flag
     */
    void setOFlag(boolean value);

    /**
     * Returns B flag in RP Object.
     *
     * @return B flag in RP Object
     */
    boolean getBFlag();

    /**
     * Sets B flag with specified value.
     *
     * @param value B flag
     */
    void setBFlag(boolean value);

    /**
     * Returns R flag in RP Object.
     *
     * @return R flag in RP Object
     */
    boolean getRFlag();

    /**
     * Sets R flag with specified value.
     *
     * @param value R flag
     */
    void setRFlag(boolean value);

    /**
     * Returns Priority Flag in RP Object.
     *
     * @return Priority Flag in RP Object
     */
    byte getPriFlag();

    /**
     * Sets Priority Flag with specified value.
     *
     * @param value Priority Flag
     */
    void setPriFlag(byte value);

    /**
     * Returns list of Optional Tlvs in RP Object.
     *
     * @return list of Optional Tlvs in RP Object
     */
    LinkedList<PcepValueType> getOptionalTlv();

    /**
     * Sets list of Optional Tlvs in RP Object and returns its builder.
     *
     * @param llOptionalTlv list of Optional Tlvs
     */
    void setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv);

    /**
     * Writes the RP Object into channel buffer.
     *
     * @param bb channel buffer
     * @return Returns the writerIndex of this buffer
     * @throws PcepParseException while writing RP object into Channel Buffer.
     */
    int write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build bandwidth object.
     */
    interface Builder {

        /**
         * Builds RP Object.
         *
         * @return RP Object
         */
        PcepRPObject build();

        /**
         * Returns RP object header.
         *
         * @return RP object header
         */
        PcepObjectHeader getRPObjHeader();

        /**
         * Sets RP object header and returns its builder.
         *
         * @param obj RP object header
         * @return Builder by setting RP object header
         */
        Builder setRPObjHeader(PcepObjectHeader obj);

        /**
         * Returns Request Id Number in RP Object.
         *
         * @return Request Id Number in RP Object
         */
        int getRequestIdNum();

        /**
         * Sets Request Id Number and returns its builder.
         *
         * @param value Request Id Number
         * @return Builder by setting Request Id Number
         */
        Builder setRequestIdNum(int value);

        /**
         * Returns O flag in RP Object.
         *
         * @return O flag in RP Object
         */
        boolean getOFlag();

        /**
         * Sets O flag and returns its builder.
         *
         * @param value O flag
         * @return Builder by setting O flag
         */
        Builder setOFlag(boolean value);

        /**
         * Returns B flag in RP Object.
         *
         * @return B flag in RP Object
         */
        boolean getBFlag();

        /**
         * Sets B flag and returns its builder.
         *
         * @param value B flag
         * @return Builder by setting B flag
         */
        Builder setBFlag(boolean value);

        /**
         * Returns R flag in RP Object.
         *
         * @return R flag in RP Object
         */
        boolean getRFlag();

        /**
         * Sets R flag and returns its builder.
         *
         * @param value R flag
         * @return Builder by setting R flag
         */
        Builder setRFlag(boolean value);

        /**
         * Returns Priority Flag in RP Object.
         *
         * @return Priority Flag in RP Object
         */
        byte getPriFlag();

        /**
         * Sets Priority Flag and returns its builder.
         *
         * @param value Priority Flag
         * @return Builder by setting Priority Flag
         */
        Builder setPriFlag(byte value);

        /**
         * Returns list of Optional Tlvs in RP Object.
         *
         * @return list of Optional Tlvs
         */
        LinkedList<PcepValueType> getOptionalTlv();

        /**
         * Sets list of Optional Tlvs and returns its builder.
         *
         * @param llOptionalTlv list of Optional Tlvs
         * @return Builder by setting list of Optional Tlvs
         */
        Builder setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv);

        /**
         * Sets P flag in RP object header and returns its builder.
         *
         * @param value boolean value to set P flag
         * @return Builder by setting P flag
         */
        Builder setPFlag(boolean value);

        /**
         * Sets I flag in RP object header and returns its builder.
         *
         * @param value boolean value to set I flag
         * @return Builder by setting I flag
         */
        Builder setIFlag(boolean value);
    }
}
