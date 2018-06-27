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

import org.onosproject.drivers.server.devices.nic.NicDevice;
import org.onosproject.protocol.rest.RestSBDevice;

import java.util.Collection;

/**
 * Represents an abstraction of a REST server device in ONOS.
 */
public interface RestServerSBDevice extends RestSBDevice {
    /**
     * Returns the set of CPUs of the server.
     *
     * @return set of CPUs
     */
    Collection<CpuDevice> cpus();

    /**
     * Returns the number of CPUs of the server.
     *
     * @return number of CPUs
     */
    int numberOfCpus();

    /**
     * Returns the set of NICs of the server.
     *
     * @return set of NICs
     */
    Collection<NicDevice> nics();

    /**
     * Returns the number of NICs of the server.
     *
     * @return number of NICs
     */
    int numberOfNics();

}
