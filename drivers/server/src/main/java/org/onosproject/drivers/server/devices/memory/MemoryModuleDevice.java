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

/**
 * Represents an abstraction of a main memory device.
 */
public interface MemoryModuleDevice {

    /**
     * Maximum main memory speed in million transfers per second (MT/s).
     */
    static final long MAX_SPEED_MTS = 8000;

    /**
     * Returns the type of this main memory module.
     *
     * @return main memory type
     */
    MemoryType type();

    /**
     * Returns the manufacturer of this main memory module.
     *
     * @return main memory manufacturer
     */
    String manufacturer();

    /**
     * Returns the serial number of this main memory module.
     *
     * @return main memory serial number
     */
    String serialNumber();

    /**
     * Returns the data width (in bits) of this main memory module.
     *
     * @return data width in bits
     */
    int dataWidth();

    /**
     * Returns the total width (in bits) of this main memory module.
     *
     * @return total width in bits
     */
    int totalWidth();

    /**
     * Returns the capacity (in MBytes) of this main memory module.
     *
     * @return capacity in MBytes
     */
    long capacity();

    /**
     * Returns the speed of this main memory module in MT/s.
     *
     * @return main memory speed in MT/s
     */
    long speed();

    /**
     * Returns the configuted speed of this main memory in MT/s.
     *
     * @return configured main memory speed in MT/s
     */
    long configuredSpeed();

}
