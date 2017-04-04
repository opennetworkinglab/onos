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
package org.onosproject.pce.pceservice.constraint;

import org.onlab.util.Bandwidth;
import org.onosproject.net.Link;
import org.onosproject.net.intent.ResourceContext;
import org.onosproject.net.intent.constraint.BooleanConstraint;
import org.onosproject.bandwidthmgr.api.BandwidthMgmtService;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Constraint that evaluates whether links satisfies sharedbandwidth request.
 */
public final class SharedBandwidthConstraint extends BooleanConstraint {

    private final List<Link> links;
    private final Bandwidth sharedBwValue;
    private final Bandwidth requestBwValue;
    //temporary variable declared to hold changed bandwidth value
    private Bandwidth changedBwValue;

    // Constructor for serialization
    private SharedBandwidthConstraint() {
        links = null;
        sharedBwValue = null;
        requestBwValue = null;
    }

    /**
     * Creates a new SharedBandwidth constraint.
     *
     * @param links shared links
     * @param sharedBwValue shared bandwidth of the links
     * @param requestBwValue requested bandwidth value
     */
    public SharedBandwidthConstraint(List<Link> links, Bandwidth sharedBwValue, Bandwidth requestBwValue) {
        this.links = links;
        this.sharedBwValue = sharedBwValue;
        this.requestBwValue = requestBwValue;
    }

    /**
     * Creates a new SharedBandwidth constraint.
     *
     * @param links shared links
     * @param sharedBwValue shared bandwidth of the links
     * @param requestBwValue requested bandwidth value
     * @return SharedBandwidth instance
     */
    public static SharedBandwidthConstraint of(List<Link> links, Bandwidth sharedBwValue, Bandwidth requestBwValue) {
        return new SharedBandwidthConstraint(links, sharedBwValue, requestBwValue);
    }

    /**
     * Obtains shared links.
     *
     * @return shared links
     */
    public List<Link> links() {
        return links;
    }

    /**
     * Obtains shared bandwidth of the links.
     *
     * @return shared bandwidth
     */
    public Bandwidth sharedBwValue() {
        return sharedBwValue;
    }

    /**
     * Obtains requested bandwidth value.
     *
     * @return requested bandwidth value
     */
    public Bandwidth requestBwValue() {
        return requestBwValue;
    }

    @Override
    public boolean isValid(Link link, ResourceContext context) {
        return false;
        //Do nothing instead using isValidLink needs pce service to validate link
    }

    /**
     * Validates the link based on shared bandwidth constraint.
     *
     * @param link to validate shared bandwidth constraint
     * @param bandwidthMgmtService instance of BandwidthMgmtService
     * @return true if link satisfies shared bandwidth constraint otherwise false
     */
    public boolean isValidLink(Link link, BandwidthMgmtService bandwidthMgmtService) {
        if (bandwidthMgmtService == null) {
            return false;
        }
        changedBwValue = requestBwValue;
        if (links.contains(link)) {
            changedBwValue = requestBwValue.isGreaterThan(sharedBwValue) ? requestBwValue.subtract(sharedBwValue)
                    : Bandwidth.bps(0);
        }

        return bandwidthMgmtService.isBandwidthAvailable(link, changedBwValue.bps());
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestBwValue, sharedBwValue, links);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof SharedBandwidthConstraint) {
            SharedBandwidthConstraint other = (SharedBandwidthConstraint) obj;
            return Objects.equals(this.requestBwValue, other.requestBwValue)
                    && Objects.equals(this.sharedBwValue, other.sharedBwValue)
                    && Objects.equals(this.links, other.links);
        }

        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("requestBwValue", requestBwValue)
                .add("sharedBwValue", sharedBwValue)
                .add("links", links)
                .toString();
    }
}