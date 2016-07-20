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
 * Abstraction of an entity providing PCEP End Points Object.
 */
public interface PcepEndPointsObject {

    /**
     * Returns Source IpAddress from End Points Object.
     *
     * @return Source IpAddress from End Points Object
     */
    int getSourceIpAddress();

    /**
     * Sets Source IpAddress in End Points Object.
     *
     * @param sourceIpAddress Source IP Address
     */
    void setSourceIpAddress(int sourceIpAddress);

    /**
     * Returns Destination IpAddress from End Points Object.
     *
     * @return Destination IpAddress from End Points Object
     */
    int getDestIpAddress();

    /**
     * Sets Destination IpAddress in End Points Object.
     *
     * @param destIpAddress Destination IP Address
     */
    void setDestIpAddress(int destIpAddress);

    /**
     * Writes the EndPointsObject into channel buffer.
     *
     * @param bb channel buffer
     * @return Returns the writerIndex of this buffer
     * @throws PcepParseException while writing EndPointObject into ChannelBuffer
     */
    int write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build EndPoints object.
     */
    interface Builder {

        /**
         * Builds End Points Object.
         *
         * @return End Points Object
         * @throws PcepParseException while building EndPointObject
         */
        PcepEndPointsObject build() throws PcepParseException;

        /**
         * Returns End Points Object header.
         *
         * @return End Points Object header
         */
        PcepObjectHeader getEndPointsObjHeader();

        /**
         * Sets End Points Object header and returns its builder.
         *
         * @param obj End Points Object header
         * @return Builder by setting End Points Object header
         */
        Builder setEndPointsObjHeader(PcepObjectHeader obj);

        /**
         * Returns Source IpAddress from End Points Object.
         *
         * @return Source IpAddress from End Points Object
         */
        int getSourceIpAddress();

        /**
         * Sets Source IpAddress in End Points Object and returns builder.
         *
         * @param sourceIpAddress Source IP Address
         * @return Builder by setting Source IpAddress in End Points Object
         */
        Builder setSourceIpAddress(int sourceIpAddress);

        /**
         * Returns Destination IpAddress from End Points Object.
         *
         * @return Destination IpAddress from End Points Object
         */
        int getDestIpAddress();

        /**
         * Sets Destination IpAddress in End Points Object.
         *
         * @param destIpAddress Destination IP Address
         * @return Builder by setting Destination IpAddress in End Points Object
         */
        Builder setDestIpAddress(int destIpAddress);

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
