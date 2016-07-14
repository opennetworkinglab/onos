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
 * Abstraction of an entity providing PCEP FEC Object of Type is 5 Unnumbered Adjacency with IPv4 NodeIDs.
 */
public interface PcepFecObjectIPv4UnnumberedAdjacency extends PcepFecObject {

    /**
     * Returns Local NodeID of FEC Object.
     *
     * @return Local NodeID of FEC Object
     */
    int getLocalNodeID();

    /**
     * Sets Local NodeID with specified value.
     *
     * @param value Local NodeID
     */
    void setLocalNodeID(int value);

    /**
     * Returns Local InterfaceID of FEC Object.
     *
     * @return Local InterfaceID of FEC Object
     */
    int getLocalInterfaceID();

    /**
     * Sets Local InterfaceID with specified value.
     *
     * @param value Local InterfaceID
     */
    void setLocalInterfaceID(int value);

    /**
     * Returns Remote NodeID of FEC Object.
     *
     * @return Remote NodeID of FEC Object
     */
    int getRemoteNodeID();

    /**
     * Sets Remote NodeID with specified value.
     *
     * @param value Remote NodeID
     */
    void setRemoteNodeID(int value);

    /**
     * Returns Remote InterfaceID of FEC Object.
     *
     * @return Remote InterfaceID of FEC Object
     */
    int getRemoteInterfaceID();

    /**
     * Sets Remote InterfaceID with specified value.
     *
     * @param value Remote InterfaceID
     */
    void setRemoteInterfaceID(int value);

    @Override
    int write(ChannelBuffer bb) throws PcepParseException;

    /**
     * Builder interface with get and set functions to build bandwidth object.
     */
    interface Builder {

        /**
         * Builds FEC Unnumbered Adjacency with IPv4 Object.
         *
         * @return FEC Unnumbered Adjacency with IPv4 Object
         * @throws PcepParseException when building FEC IPv4 Unnumbered Adjacency object.
         */
        PcepFecObjectIPv4UnnumberedAdjacency build() throws PcepParseException;

        /**
         * Returns FEC Unnumbered Adjacency with IPv4 header.
         *
         * @return FEC Unnumbered Adjacency with IPv4 header
         */
        PcepObjectHeader getFecIpv4UnnumberedAdjacencyObjHeader();

        /**
         * Sets FEC Unnumbered Adjacency with IPv4 header and returns its builder.
         *
         * @param obj FEC Unnumbered Adjacency with IPv4 header
         * @return Builder by setting FEC Unnumbered Adjacency with IPv4 header
         */
        Builder setFecIpv4UnnumberedAdjacencyObjHeader(PcepObjectHeader obj);

        /**
         * Returns Local NodeID of FEC Object.
         *
         * @return Local NodeID of FEC Object
         */
        int getLocalNodeID();

        /**
         * Sets Local NodeID and returns its builder.
         *
         * @param value Local NodeID
         * @return Builder by setting Local NodeID
         */
        Builder setLocalNodeID(int value);

        /**
         * Returns Local InterfaceID of FEC Object.
         *
         * @return Local InterfaceID of FEC Object
         */
        int getLocalInterfaceID();

        /**
         * Sets Local InterfaceID and returns its builder.
         *
         * @param value Local InterfaceID
         * @return Builder by setting Local InterfaceID
         */
        Builder setLocalInterfaceID(int value);

        /**
         * Returns Remote NodeID of FEC Object.
         *
         * @return Remote NodeID of FEC Object
         */
        int getRemoteNodeID();

        /**
         * Sets Remote NodeID and returns its builder.
         *
         * @param value Remote NodeID
         * @return Builder by setting Remote NodeID
         */
        Builder setRemoteNodeID(int value);

        /**
         * Returns Remote InterfaceID of FEC Object.
         *
         * @return Remote InterfaceID of FEC Object
         */
        int getRemoteInterfaceID();

        /**
         * Sets Remote InterfaceID and returns its builder.
         *
         * @param value Remote InterfaceID
         * @return Builder by setting Remote InterfaceID
         */
        Builder setRemoteInterfaceID(int value);

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
