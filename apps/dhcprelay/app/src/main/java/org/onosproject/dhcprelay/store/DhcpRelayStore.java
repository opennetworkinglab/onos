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

package org.onosproject.dhcprelay.store;

import org.onosproject.net.HostId;
import org.onosproject.store.Store;
import org.onosproject.store.StoreDelegate;

import java.util.Collection;
import java.util.Optional;

/**
 * Stores DHCP records which relay-ed by DHCP relay application.
 */
public interface DhcpRelayStore extends Store<DhcpRelayStoreEvent, StoreDelegate<DhcpRelayStoreEvent>> {

    /**
     * Creates or updates DHCP record for specific host id (mac + vlan).
     *
     * @param hostId the id of host
     * @param dhcpRecord the DHCP record to update
     */
    void updateDhcpRecord(HostId hostId, DhcpRecord dhcpRecord);

    /**
     * Gets DHCP record for specific host id (mac + vlan).
     *
     * @param hostId the id of host
     * @return the DHCP record of the host; empty if record not exists
     */
    Optional<DhcpRecord> getDhcpRecord(HostId hostId);

    /**
     * Gets all DHCP records from store.
     *
     * @return all DHCP records from store
     */
    Collection<DhcpRecord> getDhcpRecords();

    /**
     * Removes record for specific host id (mac + vlan).
     *
     * @param hostId the id of host
     * @return the DHCP record of the host; empty if record not exists
     */
    Optional<DhcpRecord> removeDhcpRecord(HostId hostId);
}
