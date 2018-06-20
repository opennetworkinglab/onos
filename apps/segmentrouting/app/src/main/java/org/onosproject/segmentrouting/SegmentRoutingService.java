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

import com.google.common.annotations.Beta;
import com.google.common.collect.Multimap;
import org.apache.commons.lang3.NotImplementedException;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.VlanId;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.segmentrouting.grouphandler.NextNeighbors;
import org.onosproject.segmentrouting.mcast.McastRole;
import org.onosproject.segmentrouting.mcast.McastRoleStoreKey;
import org.onosproject.segmentrouting.pwaas.DefaultL2TunnelDescription;
import org.onosproject.segmentrouting.pwaas.L2Tunnel;
import org.onosproject.segmentrouting.pwaas.L2TunnelHandler;
import org.onosproject.segmentrouting.pwaas.L2TunnelPolicy;
import org.onosproject.segmentrouting.pwaas.L2TunnelDescription;
import org.onosproject.segmentrouting.storekey.DestinationSetNextObjectiveStoreKey;

import com.google.common.collect.ImmutableMap;
import org.onosproject.segmentrouting.mcast.McastStoreKey;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Segment Routing Service for REST API.
 */
public interface SegmentRoutingService {
    /**
     * VLAN cross-connect ACL priority.
     *
     * @deprecated in ONOS 1.12. Replaced by {@link org.onosproject.segmentrouting.xconnect.api.XconnectService}
     */
    @Deprecated
    int XCONNECT_ACL_PRIORITY = 60000;

    /**
     * VLAN cross-connect Bridging priority.
     *
     * @deprecated in ONOS 1.12. Replaced by {@link org.onosproject.segmentrouting.xconnect.api.XconnectService}
     */
    @Deprecated
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
     * Returns the l2 tunnel descriptions.
     *
     * @param pending if true fetch pending pseudowires, else fetch installed
     * @return set of l2 tunnel descriptions.
     */
    Set<L2TunnelDescription> getL2TunnelDescriptions(boolean pending);

    /**
     * Returns all l2 tunnels of pseudowires.
     *
     * @return list of l2 tunnels
     */
    List<L2Tunnel> getL2Tunnels();

    /**
     * Returns all l2 policie of pseudowires.
     *
     * @return list of l2 policies.
     */
    List<L2TunnelPolicy> getL2Policies();

    /**
     * Removes pseudowire.
     *
     * @param pwId The id of the pseudowire.
     * @return SUCCESS if operation successful or a descriptive error otherwise.
     */
    L2TunnelHandler.Result removePseudowire(Integer pwId);

    /**
     * Adds a Pseudowire to the system.
     *
     * @param tunnel The pseudowire tunnel.
     * @return SUCCESS if operation is successful or a descriptive error otherwise.
     */
    L2TunnelHandler.Result addPseudowire(L2TunnelDescription tunnel);

    /**
     * Adds a set of pseudowires.
     *
     *
     * @param l2TunnelDescriptions The pseudowires to add.
     * @return SUCCESS if ALL pseudowires can be instantiated and are deployed, or a
     *         a descriptive error otherwise, without deploying any pseudowire.
     * @deprecated onos-1.12 use addPseudowire instead
     */
    @Deprecated
    L2TunnelHandler.Result addPseudowiresBulk(List<DefaultL2TunnelDescription> l2TunnelDescriptions);

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
     *
     * @deprecated in 1.12 ("Magpie") release.
     */
    @Deprecated
    Map<McastStoreKey, McastRole> getMcastRoles(IpAddress mcastIp);

    /**
     * Returns the associated roles to the mcast groups.
     *
     * @param mcastIp the group ip
     * @param sourcecp the source connect point
     * @return the mapping mcastIp-device to mcast role
     */
    Map<McastRoleStoreKey, McastRole> getMcastRoles(IpAddress mcastIp,
                                                    ConnectPoint sourcecp);

    /**
     * Returns the associated paths to the mcast group.
     *
     * @param mcastIp the group ip
     * @return the mapping egress point to mcast path
     *
     * @deprecated in 1.12 ("Magpie") release.
     */
    @Deprecated
    Map<ConnectPoint, List<ConnectPoint>> getMcastPaths(IpAddress mcastIp);

    /**
     * Returns the associated trees to the mcast group.
     *
     * @param mcastIp the group ip
     * @param sourcecp the source connect point
     * @return the mapping egress point to mcast path
     */
    Multimap<ConnectPoint, List<ConnectPoint>> getMcastTrees(IpAddress mcastIp,
                                                             ConnectPoint sourcecp);

    /**
     * Return the leaders of the mcast groups.
     *
     * @param mcastIp the group ip
     * @return the mapping group-node
     */
    Map<IpAddress, NodeId> getMcastLeaders(IpAddress mcastIp);

    /**
     * Returns shouldProgram map.
     *
     * @return shouldProgram map
     */
    Map<Set<DeviceId>, NodeId> getShouldProgram();

    /**
     * Returns shouldProgram local cache.
     *
     * @return shouldProgram local cache
     */
    Map<DeviceId, Boolean> getShouldProgramCache();

    /**
     * Gets application id.
     *
     * @return application id
     */
    default ApplicationId appId() {
        throw new NotImplementedException("appId not implemented");
    }

    /**
     * Returns internal VLAN for untagged hosts on given connect point.
     * <p>
     * The internal VLAN is either vlan-untagged for an access port,
     * or vlan-native for a trunk port.
     *
     * @param connectPoint connect point
     * @return internal VLAN or null if both vlan-untagged and vlan-native are undefined
     */
    @Beta
    default VlanId getInternalVlanId(ConnectPoint connectPoint) {
        throw new NotImplementedException("getInternalVlanId not implemented");
    }


    /**
     * Returns optional pair device ID of given device.
     *
     * @param deviceId device ID
     * @return optional pair device ID. Might be empty if pair device is not configured
     */
    @Beta
    default Optional<DeviceId> getPairDeviceId(DeviceId deviceId) {
        throw new NotImplementedException("getPairDeviceId not implemented");
    }


    /**
     * Returns optional pair device local port of given device.
     *
     * @param deviceId device ID
     * @return optional pair device ID. Might be empty if pair device is not configured
     */
    @Beta
    default Optional<PortNumber> getPairLocalPort(DeviceId deviceId) {
        throw new NotImplementedException("getPairLocalPort not implemented");
    }
}
