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
package org.onosproject.net.behaviour.protection;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import javax.annotation.concurrent.Immutable;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

/**
 * State of protected path endpoint.
 */
@Beta
@Immutable
public class ProtectedTransportEndpointState {

    /**
     * Active path is currently unknown.
     */
    public static final int ACTIVE_UNKNOWN = -1;

    /**
     * List of underlying path/flow in priority order.
     */
    private final List<TransportEndpointState> pathStates;

    /**
     * {@link #pathStates} index of the active Path or {@link #ACTIVE_UNKNOWN}.
     */
    private final int activePathIndex;

    // TODO Do we need reference to the config object?
    private final ProtectedTransportEndpointDescription description;


    protected ProtectedTransportEndpointState(ProtectedTransportEndpointDescription description,
                                              List<TransportEndpointState> pathStates,
                                              int activePathIndex) {
        this.description = checkNotNull(description);
        this.pathStates = ImmutableList.copyOf(pathStates);
        this.activePathIndex = activePathIndex;
    }

    /**
     * Returns the description of this ProtectedPathEndPoint.
     *
     * @return the description
     */
    public ProtectedTransportEndpointDescription description() {
        return description;
    }

    /**
     * Returns the {@link TransportEndpointState}s forming the ProtectedPathEndPoint.
     *
     * @return the pathStates
     */
    public List<TransportEndpointState> pathStates() {
        return pathStates;
    }

    /**
     * Returns the index of the working Path in {@link #pathStates()} List or
     * {@link #ACTIVE_UNKNOWN}.
     *
     * @return the activePathIndex
     */
    public int workingPathIndex() {
        return activePathIndex;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("pathState", pathStates)
                .add("activePathIndex", activePathIndex)
                .add("description", description)
                .toString();
    }

    /**
     * Returns {@link ProtectedTransportEndpointState} builder.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<TransportEndpointState> pathStates;
        private int activePathIndex = ACTIVE_UNKNOWN;
        private ProtectedTransportEndpointDescription description;

        /**
         * Copies all the fields from {@code src}.
         *
         * @param src object to copy from
         * @return this
         */
        public Builder copyFrom(ProtectedTransportEndpointState src) {
            this.pathStates = src.pathStates();
            this.activePathIndex = src.workingPathIndex();
            this.description = src.description();
            return this;
        }

        /**
         * Sets the path states.
         *
         * @param pathStates the path states
         * @return this
         */
        public Builder withPathStates(List<TransportEndpointState> pathStates) {
            this.pathStates = pathStates;
            return this;
        }

        /**
         * Sets the activePathIndex.
         *
         * @param activePathIndex the activePathIndex
         * @return this
         */
        public Builder withActivePathIndex(int activePathIndex) {
            this.activePathIndex = activePathIndex;
            return this;
        }

        /**
         * Sets the description.
         *
         * @param description of this {@link ProtectedTransportEndpointState}
         * @return this
         */
        public Builder withDescription(ProtectedTransportEndpointDescription description) {
            this.description = description;
            return this;
        }

        /**
         * Builds {@link ProtectedTransportEndpointState}.
         *
         * @return {@link ProtectedTransportEndpointState}
         */
        public ProtectedTransportEndpointState build() {
            checkNotNull(description, "description field is mandatory");
            checkNotNull(pathStates, "pathStates field is mandatory");
            checkArgument(activePathIndex < pathStates.size(),
                          "Invalid active path index %s > %s",
                          activePathIndex, pathStates.size());
            return new ProtectedTransportEndpointState(description, pathStates, activePathIndex);
        }

    }
}
