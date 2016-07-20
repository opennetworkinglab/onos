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
package org.onosproject.pcep.controller.impl;

import org.onosproject.pcep.controller.PcepPacketStats;

/**
 * The implementation for PCEP packet statistics.
 */
public class PcepPacketStatsImpl implements PcepPacketStats {

    private int inPacketCount;
    private int outPacketCount;
    private int wrongPacketCount;
    private long time;

    /**
     * Default constructor.
     */
    public PcepPacketStatsImpl() {
        this.inPacketCount = 0;
        this.outPacketCount = 0;
        this.wrongPacketCount = 0;
        this.time = 0;
    }

    @Override
    public int outPacketCount() {
        return outPacketCount;
    }

    @Override
    public int inPacketCount() {
        return inPacketCount;
    }

    @Override
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

    @Override
    public long getTime() {
        return this.time;
    }

    /**
     * Sets the time value.
     *
     * @param time long value of time
     */
    public void setTime(long time) {
        this.time = time;
    }
}
