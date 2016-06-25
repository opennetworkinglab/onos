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
 * Abstraction of an entity providing FEC Object of Type 3 IPv4 Adjacency.
 */
public interface PcepFecObjectIPv4Adjacency extends PcepFecObject {

    /**
     * Returns Local IPv4Address of FEC Object.
     *
     * @return Local IPv4Address of FEC Object
     */
    int getLocalIPv4Address();

    /**
     * Sets Local IPv4Address with specified value.
     *
     * @param value Local IPv4Address
     */
    void seLocalIPv4Address(int value);

    /**
     * Returns Remote IPv4Address of FEC Object.
     *
     * @return Remote IPv4Address of FEC Object
     */
    int getRemoteIPv4Address();

    /**
     * Sets Remote IPv4Address with specified value.
     *
     * @param value Remote IPv4Address
     */
    void seRemoteIPv4Address(int value);

    @Override
    int write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build FEC object.
     */
    interface Builder {

        /**
         * Builds FEC Object IPv4 Adjacency.
         *
         * @return FEC Object IPv4 Adjacency
         * @throws PcepParseException while building FEC IPv4 Adjacency object.
         */
        PcepFecObjectIPv4Adjacency build() throws PcepParseException;

        /**
         * Returns FEC Object IPv4 Adjacency header.
         *
         * @return FEC Object IPv4 Adjacency header
         */
        PcepObjectHeader getFecIpv4AdjacencyObjHeader();

        /**
         * Sets FEC Object IPv4 Adjacency header and returns its builder.
         *
         * @param obj FEC Object IPv4 Adjacency header
         * @return Builder by setting FEC Object IPv4 header
         */
        Builder setFecIpv4AdjacencyObjHeader(PcepObjectHeader obj);

        /**
         * Returns Local IPv4Address of FEC Object.
         *
         * @return Local IPv4Address of FEC Object
         */
        int getLocalIPv4Address();

        /**
         * Sets Local IPv4Address and returns its builder.
         *
         * @param value Local IPv4Address
         * @return Builder by setting Local IPv4Address
         */
        Builder seLocalIPv4Address(int value);

        /**
         * Sets Remote IPv4Address with specified value.
         *
         * @return Remote IPv4 Address
         */
        int getRemoteIPv4Address();

        /**
         * Sets Remote IPv4Address and returns its builder.
         *
         * @param value Remote IPv4Address
         * @return Builder by setting Remote IPv4Address
         */
        Builder seRemoteIPv4Address(int value);

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
