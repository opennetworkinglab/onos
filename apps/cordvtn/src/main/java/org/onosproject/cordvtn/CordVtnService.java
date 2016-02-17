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
package org.onosproject.cordvtn;

import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.HostId;

import java.util.Map;

/**
 * Service for provisioning overlay virtual networks on compute nodes.
 */
public interface CordVtnService {

    String CORDVTN_APP_ID = "org.onosproject.cordvtn";

    /**
     * Adds a new VM on a given node and connect point.
     *
     * @param node cordvtn node
     * @param connectPoint connect point
     */
    void addServiceVm(CordVtnNode node, ConnectPoint connectPoint);

    /**
     * Removes a VM from a given node and connect point.
     *
     * @param connectPoint connect point
     */
    void removeServiceVm(ConnectPoint connectPoint);

    /**
     * Creates dependencies for a given tenant service.
     *
     * @param tServiceId id of the service which has a dependency
     * @param pServiceId id of the service which provide dependency
     * @param isBidirectional true to enable bidirectional connectivity between two services
     */
    void createServiceDependency(CordServiceId tServiceId,
                                 CordServiceId pServiceId,
                                 boolean isBidirectional);

    /**
     * Removes all dependencies from a given tenant service.
     *
     * @param tServiceId id of the service which has a dependency
     * @param pServiceId id of the service which provide dependency
     */
    void removeServiceDependency(CordServiceId tServiceId, CordServiceId pServiceId);

    /**
     * Updates virtual service gateways.
     *
     * @param vSgHost host id of vSG host
     * @param serviceVlan service vlan id
     * @param vSgs map of ip and mac address of vSGs running in this vSG host
     */
    void updateVirtualSubscriberGateways(HostId vSgHost, String serviceVlan,
                                         Map<IpAddress, MacAddress> vSgs);
}
