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
package org.onosproject.net;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of OCh port (Optical Channel).
 * Also referred to as a line side port (L-port) or narrow band port.
 * See ITU G.709 "Interfaces for the Optical Transport Network (OTN)"
 *
 * @deprecated in Goldeneye (1.6.0)
 */
@Deprecated
public class OchPort extends DefaultPort {

    private final OduSignalType signalType;
    private final boolean isTunable;
    private final OchSignal lambda;

    /**
     * Creates an OCh port in the specified network element.
     *
     * @param element     parent network element
     * @param number      port number
     * @param isEnabled   port enabled state
     * @param signalType  ODU signal type
     * @param isTunable   tunable wavelength capability
     * @param lambda      OCh signal
     * @param annotations optional key/value annotations
     */
    public OchPort(Element element, PortNumber number, boolean isEnabled, OduSignalType signalType,
                   boolean isTunable, OchSignal lambda, Annotations... annotations) {
        super(element, number, isEnabled, Type.OCH, checkNotNull(signalType).bitRate(), annotations);
        this.signalType = signalType;
        this.isTunable = isTunable;
        this.lambda = checkNotNull(lambda);
    }

    /**
     * Returns ODU signal type.
     *
     * @return ODU signal type
     */
    public OduSignalType signalType() {
        return signalType;
    }

    /**
     * Returns true if port is wavelength tunable.
     *
     * @return tunable wavelength capability
     */
    public boolean isTunable() {
        return isTunable;
    }

    /**
     * Returns OCh signal.
     *
     * @return OCh signal
     */
    public OchSignal lambda() {
        return lambda;
    }

    @Override
    public int hashCode() {
        return Objects.hash(number(), isEnabled(), type(), signalType, isTunable, lambda, annotations());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        // Subclass is considered as a change of identity, hence equals() will return false if class type don't match
        if (obj != null && getClass() == obj.getClass()) {
            final OchPort other = (OchPort) obj;
            return Objects.equals(this.element().id(), other.element().id()) &&
                    Objects.equals(this.number(), other.number()) &&
                    Objects.equals(this.isEnabled(), other.isEnabled()) &&
                    Objects.equals(this.signalType, other.signalType) &&
                    Objects.equals(this.isTunable, other.isTunable) &&
                    Objects.equals(this.lambda, other.lambda) &&
                    Objects.equals(this.annotations(), other.annotations());
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("element", element().id())
                .add("number", number())
                .add("isEnabled", isEnabled())
                .add("type", type())
                .add("signalType", signalType)
                .add("isTunable", isTunable)
                .add("lambda", lambda)
                .toString();
    }
}
