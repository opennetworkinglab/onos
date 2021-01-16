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
import org.onosproject.event.ListenerService;

import java.util.Set;

/**
 * Service for interacting with the inventory of kubevirt port.
 */
public interface KubevirtPortService
        extends ListenerService<KubevirtPortEvent, KubevirtPortListener> {

    /**
     * Returns the kubevirt port with the supplied MAC address.
     *
     * @param mac MAC address
     * @return kubevirt port
     */
    KubevirtPort port(MacAddress mac);

    /**
     * Returns the kubevirt ports belongs to the given network.
     *
     * @param networkId network identifier
     * @return kubevirt ports
     */
    Set<KubevirtPort> ports(String networkId);

    /**
     * Returns all kubevirt ports registered.
     *
     * @return set of kubevirt ports
     */
    Set<KubevirtPort> ports();
}
