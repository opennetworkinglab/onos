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

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;


/**
 * Default implementation of Qos description entity.
 */
@Beta
public final class DefaultQosDescription extends AbstractDescription
        implements QosDescription {

    private final QosId qosId;
    private final Type type;
    private final Optional<Bandwidth> maxRate;
    private final Optional<Long> cir;
    private final Optional<Long> cbs;
    private final Optional<Map<Long, QueueDescription>> queues;

    private DefaultQosDescription(QosId qosId, Type type, Optional<Bandwidth> maxRate,
                                  Optional<Long> cir, Optional<Long> cbs,
                                  Optional<Map<Long, QueueDescription>> queues,
                                  SparseAnnotations... annotations) {
        super(annotations);
        this.qosId = checkNotNull(qosId);
        this.type = checkNotNull(type);
        this.maxRate = maxRate;
        this.cir = cir;
        this.cbs = cbs;
        this.queues = queues;
    }

    @Override
    public QosId qosId() {
        return qosId;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public Optional<Bandwidth> maxRate() {
        return maxRate;
    }

    @Override
    public Optional<Long> cir() {
        return cir;
    }

    @Override
    public Optional<Long> cbs() {
        return cbs;
    }

    @Override
    public Optional<Map<Long, QueueDescription>> queues() {
        return queues;
    }

    @Override
    public int hashCode() {
        return Objects.hash(qosId, type, maxRate, cir, cbs, queues);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultQosDescription) {
            final DefaultQosDescription other = (DefaultQosDescription) obj;
            return Objects.equals(this.qosId, other.qosId) &&
                    Objects.equals(this.type, other.type) &&
                    Objects.equals(this.maxRate, other.maxRate) &&
                    Objects.equals(this.cir, other.cir) &&
                    Objects.equals(this.cbs, other.cbs) &&
                    Objects.equals(this.queues, other.queues);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("qosId", qosId())
                .add("type", type())
                .add("maxRate", maxRate().orElse(null))
                .add("cir", cir().orElse(0L))
                .add("cbs", cbs().orElse(0L))
                .add("queues", queues().orElse(null))
                .toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements QosDescription.Builder {

        private QosId qosId;
        private Type type;
        private Optional<Bandwidth> maxRate = Optional.empty();
        private Optional<Long> cir = Optional.empty();
        private Optional<Long> cbs = Optional.empty();
        private Optional<Map<Long, QueueDescription>> queues = Optional.empty();

        private Builder() {
        }

        @Override
        public QosDescription build() {
            return new DefaultQosDescription(qosId, type, maxRate, cir, cbs, queues);
        }

        @Override
        public Builder qosId(QosId qosId) {
            this.qosId = qosId;
            return this;
        }

        @Override
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        @Override
        public Builder maxRate(Bandwidth maxRate) {
            this.maxRate = Optional.ofNullable(maxRate);
            return this;
        }

        @Override
        public Builder cir(Long cir) {
            this.cir = Optional.ofNullable(cir);
            return this;
        }

        @Override
        public Builder cbs(Long cbs) {
            this.cbs = Optional.ofNullable(cbs);
            return this;
        }

        @Override
        public Builder queues(Map<Long, QueueDescription> queues) {
            this.queues = Optional.ofNullable(queues);
            return this;
        }
    }
}

