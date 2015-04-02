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

import com.google.common.collect.ImmutableList;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.flow.criteria.Criterion;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of a filtering objective.
 */
public final class DefaultFilteringObjective implements FilteringObjective {


    private final Criterion key;
    private final boolean permanent;
    private final int timeout;
    private final ApplicationId appId;
    private final int priority;
    private final List<Criterion> conditions;
    private final int id;
    private final Operation op;

    private DefaultFilteringObjective(Criterion key, boolean permanent, int timeout,
                                      ApplicationId appId, int priority,
                                      List<Criterion> conditions, Operation op) {
        this.key = key;
        this.permanent = permanent;
        this.timeout = timeout;
        this.appId = appId;
        this.priority = priority;
        this.conditions = conditions;
        this.op = op;

        this.id = Objects.hash(key, conditions, permanent,
                               timeout, appId, priority);
    }


    @Override
    public Criterion key() {
        return key;
    }

    @Override
    public Collection<Criterion> conditions() {
        return conditions;
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

    /**
     * Returns a new builder.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }


    public static final class Builder implements FilteringObjective.Builder {
        private final ImmutableList.Builder<Criterion> listBuilder
                = ImmutableList.builder();

        private Criterion key;
        private boolean permanent = DEFAULT_PERMANENT;
        private int timeout = DEFAULT_TIMEOUT;
        private ApplicationId appId;
        private int priority = DEFAULT_PRIORITY;

        @Override
        public Builder addCondition(Criterion criterion) {
            listBuilder.add(criterion);
            return this;
        }

        @Override
        public Builder withKey(Criterion criterion) {
            key = criterion;
            return this;
        }

        @Override
        public Builder makeTemporary(int timeout) {
            this.timeout = timeout;
            permanent = false;
            return this;
        }

        @Override
        public Builder makePermanent() {
            permanent = true;
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
        public FilteringObjective add() {
            List<Criterion> conditions = listBuilder.build();
            checkNotNull(key, "Must have a key.");
            checkArgument(!conditions.isEmpty(), "Must have at least one condition.");
            checkNotNull(appId, "Must supply an application id");

            return new DefaultFilteringObjective(key, permanent, timeout,
                                                appId, priority, conditions,
                                                Operation.ADD);

        }

        @Override
        public FilteringObjective remove() {
            List<Criterion> conditions = listBuilder.build();
            checkNotNull(key, "Must have a key.");
            checkArgument(!conditions.isEmpty(), "Must have at least one condition.");
            checkNotNull(appId, "Must supply an application id");

            return new DefaultFilteringObjective(key, permanent, timeout,
                                                 appId, priority, conditions,
                                                 Operation.REMOVE);

        }


    }

}
