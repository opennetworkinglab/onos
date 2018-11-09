/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.incubator.net.tunnel;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;
import java.util.Optional;

import com.google.common.annotations.Beta;
import org.onosproject.net.AbstractModel;
import org.onosproject.net.Annotations;
import org.onosproject.net.ElementId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;

/**
 * Default optical tunnel point model implementation.
 */
@Beta
public class DefaultOpticalTunnelEndPoint extends AbstractModel implements OpticalTunnelEndPoint {
    private final Optional<ElementId> elementId;
    private final Optional<PortNumber> portNumber;
    private final Optional<OpticalTunnelEndPoint> parentPoint;
    private final Type type;
    private final OpticalLogicId id;
    private final boolean isGlobal;

    /**
     * Creates a optical tunnel point attributed to the specified provider (may be null).
     * if provider is null, which means the optical tunnel point is not managed by the SB.
     *
     * @param providerId  tunnelProvider Id
     * @param elementId   parent network element
     * @param number      port number
     * @param parentPoint parent port or parent label
     * @param type        port type
     * @param id          LabelId
     * @param isGlobal    indicator whether the label is global significant or not
     * @param annotations optional key/value annotations
     */
    public DefaultOpticalTunnelEndPoint(ProviderId providerId, Optional<ElementId> elementId,
                        Optional<PortNumber> number, Optional<OpticalTunnelEndPoint> parentPoint,
                        Type type, OpticalLogicId id, boolean isGlobal, Annotations... annotations) {
        super(providerId, annotations);
        this.elementId = elementId;
        this.portNumber = number;
        this.parentPoint = parentPoint;
        this.id = id;
        this.type = type;
        this.isGlobal = isGlobal;
    }

    @Override
    public OpticalLogicId id() {
        return id;
    }

    @Override
    public Optional<ElementId> elementId() {
        return elementId;
    }

    @Override
    public Optional<PortNumber> portNumber() {
        return portNumber;
    }

    @Override
    public Optional<OpticalTunnelEndPoint> parentPoint() {
        return parentPoint;
    }

    @Override
    public boolean isGlobal() {
        return isGlobal;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(elementId, portNumber, parentPoint, id);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultOpticalTunnelEndPoint) {
            final DefaultOpticalTunnelEndPoint other = (DefaultOpticalTunnelEndPoint) obj;
            return Objects.equals(this.id, other.id) &&
                   Objects.equals(this.type, other.type) &&
                   Objects.equals(this.isGlobal, other.isGlobal) &&
                   Objects.equals(this.elementId, other.elementId) &&
                   Objects.equals(this.portNumber, other.portNumber) &&
                   Objects.equals(this.parentPoint, other.parentPoint);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("elementId", elementId)
                .add("portNumber", portNumber)
                .add("parentPoint", parentPoint)
                .add("type", type)
                .add("id", id)
                .add("isGlobal", isGlobal)
                .toString();
    }

}
