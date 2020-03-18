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

import org.onosproject.drivers.server.devices.memory.MemoryHierarchyDevice;
import org.onosproject.drivers.server.devices.memory.MemoryModuleDevice;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.drivers.server.Constants.MSG_MEM_HIERARCHY_EMPTY;
import static org.onosproject.drivers.server.Constants.MSG_MEM_MODULE_NULL;
import static org.onosproject.drivers.server.Constants.MSG_MEM_CAPACITY_NEGATIVE;

/**
 * Default implementation for main memory hierarchy devices.
 */
public final class DefaultMemoryHierarchyDevice implements MemoryHierarchyDevice {

    private final long totalCapacity;
    private Collection<MemoryModuleDevice> memoryHierarchy = null;

    private DefaultMemoryHierarchyDevice(long totalCapacity,
                                         Collection<MemoryModuleDevice> memoryHierarchy) {
        checkArgument(totalCapacity > 0, MSG_MEM_CAPACITY_NEGATIVE);
        checkArgument(!memoryHierarchy.isEmpty(), MSG_MEM_HIERARCHY_EMPTY);

        this.totalCapacity = totalCapacity;
        this.memoryHierarchy = memoryHierarchy;
    }

    /**
     * Creates a builder for DefaultMemoryHierarchyDevice object.
     *
     * @return builder object for DefaultMemoryHierarchyDevice object
     */
    public static DefaultMemoryHierarchyDevice.Builder builder() {
        return new Builder();
    }

    @Override
    public int modulesNb() {
        return this.memoryHierarchy.size();
    }

    @Override
    public long totalCapacity() {
        return this.totalCapacity;
    }

    @Override
    public Collection<MemoryModuleDevice> memoryHierarchy() {
        return this.memoryHierarchy;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("modulesNb", modulesNb())
                .add("totalCapacity", totalCapacity())
                .add("memoryHierarchy", memoryHierarchy())
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof MemoryHierarchyDevice)) {
            return false;
        }
        MemoryHierarchyDevice device = (MemoryHierarchyDevice) obj;
        return  this.modulesNb() ==  device.modulesNb() &&
                this.totalCapacity() == device.totalCapacity() &&
                this.memoryHierarchy() == device.memoryHierarchy();
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalCapacity, memoryHierarchy);
    }

    public static final class Builder {
        long totalCapacity = 0;
        Collection<MemoryModuleDevice> memoryHierarchy = Sets.newHashSet();

        private Builder() {

        }

        /**
         * Adds a new memory module into the hierarchy.
         *
         * @param moduleDev a memory module to add into the hierarchy
         * @return builder object
         */
        public Builder addMemoryModule(MemoryModuleDevice moduleDev) {
            checkNotNull(moduleDev, MSG_MEM_MODULE_NULL);
            this.memoryHierarchy.add(moduleDev);
            this.totalCapacity += moduleDev.capacity();

            return this;
        }

        /**
         * Creates a DefaultMemoryHierarchyDevice object.
         *
         * @return DefaultMemoryHierarchyDevice object
         */
        public DefaultMemoryHierarchyDevice build() {
            return new DefaultMemoryHierarchyDevice(totalCapacity, memoryHierarchy);
        }

    }

}
