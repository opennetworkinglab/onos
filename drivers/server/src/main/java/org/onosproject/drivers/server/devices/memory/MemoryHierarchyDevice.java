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

package org.onosproject.drivers.server.devices.memory;

import java.util.Collection;

/**
 * Represents an abstraction of a main memory hierarchy.
 */
public interface MemoryHierarchyDevice {

    /**
     * Returns the number of memory modules of this main memory hierarchy.
     *
     * @return number of main memory modules
     */
    int modulesNb();

    /**
     * Returns the capacity of the entire main memory hierarchy in MBytes.
     *
     * @return entire main memory hierarchy's capacity in Mbytes
     */
    long totalCapacity();

    /**
     * Returns the main memory hierarchy.
     *
     * @return main memory hierarchy
     */
    Collection<MemoryModuleDevice> memoryHierarchy();

}
