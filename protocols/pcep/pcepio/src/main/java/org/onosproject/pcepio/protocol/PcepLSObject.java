/*
 * Copyright 2016-present Open Networking Laboratory
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

import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.types.PcepObjectHeader;
import org.onosproject.pcepio.types.PcepValueType;

/**
 * Abstraction of an entity providing PCEP LS Object.
 */
public interface PcepLSObject {

    /**
     * Returns LS object header.
     *
     * @return LS object header
     */
    PcepObjectHeader getLSObjHeader();

    /**
     * Sets LS Object header.
     *
     * @param obj LS Object header
     */
    void setLSObjHeader(PcepObjectHeader obj);

    /**
     * Returns ProtocolId in LS Object.
     *
     * @return ProtocolId in LS Object
     */
    byte getProtocolId();

    /**
     * Sets ProtocolId in LS Object.
     *
     * @param protId ProtocolId in LS Object
     */
    void setProtocolId(byte protId);

    /**
     * Returns R flag in LS Object.
     *
     * @return R flag in LS Object
     */
    boolean getRemoveFlag();

    /**
     * Sets R flag in LS Object.
     *
     * @param removeFlag R flag in LS Object
     */
    void setRemoveFlag(boolean removeFlag);

    /**
     * Returns sync flag in LS Object.
     *
     * @return sync flag in LS Object
     */
    boolean getSyncFlag();

    /**
     * Sets sync flag in LS Object.
     *
     * @param syncFlag sync flag in LS Object
     */
    void setSyncFlag(boolean syncFlag);

    /**
     * Returns LS ID in LS Object.
     *
     * @return LS ID in LS Object
     */
    long getLSId();

    /**
     * Sets LS ID in LS Object.
     *
     * @param lsId LS ID in LS Object
     */
    void setLSId(long lsId);

    /**
     * Returns list of Optional Tlvs in LS Object.
     *
     * @return list of Optional Tlvs
     */
    List<PcepValueType> getOptionalTlv();

    /**
     * Sets list of Optional Tlvs in LS Object.
     *
     * @param optionalTlvList list of Optional Tlvs
     */
    void setOptionalTlv(List<PcepValueType> optionalTlvList);

    /**
     * Writes the LS Object into channel buffer.
     *
     * @param bb channel buffer
     * @return Returns the writerIndex of this buffer
     * @throws PcepParseException when object header is not written to channel buffer
     */
    int write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build LS object.
     */
    interface Builder {

        /**
         * Builds LS Object.
         *
         * @return LS Object
         */
        PcepLSObject build();

        /**
         * Returns LS object header.
         *
         * @return LS object header
         */
        PcepObjectHeader getLSObjHeader();

        /**
         * Sets LS object header and returns its builder.
         *
         * @param obj LS object header
         * @return Builder by setting LS object header
         */
        Builder setLSObjHeader(PcepObjectHeader obj);

        /**
         * Returns ProtocolId in LS Object.
         *
         * @return ProtocolId in LS Object
         */
        byte getProtocolId();

        /**
         * Sets ProtocolId in LS Object and returns its builder.
         *
         * @param protId ProtocolId in LS Object
         * @return Builder by setting ProtocolId
         */
        Builder setProtocolId(byte protId);

        /**
         * Returns R flag in LS Object.
         *
         * @return R flag in LS Object
         */
        boolean getRemoveFlag();

        /**
         * Sets R flag in LS Object and returns its builder.
         *
         * @param removeFlag R flag in LS Object
         * @return Builder by setting R flag
         */
        Builder setRemoveFlag(boolean removeFlag);

        /**
         * Returns sync flag in LS Object.
         *
         * @return sync flag in LS Object
         */
        boolean getSyncFlag();

        /**
         * Sets sync flag in LS Object and returns its builder.
         *
         * @param syncFlag sync flag in LS Object
         * @return Builder by setting sync flag
         */
        Builder setSyncFlag(boolean syncFlag);

        /**
         * Returns LS ID in LS Object.
         *
         * @return LS ID in LS Object
         */
        long getLSId();

        /**
         * Sets LS ID in LS Object and returns its builder.
         *
         * @param lsId LS ID in LS Object
         * @return Builder by setting LS ID
         */
        Builder setLSId(long lsId);

        /**
         * Returns list of Optional Tlvs in LS Object.
         *
         * @return list of Optional Tlvs
         */
        List<PcepValueType> getOptionalTlv();

        /**
         * Sets list of Optional Tlvs in LS Object and returns its builder.
         *
         * @param optionalTlvList list of Optional Tlvs
         * @return Builder by setting list of Optional Tlvs
         */
        Builder setOptionalTlv(List<PcepValueType> optionalTlvList);

        /**
         * Sets Processing rule flag in LS object header and returns its builder.
         *
         * @param value boolean value to set Processing rule flag
         * @return Builder by setting Processing rule flag
         */
        Builder setPFlag(boolean value);

        /**
         * Sets Ignore flag in LS object header and returns its builder.
         *
         * @param value boolean value to set Ignore flag
         * @return Builder by setting Ignore flag
         */
        Builder setIFlag(boolean value);
    }
}
