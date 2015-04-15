/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.net.host;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;

/**
 * Service for administering the inventory of end-station hosts.
 */
public interface HostAdminService extends HostService {

    /**
     * Removes the end-station host with the specified identifier.
     *
     * @param hostId host identifier
     */
    void removeHost(HostId hostId);

    /**
     * Binds IP and MAC addresses to the given connection point.
     * <p>
     * The addresses are added to the set of addresses already bound to the
     * connection point.
     *
     * @param addresses address object containing addresses to add and the port
     * to add them to
     */
    void bindAddressesToPort(PortAddresses addresses);

    /**
     * Removes the addresses contained in the given PortAddresses object from
     * the set of addresses bound to the port.
     *
     * @param portAddresses set of addresses to remove and port to remove them
     * from
     */
    void unbindAddressesFromPort(PortAddresses portAddresses);

    /**
     * Removes all address information for the given connection point.
     *
     * @param connectPoint the connection point to remove address information
     */
    void clearAddresses(ConnectPoint connectPoint);

}
