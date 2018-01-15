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
package org.onosproject.segmentrouting;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.segmentrouting.grouphandler.NextNeighbors;
import org.onosproject.segmentrouting.pwaas.DefaultL2Tunnel;
import org.onosproject.segmentrouting.pwaas.DefaultL2TunnelPolicy;
import org.onosproject.segmentrouting.pwaas.L2TunnelHandler;
import org.onosproject.segmentrouting.storekey.DestinationSetNextObjectiveStoreKey;

import com.google.common.collect.ImmutableMap;
import org.onosproject.segmentrouting.storekey.McastStoreKey;

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
     * Returns all l2 tunnels of pseudowires.
     *
     * @return list of l2 tunnels
     */
    List<DefaultL2Tunnel> getL2Tunnels();

    /**
     * Returns all l2 policie of pseudowires.
     *
     * @return list of l2 policies.
     */
    List<DefaultL2TunnelPolicy> getL2Policies();

    /**
     * Removes pw. Essentially updates configuration for PwaasConfig
     * and sends event for removal. The rest are handled by L2TunnelHandler
     *
     * @param pwId The pseudowire id
     * @return SUCCESS if operation successful or a descriptive error otherwise.
     */
    L2TunnelHandler.Result removePseudowire(String pwId);

    /**
     * Adds a Pseudowire to the configuration.
     *
     * @param tunnelId The pseudowire id
     * @param pwLabel Pw label
     * @param cP1 Connection Point 1 of pw
     * @param cP1InnerVlan Outer vlan of cp2
     * @param cP1OuterVlan Outer vlan of cp1
     * @param cP2 Connection Point 2 of pw
     * @param cP2InnerVlan Inner vlan of cp2
     * @param cP2OuterVlan Outer vlan of cp1
     * @param mode Mode of pw
     * @param sdTag Service Delimiting tag of pw
     * @return SUCCESS if operation is successful or a descriptive error otherwise.
     */
    L2TunnelHandler.Result addPseudowire(String tunnelId, String pwLabel, String cP1,
                                         String cP1InnerVlan, String cP1OuterVlan, String cP2,
                                         String cP2InnerVlan, String cP2OuterVlan,
                                         String mode, String sdTag);

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
     * Returns the destinatiomSet-NextObjective store contents.
     *
     * @return current contents of the destinationSetNextObjectiveStore
     */
    ImmutableMap<DestinationSetNextObjectiveStoreKey, NextNeighbors> getDestinationSet();

    /**
     * Triggers the verification of all ECMP groups in the specified device.
     * Adjusts the group buckets if verification finds that there are more or less
     * buckets than what should be there.
     *
     * @param id the device identifier
     */
    void verifyGroups(DeviceId id);

    /**
     * Returns the internal link state as seen by this instance of the
     * controller.
     *
     * @return the internal link state
     */
    ImmutableMap<Link, Boolean> getSeenLinks();

    /**
     * Returns the ports administratively disabled by the controller.
     *
     * @return a map of devices and port numbers for administratively disabled
     *         ports. Does not include ports manually disabled by the operator.
     */
    ImmutableMap<DeviceId, Set<PortNumber>> getDownedPortState();

   /**
     * Returns the associated next ids to the mcast groups or to the single
     * group if mcastIp is present.
     *
     * @param mcastIp the group ip
     * @return the mapping mcastIp-device to next id
     */
    Map<McastStoreKey, Integer> getMcastNextIds(IpAddress mcastIp);

    /**
     * Returns the associated roles to the mcast groups or to the single
     * group if mcastIp is present.
     *
     * @param mcastIp the group ip
     * @return the mapping mcastIp-device to mcast role
     */
    Map<McastStoreKey, McastHandler.McastRole> getMcastRoles(IpAddress mcastIp);

    /**
     * Returns the associated paths to the mcast group.
     *
     * @param mcastIp the group ip
     * @return the mapping egress point to mcast path
     */
    Map<ConnectPoint, List<ConnectPoint>> getMcastPaths(IpAddress mcastIp);

}
