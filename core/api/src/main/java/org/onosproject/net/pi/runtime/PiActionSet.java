/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.net.pi.runtime;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Instance of an action set of a protocol-independent pipeline used
 * when doing one-shot action selector programming. Contains a set of weighted
 * actions, and it is equivalent to the action profile action set from P4Runtime
 * specifications.
 */
@Beta
public final class PiActionSet implements PiTableAction {

    private final Set<WeightedAction> actionSet;

    private PiActionSet(Set<WeightedAction> actionSet) {
        this.actionSet = actionSet;
    }

    /**
     * Returns the set of actions.
     *
     * @return the set of actions
     */
    public Set<WeightedAction> actions() {
        return actionSet;
    }

    @Override
    public Type type() {
        return Type.ACTION_SET;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiActionSet that = (PiActionSet) o;
        return Objects.equal(actionSet, that.actionSet);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(actionSet);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("actionSet", actionSet)
                .toString();
    }

    /**
     * Returns a new builder of an action set.
     *
     * @return action set builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of an action set.
     */
    public static final class Builder {

        private final Set<WeightedAction> actionSet = Sets.newHashSet();

        private Builder() {
            // hides constructor.
        }

        /**
         * Adds a weighted action to this action set.
         *
         * @param action The action to add
         * @param weight The weight associated to the action
         * @return this
         */
        public Builder addWeightedAction(
                PiAction action, int weight) {
            actionSet.add(new WeightedAction(action, weight));
            return this;
        }

        /**
         * Creates a new action profile action set.
         *
         * @return action profile action set
         */
        public PiActionSet build() {
            return new PiActionSet(Set.copyOf(actionSet));
        }
    }

    /**
     * Weighted action used in an actions set.
     */
    public static final class WeightedAction {
        public static final int DEFAULT_WEIGHT = 1;

        private final int weight;
        private final PiAction action;

        /**
         * Creates a new weighted action instance that can be used in an action
         * set, from the given PI action and weight.
         *
         * @param action the action
         * @param weight the weigh
         */
        public WeightedAction(PiAction action, int weight) {
            this.weight = weight;
            this.action = action;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            WeightedAction that = (WeightedAction) o;
            return weight == that.weight &&
                    Objects.equal(action, that.action);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(weight, action);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("weight", weight)
                    .add("action", action)
                    .toString();
        }

        public int weight() {
            return weight;
        }

        public PiAction action() {
            return action;
        }
    }
}


