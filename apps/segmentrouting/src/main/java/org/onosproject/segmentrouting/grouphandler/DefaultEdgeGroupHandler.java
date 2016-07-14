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
package org.onosproject.segmentrouting.grouphandler;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.link.LinkService;
import org.onosproject.segmentrouting.SegmentRoutingManager;
import org.onosproject.segmentrouting.config.DeviceProperties;

/**
 * Default ECMP group handler creation module for an edge device.
 * This component creates a set of ECMP groups for every neighbor
 * that this device is connected to.
 * For example, consider a network of 4 devices: D0 (Segment ID: 100),
 * D1 (Segment ID: 101), D2 (Segment ID: 102) and D3 (Segment ID: 103),
 * where D0 and D3 are edge devices and D1 and D2 are transit devices.
 * Assume device D0 is connected to 2 neighbors (D1 and D2 ).
 * The following groups will be created in D0:
 * 1) all ports to D1 + with no label push, // direct attach case, seen
 * 2) all ports to D1 + with label 102 pushed, // this is not needed
 * 3) all ports to D1 + with label 103 pushed, // maybe needed, sometimes seen
 * 4) all ports to D2 + with no label push,
 * 5) all ports to D2 + with label 101 pushed,
 * 6) all ports to D2 + with label 103 pushed,
 * 7) all ports to D1 and D2 + with label 103 pushed // ecmp case
 * 8) what about ecmp no label case
 */
public class DefaultEdgeGroupHandler extends DefaultGroupHandler {
    protected DefaultEdgeGroupHandler(DeviceId deviceId,
                                  ApplicationId appId,
                                  DeviceProperties config,
                                  LinkService linkService,
                                  FlowObjectiveService flowObjService,
                                  SegmentRoutingManager srManager) {
        super(deviceId, appId, config, linkService, flowObjService, srManager);
    }

    @Override
    public void createGroups() {
        log.debug("Creating default groups "
                + "for edge device {}", deviceId);
        Set<DeviceId> neighbors = devicePortMap.keySet();
        if (neighbors == null || neighbors.isEmpty()) {
            return;
        }

        // Create all possible Neighbor sets from this router
        Set<Set<DeviceId>> powerSet = getPowerSetOfNeighbors(neighbors);
        log.trace("createGroupsAtEdgeRouter: The size of neighbor powerset "
                + "for sw {} is {}", deviceId, powerSet.size());
        Set<NeighborSet> nsSet = new HashSet<>();
        for (Set<DeviceId> combo : powerSet) {
            if (combo.isEmpty()) {
                continue;
            }
            List<Integer> groupSegmentIds =
                    getSegmentIdsTobePairedWithNeighborSet(combo);
            for (Integer sId : groupSegmentIds) {
                NeighborSet ns = new NeighborSet(combo, sId);
                log.trace("createGroupsAtEdgeRouter: sw {} "
                        + "combo {} sId {} ns {}",
                        deviceId, combo, sId, ns);
                nsSet.add(ns);
            }
        }
        log.trace("createGroupsAtEdgeRouter: The neighborset "
                + "with label for sw {} is {}",
                deviceId, nsSet);

        //createGroupsFromNeighborsets(nsSet);
    }

    @Override
    protected void newNeighbor(Link newNeighborLink) {
        log.debug("New Neighbor: Updating groups "
                + "for edge device {}", deviceId);
        // Recompute neighbor power set
        addNeighborAtPort(newNeighborLink.dst().deviceId(),
                          newNeighborLink.src().port());
        // Compute new neighbor sets due to the addition of new neighbor
        Set<NeighborSet> nsSet = computeImpactedNeighborsetForPortEvent(
                                             newNeighborLink.dst().deviceId(),
                                             devicePortMap.keySet());
        //createGroupsFromNeighborsets(nsSet);
    }

    @Override
    protected void newPortToExistingNeighbor(Link newNeighborLink) {
        /*log.debug("New port to existing neighbor: Updating "
                + "groups for edge device {}", deviceId);
        addNeighborAtPort(newNeighborLink.dst().deviceId(),
                          newNeighborLink.src().port());
        Set<NeighborSet> nsSet = computeImpactedNeighborsetForPortEvent(
                                              newNeighborLink.dst().deviceId(),
                                              devicePortMap.keySet());
        for (NeighborSet ns : nsSet) {
            // Create the new bucket to be updated
            TrafficTreatment.Builder tBuilder =
                    DefaultTrafficTreatment.builder();
            tBuilder.setOutput(newNeighborLink.src().port())
                    .setEthDst(deviceConfig.getDeviceMac(
                          newNeighborLink.dst().deviceId()))
                    .setEthSrc(nodeMacAddr);
            if (ns.getEdgeLabel() != NeighborSet.NO_EDGE_LABEL) {
                tBuilder.pushMpls()
                        .setMpls(MplsLabel.
                                 mplsLabel(ns.getEdgeLabel()));
            }

            Integer nextId = deviceNextObjectiveIds.get(ns);
            if (nextId != null) {
                NextObjective.Builder nextObjBuilder = DefaultNextObjective
                        .builder().withId(nextId)
                        .withType(NextObjective.Type.HASHED).fromApp(appId);

                nextObjBuilder.addTreatment(tBuilder.build());

                NextObjective nextObjective = nextObjBuilder.add();
                flowObjectiveService.next(deviceId, nextObjective);
            }
        }*/
    }

    @Override
    protected Set<NeighborSet> computeImpactedNeighborsetForPortEvent(
                                            DeviceId impactedNeighbor,
                                            Set<DeviceId> updatedNeighbors) {
        Set<Set<DeviceId>> powerSet = getPowerSetOfNeighbors(updatedNeighbors);

        Set<DeviceId> tmp = new HashSet<>();
        tmp.addAll(updatedNeighbors);
        tmp.remove(impactedNeighbor);
        Set<Set<DeviceId>> tmpPowerSet = getPowerSetOfNeighbors(tmp);

        // Compute the impacted neighbor sets
        powerSet.removeAll(tmpPowerSet);

        Set<NeighborSet> nsSet = new HashSet<>();
        for (Set<DeviceId> combo : powerSet) {
            if (combo.isEmpty()) {
                continue;
            }
            List<Integer> groupSegmentIds =
                    getSegmentIdsTobePairedWithNeighborSet(combo);
            for (Integer sId : groupSegmentIds) {
                NeighborSet ns = new NeighborSet(combo, sId);
                log.trace("computeImpactedNeighborsetForPortEvent: sw {} "
                        + "combo {} sId {} ns {}",
                        deviceId, combo, sId, ns);
                nsSet.add(ns);
            }
        }
        log.trace("computeImpactedNeighborsetForPortEvent: The neighborset "
                + "with label for sw {} is {}",
                deviceId, nsSet);
        return nsSet;
    }

}
