/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.api;

import org.onlab.packet.MacAddress;

public interface KubevirtPortAdminService extends KubevirtPortService {

    /**
     * Creates a kubevirt port with the given information.
     *
     * @param port a new kubevirt port
     */
    void createPort(KubevirtPort port);

    /**
     * Updates the kubevirt port with the given information.
     *
     * @param port the updated kubevirt port
     */
    void updatePort(KubevirtPort port);

    /**
     * Removes the kubevirt port.
     *
     * @param mac MAC address bound with the port
     */
    void removePort(MacAddress mac);

    /**
     * Clears the existing ports.
     */
    void clear();
}
