/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net;

import org.onosproject.net.provider.ProviderId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.net.Link.State.ACTIVE;
import static org.onosproject.net.DefaultAnnotations.EMPTY;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default infrastructure link model implementation.
 */
public class DefaultLink extends AbstractProjectableModel implements Link {

    private final ConnectPoint src;
    private final ConnectPoint dst;
    private final Type type;
    private final State state;
    private final boolean isExpected;

    /**
     * Creates an infrastructure link using the supplied information.
     *
     * @param providerId  provider identity
     * @param src         link source
     * @param dst         link destination
     * @param type        link type
     * @param state       link state
     * @param annotations optional key/value annotations
     */
    protected DefaultLink(ProviderId providerId, ConnectPoint src, ConnectPoint dst,
                       Type type, State state, Annotations... annotations) {
        this(providerId, src, dst, type, state, false, annotations);
    }

    /**
     * Creates an infrastructure link using the supplied information.
     * Links marked as durable will remain in the inventory when a vanish
     * message is received and instead will be marked as inactive.
     *
     * @param providerId  provider identity
     * @param src         link source
     * @param dst         link destination
     * @param type        link type
     * @param state       link state
     * @param isExpected  indicates if the link is preconfigured
     * @param annotations optional key/value annotations
     */
    private DefaultLink(ProviderId providerId, ConnectPoint src, ConnectPoint dst,
                           Type type, State state,
                           boolean isExpected, Annotations... annotations) {
        super(providerId, annotations);
        this.src = src;
        this.dst = dst;
        this.type = type;
        this.state = state;
        this.isExpected = isExpected;
    }

    @Override
    public ConnectPoint src() {
        return src;
    }

    @Override
    public ConnectPoint dst() {
        return dst;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public State state() {
        return state;
    }

    @Override
    public boolean isExpected() {
        return isExpected;
    }

    // Note: Durability & state are purposefully omitted form equality & hashCode.

    @Override
    public int hashCode() {
        return Objects.hash(src, dst, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultLink) {
            final DefaultLink other = (DefaultLink) obj;
            return Objects.equals(this.src, other.src) &&
                    Objects.equals(this.dst, other.dst) &&
                    Objects.equals(this.type, other.type) &&
                    Objects.equals(this.isExpected, other.isExpected);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("src", src)
                .add("dst", dst)
                .add("type", type)
                .add("state", state)
                .add("expected", isExpected)
                .toString();
    }

    /**
     * Creates a new default link builder.
     *
     * @return default link builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for DefaultLink objects.
     */
    public static class Builder {
        private ProviderId providerId;
        private Annotations annotations = EMPTY;
        private ConnectPoint src;
        private ConnectPoint dst;
        private Type type;
        private State state = ACTIVE;
        private boolean isExpected = false;

        protected Builder() {
            // Hide constructor
        }

        /**
         * Sets the providerId to be used by the builder.
         *
         * @param providerId new provider id
         * @return self
         */
        public Builder providerId(ProviderId providerId) {
            this.providerId = providerId;
            return this;
        }

        /**
         * Sets the annotations to be used by the builder.
         *
         * @param annotations new annotations
         * @return self
         */
        public Builder annotations(Annotations annotations) {
            this.annotations = annotations;
            return this;
        }

        /**
         * Sets the source connect point to be used by the builder.
         *
         * @param src source connect point
         * @return self
         */
        public Builder src(ConnectPoint src) {
            this.src = src;
            return this;
        }

        /**
         * Sets the destination connect point to be used by the builder.
         *
         * @param dst new destination connect point
         * @return self
         */
        public Builder dst(ConnectPoint dst) {
            this.dst = dst;
            return this;
        }

        /**
         * Sets the link type to be used by the builder.
         *
         * @param type new link type
         * @return self
         */
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        /**
         * Sets the link state to be used by the builder.
         *
         * @param state new link state
         * @return self
         */
        public Builder state(State state) {
            this.state = state;
            return this;
        }

        /**
         * Sets the expected flag to be used by the builder.
         *
         * @param isExpected new expected flag
         * @return self
         */
        public Builder isExpected(boolean isExpected) {
            this.isExpected = isExpected;
            return this;
        }

        /**
         * Builds a default link object from the accumulated parameters.
         *
         * @return default link object
         */
        public DefaultLink build() {
            checkNotNull(src, "Source connect point cannot be null");
            checkNotNull(dst, "Destination connect point cannot be null");
            checkNotNull(type, "Type cannot be null");
            checkNotNull(providerId, "Provider Id cannot be null");

            return new DefaultLink(providerId, src, dst,
                                   type, state,
                                   isExpected, annotations);
        }

    }



}
