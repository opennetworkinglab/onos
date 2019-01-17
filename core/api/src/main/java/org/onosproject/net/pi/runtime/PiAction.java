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

package org.onosproject.net.pi.runtime;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionParamId;

import java.util.Collection;
import java.util.Map;
import java.util.StringJoiner;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Instance of an action, and its runtime parameters, of a table entry in a
 * protocol-independent pipeline.
 */
@Beta
public final class PiAction implements PiTableAction {

    private final PiActionId actionId;
    private final ImmutableMap<PiActionParamId, PiActionParam> runtimeParams;

    /**
     * Creates a new action instance for the given action identifier and runtime
     * parameters.
     *
     * @param actionId      action identifier
     * @param runtimeParams list of runtime parameters
     */
    private PiAction(PiActionId actionId,
                     Map<PiActionParamId, PiActionParam> runtimeParams) {
        this.actionId = actionId;
        this.runtimeParams = ImmutableMap.copyOf(runtimeParams);
    }

    @Override
    public Type type() {
        return Type.ACTION;
    }

    /**
     * Return the identifier of this action.
     *
     * @return action identifier
     */
    public PiActionId id() {
        return actionId;
    }

    /**
     * Returns all runtime parameters of this action. Return an empty collection
     * if the action doesn't take any runtime parameters.
     *
     * @return list of byte sequences
     */
    public Collection<PiActionParam> parameters() {
        return runtimeParams.values();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiAction piAction = (PiAction) o;
        return Objects.equal(actionId, piAction.actionId) &&
                Objects.equal(runtimeParams, piAction.runtimeParams);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(actionId, runtimeParams);
    }

    @Override
    public String toString() {
        StringJoiner stringParams = new StringJoiner(", ", "(", ")");
        this.parameters().forEach(p -> stringParams.add(p.toString()));
        return this.id().toString() + stringParams.toString();
    }

    /**
     * Returns an action builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of protocol-independent actions.
     */
    public static final class Builder {

        private PiActionId actionId;
        private Map<PiActionParamId, PiActionParam> runtimeParams = Maps.newHashMap();

        private Builder() {
            // hides constructor.
        }

        /**
         * Sets the identifier of this action.
         *
         * @param actionId action identifier
         * @return this
         */
        public Builder withId(PiActionId actionId) {
            this.actionId = actionId;
            return this;
        }

        /**
         * Adds a runtime parameter.
         *
         * @param param action parameter
         * @return this
         */
        public Builder withParameter(PiActionParam param) {
            checkNotNull(param);
            runtimeParams.put(param.id(), param);
            return this;
        }

        /**
         * Adds many runtime parameters.
         *
         * @param params collection of action parameters
         * @return this
         */
        public Builder withParameters(Collection<PiActionParam> params) {
            checkNotNull(params);
            params.forEach(this::withParameter);
            return this;
        }

        /**
         * Returns a new action instance.
         *
         * @return action
         */
        public PiAction build() {
            checkNotNull(actionId);
            return new PiAction(actionId, runtimeParams);
        }
    }
}
