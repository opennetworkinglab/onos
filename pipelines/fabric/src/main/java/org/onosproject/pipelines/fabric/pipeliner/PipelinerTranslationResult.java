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

package org.onosproject.pipelines.fabric.pipeliner;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.group.GroupDescription;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Translation results from fabric pipeliner.
 */
public final class PipelinerTranslationResult {
    private Collection<FlowRule> flowRules;
    private Collection<GroupDescription> groups;
    private ObjectiveError error;

    private PipelinerTranslationResult(Collection<FlowRule> flowRules,
                                      Collection<GroupDescription> groups,
                                      ObjectiveError error) {
        this.flowRules = flowRules;
        this.groups = groups;
        this.error = error;
    }

    /**
     * Gets flow rules from result.
     *
     * @return flow rules
     */
    public Collection<FlowRule> flowRules() {
        return flowRules;
    }

    /**
     * Gets groups from result.
     *
     * @return groups
     */
    public Collection<GroupDescription> groups() {
        return groups;
    }

    /**
     * Gets error from result.
     *
     * @return error of the result; empty if there is no error
     */
    public Optional<ObjectiveError> error() {
        return Optional.ofNullable(error);
    }

    /**
     * Creates a new builder.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
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
        final PipelinerTranslationResult other = (PipelinerTranslationResult) obj;
        return Objects.equals(this.flowRules, other.flowRules)
                && Objects.equals(this.groups, other.groups)
                && Objects.equals(this.error, other.error);
    }

    /**
     * Builder for PipelinerTranslationResult.
     */
    public static final class Builder {
        private ImmutableList.Builder<FlowRule> flowRules = ImmutableList.builder();
        private ImmutableList.Builder<GroupDescription> groups = ImmutableList.builder();
        private ObjectiveError error = null;

        // Hide default constructor
        private Builder() {
        }

        /**
         * Adds flow rule to the result.
         *
         * @param flowRule the flow rule
         */
        public void addFlowRule(FlowRule flowRule) {
            flowRules.add(flowRule);
        }

        /**
         * Adds group to the result.
         *
         * @param group the group
         */
        public void addGroup(GroupDescription group) {
            groups.add(group);
        }

        /**
         * Sets objective error to the result.
         *
         * @param error the error
         */
        public void setError(ObjectiveError error) {
            this.error = error;
        }

        public PipelinerTranslationResult build() {
            return new PipelinerTranslationResult(flowRules.build(),
                                                  groups.build(),
                                                  error);
        }
    }
}
