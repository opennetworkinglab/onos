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

package org.onosproject.pipelines.fabric.impl.behaviour.pipeliner;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.GroupDescription;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Result of a pipeliner translation from an objective to flows and groups.
 */
final class ObjectiveTranslation {

    private final ImmutableMap<FlowId, FlowRule> flowRules;
    private final ImmutableMap<Integer, GroupDescription> groups;
    private final ObjectiveError error;

    private ObjectiveTranslation(Map<FlowId, FlowRule> flowRules,
                                 Map<Integer, GroupDescription> groups,
                                 ObjectiveError error) {
        this.flowRules = ImmutableMap.copyOf(flowRules);
        this.groups = ImmutableMap.copyOf(groups);
        this.error = error;
    }

    /**
     * Returns flow rules of this translation.
     *
     * @return flow rules
     */
    Collection<FlowRule> flowRules() {
        return flowRules.values();
    }

    /**
     * Returns groups of this translation.
     *
     * @return groups
     */
    Collection<GroupDescription> groups() {
        return groups.values();
    }

    /**
     * Returns the error of this translation, is any.
     *
     * @return optional error
     */
    Optional<ObjectiveError> error() {
        return Optional.ofNullable(error);
    }

    /**
     * Creates a new builder.
     *
     * @return the builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a new translation that signals the given error.
     *
     * @param error objective error
     * @return new objective translation
     */
    static ObjectiveTranslation ofError(ObjectiveError error) {
        checkNotNull(error);
        return new ObjectiveTranslation(
                Collections.emptyMap(), Collections.emptyMap(), error);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("flowRules", flowRules)
                .add("groups", groups)
                .add("error", error)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(flowRules, groups, error);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ObjectiveTranslation other = (ObjectiveTranslation) obj;
        return flowRulesExactMatch(other.flowRules)
                && Objects.equals(this.groups, other.groups)
                && Objects.equals(this.error, other.error);
    }

    private boolean flowRulesExactMatch(Map<FlowId, FlowRule> otherFlowRules) {
        if (otherFlowRules == null || otherFlowRules.size() != this.flowRules.size()) {
            return false;
        }
        return this.flowRules.values().stream()
                .allMatch(f -> otherFlowRules.containsKey(f.id())
                        && otherFlowRules.get(f.id()).exactMatch(f));
    }

    /**
     * Builder for ObjectiveTranslation. This implementation checks that flow
     * and groups are not added when an existing one with same ID (FlowId or
     * GroupId) has already been added.
     */
    static final class Builder {

        private final Map<FlowId, FlowRule> flowRules = Maps.newHashMap();
        private final Map<Integer, GroupDescription> groups = Maps.newHashMap();

        // Hide default constructor
        private Builder() {
        }

        /**
         * Adds a flow rule to this translation.
         *
         * @param flowRule flow rule
         * @return this
         * @throws FabricPipelinerException if a FlowRule with same FlowId
         *                                  already exists in this translation
         */
        Builder addFlowRule(FlowRule flowRule)
                throws FabricPipelinerException {
            checkNotNull(flowRule);
            if (flowRules.containsKey(flowRule.id())) {
                final FlowRule existingFlowRule = flowRules.get(flowRule.id());
                if (!existingFlowRule.exactMatch(flowRule)) {
                    throw new FabricPipelinerException(format(
                            "Another FlowRule with same ID has already been " +
                                    "added to this translation: existing=%s, new=%s",
                            existingFlowRule, flowRule));
                }
            }
            flowRules.put(flowRule.id(), flowRule);
            return this;
        }

        /**
         * Adds group to this translation.
         *
         * @param group group
         * @return this
         * @throws FabricPipelinerException if a FlowRule with same GroupId
         *                                  already exists in this translation
         */
        Builder addGroup(GroupDescription group)
                throws FabricPipelinerException {
            checkNotNull(group);
            if (groups.containsKey(group.givenGroupId())) {
                final GroupDescription existingGroup = groups.get(group.givenGroupId());
                if (!existingGroup.equals(group)) {
                    throw new FabricPipelinerException(format(
                            "Another Group with same ID has already been " +
                                    "added to this translation: existing=%s, new=%s",
                            existingGroup, group));
                }
            }
            groups.put(group.givenGroupId(), group);
            return this;
        }

        /**
         * Creates ane translation.
         *
         * @return translation instance
         */
        ObjectiveTranslation build() {
            return new ObjectiveTranslation(flowRules, groups, null);
        }
    }
}
