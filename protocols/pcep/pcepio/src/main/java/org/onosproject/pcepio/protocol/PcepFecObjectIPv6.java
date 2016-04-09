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
 * Abstraction of an entity providing FEC Object of Type is 2 IPv6 Node ID.
 */
public interface PcepFecObjectIPv6 extends PcepFecObject {

    /**
     * Returns NodeID of FEC Object.
     *
     * @return NodeID of FEC Object
     */
    byte[] getNodeID();

    /**
     * Sets NodeID with specified value.
     *
     * @param value node id
     */
    void setNodeID(byte[] value);

    @Override
    int write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build FEC object.
     */
    interface Builder {

        /**
         * Builds FEC Object IPv6.
         *
         * @return FEC Object IPv6
         * @throws PcepParseException while building FEC IPv6 Object.
         */
        PcepFecObjectIPv6 build() throws PcepParseException;

        /**
         * Returns FEC Object IPv6 header.
         *
         * @return FEC Object IPv6 header
         */
        PcepObjectHeader getFecIpv6ObjHeader();

        /**
         * Sets FEC Object IPv6 header and returns its builder.
         *
         * @param obj FEC Object IPv6 header
         * @return Builder by setting FEC Object IPv6 header
         */
        Builder setFecIpv6ObjHeader(PcepObjectHeader obj);

        /**
         * Returns NodeID of FEC Object.
         *
         * @return NodeID of FEC Object
         */
        byte[] getNodeID();

        /**
         * Sets NodeID and returns its builder.
         *
         * @param value node id
         * @return Builder by setting NodeID
         */
        Builder setNodeID(byte[] value);

        /**
         * Sets P flag in FEC object header and returns its builder.
         *
         * @param value boolean value to set P flag
         * @return Builder by setting P flag
         */
        Builder setPFlag(boolean value);

        /**
         * Sets I flag in FEC object header and returns its builder.
         *
         * @param value boolean value to set I flag
         * @return Builder by setting I flag
         */
        Builder setIFlag(boolean value);
    }
}
