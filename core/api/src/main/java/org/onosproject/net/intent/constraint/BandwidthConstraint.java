/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.intent.constraint;

import com.google.common.annotations.Beta;

import org.onlab.util.Bandwidth;
import org.onlab.util.DataRateUnit;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.intent.ResourceContext;
import org.onosproject.net.resource.Resources;

import java.util.Objects;
import java.util.stream.Stream;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Constraint that evaluates links based on available bandwidths.
 */
@Beta
public final class BandwidthConstraint extends BooleanConstraint {

    private final Bandwidth bandwidth;

    /**
     * Creates a new bandwidth constraint.
     *
     * @param bandwidth required bandwidth
     */
    public BandwidthConstraint(Bandwidth bandwidth) {
        this.bandwidth = checkNotNull(bandwidth, "Bandwidth cannot be null");
    }

    /**
     * Creates a new bandwidth constraint.
     *
     * @param v         required amount of bandwidth
     * @param unit      {@link DataRateUnit} of {@code v}
     * @return  {@link BandwidthConstraint} instance with given bandwidth requirement
     */
    public static BandwidthConstraint of(double v, DataRateUnit unit) {
        return new BandwidthConstraint(Bandwidth.of(v, unit));
    }

    // Constructor for serialization
    private BandwidthConstraint() {
        this.bandwidth = null;
    }

    @Override
    public boolean isValid(Link link, ResourceContext context) {
        return Stream.of(link.src(), link.dst())
                .filter(cp -> cp.elementId() instanceof DeviceId)
                .map(cp -> Resources.continuous(cp.deviceId(), cp.port(), Bandwidth.class).resource(bandwidth.bps()))
                .allMatch(context::isAvailable);
    }

    /**
     * Returns the bandwidth required by this constraint.
     *
     * @return required bandwidth
     */
    public Bandwidth bandwidth() {
        return bandwidth;
    }

    @Override
    public int hashCode() {
        return bandwidth.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final BandwidthConstraint other = (BandwidthConstraint) obj;
        return Objects.equals(this.bandwidth, other.bandwidth);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("bandwidth", bandwidth).toString();
    }
}
