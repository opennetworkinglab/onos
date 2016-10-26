/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.net.behaviour;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import org.onlab.util.Bandwidth;
import org.onosproject.net.AbstractDescription;
import org.onosproject.net.SparseAnnotations;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of Queue description entity.
 */
@Beta
public final class DefaultQueueDescription extends AbstractDescription
        implements QueueDescription {

    private final QueueId queueId;
    private final Optional<Integer> dscp;
    private final EnumSet<Type> type;
    private final Optional<Bandwidth> maxRate;
    private final Optional<Bandwidth> minRate;
    private final Optional<Long> burst;
    private final Optional<Long> priority;

    public static final int MIN_DSCP = 0;
    public static final int MAX_DSCP = 63;

    private DefaultQueueDescription(QueueId queueId, Optional<Integer> dscp, EnumSet<Type> type,
                                    Optional<Bandwidth> maxRate, Optional<Bandwidth> minRate,
                                    Optional<Long> burst, Optional<Long> priority,
                                    SparseAnnotations... annotations) {
        super(annotations);
        if (dscp.isPresent()) {
            checkArgument(dscp.get() < MIN_DSCP || dscp.get() > MAX_DSCP, "dscp should be in range 0 to 63.");
        }
        this.queueId = checkNotNull(queueId);
        this.dscp = dscp;
        this.type = type;
        this.maxRate = maxRate;
        this.minRate = minRate;
        this.burst = burst;
        this.priority = priority;
    }

    @Override
    public QueueId queueId() {
        return queueId;
    }

    @Override
    public EnumSet<Type> type() {
        return type;
    }

    @Override
    public Optional<Integer> dscp() {
        return dscp;
    }

    @Override
    public Optional<Bandwidth> maxRate() {
        return maxRate;
    }

    @Override
    public Optional<Bandwidth> minRate() {
        return minRate;
    }

    @Override
    public Optional<Long> burst() {
        return burst;
    }

    @Override
    public Optional<Long> priority() {
        return priority;
    }

    @Override
    public int hashCode() {
        return Objects.hash(queueId, type, dscp, maxRate, minRate, burst, priority);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultQueueDescription) {
            final DefaultQueueDescription other = (DefaultQueueDescription) obj;
            return Objects.equals(this.queueId, other.queueId) &&
                    Objects.equals(this.type, other.type) &&
                    Objects.equals(this.dscp, other.dscp) &&
                    Objects.equals(this.maxRate, other.maxRate) &&
                    Objects.equals(this.minRate, other.minRate) &&
                    Objects.equals(this.burst, other.burst) &&
                    Objects.equals(this.priority, other.priority);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", queueId())
                .add("type", type())
                .add("dscp", dscp().orElse(0))
                .add("maxRate", maxRate().orElse(null))
                .add("minRate", minRate().orElse(null))
                .add("burst", burst().orElse(0L))
                .add("priority", priority().orElse(0L))
                .toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements QueueDescription.Builder {

        private QueueId queueId;
        private Optional<Integer> dscp = Optional.empty();
        private EnumSet<Type> type;
        private Optional<Bandwidth> minRate = Optional.empty();
        private Optional<Bandwidth> maxRate = Optional.empty();
        private Optional<Long> burst = Optional.empty();
        private Optional<Long> priority = Optional.empty();

        private Builder() {
        }

        @Override
        public QueueDescription build() {
            return new DefaultQueueDescription(queueId, dscp, type,
                    maxRate, minRate, burst, priority);
        }

        @Override
        public Builder queueId(QueueId queueId) {
            this.queueId = queueId;
            return this;
        }

        @Override
        public Builder dscp(Integer dscp) {
            this.dscp = Optional.ofNullable(dscp);
            return this;
        }

        @Override
        public Builder type(EnumSet<Type> type) {
            this.type = type;
            return this;
        }

        @Override
        public Builder maxRate(Bandwidth maxRate) {
            this.maxRate = Optional.ofNullable(maxRate);
            return this;
        }

        @Override
        public Builder minRate(Bandwidth minRate) {
            this.minRate = Optional.ofNullable(minRate);
            return this;
        }

        @Override
        public Builder burst(Long burst) {
            this.burst = Optional.ofNullable(burst);
            return this;
        }

        @Override
        public Builder priority(Long priority) {
            this.priority = Optional.ofNullable(priority);
            return this;
        }
    }
}
