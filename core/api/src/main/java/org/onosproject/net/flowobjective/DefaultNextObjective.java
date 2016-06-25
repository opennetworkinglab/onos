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
package org.onosproject.net.flowobjective;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of a next objective.
 */
@Beta
public final class DefaultNextObjective implements NextObjective {

    private final List<TrafficTreatment> treatments;
    private final ApplicationId appId;
    private final Type type;
    private final Integer id;
    private final Operation op;
    private final Optional<ObjectiveContext> context;
    private final TrafficSelector meta;

    private DefaultNextObjective(Builder builder) {
        this.treatments = builder.treatments;
        this.appId = builder.appId;
        this.type = builder.type;
        this.id = builder.id;
        this.op = builder.op;
        this.context = Optional.ofNullable(builder.context);
        this.meta = builder.meta;
    }

    @Override
    public Collection<TrafficTreatment> next() {
        return treatments;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public ApplicationId appId() {
        return appId;
    }

    @Override
    public int timeout() {
        return 0;
    }

    @Override
    public boolean permanent() {
        return false;
    }

    @Override
    public Operation op() {
        return op;
    }

    @Override
    public Optional<ObjectiveContext> context() {
        return context;
    }

    @Override
    public TrafficSelector meta() {
        return meta;
    }

    @Override
    public int hashCode() {
        return Objects.hash(treatments, appId, type, id, op, meta);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultNextObjective) {
            final DefaultNextObjective other = (DefaultNextObjective) obj;
            return Objects.equals(this.treatments, other.treatments)
                    && Objects.equals(this.appId, other.appId)
                    && Objects.equals(this.type, other.type)
                    && Objects.equals(this.id, other.id)
                    && Objects.equals(this.op, other.op)
                    && Objects.equals(this.meta, other.meta);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id())
                .add("type", type())
                .add("op", op())
                .add("priority", priority())
                .add("next", next())
                .add("meta", meta())
                .add("appId", appId())
                .add("permanent", permanent())
                .add("timeout", timeout())
                .toString();
    }

    /**
     * Returns a new builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder copy() {
        return new Builder(this);
    }

    public static final class Builder implements NextObjective.Builder {

        private ApplicationId appId;
        private Type type;
        private Integer id;
        private List<TrafficTreatment> treatments;
        private Operation op;
        private ObjectiveContext context;
        private TrafficSelector meta;

        private final ImmutableList.Builder<TrafficTreatment> listBuilder
                = ImmutableList.builder();

        // Creates an empty builder
        private Builder() {
        }

        // Creates a builder set to create a copy of the specified objective.
        private Builder(NextObjective objective) {
            this.type = objective.type();
            this.id = objective.id();
            this.treatments = ImmutableList.copyOf(objective.next());
            this.listBuilder.addAll(objective.next());
            this.meta = objective.meta();
            this.appId = objective.appId();
            this.op = objective.op();
        }

        @Override
        public Builder withId(int nextId) {
            this.id = nextId;
            return this;
        }

        @Override
        public Builder withType(Type type) {
            this.type = type;
            return this;
        }

        @Override
        public Builder addTreatment(TrafficTreatment treatment) {
            listBuilder.add(treatment);
            return this;
        }

        /**
         * Noop. This method has no effect.
         *
         * @param timeout a timeout
         * @return a next objective builder
         */
        @Override
        public Builder makeTemporary(int timeout) {
            return this;
        }

        /**
         * Noop. This method has no effect.
         *
         * @return a next objective builder
         */
        @Override
        public Builder makePermanent() {
            return this;
        }

        @Override
        public Builder fromApp(ApplicationId appId) {
            this.appId = appId;
            return this;
        }

        /**
         * Noop. This method has no effect.
         *
         * @param priority an integer
         * @return a next objective builder
         */
        @Override
        public Builder withPriority(int priority) {
            return this;
        }

        @Override
        public Builder withMeta(TrafficSelector meta) {
            this.meta = meta;
            return this;
        }

        @Override
        public NextObjective add() {
            treatments = listBuilder.build();
            op = Operation.ADD;
            checkNotNull(appId, "Must supply an application id");
            checkNotNull(id, "id cannot be null");
            checkNotNull(type, "The type cannot be null");
            checkArgument(!treatments.isEmpty(), "Must have at least one treatment");

            return new DefaultNextObjective(this);
        }

        @Override
        public NextObjective remove() {
            treatments = listBuilder.build();
            op = Operation.REMOVE;
            checkNotNull(appId, "Must supply an application id");
            checkNotNull(id, "id cannot be null");
            checkNotNull(type, "The type cannot be null");

            return new DefaultNextObjective(this);
        }

        @Override
        public NextObjective add(ObjectiveContext context) {
            treatments = listBuilder.build();
            op = Operation.ADD;
            this.context = context;
            checkNotNull(appId, "Must supply an application id");
            checkNotNull(id, "id cannot be null");
            checkNotNull(type, "The type cannot be null");
            checkArgument(!treatments.isEmpty(), "Must have at least one treatment");

            return new DefaultNextObjective(this);
        }

        @Override
        public NextObjective remove(ObjectiveContext context) {
            treatments = listBuilder.build();
            op = Operation.REMOVE;
            this.context = context;
            checkNotNull(appId, "Must supply an application id");
            checkNotNull(id, "id cannot be null");
            checkNotNull(type, "The type cannot be null");

            return new DefaultNextObjective(this);
        }

        @Override
        public NextObjective addToExisting() {
            treatments = listBuilder.build();
            op = Operation.ADD_TO_EXISTING;
            checkNotNull(appId, "Must supply an application id");
            checkNotNull(id, "id cannot be null");
            checkNotNull(type, "The type cannot be null");
            checkArgument(!treatments.isEmpty(), "Must have at least one treatment");

            return new DefaultNextObjective(this);
        }

        @Override
        public NextObjective removeFromExisting() {
            treatments = listBuilder.build();
            op = Operation.REMOVE_FROM_EXISTING;
            checkNotNull(appId, "Must supply an application id");
            checkNotNull(id, "id cannot be null");
            checkNotNull(type, "The type cannot be null");

            return new DefaultNextObjective(this);
        }

        @Override
        public NextObjective addToExisting(ObjectiveContext context) {
            treatments = listBuilder.build();
            op = Operation.ADD_TO_EXISTING;
            this.context = context;
            checkNotNull(appId, "Must supply an application id");
            checkNotNull(id, "id cannot be null");
            checkNotNull(type, "The type cannot be null");
            checkArgument(!treatments.isEmpty(), "Must have at least one treatment");

            return new DefaultNextObjective(this);
        }

        @Override
        public NextObjective removeFromExisting(ObjectiveContext context) {
            treatments = listBuilder.build();
            op = Operation.REMOVE_FROM_EXISTING;
            this.context = context;
            checkNotNull(appId, "Must supply an application id");
            checkNotNull(id, "id cannot be null");
            checkNotNull(type, "The type cannot be null");

            return new DefaultNextObjective(this);
        }

    }

}
