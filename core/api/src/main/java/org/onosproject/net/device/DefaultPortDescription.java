/*
 * Copyright 2014-present Open Networking Laboratory
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
    private final Type type;
    private final long portSpeed;

    /**
     * Creates a DEFAULT_SPEED COPPER port description using the supplied information.
     *
     * @param number      port number
     * @param isEnabled   port enabled state
     * @param annotations optional key/value annotations map
     */
    public DefaultPortDescription(PortNumber number, boolean isEnabled,
                                  SparseAnnotations... annotations) {
        this(number, isEnabled, Type.COPPER, DEFAULT_SPEED, annotations);
    }

    /**
     * Creates a port description using the supplied information.
     *
     * @param number      port number
     * @param isEnabled   port enabled state
     * @param type        port type
     * @param portSpeed   port speed in Mbps
     * @param annotations optional key/value annotations map
     */
    public DefaultPortDescription(PortNumber number, boolean isEnabled,
                                  Type type, long portSpeed,
                                  SparseAnnotations...annotations) {
        super(annotations);
        this.number = checkNotNull(number);
        this.isEnabled = isEnabled;
        this.type = type;
        this.portSpeed = portSpeed;
    }

    // Default constructor for serialization
    protected DefaultPortDescription() {
        this.number = null;
        this.isEnabled = false;
        this.portSpeed = DEFAULT_SPEED;
        this.type = Type.COPPER;
    }

    /**
     * Creates a port description using the supplied information.
     *
     * @param base        PortDescription to get basic information from
     * @param annotations optional key/value annotations map
     */
    public DefaultPortDescription(PortDescription base,
                                  SparseAnnotations annotations) {
        this(base.portNumber(), base.isEnabled(), base.type(), base.portSpeed(),
             annotations);
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

}
