/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.iptopology.api;

import java.util.Objects;

import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;

import com.google.common.base.MoreObjects;

/**
 * Abstraction of a network termination point expressed as a pair of the network element identifier and device
 * interface.
 */
public class TerminationPoint {
    private final ElementId elementId;
    private final DeviceInterface deviceInterface;

    /**
     * Constructor to initialize its parameters.
     *
     * @param elementId network element identifier
     * @param deviceInterface device interface
     */
    public TerminationPoint(ElementId elementId, DeviceInterface deviceInterface) {
        this.elementId = elementId;
        this.deviceInterface = deviceInterface;
    }

    /**
     * Returns the network element identifier.
     *
     * @return element identifier
     */
    public ElementId elementId() {
        return elementId;
    }

    /**
     * Returns the identifier of the infrastructure device if the termination
     * point belongs to a network element which is indeed an ip
     * device.
     *
     * @return network element identifier as a device identifier
     * @throws java.lang.IllegalStateException if termination point is not
     *                                         associated with a device
     */
    public DeviceId deviceId() {
        if (elementId instanceof DeviceId) {
            return (DeviceId) elementId;
        }
        throw new IllegalStateException("Termination point not associated " +
                "with an ip device");
    }

    /**
     * Returns Device interface details.
     *
     * @return device interface details
     */
    public DeviceInterface deviceInterface() {
        return deviceInterface;
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementId, deviceInterface);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof TerminationPoint) {
            final TerminationPoint other = (TerminationPoint) obj;
            return Objects.equals(this.elementId, other.elementId)
                    && Objects.equals(this.deviceInterface, other.deviceInterface);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("elementId", elementId)
                .add("deviceInterface", deviceInterface)
                .toString();
    }
}