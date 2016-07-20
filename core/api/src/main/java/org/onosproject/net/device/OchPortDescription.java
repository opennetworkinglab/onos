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
package org.onosproject.net.device;

import com.google.common.base.MoreObjects;

import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of immutable OCh port description.
 *
 * @deprecated in Goldeneye (1.6.0)
 */
@Deprecated
public class OchPortDescription extends DefaultPortDescription {

    private final OduSignalType signalType;
    private final boolean isTunable;
    private final OchSignal lambda;

    /**
     * Creates OCH port description based on the supplied information.
     *
     * @param number      port number
     * @param isEnabled   port enabled state
     * @param signalType  ODU signal type
     * @param isTunable   tunable wavelength capability
     * @param lambda      OCh signal
     * @param annotations optional key/value annotations map
     *
     * @deprecated in Goldeneye (1.6.0)
     */
    @Deprecated
    public OchPortDescription(PortNumber number, boolean isEnabled, OduSignalType signalType,
                              boolean isTunable, OchSignal lambda, SparseAnnotations... annotations) {
        super(number, isEnabled, Port.Type.OCH, 0, annotations);
        this.signalType = signalType;
        this.isTunable = isTunable;
        this.lambda = checkNotNull(lambda);
    }

    /**
     * Creates OCH port description based on the supplied information.
     *
     * @param base        PortDescription to get basic information from
     * @param signalType  ODU signal type
     * @param isTunable   tunable wavelength capability
     * @param lambda      OCh signal
     * @param annotations optional key/value annotations map
     *
     * @deprecated in Goldeneye (1.6.0)
     */
    @Deprecated
    public OchPortDescription(PortDescription base, OduSignalType signalType, boolean isTunable,
                              OchSignal lambda, SparseAnnotations annotations) {
        super(base, annotations);
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
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("number", portNumber())
                .add("isEnabled", isEnabled())
                .add("type", type())
                .add("signalType", signalType)
                .add("isTunable", isTunable)
                .add("lambda", lambda)
                .add("annotations", annotations())
                .toString();
    }

}
