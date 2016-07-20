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
import java.util.Set;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.link.LinkService;
import org.onosproject.segmentrouting.SegmentRoutingManager;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.segmentrouting.config.DeviceProperties;

/**
 * Default ECMP group handler creation module for a transit device.
 * This component creates a set of ECMP groups for every neighbor
 * that this device is connected to.
 * For example, consider a network of 4 devices: D0 (Segment ID: 100),
 * D1 (Segment ID: 101), D2 (Segment ID: 102) and D3 (Segment ID: 103),
 * where D0 and D3 are edge devices and D1 and D2 are transit devices.
 * Assume transit device D1 is connected to 2 neighbors (D0 and D3 ).
 * The following groups will be created in D1:
 * 1) all ports to D0 + with no label push,
 * 2) all ports to D3 + with no label push,
 */
public class DefaultTransitGroupHandler extends DefaultGroupHandler {
    protected DefaultTransitGroupHandler(DeviceId deviceId,
                                  ApplicationId appId,
                                  DeviceProperties config,
                                  LinkService linkService,
                                  FlowObjectiveService flowObjService,
                                  SegmentRoutingManager srManager) {
        super(deviceId, appId, config, linkService, flowObjService, srManager);
    }

    @Override
    public void createGroups() {
        Set<DeviceId> neighbors = devicePortMap.keySet();
        if (neighbors == null || neighbors.isEmpty()) {
            return;
        }

        // Create all possible Neighbor sets from this router
        // NOTE: Avoid any pairings of edge routers only
        Set<Set<DeviceId>> sets = getPowerSetOfNeighbors(neighbors);
        sets = filterEdgeRouterOnlyPairings(sets);
        log.debug("createGroupsAtTransitRouter: The size of neighbor powerset "
                + "for sw {} is {}", deviceId, sets.size());
        Set<NeighborSet> nsSet = new HashSet<>();
        for (Set<DeviceId> combo : sets) {
            if (combo.isEmpty()) {
                continue;
            }
            NeighborSet ns = new NeighborSet(combo);
            log.debug("createGroupsAtTransitRouter: sw {} combo {} ns {}",
                      deviceId, combo, ns);
            nsSet.add(ns);
        }
        log.debug("createGroupsAtTransitRouter: The neighborset with label "
                + "for sw {} is {}", deviceId, nsSet);

        //createGroupsFromNeighborsets(nsSet);
    }

    @Override
    protected void newNeighbor(Link newNeighborLink) {
        log.debug("New Neighbor: Updating groups for "
                + "transit device {}", deviceId);
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
                + "groups for transit device {}", deviceId);
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

        powerSet = filterEdgeRouterOnlyPairings(powerSet);
        Set<NeighborSet> nsSet = new HashSet<>();
        for (Set<DeviceId> combo : powerSet) {
            if (combo.isEmpty()) {
                continue;
            }
            NeighborSet ns = new NeighborSet(combo);
            log.debug("createGroupsAtTransitRouter: sw {} combo {} ns {}",
                      deviceId, combo, ns);
            nsSet.add(ns);
        }
        log.debug("computeImpactedNeighborsetForPortEvent: The neighborset with label "
                + "for sw {} is {}", deviceId, nsSet);

        return nsSet;
    }

    private Set<Set<DeviceId>> filterEdgeRouterOnlyPairings(Set<Set<DeviceId>> sets) {
        Set<Set<DeviceId>> fiteredSets = new HashSet<>();
        for (Set<DeviceId> deviceSubSet : sets) {
            if (deviceSubSet.size() > 1) {
                boolean avoidEdgeRouterPairing = true;
                for (DeviceId device : deviceSubSet) {
                    boolean isEdge;
                    try {
                        isEdge = deviceConfig.isEdgeDevice(device);
                    } catch (DeviceConfigNotFoundException e) {
                        log.warn(e.getMessage() + " Skipping filterEdgeRouterOnlyPairings on this device.");
                        continue;
                    }

                    if (!isEdge) {
                        avoidEdgeRouterPairing = false;
                        break;
                    }
                }
                if (!avoidEdgeRouterPairing) {
                    fiteredSets.add(deviceSubSet);
                }
            } else {
                fiteredSets.add(deviceSubSet);
            }
        }
        return fiteredSets;
    }
}
