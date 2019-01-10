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

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Implementation of Link stats info.
 */
public final class DefaultLinkStatsInfo implements LinkStatsInfo {

    private final long txPacket;
    private final long rxPacket;
    private final long txByte;
    private final long rxByte;
    private final long txDrop;
    private final long rxDrop;
    private final long timestamp;

    // private constructor not indented for invoked at outside of this class
    private DefaultLinkStatsInfo(long txPacket, long rxPacket, long txByte,
                                 long rxByte, long txDrop, long rxDrop, long timestamp) {
        this.txPacket = txPacket;
        this.rxPacket = rxPacket;
        this.txByte = txByte;
        this.rxByte = rxByte;
        this.txDrop = txDrop;
        this.rxDrop = rxDrop;
        this.timestamp = timestamp;
    }

    @Override
    public long getTxPacket() {
        return txPacket;
    }

    @Override
    public long getRxPacket() {
        return rxPacket;
    }

    @Override
    public long getTxByte() {
        return txByte;
    }

    @Override
    public long getRxByte() {
        return rxByte;
    }

    @Override
    public long getTxDrop() {
        return txDrop;
    }

    @Override
    public long getRxDrop() {
        return rxDrop;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultLinkStatsInfo that = (DefaultLinkStatsInfo) o;
        return txPacket == that.txPacket &&
                rxPacket == that.rxPacket &&
                txByte == that.txByte &&
                rxByte == that.rxByte &&
                txDrop == that.txDrop &&
                rxDrop == that.rxDrop &&
                timestamp == that.timestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(txPacket, rxPacket, txByte, rxByte, txDrop, rxDrop, timestamp);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("txPacket", txPacket)
                .add("rxPacket", rxPacket)
                .add("txByte", txByte)
                .add("rxByte", rxByte)
                .add("txDrop", txDrop)
                .add("rxDrop", rxDrop)
                .add("timestamp", timestamp)
                .toString();
    }

    /**
     * Obtains a default link stats info builder object.
     *
     * @return link stats info builder object
     */
    public static LinkStatsInfo.Builder builder() {
        return new DefaultLinkStatsInfo.DefaultBuilder();
    }

    /**
     * Builder class of LinkStatsInfo.
     */
    public static final class DefaultBuilder implements LinkStatsInfo.Builder {
        private long txPacket;
        private long rxPacket;
        private long txByte;
        private long rxByte;
        private long txDrop;
        private long rxDrop;
        private long timestamp;

        private DefaultBuilder() {
        }

        @Override
        public DefaultBuilder withTxPacket(long txPacket) {
            this.txPacket = txPacket;
            return this;
        }

        @Override
        public DefaultBuilder withRxPacket(long rxPacket) {
            this.rxPacket = rxPacket;
            return this;
        }

        @Override
        public DefaultBuilder withTxByte(long txByte) {
            this.txByte = txByte;
            return this;
        }

        @Override
        public DefaultBuilder withRxByte(long rxByte) {
            this.rxByte = rxByte;
            return this;
        }

        @Override
        public DefaultBuilder withTxDrop(long txDrop) {
            this.txDrop = txDrop;
            return this;
        }

        @Override
        public DefaultBuilder withRxDrop(long rxDrop) {
            this.rxDrop = rxDrop;
            return this;
        }

        @Override
        public DefaultBuilder withTimestamp(long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        @Override
        public LinkStatsInfo build() {
            return new DefaultLinkStatsInfo(txPacket, rxPacket, txByte, rxByte,
                    txDrop, rxDrop, timestamp);
        }
    }
}
