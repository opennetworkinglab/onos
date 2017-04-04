/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.pce.pceservice.constraint;

import org.onlab.util.Bandwidth;
import org.onlab.util.DataRateUnit;
import org.onosproject.net.Link;
import org.onosproject.net.intent.ResourceContext;
import org.onosproject.net.intent.constraint.BooleanConstraint;
import org.onosproject.bandwidthmgr.api.BandwidthMgmtService;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Constraint that evaluates links based on available pce bandwidths.
 */
public final class PceBandwidthConstraint extends BooleanConstraint {

    private final Bandwidth bandwidth;

    /**
     * Creates a new pce bandwidth constraint.
     *
     * @param bandwidth required bandwidth
     */
    public PceBandwidthConstraint(Bandwidth bandwidth) {
        this.bandwidth = checkNotNull(bandwidth, "Bandwidth cannot be null");
    }

    /**
     * Creates a new pce bandwidth constraint.
     *
     * @param v         required amount of bandwidth
     * @param unit      {@link DataRateUnit} of {@code v}
     * @return  {@link PceBandwidthConstraint} instance with given bandwidth requirement
     */
    public static PceBandwidthConstraint of(double v, DataRateUnit unit) {
        return new PceBandwidthConstraint(Bandwidth.of(v, unit));
    }

    // Constructor for serialization
    private PceBandwidthConstraint() {
        this.bandwidth = null;
    }

    @Override
    public boolean isValid(Link link, ResourceContext context) {
        return false;
        //Do nothing instead using isValidLink needs bandwidthMgmtService to validate link
    }

    /**
     * Validates the link based on pce bandwidth constraint.
     *
     * @param link to validate pce bandwidth constraint
     * @param bandwidthMgmtService instance of BandwidthMgmtService
     * @return true if link satisfies pce bandwidth constraint otherwise false
     */
    public boolean isValidLink(Link link, BandwidthMgmtService bandwidthMgmtService) {
        if (bandwidthMgmtService == null) {
            return false;
        }

        return bandwidthMgmtService.isBandwidthAvailable(link, bandwidth.bps());

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
        final PceBandwidthConstraint other = (PceBandwidthConstraint) obj;
        return Objects.equals(this.bandwidth, other.bandwidth);
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("bandwidth", bandwidth).toString();
    }
}
