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
 * Abstraction of an entity providing PCEP SRP Object.
 */
public interface PcepSrpObject {

    /**
     * Returns SRP ID of SRP Object.
     *
     * @return SRP ID of SRP Object
     */
    int getSrpID();

    /**
     * Sets SRP ID with specified value.
     *
     * @param srpID SRP ID of SRP Object
     */
    void setSrpID(int srpID);

    /**
     * Returns R flag of SRP Object.
     *
     * @return R flag of SRP Object
     */
    boolean getRFlag();

    /**
     * Sets R flag with specified value.
     *
     * @param bRFlag R Flag of SRP Object
     */
    void setRFlag(boolean bRFlag);

    /**
     * Returns S flag of SRP Object.
     *
     * @return S flag of SRP Object
     */
    boolean getSFlag();

    /**
     * Sets S(sync) flag with specified value.
     *
     * @param bSFlag S Flag of SRP Object
     */
    void setSFlag(boolean bSFlag);

    /**
     * sets the optional TLvs.
     *
     * @param llOptionalTlv list of optional tlvs
     */
    void setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv);

    /**
     * Returns list of optional tlvs.
     *
     * @return llOptionalTlv list of optional tlvs
     */
    LinkedList<PcepValueType> getOptionalTlv();

    /**
     * Writes the SRP Object into channel buffer.
     *
     * @param bb channel buffer
     * @return Returns the writerIndex of this buffer
     * @throws PcepParseException when tlv is null
     */
    int write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build SRP object.
     */
    interface Builder {

        /**
         * Builds SRP Object.
         *
         * @return SRP Object
         * @throws PcepParseException when mandatory object is not set
         */
        PcepSrpObject build() throws PcepParseException;

        /**
         * Returns SRP object header.
         *
         * @return SRP object header
         */
        PcepObjectHeader getSrpObjHeader();

        /**
         * Sets SRP object header and returns its builder.
         *
         * @param obj SRP object header
         * @return Builder by setting SRP object header
         */
        Builder setSrpObjHeader(PcepObjectHeader obj);

        /**
         * Returns SRP ID of SRP Object.
         *
         * @return SRP ID of SRP Object
         */
        int getSrpID();

        /**
         * Sets SRP ID and returns its builder.
         *
         * @param srpID SRP ID
         * @return Builder by setting SRP ID
         */
        Builder setSrpID(int srpID);

        /**
         * Returns R flag of SRP Object.
         *
         * @return R flag of SRP Object
         */
        boolean getRFlag();

        /**
         * Returns S(sync) flag of SRP Object.
         *
         * @return S flag of SRP Object
         */
        boolean getSFlag();

        /**
         * Sets R flag and returns its builder.
         *
         * @param bRFlag R flag
         * @return Builder by setting R flag
         */
        Builder setRFlag(boolean bRFlag);

        /**
         * Sets S flag and returns its builder.
         *
         * @param bSFlag S flag
         * @return Builder by setting S flag
         */
        Builder setSFlag(boolean bSFlag);

        /**
         * Returns list of optional tlvs.
         *
         * @return llOptionalTlv list of optional tlvs
         */
        LinkedList<PcepValueType> getOptionalTlv();

        /**
         * sets the optional TLvs.
         *
         * @param llOptionalTlv List of optional tlv
         * @return builder by setting list of optional tlv.
         */
        Builder setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv);

        /**
         * Sets P flag in SRP object header and returns its builder.
         *
         * @param value boolean value to set P flag
         * @return Builder by setting P flag
         */
        Builder setPFlag(boolean value);

        /**
         * Sets I flag in SRP object header and returns its builder.
         *
         * @param value boolean value to set I flag
         * @return Builder by setting I flag
         */
        Builder setIFlag(boolean value);
    }
}
