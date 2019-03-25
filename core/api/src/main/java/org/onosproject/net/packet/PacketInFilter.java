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
package org.onosproject.net.packet;

/**
 * Abstraction of incoming packet filter.
 */
public interface PacketInFilter {

    /**
     * Types of filter action applied to incoming packets.
     */
    enum FilterAction {
        /**
         * Signifies that the packet is allowed to be processed.
         */
        PACKET_ALLOW,
        /**
         * Signifies that the packet is denied from being processed
         * as it crossed the maxCounter.
         */
        PACKET_DENY,
        /**
         * Signifies that filter applied is a valid filter.
         */
        FILTER_VALID,
        /**
         * Signifies that this filter is disabled.
         */
        FILTER_DISABLED,
        /**
         * Signifies that the current window for packet processing is full
         * and the window is blocked for packet processing.
         */
        WINDOW_BLOCKED,
        /**
         * Signifies that the packet processing is blocked as the
         * threshold has crossed.
         */
        PACKET_BLOCKED,
        /**
         * Signifies that the filter applied is invalid filter.
         */
        FILTER_INVALID
    }

    /**
     * Returns FilterAction before processing the packet.
     * Decides if the packet is allowed to be processed or not.
     *
     * @param packet PackerContext holding the packet information
     * @return FilterAction
     */
    FilterAction preProcess(PacketContext packet);

    /**
     * Get the name of the counter.
     *
     * @return name of the counter
     */
    String name();

    /**
     * Get the current value of the count of packets for this particular
     * filter type waiting to get processed.
     *
     * @return count of packets with current filter type waiting to get processed
     */
    int pendingPackets();

    /**
     * Get the count of the dropped packets for this filter type.
     *
     * @return count of dropped packets for this filter type
     */
    int droppedPackets();

    /**
     * Set the pps rate for the current filter type to calculate the max counter
     * allowed with window size.
     *
     * @param pps Packet per second rate expected
     */
    void setPps(int pps);

    /**
     * Set the window size for rate limiting.
     *
     * @param winSize Window size in milli seconds
     */
    void setWinSize(int winSize);

    /**
     * Set the Guard time in case WinThres is crossed.
     *
     * @param guardTime Guard time in  seconds
     */
    void setGuardTime(int guardTime);

    /**
     * Set the Window Threshold for dropping the packet.
     *
     * @param winThres Threshold count of the consecutive windows with packet drops
     */
    void setWinThres(int winThres);

    /**
     * Stop the threads running for this filter.
     *
     */
    void stop();

}
