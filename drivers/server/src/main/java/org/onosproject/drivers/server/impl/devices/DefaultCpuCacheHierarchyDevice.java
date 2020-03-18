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
import org.onosproject.drivers.server.devices.cpu.CpuCacheHierarchyDevice;
import org.onosproject.drivers.server.devices.cpu.CpuCacheId;
import org.onosproject.drivers.server.devices.cpu.CpuCoreId;
import org.onosproject.drivers.server.devices.cpu.CpuVendor;

import com.google.common.base.MoreObjects;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.drivers.server.Constants.MSG_CPU_CACHE_CAPACITY_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_CPU_CACHE_CAPACITY_CORE_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_CPU_CACHE_CAPACITY_LLC_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_CPU_CACHE_CAPACITY_TOTAL_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_CPU_CACHE_HIERARCHY_NULL;
import static org.onosproject.drivers.server.Constants.MSG_CPU_CACHE_INSERTION_FAILED;
import static org.onosproject.drivers.server.Constants.MSG_CPU_CACHE_LEVELS_EXCEEDED;
import static org.onosproject.drivers.server.Constants.MSG_CPU_CACHE_LEVELS_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_CPU_CORES_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_CPU_SOCKETS_NEGATIVE;
import static org.onosproject.drivers.server.Constants.MSG_CPU_VENDOR_NULL;

/**
 * Default implementation for a CPU cache hierarchy.
 */
public final class DefaultCpuCacheHierarchyDevice implements CpuCacheHierarchyDevice {

    private final CpuVendor vendor;
    private final int socketsNb;
    private final int coresNb;
    private final int levels;
    private final long perCoreCapacity;
    private final long llcCapacity;
    private final long totalCapacity;
    private Map<CpuCacheId, BasicCpuCacheDevice> cacheHierarchy;

    /**
     * Maximum number of CPU cache levels.
     */
    public static final int MAX_CPU_CACHE_LEVELS = 4;

    private DefaultCpuCacheHierarchyDevice(
                CpuVendor vendor,
                int socketsNb,
                int coresNb,
                int levels,
                long perCoreCapacity,
                long llcCapacity,
                Map<CpuCacheId, BasicCpuCacheDevice> cacheHierarchy) {
        checkNotNull(vendor, MSG_CPU_VENDOR_NULL);
        checkArgument(socketsNb > 0, MSG_CPU_SOCKETS_NEGATIVE);
        checkArgument((coresNb > 0) && (coresNb < CpuCoreId.MAX_CPU_CORE_NB),
            MSG_CPU_CORES_NEGATIVE);
        checkArgument((levels > 0) && (levels <= MAX_CPU_CACHE_LEVELS),
            MSG_CPU_CACHE_LEVELS_NEGATIVE);
        checkArgument(perCoreCapacity > 0, MSG_CPU_CACHE_CAPACITY_CORE_NEGATIVE);
        checkArgument(llcCapacity > 0, MSG_CPU_CACHE_CAPACITY_LLC_NEGATIVE);
        checkNotNull(cacheHierarchy, MSG_CPU_CACHE_HIERARCHY_NULL);

        this.vendor = vendor;
        this.socketsNb = socketsNb;
        this.coresNb = coresNb;
        this.levels = levels;
        this.perCoreCapacity = perCoreCapacity;
        this.llcCapacity = llcCapacity;

        // The total capacity is the sum of the (shared) socket-level and the per-core capacities
        this.totalCapacity = (socketsNb * llcCapacity) + (coresNb * perCoreCapacity);
        checkArgument((this.totalCapacity > this.perCoreCapacity) &&
                      (this.totalCapacity > this.llcCapacity),
                      MSG_CPU_CACHE_CAPACITY_TOTAL_NEGATIVE);
        this.cacheHierarchy = cacheHierarchy;
    }

    /**
     * Creates a builder for DefaultCpuCacheHierarchyDevice object.
     *
     * @return builder object for DefaultCpuCacheHierarchyDevice object
     */
    public static DefaultCpuCacheHierarchyDevice.Builder builder() {
        return new Builder();
    }

    @Override
    public CpuVendor vendor() {
        return this.vendor;
    }

    @Override
    public int socketsNb() {
        return this.socketsNb;
    }

    @Override
    public int coresNb() {
        return this.coresNb;
    }

    @Override
    public int levels() {
        return this.levels;
    }

    @Override
    public long perCoreCapacity() {
        return this.perCoreCapacity;
    }

    @Override
    public long llcCapacity() {
        return this.llcCapacity;
    }

    @Override
    public long totalCapacity() {
        return this.totalCapacity;
    }

    @Override
    public Map<CpuCacheId, BasicCpuCacheDevice> cacheHierarchy() {
        return cacheHierarchy;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("vendor", vendor())
                .add("socketsNb", socketsNb())
                .add("coresNb", coresNb())
                .add("levels", levels())
                .add("perCoreCapacity", perCoreCapacity())
                .add("llcCapacity", llcCapacity())
                .add("totalCapacity", totalCapacity())
                .add("cacheHierarchy", cacheHierarchy())
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CpuCacheHierarchyDevice)) {
            return false;
        }
        CpuCacheHierarchyDevice device = (CpuCacheHierarchyDevice) obj;
        return  this.vendor() == device.vendor() &&
                this.socketsNb() ==  device.socketsNb() &&
                this.coresNb() ==  device.coresNb() &&
                this.levels() ==  device.levels() &&
                this.perCoreCapacity() == device.perCoreCapacity() &&
                this.llcCapacity() == device.llcCapacity() &&
                this.totalCapacity() == device.totalCapacity() &&
                this.cacheHierarchy() == device.cacheHierarchy();
    }

    @Override
    public int hashCode() {
        return Objects.hash(vendor, socketsNb, coresNb, levels, perCoreCapacity,
            llcCapacity, totalCapacity, cacheHierarchy);
    }

    public static final class Builder {

        CpuVendor vendor = null;
        int socketsNb = -1;
        int coresNb = -1;
        int levels = -1;
        long perCoreCapacity = 0;
        long llcCapacity = 0;
        Map<CpuCacheId, BasicCpuCacheDevice> cacheHierarchy =
            new HashMap<CpuCacheId, BasicCpuCacheDevice>();

        private Builder() {

        }

        /**
         * Sets the number of CPU sockets.
         *
         * @param socketsNb number of CPU sockets
         * @return builder object
         */
        public Builder setSocketsNumber(int socketsNb) {
            this.socketsNb = socketsNb;
            return this;
        }

        /**
         * Sets the number of CPU cores.
         *
         * @param coresNb number of CPU cores
         * @return builder object
         */
        public Builder setCoresNumber(int coresNb) {
            this.coresNb = coresNb;
            return this;
        }

        /**
         * Sets the number of CPU cache levels.
         *
         * @param levels number of CPU cache levels
         * @return builder object
         */
        public Builder setLevels(int levels) {
            this.levels = levels;
            return this;
        }

        /**
         * Add a basic CPU cache device into this hierarchy.
         *
         * @param cacheDev a new basic CPU cache device
         * @return builder object
         */
        public Builder addBasicCpuCacheDevice(BasicCpuCacheDevice cacheDev) {
            checkNotNull(cacheDev, "Basic CPU cache device is null");
            int currentSize = this.cacheHierarchy.size();
            this.cacheHierarchy.put(cacheDev.cacheId(), cacheDev);
            checkArgument(this.cacheHierarchy.size() == currentSize + 1,
                MSG_CPU_CACHE_INSERTION_FAILED);
            checkArgument(this.cacheHierarchy.size() <= MAX_CPU_CACHE_LEVELS,
                MSG_CPU_CACHE_LEVELS_EXCEEDED);

            if (this.vendor == null) {
                this.vendor = cacheDev.vendor();
            }

            long capacity = cacheDev.capacity();
            checkArgument(capacity > 0, MSG_CPU_CACHE_CAPACITY_NEGATIVE);
            if (cacheDev.isShared()) {
                this.llcCapacity = capacity;
            } else {
                this.perCoreCapacity += capacity;
            }

            return this;
        }

        /**
         * Creates a DefaultCpuCacheHierarchyDevice object.
         *
         * @return DefaultCpuCacheHierarchyDevice object
         */
        public DefaultCpuCacheHierarchyDevice build() {
            return new DefaultCpuCacheHierarchyDevice(
                vendor, socketsNb, coresNb, levels,
                perCoreCapacity, llcCapacity, cacheHierarchy);
        }

    }

}
