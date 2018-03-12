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
import org.onosproject.segmentrouting.SegmentRoutingManager;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.config.DeviceConfigNotFoundException;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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

/**
 * Handles pwaas related events.
 */
public class DefaultL2TunnelHandler implements L2TunnelHandler {

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

    private final KryoNamespace.Builder l2TunnelKryo;

    /**
     * Contains transport vlans used for spine-leaf pseudowires.
     */
    private final DistributedSet<VlanId> vlanStore;

    /**
     * Used for determining transport vlans for leaf-spine.
     */
    private short transportVlanUpper = 4093, transportVlanLower = 3500;

    private static final VlanId UNTAGGED_TRANSPORT_VLAN = VlanId.vlanId((short) 4094);

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

        vlanStore = srManager.storageService.<VlanId>setBuilder()
                .withName("onos-transport-vlan-store")
                .withSerializer(Serializer.using(
                        new KryoNamespace.Builder()
                                .register(KryoNamespaces.API)
                                .build()))
                .build()
                .asDistributedSet();
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
    public Set<L2TunnelDescription> getL2Descriptions() {
        List<L2Tunnel> tunnels = getL2Tunnels();
        List<L2TunnelPolicy> policies = getL2Policies();

        // determine affected pseudowires and update them at once
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

    /**
     * Returns all L2 Policies.
     *
     * @return List of policies
     */
    @Override
    public List<L2TunnelPolicy> getL2Policies() {

        return new ArrayList<>(l2PolicyStore
                .values()
                .stream()
                .map(Versioned::value)
                .collect(Collectors.toList()));
    }

    /**
     * Returns all L2 Tunnels.
     *
     * @return List of tunnels.
     */
    @Override
    public List<L2Tunnel> getL2Tunnels() {

        return new ArrayList<>(l2TunnelStore
                .values()
                .stream()
                .map(Versioned::value)
                .collect(Collectors.toList()));
    }

    @Override
    public void processLinkDown(Link link) {

        List<L2Tunnel> tunnels = getL2Tunnels();
        List<L2TunnelPolicy> policies = getL2Policies();

        // determine affected pseudowires and update them at once
        Set<L2TunnelDescription> pwToUpdate = tunnels
                .stream()
                .filter(tun -> tun.pathUsed().contains(link))
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


        log.info("Pseudowires affected by link failure : {}, rerouting them...", pwToUpdate);

        // update all pseudowires
        pwToUpdate.forEach(tun -> updatePw(tun, tun));
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
     * Determines vlan used for transporting the pw traffic.
     *
     * Leaf-Leaf traffic is transferred untagged, thus we choose the UNTAGGED_TRANSPORT_VLAN
     * and also make sure to add the popVlan instruction.
     * For spine-leaf pws we choose the highest vlan value available from a certain range.
     *
     * @param spinePw if the pw is leaf-spine.
     * @return The vlan id chossen to transport this pseudowire. If vlan is UNTAGGED_TRANSPORT_VLAN
     *         then the pw is transported untagged.
     */
    private VlanId determineTransportVlan(boolean spinePw) {

        if (!spinePw) {

            log.info("Untagged transport with internal vlan {} for pseudowire!", UNTAGGED_TRANSPORT_VLAN);
            return UNTAGGED_TRANSPORT_VLAN;
        } else {
            for (short i = transportVlanUpper; i > transportVlanLower; i--) {

                VlanId vlanToUse = VlanId.vlanId((short) i);
                if (!vlanStore.contains(vlanToUse)) {

                    vlanStore.add(vlanToUse);
                    log.info("Transport vlan {} for pseudowire!", vlanToUse);
                    return vlanToUse;
                }
            }

            log.info("No available transport vlan found, pseudowire traffic will be carried untagged " +
                             "with internal vlan {}!", UNTAGGED_TRANSPORT_VLAN);
            return UNTAGGED_TRANSPORT_VLAN;
        }
    }

    /**
     * Returns true if path size is valid according to the current logic.
     *
     * @param pathSize The size of the path
     * @return True if path size is valid, false otherwise.
     */
    private boolean isValidPathSize(int pathSize) {
        return ((pathSize >= 1) && (pathSize <= 4));
    }

    /**
     * Adds a single pseudowire.
     *
     * @param pw The pseudowire
     * @param spinePw True if pseudowire is from leaf to spine
     * @return result of pseudowire deployment
     */
    private Result deployPseudowire(L2TunnelDescription pw, boolean spinePw) {

        Result result;
        long l2TunnelId;

        l2TunnelId = pw.l2Tunnel().tunnelId();

        // The tunnel id cannot be 0.
        if (l2TunnelId == 0) {
            log.warn("Tunnel id id must be > 0");
            return Result.ADDITION_ERROR;
        }

        // get path here, need to use the same for fwd and rev direction
        List<Link> path = getPath(pw.l2TunnelPolicy().cP1(),
                                  pw.l2TunnelPolicy().cP2());
        if (path == null) {
            log.info("Deploying process : No path between the connection points for pseudowire {}", l2TunnelId);
            return WRONG_PARAMETERS;
        }

        Link fwdNextHop;
        Link revNextHop;
        if (!isValidPathSize(path.size())) {
            log.error("Deploying process : Path size for pseudowire should be of" +
                              " one of the following sizes = [1, 2, 3, 4], for pseudowire {}",
                      l2TunnelId);
            return INTERNAL_ERROR;
        }

        // spinePw signifies if we have a leaf-spine pw
        // thus only one label should be pushed (that of pw)
        // if size>1 we need to push intermediate labels also.
        if (path.size() > 1) {
            spinePw = false;
        }

        fwdNextHop = path.get(0);
        revNextHop = reverseLink(path.get(path.size() - 1));

        pw.l2Tunnel().setPath(path);
        pw.l2Tunnel().setTransportVlan(determineTransportVlan(spinePw));

        // next hops for next objectives
        log.info("Deploying process : Establishing forward direction for pseudowire {}", l2TunnelId);

        VlanId egressVlan = determineEgressVlan(pw.l2TunnelPolicy().cP1OuterTag(),
                                                pw.l2TunnelPolicy().cP1InnerTag(),
                                                pw.l2TunnelPolicy().cP2OuterTag(),
                                                pw.l2TunnelPolicy().cP2InnerTag());
        // We establish the tunnel.
        // result.nextId will be used in fwd
        result = deployPseudoWireInit(pw.l2Tunnel(),
                                      pw.l2TunnelPolicy().cP1(),
                                      pw.l2TunnelPolicy().cP2(),
                                      FWD,
                                      fwdNextHop,
                                      spinePw,
                                      egressVlan);
        if (result != SUCCESS) {
            log.info("Deploying process : Error in deploying pseudowire initiation for CP1");
            return Result.ADDITION_ERROR;
        }

        // We create the policy.
        result = deployPolicy(l2TunnelId,
                              pw.l2TunnelPolicy().cP1(),
                              pw.l2TunnelPolicy().cP1InnerTag(),
                              pw.l2TunnelPolicy().cP1OuterTag(),
                              egressVlan,
                              result.nextId);
        if (result != SUCCESS) {
            log.info("Deploying process : Error in deploying pseudowire policy for CP1");
            return Result.ADDITION_ERROR;
        }

        // We terminate the tunnel
        result = deployPseudoWireTerm(pw.l2Tunnel(),
                                       pw.l2TunnelPolicy().cP2(),
                                       egressVlan,
                                       FWD,
                                      spinePw);

        if (result != SUCCESS) {
            log.info("Deploying process : Error in deploying pseudowire termination for CP1");
            return Result.ADDITION_ERROR;

        }

        log.info("Deploying process : Establishing reverse direction for pseudowire {}", l2TunnelId);

        egressVlan = determineEgressVlan(pw.l2TunnelPolicy().cP2OuterTag(),
                                         pw.l2TunnelPolicy().cP2InnerTag(),
                                         pw.l2TunnelPolicy().cP1OuterTag(),
                                         pw.l2TunnelPolicy().cP1InnerTag());

        // We establish the reverse tunnel.
        result = deployPseudoWireInit(pw.l2Tunnel(),
                                       pw.l2TunnelPolicy().cP2(),
                                       pw.l2TunnelPolicy().cP1(),
                                       REV,
                                       revNextHop,
                                       spinePw,
                                       egressVlan);
        if (result != SUCCESS) {
            log.info("Deploying process : Error in deploying pseudowire initiation for CP2");
            return Result.ADDITION_ERROR;
        }


        result = deployPolicy(l2TunnelId,
                               pw.l2TunnelPolicy().cP2(),
                               pw.l2TunnelPolicy().cP2InnerTag(),
                               pw.l2TunnelPolicy().cP2OuterTag(),
                               egressVlan,
                               result.nextId);
        if (result != SUCCESS) {
            log.info("Deploying process : Error in deploying policy for CP2");
            return Result.ADDITION_ERROR;
        }

        result = deployPseudoWireTerm(pw.l2Tunnel(),
                                       pw.l2TunnelPolicy().cP1(),
                                       egressVlan,
                                       REV,
                                      spinePw);

        if (result != SUCCESS) {
            log.info("Deploying process : Error in deploying pseudowire termination for CP2");
            return Result.ADDITION_ERROR;
        }

        log.info("Deploying process : Updating relevant information for pseudowire {}", l2TunnelId);

        // Populate stores
        l2TunnelStore.put(Long.toString(l2TunnelId), pw.l2Tunnel());
        l2PolicyStore.put(Long.toString(l2TunnelId), pw.l2TunnelPolicy());

        return Result.SUCCESS;
    }

    /**
     * To deploy a number of pseudo wires.
     *
     * @param pwToAdd the set of pseudo wires to add
     */
    public void deploy(Set<L2TunnelDescription> pwToAdd) {

        Result result;

        for (L2TunnelDescription currentL2Tunnel : pwToAdd) {
            ConnectPoint cp1 = currentL2Tunnel.l2TunnelPolicy().cP1();
            ConnectPoint cp2 = currentL2Tunnel.l2TunnelPolicy().cP2();
            long tunnelId = currentL2Tunnel.l2TunnelPolicy().tunnelId();


            try {
                // differentiate between leaf-leaf pseudowires and leaf-spine
                // and pass the appropriate flag in them.
                if (!srManager.deviceConfiguration().isEdgeDevice(cp1.deviceId()) &&
                    !srManager.deviceConfiguration().isEdgeDevice(cp2.deviceId())) {
                    log.warn("Can not deploy pseudowire from spine to spine!");
                    result = Result.INTERNAL_ERROR;
                } else if (srManager.deviceConfiguration().isEdgeDevice(cp1.deviceId()) &&
                     srManager.deviceConfiguration().isEdgeDevice(cp2.deviceId())) {
                    log.info("Deploying a leaf-leaf pseudowire {}", tunnelId);
                    result = deployPseudowire(currentL2Tunnel, false);
                } else {
                    log.info("Deploying a leaf-spine pseudowire {}", tunnelId);
                    result = deployPseudowire(currentL2Tunnel, true);
                }
            } catch (DeviceConfigNotFoundException e) {
                log.error("Exception caught when deploying pseudowire", e.toString());
                result = Result.INTERNAL_ERROR;
            }

            switch (result) {
                case INTERNAL_ERROR:
                    log.warn("Could not deploy pseudowire {}, internal error!", tunnelId);
                    break;
                case WRONG_PARAMETERS:
                    log.warn("Could not deploy pseudowire {}, wrong parameters!", tunnelId);
                    break;
                case ADDITION_ERROR:
                    log.warn("Could not deploy pseudowire {}, error in populating rules!", tunnelId);
                    break;
                default:
                    log.info("Pseudowire with {} succesfully deployed!", tunnelId);
                    break;
            }
        }
    }

    /**
     * Helper function to update a pw.
     * <p>
     * Called upon configuration changes that update existing pseudowires and
     * when links fail. Checking of mastership for CP1 is mandatory because it is
     * called in multiple instances for both cases.
     * <p>
     * Meant to call asynchronously for various events, thus this call can not block and need
     * to perform asynchronous operations.
     * <p>
     * For this reason error checking is omitted.
     *
     * @param oldPw the pseudo wire to remove
     * @param newPw the pseudo wire to add
     */
    private void updatePw(L2TunnelDescription oldPw,
                         L2TunnelDescription newPw) {
        ConnectPoint oldCp1 = oldPw.l2TunnelPolicy().cP1();
        long tunnelId = oldPw.l2Tunnel().tunnelId();

        // only determine if the new pseudowire is leaf-spine, because
        // removal process is the same for both leaf-leaf and leaf-spine pws
        boolean newPwSpine;
        try {
            newPwSpine = !srManager.deviceConfiguration().isEdgeDevice(newPw.l2TunnelPolicy().cP1().deviceId()) ||
                    !srManager.deviceConfiguration().isEdgeDevice(newPw.l2TunnelPolicy().cP2().deviceId());
        } catch (DeviceConfigNotFoundException e) {
            // if exception is caught treat the new pw as leaf-leaf
            newPwSpine = false;
        }

        // copy the variable here because we need to use it in lambda thus it needs to be final
        boolean finalNewPwSpine = newPwSpine;

        log.info("Updating pseudowire {}", oldPw.l2Tunnel().tunnelId());

        // The async tasks to orchestrate the next and forwarding update
        CompletableFuture<ObjectiveError> fwdInitNextFuture = new CompletableFuture<>();
        CompletableFuture<ObjectiveError> revInitNextFuture = new CompletableFuture<>();
        CompletableFuture<ObjectiveError> fwdTermNextFuture = new CompletableFuture<>();
        CompletableFuture<ObjectiveError> revTermNextFuture = new CompletableFuture<>();
        CompletableFuture<ObjectiveError> fwdPwFuture = new CompletableFuture<>();
        CompletableFuture<ObjectiveError> revPwFuture = new CompletableFuture<>();

        // first delete all information from our stores, we can not do it asynchronously
        l2PolicyStore.remove(Long.toString(tunnelId));

        // grab the old l2 tunnel from the store, since it carries information which is not exposed
        // to the user configuration and set it to oldPw.
        oldPw.setL2Tunnel(l2TunnelStore.get(Long.toString(tunnelId)).value());
        VlanId transportVlan = l2TunnelStore.get(Long.toString(tunnelId)).value().transportVlan();
        l2TunnelStore.remove(Long.toString(tunnelId));

        // remove the reserved transport vlan, if one is used
        if (!transportVlan.equals(UNTAGGED_TRANSPORT_VLAN)) {
            vlanStore.remove(transportVlan);
        }

        // First we remove both policy.
        log.debug("Start deleting fwd policy for {}", tunnelId);
        VlanId egressVlan = determineEgressVlan(oldPw.l2TunnelPolicy().cP1OuterTag(),
                                                 oldPw.l2TunnelPolicy().cP1InnerTag(),
                                                 oldPw.l2TunnelPolicy().cP2OuterTag(),
                                                 oldPw.l2TunnelPolicy().cP2InnerTag());
        deletePolicy(tunnelId, oldPw.l2TunnelPolicy().cP1(),
                      oldPw.l2TunnelPolicy().cP1InnerTag(),
                      oldPw.l2TunnelPolicy().cP1OuterTag(),
                      egressVlan,
                      fwdInitNextFuture,
                      FWD);

        deletePolicy(tunnelId, oldPw.l2TunnelPolicy().cP2(),
                      oldPw.l2TunnelPolicy().cP2InnerTag(),
                      oldPw.l2TunnelPolicy().cP2OuterTag(),
                      egressVlan, revInitNextFuture,
                      REV);

        // Finally we remove both the tunnels.
        fwdInitNextFuture.thenAcceptAsync(status -> {
            if (status == null) {
                log.debug("Update process : Fwd policy removed. " +
                                  "Now remove fwd {} for {}", INITIATION, tunnelId);
                tearDownPseudoWireInit(tunnelId, oldPw.l2TunnelPolicy().cP1(), fwdTermNextFuture, FWD);
            }
        });
        revInitNextFuture.thenAcceptAsync(status -> {
            if (status == null) {
                log.debug("Update process : Rev policy removed. " +
                                  "Now remove rev {} for {}", INITIATION, tunnelId);
                tearDownPseudoWireInit(tunnelId, oldPw.l2TunnelPolicy().cP2(), revTermNextFuture, REV);
            }
        });
        fwdTermNextFuture.thenAcceptAsync(status -> {
            if (status == null) {
                log.debug("Update process : Fwd {} removed. " +
                                  "Now remove fwd {} for {}", INITIATION, TERMINATION, tunnelId);
                tearDownPseudoWireTerm(oldPw.l2Tunnel(), oldPw.l2TunnelPolicy().cP2(),  fwdPwFuture, FWD);
            }
        });
        revTermNextFuture.thenAcceptAsync(status -> {
            if (status == null) {
                log.debug("Update process : Rev {} removed. " +
                                  "Now remove rev {} for {}", INITIATION, TERMINATION, tunnelId);
                tearDownPseudoWireTerm(oldPw.l2Tunnel(), oldPw.l2TunnelPolicy().cP1(), revPwFuture, REV);
            }
        });

        // get path here, need to use the same for fwd and rev direction
        List<Link> path = getPath(newPw.l2TunnelPolicy().cP1(),
                                   newPw.l2TunnelPolicy().cP2());
        if (path == null) {
            log.error("Update process : " +
                             "No path between the connection points for pseudowire {}", newPw.l2Tunnel().tunnelId());
            return;
        }

        Link fwdNextHop, revNextHop;
        if (!isValidPathSize(path.size())) {
            log.error("Deploying process : Path size for pseudowire should be of one of the following sizes" +
                              " = [1, 2, 3, 4], for pseudowire {}",
                      newPw.l2Tunnel().tunnelId());
            return;
        }

        // spinePw signifies if we have a leaf-spine pw
        // thus only one label should be pushed (that of pw)
        // if size>1 we need to push intermediate labels also.
        if (path.size() > 1) {
            newPwSpine = false;
        }

        fwdNextHop = path.get(0);
        revNextHop = reverseLink(path.get(path.size() - 1));

        // set new path and transport vlan.
        newPw.l2Tunnel().setPath(path);
        newPw.l2Tunnel().setTransportVlan(determineTransportVlan(newPwSpine));

        // At the end we install the updated PW.
        fwdPwFuture.thenAcceptAsync(status -> {
            if (status == null) {

                // Upgrade stores and book keeping information, need to move this here
                // cause this call is asynchronous.
                l2PolicyStore.put(Long.toString(tunnelId), newPw.l2TunnelPolicy());
                l2TunnelStore.put(Long.toString(tunnelId), newPw.l2Tunnel());

                VlanId egressVlanId = determineEgressVlan(newPw.l2TunnelPolicy().cP1OuterTag(),
                                                          newPw.l2TunnelPolicy().cP1InnerTag(),
                                                          newPw.l2TunnelPolicy().cP2OuterTag(),
                                                          newPw.l2TunnelPolicy().cP2InnerTag());

                log.debug("Update process : Deploying new fwd pw for {}", tunnelId);
                Result lamdaResult = deployPseudoWireInit(newPw.l2Tunnel(), newPw.l2TunnelPolicy().cP1(),
                                                           newPw.l2TunnelPolicy().cP2(), FWD,
                                                           fwdNextHop, finalNewPwSpine, egressVlanId);
                if (lamdaResult != SUCCESS) {
                    return;
                }

                lamdaResult = deployPolicy(tunnelId, newPw.l2TunnelPolicy().cP1(),
                                            newPw.l2TunnelPolicy().cP1InnerTag(),
                                           newPw.l2TunnelPolicy().cP1OuterTag(),
                                            egressVlanId, lamdaResult.nextId);
                if (lamdaResult != SUCCESS) {
                    return;
                }
                deployPseudoWireTerm(newPw.l2Tunnel(), newPw.l2TunnelPolicy().cP2(),
                                      egressVlanId, FWD, finalNewPwSpine);

            }
        });
        revPwFuture.thenAcceptAsync(status -> {
            if (status == null) {

                log.debug("Update process : Deploying new rev pw for {}", tunnelId);

                VlanId egressVlanId = determineEgressVlan(newPw.l2TunnelPolicy().cP2OuterTag(),
                                                          newPw.l2TunnelPolicy().cP2InnerTag(),
                                                          newPw.l2TunnelPolicy().cP1OuterTag(),
                                                          newPw.l2TunnelPolicy().cP1InnerTag());

                Result lamdaResult = deployPseudoWireInit(newPw.l2Tunnel(),
                                                           newPw.l2TunnelPolicy().cP2(),
                                                           newPw.l2TunnelPolicy().cP1(),
                                                           REV,
                                                           revNextHop, finalNewPwSpine, egressVlanId);
                if (lamdaResult != SUCCESS) {
                    return;
                }

                lamdaResult = deployPolicy(tunnelId,
                                            newPw.l2TunnelPolicy().cP2(),
                                            newPw.l2TunnelPolicy().cP2InnerTag(),
                                            newPw.l2TunnelPolicy().cP2OuterTag(),
                                            egressVlanId,
                                            lamdaResult.nextId);
                if (lamdaResult != SUCCESS) {
                    return;
                }
                deployPseudoWireTerm(newPw.l2Tunnel(),
                                      newPw.l2TunnelPolicy().cP1(),
                                      egressVlanId,
                                      REV, finalNewPwSpine);
            }
        });
    }

    /**
     * Helper function for removing a single pseudowire.
     * <p>
     * No mastership of CP1 is checked, because it can be called from
     * the CLI for removal of pseudowires.
     *
     * @param l2TunnelId the id of the pseudowire to tear down
     * @return Returns SUCCESS if no error is obeserved or an appropriate
     * error on a failure
     */
    private Result tearDownPseudowire(long l2TunnelId) {

        CompletableFuture<ObjectiveError> fwdInitNextFuture = new CompletableFuture<>();
        CompletableFuture<ObjectiveError> fwdTermNextFuture = new CompletableFuture<>();

        CompletableFuture<ObjectiveError> revInitNextFuture = new CompletableFuture<>();
        CompletableFuture<ObjectiveError> revTermNextFuture = new CompletableFuture<>();

        if (l2TunnelId == 0) {
            log.warn("Removal process : Tunnel id cannot be 0");
            return Result.WRONG_PARAMETERS;
        }

        // check existence of tunnels/policy in the store, if one is missing abort!
        Versioned<L2Tunnel> l2TunnelVersioned = l2TunnelStore.get(Long.toString(l2TunnelId));
        Versioned<L2TunnelPolicy> l2TunnelPolicyVersioned = l2PolicyStore.get(Long.toString(l2TunnelId));
        if ((l2TunnelVersioned == null) || (l2TunnelPolicyVersioned == null)) {
            log.warn("Removal process : Policy and/or tunnel missing for tunnel id {}", l2TunnelId);
            return Result.REMOVAL_ERROR;
        }

        L2TunnelDescription pwToRemove = new DefaultL2TunnelDescription(l2TunnelVersioned.value(),
                                                                               l2TunnelPolicyVersioned.value());

        // remove the tunnels and the policies from the store
        l2PolicyStore.remove(Long.toString(l2TunnelId));
        l2TunnelStore.remove(Long.toString(l2TunnelId));

        // remove the reserved transport vlan
        if (!pwToRemove.l2Tunnel().transportVlan().equals(UNTAGGED_TRANSPORT_VLAN)) {
            vlanStore.remove(pwToRemove.l2Tunnel().transportVlan());
        }

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

        log.info("Removal process : Tearing down reverse direction of pseudowire {}", l2TunnelId);

        egressVlan = determineEgressVlan(pwToRemove.l2TunnelPolicy().cP2OuterTag(),
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

        return Result.SUCCESS;
    }

    @Override
    public void tearDown(Set<L2TunnelDescription> pwToRemove) {

        Result result;

        // We remove all the pw in the configuration file.
        for (L2TunnelDescription currentL2Tunnel : pwToRemove) {
            ConnectPoint cp1 = currentL2Tunnel.l2TunnelPolicy().cP1();
            ConnectPoint cp2 = currentL2Tunnel.l2TunnelPolicy().cP2();
            long tunnelId = currentL2Tunnel.l2TunnelPolicy().tunnelId();

            // no need to differentiate here between leaf-leaf and leaf-spine, because
            // the only change is in the groups, which we do not remove either way
            log.info("Removing pseudowire {}", tunnelId);

            result = tearDownPseudowire(tunnelId);
            switch (result) {
                case WRONG_PARAMETERS:
                    log.warn("Error in supplied parameters for the pseudowire removal with tunnel id {}!",
                            tunnelId);
                    break;
                case REMOVAL_ERROR:
                    log.warn("Error in pseudowire removal with tunnel id {}!", tunnelId);
                    break;
                default:
                    log.warn("Pseudowire with tunnel id {} was removed successfully", tunnelId);
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
     * @param nextId       the next objective id
     * @param egressVlan   Vlan-id to set, depends on ingress vlan
     *                     combinations. For example, if pw is double tagged
     *                     then this is the value of the outer vlan, if single
     *                     tagged then it is the new value of the single tag.
     *                     Should be None for untagged traffic.
     * @return the result of the operation
     */
    private Result deployPolicy(long tunnelId, ConnectPoint ingress, VlanId ingressInner,
                                VlanId ingressOuter, VlanId egressVlan, int nextId) {

        List<Objective> objectives = Lists.newArrayList();
        // We create the forwarding objective for supporting
        // the l2 tunnel.
        ForwardingObjective.Builder fwdBuilder = createInitFwdObjective(tunnelId, ingress.port(), nextId);
        // We create and add objective context.
        ObjectiveContext context = new DefaultObjectiveContext((objective) ->
                                                                log.debug("FwdObj for tunnel {} populated", tunnelId),
                                                               (objective, error) ->
                                                                log.warn("Failed to populate fwdrObj " +
                                                                                 "for tunnel {}", tunnelId, error));
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
                                                                                     "tunnel {}", tunnelId, error));
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
     * @param spinePw if the pseudowire involves a spine switch
     * @return the result of the operation
     */
    private Result deployPseudoWireInit(L2Tunnel l2Tunnel, ConnectPoint ingress,
                                        ConnectPoint egress, Direction direction,
                                        Link nextHop, boolean spinePw, VlanId termVlanId) {

        if (nextHop == null) {
            log.warn("No path between ingress and egress cps for tunnel {}", l2Tunnel.tunnelId());
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
                                                                         spinePw,
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
        result.nextId = nextObjective.id();
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
     * @param spinePw if the pseudowire involves a spine switch
     * @return the result of the operation
     */
    private Result deployPseudoWireTerm(L2Tunnel l2Tunnel, ConnectPoint egress,
                                        VlanId egressVlan, Direction direction, boolean spinePw) {

        // We create the group relative to the termination.
        NextObjective.Builder nextObjectiveBuilder = createNextObjective(TERMINATION, egress, null,
                                                                         l2Tunnel, egress.deviceId(),
                                                                         spinePw,
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
                                                                             " for tunnel termination {}",
                                                                             l2Tunnel.tunnelId(), error));
        srManager.flowObjectiveService.forward(egress.deviceId(), fwdBuilder.add(context));
        log.debug("Creating new FwdObj for termination NextObj with id={} for tunnel {}",
                  nextId, l2Tunnel.tunnelId());

        if (spinePw) {

            // determine the input port at the
            PortNumber inPort;

            if (egress.deviceId().
                    equals(l2Tunnel.pathUsed().get(0).dst().deviceId())) {
                    inPort = l2Tunnel.pathUsed().get(0).dst().port();
            } else {
                    inPort = l2Tunnel.pathUsed().get(0).src().port();
            }

            MacAddress dstMac;
            try {
                dstMac = srManager.deviceConfiguration().getDeviceMac(egress.deviceId());
            } catch (Exception e) {
                log.info("Device not found in configuration, no programming of MAC address");
                dstMac = null;
            }

            log.info("Populating filtering objective for pseudowire transport" +
                             " with vlan = {}, port = {}, mac = {}",
                     l2Tunnel.transportVlan(),
                     inPort,
                     dstMac);
            FilteringObjective.Builder filteringObjectiveBuilder =
                    createNormalPipelineFiltObjective(inPort, l2Tunnel.transportVlan(), dstMac);
            context = new DefaultObjectiveContext(( objective ) ->
                                                          log.debug("Special filtObj for  " + "for {} populated",
                                                                    l2Tunnel.tunnelId()),
                                                  ( objective, error ) ->
                                                          log.warn("Failed to populate " +
                                                                           "special filtObj " +
                                                                           "rule for {}: {}",
                                                                                   l2Tunnel.tunnelId(), error));
            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            filteringObjectiveBuilder.withMeta(treatment.build());
            srManager.flowObjectiveService.filter(egress.deviceId(), filteringObjectiveBuilder.add(context));
            log.debug("Creating new special FiltObj for termination point with tunnel {} for port {}",
                      l2Tunnel.tunnelId(),
                      inPort);
        }

        return SUCCESS;
    }


    /**
     * Creates the filtering objective according to a given port and vlanid.
     *
     * @param inPort   the in port
     * @param vlanId the inner vlan tag
     * @return the filtering objective
     */
    private FilteringObjective.Builder createNormalPipelineFiltObjective(PortNumber inPort,
                                                                         VlanId vlanId,
                                                                         MacAddress dstMac) {

        log.info("Creating filtering objective for pseudowire transport with vlan={}, port={}, mac={}",
                 vlanId,
                 inPort,
                 dstMac);
        FilteringObjective.Builder fwdBuilder = DefaultFilteringObjective
                .builder()
                .withKey(Criteria.matchInPort(inPort))
                .addCondition(Criteria.matchVlanId(vlanId))
                .withPriority(SegmentRoutingService.DEFAULT_PRIORITY)
                .permit()
                .fromApp(srManager.appId());

        if (dstMac != null) {
            fwdBuilder.addCondition(Criteria.matchEthDst(dstMac));
        }

        return fwdBuilder;
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

        log.info("Creating filtering objective for vlans {} / {}", outerTag, innerTag);
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
     * @param spinePw if the pw involves a spine switch
     * @return the next objective to support the pipeline
     */
    private NextObjective.Builder createNextObjective(Pipeline pipeline, ConnectPoint srcCp,
                                                      ConnectPoint dstCp,  L2Tunnel l2Tunnel,
                                                      DeviceId egressId, boolean spinePw, VlanId termVlanId) {
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
                log.warn("Pw label not configured");
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

            // if pw is leaf-to-leaf we need to
            // add the routing label also
            if (!spinePw) {
                // We retrieve the sr label from the config
                // specific for pseudowire traffic
                // using the egress leaf device id.
                MplsLabel srLabel;
                try {
                    srLabel = MplsLabel.mplsLabel(srManager.deviceConfiguration().getPWRoutingLabel(egressId));

                } catch (DeviceConfigNotFoundException e) {
                    log.warn("Sr label for pw traffic not configured");
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
                log.warn("Was not able to find the ingress mac");
                return null;
            }
            treatmentBuilder.setEthSrc(ingressMac);
            MacAddress neighborMac;
            try {
                neighborMac = srManager.deviceConfiguration().getDeviceMac(dstCp.deviceId());
            } catch (DeviceConfigNotFoundException e) {
                log.warn("Was not able to find the neighbor mac");
                return null;
            }
            treatmentBuilder.setEthDst(neighborMac);

            // if not a leaf-spine pw we need to POP the vlan at the output
            // since we carry this traffic untagged.
            if (!spinePw) {
                treatmentBuilder.popVlan();
            }

            // set the appropriate transport vlan
            treatmentBuilder.setVlanId(l2Tunnel.transportVlan());
        } else {
            // We create the next objective which
            // will be a simple l2 group.
            nextObjBuilder = DefaultNextObjective
                    .builder()
                    .withType(NextObjective.Type.SIMPLE)
                    .fromApp(srManager.appId());

            // for termination point we use the outer vlan of the
            // encapsulated packet
            treatmentBuilder.setVlanId(termVlanId);
        }

        treatmentBuilder.setOutput(srcCp.port());
        nextObjBuilder.addTreatment(treatmentBuilder.build());
        return nextObjBuilder;
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
        /* TODO We retrieve a set of paths in case of a link failure, what happens
         * if the TopologyService gets the link notification AFTER us and has not updated the paths?
         *
         * TODO This has the potential to act on old topology.
         * Maybe we should make SRManager be a listener on topology events instead raw link events.
         */
        Set<Path> paths = srManager.topologyService.getPaths(
                srManager.topologyService.currentTopology(),
                srcCp.deviceId(), dstCp.deviceId());

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
            log.warn("Abort delete of policy for tunnel {}: next does not exist in the store", tunnelId);
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
                log.warn("Failed to remove previous fwdObj for policy {}: {}", tunnelId, error);
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

        String key = generateKey(l2TunnelId, direction);
        if (!l2InitiationNextObjStore.containsKey(key)) {
            log.info("Abort delete of {} for {}: next does not exist in the store", INITIATION, key);
            if (future != null) {
                future.complete(null);
            }
            return;
        }
        NextObjective nextObjective = l2InitiationNextObjStore.get(key).value();

        // un-comment in case you want to delete groups used by the pw
        // however, this will break the update of pseudowires cause the L2 interface group can
        // not be deleted (it is referenced by other groups)
        /*
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

        String key = generateKey(l2Tunnel.tunnelId(), direction);
        if (!l2TerminationNextObjStore.containsKey(key)) {
            log.info("Abort delete of {} for {}: next does not exist in the store", TERMINATION, key);
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

        // delete the extra filtering objective for terminating
        // spine-spine pws
        if (!l2Tunnel.transportVlan().equals(UNTAGGED_TRANSPORT_VLAN)) {

            // determine the input port at the
            PortNumber inPort;

            if (egress.deviceId().
                    equals(l2Tunnel.pathUsed().get(0).dst().deviceId())) {
                inPort = l2Tunnel.pathUsed().get(0).dst().port();
            } else {
                inPort = l2Tunnel.pathUsed().get(0).src().port();
            }

            MacAddress dstMac;
            try {
                dstMac = srManager.deviceConfiguration().getDeviceMac(egress.deviceId());
            } catch (Exception e) {
                log.info("Device not found in configuration, no programming of MAC address");
                dstMac = null;
            }

            log.info("Removing filtering objective for pseudowire transport" +
                             " with vlan = {}, port = {}, mac = {}",
                     l2Tunnel.transportVlan(),
                     inPort,
                     dstMac);
            FilteringObjective.Builder filteringObjectiveBuilder =
                    createNormalPipelineFiltObjective(inPort, l2Tunnel.transportVlan(), dstMac);
            context = new DefaultObjectiveContext(( objective ) ->
                                                          log.debug("Special filtObj for  " + "for {} removed",
                                                                    l2Tunnel.tunnelId()), ( objective, error ) ->
                    log.warn("Failed to populate " + "special filtObj " +
                                     "rule for {}: {}", l2Tunnel.tunnelId(), error));
            TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder();
            filteringObjectiveBuilder.withMeta(treatment.build());
            srManager.flowObjectiveService.filter(egress.deviceId(), filteringObjectiveBuilder.remove(context));
            log.debug("Removing special FiltObj for termination point with tunnel {} for port {}",
                      l2Tunnel.tunnelId(),
                      inPort);
        }

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
