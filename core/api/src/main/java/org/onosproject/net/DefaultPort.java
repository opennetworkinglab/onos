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

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default port implementation.
 */
public class DefaultPort extends AbstractAnnotated implements Port {

    /** Default port speed in Mbps. */
    public static final long DEFAULT_SPEED = 1_000;

    private final Element element;
    private final PortNumber number;
    private final boolean isEnabled;
    private final Type type;
    private final long portSpeed;

    /**
     * Creates a network element attributed to the specified provider.
     *
     * @param element     parent network element
     * @param number      port number
     * @param isEnabled   indicator whether the port is up and active
     * @param annotations optional key/value annotations
     */
    public DefaultPort(Element element, PortNumber number, boolean isEnabled,
                       Annotations... annotations) {
        this(element, number, isEnabled, Type.COPPER, DEFAULT_SPEED, annotations);
    }

    /**
     * Creates a network element attributed to the specified provider.
     *
     * @param element     parent network element
     * @param number      port number
     * @param isEnabled   indicator whether the port is up and active
     * @param type        port type
     * @param portSpeed   port speed in Mbs
     * @param annotations optional key/value annotations
     */
    public DefaultPort(Element element, PortNumber number, boolean isEnabled,
                       Type type, long portSpeed, Annotations... annotations) {
        super(annotations);
        this.element = element;
        this.number = number;
        this.isEnabled = isEnabled;
        this.type = type;
        this.portSpeed = portSpeed;
    }

    @Override
    public Element element() {
        return element;
    }

    @Override
    public PortNumber number() {
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
    public int hashCode() {
        return Objects.hash(number, isEnabled, type, portSpeed, annotations());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultPort) {
            final DefaultPort other = (DefaultPort) obj;
            return Objects.equals(this.element.id(), other.element.id()) &&
                    Objects.equals(this.number, other.number) &&
                    Objects.equals(this.isEnabled, other.isEnabled) &&
                    Objects.equals(this.type, other.type) &&
                    Objects.equals(this.portSpeed, other.portSpeed) &&
                    Objects.equals(this.annotations(), other.annotations());
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("element", element.id())
                .add("number", number)
                .add("isEnabled", isEnabled)
                .add("type", type)
                .add("portSpeed", portSpeed)
                .add("annotations", annotations())
                .toString();
    }

}
