/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.net.optical;

import org.onlab.util.Frequency;
import com.google.common.annotations.Beta;

/**
 * OMS port (Optical Multiplexing Section).
 * Also referred to as a WDM port or W-port.
 * See ITU G.709 "Interfaces for the Optical Transport Network (OTN)"
 *
 * Assumes we only support fixed grid for now.
 */
@Beta
public interface OmsPort extends ProjectedPort {

    /**
     * Returns the total number of channels on the port.
     *
     * @return total number of channels
     */
    default short totalChannels() {
        Frequency diff = maxFrequency().subtract(minFrequency());
        return (short) (diff.asHz() / grid().asHz() + 1);
    }

    /**
     * Returns the minimum frequency.
     *
     * @return minimum frequency
     */
    Frequency minFrequency();

    /**
     * Returns the maximum frequency.
     *
     * @return maximum frequency
     */
    Frequency maxFrequency();

    /**
     * Returns the grid spacing frequency.
     *
     * @return grid spacing frequency
     */
    Frequency grid();

}
