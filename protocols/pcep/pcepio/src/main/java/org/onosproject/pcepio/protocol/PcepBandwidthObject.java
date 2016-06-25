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
 * Abstraction of an entity providing PCEP Bandwidth Object.
 */
public interface PcepBandwidthObject {

    /**
     * Returns bandwidth value.
     *
     * @return bandwidth value
     */
    float getBandwidth();

    /**
     * Sets bandwidth with specified value.
     *
     * @param iBandwidth Bandwidth's value
     */
    void setBandwidth(float iBandwidth);

    /**
     * Writes the BandwidthObject into channel buffer.
     *
     * @param bb channel buffer
     * @return Returns the writerIndex of this buffer
     * @throws PcepParseException if bandwidth object header fails to write in channel buffer
     */
    int write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build bandwidth object.
     */
    interface Builder {

        /**
         * Builds BandwidthObject.
         *
         * @return BandwidthObject
         * @throws PcepParseException if build fails while creating PcepBandwidthObject
         */
        PcepBandwidthObject build() throws PcepParseException;

        /**
         * Returns bandwidth object header.
         *
         * @return bandwidth object header
         */
        PcepObjectHeader getBandwidthObjHeader();

        /**
         * Sets bandwidth object header and returns its builder.
         *
         * @param obj Bandwidth object header
         * @return Builder by setting Bandwidth object header
         */
        Builder setBandwidthObjHeader(PcepObjectHeader obj);

        /**
         * Returns bandwidth value.
         *
         * @return bandwidth
         */
        float getBandwidth();

        /**
         * Sets bandwidth value and return its builder.
         *
         * @param iBandwidth bandwidth value
         * @return Builder by setting bandwidth
         */
        Builder setBandwidth(float iBandwidth);

        /**
         * Sets P flag in Bandwidth object header and returns its builder.
         *
         * @param value boolean value to set P flag
         * @return Builder by setting P flag
         */
        Builder setPFlag(boolean value);

        /**
         * Sets I flag in Bandwidth object header and returns its builder.
         *
         * @param value boolean value to set I flag
         * @return Builder by setting I flag
         */
        Builder setIFlag(boolean value);
    }
}
