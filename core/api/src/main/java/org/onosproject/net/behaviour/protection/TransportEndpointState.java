/*
 * Copyright 2016-present Open Networking Foundation
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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.concurrent.Immutable;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

/**
 * State of a underlying path endpoint, forming protected path.
 */
@Beta
@Immutable
public class TransportEndpointState {

    /**
     *  ID assigned by the implementation.
     */
    private final TransportEndpointId id;

    // meant to show liveness state by OAM probe, etc.
    private final boolean live;

    // TODO do we need opaque config here? ⇨ May be. Comments?
    @Beta
    private final Map<String, String> attributes;

    // TODO Do we need reference to the config object?
    private final TransportEndpointDescription description;

    protected TransportEndpointState(TransportEndpointDescription description,
                                     TransportEndpointId id,
                                     boolean live,
                                     Map<String, String> attributes) {
        this.id = checkNotNull(id);
        this.live = live;
        this.description = checkNotNull(description);
        this.attributes = ImmutableMap.copyOf(attributes);
    }

    /**
     * Returns {@link TransportEndpointId}.
     *
     * @return identifier
     */
    public TransportEndpointId id() {
        return id;
    }

    /**
     * Returns liveness state of this endpoint.
     *
     * @return true if this endpoint is live.
     */
    public boolean isLive() {
        return live;
    }

    /**
     * Returns description associated to this state.
     *
     * @return the description
     */
    public TransportEndpointDescription description() {
        return description;
    }

    /**
     * Returns implementation defined attributes.
     *
     * @return the attributes
     */
    @Beta
    public Map<String, String> attributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("live", live)
                .add("description", description)
                .add("attributes", attributes)
                .toString();
    }

    /**
     * Returns {@link TransportEndpointState} builder.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private TransportEndpointId id;
        private boolean live;
        private Map<String, String> attributes = new HashMap<>();
        private TransportEndpointDescription description;

        /**
         * Copies all the fields from {@code src}.
         *
         * @param src object to copy from
         * @return this
         */
        public Builder copyFrom(TransportEndpointState src) {
            this.id = src.id();
            this.live = src.isLive();
            this.attributes.putAll(src.attributes());
            this.description = src.description();
            return this;
        }

        /**
         * Sets id.
         *
         * @param id {@link TransportEndpointId}
         * @return this
         */
        public Builder withId(TransportEndpointId id) {
            this.id = checkNotNull(id);
            return this;
        }

        /**
         * Sets liveness state.
         *
         * @param live liveness state
         * @return this
         */
        public Builder withLive(boolean live) {
            this.live = live;
            return this;
        }

        /**
         * Sets the description.
         *
         * @param description description
         * @return this
         */
        public Builder withDescription(TransportEndpointDescription description) {
            this.description = checkNotNull(description);
            return this;
        }

        /**
         * Adds specified attributes.
         *
         * @param attributes to add
         * @return this
         */
        public Builder addAttributes(Map<String, String> attributes) {
            this.attributes.putAll(attributes);
            return this;
        }

        /**
         * Replaces attributes with the specified Map.
         *
         * @param attributes to add
         * @return this
         */
        public Builder replaceAttributes(Map<String, String> attributes) {
            this.attributes.clear();
            return addAttributes(attributes);
        }

        /**
         * Builds {@link TransportEndpointState}.
         *
         * @return {@link TransportEndpointState}
         */
        public TransportEndpointState build() {
            checkNotNull(id, "id field is mandatory");
            checkNotNull(description, "description field is mandatory");
            return new TransportEndpointState(description, id, live, attributes);
        }

    }

}
