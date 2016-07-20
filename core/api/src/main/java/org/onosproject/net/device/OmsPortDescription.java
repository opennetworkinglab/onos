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
import org.onlab.util.Frequency;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;

/**
 * Default implementation of immutable OMS port description.
 *
 * @deprecated in Goldeneye (1.6.0)
 */
@Deprecated
public class OmsPortDescription extends DefaultPortDescription {

    private final Frequency minFrequency;
    private final Frequency maxFrequency;
    private final Frequency grid;

    /**
     * Creates OMS port description based on the supplied information.
     *
     * @param number        port number
     * @param isEnabled     port enabled state
     * @param minFrequency  minimum frequency
     * @param maxFrequency  maximum frequency
     * @param grid          grid spacing frequency
     * @param annotations   optional key/value annotations map
     *
     * @deprecated in Goldeneye (1.6.0)
     */
    @Deprecated
    public OmsPortDescription(PortNumber number, boolean isEnabled, Frequency minFrequency, Frequency maxFrequency,
                              Frequency grid, SparseAnnotations... annotations) {
        super(number, isEnabled, Port.Type.OMS, 0, annotations);
        this.minFrequency = minFrequency;
        this.maxFrequency = maxFrequency;
        this.grid = grid;
    }

    /**
     * Creates OMS port description based on the supplied information.
     *
     * @param base          PortDescription to get basic information from
     * @param minFrequency  minimum frequency
     * @param maxFrequency  maximum frequency
     * @param grid          grid spacing frequency
     * @param annotations   optional key/value annotations map
     *
     * @deprecated in Goldeneye (1.6.0)
     */
    @Deprecated
    public OmsPortDescription(PortDescription base, Frequency minFrequency, Frequency maxFrequency,
                              Frequency grid, SparseAnnotations annotations) {
        super(base, annotations);
        this.minFrequency = minFrequency;
        this.maxFrequency = maxFrequency;
        this.grid = grid;
    }

    /**
     * Returns minimum frequency.
     *
     * @return minimum frequency
     */
    public Frequency minFrequency() {
        return minFrequency;
    }

    /**
     * Returns maximum frequency.
     *
     * @return maximum frequency
     */
    public Frequency maxFrequency() {
        return maxFrequency;
    }

    /**
     * Returns grid spacing frequency.
     *
     * @return grid spacing frequency
     */
    public Frequency grid() {
        return grid;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("number", portNumber())
                .add("isEnabled", isEnabled())
                .add("type", type())
                .add("minFrequency", minFrequency)
                .add("maxFrequency", maxFrequency)
                .add("grid", grid)
                .add("annotations", annotations())
                .toString();
    }

}

