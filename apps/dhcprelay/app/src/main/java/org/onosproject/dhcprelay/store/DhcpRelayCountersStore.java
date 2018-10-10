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


import java.util.Optional;
import java.util.Map;
import java.util.Set;

/**
 * Stores DHCP Relay Counters records.
 */
public interface DhcpRelayCountersStore {

    /**
     * Creates or updates DHCP record for specific host id (mac + vlan).
     *
     * @param counterClass class of counters (direct, indirect, global)
     * @param counterName name of counter
     */
    void incrementCounter(String counterClass, String counterName);

    /**
     * Gets the DHCP counter record for a given counter class.
     *
     * @param counterClass the class of counters (direct, indirect, global)
     * @return the DHCP counter record for a given counter class; empty if record not exists
     */
    Optional<DhcpRelayCounters> getCounters(String counterClass);

    /**
     * Gets all classes of DHCP counters record from store.
     *
     * @return all classes of DHCP counters records from store
     */
    Set<Map.Entry<String, DhcpRelayCounters>> getAllCounters();

    /**
     * Resets counter value for a given counter class.
     *
     * @param counterClass the class of counters (direct, indirect, global)
     */
    void resetCounters(String counterClass);

    /**
     * Resets counter value for a all counter classes.
     *
     */
    void resetAllCounters();

}
