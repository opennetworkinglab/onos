/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.bmv2.api.runtime;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import org.onlab.util.ImmutableByteSequence;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * An action of a BMv2 match-action table entry.
 */
@Beta
public final class Bmv2Action {

    private final String name;
    private final List<ImmutableByteSequence> parameters;

    protected Bmv2Action(String name, List<ImmutableByteSequence> parameters) {
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
     * Return the name of this action.
     *
     * @return action name
     */
    public final String name() {
        return name;
    }

    /**
     * Returns an immutable view of the list of parameters of this action.
     *
     * @return list of byte sequence
     */
    public final List<ImmutableByteSequence> parameters() {
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
     * A BMv2 action builder.
     */
    public static final class Builder {

        private String name = null;
        private List<ImmutableByteSequence> parameters;

        private Builder() {
            this.parameters = Lists.newArrayList();
        }

        /**
         * Sets the action name.
         *
         * @param actionName a string value
         * @return this
         */
        public Builder withName(String actionName) {
            this.name = checkNotNull(actionName);
            return this;
        }

        /**
         * Adds a parameter at the end of the parameters list.
         *
         * @param parameter a ByteBuffer value
         * @return this
         */
        public Builder addParameter(ImmutableByteSequence parameter) {
            parameters.add(checkNotNull(parameter));
            return this;
        }

        /**
         * Builds a BMv2 action object.
         *
         * @return a BMv2 action
         */
        public Bmv2Action build() {
            checkState(name != null, "action name not set");
            return new Bmv2Action(name, parameters);
        }
    }
}
