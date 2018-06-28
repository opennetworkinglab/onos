/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.api;

import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of instance port; not intended for direct use.
 */
public interface InstancePortStore
        extends Store<InstancePortEvent, InstancePortStoreDelegate> {

    /**
     * Creates a new instance port.
     *
     * @param port a new instance port
     */
    void createInstancePort(InstancePort port);

    /**
     * Updates the existing instance port.
     *
     * @param port the existing instance port
     */
    void updateInstancePort(InstancePort port);

    /**
     * Removes the existing instance port.
     *
     * @param portId instance port identifier
     * @return the removed instance port
     */
    InstancePort removeInstancePort(String portId);

    /**
     * Obtains the existing instance port.
     *
     * @param portId instance port identifier
     * @return queried instance port
     */
    InstancePort instancePort(String portId);

    /**
     * Obtains a collection of all of instance ports.
     *
     * @return a collection of all of instance ports
     */
    Set<InstancePort> instancePorts();

    /**
     * Removes all instance ports.
     */
    void clear();
}
