/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnrsc.floatingip;

import java.util.Collection;

import org.onlab.packet.IpAddress;
import org.onosproject.vtnrsc.FloatingIp;
import org.onosproject.vtnrsc.FloatingIpId;
import org.onosproject.vtnrsc.TenantId;

/**
 * Service for interacting with the inventory of floating IP.
 */
public interface FloatingIpService {
    /**
     * Returns exists or not of specific floatingIp identifier.
     *
     * @param floatingIpId floatingIp identifier
     * @return true or false
     */
    boolean exists(FloatingIpId floatingIpId);

    /**
     * Returns is used or not of specific floating IP address.
     *
     * @param floatingIpAddr floatingIp address
     * @param floatingIpId floatingIp identifier
     * @return true or false
     */
    boolean floatingIpIsUsed(IpAddress floatingIpAddr, FloatingIpId floatingIpId);

    /**
     * Returns is used or not of specific fixed IP address.
     *
     * @param fixedIpAddr fixedIp address
     * @param tenantId the tenant identifier of floating IP
     * @param floatingIpId floatingIp identifier
     * @return true or false
     */
    boolean fixedIpIsUsed(IpAddress fixedIpAddr, TenantId tenantId, FloatingIpId floatingIpId);

    /**
     * Returns a collection of the currently known floating IP.
     *
     * @return collection of floating IP
     */
    Collection<FloatingIp> getFloatingIps();

    /**
     * Returns the floatingIp with the specified identifier.
     *
     * @param floatingIpId floatingIp identifier
     * @return floatingIp or null if one with the given identifier is not known
     */
    FloatingIp getFloatingIp(FloatingIpId floatingIpId);

    /**
     * Creates new floatingIps.
     *
     * @param floatingIps the collection of floatingIp
     * @return true if the identifier floatingIp has been created right
     */
    boolean createFloatingIps(Collection<FloatingIp> floatingIps);

    /**
     * Updates existing floatingIps.
     *
     * @param floatingIps the collection of floatingIp
     * @return true if all floatingIp were updated successfully
     */
    boolean updateFloatingIps(Collection<FloatingIp> floatingIps);

    /**
     * Removes the specified floatingIp from the store.
     *
     * @param floatingIpIds the collection of floatingIp identifier
     * @return true if remove identifier floatingIp successfully
     */
    boolean removeFloatingIps(Collection<FloatingIpId> floatingIpIds);

    /**
     * Adds the specified listener to floating Ip manager.
     *
     * @param listener floating Ip listener
     */
    void addListener(FloatingIpListener listener);

    /**
     * Removes the specified listener to floating Ip manager.
     *
     * @param listener floating Ip listener
     */
    void removeListener(FloatingIpListener listener);
}
