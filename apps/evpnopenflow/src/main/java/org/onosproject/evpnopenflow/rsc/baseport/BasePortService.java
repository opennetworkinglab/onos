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

package org.onosproject.evpnopenflow.rsc.baseport;

import com.fasterxml.jackson.databind.JsonNode;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.evpnopenflow.rsc.BasePort;
import org.onosproject.evpnopenflow.rsc.BasePortId;
import org.onosproject.net.DeviceId;
import org.onosproject.vtnrsc.FixedIp;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.TenantNetworkId;

import java.util.Collection;


/**
 * Service for interacting with the inventory of basePort.
 */
public interface BasePortService {
    /**
     * Returns if the basePort is existed.
     *
     * @param basePortId basePort identifier
     * @return true or false if one with the given identifier is not existed.
     */
    boolean exists(BasePortId basePortId);

    /**
     * Returns the basePort with the identifier.
     *
     * @param basePortId basePort ID
     * @return BasePort or null if one with the given ID is not know.
     */
    BasePort getPort(BasePortId basePortId);

    /**
     * Returns the basePort associated with the fixedIP.
     *
     * @param fixedIP the fixedIP identifier
     * @return basePort.
     */
    BasePort getPort(FixedIp fixedIP);

    /**
     * Returns the basePort associated with the mac address.
     *
     * @param mac the mac address
     * @return basePort.
     */
    BasePort getPort(MacAddress mac);

    /**
     * Returns the basePort associated with the networkId and ip.
     *
     * @param networkId the TenantNetworkId identifier
     * @param ip        the ip identifier
     * @return basePort.
     */
    BasePort getPort(TenantNetworkId networkId, IpAddress ip);

    /**
     * Returns the collection of the currently known basePort.
     *
     * @return collection of BasePort.
     */
    Collection<BasePort> getPorts();

    /**
     * Returns the collection of the basePorts associated with the networkId.
     *
     * @param networkId the network identifer
     * @return collection of basePort.
     */
    Collection<BasePort> getPorts(TenantNetworkId networkId);

    /**
     * Returns the collection of the basePorts associated with the tenantId.
     *
     * @param tenantId the tenant identifier
     * @return collection of basePorts.
     */
    Collection<BasePort> getPorts(TenantId tenantId);

    /**
     * Returns the collection of the basePorts associated with the deviceId.
     *
     * @param deviceId the device identifier
     * @return collection of basePort.
     */
    Collection<BasePort> getPorts(DeviceId deviceId);

    /**
     * Creates basePorts by basePorts.
     *
     * @param basePorts the iterable collection of basePorts
     * @return true if all given identifiers created successfully.
     */
    boolean createPorts(Iterable<BasePort> basePorts);

    /**
     * Updates basePorts by basePorts.
     *
     * @param basePorts the iterable  collection of basePorts
     * @return true if all given identifiers updated successfully.
     */
    boolean updatePorts(Iterable<BasePort> basePorts);

    /**
     * Deletes basePortIds by basePortIds.
     *
     * @param basePortIds the iterable collection of basePort identifiers
     * @return true or false if one with the given identifier to delete is
     * successfully.
     */
    boolean removePorts(Iterable<BasePortId> basePortIds);

    /**
     * process gluon config for vpn port information.
     *
     * @param action can be either update or delete
     * @param key    can contain the id and also target information
     * @param value  content of the vpn port configuration
     */
    void processGluonConfig(String action, String key, JsonNode value);

    /**
     * Adds the specified listener to Vpn Port manager.
     *
     * @param listener Vpn Port listener
     */
    void addListener(BasePortListener listener);

    /**
     * Removes the specified listener to Vpn Port manager.
     *
     * @param listener Vpn Port listener
     */
    void removeListener(BasePortListener listener);
}
