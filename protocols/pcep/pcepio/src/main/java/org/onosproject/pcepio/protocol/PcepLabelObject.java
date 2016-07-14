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
 * Abstraction of an entity providing PCEP Label Object.
 */
public interface PcepLabelObject {

    /**
     * Returns O flag in Label Object.
     *
     * @return Boolean value
     */
    boolean getOFlag();

    /**
     * Sets O flag in Label Object with specified value.
     *
     * @param value O flag
     */
    void setOFlag(boolean value);

    /**
     * Returns Label from Label Object.
     *
     * @return Label value
     */
    int getLabel();

    /**
     * Sets Label field in Label Object with specified value.
     *
     * @param value Label
     */
    void setLabel(int value);

    /**
     * Returns list of Optional Tlvs.
     *
     * @return list of Optional Tlvs
     */
    LinkedList<PcepValueType> getOptionalTlv();

    /**
     * Sets Optional Tlvs in Label Object.
     *
     * @param llOptionalTlv list of Optional Tlvs
     */
    void setOptionalTlv(LinkedList<PcepValueType> llOptionalTlv);

    /**
     * Writes the Label Object into channel buffer.
     *
     * @param bb channel buffer
     * @return Returns the writerIndex of this buffer
     * @throws PcepParseException while writing LABEL object into Channel Buffer.
     */
    int write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build Label object.
     */
    interface Builder {

        /**
         * Builds Label Object.
         *
         * @return Label Object
         * @throws PcepParseException while building LABEL object.
         */
        PcepLabelObject build() throws PcepParseException;

        /**
         * Returns Label object header.
         *
         * @return Label object header
         */
        PcepObjectHeader getLabelObjHeader();

        /**
         * Sets Label object header and returns its builder.
         *
         * @param obj Label object header
         * @return Builder by setting Label object header
         */
        Builder setLabelObjHeader(PcepObjectHeader obj);

        /**
         * Returns O flag in Label Object.
         *
         * @return Label value
         */
        boolean getOFlag();

        /**
         * Sets O flag and return its builder.
         *
         * @param value O flag
         * @return Builder by setting O flag
         */
        Builder setOFlag(boolean value);

        /**
         * Returns Label from Label Object.
         *
         * @return Label value
         */
        int getLabel();

        /**
         * Sets Label field and return its builder.
         *
         * @param value Label field
         * @return Builder by setting Label field
         */
        Builder setLabel(int value);

        /**
         * Returns list of Optional Tlvs.
         *
         * @return list of Optional Tlvs
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
         * Sets P flag in Label object header and returns its builder.
         *
         * @param value boolean value to set P flag
         * @return Builder by setting P flag
         */
        Builder setPFlag(boolean value);

        /**
         * Sets I flag in Label object header and returns its builder.
         *
         * @param value boolean value to set I flag
         * @return Builder by setting I flag
         */
        Builder setIFlag(boolean value);
    }
}
