/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.vtnrsc.virtualport;

import java.util.Collection;

import org.onosproject.net.DeviceId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetworkId;
import org.onosproject.vtnrsc.VirtualPort;
import org.onosproject.vtnrsc.VirtualPortId;

/**
 * Service for interacting with the inventory of virtualPort.
 */
public interface VirtualPortService {
    /**
     * Returns if the virtualPort is existed.
     *
     * @param virtualPortId virtualPort identifier
     * @return true or false if one with the given identifier is not existed.
     */
    boolean exists(VirtualPortId virtualPortId);

    /**
     * Returns the virtualPort with the identifier.
     *
     * @param virtualPortId virtualPort ID
     * @return VirtualPort or null if one with the given ID is not know.
     */
    VirtualPort getPort(VirtualPortId virtualPortId);

    /**
     * Returns the collection of the currently known virtualPort.
     * @return collection of VirtualPort.
     */
    Collection<VirtualPort> getPorts();

    /**
     * Returns the collection of the virtualPorts associated with the networkId.
     *
     * @param networkId  the network identifer
     * @return collection of virtualPort.
     */
    Collection<VirtualPort> getPorts(TenantNetworkId networkId);

    /**
     * Returns the collection of the virtualPorts associated with the tenantId.
     *
     * @param tenantId   the tenant identifier
     * @return collection of virtualPorts.
     */
    Collection<VirtualPort> getPorts(TenantId tenantId);

    /**
     * Returns the collection of the virtualPorts associated with the deviceId.
     *
     * @param deviceId   the device identifier
     * @return collection of virtualPort.
     */
    Collection<VirtualPort> getPorts(DeviceId deviceId);

    /**
     * Creates virtualPorts by virtualPorts.
     *
     * @param virtualPorts the iterable collection of virtualPorts
     * @return true if all given identifiers created successfully.
     */
    boolean createPorts(Iterable<VirtualPort> virtualPorts);

    /**
     * Updates virtualPorts by virtualPorts.
     *
     * @param virtualPorts the iterable  collection of virtualPorts
     * @return true if all given identifiers updated successfully.
     */
    boolean updatePorts(Iterable<VirtualPort> virtualPorts);

    /**
     * Deletes virtualPortIds by virtualPortIds.
     *
     * @param virtualPortIds the iterable collection of virtualPort identifiers
     * @return true or false if one with the given identifier to delete is
     *         successfully.
     */
    boolean removePorts(Iterable<VirtualPortId> virtualPortIds);
}
