/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.cluster.impl;

import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.metrics.MetricsService;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.core.MetricsHelper;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.mastership.MastershipAdminService;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mastership.MastershipStore;
import org.onosproject.mastership.MastershipStoreDelegate;
import org.onosproject.mastership.MastershipTerm;
import org.onosproject.mastership.MastershipTermService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.region.Region;
import org.onosproject.net.region.RegionService;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static org.onlab.metrics.MetricsUtil.startTimer;
import static org.onlab.metrics.MetricsUtil.stopTimer;
import static org.onosproject.net.MastershipRole.MASTER;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.CLUSTER_READ;
import static org.onosproject.security.AppPermission.Type.CLUSTER_WRITE;
import static org.slf4j.LoggerFactory.getLogger;



@Component(immediate = true)
@Service
public class MastershipManager
    extends AbstractListenerManager<MastershipEvent, MastershipListener>
    implements MastershipService, MastershipAdminService, MastershipTermService,
               MetricsHelper {

    private static final String NODE_ID_NULL = "Node ID cannot be null";
    private static final String DEVICE_ID_NULL = "Device ID cannot be null";
    private static final String ROLE_NULL = "Mastership role cannot be null";

    private final Logger log = getLogger(getClass());

    private final MastershipStoreDelegate delegate = new InternalDelegate();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MetricsService metricsService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RegionService regionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    private NodeId localNodeId;
    private Timer requestRoleTimer;

    static final boolean DEFAULT_USE_REGION_FOR_BALANCE_ROLES = false;
    @Property(name = "useRegionForBalanceRoles", boolValue = DEFAULT_USE_REGION_FOR_BALANCE_ROLES,
              label = "Use Regions for balancing roles")
    public boolean useRegionForBalanceRoles;

    @Activate
    public void activate() {
        cfgService.registerProperties(getClass());
        modified();

        requestRoleTimer = createTimer("Mastership", "requestRole", "responseTime");
        localNodeId = clusterService.getLocalNode().id();
        eventDispatcher.addSink(MastershipEvent.class, listenerRegistry);
        store.setDelegate(delegate);
        log.info("Started");
    }

    @Modified
    public void modified() {
        Set<ConfigProperty> configProperties = cfgService.getProperties(getClass().getCanonicalName());
        for (ConfigProperty property : configProperties) {
            if ("useRegionForBalanceRoles".equals(property.name())) {
                useRegionForBalanceRoles = property.asBoolean();
            }
        }
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(MastershipEvent.class);
        store.unsetDelegate(delegate);
        log.info("Stopped");
        cfgService.unregisterProperties(getClass(), false);
    }

    @Override
    public CompletableFuture<Void> setRole(NodeId nodeId, DeviceId deviceId, MastershipRole role) {
        checkNotNull(nodeId, NODE_ID_NULL);
        checkNotNull(deviceId, DEVICE_ID_NULL);
        checkNotNull(role, ROLE_NULL);

        CompletableFuture<MastershipEvent> eventFuture = null;

        switch (role) {
            case MASTER:
                eventFuture = store.setMaster(nodeId, deviceId);
                break;
            case STANDBY:
                eventFuture = store.setStandby(nodeId, deviceId);
                break;
            case NONE:
                eventFuture = store.relinquishRole(nodeId, deviceId);
                break;
            default:
                log.info("Unknown role; ignoring");
                return CompletableFuture.completedFuture(null);
        }

        return eventFuture.thenAccept(this::post)
                          .thenApply(v -> null);
    }

    @Override
    public MastershipRole getLocalRole(DeviceId deviceId) {
        checkPermission(CLUSTER_READ);

        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getRole(clusterService.getLocalNode().id(), deviceId);
    }

    @Override
    public CompletableFuture<Void> relinquishMastership(DeviceId deviceId) {
        checkPermission(CLUSTER_WRITE);
        return store.relinquishRole(localNodeId, deviceId)
                    .thenAccept(this::post)
                    .thenApply(v -> null);
    }

    @Override
    public CompletableFuture<MastershipRole> requestRoleFor(DeviceId deviceId) {
        checkPermission(CLUSTER_WRITE);

        checkNotNull(deviceId, DEVICE_ID_NULL);
        final Context timer = startTimer(requestRoleTimer);
        return store.requestRole(deviceId).whenComplete((result, error) -> stopTimer(timer));

    }

    @Override
    public NodeId getMasterFor(DeviceId deviceId) {
        checkPermission(CLUSTER_READ);

        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getMaster(deviceId);
    }

    @Override
    public Set<DeviceId> getDevicesOf(NodeId nodeId) {
        checkPermission(CLUSTER_READ);

        checkNotNull(nodeId, NODE_ID_NULL);
        return store.getDevices(nodeId);
    }

    @Override
    public RoleInfo getNodesFor(DeviceId deviceId) {
        checkPermission(CLUSTER_READ);

        checkNotNull(deviceId, DEVICE_ID_NULL);
        return store.getNodes(deviceId);
    }

    @Override
    public MastershipTerm getMastershipTerm(DeviceId deviceId) {
        checkPermission(CLUSTER_READ);
        return store.getTermFor(deviceId);
    }

    @Override
    public MetricsService metricsService() {
        return metricsService;
    }

    @Override
    public void balanceRoles() {
        List<ControllerNode> nodes = newArrayList(clusterService.getNodes());
        Map<ControllerNode, Set<DeviceId>> controllerDevices = new HashMap<>();
        int deviceCount = 0;

        // Create buckets reflecting current ownership.
        for (ControllerNode node : nodes) {
            if (clusterService.getState(node.id()).isActive()) {
                Set<DeviceId> devicesOf = new HashSet<>(getDevicesOf(node.id()));
                deviceCount += devicesOf.size();
                controllerDevices.put(node, devicesOf);
                log.info("Node {} has {} devices.", node.id(), devicesOf.size());
            }
        }

        if (useRegionForBalanceRoles && balanceRolesUsingRegions(controllerDevices)) {
            return;
        }

        // Now re-balance the buckets until they are roughly even.
        List<CompletableFuture<Void>> balanceBucketsFutures = balanceControllerNodes(controllerDevices, deviceCount);

        CompletableFuture<Void> balanceRolesFuture = CompletableFuture.allOf(
                balanceBucketsFutures.toArray(new CompletableFuture[balanceBucketsFutures.size()]));

        Futures.getUnchecked(balanceRolesFuture);
    }

    /**
     * Balances the nodes specified in controllerDevices.
     *
     * @param controllerDevices controller nodes to devices map
     * @param deviceCount number of devices mastered by controller nodes
     * @return list of setRole futures for "moved" devices
     */
    private List<CompletableFuture<Void>> balanceControllerNodes(
            Map<ControllerNode, Set<DeviceId>> controllerDevices, int deviceCount) {
        // Now re-balance the buckets until they are roughly even.
        List<CompletableFuture<Void>> balanceBucketsFutures = Lists.newLinkedList();
        int rounds = controllerDevices.keySet().size();
        for (int i = 0; i < rounds; i++) {
            // Iterate over the buckets and find the smallest and the largest.
            ControllerNode smallest = findBucket(true, controllerDevices);
            ControllerNode largest = findBucket(false, controllerDevices);
            balanceBucketsFutures.add(balanceBuckets(smallest, largest, controllerDevices, deviceCount));
        }
        return balanceBucketsFutures;
    }

    /**
     * Finds node with the minimum/maximum devices from a list of nodes.
     *
     * @param min true: minimum, false: maximum
     * @param controllerDevices controller nodes to devices map
     * @return controller node with minimum/maximum devices
     */
    private ControllerNode findBucket(boolean min,
                                      Map<ControllerNode, Set<DeviceId>>  controllerDevices) {
        int xSize = min ? Integer.MAX_VALUE : -1;
        ControllerNode xNode = null;
        for (ControllerNode node : controllerDevices.keySet()) {
            int size = controllerDevices.get(node).size();
            if ((min && size < xSize) || (!min && size > xSize)) {
                xSize = size;
                xNode = node;
            }
        }
        return xNode;
    }

    /**
     * Balance the node buckets by moving devices from largest to smallest node.
     *
     * @param smallest node that is master of the smallest number of devices
     * @param largest node that is master of the largest number of devices
     * @param controllerDevices controller nodes to devices map
     * @param deviceCount number of devices mastered by controller nodes
     * @return list of setRole futures for "moved" devices
     */
    private CompletableFuture<Void> balanceBuckets(ControllerNode smallest, ControllerNode largest,
                                Map<ControllerNode, Set<DeviceId>>  controllerDevices,
                                int deviceCount) {
        Collection<DeviceId> minBucket = controllerDevices.get(smallest);
        Collection<DeviceId> maxBucket = controllerDevices.get(largest);
        int bucketCount = controllerDevices.keySet().size();

        int delta = (maxBucket.size() - minBucket.size()) / 2;
        delta = Math.min(deviceCount / bucketCount, delta);

        List<CompletableFuture<Void>> setRoleFutures = Lists.newLinkedList();

        if (delta > 0) {
            log.info("Attempting to move {} nodes from {} to {}...", delta,
                     largest.id(), smallest.id());

            int i = 0;
            Iterator<DeviceId> it = maxBucket.iterator();
            while (it.hasNext() && i < delta) {
                DeviceId deviceId = it.next();
                log.info("Setting {} as the master for {}", smallest.id(), deviceId);
                setRoleFutures.add(setRole(smallest.id(), deviceId, MASTER));
                controllerDevices.get(smallest).add(deviceId);
                it.remove();
                i++;
            }
        }

        return CompletableFuture.allOf(setRoleFutures.toArray(new CompletableFuture[setRoleFutures.size()]));
    }

    /**
     * Balances the nodes considering Region information.
     *
     * @param allControllerDevices controller nodes to devices map
     * @return true: nodes balanced; false: nodes not balanced
     */
    private boolean balanceRolesUsingRegions(Map<ControllerNode, Set<DeviceId>> allControllerDevices) {
        Set<Region> regions = regionService.getRegions();
        if (regions.isEmpty()) {
            return false; // no balancing was done using regions.
        }

        // handle nodes belonging to regions
        Set<ControllerNode> nodesInRegions = Sets.newHashSet();
        for (Region region : regions) {
            Map<ControllerNode, Set<DeviceId>> activeRegionControllers =
                    balanceRolesInRegion(region, allControllerDevices);
            nodesInRegions.addAll(activeRegionControllers.keySet());
        }

        // handle nodes not belonging to any region
        Set<ControllerNode> nodesNotInRegions = Sets.difference(allControllerDevices.keySet(), nodesInRegions);
        if (!nodesNotInRegions.isEmpty()) {
            int deviceCount = 0;
            Map<ControllerNode, Set<DeviceId>> controllerDevicesNotInRegions = new HashMap<>();
            for (ControllerNode controllerNode: nodesNotInRegions) {
                controllerDevicesNotInRegions.put(controllerNode, allControllerDevices.get(controllerNode));
                deviceCount += allControllerDevices.get(controllerNode).size();
            }
            // Now re-balance the buckets until they are roughly even.
            List<CompletableFuture<Void>> balanceBucketsFutures =
                    balanceControllerNodes(controllerDevicesNotInRegions, deviceCount);

            CompletableFuture<Void> balanceRolesFuture = CompletableFuture.allOf(
                    balanceBucketsFutures.toArray(new CompletableFuture[balanceBucketsFutures.size()]));

            Futures.getUnchecked(balanceRolesFuture);
        }
        return true; // balancing was done using regions.
    }

    /**
     * Balances the nodes in specified region.
     *
     * @param region region in which nodes are to be balanced
     * @param allControllerDevices controller nodes to devices map
     * @return controller nodes that were balanced
     */
    private Map<ControllerNode, Set<DeviceId>> balanceRolesInRegion(Region region,
         Map<ControllerNode, Set<DeviceId>> allControllerDevices) {

        // retrieve all devices associated with specified region
        Set<DeviceId> devicesInRegion = regionService.getRegionDevices(region.id());
        log.info("Region {} has {} devices.", region.id(), devicesInRegion.size());
        if (devicesInRegion.isEmpty()) {
            return new HashMap<>(); // no devices in this region, so nothing to balance.
        }

        List<Set<NodeId>> mastersList = region.masters();
        log.info("Region {} has {} sets of masters.", region.id(), mastersList.size());
        if (mastersList.isEmpty()) {
            // TODO handle devices that belong to a region, which has no masters defined
            return new HashMap<>(); // for now just leave devices alone
        }

        // get the region's preferred set of masters
        Set<DeviceId> devicesInMasters = Sets.newHashSet();
        Map<ControllerNode, Set<DeviceId>> regionalControllerDevices =
                getRegionsPreferredMasters(region, devicesInMasters, allControllerDevices);

        // Now re-balance the buckets until they are roughly even.
        List<CompletableFuture<Void>> balanceBucketsFutures =
                balanceControllerNodes(regionalControllerDevices, devicesInMasters.size());

        // handle devices that are not currently mastered by the master node set
        Set<DeviceId> devicesNotMasteredWithControllers = Sets.difference(devicesInRegion, devicesInMasters);
        if (!devicesNotMasteredWithControllers.isEmpty()) {
            // active controllers in master node set are already balanced, just
            // assign device mastership in sequence
            List<ControllerNode> sorted = new ArrayList<>(regionalControllerDevices.keySet());
            Collections.sort(sorted, (o1, o2) ->
                    ((Integer) (regionalControllerDevices.get(o1)).size())
                            .compareTo((Integer) (regionalControllerDevices.get(o2)).size()));
            int deviceIndex = 0;
            for (DeviceId deviceId : devicesNotMasteredWithControllers) {
                ControllerNode cnode = sorted.get(deviceIndex % sorted.size());
                balanceBucketsFutures.add(setRole(cnode.id(), deviceId, MASTER));
                regionalControllerDevices.get(cnode).add(deviceId);
                deviceIndex++;
            }
        }

        CompletableFuture<Void> balanceRolesFuture = CompletableFuture.allOf(
                balanceBucketsFutures.toArray(new CompletableFuture[balanceBucketsFutures.size()]));

        Futures.getUnchecked(balanceRolesFuture);

        // update the map before returning
        regionalControllerDevices.forEach((controllerNode, deviceIds) -> {
            regionalControllerDevices.put(controllerNode, new HashSet<>(getDevicesOf(controllerNode.id())));
        });

        return regionalControllerDevices;
    }

    /**
     * Get region's preferred set of master nodes - the first master node set that has at
     * least one active node.
     *
     * @param region region for which preferred set of master nodes is requested
     * @param devicesInMasters device set to track devices in preferred set of master nodes
     * @param allControllerDevices controller nodes to devices map
     * @return region's preferred master nodes (and devices that use them as masters)
     */
    private Map<ControllerNode, Set<DeviceId>> getRegionsPreferredMasters(Region region,
            Set<DeviceId> devicesInMasters,
            Map<ControllerNode, Set<DeviceId>> allControllerDevices) {
        Map<ControllerNode, Set<DeviceId>> regionalControllerDevices = new HashMap<>();
        int listIndex = 0;
        for (Set<NodeId> masterSet: region.masters()) {
            log.info("Region {} masters set {} has {} nodes.",
                     region.id(), listIndex, masterSet.size());
            if (masterSet.isEmpty()) { // nothing on this level
                listIndex++;
                continue;
            }
            // Create buckets reflecting current ownership.
            for (NodeId nodeId : masterSet) {
                if (clusterService.getState(nodeId).isActive()) {
                    ControllerNode controllerNode = clusterService.getNode(nodeId);
                    Set<DeviceId> devicesOf = new HashSet<>(allControllerDevices.get(controllerNode));
                    regionalControllerDevices.put(controllerNode, devicesOf);
                    devicesInMasters.addAll(devicesOf);
                    log.info("Active Node {} has {} devices.", nodeId, devicesOf.size());
                }
            }
            if (!regionalControllerDevices.isEmpty()) {
                break; // now have a set of >0 active controllers
            }
            listIndex++; // keep on looking
        }
        return regionalControllerDevices;
    }

    public class InternalDelegate implements MastershipStoreDelegate {
        @Override
        public void notify(MastershipEvent event) {
            post(event);
        }
    }

}
