/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.openstacktelemetry.api;

/**
 * Link info interface.
 */
public interface LinkInfo {

    /**
     * Obtains the link identification.
     *
     * @return link identification
     */
    String linkId();

    /**
     * Obtains the source IP.
     *
     * @return source IP
     */
    String srcIp();

    /**
     * Obtains source port number.
     *
     * @return source port index
     */
    int srcPort();

    /**
     * Obtains the destination IP.
     *
     * @return destination IP
     */
    String dstIp();

    /**
     * Obtains destination port number.
     *
     * @return destination port number
     */
    int dstPort();

    /**
     * Obtains link stats.
     *
     * @return link stats
     */
    LinkStatsInfo linkStats();

    /**
     * Obtains protocol (e.g., TCP/UDP/ICMP).
     *
     * @return protocol
     */
    String protocol();

    /**
     * Interface of link info builder.
     */
    interface Builder {

        /**
         * Sets link identifier.
         *
         * @param linkId link identifier
         * @return builder instance
         */
        Builder withLinkId(String linkId);

        /**
         * Sets the link source IP.
         *
         * @param srcIp link source IP
         * @return builder instance
         */
        Builder withSrcIp(String srcIp);

        /**
         * Sets the source port number.
         *
         * @param srcPort source port number
         * @return builder instance
         */
        Builder withSrcPort(int srcPort);

        /**
         * Sets the link destination IP.
         *
         * @param dstIp link destination IP
         * @return builder instance
         */
        Builder withDstIp(String dstIp);

        /**
         * Sets the destination port number.
         *
         * @param dstPort destination port number
         * @return builder instance
         */
        Builder withDstPort(int dstPort);

        /**
         * Sets the link stats.
         *
         * @param linkStats link stats
         * @return builder instance
         */
        Builder withLinkStats(LinkStatsInfo linkStats);

        /**
         * Sets protocol.
         *
         * @param protocol protocol
         * @return builder instance
         */
        Builder withProtocol(String protocol);

        /**
         * Creates a LinkInfo instance.
         *
         * @return LinkInfo instance
         */
        DefaultLinkInfo build();
    }
}
