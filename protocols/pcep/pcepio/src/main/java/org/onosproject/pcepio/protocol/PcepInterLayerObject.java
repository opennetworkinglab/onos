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
import org.onosproject.pcepio.types.PcepObjectHeader;

/**
 * Abstraction of an entity providing PCEP INTER Layer Object.
 */
public interface PcepInterLayerObject {

    /**
     * Returns N Flag in INTER Layer Object.
     *
     * @return N Flag in INTER Layer Object
     */
    boolean getbNFlag();

    /**
     * Sets N Flag in INTER Layer Object with specified value.
     *
     * @param value N Flag
     */
    void setbNFlag(boolean value);

    /**
     * Returns I Flag in INTER Layer Object.
     *
     * @return I Flag in INTER Layer Object
     */
    boolean getbIFlag();

    /**
     * Sets I Flag in INTER Layer Object with specified value.
     *
     * @param value I Flag
     */
    void setbIFlag(boolean value);

    /**
     * Writes the INTER Layer Object into channel buffer.
     *
     * @param bb channel buffer
     * @return Returns the writerIndex of this buffer
     * @throws PcepParseException while writing Inter Layer Object.
     */
    int write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build INTER Layer object.
     */
    interface Builder {

        /**
         * Builds INTER Layer object.
         *
         * @return INTER Layer object
         */
        PcepInterLayerObject build();

        /**
         * Returns INTER Layer object header.
         *
         * @return INTER Layer object header
         */
        PcepObjectHeader getInterLayerObjHeader();

        /**
         * Sets INTER Layer object header and returns its builder.
         *
         * @param obj INTER Layer object header
         * @return Builder by setting INTER Layer object header
         */
        Builder setInterLayerObjHeader(PcepObjectHeader obj);

        /**
         * Returns N Flag in INTER Layer Object.
         *
         * @return N Flag in INTER Layer Object
         */
        boolean getbNFlag();

        /**
         * Sets N flag and return its builder.
         *
         * @param value N flag
         * @return Builder by setting N flag
         */
        Builder setbNFlag(boolean value);

        /**
         * Returns I Flag in INTER Layer Object.
         *
         * @return I Flag in INTER Layer Object
         */
        boolean getbIFlag();

        /**
         * Sets I flag and return its builder.
         *
         * @param value I flag
         * @return Builder by setting N flag
         */
        Builder setbIFlag(boolean value);

        /**
         * Sets P flag in INTER Layer object header and returns its builder.
         *
         * @param value boolean value to set P flag
         * @return Builder by setting P flag
         */
        Builder setPFlag(boolean value);

        /**
         * Sets I flag in INTER Layer object header and returns its builder.
         *
         * @param value boolean value to set I flag
         * @return Builder by setting I flag
         */
        Builder setIFlag(boolean value);
    }
}
