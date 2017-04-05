/*
 * Copyright 2016-present Open Networking Laboratory
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
import org.onosproject.net.DeviceId;
import org.onosproject.net.DisjointPath;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.NetworkConfigEvent;
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
import org.onosproject.segmentrouting.config.PwaasConfig;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static org.onosproject.net.flowobjective.ForwardingObjective.Flag.VERSATILE;
import static org.onosproject.segmentrouting.pwaas.L2Mode.TAGGED;
import static org.onosproject.segmentrouting.pwaas.L2TunnelHandler.Pipeline.INITIATION;
import static org.onosproject.segmentrouting.pwaas.L2TunnelHandler.Pipeline.TERMINATION;
import static org.onosproject.segmentrouting.pwaas.L2TunnelHandler.Result.*;
import static org.onosproject.segmentrouting.pwaas.L2TunnelHandler.Direction.FWD;
import static org.onosproject.segmentrouting.pwaas.L2TunnelHandler.Direction.REV;

/**
 * Handles pwaas related events.
 */
public class L2TunnelHandler {

    private static final Logger log = LoggerFactory.getLogger(L2TunnelHandler.class);
    /**
     * Error message for invalid paths.
     */
    private static final String WRONG_TOPOLOGY = "Path in leaf-spine topology" +
            " should always be two hops: ";

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
     * TODO a proper store is necessary to handle the policies, collisions and recovery.
     * We should have a proper store for the policies and the tunnels. For several reasons:
     * 1) We should avoid the overlapping of different policies;
     * 2) We should avoid the overlapping of different tunnels;
     * 3) We should have a proper mechanism for the protection;
     * The most important one is 3). At least for 3.0 EA0 was not possible
     * to remove the bucket, so we need a mapping between policies and tunnel
     * in order to proper update the fwd objective for the recovery of a fault.
     */
    private final KryoNamespace.Builder l2TunnelKryo;

    /**
     * Create a l2 tunnel handler for the deploy and
     * for the tear down of pseudo wires.
     *
     * @param segmentRoutingManager the segment routing manager
     */
    public L2TunnelHandler(SegmentRoutingManager segmentRoutingManager) {
        srManager = segmentRoutingManager;
        l2TunnelKryo = new KryoNamespace.Builder()
                .register(KryoNamespaces.API);

        l2InitiationNextObjStore = srManager.storageService
                .<String, NextObjective>consistentMapBuilder()
                .withName("onos-l2initiation-nextobj-store")
                .withSerializer(Serializer.using(l2TunnelKryo.build()))
                .build();

        l2TerminationNextObjStore = srManager.storageService
                .<String, NextObjective>consistentMapBuilder()
                .withName("onos-l2termination-nextobj-store")
                .withSerializer(Serializer.using(l2TunnelKryo.build()))
                .build();
    }

    /**
     * Processes Pwaas Config added event.
     *
     * @param event network config add event
     */
    public void processPwaasConfigAdded(NetworkConfigEvent event) {
        log.info("Processing Pwaas CONFIG_ADDED");
        PwaasConfig config = (PwaasConfig) event.config().get();
        Set<DefaultL2TunnelDescription> pwToAdd = config.getPwIds()
                .stream()
                .map(config::getPwDescription)
                .collect(Collectors.toSet());
        // We deploy all the pseudo wire deployed
        deploy(pwToAdd);
    }

    /**
     * To deploy a number of pseudo wires.
     *
     * @param pwToAdd the set of pseudo wires to add
     */
    private void deploy(Set<DefaultL2TunnelDescription> pwToAdd) {
        Result result;
        long l2TunnelId;
        for (DefaultL2TunnelDescription currentL2Tunnel : pwToAdd) {
            l2TunnelId = currentL2Tunnel.l2Tunnel().tunnelId();
            // The tunnel id cannot be 0.
            if (l2TunnelId == 0) {
                log.warn("Tunnel id id must be > 0");
                continue;
            }
            // We do a sanity check of the pseudo wire.
            result = verifyPseudoWire(currentL2Tunnel);
            if (result != SUCCESS) {
                continue;
            }
            // We establish the tunnel.
            result = deployPseudoWireInit(
                    currentL2Tunnel.l2Tunnel(),
                    currentL2Tunnel.l2TunnelPolicy().cP1(),
                    currentL2Tunnel.l2TunnelPolicy().cP2(),
                    FWD
            );
            if (result != SUCCESS) {
                continue;
            }
            // We create the policy.
            result = deployPolicy(
                    l2TunnelId,
                    currentL2Tunnel.l2TunnelPolicy().cP1(),
                    currentL2Tunnel.l2TunnelPolicy().cP1InnerTag(),
                    currentL2Tunnel.l2TunnelPolicy().cP1OuterTag(),
                    result.nextId
            );
            if (result != SUCCESS) {
                continue;
            }
            // We terminate the tunnel
            result = deployPseudoWireTerm(
                    currentL2Tunnel.l2Tunnel(),
                    currentL2Tunnel.l2TunnelPolicy().cP2(),
                    currentL2Tunnel.l2TunnelPolicy().cP2OuterTag(),
                    FWD
            );
            if (result != SUCCESS) {
                continue;
            }
            // We establish the reverse tunnel.
            result = deployPseudoWireInit(
                    currentL2Tunnel.l2Tunnel(),
                    currentL2Tunnel.l2TunnelPolicy().cP2(),
                    currentL2Tunnel.l2TunnelPolicy().cP1(),
                    REV
            );
            if (result != SUCCESS) {
                continue;
            }
            result = deployPolicy(
                    l2TunnelId,
                    currentL2Tunnel.l2TunnelPolicy().cP2(),
                    currentL2Tunnel.l2TunnelPolicy().cP2InnerTag(),
                    currentL2Tunnel.l2TunnelPolicy().cP2OuterTag(),
                    result.nextId
            );
            if (result != SUCCESS) {
                continue;
            }
            deployPseudoWireTerm(
                    currentL2Tunnel.l2Tunnel(),
                    currentL2Tunnel.l2TunnelPolicy().cP1(),
                    currentL2Tunnel.l2TunnelPolicy().cP1OuterTag(),
                    REV
            );
        }
    }

    /**
     * Processes PWaaS Config updated event.
     *
     * @param event network config updated event
     */
    public void processPwaasConfigUpdated(NetworkConfigEvent event) {
        log.info("Processing Pwaas CONFIG_UPDATED");
        // We retrieve the old pseudo wires.
        PwaasConfig prevConfig = (PwaasConfig) event.prevConfig().get();
        Set<Long> prevPws = prevConfig.getPwIds();
        // We retrieve the new pseudo wires.
        PwaasConfig config = (PwaasConfig) event.config().get();
        Set<Long> newPws = config.getPwIds();
        // We compute the pseudo wires to update.
        Set<Long> updPws = newPws.stream()
                .filter(tunnelId -> prevPws.contains(tunnelId) &&
                !config.getPwDescription(tunnelId).equals(prevConfig.getPwDescription(tunnelId)))
                .collect(Collectors.toSet());
        // The pseudo wires to remove.
        Set<DefaultL2TunnelDescription> pwToRemove = prevPws.stream()
                .filter(tunnelId -> !newPws.contains(tunnelId))
                .map(prevConfig::getPwDescription)
                .collect(Collectors.toSet());
        tearDown(pwToRemove);
        // The pseudo wires to add.
        Set<DefaultL2TunnelDescription> pwToAdd = newPws.stream()
                .filter(tunnelId -> !prevPws.contains(tunnelId))
                .map(config::getPwDescription)
                .collect(Collectors.toSet());
        deploy(pwToAdd);
        // The pseudo wires to update.
        updPws.forEach(tunnelId -> updatePw(
                prevConfig.getPwDescription(tunnelId),
                config.getPwDescription(tunnelId))
        );
    }

    /**
     * Helper function to update a pw.
     *
     * @param oldPw the pseudo wire to remove
     * @param newPw the pseudo wirte to add
     */
    private void updatePw(DefaultL2TunnelDescription oldPw,
                          DefaultL2TunnelDescription newPw) {
        long tunnelId = oldPw.l2Tunnel().tunnelId();
        // The async tasks to orchestrate the next and
        // forwarding update.
        CompletableFuture<ObjectiveError> fwdInitNextFuture = new CompletableFuture<>();
        CompletableFuture<ObjectiveError> revInitNextFuture = new CompletableFuture<>();
        CompletableFuture<ObjectiveError> fwdTermNextFuture = new CompletableFuture<>();
        CompletableFuture<ObjectiveError> revTermNextFuture = new CompletableFuture<>();
        CompletableFuture<ObjectiveError> fwdPwFuture = new CompletableFuture<>();
        CompletableFuture<ObjectiveError> revPwFuture = new CompletableFuture<>();


        Result result = verifyPseudoWire(newPw);
        if (result != SUCCESS) {
            return;
        }
        // First we remove both policy.
        log.debug("Start deleting fwd policy for {}", tunnelId);
        deletePolicy(
                tunnelId,
                oldPw.l2TunnelPolicy().cP1(),
                oldPw.l2TunnelPolicy().cP1InnerTag(),
                oldPw.l2TunnelPolicy().cP1OuterTag(),
                fwdInitNextFuture,
                FWD
        );
        log.debug("Start deleting rev policy for {}", tunnelId);
        deletePolicy(
                tunnelId,
                oldPw.l2TunnelPolicy().cP2(),
                oldPw.l2TunnelPolicy().cP2InnerTag(),
                oldPw.l2TunnelPolicy().cP2OuterTag(),
                revInitNextFuture,
                REV
        );
        // Finally we remove both the tunnels.
        fwdInitNextFuture.thenAcceptAsync(status -> {
            if (status == null) {
                log.debug("Fwd policy removed. Now remove fwd {} for {}", INITIATION, tunnelId);
                tearDownPseudoWireInit(
                        tunnelId,
                        oldPw.l2TunnelPolicy().cP1(),
                        fwdTermNextFuture,
                        FWD
                );
            }
        });
        revInitNextFuture.thenAcceptAsync(status -> {
           if (status == null) {
               log.debug("Rev policy removed. Now remove rev {} for {}", INITIATION, tunnelId);
               tearDownPseudoWireInit(
                       tunnelId,
                       oldPw.l2TunnelPolicy().cP2(),
                       revTermNextFuture,
                       REV
               );

           }
        });
        fwdTermNextFuture.thenAcceptAsync(status -> {
            if (status == null) {
                log.debug("Fwd {} removed. Now remove fwd {} for {}", INITIATION, TERMINATION, tunnelId);
                tearDownPseudoWireTerm(
                        oldPw.l2Tunnel(),
                        oldPw.l2TunnelPolicy().cP2(),
                        fwdPwFuture,
                        FWD
                );
            }
        });
        revTermNextFuture.thenAcceptAsync(status -> {
            if (status == null) {
                log.debug("Rev {} removed. Now remove rev {} for {}", INITIATION, TERMINATION, tunnelId);
                tearDownPseudoWireTerm(
                        oldPw.l2Tunnel(),
                        oldPw.l2TunnelPolicy().cP1(),
                        revPwFuture,
                        REV
                );
            }
        });
        // At the end we install the new pw.
        fwdPwFuture.thenAcceptAsync(status -> {
            if (status == null) {
                log.debug("Deploying new fwd pw for {}", tunnelId);
                Result lamdaResult = deployPseudoWireInit(
                        newPw.l2Tunnel(),
                        newPw.l2TunnelPolicy().cP1(),
                        newPw.l2TunnelPolicy().cP2(),
                        FWD
                );
                if (lamdaResult != SUCCESS) {
                    return;
                }
                lamdaResult = deployPolicy(
                        tunnelId,
                        newPw.l2TunnelPolicy().cP1(),
                        newPw.l2TunnelPolicy().cP1InnerTag(),
                        newPw.l2TunnelPolicy().cP1OuterTag(),
                        lamdaResult.nextId
                );
                if (lamdaResult != SUCCESS) {
                    return;
                }
                deployPseudoWireTerm(
                        newPw.l2Tunnel(),
                        newPw.l2TunnelPolicy().cP2(),
                        newPw.l2TunnelPolicy().cP2OuterTag(),
                        FWD
                );

            }
        });
        revPwFuture.thenAcceptAsync(status -> {
            if (status == null) {
                log.debug("Deploying new rev pw for {}", tunnelId);
                Result lamdaResult = deployPseudoWireInit(
                        newPw.l2Tunnel(),
                        newPw.l2TunnelPolicy().cP2(),
                        newPw.l2TunnelPolicy().cP1(),
                        REV
                );
                if (lamdaResult != SUCCESS) {
                    return;
                }
                lamdaResult = deployPolicy(
                        tunnelId,
                        newPw.l2TunnelPolicy().cP2(),
                        newPw.l2TunnelPolicy().cP2InnerTag(),
                        newPw.l2TunnelPolicy().cP2OuterTag(),
                        lamdaResult.nextId
                );
                if (lamdaResult != SUCCESS) {
                    return;
                }
                deployPseudoWireTerm(
                        newPw.l2Tunnel(),
                        newPw.l2TunnelPolicy().cP1(),
                        newPw.l2TunnelPolicy().cP1OuterTag(),
                        REV
                );
            }
        });
    }

    /**
     * Processes Pwaas Config removed event.
     *
     * @param event network config removed event
     */
    public void processPwaasConfigRemoved(NetworkConfigEvent event) {
        log.info("Processing Pwaas CONFIG_REMOVED");
        PwaasConfig config = (PwaasConfig) event.prevConfig().get();
        Set<DefaultL2TunnelDescription> pwToRemove = config.getPwIds()
                .stream()
                .map(config::getPwDescription)
                .collect(Collectors.toSet());
        // We teardown all the pseudo wire deployed
        tearDown(pwToRemove);
    }

    /**
     * Helper function to handle the pw removal.
     *
     * @param pwToRemove the pseudo wires to remove
     */
    private void tearDown(Set<DefaultL2TunnelDescription> pwToRemove) {
        Result result;
        long l2TunnelId;
        // We remove all the pw in the configuration file.
        for (DefaultL2TunnelDescription currentL2Tunnel : pwToRemove) {
            l2TunnelId = currentL2Tunnel.l2Tunnel().tunnelId();
            if (l2TunnelId == 0) {
                log.warn("Tunnel id cannot be 0");
                continue;
            }
            result = verifyPseudoWire(currentL2Tunnel);
            if (result != SUCCESS) {
                continue;
            }
            // First all we have to delete the policy.
            deletePolicy(
                    l2TunnelId,
                    currentL2Tunnel.l2TunnelPolicy().cP1(),
                    currentL2Tunnel.l2TunnelPolicy().cP1InnerTag(),
                    currentL2Tunnel.l2TunnelPolicy().cP1OuterTag(),
                    null,
                    FWD
            );
            // Finally we will tear down the pseudo wire.
            tearDownPseudoWireInit(
                    l2TunnelId,
                    currentL2Tunnel.l2TunnelPolicy().cP1(),
                    null,
                    FWD
            );
            tearDownPseudoWireTerm(
                    currentL2Tunnel.l2Tunnel(),
                    currentL2Tunnel.l2TunnelPolicy().cP2(),
                    null,
                    FWD
            );
            // We do the same operations on the reverse side.
            deletePolicy(
                    l2TunnelId,
                    currentL2Tunnel.l2TunnelPolicy().cP2(),
                    currentL2Tunnel.l2TunnelPolicy().cP2InnerTag(),
                    currentL2Tunnel.l2TunnelPolicy().cP2OuterTag(),
                    null,
                    REV
            );
            tearDownPseudoWireInit(
                    l2TunnelId,
                    currentL2Tunnel.l2TunnelPolicy().cP2(),
                    null,
                    REV
            );
            tearDownPseudoWireTerm(
                    currentL2Tunnel.l2Tunnel(),
                    currentL2Tunnel.l2TunnelPolicy().cP1(),
                    null,
                    REV
            );
        }

    }

    /**
     * Helper method to verify the integrity of the pseudo wire.
     *
     * @param l2TunnelDescription the pseudo wire description
     * @return the result of the check
     */
    private Result verifyPseudoWire(DefaultL2TunnelDescription l2TunnelDescription) {
        Result result;
        DefaultL2Tunnel l2Tunnel = l2TunnelDescription.l2Tunnel();
        DefaultL2TunnelPolicy l2TunnelPolicy = l2TunnelDescription.l2TunnelPolicy();
        result = verifyTunnel(l2Tunnel);
        if (result != SUCCESS) {
            log.warn("Tunnel {}: did not pass the validation", l2Tunnel.tunnelId());
            return result;
        }
        result = verifyPolicy(
                l2TunnelPolicy.isAllVlan(),
                l2TunnelPolicy.cP1InnerTag(),
                l2TunnelPolicy.cP1OuterTag(),
                l2TunnelPolicy.cP2InnerTag(),
                l2TunnelPolicy.cP2OuterTag()
        );
        if (result != SUCCESS) {
            log.warn("Policy for tunnel {}: did not pass the validation", l2Tunnel.tunnelId());
            return result;
        }

        return SUCCESS;
    }

    /**
     * Handles the policy establishment which consists in
     * create the filtering and forwarding objectives related
     * to the initiation and termination.
     *
     * @param tunnelId the tunnel id
     * @param ingress the ingress point
     * @param ingressInner the ingress inner tag
     * @param ingressOuter the ingress outer tag
     * @param nextId the next objective id
     * @return the result of the operation
     */
    private Result deployPolicy(long tunnelId,
                                ConnectPoint ingress,
                                VlanId ingressInner,
                                VlanId ingressOuter,
                                int nextId) {
        if (!srManager.mastershipService.isLocalMaster(ingress.deviceId())) {
            log.info("Abort creation of policy for tunnel {}: I am not the master", tunnelId);
            return SUCCESS;
        }
        List<Objective> objectives = Lists.newArrayList();
        // We create the forwarding objective for supporting
        // the l2 tunnel.
        ForwardingObjective.Builder fwdBuilder = createInitFwdObjective(
                tunnelId,
                ingress.port(),
                nextId
        );
        // We create and add objective context.
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective)
                        -> log.debug("FwdObj for tunnel {} populated", tunnelId),
                (objective, error)
                        -> log.warn("Failed to populate fwdrObj for tunnel {}", tunnelId, error));
        objectives.add(fwdBuilder.add(context));
        // We create the filtering objective to define the
        // permit traffic in the switch
        FilteringObjective.Builder filtBuilder = createFiltObjective(
                ingress.port(),
                ingressInner,
                ingressOuter
        );
        // We add the metadata.
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .setTunnelId(tunnelId);
        filtBuilder.withMeta(treatment.build());
        // We create and add objective context.
        context = new DefaultObjectiveContext(
                (objective)
                        -> log.debug("FilterObj for tunnel {} populated", tunnelId),
                (objective, error)
                        -> log.warn("Failed to populate filterObj for tunnel {}", tunnelId, error));
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
     * Helper method to verify if the policy is whether or not
     * supported.
     *
     * @param isAllVlan all vlan mode
     * @param ingressInner the ingress inner tag
     * @param ingressOuter the ingress outer tag
     * @param egressInner the egress inner tag
     * @param egressOuter the egress outer tag
     * @return the result of verification
     */
    private Result verifyPolicy(boolean isAllVlan,
                                VlanId ingressInner,
                                VlanId ingressOuter,
                                VlanId egressInner,
                                VlanId egressOuter) {
        // AllVlan mode is not supported yet.
        if (isAllVlan) {
            log.warn("AllVlan not supported yet");
            return UNSUPPORTED;
        }
        // The vlan tags for cP1 and cP2 have to be different from
        // vlan none.
        if (ingressInner.equals(VlanId.NONE) ||
                ingressOuter.equals(VlanId.NONE) ||
                egressInner.equals(VlanId.NONE) ||
                egressOuter.equals(VlanId.NONE)) {
            log.warn("The vlan tags for the connect point have to be" +
                             "different from vlan none");
            return WRONG_PARAMETERS;
        }
        return SUCCESS;
    }

    /**
     * Handles the tunnel establishment which consists in
     * create the next objectives related to the initiation.
     *
     * @param l2Tunnel the tunnel to deploy
     * @param ingress the ingress connect point
     * @param egress the egress connect point
     * @param direction the direction of the pw
     * @return the result of the operation
     */
    private Result deployPseudoWireInit(DefaultL2Tunnel l2Tunnel,
                                        ConnectPoint ingress,
                                        ConnectPoint egress,
                                        Direction direction) {
        if (!srManager.mastershipService.isLocalMaster(ingress.deviceId())) {
            log.info("Abort initiation of tunnel {}: I am not the master", l2Tunnel.tunnelId());
            return SUCCESS;
        }
        // We need at least a path between ingress and egress.
        Link nextHop = getNextHop(ingress, egress);
        if (nextHop == null) {
            log.warn("No path between ingress and egress");
            return WRONG_PARAMETERS;
        }
        // We create the next objective without the metadata
        // context and id. We check if it already exists in the
        // store. If not we store as it is in the store.
        NextObjective.Builder nextObjectiveBuilder = createNextObjective(
                INITIATION,
                nextHop.src(),
                nextHop.dst(),
                l2Tunnel,
                egress.deviceId()
        );
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
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective)
                        -> log.debug("Initiation l2 tunnel rule for {} populated",
                                     l2Tunnel.tunnelId()),
                (objective, error)
                        -> log.warn("Failed to populate Initiation l2 tunnel rule for {}: {}",
                                    l2Tunnel.tunnelId(), error));
        NextObjective nextObjective = nextObjectiveBuilder.add(context);
        srManager.flowObjectiveService.next(ingress.deviceId(), nextObjective);
        log.debug("Initiation next objective for {} not found. Creating new NextObj with id={}",
                  l2Tunnel.tunnelId(),
                  nextObjective.id()
        );
        Result result = SUCCESS;
        result.nextId = nextObjective.id();
        return result;
    }

    /**
     * Handles the tunnel termination, which consists in the creation
     * of a forwarding objective and a next objective.
     *
     * @param l2Tunnel the tunnel to terminate
     * @param egress the egress point
     * @param egressVlan the expected vlan at egress
     * @param direction the direction
     * @return the result of the operation
     */
    private Result deployPseudoWireTerm(DefaultL2Tunnel l2Tunnel,
                                        ConnectPoint egress,
                                        VlanId egressVlan,
                                        Direction direction) {
        // We create the group relative to the termination.
        // It's fine to abort the termination if we are
        // not the master.
        if (!srManager.mastershipService.isLocalMaster(egress.deviceId())) {
            log.info("Abort termination of tunnel {}: I am not the master", l2Tunnel.tunnelId());
            return SUCCESS;
        }
        NextObjective.Builder nextObjectiveBuilder = createNextObjective(
                TERMINATION,
                egress,
                null,
                null,
                egress.deviceId()
        );
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
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective)
                        -> log.debug("Termination l2 tunnel rule for {} populated",
                                     l2Tunnel.tunnelId()),
                (objective, error)
                        -> log.warn("Failed to populate termination l2 tunnel rule for {}: {}",
                                    l2Tunnel.tunnelId(), error));
        NextObjective nextObjective = nextObjectiveBuilder.add(context);
        srManager.flowObjectiveService.next(egress.deviceId(), nextObjective);
        log.debug("Termination next objective for {} not found. Creating new NextObj with id={}",
                  l2Tunnel.tunnelId(),
                  nextObjective.id()
        );
        // We create the flow relative to the termination.
        ForwardingObjective.Builder fwdBuilder = createTermFwdObjective(
                l2Tunnel.pwLabel(),
                l2Tunnel.tunnelId(),
                egress.port(),
                nextObjective.id()
        );
        context = new DefaultObjectiveContext(
                (objective)
                        -> log.debug("FwdObj for tunnel termination {} populated",
                                     l2Tunnel.tunnelId()),
                (objective, error)
                        -> log.warn("Failed to populate fwdrObj for tunnel termination {}",
                                    l2Tunnel.tunnelId(), error));
        srManager.flowObjectiveService.forward(egress.deviceId(), fwdBuilder.add(context));
        log.debug("Creating new FwdObj for termination NextObj with id={} for tunnel {}", nextId, l2Tunnel.tunnelId());
        return SUCCESS;

    }

    /**
     * Helper method to verify if the tunnel is whether or not
     * supported.
     *
     * @param l2Tunnel the tunnel to verify
     * @return the result of the verification
     */
    private Result verifyTunnel(DefaultL2Tunnel l2Tunnel) {
        // Service delimiting tag not supported yet.
        if (!l2Tunnel.sdTag().equals(VlanId.NONE)) {
            log.warn("Service delimiting tag not supported yet");
            return UNSUPPORTED;
        }
        // Tag mode not supported yet.
        if (l2Tunnel.pwMode() == TAGGED) {
            log.warn("Tagged mode not supported yet");
            return UNSUPPORTED;
        }
        // Raw mode without service delimiting tag
        // is the only mode supported for now.
        return SUCCESS;
    }

    /**
     * Creates the filtering objective according to a given policy.
     *
     * @param inPort the in port
     * @param innerTag the inner vlan tag
     * @param outerTag the outer vlan tag
     * @return the filtering objective
     */
    private FilteringObjective.Builder createFiltObjective(PortNumber inPort,
                                                           VlanId innerTag,
                                                           VlanId outerTag) {
        return DefaultFilteringObjective.builder()
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
     * @param pwLabel the pseudo wire label
     * @param tunnelId the tunnel id
     * @param egressPort the egress port
     * @param nextId the next step
     * @return the forwarding objective to support the termination
     */
    private ForwardingObjective.Builder createTermFwdObjective(MplsLabel pwLabel,
                                                               long tunnelId,
                                                               PortNumber egressPort,
                                                               int nextId) {
        TrafficSelector.Builder trafficSelector = DefaultTrafficSelector
                .builder();
        TrafficTreatment.Builder trafficTreatment = DefaultTrafficTreatment
                .builder();
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

        return DefaultForwardingObjective.builder()
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
     * @param inPort the input port
     * @param nextId the next step
     * @return the forwarding objective to support the initiation.
     */
    private ForwardingObjective.Builder createInitFwdObjective(long tunnelId,
                                                               PortNumber inPort,
                                                               int nextId) {
        TrafficSelector.Builder trafficSelector = DefaultTrafficSelector
                .builder();
        // The flow has to match on the mpls logical
        // port and the tunnel id.
        trafficSelector.matchTunnelId(tunnelId);
        trafficSelector.matchInPort(inPort);

        return DefaultForwardingObjective.builder()
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
     * @param srcCp the source port
     * @param dstCp the destination port
     * @param l2Tunnel the tunnel to support
     * @param egressId the egress device id
     * @return the next objective to support the pipeline
     */
    private NextObjective.Builder createNextObjective(Pipeline pipeline,
                                                      ConnectPoint srcCp,
                                                      ConnectPoint dstCp,
                                                      DefaultL2Tunnel l2Tunnel,
                                                      DeviceId egressId) {
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
            // We retrieve the sr label from the config
            // using the egress leaf device id.
            MplsLabel srLabel;
            try {
                 srLabel = MplsLabel.mplsLabel(
                         srManager.deviceConfiguration().getIPv4SegmentId(egressId)
                 );
            } catch (DeviceConfigNotFoundException e) {
                log.warn("Sr label not configured");
                return null;
            }
            treatmentBuilder.pushMpls();
            treatmentBuilder.setMpls(srLabel);
            treatmentBuilder.setMplsBos(false);
            treatmentBuilder.copyTtlOut();
            // We have to rewrite the src and dst mac address.
            MacAddress ingressMac;
            try {
                ingressMac = srManager
                        .deviceConfiguration()
                        .getDeviceMac(srcCp.deviceId());
            } catch (DeviceConfigNotFoundException e) {
                log.warn("Was not able to find the ingress mac");
                return null;
            }
            treatmentBuilder.setEthSrc(ingressMac);
            MacAddress neighborMac;
            try {
                neighborMac = srManager
                        .deviceConfiguration()
                        .getDeviceMac(dstCp.deviceId());
            } catch (DeviceConfigNotFoundException e) {
                log.warn("Was not able to find the neighbor mac");
                return null;
            }
            treatmentBuilder.setEthDst(neighborMac);
        } else {
            // We create the next objective which
            // will be a simple l2 group.
            nextObjBuilder = DefaultNextObjective
                    .builder()
                    .withType(NextObjective.Type.SIMPLE)
                    .fromApp(srManager.appId());
        }
        treatmentBuilder.setOutput(srcCp.port());
        nextObjBuilder.addTreatment(treatmentBuilder.build());
        return nextObjBuilder;
    }

    /**
     * Returns the next hop.
     *
     * @param srcCp the ingress connect point
     * @param dstCp the egress connect point
     * @return the next hop
     */
    private Link getNextHop(ConnectPoint srcCp, ConnectPoint dstCp) {
        // We retrieve a set of disjoint paths.
        Set<DisjointPath> paths = srManager.pathService.getDisjointPaths(
                srcCp.elementId(),
                dstCp.elementId()
        );
        // We randmly pick a path.
        if (paths.isEmpty()) {
            return null;
        }
        int size = paths.size();
        int index = RandomUtils.nextInt(0, size);
        // We verify if the path is ok and there is not
        // a misconfiguration.
        List<Link> links = Iterables.get(paths, index).links();
        checkState(links.size() == 2, WRONG_TOPOLOGY, links);
        return links.get(0);
    }

    /**
     * Deletes a given policy using the parameter supplied.
     *
     * @param tunnelId the tunnel id
     * @param ingress the ingress point
     * @param ingressInner the ingress inner vlan id
     * @param ingressOuter the ingress outer vlan id
     * @param future to perform the async operation
     * @param direction the direction: forward or reverse
     */
    private void deletePolicy(long tunnelId,
                              ConnectPoint ingress,
                              VlanId ingressInner,
                              VlanId ingressOuter,
                              CompletableFuture<ObjectiveError> future,
                              Direction direction) {
        if (!srManager.mastershipService.isLocalMaster(ingress.deviceId())) {
            log.info("Abort delete of policy for tunnel {}: I am not the master", tunnelId);
            if (future != null) {
                future.complete(null);
            }
            return;
        }
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
        ForwardingObjective.Builder fwdBuilder = createInitFwdObjective(
                tunnelId,
                ingress.port(),
                nextId
        );
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
        FilteringObjective.Builder filtBuilder = createFiltObjective(
                ingress.port(),
                ingressInner,
                ingressOuter
        );
        TrafficTreatment.Builder treatment = DefaultTrafficTreatment.builder()
                .setTunnelId(tunnelId);
        filtBuilder.withMeta(treatment.build());
        context = new DefaultObjectiveContext(
                (objective)
                        -> log.debug("FilterObj for policy {} revoked", tunnelId),
                (objective, error)
                        -> log.warn("Failed to revoke filterObj for policy {}", tunnelId, error));
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
     * @param ingress the ingress connect point
     * @param future to perform an async operation
     * @param direction the direction: reverse of forward
     */
    private void tearDownPseudoWireInit(long l2TunnelId,
                                        ConnectPoint ingress,
                                        CompletableFuture<ObjectiveError> future,
                                        Direction direction) {
        String key = generateKey(l2TunnelId, direction);
        if (!srManager.mastershipService.isLocalMaster(ingress.deviceId())) {
            log.info("Abort delete of {} for {}: I am not the master", INITIATION, key);
            if (future != null) {
                future.complete(null);
            }
            return;
        }
        if (!l2InitiationNextObjStore.containsKey(key)) {
            log.info("Abort delete of {} for {}: next does not exist in the store", INITIATION, key);
            if (future != null) {
                future.complete(null);
            }
            return;
        }
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
        srManager.flowObjectiveService
                .next(ingress.deviceId(), (NextObjective) nextObjective.copy().remove(context));
        l2InitiationNextObjStore.remove(key);
    }

    /**
     * Deletes the pseudo wire termination.
     *
     * @param l2Tunnel the tunnel
     * @param egress the egress connect point
     * @param future the async task
     * @param direction the direction of the tunnel
     */
    private void tearDownPseudoWireTerm(DefaultL2Tunnel l2Tunnel,
                                        ConnectPoint egress,
                                        CompletableFuture<ObjectiveError> future,
                                        Direction direction) {
        /*
         * We verify the mastership for the termination.
         */
        String key = generateKey(l2Tunnel.tunnelId(), direction);
        if (!srManager.mastershipService.isLocalMaster(egress.deviceId())) {
            log.info("Abort delete of {} for {}: I am not the master", TERMINATION, key);
            if (future != null) {
                future.complete(null);
            }
            return;
        }
        if (!l2TerminationNextObjStore.containsKey(key)) {
            log.info("Abort delete of {} for {}: next does not exist in the store", TERMINATION, key);
            if (future != null) {
                future.complete(null);
            }
            return;
        }
        NextObjective nextObjective = l2TerminationNextObjStore.get(key).value();
        ForwardingObjective.Builder fwdBuilder = createTermFwdObjective(
                l2Tunnel.pwLabel(),
                l2Tunnel.tunnelId(),
                egress.port(),
                nextObjective.id()
        );
        ObjectiveContext context = new DefaultObjectiveContext(
                (objective)
                        -> log.debug("FwdObj for {} {} removed", TERMINATION, l2Tunnel.tunnelId()),
                (objective, error)
                        -> log.warn("Failed to remove fwdObj for {} {}", TERMINATION, l2Tunnel.tunnelId(),
                                    error));
        srManager.flowObjectiveService.forward(egress.deviceId(), fwdBuilder.remove(context));

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
        srManager.flowObjectiveService
                .next(egress.deviceId(), (NextObjective) nextObjective.copy().remove(context));
        l2TerminationNextObjStore.remove(key);
    }

    /**
     * Utilities to generate pw key.
     *
     * @param tunnelId the tunnel id
     * @param direction the direction of the pw
     * @return the key of the store
     */
    private String generateKey(long tunnelId, Direction direction) {
        return String.format("%s-%s", tunnelId, direction);
    }

    /**
     * Pwaas pipelines.
     */
    protected enum Pipeline {
        /**
         * The initiation pipeline.
         */
        INITIATION,
        /**
         * The termination pipeline.
         */
        TERMINATION
    }

    /**
     * Enum helper to carry the outcomes of an operation.
     */
    public enum Result {
        /**
         * Happy ending scenario it has been created.
         */
        SUCCESS(0, "It has been Created"),
        /**
         * We have problems with the supplied parameters.
         */
        WRONG_PARAMETERS(1, "Wrong parameters"),
        /**
         * It already exists.
         */
        ID_EXISTS(2, "The id already exists"),
        /**
         * We have an internal error during the deployment
         * phase.
         */
        INTERNAL_ERROR(3, "Internal error"),
        /**
         * The operation is not supported.
         */
        UNSUPPORTED(4, "Unsupported");

        private final int code;
        private final String description;
        private int nextId;

        Result(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return code + ": " + description;
        }
    }

    /**
     * Enum helper for handling the direction of the pw.
     */
    public enum Direction {
        /**
         * The forward direction of the pseudo wire.
         */
        FWD,
        /**
         * The reverse direction of the pseudo wire.
         */
        REV
    }

}
