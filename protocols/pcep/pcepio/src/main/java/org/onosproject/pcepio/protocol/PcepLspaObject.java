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
 * Abstraction of an entity providing PCEP LSPA Object.
 */
public interface PcepLspaObject {

    /**
     * Returns L flag in LSPA Object.
     *
     * @return L flag in LSPA Object
     */
    boolean getLFlag();

    /**
     * Sets L flag in LSPA Object.
     *
     * @param value L flag
     */
    void setLFlag(boolean value);

    /**
     * Returns Exclude Any field in LSPA Object.
     *
     * @return Exclude Any field in LSPA Object
     */
    int getExcludeAny();

    /**
     * Sets Exclude Any field in LSPA Object.
     *
     * @param value Exclude Any field
     */
    void setExcludeAny(int value);

    /**
     * Returns Include Any field in LSPA Object.
     *
     * @return Include Any field in LSPA Object
     */
    int getIncludeAny();

    /**
     * Sets Include Any field in LSPA Object.
     *
     * @param value Include Any field
     */
    void setIncludeAny(int value);

    /**
     * Returns Include All field in LSPA Object.
     *
     * @return Include All field in LSPA Object
     */
    int getIncludeAll();

    /**
     * Sets Include All field in LSPA Object.
     *
     * @param value Include All field
     */
    void setIncludeAll(int value);

    /**
     * Returns Setup Priority field in LSPA Object.
     *
     * @return Setup Priority field in LSPA Object
     */
    byte getSetupPriority();

    /**
     * Sets Setup Priority field in LSPA Object.
     *
     * @param value Setup Priority field
     */
    void setSetupPriority(byte value);

    /**
     * Returns Hold Priority field in LSPA Object.
     *
     * @return Hold Priority field in LSPA Object
     */
    byte getHoldPriority();

    /**
     * Sets Hold Priority field in LSPA Object.
     *
     * @param value Hold Priority field
     */
    void setHoldPriority(byte value);

    /**
     * Returns list of Optional Tlvs in LSPA Object.
     *
     * @return list of Optional Tlvs in LSPA Object
     */
    LinkedList<PcepValueType> getOptionalTlv();

    /**
     * Sets Optional Tlvs in LSPA Object.
     *
     * @param llOptionalTlv Optional Tlvs in LSPA Object
     */
    void setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv);

    /**
     * Writes the LSPA Object into channel buffer.
     *
     * @param bb channel buffer
     * @return Returns the writerIndex of this buffer
     * @throws PcepParseException while writing LSPA object into Channel Buffer.
     */
    int write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build bandwidth object.
     */
    interface Builder {

        /**
         * Builds LSPA Object.
         *
         * @return LSPA Object
         * @throws PcepParseException while building LSPA object.
         */
        PcepLspaObject build() throws PcepParseException;

        /**
         * Returns LSPA object header.
         *
         * @return LSPA object header
         */
        PcepObjectHeader getLspaObjHeader();

        /**
         * Sets LSPA object header and returns its builder.
         *
         * @param obj LSPA object header
         * @return Builder by setting LSPA object header
         */
        Builder setLspaObjHeader(PcepObjectHeader obj);

        /**
         * Returns L flag in LSPA Object.
         *
         * @return L flag in LSPA Object
         */
        boolean getLFlag();

        /**
         * Sets L flag in LSPA Object and return its builder.
         *
         * @param value L flag in LSPA Object
         * @return Builder by setting L flag
         */
        Builder setLFlag(boolean value);

        /**
         * Returns Exclude Any field in LSPA Object.
         *
         * @return Exclude Any field in LSPA Object
         */
        int getExcludeAny();

        /**
         * Sets Exclude Any field in LSPA Object and return its builder.
         *
         * @param value Exclude Any field in LSPA Object
         * @return Builder by setting Exclude Any field
         */
        Builder setExcludeAny(int value);

        /**
         * Returns Include Any field in LSPA Object.
         *
         * @return Include Any field in LSPA Object
         */
        int getIncludeAny();

        /**
         * Sets Include Any field in LSPA Object and return its builder.
         *
         * @param value Include Any field in LSPA Object
         * @return Builder by setting Include Any field
         */
        Builder setIncludeAny(int value);

        /**
         * Returns Include All field in LSPA Object.
         *
         * @return Include All field in LSPA Object
         */
        int getIncludeAll();

        /**
         * Sets Include All field in LSPA Object and return its builder.
         *
         * @param value Include All field in LSPA Object
         * @return Builder by setting Include All field
         */
        Builder setIncludeAll(int value);

        /**
         * Returns Setup Priority field in LSPA Object.
         *
         * @return Setup Priority field in LSPA Object
         */
        byte getSetupPriority();

        /**
         * Sets Setup Priority field in LSPA Object and return its builder.
         *
         * @param value Setup Priority field in LSPA Object
         * @return Builder by setting Setup Priority field
         */
        Builder setSetupPriority(byte value);

        /**
         * Returns Hold Priority field in LSPA Object.
         *
         * @return Hold Priority field in LSPA Object
         */
        byte getHoldPriority();

        /**
         * Sets Hold Priority field in LSPA Object and return its builder.
         *
         * @param value Hold Priority field in LSPA Object
         * @return Builder by setting Hold Priority field
         */
        Builder setHoldPriority(byte value);

        /**
         * Returns list of Optional Tlvs in LSPA Object.
         *
         * @return list of Optional Tlvs in LSPA Object
         */
        LinkedList<PcepValueType> getOptionalTlv();

        /**
         * Sets list of Optional Tlvs in LSPA Object.
         *
         * @param llOptionalTlv list of Optional Tlvs
         * @return builder by setting list of Optional Tlvs
         */
        Builder setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv);

        /**
         * Sets P flag in LSPA object header and returns its builder.
         *
         * @param value boolean value to set P flag
         * @return Builder by setting P flag
         */
        Builder setPFlag(boolean value);

        /**
         * Sets I flag in LSPA object header and returns its builder.
         *
         * @param value boolean value to set I flag
         * @return Builder by setting I flag
         */
        Builder setIFlag(boolean value);
    }
}
