/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.net.behaviour.trafficcontrol;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;

import java.util.Collection;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of the policer interface.
 */
@Beta
public final class DefaultPolicer implements Policer, PolicerEntry {

    // Immutable parameters
    private final DeviceId deviceId;
    private final ApplicationId applicationId;
    private final PolicerId policerId;
    private final boolean colorAware;
    private final Unit unit;
    private final Collection<TokenBucket> tokenBuckets;
    private final String description;

    // Mutable parameters
    private long referenceCount;
    private long processedPackets;
    private long processedBytes;
    private long life;

    private DefaultPolicer(DeviceId dId, ApplicationId aId, PolicerId pId,
                           boolean cA, Unit u, Collection<TokenBucket> tB,
                           String d) {
        deviceId = dId;
        applicationId = aId;
        policerId = pId;
        colorAware = cA;
        unit = u;
        tokenBuckets = tB;
        description = d;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public ApplicationId applicationId() {
        return applicationId;
    }

    @Override
    public PolicerId policerId() {
        return policerId;
    }

    @Override
    public boolean isColorAware() {
        return colorAware;
    }

    @Override
    public Unit unit() {
        return unit;
    }

    @Override
    public Collection<TokenBucket> tokenBuckets() {
        return tokenBuckets;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public long referenceCount() {
        return referenceCount;
    }

    @Override
    public void setReferenceCount(long count) {
        referenceCount = count;
    }

    @Override
    public long processedPackets() {
        return processedPackets;
    }

    @Override
    public void setProcessedPackets(long packets) {
        processedPackets = packets;
    }

    @Override
    public long processedBytes() {
        return processedBytes;
    }

    @Override
    public void setProcessedBytes(long bytes) {
        processedBytes = bytes;
    }

    @Override
    public long life() {
        return life;
    }

    @Override
    public void setLife(long l) {
        life = l;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("appId", applicationId())
                .add("id", policerId())
                .add("isColorAware", isColorAware())
                .add("unit", unit())
                .add("tokenBuckets", tokenBuckets())
                .add("description", description())
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultPolicer that = (DefaultPolicer) o;
        return Objects.equal(policerId, that.policerId);
    }

    @Override
    public int hashCode() {
        return policerId.hashCode();
    }

    /**
     * Returns a new builder reference.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Implementation of the policer builder interface.
     */
    public static final class Builder implements Policer.Builder {

        private DeviceId deviceId;
        private ApplicationId applicationId;
        private PolicerId policerId;
        // Default to unaware
        private boolean colorAware = false;
        // Default to MBps
        private Unit unit = Unit.MB_PER_SEC;
        private Collection<TokenBucket> tokenBuckets;
        private String description = "";

        @Override
        public Policer.Builder forDeviceId(DeviceId dId) {
            deviceId = dId;
            return this;
        }

        @Override
        public Policer.Builder fromApp(ApplicationId appId) {
            applicationId = appId;
            return this;
        }

        @Override
        public Policer.Builder withId(PolicerId id) {
            policerId = id;
            return this;
        }

        @Override
        public Policer.Builder colorAware(boolean isColorAware) {
            colorAware = isColorAware;
            return this;
        }

        @Override
        public Policer.Builder withUnit(Unit u) {
            unit = u;
            return this;
        }

        @Override
        public Policer.Builder withPolicingResource(PolicingResource policingResource) {
            policerId = policingResource.policerId();
            deviceId = policingResource.connectPoint().deviceId();
            return this;
        }

        @Override
        public Policer.Builder withTokenBuckets(Collection<TokenBucket> tB) {
            tokenBuckets = ImmutableSet.copyOf(tB);
            return this;
        }

       @Override
       public Policer.Builder withDescription(String d) {
           description = d;
           return this;
       }

        @Override
        public DefaultPolicer build() {
            // Not null condition on some mandatory parameters
            checkNotNull(deviceId, "Must specify a deviceId");
            checkNotNull(applicationId, "Must specify an application id");
            checkNotNull(policerId, "Must specify a policer id");
            checkNotNull(unit, "Must specify a unit for the policer");
            checkNotNull(tokenBuckets, "Must have token buckets");
            checkNotNull(description, "Must have a description");

            // Verify argument conditions
            checkArgument(!tokenBuckets.isEmpty(), "Must have at least one token bucket");

            // Finally we build the policer
            return new DefaultPolicer(deviceId, applicationId, policerId,
                                      colorAware, unit, tokenBuckets,
                                      description);
        }
    }
}
