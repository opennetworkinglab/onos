/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.incubator.net.virtual;

import com.google.common.annotations.Beta;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.core.ApplicationId;
import org.onosproject.event.ListenerService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.TenantId;

import java.util.Set;

/**
 * Service for querying virtual network inventory.
 */
@Beta
public interface VirtualNetworkService
        extends ListenerService<VirtualNetworkEvent, VirtualNetworkListener> {

    /**
     * The topic used for obtaining globally unique ids.
     */
    String VIRTUAL_NETWORK_TOPIC = "virtual-network-ids";

    /**
     * Returns a collection of all virtual networks created on behalf of the
     * specified tenant.
     *
     * @param tenantId tenant identifier
     * @return collection of networks
     * @throws org.onlab.util.ItemNotFoundException if no such network found
     */
    Set<VirtualNetwork> getVirtualNetworks(TenantId tenantId);

    /**
     * Returns the virtual network matching the network identifier.
     *
     * @param networkId virtual network identifier
     * @return virtual network instance
     * @throws org.onlab.util.ItemNotFoundException if no such network found
     */
    VirtualNetwork getVirtualNetwork(NetworkId networkId);

    /**
     * Returns {@code tenantId} for specified virtual network id.
     *
     * @param networkId virtual network identifier
     * @return tenantId tenant identifier
     * @throws org.onlab.util.ItemNotFoundException if no such network found
     */
    TenantId getTenantId(NetworkId networkId);

    /**
     * Returns a collection of all virtual devices in the specified network.
     *
     * @param networkId network identifier
     * @return collection of devices
     * @throws org.onlab.util.ItemNotFoundException if no such network found
     */
    Set<VirtualDevice> getVirtualDevices(NetworkId networkId);

    /**
     * Returns a collection of all virtual hosts in the specified network.
     *
     * @param networkId network identifier
     * @return collection of hosts
     * @throws org.onlab.util.ItemNotFoundException if no such network found
     */
    Set<VirtualHost> getVirtualHosts(NetworkId networkId);

    /**
     * Returns collection of all virtual links in the specified network.
     *
     * @param networkId network identifier
     * @return collection of links
     * @throws org.onlab.util.ItemNotFoundException if no such network found
     */
    Set<VirtualLink> getVirtualLinks(NetworkId networkId);

    /**
     * Returns list of all virtual ports of the specified device. If the
     * device identifier is null then all of the virtual ports in the specified
     * network will be returned.
     *
     * @param networkId network identifier
     * @param deviceId  device identifier
     * @return list of ports
     * @throws org.onlab.util.ItemNotFoundException if no such network found
     */
    Set<VirtualPort> getVirtualPorts(NetworkId networkId, DeviceId deviceId);

    /**
     * Returns list of physical device identifier mapping with the virtual
     * device in the specified network. The physical devices are specified by
     * port mapping mechanism.
     *
     * @param networkId network identifier
     * @param deviceId the virtual device identifier
     * @return collection of the specified device's identifier
     */
    Set<DeviceId> getPhysicalDevices(NetworkId networkId, DeviceId deviceId);

    /**
     * Returns implementation of the specified service class for operating
     * in the context of the given network.
     * <p>
     * The following services will be available:
     * <ul>
     * <li>{@link org.onosproject.net.device.DeviceService}</li>
     * <li>{@link org.onosproject.net.link.LinkService}</li>
     * <li>{@link org.onosproject.net.host.HostService}</li>
     * <li>{@link org.onosproject.net.topology.TopologyService}</li>
     * <li>{@link org.onosproject.net.topology.PathService}</li>
     * <li>{@link org.onosproject.net.packet.PacketService}</li>
     * <li>{@link org.onosproject.net.flow.FlowRuleService}</li>
     * <li>{@link org.onosproject.net.flowobjective.FlowObjectiveService}</li>
     * <li>{@link org.onosproject.net.intent.IntentService}</li>
     * <li>{@link org.onosproject.mastership.MastershipService}</li>
     * <li>{@link org.onosproject.mastership.MastershipAdminService}</li>
     * <li>{@link org.onosproject.mastership.MastershipTermService}</li>
     * </ul>
     *
     * @param networkId    network identifier
     * @param serviceClass service class
     * @param <T>          type of service
     * @return implementation class
     * @throws org.onlab.util.ItemNotFoundException    if no such network found
     * @throws org.onlab.osgi.ServiceNotFoundException if no implementation found
     */
    <T> T get(NetworkId networkId, Class<T> serviceClass);

    /**
     * Returns service directory.
     *
     * @return a service directory
     */
    ServiceDirectory getServiceDirectory();

    /**
     * Returns the application identifier for a virtual network.
     *
     * @param networkId network identifier
     * @return an representative application identifier for a virtual network
     */
    ApplicationId getVirtualNetworkApplicationId(NetworkId networkId);
}
