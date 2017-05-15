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
package org.onosproject.segmentrouting;

import org.onlab.packet.IpPrefix;
import org.onosproject.net.DeviceId;
import org.onosproject.segmentrouting.storekey.NeighborSetNextObjectiveStoreKey;

import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Segment Routing Service for REST API.
 */
public interface SegmentRoutingService {
    /**
     * VLAN cross-connect priority.
     */
    int XCONNECT_PRIORITY = 1000;

    /**
     * Default flow priority.
     */
    int DEFAULT_PRIORITY = 100;

    /**
     * Minimum IP priority.
     *
     * Should &lt; 0 such that priority of /0 will not conflict with lowest
     * priority default entries.
     */
    int MIN_IP_PRIORITY = 10;

    /**
     * Subnet flooding flow priority.
     */
    int FLOOD_PRIORITY = 5;

    /**
     * Returns all tunnels.
     *
     * @return list of tunnels
     */
    List<Tunnel> getTunnels();

    /**
     * Creates a tunnel.
     *
     * @param tunnel tunnel reference to create
     * @return WRONG_PATH if the tunnel path is wrong, ID_EXISTS if the tunnel ID
     * exists already, TUNNEL_EXISTS if the same tunnel exists, INTERNAL_ERROR
     * if the tunnel creation failed internally, SUCCESS if the tunnel is created
     * successfully
     */
    TunnelHandler.Result createTunnel(Tunnel tunnel);

    /**
     * Returns all policies.
     *
     * @return list of policy
     */
    List<Policy> getPolicies();

    /**
     * Creates a policy.
     *
     * @param policy policy reference to create
     * @return ID_EXISTS if the same policy ID exists,
     *  POLICY_EXISTS if the same policy exists, TUNNEL_NOT_FOUND if the tunnel
     *  does not exists, UNSUPPORTED_TYPE if the policy type is not supported,
     *  SUCCESS if the policy is created successfully.
     */
    PolicyHandler.Result createPolicy(Policy policy);

    /**
     * Removes a tunnel.
     *
     * @param tunnel tunnel reference to remove
     * @return TUNNEL_NOT_FOUND if the tunnel to remove does not exists,
     * INTERNAL_ERROR if the tunnel creation failed internally, SUCCESS
     * if the tunnel is created successfully.
     */
    TunnelHandler.Result removeTunnel(Tunnel tunnel);

    /**
     * Removes a policy.
     *
     * @param policy policy reference to remove
     * @return POLICY_NOT_FOUND if the policy to remove does not exists,
     * SUCCESS if it is removed successfully
     */
    PolicyHandler.Result removePolicy(Policy policy);

    /**
     * Use current state of the network to repopulate forwarding rules.
     *
     */
    void rerouteNetwork();

    /**
     * Returns device-subnet mapping.
     *
     * @return device-subnet mapping
     */
    Map<DeviceId, Set<IpPrefix>> getDeviceSubnetMap();

    /**
     * Returns the current ECMP shortest path graph in this controller instance.
     *
     * @return ECMP shortest path graph
     */
    ImmutableMap<DeviceId, EcmpShortestPathGraph> getCurrentEcmpSpg();

    /**
     * Returns the neighborSet-NextObjective store contents.
     *
     * @return current contents of the neighborSetNextObjectiveStore
     */
    ImmutableMap<NeighborSetNextObjectiveStoreKey, Integer> getNeighborSet();
}
