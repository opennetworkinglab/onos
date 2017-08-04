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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.optical.device.OmsPortHelper.stripHandledAnnotations;

import java.util.Objects;

import org.onlab.util.Frequency;
import org.onosproject.net.Annotations;
import org.onosproject.net.Port;
import org.onosproject.net.optical.OmsPort;
import org.onosproject.net.utils.ForwardingPort;

import com.google.common.annotations.Beta;

/**
 * Implementation of OMS port (Optical Multiplexing Section).
 * Also referred to as a WDM port or W-port.
 * See ITU G.709 "Interfaces for the Optical Transport Network (OTN)"
 *
 * Assumes we only support fixed grid for now.
 */
@Beta
public class DefaultOmsPort extends ForwardingPort implements OmsPort {

    private final Frequency minFrequency;     // Minimum frequency
    private final Frequency maxFrequency;     // Maximum frequency
    private final Frequency grid;             // Grid spacing frequency

    /**
     * Creates an OMS port.
     *
     * @param delegate      Port
     * @param minFrequency  minimum frequency
     * @param maxFrequency  maximum frequency
     * @param grid          grid spacing frequency
     */
    public DefaultOmsPort(Port delegate, Frequency minFrequency, Frequency maxFrequency, Frequency grid) {
        super(delegate);

        this.minFrequency = checkNotNull(minFrequency);
        this.maxFrequency = checkNotNull(maxFrequency);
        this.grid = checkNotNull(grid);
    }

    @Override
    public Type type() {
        return Type.OMS;
    }

    @Override
    public long portSpeed() {
        return 0;
    }

    @Override
    public Annotations unhandledAnnotations() {
        return stripHandledAnnotations(super.annotations());
    }


    @Override
    public Frequency minFrequency() {
        return minFrequency;
    }

    @Override
    public Frequency maxFrequency() {
        return maxFrequency;
    }

    @Override
    public Frequency grid() {
        return grid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(),
                            minFrequency(),
                            maxFrequency(),
                            grid());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj != null && getClass() == obj.getClass()) {
            final DefaultOmsPort that = (DefaultOmsPort) obj;
            return super.toEqualsBuilder(that)
                    .append(this.minFrequency(), that.minFrequency())
                    .append(this.maxFrequency(), that.maxFrequency())
                    .append(this.grid(), that.grid())
                    .isEquals();
        }
        return false;
    }

    @Override
    public String toString() {
        return super.toStringHelper()
                .add("minFrequency", minFrequency())
                .add("maxFrequency", maxFrequency())
                .add("grid", grid())
                .add("annotations", unhandledAnnotations())
                .toString();
    }

}
