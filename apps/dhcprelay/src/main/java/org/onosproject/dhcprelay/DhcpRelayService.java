/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.dhcprelay;

import org.onosproject.dhcprelay.store.DhcpRecord;
import org.onosproject.net.HostId;

import java.util.Collection;
import java.util.Optional;

public interface DhcpRelayService {
    /**
     * Gets DHCP record for specific host id (mac + vlan).
     *
     * @param hostId the id of host
     * @return the DHCP record of the host
     */
    Optional<DhcpRecord> getDhcpRecord(HostId hostId);

    /**
     * Gets all DHCP records from store.
     *
     * @return all DHCP records from store
     */
    Collection<DhcpRecord> getDhcpRecords();
}
