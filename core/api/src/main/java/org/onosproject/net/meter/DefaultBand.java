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
package org.onosproject.net.meter;

import com.google.common.base.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A default implementation for a Band.
 */
public final class DefaultBand implements Band, BandEntry {

    private final Type type;
    private final long rate;
    //TODO: should be made optional
    private final Long burstSize;
    private final Short prec;
    private long packets;
    private long bytes;

    public DefaultBand(Type type, long rate,
                       Long burstSize, Short prec) {
        this.type = type;
        if (type == Type.REMARK) {
            checkArgument(prec <= MAX_PRECEDENCE && prec >= MIN_PRECEDENCE, ERR_MSG);
        }
        this.rate = rate;
        this.burstSize = burstSize;
        this.prec = prec;
    }

    @Override
    public long rate() {
        return rate;
    }

    @Override
    public Long burst() {
        return burstSize;
    }

    @Override
    public Short dropPrecedence() {
        return prec;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public long packets() {
        return packets;
    }

    @Override
    public long bytes() {
        return bytes;
    }

    @Override
    public void setPackets(long packets) {
        this.packets = packets;
    }

    @Override
    public void setBytes(long bytes) {
        this.bytes = bytes;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("rate", rate)
                .add("burst-size", burstSize)
                .add("type", type)
                .add("drop-precedence", prec).toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultBand that = (DefaultBand) o;
        return rate == that.rate &&
                type == that.type &&
                Objects.equal(burstSize, that.burstSize) &&
                Objects.equal(prec, that.prec);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type, rate, burstSize, prec);
    }

    public static final class Builder implements Band.Builder {

        private long rate;
        private Long burstSize;
        private Short prec;
        private Type type;

        @Override
        public Band.Builder withRate(long rate) {
            this.rate = rate;
            return this;
        }

        @Override
        public Band.Builder burstSize(long burstSize) {
            this.burstSize = burstSize;
            return this;
        }

        @Override
        public Band.Builder dropPrecedence(short prec) {
            this.prec = prec;
            return this;
        }

        @Override
        public Band.Builder ofType(Type type) {
            this.type = type;
            return this;
        }

        @Override
        public DefaultBand build() {
            checkNotNull(type, "Band type can not be null");
            checkArgument(type == Type.REMARK ^ prec == null,
                    "Only REMARK bands can have a precedence.");
            return new DefaultBand(type, rate, burstSize, prec);
        }
    }
}
