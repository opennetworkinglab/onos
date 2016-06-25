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
package org.onosproject.store.trivial;

import static org.onosproject.mastership.MastershipEvent.Type.BACKUPS_CHANGED;
import static org.onosproject.mastership.MastershipEvent.Type.MASTER_CHANGED;
import static org.slf4j.LoggerFactory.getLogger;

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
import org.onosproject.cluster.ControllerNode.State;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipStore;
import org.onosproject.mastership.MastershipStoreDelegate;
import org.onosproject.mastership.MastershipTerm;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.store.AbstractStore;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

/**
 * Manages inventory of controller mastership over devices using
 * trivial, non-distributed in-memory structures implementation.
 */
@Component(immediate = true)
@Service
public class SimpleMastershipStore
        extends AbstractStore<MastershipEvent, MastershipStoreDelegate>
        implements MastershipStore {

    private final Logger log = getLogger(getClass());

    private static final int NOTHING = 0;
    private static final int INIT = 1;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    //devices mapped to their masters, to emulate multiple nodes
    protected final Map<DeviceId, NodeId> masterMap = new HashMap<>();
    //emulate backups with pile of nodes
    protected final Map<DeviceId, List<NodeId>> backups = new HashMap<>();
    //terms
    protected final Map<DeviceId, AtomicInteger> termMap = new HashMap<>();

    @Activate
    public void activate() {
        if (clusterService == null) {
          // just for ease of unit test
          final ControllerNode instance =
                  new DefaultControllerNode(new NodeId("local"),
                                            IpAddress.valueOf("127.0.0.1"));

            clusterService = new ClusterService() {

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
                public State getState(NodeId nodeId) {
                    if (instance.id().equals(nodeId)) {
                        return State.ACTIVE;
                    } else {
                        return State.INACTIVE;
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
        }
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public synchronized CompletableFuture<MastershipEvent> setMaster(NodeId nodeId, DeviceId deviceId) {

        MastershipRole role = getRole(nodeId, deviceId);
        switch (role) {
        case MASTER:
            // no-op
            return CompletableFuture.completedFuture(null);
        case STANDBY:
        case NONE:
            NodeId prevMaster = masterMap.put(deviceId, nodeId);
            incrementTerm(deviceId);
            removeFromBackups(deviceId, nodeId);
            addToBackup(deviceId, prevMaster);
            break;
        default:
            log.warn("unknown Mastership Role {}", role);
            return null;
        }

        return CompletableFuture.completedFuture(
                new MastershipEvent(MASTER_CHANGED, deviceId, getNodes(deviceId)));
    }

    @Override
    public NodeId getMaster(DeviceId deviceId) {
        return masterMap.get(deviceId);
    }

    // synchronized for atomic read
    @Override
    public synchronized RoleInfo getNodes(DeviceId deviceId) {
        return new RoleInfo(masterMap.get(deviceId),
                            backups.getOrDefault(deviceId, ImmutableList.of()));
    }

    @Override
    public Set<DeviceId> getDevices(NodeId nodeId) {
        Set<DeviceId> ids = new HashSet<>();
        for (Map.Entry<DeviceId, NodeId> d : masterMap.entrySet()) {
            if (Objects.equals(d.getValue(), nodeId)) {
                ids.add(d.getKey());
            }
        }
        return ids;
    }

    @Override
    public synchronized CompletableFuture<MastershipRole> requestRole(DeviceId deviceId) {
        //query+possible reelection
        NodeId node = clusterService.getLocalNode().id();
        MastershipRole role = getRole(node, deviceId);

        switch (role) {
            case MASTER:
                return CompletableFuture.completedFuture(MastershipRole.MASTER);
            case STANDBY:
                if (getMaster(deviceId) == null) {
                    // no master => become master
                    masterMap.put(deviceId, node);
                    incrementTerm(deviceId);
                    // remove from backup list
                    removeFromBackups(deviceId, node);
                    notifyDelegate(new MastershipEvent(MASTER_CHANGED, deviceId,
                                                       getNodes(deviceId)));
                    return CompletableFuture.completedFuture(MastershipRole.MASTER);
                }
                return CompletableFuture.completedFuture(MastershipRole.STANDBY);
            case NONE:
                if (getMaster(deviceId) == null) {
                    // no master => become master
                    masterMap.put(deviceId, node);
                    incrementTerm(deviceId);
                    notifyDelegate(new MastershipEvent(MASTER_CHANGED, deviceId,
                                                       getNodes(deviceId)));
                    return CompletableFuture.completedFuture(MastershipRole.MASTER);
                }
                // add to backup list
                if (addToBackup(deviceId, node)) {
                    notifyDelegate(new MastershipEvent(BACKUPS_CHANGED, deviceId,
                                                       getNodes(deviceId)));
                }
                return CompletableFuture.completedFuture(MastershipRole.STANDBY);
            default:
                log.warn("unknown Mastership Role {}", role);
        }
        return CompletableFuture.completedFuture(role);
    }

    // add to backup if not there already, silently ignores null node
    private synchronized boolean addToBackup(DeviceId deviceId, NodeId nodeId) {
        boolean modified = false;
        List<NodeId> stbys = backups.getOrDefault(deviceId, new ArrayList<>());
        if (nodeId != null && !stbys.contains(nodeId)) {
            stbys.add(nodeId);
            modified = true;
        }
        backups.put(deviceId, stbys);
        return modified;
    }

    private synchronized boolean removeFromBackups(DeviceId deviceId, NodeId node) {
        List<NodeId> stbys = backups.getOrDefault(deviceId, new ArrayList<>());
        boolean modified = stbys.remove(node);
        backups.put(deviceId, stbys);
        return modified;
    }

    private synchronized void incrementTerm(DeviceId deviceId) {
        AtomicInteger term = termMap.getOrDefault(deviceId, new AtomicInteger(NOTHING));
        term.incrementAndGet();
        termMap.put(deviceId, term);
    }

    @Override
    public MastershipRole getRole(NodeId nodeId, DeviceId deviceId) {
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

    // synchronized for atomic read
    @Override
    public synchronized MastershipTerm getTermFor(DeviceId deviceId) {
        if ((termMap.get(deviceId) == null)) {
            return MastershipTerm.of(masterMap.get(deviceId), NOTHING);
        }
        return MastershipTerm.of(
                masterMap.get(deviceId), termMap.get(deviceId).get());
    }

    @Override
    public synchronized CompletableFuture<MastershipEvent> setStandby(NodeId nodeId, DeviceId deviceId) {
        MastershipRole role = getRole(nodeId, deviceId);
        switch (role) {
        case MASTER:
            NodeId backup = reelect(deviceId, nodeId);
            if (backup == null) {
                // no master alternative
                masterMap.remove(deviceId);
                // TODO: Should there be new event type for no MASTER?
                return CompletableFuture.completedFuture(
                        new MastershipEvent(MASTER_CHANGED, deviceId, getNodes(deviceId)));
            } else {
                NodeId prevMaster = masterMap.put(deviceId, backup);
                incrementTerm(deviceId);
                addToBackup(deviceId, prevMaster);
                return CompletableFuture.completedFuture(
                        new MastershipEvent(MASTER_CHANGED, deviceId, getNodes(deviceId)));
            }

        case STANDBY:
        case NONE:
            boolean modified = addToBackup(deviceId, nodeId);
            if (modified) {
                return CompletableFuture.completedFuture(
                        new MastershipEvent(BACKUPS_CHANGED, deviceId, getNodes(deviceId)));
            }
            break;

        default:
            log.warn("unknown Mastership Role {}", role);
        }
        return null;
    }

    //dumbly selects next-available node that's not the current one
    //emulate leader election
    private synchronized NodeId reelect(DeviceId did, NodeId nodeId) {
        List<NodeId> stbys = backups.getOrDefault(did, Collections.emptyList());
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
    public synchronized CompletableFuture<MastershipEvent> relinquishRole(NodeId nodeId, DeviceId deviceId) {
        MastershipRole role = getRole(nodeId, deviceId);
        switch (role) {
        case MASTER:
            NodeId backup = reelect(deviceId, nodeId);
            masterMap.put(deviceId, backup);
            incrementTerm(deviceId);
            return CompletableFuture.completedFuture(
                    new MastershipEvent(MASTER_CHANGED, deviceId, getNodes(deviceId)));

        case STANDBY:
            if (removeFromBackups(deviceId, nodeId)) {
                return CompletableFuture.completedFuture(
                    new MastershipEvent(BACKUPS_CHANGED, deviceId, getNodes(deviceId)));
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
    public synchronized void relinquishAllRole(NodeId nodeId) {
        List<CompletableFuture<MastershipEvent>> eventFutures = new ArrayList<>();
        Set<DeviceId> toRelinquish = new HashSet<>();

        masterMap.entrySet().stream()
            .filter(entry -> nodeId.equals(entry.getValue()))
            .forEach(entry -> toRelinquish.add(entry.getKey()));

        backups.entrySet().stream()
            .filter(entry -> entry.getValue().contains(nodeId))
            .forEach(entry -> toRelinquish.add(entry.getKey()));

        toRelinquish.forEach(deviceId -> eventFutures.add(relinquishRole(nodeId, deviceId)));

        eventFutures.forEach(future -> {
            future.whenComplete((event, error) -> notifyDelegate(event));
        });
    }
}
