/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.segmentrouting.pwaas;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.RandomUtils;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onlab.util.KryoNamespace;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flowobjective.DefaultFilteringObjective;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.DefaultObjectiveContext;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.segmentrouting.SRLinkWeigher;
import org.onosproject.segmentrouting.SegmentRoutingManager;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedLock;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageException;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.onosproject.net.flowobjective.ForwardingObjective.Flag.VERSATILE;
import static org.onosproject.segmentrouting.pwaas.L2TunnelHandler.Pipeline.INITIATION;
import static org.onosproject.segmentrouting.pwaas.L2TunnelHandler.Pipeline.TERMINATION;
import static org.onosproject.segmentrouting.pwaas.L2TunnelHandler.Result.*;
import static org.onosproject.segmentrouting.pwaas.L2TunnelHandler.Direction.FWD;
import static org.onosproject.segmentrouting.pwaas.L2TunnelHandler.Direction.REV;
import static org.onosproject.segmentrouting.pwaas.PwaasUtil.*;

/**
 * Handler for pseudowire management.
 */
public class DefaultL2TunnelHandler implements L2TunnelHandler {

    private static final String LOCK_NAME = "l2-tunnel-handler-lock";
    private static final Logger log = LoggerFactory.getLogger(DefaultL2TunnelHandler.class);

    private final SegmentRoutingManager srManager;
    /**
     * To store the next objectives related to the initiation.
     */
    private final ConsistentMap<String, NextObjective> l2InitiationNextObjStore;
    /**
     * To store the next objectives related to the termination.
     */
    private final ConsistentMap<String, NextObjective> l2TerminationNextObjStore;

    /**
     * To store policies.
     */
    private final ConsistentMap<String, L2TunnelPolicy> l2PolicyStore;

    /**
     * To store tunnels.
     */
    private final ConsistentMap<String, L2Tunnel> l2TunnelStore;

    /**
     * To store pending tunnels that need to be installed.
     */
    private final ConsistentMap<String, L2Tunnel> pendingL2TunnelStore;

    /**
     * To store pending policies that need to be installed.
     */
    private final ConsistentMap<String, L2TunnelPolicy> pendingL2PolicyStore;

    private final KryoNamespace.Builder l2TunnelKryo;

    /**
     * Lock used when creating or removing pseudowires.
     */
    private final DistributedLock pwLock;

    private static final long LOCK_TIMEOUT = 2000;

    /**
     * Create a l2 tunnel handler for the deploy and
     * for the tear down of pseudo wires.
     *
     * @param segmentRoutingManager the segment routing manager
     */
    public DefaultL2TunnelHandler(SegmentRoutingManager segmentRoutingManager) {
        srManager = segmentRoutingManager;
        l2TunnelKryo = new KryoNamespace.Builder()
                .register(KryoNamespaces.API)
                .register(L2Tunnel.class,
                          L2TunnelPolicy.class,
                          DefaultL2Tunnel.class,
                          DefaultL2TunnelPolicy.class,
                          L2Mode.class,
                          MplsLabel.class,
                          VlanId.class,
                          ConnectPoint.class);

        l2InitiationNextObjStore = srManager.
                storageService.
                <String, NextObjective>consistentMapBuilder().
                withName("onos-l2initiation-nextobj-store").
                withSerializer(Serializer.using(l2TunnelKryo.build())).
                build();

        l2TerminationNextObjStore = srManager.storageService.
                <String, NextObjective>consistentMapBuilder()
                .withName("onos-l2termination-nextobj-store")
                .withSerializer(Serializer.using(l2TunnelKryo.build()))
                .build();

        l2PolicyStore = srManager.storageService
                .<String, L2TunnelPolicy>consistentMapBuilder()
                .withName("onos-l2-policy-store")
                .withSerializer(Serializer.using(l2TunnelKryo.build()))
                .build();

        l2TunnelStore = srManager.storageService
                .<String, L2Tunnel>consistentMapBuilder()
                .withName("onos-l2-tunnel-store")
                .withSerializer(Serializer.using(l2TunnelKryo.build()))
                .build();

        pendingL2PolicyStore = srManager.storageService
                .<String, L2TunnelPolicy>consistentMapBuilder()
                .withName("onos-l2-pending-policy-store")
                .withSerializer(Serializer.using(l2TunnelKryo.build()))
                .build();

        pendingL2TunnelStore = srManager.storageService
                .<String, L2Tunnel>consistentMapBuilder()
                .withName("onos-l2-pending-tunnel-store")
                .withSerializer(Serializer.using(l2TunnelKryo.build()))
                .build();

        pwLock = srManager.storageService.lockBuilder()
                .withName(LOCK_NAME)
                .build()
                .asLock(LOCK_TIMEOUT);
    }

    /**
     * Used by manager only in initialization.
     */
    @Override
    public void init() {
        // Since we have no pseudowires in netcfg there
        // is nothing to do in initialization.
        // I leave it here because potentially we might need to
        // use it in the future.
    }

    @Override
    public Set<L2TunnelDescription> getL2Descriptions(boolean pending) {
            // get pending tunnels/policies OR installed tunnels/policies
            List<L2Tunnel> tunnels = pending ? getL2PendingTunnels() : getL2Tunnels();
            List<L2TunnelPolicy> policies = pending ? getL2PendingPolicies() : getL2Policies();
            return tunnels.stream()
                .map(l2Tunnel -> {
                    L2TunnelPolicy policy = null;
                    for (L2TunnelPolicy l2Policy : policies) {
                        if (l2Policy.tunnelId() == l2Tunnel.tunnelId()) {
                            policy = l2Policy;
                            break;
                        }
                    }

                    return new DefaultL2TunnelDescription(l2Tunnel, policy);
                })
                .collect(Collectors.toSet());
    }

    @Override
    public List<L2TunnelPolicy> getL2Policies() {
        return new ArrayList<>(l2PolicyStore
                .values()
                .stream()
                .map(Versioned::value)
                .collect(Collectors.toList()));
    }

    @Override
    public List<L2Tunnel> getL2Tunnels() {
        return new ArrayList<>(l2TunnelStore
                .values()
                .stream()
                .map(Versioned::value)
                .collect(Collectors.toList()));
    }

    @Override
    public List<L2TunnelPolicy> getL2PendingPolicies() {
        return new ArrayList<>(pendingL2PolicyStore
                                       .values()
                                       .stream()
                                       .map(Versioned::value)
                                       .collect(Collectors.toList()));
    }

    @Override
    public List<L2Tunnel> getL2PendingTunnels() {
        return new ArrayList<>(pendingL2TunnelStore
                                       .values()
                                       .stream()
                                       .map(Versioned::value)
                                       .collect(Collectors.toList()));
    }

    @Override
    public Result verifyGlobalValidity(L2TunnelDescription pwToAdd) {

        // get both added and pending pseudowires
        List<L2TunnelDescription> newPseudowires = new ArrayList<>();
        newPseudowires.addAll(getL2Descriptions(false));
        newPseudowires.addAll(getL2Descriptions(true));
        // add the new one
        newPseudowires.add(pwToAdd);

        return configurationValidity(newPseudowires);
    }

    /**
     * Returns the new vlan id for an ingress point of a
     * pseudowire. For double tagged, it is the outer,
     * For single tagged it is the single tag, and for
     * inner it is None.
     *
     * @param ingressOuter vlanid of ingress outer
     * @param ingressInner vlanid of ingress inner
     * @param egressOuter  vlanid of egress outer
     * @param egressInner  vlanid of egress inner
     * @return returns the vlan id which will be installed at vlan table 1.
     */
    private VlanId determineEgressVlan(VlanId ingressOuter, VlanId ingressInner,
                                      VlanId egressOuter, VlanId egressInner) {
        // validity of vlan combinations was checked at verifyPseudowire
        if (!(ingressOuter.equals(VlanId.NONE))) {
            return egressOuter;
        } else if (!(ingressInner.equals(VlanId.NONE))) {
            return egressInner;
        } else {
            return VlanId.vlanId("None");
        }
    }

    /**
     * Returns the devices existing on a given path.
     *
     * @param path The path to traverse.
     * @return The devices on the path with the order they
     *         are traversed.
     */
    private List<DeviceId> getDevicesOnPath(List<Link> path) {

        // iterate over links and get all devices in the order
        // we find them
        List<DeviceId> deviceList = new ArrayList<>();
        for (Link link : path) {
            if (!deviceList.contains(link.src().deviceId())) {
                deviceList.add(link.src().deviceId());
            }
            if (!deviceList.contains(link.dst().deviceId())) {
                deviceList.add(link.dst().deviceId());
            }
        }

        return deviceList;
    }

    /**
     * Returns true if path is valid according to the current logic.
     * For example : leaf to spine pseudowires can be either leaf-spine or
     * leaf-spine-spine. However, in the configuration we might specify spine device
     * first which will result in spine-spine-leaf. If leafSpinePw == true we have one of these
     * two cases and need to provision for them.
     *
     * If we have a leaf to leaf pseudowire then all the intermediate devices must
     * be spines. However, in case of paired switches we can have two leafs interconnected
     * with each other directly.
     *
     * @param path the chosen path
     * @param leafSpinePw if it is a leaf to spine pseudowire
     * @return True if path size is valid, false otherwise.
     */
    private boolean isValidPath(List<Link> path, boolean leafSpinePw) {

        log.debug("Checking path validity for pseudowire.");
        List<DeviceId> devices = getDevicesOnPath(path);
        if (devices.size() < 2) {
            log.error("Path size for pseudowire should be greater than 1!");
            return false;
        }

        try {
            if (leafSpinePw) {
                // can either be leaf-spine-spine or leaf-spine
                // first device is leaf, all other must be spines
                log.debug("Devices on path are {} for leaf to spine pseudowire", devices);
                // if first device is a leaf then all other must be spines
                if (srManager.deviceConfiguration().isEdgeDevice(devices.get(0))) {
                    devices.remove(0);
                    for (DeviceId devId : devices) {
                        log.debug("Device {} should be a spine!", devId);
                        if (srManager.deviceConfiguration().isEdgeDevice(devId)) {
                            return false;
                        }
                    }
                } else {
                    // if first device is spine, last device must be a leaf
                    // all other devices must be spines
                    if (!srManager.deviceConfiguration().isEdgeDevice(devices.get(devices.size() - 1))) {
                        return false;
                    }
                    devices.remove(devices.size() - 1);
                    for (DeviceId devId : devices) {
                        log.debug("Device {} should be a spine!", devId);
                        if (srManager.deviceConfiguration().isEdgeDevice(devId)) {
                            return false;
                        }
                    }
                }
            } else {
                // can either be leaf-leaf (paired leafs) / leaf-spine-leaf
                // or leaf-spine-spine-leaf
                log.debug("Devices on path are {} for leaf to leaf pseudowire", devices);
                // check first device, needs to be a leaf
                if (!srManager.deviceConfiguration().isEdgeDevice(devices.get(0))) {
                    return false;
                }
                // check last device, needs to be a leaf
                if (!srManager.deviceConfiguration().isEdgeDevice(devices.get(devices.size() - 1))) {
                    return false;
                }
                // remove these devices, rest must all be spines
                devices.remove(0);
                devices.remove(devices.size() - 1);
                for (DeviceId devId : devices) {
                    log.debug("Device {} should be a spine!", devId);
                    if (srManager.deviceConfiguration().isEdgeDevice(devId)) {
                        return false;
                    }
                }

            }
        } catch (DeviceConfigNotFoundException e) {
            log.error("Device not found in configuration : {}", e);
            return false;
        }

        return true;
    }

    /**
     * Adds a single pseudowire.
     *
     * @param pw The pseudowire to deploy
     * @return result of pseudowire deployment
     */
    public Result deployPseudowire(L2TunnelDescription pw) {

        try {
            // take the lock
            pwLock.lock();
            Result result;
            long l2TunnelId;
            log.debug("Pseudowire with {} deployment started, check log for any errors in this process!",
                      pw.l2Tunnel().tunnelId());
            l2TunnelId = pw.l2Tunnel().tunnelId();
            // The tunnel id cannot be 0.
            if (l2TunnelId == 0) {
                log.warn("Tunnel id id must be > 0 in {}", l2TunnelId);
                return Result.WRONG_PARAMETERS
                        .appendError("Tunnel id id must be > 0");
            }

            result = verifyGlobalValidity(pw);
            if (result != SUCCESS) {
                log.error("Global validity for pseudowire {} is wrong!", l2TunnelId);
                return result;
            }

            // leafSpinePw determines if this is a leaf-leaf
            // or leaf-spine pseudowire
            boolean leafSpinePw;
            ConnectPoint cp1 = pw.l2TunnelPolicy().cP1();
            ConnectPoint cp2 = pw.l2TunnelPolicy().cP2();
            try {
                // differentiate between leaf-leaf pseudowires and leaf-spine
                if (!srManager.deviceConfiguration().isEdgeDevice(cp1.deviceId()) &&
                        !srManager.deviceConfiguration().isEdgeDevice(cp2.deviceId())) {
                    log.error("Can not deploy pseudowire {} from spine to spine!", l2TunnelId);
                    return Result.WRONG_PARAMETERS
                            .appendError("Can not deploy pseudowire from spine to spine!");
                } else if (srManager.deviceConfiguration().isEdgeDevice(cp1.deviceId()) &&
                        srManager.deviceConfiguration().isEdgeDevice(cp2.deviceId())) {
                    leafSpinePw = false;
                } else {
                    leafSpinePw = true;
                }
            } catch (DeviceConfigNotFoundException e) {
                log.error("Device for pseudowire {} connection points does not exist in the configuration", l2TunnelId);
                return Result.INTERNAL_ERROR
                        .appendError("Device for pseudowire connection points does not exist in the configuration");
            }

            // reverse the policy in order for leaf switch to be at CP1
            // this will help us for re-using SRLinkWeigher for computing valid paths
            L2TunnelPolicy reversedPolicy = reverseL2TunnelPolicy(pw.l2TunnelPolicy());
            if (reversedPolicy == null) {
                log.error("Error in reversing policy, device configuration was not found for pseudowire {}.",
                          l2TunnelId);
                return INTERNAL_ERROR
                        .appendError("Device configuration not found when reversing the policy.");
            }
            pw.setL2TunnelPolicy(reversedPolicy);

            // get path here, need to use the same for fwd and rev direction
            List<Link> path = getPath(pw.l2TunnelPolicy().cP1(),
                                      pw.l2TunnelPolicy().cP2());
            if (path == null) {
                log.error("Deploying process : No path between the connection points for pseudowire {}", l2TunnelId);
                return PATH_NOT_FOUND.appendError("No path between the connection points for pseudowire!");
            }

            Link fwdNextHop;
            Link revNextHop;
            if (!isValidPath(path, leafSpinePw)) {
                log.error("Deploying process : Path for pseudowire {} is not valid", l2TunnelId);
                return INTERNAL_ERROR.appendError("Internal error : path for pseudowire is not valid!");
            }
            // oneHope flag is used to determine if we need to
            // install transit mpls rules
            boolean oneHop = true;
            if (path.size() > 1) {
                oneHop = false;
            }

            fwdNextHop = path.get(0);
            revNextHop = reverseLink(path.get(path.size() - 1));

            pw.l2Tunnel().setPath(path);
            pw.l2Tunnel().setTransportVlan(srManager.PSEUDOWIRE_VLAN);

            // next hops for next objectives
            log.info("Deploying process : Establishing forward direction for pseudowire {}", l2TunnelId);

            VlanId egressVlan = determineEgressVlan(pw.l2TunnelPolicy().cP1OuterTag(),
                                                    pw.l2TunnelPolicy().cP1InnerTag(),
                                                    pw.l2TunnelPolicy().cP2OuterTag(),
                                                    pw.l2TunnelPolicy().cP2InnerTag());
            result = deployPseudoWireInit(pw.l2Tunnel(),
                                          pw.l2TunnelPolicy().cP1(),
                                          pw.l2TunnelPolicy().cP2(),
                                          FWD,
                                          fwdNextHop,
                                          oneHop,
                                          egressVlan);
            if (result != SUCCESS) {
                log.error("Deploying process : Error in deploying pseudowire {} initiation for CP1", l2TunnelId);
                return Result.INTERNAL_ERROR.appendError("Error in deploying pseudowire initiation for CP1");
            }

            result = deployPolicy(l2TunnelId,
                                  pw.l2TunnelPolicy().cP1(),
                                  pw.l2TunnelPolicy().cP1InnerTag(),
                                  pw.l2TunnelPolicy().cP1OuterTag(),
                                  egressVlan,
                                  result.getNextId());
            if (result != SUCCESS) {
                log.error("Deploying process : Error in deploying pseudowire {} policy for CP1", l2TunnelId);
                return Result.INTERNAL_ERROR.appendError("Error in deploying pseudowire policy for CP1");
            }

            result = deployPseudoWireTerm(pw.l2Tunnel(),
                                          pw.l2TunnelPolicy().cP2(),
                                          egressVlan,
                                          FWD,
                                          oneHop);

            if (result != SUCCESS) {
                log.error("Deploying process : Error in deploying pseudowire {} termination for CP1", l2TunnelId);
                return Result.INTERNAL_ERROR.appendError("Error in deploying pseudowire termination for CP1");
            }

            // We establish the reverse tunnel.
            log.info("Deploying process : Establishing reverse direction for pseudowire {}", l2TunnelId);
            egressVlan = determineEgressVlan(pw.l2TunnelPolicy().cP2OuterTag(),
                                             pw.l2TunnelPolicy().cP2InnerTag(),
                                             pw.l2TunnelPolicy().cP1OuterTag(),
                                             pw.l2TunnelPolicy().cP1InnerTag());

            result = deployPseudoWireInit(pw.l2Tunnel(),
                                          pw.l2TunnelPolicy().cP2(),
                                          pw.l2TunnelPolicy().cP1(),
                                          REV,
                                          revNextHop,
                                          oneHop,
                                          egressVlan);
            if (result != SUCCESS) {
                log.error("Deploying process : Error in deploying pseudowire {} initiation for CP2", l2TunnelId);
                return Result.INTERNAL_ERROR
                        .appendError("Error in deploying pseudowire initiation for CP2");
            }

            result = deployPolicy(l2TunnelId,
                                  pw.l2TunnelPolicy().cP2(),
                                  pw.l2TunnelPolicy().cP2InnerTag(),
                                  pw.l2TunnelPolicy().cP2OuterTag(),
                                  egressVlan,
                                  result.getNextId());
            if (result != SUCCESS) {
                log.error("Deploying process : Error in deploying policy {} for CP2", l2TunnelId);
                return Result.INTERNAL_ERROR
                        .appendError("Deploying process : Error in deploying policy for CP2");
            }

            result = deployPseudoWireTerm(pw.l2Tunnel(),
                                          pw.l2TunnelPolicy().cP1(),
                                          egressVlan,
                                          REV,
                                          oneHop);

            if (result != SUCCESS) {
                log.error("Deploying process : Error in deploying pseudowire {} termination for CP2", l2TunnelId);
                return Result.INTERNAL_ERROR.appendError("Error in deploying pseudowire termination for CP2");
            }

            log.info("Deploying process : Updating relevant information for pseudowire {}", l2TunnelId);

            // Populate stores as the final step of the process
            l2TunnelStore.put(Long.toString(l2TunnelId), pw.l2Tunnel());
            l2PolicyStore.put(Long.toString(l2TunnelId), pw.l2TunnelPolicy());

            return Result.SUCCESS;
        } catch (StorageException.Timeout e) {
            log.error("Can not acquire distributed lock for pseudowire {}!", pw.l2Tunnel().tunnelId());
            return Result.INTERNAL_ERROR.appendError("Can not acquire distributed lock!");
        } finally {
            // release the lock
            pwLock.unlock();
        }
    }

    @Override
    public Result checkIfPwExists(long tunnelId, boolean pending) {

        List<L2TunnelDescription> pseudowires = getL2Descriptions(pending)
                .stream()
                .filter(pw -> pw.l2Tunnel().tunnelId() == tunnelId)
                .collect(Collectors.toList());

        if (pseudowires.size() == 0) {
            String store = ((pending) ? "pending" : "installed");
            log.debug("Pseudowire {} does not exist in {} store", tunnelId, store);
            return Result.WRONG_PARAMETERS.
                    appendError("Pseudowire " + tunnelId + " does not exist in " + store);
        } else {
            return SUCCESS;
        }
    }

    /**
     * Tears down connection points of pseudowires. We can either tear down both connection points,
     * or each one of them.
     *
     * @param l2TunnelId The tunnel id for this pseudowire.
     * @param tearDownFirst Boolean, true if we want to tear down cp1
     * @param tearDownSecond Boolean, true if we want to tear down cp2
     * @param pending Boolean, if true remove only pseudowire from pending stores since no flows/groups
     *                in the network, else remove flows/groups in the devices also.
     * @return Result of tearing down the pseudowire, SUCCESS if everything was ok
     *         a descriptive error otherwise.
     */
    private Result tearDownConnectionPoints(long l2TunnelId, boolean tearDownFirst,
                                            boolean tearDownSecond, boolean pending) {

        Result res;
        CompletableFuture<ObjectiveError> fwdInitNextFuture = new CompletableFuture<>();
        CompletableFuture<ObjectiveError> fwdTermNextFuture = new CompletableFuture<>();
        CompletableFuture<ObjectiveError> revInitNextFuture = new CompletableFuture<>();
        CompletableFuture<ObjectiveError> revTermNextFuture = new CompletableFuture<>();

        if (l2TunnelId == 0) {
            log.error("Removal process : Tunnel id cannot be 0");
            return Result.WRONG_PARAMETERS.appendError("Pseudowire id can not be 0.");
        }

        res = checkIfPwExists(l2TunnelId, pending);
        if (res != Result.SUCCESS) {
            return res;
        }

        // remove and get the tunnel and the policy from the appropriate store
        // if null, return error.
        Versioned<L2Tunnel> l2TunnelVersioned = pending ?
                pendingL2TunnelStore.remove(Long.toString(l2TunnelId)) :
                l2TunnelStore.remove(Long.toString(l2TunnelId));
        Versioned<L2TunnelPolicy> l2TunnelPolicyVersioned = pending ?
                pendingL2PolicyStore.remove(Long.toString(l2TunnelId)) :
                l2PolicyStore.remove(Long.toString(l2TunnelId));
        if ((l2TunnelVersioned == null) || (l2TunnelPolicyVersioned == null)) {
            log.warn("Removal process : Policy and/or tunnel missing for tunnel id {}", l2TunnelId);
            return Result.INTERNAL_ERROR
                    .appendError("Policy and/or tunnel missing for pseudowire!");
        }

        L2TunnelDescription pwToRemove = new DefaultL2TunnelDescription(l2TunnelVersioned.value(),
                                                                        l2TunnelPolicyVersioned.value());

        if (pending) {
            // no need to remove flows / groups for a pseudowire
            // in pending state
            return Result.SUCCESS;
        }

        // remove flows/groups involving with this pseudowire
        if (tearDownFirst) {
            log.info("Removal process : Tearing down forward direction of pseudowire {}", l2TunnelId);

            VlanId egressVlan = determineEgressVlan(pwToRemove.l2TunnelPolicy().cP1OuterTag(),
                                                    pwToRemove.l2TunnelPolicy().cP1InnerTag(),
                                                    pwToRemove.l2TunnelPolicy().cP2OuterTag(),
                                                    pwToRemove.l2TunnelPolicy().cP2InnerTag());
            deletePolicy(l2TunnelId,
                         pwToRemove.l2TunnelPolicy().cP1(),
                         pwToRemove.l2TunnelPolicy().cP1InnerTag(),
                         pwToRemove.l2TunnelPolicy().cP1OuterTag(),
                         egressVlan,
                         fwdInitNextFuture,
                         FWD);

            fwdInitNextFuture.thenAcceptAsync(status -> {
                if (status == null) {
                    // Finally we will tear down the pseudo wire.
                    tearDownPseudoWireInit(l2TunnelId,
                                           pwToRemove.l2TunnelPolicy().cP1(),
                                           fwdTermNextFuture,
                                           FWD);
                }
            });

            fwdTermNextFuture.thenAcceptAsync(status -> {
                if (status == null) {
                    tearDownPseudoWireTerm(pwToRemove.l2Tunnel(),
                                           pwToRemove.l2TunnelPolicy().cP2(),
                                           null,
                                           FWD);
                }
            });
        }

        if (tearDownSecond) {
            log.info("Removal process : Tearing down reverse direction of pseudowire {}", l2TunnelId);

            VlanId egressVlan = determineEgressVlan(pwToRemove.l2TunnelPolicy().cP2OuterTag(),
                                             pwToRemove.l2TunnelPolicy().cP2InnerTag(),
                                             pwToRemove.l2TunnelPolicy().cP1OuterTag(),
                                             pwToRemove.l2TunnelPolicy().cP1InnerTag());

            // We do the same operations on the reverse side.
            deletePolicy(l2TunnelId,
                         pwToRemove.l2TunnelPolicy().cP2(),
                         pwToRemove.l2TunnelPolicy().cP2InnerTag(),
                         pwToRemove.l2TunnelPolicy().cP2OuterTag(),
                         egressVlan,
                         revInitNextFuture,
                         REV);

            revInitNextFuture.thenAcceptAsync(status -> {
                if (status == null) {
                    tearDownPseudoWireInit(l2TunnelId,
                                           pwToRemove.l2TunnelPolicy().cP2(),
                                           revTermNextFuture,
                                           REV);
                }
            });

            revTermNextFuture.thenAcceptAsync(status -> {
                if (status == null) {
                    tearDownPseudoWireTerm(pwToRemove.l2Tunnel(),
                                           pwToRemove.l2TunnelPolicy().cP1(),
                                           null,
                                           REV);
                }
            });
        }

        return Result.SUCCESS;
    }

    /**
     * Helper function for removing a single pseudowire.
     *
     * Tries to remove pseudowire from any store it might reside (pending or installed).
     *
     * @param l2TunnelId the id of the pseudowire to tear down
     * @return Returns SUCCESS if no error is obeserved or an appropriate
     * error on a failure
     */
    public Result tearDownPseudowire(long l2TunnelId) {

        try {
            // take the lock
            pwLock.lock();

            if (checkIfPwExists(l2TunnelId, true) == Result.SUCCESS) {
                return tearDownConnectionPoints(l2TunnelId, true, true, true);
            } else if (checkIfPwExists(l2TunnelId, false) == Result.SUCCESS) {
                return tearDownConnectionPoints(l2TunnelId, true, true, false);
            } else {
                return Result.WRONG_PARAMETERS.appendError("Pseudowire with "
                                                                   + l2TunnelId
                                                                   + " did not reside in any store!");
            }
        } catch (StorageException.Timeout e) {
            log.error("Can not acquire distributed lock for pseudowire {}!", l2TunnelId);
            return Result.INTERNAL_ERROR.appendError("Can not acquire distributed lock!");
        } finally {
            // release the lock
            pwLock.unlock();
        }
    }

    @Override
    @Deprecated
    public void tearDown(Set<L2TunnelDescription> pwToRemove) {

        for (L2TunnelDescription currentL2Tunnel : pwToRemove) {

            long tunnelId = currentL2Tunnel.l2TunnelPolicy().tunnelId();
            log.info("Removing pseudowire {}", tunnelId);

            Result result = tearDownPseudowire(tunnelId);
            if (result != Result.SUCCESS) {
                log.error("Could not remove pseudowire {}!", tunnelId);
            }
        }
    }

    /**
     * Handles the policy establishment which consists in
     * create the filtering and forwarding objectives related
     * to the initiation and termination.
     *
     * @param tunnelId     the tunnel id
     * @param ingress      the ingress point
     * @param ingressInner the ingress inner tag
     * @param ingressOuter the ingress outer tag
     * @param egressVlan   Vlan-id to set, depends on ingress vlan
     *                     combinations. For example, if pw is double tagged
     *                     then this is the value of the outer vlan, if single
     *                     tagged then it is the new value of the single tag.
     *                     Should be None for untagged traffic.
     * @param nextId       the next objective id
     * @return the result of the operation
     */
    private Result deployPolicy(long tunnelId, ConnectPoint ingress, VlanId ingressInner,
                                VlanId ingressOuter, VlanId egressVlan, int nextId) {
        log.debug("Starting deploying policy for pseudowire {}.", tunnelId);

        List<Objective> objectives = Lists.newArrayList();
        // We create the forwarding objective for supporting
        // the l2 tunnel.
        ForwardingObjective.Builder fwdBuilder = createInitFwdObjective(tunnelId, ingress.port(), nextId);
        // We create and add objective context.
        ObjectiveContext context = new DefaultObjectiveContext((objective) ->
                                                                log.debug("FwdObj for tunnel {} populated", tunnelId),
                                                               (objective, error) ->
                                                                log.warn("Failed to populate fwdObj " +
                                                                                 "for tunnel {} : {}",
                                                                         tunnelId, error));
        objectives.add(fwdBuilder.add(context));

        // We create the filtering objective to define the
        // permit traffic in the switch
        FilteringObjective.Builder filtBuilder = createFiltObjective(ingress.port(), ingressInner, ingressOuter);

        // We add the metadata.
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment
                .builder()
                .setTunnelId(tunnelId)
                .setVlanId(egressVlan);
        filtBuilder.withMeta(treatment.build());

        // We create and add objective context.
        context = new DefaultObjectiveContext((objective) -> log.debug("FilterObj for tunnel {} populated", tunnelId),
                                              (objective, error) -> log.warn("Failed to populate filterObj for " +
                                                                                     "tunnel {} : {}",
                                                                             tunnelId, error));
        objectives.add(filtBuilder.add(context));

        for (Objective objective : objectives) {
            if (objective instanceof ForwardingObjective) {
                srManager.flowObjectiveService.forward(ingress.deviceId(), (ForwardingObjective) objective);
                log.debug("Creating new FwdObj for initiation NextObj with id={} for tunnel {}", nextId, tunnelId);
            } else {
                srManager.flowObjectiveService.filter(ingress.deviceId(), (FilteringObjective) objective);
                log.debug("Creating new FiltObj for tunnel {}", tunnelId);
            }
        }
        return SUCCESS;
    }

    /**
     * Handles the tunnel establishment which consists in
     * create the next objectives related to the initiation.
     *
     * @param l2Tunnel  the tunnel to deploy
     * @param ingress   the ingress connect point
     * @param egress    the egress connect point
     * @param direction the direction of the pw
     * @param nextHop next hop of the initiation point
     * @param oneHop if this pseudowire has only one link
     * @param termVlanId the termination vlan id
     * @return the result of the operation
     */
    private Result deployPseudoWireInit(L2Tunnel l2Tunnel, ConnectPoint ingress,
                                        ConnectPoint egress, Direction direction,
                                        Link nextHop, boolean oneHop, VlanId termVlanId) {
        log.debug("Started deploying init next objectives for pseudowire {} for tunnel {} -> {}.",
                  l2Tunnel.tunnelId(), ingress, egress);
        if (nextHop == null) {
            log.warn("No path between ingress and egress connection points for tunnel {}", l2Tunnel.tunnelId());
            return WRONG_PARAMETERS;
        }

        // We create the next objective without the metadata
        // context and id. We check if it already exists in the
        // store. If not we store as it is in the store.
        NextObjective.Builder nextObjectiveBuilder = createNextObjective(INITIATION,
                                                                         nextHop.src(),
                                                                         nextHop.dst(),
                                                                         l2Tunnel,
                                                                         egress.deviceId(),
                                                                         oneHop,
                                                                         termVlanId);

        if (nextObjectiveBuilder == null) {
            return INTERNAL_ERROR;
        }
        // We set the metadata. We will use this metadata
        // to inform the driver we are doing a l2 tunnel.
        TrafficSelector metadata = DefaultTrafficSelector
                .builder()
                .matchTunnelId(l2Tunnel.tunnelId())
                .build();
        nextObjectiveBuilder.withMeta(metadata);
        int nextId = srManager.flowObjectiveService.allocateNextId();
        if (nextId < 0) {
            log.warn("Not able to allocate a next id for initiation");
            return INTERNAL_ERROR;
        }
        nextObjectiveBuilder.withId(nextId);
        String key = generateKey(l2Tunnel.tunnelId(), direction);
        l2InitiationNextObjStore.put(key, nextObjectiveBuilder.add());
        ObjectiveContext context = new DefaultObjectiveContext((objective) ->
                                                                 log.debug("Initiation l2 tunnel rule " +
                                                                                   "for {} populated",
                                                                           l2Tunnel.tunnelId()),
                                                               (objective, error) ->
                                                                       log.warn("Failed to populate Initiation " +
                                                                                        "l2 tunnel rule for {}: {}",
                                                                                l2Tunnel.tunnelId(), error));
        NextObjective nextObjective = nextObjectiveBuilder.add(context);
        srManager.flowObjectiveService.next(ingress.deviceId(), nextObjective);
        log.debug("Initiation next objective for {} not found. Creating new NextObj with id={}",
                  l2Tunnel.tunnelId(), nextObjective.id());
        Result result = SUCCESS;
        result.setNextId(nextObjective.id());
        return result;
    }

    /**
     * Handles the tunnel termination, which consists in the creation
     * of a forwarding objective and a next objective.
     *
     * @param l2Tunnel   the tunnel to terminate
     * @param egress     the egress point
     * @param egressVlan the expected vlan at egress
     * @param direction  the direction
     * @return the result of the operation
     */
    private Result deployPseudoWireTerm(L2Tunnel l2Tunnel, ConnectPoint egress,
                                        VlanId egressVlan, Direction direction,
                                        boolean oneHop) {
        log.debug("Started deploying termination objectives for pseudowire {} , direction {}.",
                  l2Tunnel.tunnelId(), direction == FWD ? "forward" : "reverse");

        // We create the group relative to the termination.
        NextObjective.Builder nextObjectiveBuilder = createNextObjective(TERMINATION, egress, null,
                                                                         l2Tunnel, egress.deviceId(),
                                                                         oneHop,
                                                                         egressVlan);
        if (nextObjectiveBuilder == null) {
            return INTERNAL_ERROR;
        }
        TrafficSelector metadata = DefaultTrafficSelector
                .builder()
                .matchVlanId(egressVlan)
                .build();
        nextObjectiveBuilder.withMeta(metadata);
        int nextId = srManager.flowObjectiveService.allocateNextId();
        if (nextId < 0) {
            log.warn("Not able to allocate a next id for initiation");
            return INTERNAL_ERROR;
        }
        nextObjectiveBuilder.withId(nextId);
        String key = generateKey(l2Tunnel.tunnelId(), direction);
        l2TerminationNextObjStore.put(key, nextObjectiveBuilder.add());
        ObjectiveContext context = new DefaultObjectiveContext((objective) -> log.debug("Termination l2 tunnel rule " +
                                                                                        "for {} populated",
                                                                                        l2Tunnel.tunnelId()),
                                                               (objective, error) -> log.warn("Failed to populate " +
                                                                                              "termination l2 tunnel " +
                                                                                              "rule for {}: {}",
                                                                                              l2Tunnel.tunnelId(),
                                                                                              error));
        NextObjective nextObjective = nextObjectiveBuilder.add(context);
        srManager.flowObjectiveService.next(egress.deviceId(), nextObjective);
        log.debug("Termination next objective for {} not found. Creating new NextObj with id={}",
                  l2Tunnel.tunnelId(), nextObjective.id());

        // We create the flow relative to the termination.
        ForwardingObjective.Builder fwdBuilder = createTermFwdObjective(l2Tunnel.pwLabel(), l2Tunnel.tunnelId(),
                                                                        egress.port(), nextObjective.id());
        context = new DefaultObjectiveContext((objective) -> log.debug("FwdObj for tunnel termination {} populated",
                                                                       l2Tunnel.tunnelId()),
                                              (objective, error) -> log.warn("Failed to populate fwdrObj" +
                                                                             " for tunnel termination {} : {}",
                                                                             l2Tunnel.tunnelId(), error));
        srManager.flowObjectiveService.forward(egress.deviceId(), fwdBuilder.add(context));
        log.debug("Creating new FwdObj for termination NextObj with id={} for tunnel {}",
                  nextId, l2Tunnel.tunnelId());

        return SUCCESS;
    }


    /**
     * Creates the filtering objective according to a given policy.
     *
     * @param inPort   the in port
     * @param innerTag the inner vlan tag
     * @param outerTag the outer vlan tag
     * @return the filtering objective
     */
    private FilteringObjective.Builder createFiltObjective(PortNumber inPort, VlanId innerTag, VlanId outerTag) {

        log.debug("Creating connection point filtering objective for vlans {} / {}", outerTag, innerTag);
        return DefaultFilteringObjective
                .builder()
                .withKey(Criteria.matchInPort(inPort))
                .addCondition(Criteria.matchInnerVlanId(innerTag))
                .addCondition(Criteria.matchVlanId(outerTag))
                .withPriority(SegmentRoutingService.DEFAULT_PRIORITY)
                .permit()
                .fromApp(srManager.appId());
    }

    /**
     * Creates the forwarding objective for the termination.
     *
     * @param pwLabel    the pseudo wire label
     * @param tunnelId   the tunnel id
     * @param egressPort the egress port
     * @param nextId     the next step
     * @return the forwarding objective to support the termination
     */
    private ForwardingObjective.Builder createTermFwdObjective(MplsLabel pwLabel, long tunnelId,
                                                               PortNumber egressPort, int nextId) {

        log.debug("Creating forwarding objective for termination for tunnel {} : pwLabel {}, egressPort {}, nextId {}",
                 tunnelId, pwLabel, egressPort, nextId);
        TrafficSelector.Builder trafficSelector = DefaultTrafficSelector.builder();
        TrafficTreatment.Builder trafficTreatment = DefaultTrafficTreatment.builder();
        // The flow has to match on the pw label and bos
        trafficSelector.matchEthType(Ethernet.MPLS_UNICAST);
        trafficSelector.matchMplsLabel(pwLabel);
        trafficSelector.matchMplsBos(true);
        // The flow has to decrement ttl, restore ttl in
        // pop mpls, set tunnel id and port.
        trafficTreatment.decMplsTtl();
        trafficTreatment.copyTtlIn();
        trafficTreatment.popMpls();
        trafficTreatment.setTunnelId(tunnelId);
        trafficTreatment.setOutput(egressPort);

        return DefaultForwardingObjective
                .builder()
                .fromApp(srManager.appId())
                .makePermanent()
                .nextStep(nextId)
                .withPriority(SegmentRoutingService.DEFAULT_PRIORITY)
                .withSelector(trafficSelector.build())
                .withTreatment(trafficTreatment.build())
                .withFlag(VERSATILE);
    }

    /**
     * Creates the forwarding objective for the initiation.
     *
     * @param tunnelId the tunnel id
     * @param inPort   the input port
     * @param nextId   the next step
     * @return the forwarding objective to support the initiation.
     */
    private ForwardingObjective.Builder createInitFwdObjective(long tunnelId, PortNumber inPort, int nextId) {

        log.debug("Creating forwarding objective for tunnel {} : Port {} , nextId {}", tunnelId, inPort, nextId);
        TrafficSelector.Builder trafficSelector = DefaultTrafficSelector.builder();

        // The flow has to match on the mpls logical
        // port and the tunnel id.
        trafficSelector.matchTunnelId(tunnelId);
        trafficSelector.matchInPort(inPort);

        return DefaultForwardingObjective
                .builder()
                .fromApp(srManager.appId())
                .makePermanent()
                .nextStep(nextId)
                .withPriority(SegmentRoutingService.DEFAULT_PRIORITY)
                .withSelector(trafficSelector.build())
                .withFlag(VERSATILE);

    }

    /**
     * Creates the next objective according to a given
     * pipeline. We don't set the next id and we don't
     * create the final meta to check if we are re-using
     * the same next objective for different tunnels.
     *
     * @param pipeline the pipeline to support
     * @param srcCp    the source port
     * @param dstCp    the destination port
     * @param l2Tunnel the tunnel to support
     * @param egressId the egress device id
     * @param oneHop if the pw only has one hop, push only pw label
     * @param termVlanId the outer vlan id of the packet exiting a termination point
     * @return the next objective to support the pipeline
     */
    private NextObjective.Builder createNextObjective(Pipeline pipeline, ConnectPoint srcCp,
                                                      ConnectPoint dstCp,  L2Tunnel l2Tunnel,
                                                      DeviceId egressId, boolean oneHop,
                                                      VlanId termVlanId) {
        log.debug("Creating {} next objective for pseudowire {}.",
                  pipeline == TERMINATION ? "termination" : "inititation");

        NextObjective.Builder nextObjBuilder;
        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
        if (pipeline == INITIATION) {
            nextObjBuilder = DefaultNextObjective
                    .builder()
                    .withType(NextObjective.Type.SIMPLE)
                    .fromApp(srManager.appId());
            // The pw label is the bottom of stack. It has to
            // be different -1.
            if (l2Tunnel.pwLabel().toInt() == MplsLabel.MAX_MPLS) {
                log.error("Pw label not configured");
                return null;
            }
            treatmentBuilder.pushMpls();
            treatmentBuilder.setMpls(l2Tunnel.pwLabel());
            treatmentBuilder.setMplsBos(true);
            treatmentBuilder.copyTtlOut();

            // If the inter-co label is present we have to set the label.
            if (l2Tunnel.interCoLabel().toInt() != MplsLabel.MAX_MPLS) {
                treatmentBuilder.pushMpls();
                treatmentBuilder.setMpls(l2Tunnel.interCoLabel());
                treatmentBuilder.setMplsBos(false);
                treatmentBuilder.copyTtlOut();
            }

            // if not oneHop install transit mpls labels also
            if (!oneHop) {
                // We retrieve the sr label from the config
                // specific for pseudowire traffic
                // using the egress leaf device id.
                MplsLabel srLabel;
                try {
                    srLabel = MplsLabel.mplsLabel(srManager.deviceConfiguration().getPWRoutingLabel(egressId));

                } catch (DeviceConfigNotFoundException e) {
                    log.error("Sr label for pw traffic not configured");
                    return null;
                }

                treatmentBuilder.pushMpls();
                treatmentBuilder.setMpls(srLabel);
                treatmentBuilder.setMplsBos(false);
                treatmentBuilder.copyTtlOut();
            }

            // We have to rewrite the src and dst mac address.
            MacAddress ingressMac;
            try {
                ingressMac = srManager.deviceConfiguration().getDeviceMac(srcCp.deviceId());
            } catch (DeviceConfigNotFoundException e) {
                log.error("Was not able to find the ingress mac");
                return null;
            }
            treatmentBuilder.setEthSrc(ingressMac);
            MacAddress neighborMac;
            try {
                neighborMac = srManager.deviceConfiguration().getDeviceMac(dstCp.deviceId());
            } catch (DeviceConfigNotFoundException e) {
                log.error("Was not able to find the neighbor mac");
                return null;
            }
            treatmentBuilder.setEthDst(neighborMac);

            // set the appropriate transport vlan from tunnel information
            treatmentBuilder.setVlanId(l2Tunnel.transportVlan());
        } else {
            // We create the next objective which
            // will be a simple l2 group.
            nextObjBuilder = DefaultNextObjective
                    .builder()
                    .withType(NextObjective.Type.SIMPLE)
                    .fromApp(srManager.appId());

            // for termination point we use the outer vlan of the
            // encapsulated packet for the vlan
            treatmentBuilder.setVlanId(termVlanId);
        }

        treatmentBuilder.setOutput(srcCp.port());
        nextObjBuilder.addTreatment(treatmentBuilder.build());
        return nextObjBuilder;
    }

    /**
     * Reverse an l2 tunnel policy in order to have as CP1 the leaf switch,
     * in case one of the switches is a spine.
     *
     * This makes possible the usage of SRLinkWeigher for computing valid paths,
     * which cuts leaf-spine links from the path computation with a src different
     * than the source leaf.
     *
     * @param policy The policy to reverse, if needed
     * @return a l2TunnelPolicy containing the leaf at CP1, suitable for usage with
     *         current SRLinkWeigher
     */
    private L2TunnelPolicy reverseL2TunnelPolicy(L2TunnelPolicy policy) {

        log.debug("Reversing policy for pseudowire.");
        try {
            // if cp1 is a leaf, just return
            if (srManager.deviceConfiguration().isEdgeDevice(policy.cP1().deviceId())) {
                return policy;
            } else {
                // return a policy with reversed cp1 and cp2, and also with reversed tags
                return new DefaultL2TunnelPolicy(policy.tunnelId(),
                                                 policy.cP2(), policy.cP2InnerTag(), policy.cP2OuterTag(),
                                                 policy.cP1(), policy.cP1InnerTag(), policy.cP1OuterTag());

            }
        } catch (DeviceConfigNotFoundException e) {
            // should never come here, since it has been checked before
            log.error("Configuration for device {}, does not exist!");
            return null;
        }
    }

    /**
     * Reverses a link.
     *
     * @param link link to be reversed
     * @return the reversed link
     */
    private Link reverseLink(Link link) {

        DefaultLink.Builder linkBuilder = DefaultLink.builder();

        linkBuilder.src(link.dst());
        linkBuilder.dst(link.src());
        linkBuilder.type(link.type());
        linkBuilder.providerId(link.providerId());

        return linkBuilder.build();
    }

    /**
     * Returns the path betwwen two connect points.
     *
     * @param srcCp source connect point
     * @param dstCp destination connect point
     * @return the path
     */
    private List<Link> getPath(ConnectPoint srcCp, ConnectPoint dstCp) {

        // use SRLinkWeigher to avoid pair links, and also
        // avoid going from the spine to the leaf and to the
        // spine again, we need to have the leaf as CP1 here.
        LinkWeigher srLw = new SRLinkWeigher(srManager, srcCp.deviceId(), new HashSet<Link>());

        Set<Path> paths = srManager.topologyService.
                getPaths(srManager.topologyService.currentTopology(),
                srcCp.deviceId(), dstCp.deviceId(), srLw);

        log.debug("Paths obtained from topology service {}", paths);

        // We randomly pick a path.
        if (paths.isEmpty()) {
            return null;
        }
        int size = paths.size();
        int index = RandomUtils.nextInt(0, size);

        List<Link> result = Iterables.get(paths, index).links();
        log.debug("Randomly picked a path {}", result);

        return result;
    }

    /**
     * Deletes a given policy using the parameter supplied.
     *
     * @param tunnelId     the tunnel id
     * @param ingress      the ingress point
     * @param ingressInner the ingress inner vlan id
     * @param ingressOuter the ingress outer vlan id
     * @param future       to perform the async operation
     * @param direction    the direction: forward or reverse
     */
    private void deletePolicy(long tunnelId, ConnectPoint ingress, VlanId ingressInner, VlanId ingressOuter,
                              VlanId egressVlan, CompletableFuture<ObjectiveError> future, Direction direction) {

        String key = generateKey(tunnelId, direction);
        if (!l2InitiationNextObjStore.containsKey(key)) {
            log.error("Abort delete of policy for tunnel {}: next does not exist in the store", tunnelId);
            if (future != null) {
                future.complete(null);
            }
            return;
        }
        NextObjective nextObjective = l2InitiationNextObjStore.get(key).value();
        int nextId = nextObjective.id();
        List<Objective> objectives = Lists.newArrayList();
        // We create the forwarding objective.
        ForwardingObjective.Builder fwdBuilder = createInitFwdObjective(tunnelId, ingress.port(), nextId);
        ObjectiveContext context = new ObjectiveContext() {
            @Override
            public void onSuccess(Objective objective) {
                log.debug("Previous fwdObj for policy {} removed", tunnelId);
                if (future != null) {
                    future.complete(null);
                }
            }

            @Override
            public void onError(Objective objective, ObjectiveError error) {
                log.error("Failed to remove previous fwdObj for policy {}: {}", tunnelId, error);
                if (future != null) {
                    future.complete(error);
                }
            }
        };
        objectives.add(fwdBuilder.remove(context));
        // We create the filtering objective to define the
        // permit traffic in the switch
        FilteringObjective.Builder filtBuilder = createFiltObjective(ingress.port(), ingressInner, ingressOuter);
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment
                .builder()
                .setTunnelId(tunnelId)
                .setVlanId(egressVlan);
        filtBuilder.withMeta(treatment.build());
        context = new DefaultObjectiveContext((objective) -> log.debug("FilterObj for policy {} revoked", tunnelId),
                                              (objective, error) ->
                                                      log.warn("Failed to revoke filterObj for policy {}",
                                                               tunnelId, error));
        objectives.add(filtBuilder.remove(context));

        for (Objective objective : objectives) {
            if (objective instanceof ForwardingObjective) {
                srManager.flowObjectiveService.forward(ingress.deviceId(), (ForwardingObjective) objective);
            } else {
                srManager.flowObjectiveService.filter(ingress.deviceId(), (FilteringObjective) objective);
            }
        }
    }

    /**
     * Deletes the pseudo wire initiation.
     *
     * @param l2TunnelId the tunnel id
     * @param ingress    the ingress connect point
     * @param future     to perform an async operation
     * @param direction  the direction: reverse of forward
     */
    private void tearDownPseudoWireInit(long l2TunnelId, ConnectPoint ingress,
                                        CompletableFuture<ObjectiveError> future, Direction direction) {
        log.debug("Starting tearing dowing initation of pseudowire {} for direction {}.",
                  l2TunnelId, direction == FWD ? "forward" : "reverse");
        String key = generateKey(l2TunnelId, direction);
        if (!l2InitiationNextObjStore.containsKey(key)) {
            log.error("Abort delete of {} for {}: next does not exist in the store", INITIATION, key);
            if (future != null) {
                future.complete(null);
            }
            return;
        }

        // un-comment in case you want to delete groups used by the pw
        // however, this will break the update of pseudowires cause the L2 interface group can
        // not be deleted (it is referenced by other groups)
        /*
        NextObjective nextObjective = l2InitiationNextObjStore.get(key).value();
        ObjectiveContext context = new ObjectiveContext() {
            @Override
            public void onSuccess(Objective objective) {
                log.debug("Previous {} next for {} removed", INITIATION, key);
                if (future != null) {
                    future.complete(null);
                }
            }

            @Override
            public void onError(Objective objective, ObjectiveError error) {
                log.warn("Failed to remove previous {} next for {}: {}", INITIATION, key, error);
                if (future != null) {
                    future.complete(error);
                }
            }
        };
        srManager.flowObjectiveService.next(ingress.deviceId(), (NextObjective) nextObjective.copy().remove(context));
        */

        future.complete(null);
        l2InitiationNextObjStore.remove(key);
    }

    /**
     * Deletes the pseudo wire termination.
     *
     * @param l2Tunnel  the tunnel
     * @param egress    the egress connect point
     * @param future    the async task
     * @param direction the direction of the tunnel
     */
    private void tearDownPseudoWireTerm(L2Tunnel l2Tunnel,
                                        ConnectPoint egress,
                                        CompletableFuture<ObjectiveError> future,
                                        Direction direction) {
        log.debug("Starting tearing down termination for pseudowire {} direction {}.",
                  l2Tunnel.tunnelId(), direction == FWD ? "forward" : "reverse");
        String key = generateKey(l2Tunnel.tunnelId(), direction);
        if (!l2TerminationNextObjStore.containsKey(key)) {
            log.error("Abort delete of {} for {}: next does not exist in the store", TERMINATION, key);
            if (future != null) {
                future.complete(null);
            }
            return;
        }
        NextObjective nextObjective = l2TerminationNextObjStore.get(key).value();
        ForwardingObjective.Builder fwdBuilder = createTermFwdObjective(l2Tunnel.pwLabel(),
                                                                        l2Tunnel.tunnelId(),
                                                                        egress.port(),
                                                                        nextObjective.id());
        ObjectiveContext context = new DefaultObjectiveContext((objective) ->
                                                                       log.debug("FwdObj for {} {}, " +
                                                                                         "direction {} removed",
                                                                                        TERMINATION,
                                                                                        l2Tunnel.tunnelId(),
                                                                                        direction),
                                                               (objective, error) ->
                                                                       log.warn("Failed to remove fwdObj " +
                                                                                        "for {} {}" +
                                                                                        ", direction {}",
                                                                                TERMINATION,
                                                                                l2Tunnel.tunnelId(),
                                                                                error,
                                                                                direction));
        srManager.flowObjectiveService.forward(egress.deviceId(), fwdBuilder.remove(context));

        // un-comment in case you want to delete groups used by the pw
        // however, this will break the update of pseudowires cause the L2 interface group can
        // not be deleted (it is referenced by other groups)
        /*
        context = new ObjectiveContext() {
            @Override
            public void onSuccess(Objective objective) {
                log.debug("Previous {} next for {} removed", TERMINATION, key);
                if (future != null) {
                    future.complete(null);
                }
            }

            @Override
            public void onError(Objective objective, ObjectiveError error) {
                log.warn("Failed to remove previous {} next for {}: {}", TERMINATION, key, error);
                if (future != null) {
                    future.complete(error);
                }
            }
        };
        srManager.flowObjectiveService.next(egress.deviceId(), (NextObjective) nextObjective.copy().remove(context));
        */

        l2TerminationNextObjStore.remove(key);
        future.complete(null);
    }

    /**
     * Utilities to generate pw key.
     *
     * @param tunnelId  the tunnel id
     * @param direction the direction of the pw
     * @return the key of the store
     */
    private String generateKey(long tunnelId, Direction direction) {
        return String.format("%s-%s", tunnelId, direction);
    }
}
