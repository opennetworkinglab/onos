/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.net.resource.link;

import org.onlab.util.Bandwidth;
import org.onlab.util.DataRateUnit;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of bandwidth resource in bps.
 *
 * @deprecated in Emu Release
 */
@Deprecated
public final class BandwidthResource implements LinkResource {

    private final Bandwidth bandwidth;

    /**
     * Creates a new instance with given bandwidth.
     *
     * @param bandwidth bandwidth value to be assigned
     */
    public BandwidthResource(Bandwidth bandwidth) {
        this.bandwidth = checkNotNull(bandwidth);
    }

    // Constructor for serialization
    private BandwidthResource() {
        this.bandwidth = null;
    }

    /**
     * Creates a new bandwidth resource.
     *
     * @param v         amount of bandwidth to request
     * @param unit      {@link DataRateUnit} of {@code v}
     * @return  {@link BandwidthResource} instance with given bandwidth
     */
    public static BandwidthResource of(double v, DataRateUnit unit) {
        return new BandwidthResource(Bandwidth.of(v, unit));
    }

    /**
     * Returns bandwidth as a double value.
     *
     * @return bandwidth as a double value
     */
    public double toDouble() {
        return bandwidth.bps();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BandwidthResource) {
            BandwidthResource that = (BandwidthResource) obj;
            return Objects.equals(this.bandwidth, that.bandwidth);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.bandwidth);
    }

    @Override
    public String toString() {
        return String.valueOf(this.bandwidth);
    }
}
