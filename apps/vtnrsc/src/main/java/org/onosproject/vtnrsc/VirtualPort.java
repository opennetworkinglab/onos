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
package org.onosproject.vtnrsc;

import java.util.Collection;
import java.util.Set;

import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;

/**
 * Representation of the VirtualPort.
 */
public interface VirtualPort {
    /**
     * Coarse classification of the type of the virtual port.
     */
    enum State {
        /**
         * Signifies that a virtualPort is currently active,This state mean that
         * this virtualPort is available.
         */
        ACTIVE,
        /**
         * Signifies that a virtualPort is currently unavailable.
         */
        DOWN;
    }

    /**
     * Returns the virtualPort identifier.
     *
     * @return virtualPort identifier
     */
    VirtualPortId portId();

    /**
     * Returns the network identifier.
     *
     * @return tenantNetwork identifier
     */
    TenantNetworkId networkId();

    /**
     * Returns the symbolic name for the virtualPort.
     *
     * @return virtualPort name
     */
    String name();

    /**
     * Returns the administrative status of the port,which is up(true) or
     * down(false).
     *
     * @return true if the administrative status of the port is up
     */
    boolean adminStateUp();

    /**
     * Returns the state.
     *
     * @return state
     */
    State state();

    /**
     * Returns the MAC address.
     *
     * @return MAC Address
     */
    MacAddress macAddress();

    /**
     * Returns the port tenantId.
     *
     * @return port tenantId
     */
    TenantId tenantId();

    /**
     * Returns the device identifier.
     *
     * @return deviceId
     */
    DeviceId deviceId();

    /**
     * Returns the identifier of the entity that uses this port.
     *
     * @return deviceOwner
     */
    String deviceOwner();

    /**
     * Returns the virtualPort allowedAddressPairs.
     *
     * @return virtualPort allowedAddressPairs
     */
    Collection<AllowedAddressPair> allowedAddressPairs();

    /**
     * Returns set of IP addresses for the port, include the IP addresses and subnet
     * identity.
     *
     * @return FixedIps Set of fixedIp
     */
    Set<FixedIp> fixedIps();

    /**
     * Returns the virtualPort bindinghostId.
     *
     * @return virtualPort bindinghostId
     */
    BindingHostId bindingHostId();

    /**
     * Returns the virtualPort bindingVnicType.
     *
     * @return virtualPort bindingVnicType
     */
    String bindingVnicType();

    /**
     * Returns the virtualPort bindingVifType.
     *
     * @return virtualPort bindingVifType
     */
    String bindingVifType();

    /**
     * Returns the virtualPort bindingvifDetail.
     *
     * @return virtualPort bindingvifDetail
     */
    String bindingVifDetails();

    /**
     * Returns the security groups.
     *
     * @return port security groups
     */
    Iterable<SecurityGroup> securityGroups();
}
