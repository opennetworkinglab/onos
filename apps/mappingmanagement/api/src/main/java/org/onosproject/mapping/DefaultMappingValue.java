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
package org.onosproject.mapping;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onosproject.mapping.actions.MappingAction;
import org.onosproject.mapping.actions.MappingActions;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default mapping value implementation.
 */
public final class DefaultMappingValue implements MappingValue {

    private final MappingAction action;
    private final List<MappingTreatment> treatments;

    /**
     * Creates a new mapping value from the specified mapping action and a
     * list of mapping treatments.
     *
     * @param action     mapping action
     * @param treatments a collection of mapping treatment
     */
    private DefaultMappingValue(MappingAction action,
                                List<MappingTreatment> treatments) {
        this.action = action;
        this.treatments = ImmutableList.copyOf(checkNotNull(treatments));
    }

    @Override
    public MappingAction action() {
        return action;
    }

    @Override
    public List<MappingTreatment> treatments() {
        return ImmutableList.copyOf(treatments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(action, treatments);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultMappingValue) {
            DefaultMappingValue that = (DefaultMappingValue) obj;
            return Objects.equals(action, that.action) &&
                    Objects.equals(treatments, that.treatments);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("action", action)
                .add("treatments", treatments)
                .toString();
    }

    /**
     * Returns a new mapping value builder.
     *
     * @return mapping value builder
     */
    public static MappingValue.Builder builder() {
        return new DefaultMappingValue.Builder();
    }

    /**
     * Returns a new mapping value builder primed to produce entities
     * patterned after the supplied mapping value.
     *
     * @param value base mapping value
     * @return mapping value builder
     */
    public static MappingValue.Builder builder(MappingValue value) {
        return new Builder(value);
    }

    /**
     * Builds a mapping value.
     */
    public static final class Builder implements MappingValue.Builder {

        private MappingAction action;
        private List<MappingTreatment> treatments = Lists.newArrayList();

        // creates a new builder
        private Builder() {
        }

        // creates a new builder based off an existing mapping value
        private Builder(MappingValue value) {
            value.treatments().forEach(t -> treatments.add(t));
            action = value.action();
        }

        @Override
        public Builder withAction(MappingAction action) {
            this.action = action;
            return this;
        }

        @Override
        public Builder add(MappingTreatment treatment) {
            treatments.add(treatment);
            return this;
        }

        @Override
        public MappingValue build() {

            // if no action has been specified, we simply assign noAction
            if (action == null) {
                action = MappingActions.noAction();
            }

            // FIXME: we will check the number of treatment later
            // checkArgument(treatments.size() >= 1,
            //        "Must specify more than one mapping treatment");

            return new DefaultMappingValue(action, treatments);
        }
    }
}
