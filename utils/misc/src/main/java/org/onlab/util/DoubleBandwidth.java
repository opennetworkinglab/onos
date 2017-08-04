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
package org.onlab.util;

import java.util.Objects;

/**
 * Representation of bandwidth.
 * Use the static factory method corresponding to the unit (like Kbps) you desire on instantiation.
 */
final class DoubleBandwidth implements Bandwidth {

    private final double bps;

    /**
     * Creates a new instance with given bandwidth.
     *
     * @param bps bandwidth value to be assigned
     */
    DoubleBandwidth(double bps) {
        this.bps = bps;
    }

    // Constructor for serialization
    private DoubleBandwidth() {
        this.bps = 0;
    }
    /**
     * Returns bandwidth in bps.
     *
     * @return bandwidth in bps.
     */
    public double bps() {
        return bps;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof DoubleBandwidth)) {
            return false;
        }

        DoubleBandwidth that = (DoubleBandwidth) obj;
        return Objects.equals(this.bps, that.bps);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(Double.doubleToLongBits(bps));
    }

    @Override
    public String toString() {
        return String.valueOf(this.bps);
    }
}
