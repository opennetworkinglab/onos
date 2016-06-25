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

package org.onosproject.bgp.controller;

/**
 * A representation of a packet context which allows any provider to view a packet in event, but may block the response
 * to the event if blocked has been called. This packet context can be used to react to the packet in event with a
 * packet out.
 */
public interface BgpPacketStats {
    /**
     * Returns the count for no of packets sent out.
     *
     * @return int value of no of packets sent
     */
    int outPacketCount();

    /**
     * Returns the count for no of packets received.
     *
     * @return int value of no of packets sent
     */
    int inPacketCount();

    /**
     * Returns the count for no of wrong packets received.
     *
     * @return int value of no of wrong packets received
     */
    int wrongPacketCount();

    /**
     * Returns the time.
     *
     * @return the time
     */
    long getTime();
}