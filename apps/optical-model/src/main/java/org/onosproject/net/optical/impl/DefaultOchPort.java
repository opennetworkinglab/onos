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
package org.onosproject.net.optical.impl;

import org.onosproject.net.Annotations;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.Port;
import org.onosproject.net.optical.OchPort;
import org.onosproject.net.utils.ForwardingPort;

import com.google.common.annotations.Beta;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.optical.device.OchPortHelper.stripHandledAnnotations;

import java.util.Objects;

/**
 * Implementation of OCh port (Optical Channel).
 * Also referred to as a line side port (L-port) or narrow band port.
 * See ITU G.709 "Interfaces for the Optical Transport Network (OTN)"
 */
@Beta
public class DefaultOchPort extends ForwardingPort implements OchPort {

    // Note: try to avoid direct access to the field, use accessor.
    // We might want to lazily parse annotation in the future
    private final OduSignalType signalType;
    private final boolean isTunable;
    private final OchSignal lambda;

    /**
     * Creates an OCh port in the specified network element.
     *
     * @param base Port
     * @param signalType  ODU signal type
     * @param isTunable   tunable wavelength capability
     * @param lambda      OCh signal
     */
    public DefaultOchPort(Port base,
                          OduSignalType signalType,
                          boolean isTunable,
                          OchSignal lambda) {
        super(base);
        // TODO should this class be parsing annotation to instantiate signalType?
        this.signalType = checkNotNull(signalType);
        this.isTunable = isTunable;
        this.lambda = checkNotNull(lambda);
    }

    @Override
    public Type type() {
        return Type.OCH;
    }

    @Override
    public long portSpeed() {
        return signalType.bitRate();
    }

    @Override
    public Annotations unhandledAnnotations() {
        return stripHandledAnnotations(super.annotations());
    }

    /**
     * Returns ODU signal type.
     *
     * @return ODU signal type
     */
    @Override
    public OduSignalType signalType() {
        return signalType;
    }

    /**
     * Returns true if port is wavelength tunable.
     *
     * @return tunable wavelength capability
     */
    @Override
    public boolean isTunable() {
        return isTunable;
    }

    /**
     * Returns OCh signal.
     *
     * @return OCh signal
     */
    @Override
    public OchSignal lambda() {
        return lambda;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(),
                            signalType(),
                            isTunable(),
                            lambda());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj != null && getClass() == obj.getClass()) {
            final DefaultOchPort that = (DefaultOchPort) obj;
            return super.toEqualsBuilder(that)
                    .append(this.signalType(), that.signalType())
                    .append(this.isTunable(), that.isTunable())
                    .append(this.lambda(), that.lambda())
                    .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return super.toStringHelper()
                .add("signalType", signalType())
                .add("isTunable", isTunable())
                .add("lambda", lambda())
                .add("annotations", unhandledAnnotations())
                .toString();
    }
}
