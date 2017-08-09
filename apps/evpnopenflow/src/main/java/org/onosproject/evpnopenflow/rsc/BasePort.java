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

package org.onosproject.evpnopenflow.rsc;

import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.vtnrsc.AllowedAddressPair;
import org.onosproject.vtnrsc.BindingHostId;
import org.onosproject.vtnrsc.FixedIp;
import org.onosproject.vtnrsc.SecurityGroup;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetworkId;

import java.util.Collection;
import java.util.Set;

/**
 * Representation of a Base port.
 */
public interface BasePort {
    /**
     * Coarse classification of the type of the virtual port.
     */
    enum State {
        /**
         * Signifies that a basePort is currently active,This state mean that
         * this basePort is available.
         */
        ACTIVE,
        /**
         * Signifies that a basePort is currently unavailable.
         */
        DOWN;
    }

    /**
     * Returns the basePort identifier.
     *
     * @return basePort identifier
     */
    BasePortId portId();

    /**
     * Returns the network identifier.
     *
     * @return tenantNetwork identifier
     */
    TenantNetworkId networkId();

    /**
     * Returns the symbolic name for the basePort.
     *
     * @return basePort name
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
    String state();

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
     * Returns the basePort allowedAddressPairs.
     *
     * @return basePort allowedAddressPairs
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
     * Returns the basePort bindinghostId.
     *
     * @return basePort bindinghostId
     */
    BindingHostId bindingHostId();

    /**
     * Returns the basePort bindingVnicType.
     *
     * @return basePort bindingVnicType
     */
    String bindingVnicType();

    /**
     * Returns the basePort bindingVifType.
     *
     * @return basePort bindingVifType
     */
    String bindingVifType();

    /**
     * Returns the basePort bindingvifDetail.
     *
     * @return basePort bindingvifDetail
     */
    String bindingVifDetails();

    /**
     * Returns the security groups.
     *
     * @return port security groups
     */
    Iterable<SecurityGroup> securityGroups();
}
