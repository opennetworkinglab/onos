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
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of kubevirt port; not intended for direct use.
 */
public interface KubevirtPortStore
        extends Store<KubevirtPortEvent, KubevirtPortStoreDelegate> {

    /**
     * Creates a new kubevirt port.
     *
     * @param port kubevirt port
     */
    void createPort(KubevirtPort port);

    /**
     * Updates the kubevirt port.
     *
     * @param port kubevirt port
     */
    void updatePort(KubevirtPort port);

    /**
     * Removes the kubevirt port with the given MAC address.
     *
     * @param mac port MAC address
     * @return removed kubevirt port; null if failed
     */
    KubevirtPort removePort(MacAddress mac);

    /**
     * Returns the kubevirt port with the given port MAC.
     *
     * @param mac port MAC address
     * @return kubevirt port; null if not found
     */
    KubevirtPort port(MacAddress mac);

    /**
     * Returns all kubevirt ports.
     *
     * @return set of kubevirt ports
     */
    Set<KubevirtPort> ports();

    /**
     * Removes all kubevirt ports.
     */
    void clear();
}
