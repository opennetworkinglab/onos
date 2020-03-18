/*
 * Copyright 2014-present Open Networking Foundation
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

import org.onosproject.drivers.server.devices.cpu.CpuCacheHierarchyDevice;
import org.onosproject.drivers.server.devices.cpu.CpuDevice;
import org.onosproject.drivers.server.devices.memory.MemoryHierarchyDevice;
import org.onosproject.drivers.server.devices.nic.NicDevice;
import org.onosproject.drivers.server.devices.ServerDeviceDescription;

import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.SparseAnnotations;

import org.onlab.packet.ChassisId;

import com.google.common.base.Objects;

import java.net.URI;
import java.util.Collection;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.server.Constants.MSG_CPU_CACHE_HIERARCHY_NULL;
import static org.onosproject.drivers.server.Constants.MSG_CPU_LIST_NULL;
import static org.onosproject.drivers.server.Constants.MSG_MEM_HIERARCHY_NULL;
import static org.onosproject.drivers.server.Constants.MSG_NIC_LIST_NULL;
import static org.onosproject.net.Device.Type;

/**
 * Default implementation of immutable server device description entity.
 */
public class DefaultServerDeviceDescription extends DefaultDeviceDescription
        implements ServerDeviceDescription {

    private final Collection<CpuDevice> cpus;
    private final CpuCacheHierarchyDevice caches;
    private final MemoryHierarchyDevice memory;
    private final Collection<NicDevice> nics;

    /**
     * Creates a server device description using the supplied information.
     *
     * @param uri          device URI
     * @param type         device type
     * @param manufacturer device manufacturer
     * @param hwVersion    device HW version
     * @param swVersion    device SW version
     * @param serialNumber device serial number
     * @param chassis      chassis id
     * @param cpus         set of CPUs
     * @param caches       CPU cache hierarchy
     * @param memory       memory hierarchy
     * @param nics         set of network interface cards (NICs)
     * @param annotations  optional key/value annotations map
     */
    public DefaultServerDeviceDescription(
            URI uri, Type type, String manufacturer,
            String hwVersion, String swVersion,
            String serialNumber, ChassisId chassis,
            Collection<CpuDevice> cpus, CpuCacheHierarchyDevice caches,
            MemoryHierarchyDevice memory, Collection<NicDevice> nics,
            SparseAnnotations... annotations) {
        this(uri, type, manufacturer, hwVersion, swVersion, serialNumber,
             chassis, true, cpus, caches, memory, nics, annotations);
    }

    /**
     * Creates a server device description using the supplied information.
     *
     * @param uri              device URI
     * @param type             device type
     * @param manufacturer     device manufacturer
     * @param hwVersion        device HW version
     * @param swVersion        device SW version
     * @param serialNumber     device serial number
     * @param chassis          chassis id
     * @param cpus             set of CPUs
     * @param caches           CPU cache hierarchy
     * @param memory           memory hierarchy
     * @param nics             set of network interface cards (NICs)
     * @param defaultAvailable optional whether device is by default available
     * @param annotations      optional key/value annotations map
     */
    public DefaultServerDeviceDescription(
            URI uri, Type type, String manufacturer,
            String hwVersion, String swVersion,
            String serialNumber, ChassisId chassis,
            boolean defaultAvailable,
            Collection<CpuDevice> cpus, CpuCacheHierarchyDevice caches,
            MemoryHierarchyDevice memory, Collection<NicDevice> nics,
            SparseAnnotations... annotations) {
        super(
            uri, type, manufacturer, hwVersion, swVersion,
            serialNumber, chassis, defaultAvailable, annotations
        );

        checkNotNull(cpus, MSG_CPU_LIST_NULL);
        checkNotNull(caches, MSG_CPU_CACHE_HIERARCHY_NULL);
        checkNotNull(memory, MSG_MEM_HIERARCHY_NULL);
        checkNotNull(nics, MSG_NIC_LIST_NULL);

        this.cpus = cpus;
        this.caches = caches;
        this.memory = memory;
        this.nics = nics;
    }

    /**
     * Creates a server device description using the supplied information.
     * @param base ServerDeviceDescription to basic information
     * @param annotations Annotations to use
     */
    public DefaultServerDeviceDescription(ServerDeviceDescription base,
                                    SparseAnnotations... annotations) {
        this(base.deviceUri(), base.type(), base.manufacturer(),
             base.hwVersion(), base.swVersion(), base.serialNumber(),
             base.chassisId(), base.isDefaultAvailable(),
             base.cpus(), base.caches(), base.memory(), base.nics(),
             annotations);
    }

    /**
     * Creates a device description using the supplied information.
     * @param base ServerDeviceDescription to basic information (except for type)
     * @param type device type
     * @param annotations Annotations to use
     */
    public DefaultServerDeviceDescription(
            ServerDeviceDescription base, Type type,
            SparseAnnotations... annotations) {
        this(base.deviceUri(), type, base.manufacturer(),
             base.hwVersion(), base.swVersion(), base.serialNumber(),
             base.chassisId(), base.isDefaultAvailable(),
             base.cpus(), base.caches(), base.memory(), base.nics(),
             annotations);
    }

    /**
     * Creates a device description using the supplied information.
     *
     * @param base ServerDeviceDescription to basic information (except for defaultAvailable)
     * @param defaultAvailable whether device should be made available by default
     * @param annotations Annotations to use
     */
    public DefaultServerDeviceDescription(
            ServerDeviceDescription base,
            boolean defaultAvailable,
            SparseAnnotations... annotations) {
        this(base.deviceUri(), base.type(), base.manufacturer(),
             base.hwVersion(), base.swVersion(), base.serialNumber(),
             base.chassisId(), defaultAvailable,
             base.cpus(), base.caches(), base.memory(), base.nics(),
             annotations);
    }

    @Override
    public Collection<CpuDevice> cpus() {
        return this.cpus;
    }

    @Override
    public CpuCacheHierarchyDevice caches() {
        return this.caches;
    }

    @Override
    public MemoryHierarchyDevice memory() {
        return this.memory;
    }

    @Override
    public Collection<NicDevice> nics() {
        return this.nics;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("uri",          deviceUri())
                .add("type",         type())
                .add("manufacturer", manufacturer())
                .add("hwVersion",    hwVersion())
                .add("swVersion",    swVersion())
                .add("serial",       serialNumber())
                .add("cpus",         cpus)
                .add("cpuCaches",    caches)
                .add("memory",       memory)
                .add("nics",         nics)
                .add("annotations",  annotations())
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), cpus, caches, memory, nics);
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof DefaultServerDeviceDescription) {
            if (!super.equals(object)) {
                return false;
            }
            DefaultServerDeviceDescription that = (DefaultServerDeviceDescription) object;
            return Objects.equal(this.deviceUri(),          that.deviceUri())
                && Objects.equal(this.type(),               that.type())
                && Objects.equal(this.manufacturer(),       that.manufacturer())
                && Objects.equal(this.hwVersion(),          that.hwVersion())
                && Objects.equal(this.swVersion(),          that.swVersion())
                && Objects.equal(this.serialNumber(),       that.serialNumber())
                && Objects.equal(this.chassisId(),          that.chassisId())
                && Objects.equal(this.cpus(),               that.cpus())
                && Objects.equal(this.caches(),             that.caches())
                && Objects.equal(this.memory(),             that.memory())
                && Objects.equal(this.nics(),               that.nics())
                && Objects.equal(this.isDefaultAvailable(), that.isDefaultAvailable());
        }
        return false;
    }

    // Default constructor for serialization
    DefaultServerDeviceDescription() {
        super(null, null, null, null, null, null, null, (SparseAnnotations[]) null);
        this.cpus = null;
        this.caches = null;
        this.memory = null;
        this.nics = null;
    }

}
