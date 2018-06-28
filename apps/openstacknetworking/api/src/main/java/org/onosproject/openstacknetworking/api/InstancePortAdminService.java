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

/**
 * Service for administering the inventory of OpenStack instance port.
 */
public interface InstancePortAdminService extends InstancePortService {

    /**
     * Creates an instance port with the given information.
     *
     * @param instancePort instance port
     */
    void createInstancePort(InstancePort instancePort);

    /**
     * Updates the instance port with the given information.
     *
     * @param instancePort the updated instance port
     */
    void updateInstancePort(InstancePort instancePort);

    /**
     * Removes the instance port with the given port identifier.
     *
     * @param portId port identifier
     */
    void removeInstancePort(String portId);

    /**
     * Clears the existing instance port.
     */
    void clear();
}
