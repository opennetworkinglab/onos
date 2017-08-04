/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.bgp.controller.impl;

import org.onosproject.bgp.controller.BgpPacketStats;

/**
 * A representation of a packet context which allows any provider
 * to view a packet in event, but may block the response to the
 * event if blocked has been called. This packet context can be used
 * to react to the packet in event with a packet out.
 */
public class BgpPacketStatsImpl implements BgpPacketStats {

    private int inPacketCount;
    private int outPacketCount;
    private int wrongPacketCount;
    private long time;

    /**
     * Resets parameter.
     */
    public BgpPacketStatsImpl() {
        this.inPacketCount = 0;
        this.outPacketCount = 0;
        this.wrongPacketCount = 0;
        this.time = 0;
    }

    /**
     * Get the outgoing packet count number.
     *
     * @return packet count
     */
    public int outPacketCount() {
        return outPacketCount;
    }

    /**
     * Get the incoming packet count number.
     *
     * @return packet count
     */
    public int inPacketCount() {
        return inPacketCount;
    }

    /**
     * Get the wrong packet count number.
     *
     * @return packet count
     */
    public int wrongPacketCount() {
        return wrongPacketCount;
    }

    /**
     * Increments the received packet counter.
     */
    public void addInPacket() {
        this.inPacketCount++;
    }

    /**
     * Increments the sent packet counter.
     */
    public void addOutPacket() {
        this.outPacketCount++;
    }

    /**
     * Increments the sent packet counter by specified value.
     *
     * @param value of no of packets sent
     */
    public void addOutPacket(int value) {
        this.outPacketCount = this.outPacketCount + value;
    }

    /**
     * Increments the wrong packet counter.
     */
    public void addWrongPacket() {
        this.wrongPacketCount++;
    }

    /**
     * Resets wrong packet count.
     */
    public void resetWrongPacket() {
        this.wrongPacketCount = 0;
    }

    /**
     * Get the time.
     *
     * @return time
     */
    public long getTime() {
        return this.time;
    }

    /**
     * Sets the time.
     *
     * @param time value to set
     */
    public void setTime(long time) {
        this.time = time;
    }
}