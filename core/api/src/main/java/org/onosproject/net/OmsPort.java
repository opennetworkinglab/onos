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

import org.onlab.util.Frequency;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of OMS port (Optical Multiplexing Section).
 * Also referred to as a WDM port or W-port.
 * See ITU G.709 "Interfaces for the Optical Transport Network (OTN)"
 *
 * Assumes we only support fixed grid for now.
 *
 * @deprecated in Goldeneye (1.6.0)
 */
@Deprecated
public class OmsPort extends DefaultPort {

    private final Frequency minFrequency;     // Minimum frequency
    private final Frequency maxFrequency;     // Maximum frequency
    private final Frequency grid;             // Grid spacing frequency


    /**
     * Creates an OMS port in the specified network element.
     *
     * @param element       parent network element
     * @param number        port number
     * @param isEnabled     port enabled state
     * @param minFrequency  minimum frequency
     * @param maxFrequency  maximum frequency
     * @param grid          grid spacing frequency
     * @param annotations   optional key/value annotations
     */
    public OmsPort(Element element, PortNumber number, boolean isEnabled,
                   Frequency minFrequency, Frequency maxFrequency, Frequency grid, Annotations... annotations) {
        super(element, number, isEnabled, Type.OMS, 0, annotations);
        this.minFrequency = checkNotNull(minFrequency);
        this.maxFrequency = checkNotNull(maxFrequency);
        this.grid = checkNotNull(grid);
    }

    /**
     * Returns the total number of channels on the port.
     *
     * @return total number of channels
     */
    public short totalChannels() {
        Frequency diff = maxFrequency.subtract(minFrequency);
        return (short) (diff.asHz() / grid.asHz());
    }

    /**
     * Returns the minimum frequency.
     *
     * @return minimum frequency
     */
    public Frequency minFrequency() {
        return minFrequency;
    }

    /**
     * Returns the maximum frequency.
     *
     * @return maximum frequency
     */
    public Frequency maxFrequency() {
        return maxFrequency;
    }

    /**
     * Returns the grid spacing frequency.
     *
     * @return grid spacing frequency
     */
    public Frequency grid() {
        return grid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(number(), isEnabled(), type(),
                minFrequency, maxFrequency, grid, annotations());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass()) {
            final OmsPort other = (OmsPort) obj;
            return Objects.equals(this.element().id(), other.element().id()) &&
                    Objects.equals(this.number(), other.number()) &&
                    Objects.equals(this.isEnabled(), other.isEnabled()) &&
                    Objects.equals(this.minFrequency, other.minFrequency) &&
                    Objects.equals(this.maxFrequency, other.maxFrequency) &&
                    Objects.equals(this.grid, other.grid) &&
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
                .add("minFrequency", minFrequency)
                .add("maxFrequency", maxFrequency)
                .add("grid", grid)
                .toString();
    }

}
