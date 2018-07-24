/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onlab.packet.lacp;

import java.util.Objects;

/**
 * LACP state.
 */
public class LacpState {
    private static final byte MASK_ACTIVE = 0x1;
    private static final byte MASK_TIMEOUT = 0x2;
    private static final byte MASK_AGG = 0x4;
    private static final byte MASK_SYNC = 0x8;
    private static final byte MASK_COLLECTING = 0x10;
    private static final byte MASK_DISTRIBUTING = 0x20;
    private static final byte MASK_DEFAULT = 0x40;
    private static final byte MASK_EXPIRED = (byte) 0x80;

    private byte state;

    /**
     * Constructs LACP state with zero value.
     */
    public LacpState() {
        this.state = 0;
    }

    /**
     * Constructs LACP state with given value.
     *
     * @param state state in byte.
     */
    public LacpState(byte state) {
        this.state = state;
    }

    /**
     * Gets LACP state in byte.
     *
     * @return LACP state
     */
    public byte toByte() {
        return state;
    }

    /**
     * Checks if this state has the active flag set.
     *
     * @return true if this state has the active flag set.
     */
    public boolean isActive() {
        return (state & MASK_ACTIVE) != 0;
    }

    /**
     * Sets active bit.
     *
     * @param value desired value
     * @return this
     */
    public LacpState setActive(boolean value) {
        setBit(MASK_ACTIVE, value);
        return this;
    }

    /**
     * Checks if this state has the timeout flag set. Timeout flag indicates short timeout if set.
     *
     * @return true if this state has the timeout flag set.
     */
    public boolean isTimeout() {
        return (state & MASK_TIMEOUT) != 0;
    }

    /**
     * Sets timeout bit.
     *
     * @param value desired value
     * @return this
     */
    public LacpState setTimeout(boolean value) {
        setBit(MASK_TIMEOUT, value);
        return this;
    }

    /**
     * Checks if this state has the aggregatable flag set.
     *
     * @return true if this state has the aggregatable flag set.
     */
    public boolean isAggregatable() {
        return (state & MASK_AGG) != 0;
    }

    /**
     * Sets aggregatable bit.
     *
     * @param value desired value
     * @return this
     */
    public LacpState setAggregatable(boolean value) {
        setBit(MASK_AGG, value);
        return this;
    }

    /**
     * Checks if this state has the synchronization flag set.
     *
     * @return true if this state has the synchronization flag set.
     */
    public boolean isSync() {
        return (state & MASK_SYNC) != 0;
    }

    /**
     * Sets sync bit.
     *
     * @param value desired value
     * @return this
     */
    public LacpState setSync(boolean value) {
        setBit(MASK_SYNC, value);
        return this;
    }

    /**
     * Checks if this state has the collecting flag set.
     *
     * @return true if this state has the collecting flag set.
     */
    public boolean isCollecting() {
        return (state & MASK_COLLECTING) != 0;
    }

    /**
     * Sets collecting bit.
     *
     * @param value desired value
     * @return this
     */
    public LacpState setCollecting(boolean value) {
        setBit(MASK_COLLECTING, value);
        return this;
    }

    /**
     * Checks if this state has the distributing flag set.
     *
     * @return true if this state has the distributing flag set.
     */
    public boolean isDistributing() {
        return (state & MASK_DISTRIBUTING) != 0;
    }

    /**
     * Sets distributing bit.
     *
     * @param value desired value
     * @return this
     */
    public LacpState setDistributing(boolean value) {
        setBit(MASK_DISTRIBUTING, value);
        return this;
    }

    /**
     * Checks if this state has the default flag set.
     *
     * @return true if this state has the default flag set.
     */
    public boolean isDefault() {
        return (state & MASK_DEFAULT) != 0;
    }

    /**
     * Sets default bit.
     *
     * @param value desired value
     * @return this
     */
    public LacpState setDefault(boolean value) {
        setBit(MASK_DEFAULT, value);
        return this;
    }

    /**
     * Checks if this state has the expired flag set.
     *
     * @return true if this state has the expired flag set.
     */
    public boolean isExpired() {
        return (state & MASK_EXPIRED) != 0;
    }

    /**
     * Sets expired bit.
     *
     * @param value desired value
     * @return this
     */
    public LacpState setExpired(boolean value) {
        setBit(MASK_EXPIRED, value);
        return this;
    }

    /**
     * Sets the bit masked by given mask in the state to desired value.
     *
     * @param mask bit to mask
     * @param value desire value
     */
    private void setBit(byte mask, boolean value) {
        state = (byte) (value ? state | mask : state & ~mask);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LacpState)) {
            return false;
        }
        final LacpState other = (LacpState) obj;

        return this.state == other.state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(state);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{ ");
        if (isActive()) {
            builder.append("ACT ");
        }
        if (isTimeout()) {
            builder.append("STO ");
        }
        if (isAggregatable()) {
            builder.append("AGG ");
        }
        if (isSync()) {
            builder.append("SYN ");
        }
        if (isCollecting()) {
            builder.append("COL ");
        }
        if (isDistributing()) {
            builder.append("DIS ");
        }
        if (isDefault()) {
            builder.append("DEF ");
        }
        if (isExpired()) {
            builder.append("EXP ");
        }
        builder.append("}");
        return builder.toString();
    }
}
