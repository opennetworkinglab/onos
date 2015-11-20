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
package org.onosproject.net.flowobjective;

import com.google.common.annotations.Beta;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of a forwarding objective.
 */
@Beta
public final class DefaultForwardingObjective implements ForwardingObjective {

    private final TrafficSelector selector;
    private final Flag flag;
    private final boolean permanent;
    private final int timeout;
    private final ApplicationId appId;
    private final int priority;
    private final Integer nextId;
    private final TrafficTreatment treatment;
    private final Operation op;
    private final Optional<ObjectiveContext> context;

    private final int id;

    private DefaultForwardingObjective(Builder builder) {
        this.selector = builder.selector;
        this.flag = builder.flag;
        this.permanent = builder.permanent;
        this.timeout = builder.timeout;
        this.appId = builder.appId;
        this.priority = builder.priority;
        this.nextId = builder.nextId;
        this.treatment = builder.treatment;
        this.op = builder.op;
        this.context = Optional.ofNullable(builder.context);

        this.id = Objects.hash(selector, flag, permanent,
                timeout, appId, priority, nextId,
                treatment, op);
    }


    @Override
    public TrafficSelector selector() {
        return selector;
    }

    @Override
    public Integer nextId() {
        return nextId;
    }

    @Override
    public TrafficTreatment treatment() {
        return treatment;
    }


    @Override
    public Flag flag() {
        return flag;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public int priority() {
        return priority;
    }

    @Override
    public ApplicationId appId() {
        return appId;
    }

    @Override
    public int timeout() {
        return timeout;
    }

    @Override
    public boolean permanent() {
        return permanent;
    }

    @Override
    public Operation op() {
        return op;
    }

    @Override
    public Optional<ObjectiveContext> context() {
        return context;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(selector, flag, permanent,
                            timeout, appId, priority, nextId,
                            treatment, op);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DefaultForwardingObjective)) {
            return false;
        }
        final DefaultForwardingObjective other = (DefaultForwardingObjective) obj;
        boolean nextEq = false, treatmentEq = false;
        if (this.selector.equals(other.selector) &&
                this.flag == other.flag &&
                this.permanent == other.permanent &&
                this.timeout == other.timeout &&
                this.appId.equals(other.appId) &&
                this.priority == other.priority &&
                this.op == other.op) {
            if (this.nextId != null && other.nextId != null) {
                nextEq = this.nextId == other.nextId;
            }
            if (this.treatment != null && other.treatment != null) {
                treatmentEq = this.treatment.equals(other.treatment);
            }
            if (nextEq && treatmentEq) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns a new builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements ForwardingObjective.Builder {

        private TrafficSelector selector;
        private Flag flag;
        private boolean permanent = DEFAULT_PERMANENT;
        private int timeout = DEFAULT_TIMEOUT;
        private int priority = DEFAULT_PRIORITY;
        private ApplicationId appId;
        private Integer nextId;
        private TrafficTreatment treatment;
        private Operation op;
        private ObjectiveContext context;

        @Override
        public Builder withSelector(TrafficSelector selector) {
            this.selector = selector;
            return this;
        }

        @Override
        public Builder nextStep(int nextId) {
            this.nextId = nextId;
            return this;
        }

        @Override
        public Builder withTreatment(TrafficTreatment treatment) {
            this.treatment = treatment;
            return this;
        }

        @Override
        public Builder withFlag(Flag flag) {
            this.flag = flag;
            return this;
        }

        @Override
        public Builder makeTemporary(int timeout) {
            this.timeout = timeout;
            this.permanent = false;
            return this;
        }

        @Override
        public Builder makePermanent() {
            this.permanent = true;
            return this;
        }

        @Override
        public Builder fromApp(ApplicationId appId) {
            this.appId = appId;
            return this;
        }

        @Override
        public Builder withPriority(int priority) {
            this.priority = priority;
            return this;
        }

        @Override
        public ForwardingObjective add() {
            checkNotNull(selector, "Must have a selector");
            checkNotNull(flag, "A flag must be set");
            checkArgument(nextId != null || treatment != null, "Must supply at " +
                    "least a treatment and/or a nextId");
            checkNotNull(appId, "Must supply an application id");
            op = Operation.ADD;
            return new DefaultForwardingObjective(this);
        }

        @Override
        public ForwardingObjective remove() {
            checkNotNull(selector, "Must have a selector");
            checkNotNull(flag, "A flag must be set");
            checkArgument(nextId != null || treatment != null, "Must supply at " +
                    "least a treatment and/or a nextId");
            checkNotNull(appId, "Must supply an application id");
            op = Operation.REMOVE;
            return new DefaultForwardingObjective(this);
        }

        @Override
        public ForwardingObjective add(ObjectiveContext context) {
            checkNotNull(selector, "Must have a selector");
            checkNotNull(flag, "A flag must be set");
            checkArgument(nextId != null || treatment != null, "Must supply at " +
                    "least a treatment and/or a nextId");
            checkNotNull(appId, "Must supply an application id");
            op = Operation.ADD;
            this.context = context;

            return new DefaultForwardingObjective(this);
        }

        @Override
        public ForwardingObjective remove(ObjectiveContext context) {
            checkNotNull(selector, "Must have a selector");
            checkNotNull(flag, "A flag must be set");
            checkArgument(nextId != null || treatment != null, "Must supply at " +
                    "least a treatment and/or a nextId");
            checkNotNull(appId, "Must supply an application id");
            op = Operation.REMOVE;
            this.context = context;

            return new DefaultForwardingObjective(this);
        }
    }
}
