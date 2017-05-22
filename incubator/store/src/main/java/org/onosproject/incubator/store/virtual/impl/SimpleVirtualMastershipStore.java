/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.incubator.store.virtual.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.joda.time.DateTime;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkMastershipStore;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipStoreDelegate;
import org.onosproject.mastership.MastershipTerm;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.onosproject.mastership.MastershipEvent.Type.BACKUPS_CHANGED;
import static org.onosproject.mastership.MastershipEvent.Type.MASTER_CHANGED;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the virtual network mastership store to manage inventory of
 * mastership using trivial in-memory implementation.
 */
@Component(immediate = true)
@Service
public class SimpleVirtualMastershipStore
        extends AbstractVirtualStore<MastershipEvent, MastershipStoreDelegate>
        implements VirtualNetworkMastershipStore {

    private final Logger log = getLogger(getClass());

    private static final int NOTHING = 0;
    private static final int INIT = 1;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    //devices mapped to their masters, to emulate multiple nodes
    protected final Map<NetworkId, Map<DeviceId, NodeId>> masterMapByNetwork =
            new HashMap<>();
    //emulate backups with pile of nodes
    protected final Map<NetworkId, Map<DeviceId, List<NodeId>>> backupsByNetwork =
            new HashMap<>();
    //terms
    protected final Map<NetworkId, Map<DeviceId, AtomicInteger>> termMapByNetwork =
            new HashMap<>();

    @Activate
    public void activate() {
        if (clusterService == null) {
            clusterService = createFakeClusterService();
        }
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public CompletableFuture<MastershipRole> requestRole(NetworkId networkId,
                                                         DeviceId deviceId) {
        //query+possible reelection
        NodeId node = clusterService.getLocalNode().id();
        MastershipRole role = getRole(networkId, node, deviceId);

        Map<DeviceId, NodeId> masterMap = getMasterMap(networkId);

        switch (role) {
            case MASTER:
                return CompletableFuture.completedFuture(MastershipRole.MASTER);
            case STANDBY:
                if (getMaster(networkId, deviceId) == null) {
                    // no master => become master
                    masterMap.put(deviceId, node);
                    incrementTerm(networkId, deviceId);
                    // remove from backup list
                    removeFromBackups(networkId, deviceId, node);
                    notifyDelegate(networkId, new MastershipEvent(MASTER_CHANGED, deviceId,
                                                       getNodes(networkId, deviceId)));
                    return CompletableFuture.completedFuture(MastershipRole.MASTER);
                }
                return CompletableFuture.completedFuture(MastershipRole.STANDBY);
            case NONE:
                if (getMaster(networkId, deviceId) == null) {
                    // no master => become master
                    masterMap.put(deviceId, node);
                    incrementTerm(networkId, deviceId);
                    notifyDelegate(networkId, new MastershipEvent(MASTER_CHANGED, deviceId,
                                                       getNodes(networkId, deviceId)));
                    return CompletableFuture.completedFuture(MastershipRole.MASTER);
                }
                // add to backup list
                if (addToBackup(networkId, deviceId, node)) {
                    notifyDelegate(networkId, new MastershipEvent(BACKUPS_CHANGED, deviceId,
                                                       getNodes(networkId, deviceId)));
                }
                return CompletableFuture.completedFuture(MastershipRole.STANDBY);
            default:
                log.warn("unknown Mastership Role {}", role);
        }
        return CompletableFuture.completedFuture(role);
    }

    @Override
    public MastershipRole getRole(NetworkId networkId, NodeId nodeId, DeviceId deviceId) {
        Map<DeviceId, NodeId> masterMap = getMasterMap(networkId);
        Map<DeviceId, List<NodeId>> backups = getBackups(networkId);

        //just query
        NodeId current = masterMap.get(deviceId);
        MastershipRole role;

        if (current != null && current.equals(nodeId)) {
            return MastershipRole.MASTER;
        }

        if (backups.getOrDefault(deviceId, Collections.emptyList()).contains(nodeId)) {
            role = MastershipRole.STANDBY;
        } else {
            role = MastershipRole.NONE;
        }
        return role;
    }

    @Override
    public NodeId getMaster(NetworkId networkId, DeviceId deviceId) {
        Map<DeviceId, NodeId> masterMap = getMasterMap(networkId);
        return masterMap.get(deviceId);
    }

    @Override
    public RoleInfo getNodes(NetworkId networkId, DeviceId deviceId) {
        Map<DeviceId, NodeId> masterMap = getMasterMap(networkId);
        Map<DeviceId, List<NodeId>> backups = getBackups(networkId);

        return new RoleInfo(masterMap.get(deviceId),
                            backups.getOrDefault(deviceId, ImmutableList.of()));
    }

    @Override
    public Set<DeviceId> getDevices(NetworkId networkId, NodeId nodeId) {
        Map<DeviceId, NodeId> masterMap = getMasterMap(networkId);

        Set<DeviceId> ids = new HashSet<>();
        for (Map.Entry<DeviceId, NodeId> d : masterMap.entrySet()) {
            if (Objects.equals(d.getValue(), nodeId)) {
                ids.add(d.getKey());
            }
        }
        return ids;
    }

    @Override
    public synchronized CompletableFuture<MastershipEvent> setMaster(NetworkId networkId,
                                                        NodeId nodeId, DeviceId deviceId) {
        Map<DeviceId, NodeId> masterMap = getMasterMap(networkId);

        MastershipRole role = getRole(networkId, nodeId, deviceId);
        switch (role) {
            case MASTER:
                // no-op
                return CompletableFuture.completedFuture(null);
            case STANDBY:
            case NONE:
                NodeId prevMaster = masterMap.put(deviceId, nodeId);
                incrementTerm(networkId, deviceId);
                removeFromBackups(networkId, deviceId, nodeId);
                addToBackup(networkId, deviceId, prevMaster);
                break;
            default:
                log.warn("unknown Mastership Role {}", role);
                return null;
        }

        return CompletableFuture.completedFuture(
                new MastershipEvent(MASTER_CHANGED, deviceId, getNodes(networkId, deviceId)));
    }

    @Override
    public MastershipTerm getTermFor(NetworkId networkId, DeviceId deviceId) {
        Map<DeviceId, NodeId> masterMap = getMasterMap(networkId);
        Map<DeviceId, AtomicInteger> termMap = getTermMap(networkId);

        if ((termMap.get(deviceId) == null)) {
            return MastershipTerm.of(masterMap.get(deviceId), NOTHING);
        }
        return MastershipTerm.of(
                masterMap.get(deviceId), termMap.get(deviceId).get());
    }

    @Override
    public CompletableFuture<MastershipEvent> setStandby(NetworkId networkId,
                                                         NodeId nodeId, DeviceId deviceId) {
        Map<DeviceId, NodeId> masterMap = getMasterMap(networkId);

        MastershipRole role = getRole(networkId, nodeId, deviceId);
        switch (role) {
            case MASTER:
                NodeId backup = reelect(networkId, deviceId, nodeId);
                if (backup == null) {
                    // no master alternative
                    masterMap.remove(deviceId);
                    // TODO: Should there be new event type for no MASTER?
                    return CompletableFuture.completedFuture(
                            new MastershipEvent(MASTER_CHANGED, deviceId,
                                                getNodes(networkId, deviceId)));
                } else {
                    NodeId prevMaster = masterMap.put(deviceId, backup);
                    incrementTerm(networkId, deviceId);
                    addToBackup(networkId, deviceId, prevMaster);
                    return CompletableFuture.completedFuture(
                            new MastershipEvent(MASTER_CHANGED, deviceId,
                                                getNodes(networkId, deviceId)));
                }

            case STANDBY:
            case NONE:
                boolean modified = addToBackup(networkId, deviceId, nodeId);
                if (modified) {
                    return CompletableFuture.completedFuture(
                            new MastershipEvent(BACKUPS_CHANGED, deviceId,
                                                getNodes(networkId, deviceId)));
                }
                break;

            default:
                log.warn("unknown Mastership Role {}", role);
        }
        return null;
    }


    /**
     * Dumbly selects next-available node that's not the current one.
     * emulate leader election.
     *
     * @param networkId a virtual network identifier
     * @param deviceId a virtual device identifier
     * @param nodeId a nod identifier
     * @return Next available node as a leader
     */
    private synchronized NodeId reelect(NetworkId networkId, DeviceId deviceId,
                                        NodeId nodeId) {
        Map<DeviceId, List<NodeId>> backups = getBackups(networkId);

        List<NodeId> stbys = backups.getOrDefault(deviceId, Collections.emptyList());
        NodeId backup = null;
        for (NodeId n : stbys) {
            if (!n.equals(nodeId)) {
                backup = n;
                break;
            }
        }
        stbys.remove(backup);
        return backup;
    }

    @Override
    public synchronized CompletableFuture<MastershipEvent>
    relinquishRole(NetworkId networkId, NodeId nodeId, DeviceId deviceId) {
    Map<DeviceId, NodeId> masterMap = getMasterMap(networkId);

        MastershipRole role = getRole(networkId, nodeId, deviceId);
        switch (role) {
            case MASTER:
                NodeId backup = reelect(networkId, deviceId, nodeId);
                masterMap.put(deviceId, backup);
                incrementTerm(networkId, deviceId);
                return CompletableFuture.completedFuture(
                        new MastershipEvent(MASTER_CHANGED, deviceId,
                                            getNodes(networkId, deviceId)));

            case STANDBY:
                if (removeFromBackups(networkId, deviceId, nodeId)) {
                    return CompletableFuture.completedFuture(
                            new MastershipEvent(BACKUPS_CHANGED, deviceId,
                                                getNodes(networkId, deviceId)));
                }
                break;

            case NONE:
                break;

            default:
                log.warn("unknown Mastership Role {}", role);
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void relinquishAllRole(NetworkId networkId, NodeId nodeId) {
        Map<DeviceId, NodeId> masterMap = getMasterMap(networkId);
        Map<DeviceId, List<NodeId>> backups = getBackups(networkId);

        List<CompletableFuture<MastershipEvent>> eventFutures = new ArrayList<>();
        Set<DeviceId> toRelinquish = new HashSet<>();

        masterMap.entrySet().stream()
                .filter(entry -> nodeId.equals(entry.getValue()))
                .forEach(entry -> toRelinquish.add(entry.getKey()));

        backups.entrySet().stream()
                .filter(entry -> entry.getValue().contains(nodeId))
                .forEach(entry -> toRelinquish.add(entry.getKey()));

        toRelinquish.forEach(deviceId -> eventFutures.add(
                relinquishRole(networkId, nodeId, deviceId)));

        eventFutures.forEach(future -> {
            future.whenComplete((event, error) -> notifyDelegate(networkId, event));
        });
    }

    /**
     * Increase the term for a device, and store it.
     *
     * @param networkId a virtual network identifier
     * @param deviceId a virtual device identifier
     */
    private synchronized void incrementTerm(NetworkId networkId, DeviceId deviceId) {
        Map<DeviceId, AtomicInteger> termMap = getTermMap(networkId);

        AtomicInteger term = termMap.getOrDefault(deviceId, new AtomicInteger(NOTHING));
        term.incrementAndGet();
        termMap.put(deviceId, term);
    }

    /**
     * Remove backup node for a device.
     *
     * @param networkId a virtual network identifier
     * @param deviceId a virtual device identifier
     * @param nodeId a node identifier
     * @return True if success
     */
    private synchronized boolean removeFromBackups(NetworkId networkId,
                                                   DeviceId deviceId, NodeId nodeId) {
        Map<DeviceId, List<NodeId>> backups = getBackups(networkId);

        List<NodeId> stbys = backups.getOrDefault(deviceId, new ArrayList<>());
        boolean modified = stbys.remove(nodeId);
        backups.put(deviceId, stbys);
        return modified;
    }

    /**
     * add to backup if not there already, silently ignores null node.
     *
     * @param networkId a virtual network identifier
     * @param deviceId a virtual device identifier
     * @param nodeId a node identifier
     * @return True if success
     */
    private synchronized boolean addToBackup(NetworkId networkId,
                                             DeviceId deviceId, NodeId nodeId) {
        Map<DeviceId, List<NodeId>> backups = getBackups(networkId);

        boolean modified = false;
        List<NodeId> stbys = backups.getOrDefault(deviceId, new ArrayList<>());
        if (nodeId != null && !stbys.contains(nodeId)) {
            stbys.add(nodeId);
            backups.put(deviceId, stbys);
            modified = true;
        }
        return modified;
    }

    /**
     * Returns deviceId-master map for a specified virtual network.
     *
     * @param networkId a virtual network identifier
     * @return DeviceId-master map of a given virtual network.
     */
    private Map<DeviceId, NodeId> getMasterMap(NetworkId networkId) {
        return masterMapByNetwork.computeIfAbsent(networkId, k -> new HashMap<>());
    }

    /**
     * Returns deviceId-backups map for a specified virtual network.
     *
     * @param networkId a virtual network identifier
     * @return DeviceId-backups map of a given virtual network.
     */
    private Map<DeviceId, List<NodeId>> getBackups(NetworkId networkId) {
        return backupsByNetwork.computeIfAbsent(networkId, k -> new HashMap<>());
    }

    /**
     * Returns deviceId-terms map for a specified virtual network.
     *
     * @param networkId a virtual network identifier
     * @return DeviceId-terms map of a given virtual network.
     */
    private Map<DeviceId, AtomicInteger> getTermMap(NetworkId networkId) {
        return termMapByNetwork.computeIfAbsent(networkId, k -> new HashMap<>());
    }

    /**
     * Returns a fake cluster service for a test purpose only.
     *
     * @return a fake cluster service
     */
    private ClusterService createFakeClusterService() {
        // just for ease of unit test
        final ControllerNode instance =
                new DefaultControllerNode(new NodeId("local"),
                                          IpAddress.valueOf("127.0.0.1"));

        ClusterService faceClusterService = new ClusterService() {

            private final DateTime creationTime = DateTime.now();

            @Override
            public ControllerNode getLocalNode() {
                return instance;
            }

            @Override
            public Set<ControllerNode> getNodes() {
                return ImmutableSet.of(instance);
            }

            @Override
            public ControllerNode getNode(NodeId nodeId) {
                if (instance.id().equals(nodeId)) {
                    return instance;
                }
                return null;
            }

            @Override
            public ControllerNode.State getState(NodeId nodeId) {
                if (instance.id().equals(nodeId)) {
                    return ControllerNode.State.ACTIVE;
                } else {
                    return ControllerNode.State.INACTIVE;
                }
            }

            @Override
            public DateTime getLastUpdated(NodeId nodeId) {
                return creationTime;
            }

            @Override
            public void addListener(ClusterEventListener listener) {
            }

            @Override
            public void removeListener(ClusterEventListener listener) {
            }
        };
        return faceClusterService;
    }
}
