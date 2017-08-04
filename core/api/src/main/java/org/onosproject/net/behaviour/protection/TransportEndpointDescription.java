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

import javax.annotation.concurrent.Immutable;

import org.onosproject.net.FilteredConnectPoint;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;

/**
 * Configuration for a underlying path endpoint, forming protected path.
 */
@Beta
@Immutable
public class TransportEndpointDescription {

    // TODO is there a better field name for this?
    // Only VLAN selector expected in practice for now.
    private final FilteredConnectPoint output;

    /**
     * True if this endpoint is administratively enabled, false otherwise.
     */
    private final boolean enabled;

    // Do we need opaque config per path/flow?
    // ⇨ No it should probably be expressed as org.onosproject.net.config.Config

    protected TransportEndpointDescription(FilteredConnectPoint output,
                                           boolean enabled) {
        this.output = checkNotNull(output);
        this.enabled = enabled;
    }

    /**
     * @return the output
     */
    public FilteredConnectPoint output() {
        return output;
    }

    /**
     * Returns administrative state.
     *
     * @return the enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("output", output)
                .add("enabled", enabled)
                .toString();
    }


    /**
     * Returns {@link TransportEndpointDescription} builder.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private FilteredConnectPoint output;
        private boolean enabled = true;

        /**
         * Copies all the fields from {@code src}.
         *
         * @param src object to copy from
         * @return this
         */
        public Builder copyFrom(TransportEndpointDescription src) {
            this.output = src.output();
            this.enabled = src.isEnabled();
            return this;
        }

        /**
         * Sets output configuration.
         *
         * @param output configuration to set
         * @return this
         */
        public Builder withOutput(FilteredConnectPoint output) {
            this.output = output;
            return this;
        }

        /**
         * Sets enabled state.
         *
         * @param enabled state to set
         * @return this
         */
        public Builder withEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        /**
         * Builds {@link TransportEndpointDescription}.
         *
         * @return {@link TransportEndpointDescription}
         */
        public TransportEndpointDescription build() {
            checkNotNull(output, "output field is mandatory");
            return new TransportEndpointDescription(output, enabled);
        }
    }
}
