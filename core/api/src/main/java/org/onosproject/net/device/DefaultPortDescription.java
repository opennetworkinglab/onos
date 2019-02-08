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
package org.onosproject.net.device;

import com.google.common.base.MoreObjects;
import org.onosproject.net.AbstractDescription;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.Port.Type;
import com.google.common.base.Objects;

/**
 * Default implementation of immutable port description.
 */
public class DefaultPortDescription extends AbstractDescription
        implements PortDescription {

    private static final long DEFAULT_SPEED = 1_000;

    private final PortNumber number;
    private final boolean isEnabled;
    private final boolean isRemoved;
    private final Type type;
    private final long portSpeed;

    /**
     * Creates a port description using the supplied information.
     *
     * @param number      port number
     * @param isEnabled   port enabled state
     * @param isRemoved   port removed state
     * @param type        port type
     * @param portSpeed   port speed in Mbps
     * @param annotations optional key/value annotations map
     */
    private DefaultPortDescription(PortNumber number, boolean isEnabled, boolean isRemoved,
                                  Type type, long portSpeed,
                                  SparseAnnotations...annotations) {
        super(annotations);
        this.number = checkNotNull(number);
        this.isEnabled = isEnabled;
        this.isRemoved = isRemoved;
        this.type = type;
        this.portSpeed = portSpeed;
    }

    // Default constructor for serialization
    protected DefaultPortDescription() {
        this.number = null;
        this.isEnabled = false;
        this.isRemoved = false;
        this.portSpeed = DEFAULT_SPEED;
        this.type = Type.COPPER;
    }

    @Override
    public PortNumber portNumber() {
        return number;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    @Override
    public boolean isRemoved() {
        return isRemoved;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public long portSpeed() {
        return portSpeed;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("number", number)
                .add("isEnabled", isEnabled)
                .add("isRemoved", isRemoved)
                .add("type", type)
                .add("portSpeed", portSpeed)
                .add("annotations", annotations())
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), number, isEnabled, type,
                                portSpeed);
    }

    @Override
    public boolean equals(Object object) {
        if (object != null && getClass() == object.getClass()) {
            if (!super.equals(object)) {
                return false;
            }
            DefaultPortDescription that = (DefaultPortDescription) object;
            return Objects.equal(this.number, that.number)
                    && Objects.equal(this.isEnabled, that.isEnabled)
                    && Objects.equal(this.type, that.type)
                    && Objects.equal(this.portSpeed, that.portSpeed);
        }
        return false;
    }

    /**
     * Creates port description builder with default parameters.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates port description builder inheriting with default parameters,
     * from specified port description.
     *
     * @param desc to inherit default from
     * @return builder
     */
    public static Builder builder(PortDescription desc) {
        return new Builder(desc);
    }

    public static class Builder {
        private PortNumber number;
        private boolean isEnabled = true;
        private boolean isRemoved = false;
        private Type type = Type.COPPER;
        private long portSpeed = DEFAULT_SPEED;
        private SparseAnnotations annotations = DefaultAnnotations.EMPTY;

        Builder() {}

        Builder(PortDescription desc) {
            this.number = desc.portNumber();
            this.isEnabled = desc.isEnabled();
            this.isRemoved = desc.isRemoved();
            this.type = desc.type();
            this.portSpeed = desc.portSpeed();
            this.annotations  = desc.annotations();
        }

        /**
         * Sets mandatory field PortNumber.
         *
         * @param number to set
         * @return self
         */
        public Builder withPortNumber(PortNumber number) {
            this.number = checkNotNull(number);
            return this;
        }

        /**
         * Sets enabled state.
         *
         * @param enabled state
         * @return self
         */
        public Builder isEnabled(boolean enabled) {
            this.isEnabled = enabled;
            return this;
        }

        /**
         * Sets removed state.
         *
         * @param removed state
         * @return self
         */
        public Builder isRemoved(boolean removed) {
            this.isRemoved = removed;
            return this;
        }

        /**
         * Sets port type.
         *
         * @param type of the port
         * @return self
         */
        public Builder type(Type type) {
            this.type = type;
            return this;
        }

        /**
         * Sets port speed.
         *
         * @param mbps port speed in Mbps
         * @return self
         */
        public Builder portSpeed(long mbps) {
            this.portSpeed = mbps;
            return this;
        }

        /**
         * Sets annotations.
         *
         * @param annotations of the port
         * @return self
         */
        public Builder annotations(SparseAnnotations annotations) {
            this.annotations = checkNotNull(annotations);
            return this;
        }

        /**
         * Builds the port description.
         *
         * @return port description
         */
        public DefaultPortDescription build() {
            return new DefaultPortDescription(number, isEnabled, isRemoved, type, portSpeed, annotations);
        }
    }
}
