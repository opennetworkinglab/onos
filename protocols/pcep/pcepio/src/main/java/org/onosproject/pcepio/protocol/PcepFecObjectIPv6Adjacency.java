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
 * Abstraction of an entity providing FEC Object of Type is 4 IPv6 Adjacency.
 */
public interface PcepFecObjectIPv6Adjacency extends PcepFecObject {

    /**
     * Returns Local IPv6Address of FEC Object.
     *
     * @return Local IPv6Address of FEC Object
     */
    byte[] getLocalIPv6Address();

    /**
     * Sets Local IPv6Address with specified value.
     *
     * @param value Local IPv6Address
     */
    void seLocalIPv6Address(byte[] value);

    /**
     * Returns Remote IPv6Address of FEC Object.
     *
     * @return Remote IPv6Address of FEC Object
     */
    byte[] getRemoteIPv6Address();

    /**
     * Sets Remote IPv6Address with specified value.
     *
     * @param value Remote IPv6Address
     */
    void seRemoteIPv6Address(byte[] value);

    @Override
    int write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build FEC object.
     */
    interface Builder {

        /**
         * Builds FEC Object IPv6 Adjacency.
         *
         * @return FEC Object IPv6 Adjacency
         * @throws PcepParseException while building FEC IPv6 Adjacency object.
         */
        PcepFecObjectIPv6Adjacency build() throws PcepParseException;

        /**
         * Returns FEC Object IPv6 Adjacency header.
         *
         * @return FEC Object IPv6 Adjacency header
         */
        PcepObjectHeader getFecIpv6AdjacencyObjHeader();

        /**
         * Sets FEC Object IPv6 Adjacency header and returns its builder.
         *
         * @param obj FEC Object IPv6 Adjacency header
         * @return Builder by setting FEC Object IPv6 Adjacency header
         */
        Builder setFecIpv6AdjacencyObjHeader(PcepObjectHeader obj);

        /**
         * Returns Local IPv6Address of FEC Object.
         *
         * @return Local IPv6Address of FEC Object
         */
        byte[] getLocalIPv6Address();

        /**
         * Sets Local IPv6Address and returns its builder.
         *
         * @param value Local IPv6Address
         * @return Builder by setting Local IPv6Address
         */
        Builder setLocalIPv6Address(byte[] value);

        /**
         * Returns Remote IPv6Address of FEC Object.
         *
         * @return Remote IPv6Address of FEC Object
         */
        byte[] getRemoteIPv6Address();

        /**
         * Sets Remote IPv6Address and returns its builder.
         *
         * @param value Remote IPv6Address
         * @return Builder by setting Remote IPv6Address
         */
        Builder setRemoteIPv6Address(byte[] value);

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
