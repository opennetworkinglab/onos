/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.drivers.server.impl.devices;

import org.onosproject.drivers.server.devices.cpu.BasicCpuCacheDevice;
import org.onosproject.drivers.server.devices.cpu.CpuCacheId;
import org.onosproject.drivers.server.devices.cpu.CpuCachePlacementPolicy;
import org.onosproject.drivers.server.devices.cpu.CpuVendor;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.drivers.server.Constants.MSG_CPU_CACHE_CAPACITY_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_CPU_CACHE_ID_NULL;
import static org.onosproject.drivers.server.Constants.MSG_CPU_CACHE_LINE_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_CPU_CACHE_POLICY_NULL;
import static org.onosproject.drivers.server.Constants.MSG_CPU_CACHE_SETS_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_CPU_CACHE_WAYS_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_CPU_VENDOR_NULL;

/**
 * Default implementation for basic CPU cache devices.
 */
public final class DefaultBasicCpuCacheDevice implements BasicCpuCacheDevice {

    private final CpuCacheId cacheId;
    private final CpuCachePlacementPolicy policy;
    private final CpuVendor vendor;
    private final long capacity;
    private final int sets;
    private final int lineLength;
    private final int ways;
    private final boolean isShared;

    private DefaultBasicCpuCacheDevice(CpuCacheId cacheId,
                                       CpuCachePlacementPolicy policy,
                                       CpuVendor vendor,
                                       long capacity,
                                       int sets,
                                       int ways,
                                       int lineLength,
                                       boolean isShared) {
        checkNotNull(cacheId, MSG_CPU_CACHE_ID_NULL);
        checkNotNull(policy, MSG_CPU_CACHE_POLICY_NULL);
        checkNotNull(vendor, MSG_CPU_VENDOR_NULL);
        checkArgument(capacity > 0, MSG_CPU_CACHE_CAPACITY_NEGATIVE);
        checkArgument(sets > 0, MSG_CPU_CACHE_SETS_NEGATIVE);
        checkArgument(lineLength > 0, MSG_CPU_CACHE_LINE_NEGATIVE);
        checkArgument(ways > 0, MSG_CPU_CACHE_WAYS_NEGATIVE);

        this.cacheId = cacheId;
        this.policy = policy;
        this.vendor = vendor;
        this.capacity = capacity;
        this.sets = sets;
        this.ways = ways;
        this.lineLength = lineLength;
        this.isShared = isShared;
    }

    /**
     * Creates a builder for DefaultBasicCpuCacheDevice object.
     *
     * @return builder object for DefaultBasicCpuCacheDevice object
     */
    public static DefaultBasicCpuCacheDevice.Builder builder() {
        return new Builder();
    }

    @Override
    public CpuCacheId cacheId() {
        return cacheId;
    }

    @Override
    public CpuCachePlacementPolicy policy() {
        return policy;
    }

    @Override
    public CpuVendor vendor() {
        return vendor;
    }

    @Override
    public long capacity() {
        return capacity;
    }

    @Override
    public int sets() {
        return sets;
    }

    @Override
    public int associativityWays() {
        return ways;
    }

    @Override
    public int lineLength() {
        return lineLength;
    }

    @Override
    public boolean isShared() {
        return isShared;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("cacheId",    cacheId())
                .add("policy",     policy())
                .add("vendor",     vendor())
                .add("capacity",   capacity())
                .add("sets",       sets())
                .add("ways",       associativityWays())
                .add("lineLength", lineLength())
                .add("isShared",   isShared())
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof BasicCpuCacheDevice)) {
            return false;
        }
        BasicCpuCacheDevice device = (BasicCpuCacheDevice) obj;
        return  this.cacheId() ==  device.cacheId() &&
                this.policy() == device.policy() &&
                this.vendor() == device.vendor() &&
                this.capacity() == device.capacity() &&
                this.sets() == device.sets() &&
                this.associativityWays() == device.associativityWays() &&
                this.lineLength() == device.lineLength() &&
                this.isShared() == device.isShared();
    }

    @Override
    public int hashCode() {
        return Objects.hash(cacheId, policy, vendor, capacity, sets, ways, lineLength, isShared);
    }

    public static final class Builder {

        CpuCacheId cacheId = null;
        CpuCachePlacementPolicy policy = null;
        CpuVendor vendor = null;
        long capacity = -1;
        int sets = -1;
        int ways = -1;
        int lineLength = -1;
        boolean isShared = false;

        private Builder() {

        }

        /**
         * Sets CPU cache vendor.
         *
         * @param vendorStr CPU cache vendor as a string
         * @return builder object
         */
        public Builder setVendor(String vendorStr) {
            if (!Strings.isNullOrEmpty(vendorStr)) {
                this.vendor = CpuVendor.getByName(vendorStr);
            }

            return this;
        }

        /**
         * Sets CPU cache ID.
         *
         * @param levelStr CPU cache level as a string
         * @param typeStr CPU cache type as a string
         * @return builder object
         */
        public Builder setCacheId(String levelStr, String typeStr) {
            if (!Strings.isNullOrEmpty(levelStr) && !Strings.isNullOrEmpty(typeStr)) {
                this.cacheId = CpuCacheId.builder()
                    .setLevel(levelStr)
                    .setType(typeStr)
                    .build();
            }

            return this;
        }

        /**
         * Sets CPU cache policy.
         *
         * @param policyStr CPU cache policy as a string
         * @return builder object
         */
        public Builder setPolicy(String policyStr) {
            if (!Strings.isNullOrEmpty(policyStr)) {
                this.policy = CpuCachePlacementPolicy.getByName(policyStr);
            }

            return this;
        }

        /**
         * Sets the CPU cache capacity.
         *
         * @param capacity CPU cache capacity
         * @return builder object
         */
        public Builder setCapacity(long capacity) {
            this.capacity = capacity;
            return this;
        }

        /**
         * Sets the CPU cache sets.
         *
         * @param sets CPU cache sets
         * @return builder object
         */
        public Builder setNumberOfSets(int sets) {
            this.sets = sets;
            return this;
        }

        /**
         * Sets the associativity ways of the CPU cache.
         *
         * @param ways CPU cache ways
         * @return builder object
         */
        public Builder setNumberOfWays(int ways) {
            this.ways = ways;
            return this;
        }

        /**
         * Sets the length of the CPU cache line.
         *
         * @param lineLength CPU cache line length
         * @return builder object
         */
        public Builder setLineLength(int lineLength) {
            this.lineLength = lineLength;
            return this;
        }

        /**
         * Sets whether this CPU cache is shared or not.
         *
         * @param isShared CPU cache sharing status
         * @return builder object
         */
        public Builder isShared(boolean isShared) {
            this.isShared = isShared;
            return this;
        }

        /**
         * Creates a DefaultBasicCpuCacheDevice object.
         *
         * @return DefaultBasicCpuCacheDevice object
         */
        public DefaultBasicCpuCacheDevice build() {
            return new DefaultBasicCpuCacheDevice(
                cacheId, policy, vendor, capacity,
                sets, ways, lineLength, isShared);
        }

    }

}
