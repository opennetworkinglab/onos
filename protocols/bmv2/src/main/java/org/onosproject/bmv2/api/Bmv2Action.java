/*
 * Copyright 2014-2016 Open Networking Laboratory
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

package org.onosproject.bmv2.api;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Bmv2 Action representation.
 */
public final class Bmv2Action {

    private final String name;
    private final List<ByteBuffer> parameters;

    private Bmv2Action(String name, List<ByteBuffer> parameters) {
        // hide constructor
        this.name = name;
        this.parameters = parameters;
    }

    /**
     * Returns a new action builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get action name.
     *
     * @return action name
     */
    public final String name() {
        return name;
    }

    /**
     * Get list of action parameters ordered as per P4 action definition.
     *
     * @return List of action parameters
     */
    public final List<ByteBuffer> parameters() {
        return Collections.unmodifiableList(parameters);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(name, parameters);
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2Action other = (Bmv2Action) obj;
        return Objects.equals(this.name, other.name)
                && Objects.equals(this.parameters, other.parameters);
    }

    @Override
    public final String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("parameters", parameters)
                .toString();
    }

    /**
     * A Bmv2 action builder.
     */
    public static final class Builder {

        private String name;
        private List<ByteBuffer> parameters;

        private Builder() {
            this.parameters = Lists.newArrayList();
        }

        /**
         * Set the action name.
         *
         * @param actionName a string value
         * @return this
         */
        public Builder withName(String actionName) {
            this.name = actionName;
            return this;
        }

        /**
         * Add a parameter at the end of the parameters list.
         *
         * @param parameter a ByteBuffer value
         * @return this
         */
        public Builder addParameter(ByteBuffer parameter) {
            parameters.add(parameter);
            return this;
        }

        /**
         * Builds a Bmv2 action object.
         *
         * @return a Bmv2 action
         */
        public Bmv2Action build() {
            checkNotNull(name, "Action name not set");
            return new Bmv2Action(name, parameters);
        }
    }
}
