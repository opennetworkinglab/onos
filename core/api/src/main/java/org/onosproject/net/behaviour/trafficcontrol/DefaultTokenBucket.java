/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.net.behaviour.trafficcontrol;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import org.onosproject.net.behaviour.trafficcontrol.TokenBucket.Type;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.behaviour.trafficcontrol.TokenBucket.Action.DSCP_CLASS;
import static org.onosproject.net.behaviour.trafficcontrol.TokenBucket.Action.DSCP_PRECEDENCE;

/**
 * Default implementation of the token bucket interface.
 */
@Beta
public final class DefaultTokenBucket implements TokenBucket, TokenBucketEntry {

    // Immutable parameters
    private final long rate;
    private final long burstSize;
    private final Action action;
    private final short dscp;
    private final Type type;

    // Mutable parameters
    private long processedPackets;
    private long processedBytes;

    private DefaultTokenBucket(long r, long bS, Action a, short d, Type t) {
        rate = r;
        burstSize = bS;
        action = a;
        dscp = d;
        type = t;
    }

    @Override
    public long rate() {
        return rate;
    }

    @Override
    public long burstSize() {
        return burstSize;
    }

    @Override
    public Action action() {
        return action;
    }

    @Override
    public short dscp() {
        return dscp;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public long processedPackets() {
        return processedPackets;
    }

    @Override
    public void setProcessedPackets(long packets) {
        processedPackets = packets;
    }

    @Override
    public long processedBytes() {
        return processedBytes;
    }

    @Override
    public void setProcessedBytes(long bytes) {
        processedBytes = bytes;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("rate", rate())
                .add("burstSize", burstSize())
                .add("action", action())
                .add("dscp", dscp())
                .add("type", type()).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultTokenBucket that = (DefaultTokenBucket) o;
        return rate == that.rate &&
                burstSize == that.burstSize &&
                Objects.equal(action, that.action) &&
                dscp == that.dscp &&
                Objects.equal(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(rate, burstSize, action, dscp, type);
    }

    /**
     * Returns a new builder reference.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Implementation of the token bucket builder interface.
     */
    public static final class Builder implements TokenBucket.Builder {

        private long rate;
        // Default to 2 * MTU
        private long burstSize = 2L * 1500L;
        private Action action;
        private short dscp;
        private Type type;

        @Override
        public TokenBucket.Builder withRate(long r) {
            rate = r;
            return this;
        }

        @Override
        public TokenBucket.Builder withBurstSize(long bS) {
            burstSize = bS;
            return this;
        }

        @Override
        public TokenBucket.Builder withAction(Action a) {
            action = a;
            return this;
        }

        @Override
        public TokenBucket.Builder withDscp(short d) {
            dscp = d;
            return this;
        }

        @Override
        public TokenBucket.Builder withType(Type t) {
            type = t;
            return this;
        }

        @Override
        public DefaultTokenBucket build() {
            // Not null condition on the action and on the type
            checkNotNull(action, "Must specify an action");
            checkNotNull(type, "Must specify a type");

            // If action is based on DSCP modification
            if (action == DSCP_CLASS || action == DSCP_PRECEDENCE) {
                // dscp should be a value between 0 and 255
                checkArgument(dscp >= MIN_DSCP && dscp <= MAX_DSCP, "Dscp is out of range");
            }

            // Finally we build the token bucket
            return new DefaultTokenBucket(rate, burstSize, action, dscp, type);
        }
    }
}
