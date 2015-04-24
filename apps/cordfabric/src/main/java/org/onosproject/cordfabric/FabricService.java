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

package org.onosproject.cordfabric;

import com.google.common.collect.Multimap;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;

import java.util.List;

/**
 * Service used to interact with fabric.
 */
public interface FabricService {

    /**
     * Remaps a vlan to the specified ports. The specified ports will be the
     * only ports in this vlan once the operation completes.
     *
     * @param vlanId vlan ID to add/modify
     * @param ports list of ports to add to the vlan
     */
    void addVlan(VlanId vlanId, List<ConnectPoint> ports);

    /**
     * Removes a vlan from all ports in the fabric.
     *
     * @param vlanId ID of vlan to remove
     */
    void removeVlan(VlanId vlanId);

    /**
     * Returns the vlan to port mapping for all vlans/ports configured in the
     * fabric.
     *
     * @return mapping of vlan to port
     */
    Multimap<VlanId, ConnectPoint> getVlans();
}
