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
 * Abstraction of an entity providing PCEP LSP Object.
 */
public interface PcepLspObject {

    /**
     * Returns PlspId of LSP Object.
     *
     * @return PlspId of LSP Object
     */
    int getPlspId();

    /**
     * Sets PlspId with specified value.
     *
     * @param value PlspId
     */
    void setPlspId(int value);

    /**
     * Returns O flag in LSP Object.
     *
     * @return O flag in LSP Object
     */
    byte getOFlag();

    /**
     * Sets O flag with specified value.
     *
     * @param value O flag
     */
    void setOFlag(byte value);

    /**
     * Returns C flag in LSP Object.
     *
     * @return C flag in LSP Object
     */
    boolean getCFlag();

    /**
     * Sets C flag with specified value.
     *
     * @param value C flag
     */
    void setCFlag(boolean value);

    /**
     * Returns A flag in LSP Object.
     *
     * @return A flag in LSP Object
     */
    boolean getAFlag();

    /**
     * Sets A flag with specified value.
     *
     * @param value A flag
     */
    void setAFlag(boolean value);

    /**
     * Returns R flag in LSP Object.
     *
     * @return R flag in LSP Object
     */
    boolean getRFlag();

    /**
     * Sets R flag with specified value.
     *
     * @param value R flag
     */
    void setRFlag(boolean value);

    /**
     * Returns S flag in LSP Object.
     *
     * @return S flag in LSP Object
     */
    boolean getSFlag();

    /**
     * Sets S flag with specified value.
     *
     * @param value S flag
     */
    void setSFlag(boolean value);

    /**
     * Returns D flag in LSP Object.
     *
     * @return D flag in LSP Object
     */
    boolean getDFlag();

    /**
     * Sets D flag with specified value.
     *
     * @param value D flag
     */
    void setDFlag(boolean value);

    /**
     * Returns list of Optional Tlvs in LSP Object.
     *
     * @return list of Optional Tlvs
     */
    LinkedList<PcepValueType> getOptionalTlv();

    /**
     * Sets list of Optional Tlvs in LSP Object.
     *
     * @param llOptionalTlv list of Optional Tlvs
     */
    void setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv);

    /**
     * Writes the LSP Object into channel buffer.
     *
     * @param bb channel buffer
     * @return Returns the writerIndex of this buffer
     * @throws PcepParseException while writing LSP object into Channel Buffer.
     */
    int write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build LSP object.
     */
    interface Builder {

        /**
         * Builds LSP Object.
         *
         * @return LSP Object
         */
        PcepLspObject build();

        /**
         * Returns LSP object header.
         *
         * @return LSP object header
         */
        PcepObjectHeader getLspObjHeader();

        /**
         * Sets LSP object header and returns its builder.
         *
         * @param obj LSP object header
         * @return Builder by setting LSP object header
         */
        Builder setLspObjHeader(PcepObjectHeader obj);

        /**
         * Returns PlspId of LSP Object.
         *
         * @return PlspId of LSP Object
         */
        int getPlspId();

        /**
         * Sets PlspId with specific value and return its builder.
         *
         * @param value PlspId
         * @return Builder by setting PlspId
         */
        Builder setPlspId(int value);

        /**
         * Returns C flag in LSP Object.
         *
         * @return C flag in LSP Object
         */
        boolean getCFlag();

        /**
         * Sets C flag with specific value and return its builder.
         *
         * @param value C flag
         * @return Builder by setting C flag
         */
        Builder setCFlag(boolean value);

        /**
         * Returns O flag in LSP Object.
         *
         * @return O flag in LSP Object
         */
        byte getOFlag();

        /**
         * Sets O flag with specific value and return its builder.
         *
         * @param value O flag
         * @return Builder by setting O flag
         */
        Builder setOFlag(byte value);

        /**
         * Returns A flag in LSP Object.
         *
         * @return A flag in LSP Object
         */
        boolean getAFlag();

        /**
         * Sets A flag with specific value and return its builder.
         *
         * @param value A flag
         * @return Builder by setting A flag
         */
        Builder setAFlag(boolean value);

        /**
         * Returns A flag in LSP Object.
         *
         * @return A flag in LSP Object
         */
        boolean getRFlag();

        /**
         * Sets R flag with specific value and return its builder.
         *
         * @param value r flag
         * @return Builder by setting r flag
         */
        Builder setRFlag(boolean value);

        /**
         * Returns S flag in LSP Object.
         *
         * @return S flag in LSP Object
         */
        boolean getSFlag();

        /**
         * Sets S flag with specific value and return its builder.
         *
         * @param value s flag
         * @return Builder by setting S flag
         */
        Builder setSFlag(boolean value);

        /**
         * Returns D flag in LSP Object.
         *
         * @return D flag in LSP Object
         */
        boolean getDFlag();

        /**
         * Sets D flag with specific value and return its builder.
         *
         * @param value D flag
         * @return Builder by setting D flag
         */
        Builder setDFlag(boolean value);

        /**
         * Returns list of Optional Tlvs in LSP Object.
         *
         * @return list of Optional Tlvs in LSP Object
         */
        LinkedList<PcepValueType> getOptionalTlv();

        /**
         * Sets list of Optional Tlvs and return its builder.
         *
         * @param llOptionalTlv list of Optional Tlvs
         * @return Builder by setting list of Optional Tlvs
         */
        Builder setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv);

        /**
         * Sets P flag in LSP object header and returns its builder.
         *
         * @param value boolean value to set P flag
         * @return Builder by setting P flag
         */
        Builder setPFlag(boolean value);

        /**
         * Sets I flag in LSP object header and returns its builder.
         *
         * @param value boolean value to set I flag
         * @return Builder by setting I flag
         */
        Builder setIFlag(boolean value);
    }
}
