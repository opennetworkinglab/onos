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

package org.onosproject.drivers.server.devices;

import org.onosproject.drivers.server.devices.cpu.CpuCacheHierarchyDevice;
import org.onosproject.drivers.server.devices.cpu.CpuDevice;
import org.onosproject.drivers.server.devices.memory.MemoryHierarchyDevice;
import org.onosproject.drivers.server.devices.nic.NicDevice;
import org.onosproject.net.device.DeviceDescription;

import java.util.Collection;

/**
 * Carrier of immutable information about a server device.
 */
public interface ServerDeviceDescription extends DeviceDescription {

    /**
     * The set of CPUs of the server device.
     *
     * @return set of CPUs of the server device
     */
    Collection<CpuDevice> cpus();

    /**
     * The CPU cache hierarchy of the server device.
     *
     * @return CPU cache hierarchy of the server device
     */
    CpuCacheHierarchyDevice caches();

    /**
     * The memory hierarchy of the server device.
     *
     * @return memory hierarchy of the server device
     */
    MemoryHierarchyDevice memory();

    /**
     * The set of NICs of the server device.
     *
     * @return set of NICs of the server device
     */
    Collection<NicDevice> nics();

}
