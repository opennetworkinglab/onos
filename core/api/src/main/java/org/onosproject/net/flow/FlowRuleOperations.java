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
package org.onosproject.net.flow;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.flow.FlowRuleOperation.Type.ADD;
import static org.onosproject.net.flow.FlowRuleOperation.Type.REMOVE;
import static org.onosproject.net.flow.FlowRuleOperation.Type.MODIFY;

/**
 * A batch of flow rule operations that are broken into stages.
 * TODO move this up to parent's package
 */
public class FlowRuleOperations {

    private final List<Set<FlowRuleOperation>> stages;
    private final FlowRuleOperationsContext callback;
    private final Integer stripeKey;

    private FlowRuleOperations(List<Set<FlowRuleOperation>> stages,
                               FlowRuleOperationsContext cb,
                               Integer stripeKey) {
        this.stages = stages;
        this.callback = cb;
        this.stripeKey = stripeKey;
    }

    // kryo-constructor
    protected FlowRuleOperations() {
        this.stages = Lists.newArrayList();
        this.callback = null;
        this.stripeKey = null;
    }

    /**
     * Returns the flow rule operations as sets of stages that should be
     * executed sequentially.
     *
     * @return flow rule stages
     */
    public List<Set<FlowRuleOperation>> stages() {
        return stages;
    }

    /**
     * Returns the callback for this batch of operations.
     *
     * @return callback
     */
    public FlowRuleOperationsContext callback() {
        return callback;
    }

    /**
     * Returns the stripe key. Expectation is that FlowRuleOperations with the
     * same key will be executed sequentially in the same order as they are
     * submitted to the FlowRuleService. Operations without a key or with
     * different keys might be executed in parallel.
     *
     * @return the stripe key associated to this or null if not present
     */
    public Optional<Integer> stripeKey() {
        return Optional.ofNullable(stripeKey);
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
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("stages", stages)
                .toString();
    }

    /**
     * A builder for constructing flow rule operations.
     */
    public static final class Builder {

        private final ImmutableList.Builder<Set<FlowRuleOperation>> listBuilder = ImmutableList.builder();
        private ImmutableSet.Builder<FlowRuleOperation> currentStage = ImmutableSet.builder();
        private Integer stripeKey = null;

        // prevent use of the default constructor outside of this file; use the above method
        private Builder() {}

        /**
         * Appends a flow rule add to the current stage.
         *
         * @param flowRule flow rule
         * @return this
         */
        public Builder add(FlowRule flowRule) {
            currentStage.add(new FlowRuleOperation(flowRule, ADD));
            return this;
        }

        /**
         * Appends an existing flow rule to the current stage.
         *
         * @param flowRuleOperation flow rule operation
         * @return this
         */
        public Builder operation(FlowRuleOperation flowRuleOperation) {
            currentStage.add(flowRuleOperation);
            return this;
        }

        /**
         * Appends a flow rule modify to the current stage.
         *
         * @param flowRule flow rule
         * @return this
         */
        public Builder modify(FlowRule flowRule) {
            currentStage.add(new FlowRuleOperation(flowRule, MODIFY));
            return this;
        }

        /**
         * Appends a flow rule remove to the current stage.
         *
         * @param flowRule flow rule
         * @return this
         */
        // FIXME this is confusing, consider renaming
        public Builder remove(FlowRule flowRule) {
            currentStage.add(new FlowRuleOperation(flowRule, REMOVE));
            return this;
        }

        /**
         * Closes the current stage.
         */
        private void closeStage() {
            ImmutableSet<FlowRuleOperation> stage = currentStage.build();
            if (!stage.isEmpty()) {
                listBuilder.add(stage);
            }
        }

        /**
         * Closes the current stage and starts a new one.
         *
         * @return this
         */
        public Builder newStage() {
            closeStage();
            currentStage = ImmutableSet.builder();
            return this;
        }

        /**
         * Provides semantics similar to lock striping. FlowRuleOperations
         * with the same key will be executed sequentially. Operations without
         * a key or with different keys might be executed in parallel.
         * <p>
         * This parameter is useful to correlate different operations with
         * potentially conflicting writes, to guarantee that operations are
         * executed in-order. For example, to handle the case where one operation
         * that removes a flow rule is followed by one that adds the same flow rule.
         * </p>
         *
         * @param stripeKey an integer key
         * @return this
         */
        public Builder striped(int stripeKey) {
            this.stripeKey = stripeKey;
            return this;
        }

        /**
         * Builds the immutable flow rule operations.
         *
         * @return flow rule operations
         */
        public FlowRuleOperations build() {
            return build(NullFlowRuleOperationsContext.getInstance());
        }

        /**
         * Builds the immutable flow rule operations.
         *
         * @param cb the callback to call when this operation completes
         * @return flow rule operations
         */
        public FlowRuleOperations build(FlowRuleOperationsContext cb) {
            checkNotNull(cb);

            closeStage();
            return new FlowRuleOperations(listBuilder.build(), cb, stripeKey);
        }
    }
}
