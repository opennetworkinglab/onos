/*
 * Copyright 2015 Open Networking Laboratory
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

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;

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
        this.rate = rate;
        this.burstSize = burstSize;
        this.prec = prec;
    }

    @Override
    public long rate() {
        return rate;
    }

    @Override
    public long burst() {
        return burstSize;
    }

    @Override
    public short dropPrecedence() {
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
            checkArgument(type != Type.REMARK && prec == null,
                          "Only REMARK bands can have a precendence.");

            return new DefaultBand(type, rate, burstSize, prec);
        }


    }
}
