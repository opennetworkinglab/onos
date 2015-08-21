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
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.onosproject.pcepio.types.PcepValueType;

/**
 * Abstraction of an entity providing PCEP TE Object.
 */
public interface PcepTEObject {

    /**
     * Returns TE object header.
     *
     * @return TE object header
     */
    PcepObjectHeader getTEObjHeader();

    /**
     * Sets TE Object header.
     *
     * @param obj TE Object header
     */
    void setTEObjHeader(PcepObjectHeader obj);

    /**
     * Returns ProtocolId in TE Object.
     *
     * @return ProtocolId in TE Object
     */
    byte getProtocolId();

    /**
     * Sets ProtocolId in TE Object.
     *
     * @param yProtId ProtocolId in TE Object
     */
    void setProtocolId(byte yProtId);

    /**
     * Returns R flag in TE Object.
     *
     * @return R flag in TE Object
     */
    boolean getRFlag();

    /**
     * Sets R flag in TE Object.
     *
     * @param bRFlag R flag in TE Object
     */
    void setRFlag(boolean bRFlag);

    /**
     * Returns S flag in TE Object.
     *
     * @return S flag in TE Object
     */
    boolean getSFlag();

    /**
     * Sets S flag in TE Object.
     *
     * @param bSFlag S flag in TE Object
     */
    void setSFlag(boolean bSFlag);

    /**
     * Returns TE ID in TE Object.
     *
     * @return TE ID in TE Object
     */
    int getTEId();

    /**
     * Sets TE ID in TE Object.
     *
     * @param iTEId TE ID in TE Object
     */
    void setTEId(int iTEId);

    /**
     * Returns list of Optional Tlvs in TE Object.
     *
     * @return list of Optional Tlvs
     */
    LinkedList<PcepValueType> getOptionalTlv();

    /**
     * Sets list of Optional Tlvs in TE Object.
     *
     * @param llOptionalTlv list of Optional Tlvs
     */
    void setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv);

    /**
     * Writes the TE Object into channel buffer.
     *
     * @param bb channel buffer
     * @return Returns the writerIndex of this buffer
     * @throws PcepParseException when obj header is not written to channel buffer
     */
    int write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build TE object.
     */
    interface Builder {

        /**
         * Builds TE Object.
         *
         * @return TE Object
         */
        PcepTEObject build();

        /**
         * Returns TE object header.
         *
         * @return TE object header
         */
        PcepObjectHeader getTEObjHeader();

        /**
         * Sets TE object header and returns its builder.
         *
         * @param obj TE object header
         * @return Builder by setting TE object header
         */
        Builder setTEObjHeader(PcepObjectHeader obj);

        /**
         * Returns ProtocolId in TE Object.
         *
         * @return ProtocolId in TE Object
         */
        byte getProtocolId();

        /**
         * Sets ProtocolId in TE Object and returns its builder.
         *
         * @param yProtId ProtocolId in TE Object
         * @return Builder by setting ProtocolId
         */
        Builder setProtocolId(byte yProtId);

        /**
         * Returns R flag in TE Object.
         *
         * @return R flag in TE Object
         */
        boolean getRFlag();

        /**
         * Sets R flag in TE Object and returns its builder.
         *
         * @param bRFlag R flag in TE Object
         * @return Builder by setting R flag
         */
        Builder setRFlag(boolean bRFlag);

        /**
         * Returns S flag in TE Object.
         *
         * @return S flag in TE Object
         */
        boolean getSFlag();

        /**
         * Sets S flag in TE Object and returns its builder.
         *
         * @param bSFlag S flag in TE Object
         * @return Builder by setting S flag
         */
        Builder setSFlag(boolean bSFlag);

        /**
         * Returns TE ID in TE Object.
         *
         * @return TE ID in TE Object
         */
        int getTEId();

        /**
         * Sets TE ID in TE Object and returns its builder.
         *
         * @param iTEId TE ID in TE Object
         * @return Builder by setting TE ID
         */
        Builder setTEId(int iTEId);

        /**
         * Returns list of Optional Tlvs in TE Object.
         *
         * @return list of Optional Tlvs
         */
        LinkedList<PcepValueType> getOptionalTlv();

        /**
         * Sets list of Optional Tlvs in TE Object and returns its builder.
         *
         * @param llOptionalTlv list of Optional Tlvs
         * @return Builder by setting list of Optional Tlvs
         */
        Builder setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv);

        /**
         * Sets P flag in TE object header and returns its builder.
         *
         * @param value boolean value to set P flag
         * @return Builder by setting P flag
         */
        Builder setPFlag(boolean value);

        /**
         * Sets I flag in TE object header and returns its builder.
         *
         * @param value boolean value to set I flag
         * @return Builder by setting I flag
         */
        Builder setIFlag(boolean value);
    }
}
