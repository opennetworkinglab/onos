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
import static org.onosproject.segmentrouting.pwaas.L2TunnelHandler.Result.SUCCESS;
import static org.onosproject.segmentrouting.pwaas.L2Mode.TAGGED;
import static org.onosproject.segmentrouting.pwaas.L2TunnelHandler.Pipeline.INITIATION;
import static org.onosproject.segmentrouting.pwaas.L2TunnelHandler.Result.*;

/**
 * Handles Pwaas related events.
 */
public class L2TunnelHandler {

    private static final Logger log = LoggerFactory.getLogger(L2TunnelHandler.class);

    private static final String FWD = "f";
    private static final String REV = "r";

    private static final String NOT_MASTER = "Not master controller";
    private static final String WRONG_TOPOLOGY = "Path in leaf-spine topology" +
            " should always be two hops: ";

    private final SegmentRoutingManager srManager;

    private final ConsistentMap<String, NextObjective> l2InitiationNextObjStore;

    /**
     * TODO a proper store is necessary to handle the policies and collisions.
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
        // We deploy all the pseudowires
        deploy(pwToAdd);
    }

    private void deploy(Set<DefaultL2TunnelDescription> pwToAdd) {
        Result result;
        long l2TunnelId;
        for (DefaultL2TunnelDescription currentL2Tunnel : pwToAdd) {
            l2TunnelId = currentL2Tunnel.l2Tunnel().tunnelId();
            // The tunnel id cannot be 0.
            if (l2TunnelId == 0) {
                log.warn("Tunnel id cannot be 0");
                continue;
            }
            // We do a sanity check of the pseudo wire.
            result = verifyPseudoWire(currentL2Tunnel);
            if (result != SUCCESS) {
                continue;
            }
            // We establish the tunnel.
            result = deployPseudoWire(
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
            // We establish the reverse tunnel.
            result = deployPseudoWire(
                    currentL2Tunnel.l2Tunnel(),
                    currentL2Tunnel.l2TunnelPolicy().cP2(),
                    currentL2Tunnel.l2TunnelPolicy().cP1(),
                    REV
            );
            if (result != SUCCESS) {
                continue;
            }
            deployPolicy(
                    l2TunnelId,
                    currentL2Tunnel.l2TunnelPolicy().cP2(),
                    currentL2Tunnel.l2TunnelPolicy().cP2InnerTag(),
                    currentL2Tunnel.l2TunnelPolicy().cP2OuterTag(),
                    result.nextId
            );
        }
    }

    /**
     * Processes Pwaas Config updated event.
     *
     * @param event network config updated event
     */
    public void processPwaasConfigUpdated(NetworkConfigEvent event) {
        log.info("Processing PWaaS CONFIG_UPDATED");
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
        updPws.forEach(tunnelId -> {
            updatePw(
                    prevConfig.getPwDescription(tunnelId),
                    config.getPwDescription(tunnelId)
            );
        });
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
        String fwdKey = generateKey(tunnelId, FWD);
        String revKey = generateKey(tunnelId, REV);
        Result result;
        NextObjective fwdNextObjective;
        NextObjective revNextObjective;
        // The async tasks to orchestrate the next and
        // forwarding update.
        CompletableFuture<ObjectiveError> revPolicyFuture = new CompletableFuture<>();
        CompletableFuture<ObjectiveError> fwdInitNextFuture = new CompletableFuture<>();
        CompletableFuture<ObjectiveError> revInitNextFuture = new CompletableFuture<>();
        CompletableFuture<ObjectiveError> newPwFuture = new CompletableFuture<>();

        result = verifyPseudoWire(newPw);
        if (result != SUCCESS) {
            return;
        }
        if (!l2InitiationNextObjStore.containsKey(fwdKey)) {
            log.warn("NextObj for {} does not exist in the store.", fwdKey);
            return;
        }
        fwdNextObjective = l2InitiationNextObjStore.get(fwdKey).value();
        if (!l2InitiationNextObjStore.containsKey(revKey)) {
            log.warn("NextObj for {} does not exist in the store.", revKey);
            return;
        }
        // First we remove both policy.
        revNextObjective = l2InitiationNextObjStore.get(revKey).value();
        log.debug("Start deleting fwd policy for {}", tunnelId);
        deletePolicy(
                tunnelId,
                oldPw.l2TunnelPolicy().cP1(),
                oldPw.l2TunnelPolicy().cP1InnerTag(),
                oldPw.l2TunnelPolicy().cP1OuterTag(),
                fwdNextObjective.id(),
                revPolicyFuture
        );
        revPolicyFuture.thenAcceptAsync(status -> {
            if (status == null) {
                log.debug("Fwd policy removed. Now remove rev policy for {}", tunnelId);
                deletePolicy(
                        tunnelId,
                        oldPw.l2TunnelPolicy().cP2(),
                        oldPw.l2TunnelPolicy().cP2InnerTag(),
                        oldPw.l2TunnelPolicy().cP2OuterTag(),
                        revNextObjective.id(),
                        fwdInitNextFuture
                );
            }
        });
        // Finally we remove both the tunnels.
        fwdInitNextFuture.thenAcceptAsync(status -> {
            if (status == null) {
                log.debug("Rev policy removed. Now remove fwd pw for {}", tunnelId);
                tearDownPseudoWire(
                        fwdKey,
                        fwdNextObjective,
                        oldPw.l2TunnelPolicy().cP1(),
                        oldPw.l2TunnelPolicy().cP2(),
                        revInitNextFuture
                );
            }
        });
        revInitNextFuture.thenAcceptAsync(status -> {
           if (status == null) {
               log.debug("Fwd tunnel removed. Now remove rev pw for {}", tunnelId);
               tearDownPseudoWire(
                       revKey,
                       revNextObjective,
                       oldPw.l2TunnelPolicy().cP2(),
                       oldPw.l2TunnelPolicy().cP1(),
                       newPwFuture
               );

           }
        });
        // At the end we install the new pw.
        newPwFuture.thenAcceptAsync(status -> {
            if (status == null) {
                log.debug("Deploying new fwd pw for {}", tunnelId);
                Result lamdaResult = deployPseudoWire(
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
                log.debug("Deploying new rev pw for {}", tunnelId);
                lamdaResult = deployPseudoWire(
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
            }
        });
    }

    /**
     * Processes Pwaas Config removed event.
     *
     * @param event network config removed event
     */
    public void processPwaasConfigRemoved(NetworkConfigEvent event) {
        log.info("Processing PWaas CONFIG_REMOVED");
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
        int nextId;
        NextObjective nextObjective;
        long l2TunnelId;
        for (DefaultL2TunnelDescription currentL2Tunnel : pwToRemove) {
            l2TunnelId = currentL2Tunnel.l2Tunnel().tunnelId();
            if (l2TunnelId == 0) {
                log.warn("Tunnel id cannot be 0");
                continue;
            }
            // We do a sanity check of the pseudo wire.
            result = verifyPseudoWire(currentL2Tunnel);
            if (result != SUCCESS) {
                continue;
            }
            String key = generateKey(l2TunnelId, FWD);
            if (!l2InitiationNextObjStore.containsKey(key)) {
                log.warn("NextObj for {} does not exist in the store.", key);
                continue;
            }
            nextObjective = l2InitiationNextObjStore.get(key).value();
            nextId = nextObjective.id();
            // First all we have to delete the policy.
            deletePolicy(
                    l2TunnelId,
                    currentL2Tunnel.l2TunnelPolicy().cP1(),
                    currentL2Tunnel.l2TunnelPolicy().cP1InnerTag(),
                    currentL2Tunnel.l2TunnelPolicy().cP1OuterTag(),
                    nextId,
                    null
            );
            // Finally we will tear down the pseudo wire.
            tearDownPseudoWire(
                    key,
                    nextObjective,
                    currentL2Tunnel.l2TunnelPolicy().cP1(),
                    currentL2Tunnel.l2TunnelPolicy().cP2(),
                    null
            );
            // We do the same operations on the reverse side.
            key = generateKey(l2TunnelId, REV);
            if (!l2InitiationNextObjStore.containsKey(key)) {
                log.warn("NextObj for {} does not exist in the store.", key);
                continue;
            }
            nextObjective = l2InitiationNextObjStore.get(key).value();
            nextId = nextObjective.id();
            deletePolicy(
                    l2TunnelId,
                    currentL2Tunnel.l2TunnelPolicy().cP2(),
                    currentL2Tunnel.l2TunnelPolicy().cP2InnerTag(),
                    currentL2Tunnel.l2TunnelPolicy().cP2OuterTag(),
                    nextId,
                    null
            );
            tearDownPseudoWire(
                    key,
                    nextObjective,
                    currentL2Tunnel.l2TunnelPolicy().cP2(),
                    currentL2Tunnel.l2TunnelPolicy().cP1(),
                    null
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
        // Verify if the tunnel and the policy are supported.
        result = verifyTunnel(l2Tunnel);
        if (result != SUCCESS) {
            log.warn("Tunnel {} did not pass the validation", l2Tunnel.tunnelId());
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
            log.warn("Policy for tunnel {} did not pass the validation", l2Tunnel.tunnelId());
            return result;
        }

        return SUCCESS;
    }

    /**
     * TODO Operation on the policies store.
     *
     * Handles the policy establishment which consists in
     * create the filtering and forwarding objectives related
     * to the initiation and termination.
     *
     * @param tunnelId the tunnel id
     * @param ingress the ingress point
     * @param ingressInner the ingress inner tag
     * @param ingressOuter the ingress outer tag
     * @param nextId the next objective id
     * @return SUCCESS if the policy has been deployed.
     * Otherwise an error according to the failure
     * scenario.
     */
    private Result deployPolicy(long tunnelId,
                                ConnectPoint ingress,
                                VlanId ingressInner,
                                VlanId ingressOuter,
                                int nextId) {

        ForwardingObjective.Builder fwdBuilder;
        FilteringObjective.Builder filtBuilder;
        List<Objective> objectives = Lists.newArrayList();
        // If the instance is not the master, we abort the creation.
        if (!srManager.mastershipService.isLocalMaster(ingress.deviceId())) {
            log.info("Abort creation of policy for L2 tunnel {}: {}", tunnelId, NOT_MASTER);
            return SUCCESS;
        }
        //We create the forwarding objective for supporting
        // the l2 tunnel.
        fwdBuilder = createFwdObjective(
                INITIATION,
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
        filtBuilder = createFiltObjective(
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
                log.debug("Creating new FwdObj for NextObj with id={} for tunnel {}", nextId, tunnelId);
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
     * TODO Operation on the policies store.
     *
     * Handles the tunnel establishment which consists in
     * create the next objectives related to the initiation
     * and termination.
     *
     * @param l2Tunnel the tunnel to deploy
     * @param ingress the ingress connect point
     * @param egress the egress connect point
     * @param direction the direction of the pw
     * @return SUCCESS if the tunnel has been created.
     * Otherwise an error according to the failure
     * scenario
     */
    private Result deployPseudoWire(DefaultL2Tunnel l2Tunnel,
                                    ConnectPoint ingress,
                                    ConnectPoint egress,
                                    String direction) {
        Link nextHop;
        NextObjective.Builder nextObjectiveBuilder;
        NextObjective nextObjective;
        int nextId;
        Result result;
        if (!srManager.mastershipService.isLocalMaster(ingress.deviceId())) {
            log.info("Abort initiation creation of L2 tunnel {}: {}",
                     l2Tunnel.tunnelId(), NOT_MASTER);
            return SUCCESS;
        }
        // We need at least a path between ingress and egress.
        nextHop = getNextHop(ingress, egress);
        if (nextHop == null) {
            log.warn("No path between ingress and egress");
            return WRONG_PARAMETERS;
        }
        // We create the next objective without the metadata
        // context and id. We check if it already exists in the
        // store. If not we store as it is in the store.
        nextObjectiveBuilder = createNextObjective(
                INITIATION,
                nextHop,
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
        nextId = srManager.flowObjectiveService.allocateNextId();
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
        nextObjective = nextObjectiveBuilder.add(context);
        srManager.flowObjectiveService.next(ingress.deviceId(), nextObjective);
        log.debug("Initiation next objective for {} not found. Creating new NextObj with id={}",
                  l2Tunnel.tunnelId(),
                  nextObjective.id()
        );
        result = SUCCESS;
        result.nextId = nextObjective.id();
        return result;
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
     * Create the filtering objective according to a given policy.
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
                .fromApp(srManager.appId);
    }

    /**
     * Create the forwarding objective according to a given pipeline.
     *
     * @param pipeline the pipeline
     * @param tunnelId the tunnel id
     * @param nextId the next step
     * @return the forwarding objective to support the pipeline.
     */
    private ForwardingObjective.Builder createFwdObjective(Pipeline pipeline,
                                                           long tunnelId,
                                                           PortNumber inPort,
                                                           int nextId) {
        ForwardingObjective.Builder fwdBuilder = null;
        TrafficSelector.Builder trafficSelector = DefaultTrafficSelector
                .builder();
        if (pipeline == INITIATION) {
            // The flow has to match on the mpls logical
            // port and the tunnel id.
            trafficSelector.matchTunnelId(tunnelId);
            trafficSelector.matchInPort(inPort);
            // As first step we need to create a forwarding objective.
            fwdBuilder = DefaultForwardingObjective.builder()
                    .fromApp(srManager.appId)
                    .makePermanent()
                    .nextStep(nextId)
                    .withPriority(SegmentRoutingService.DEFAULT_PRIORITY)
                    .withSelector(trafficSelector.build())
                    .withFlag(VERSATILE);
        }
        return fwdBuilder;

    }

    /**
     * Creates the next objective according to a given
     * pipeline. We don't set the next id and we don't
     * create the final meta to check if we are re-using
     * the same next objective for different tunnels.
     *
     * @param pipeline the pipeline to support
     * @param nextHop the next hop towards the destination
     * @param l2Tunnel the tunnel to support
     * @param egressId the egress device id
     * @return the next objective to support the pipeline
     */
    private NextObjective.Builder createNextObjective(Pipeline pipeline,
                                                      Link nextHop,
                                                      DefaultL2Tunnel l2Tunnel,
                                                      DeviceId egressId) {
        NextObjective.Builder nextObjBuilder;
        TrafficTreatment.Builder treatmentBuilder = DefaultTrafficTreatment.builder();
        if (pipeline == INITIATION) {
            nextObjBuilder = DefaultNextObjective
                    .builder()
                    .withType(NextObjective.Type.SIMPLE)
                    .fromApp(srManager.appId);
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
                         srManager.deviceConfiguration.getSegmentId(egressId)
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
                        .deviceConfiguration
                        .getDeviceMac(nextHop.src().deviceId());
            } catch (DeviceConfigNotFoundException e) {
                log.warn("Was not able to find the ingress mac");
                return null;
            }
            treatmentBuilder.setEthSrc(ingressMac);
            MacAddress neighborMac;
            try {
                neighborMac = srManager
                        .deviceConfiguration
                        .getDeviceMac(nextHop.dst().deviceId());
            } catch (DeviceConfigNotFoundException e) {
                log.warn("Was not able to find the neighbor mac");
                return null;
            }
            treatmentBuilder.setEthDst(neighborMac);
        } else {
            // TODO termination
            nextObjBuilder = DefaultNextObjective
                    .builder()
                    .withType(NextObjective.Type.SIMPLE)
                    .fromApp(srManager.appId);

        }
        treatmentBuilder.setOutput(nextHop.src().port());
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
     * TODO Operation on the store.
     * Deletes a given policy using the parameter supplied.
     *
     * @param tunnelId the tunnel id
     * @param ingress the ingress point
     * @param ingressInner the ingress inner vlan id
     * @param ingressOuter the ingress outer vlan id
     * @param nextId the next objective id
     */
    private void deletePolicy(long tunnelId,
                              ConnectPoint ingress,
                              VlanId ingressInner,
                              VlanId ingressOuter,
                              int nextId,
                              CompletableFuture<ObjectiveError> fwdFuture) {

        ForwardingObjective.Builder fwdBuilder;
        FilteringObjective.Builder filtBuilder;
        List<Objective> objectives = Lists.newArrayList();
        if (!srManager.mastershipService.isLocalMaster(ingress.deviceId())) {
            log.info("Abort delete of policy for L2 tunnel {}: {}", tunnelId, NOT_MASTER);
            return;
        }
        fwdBuilder = createFwdObjective(
                INITIATION,
                tunnelId,
                ingress.port(),
                nextId
        );
        ObjectiveContext context = new ObjectiveContext() {
            @Override
            public void onSuccess(Objective objective) {
                log.debug("Previous FwdObj for policy {} removed", tunnelId);
                if (fwdFuture != null) {
                    fwdFuture.complete(null);
                }
            }

            @Override
            public void onError(Objective objective, ObjectiveError error) {
                log.warn("Failed to remove previous FwdObj for policy {}: {}", tunnelId, error);
                if (fwdFuture != null) {
                    fwdFuture.complete(error);
                }
            }
        };
        objectives.add(fwdBuilder.remove(context));
        filtBuilder = createFiltObjective(
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
     * TODO Operation on the store.
     * Deletes a given pseudo wire using the parameter supplied.
     *
     * @param key the key of the store
     * @param nextObjective the next objective representing the pw
     * @param ingress the ingress connect point
     * @param egress the egress connect point
     */
    private void tearDownPseudoWire(String key,
                                    NextObjective nextObjective,
                                    ConnectPoint ingress,
                                    ConnectPoint egress,
                                    CompletableFuture<ObjectiveError> nextFutureForInit) {
        if (!srManager.mastershipService.isLocalMaster(ingress.deviceId())) {
            log.info("Abort delete of {} for {}: {}", INITIATION, key, NOT_MASTER);
            return;
        }
        ObjectiveContext context = new ObjectiveContext() {
            @Override
            public void onSuccess(Objective objective) {
                log.debug("Previous {} NextObj for {} removed", INITIATION, key);
                if (nextFutureForInit != null) {
                    nextFutureForInit.complete(null);
                }
            }

            @Override
            public void onError(Objective objective, ObjectiveError error) {
                log.warn("Failed to remove previous {} NextObj for {}: {}", INITIATION, key, error);
                if (nextFutureForInit != null) {
                    nextFutureForInit.complete(error);
                }
            }
        };
        srManager.flowObjectiveService.next(
                ingress.deviceId(),
                (NextObjective) nextObjective.copy().remove(context)
        );
        l2InitiationNextObjStore.remove(key);
    }

    /**
     * Utilities to generate pw key.
     *
     * @param tunnelId the tunnel id
     * @param direction the direction of the pw
     * @return the key of the store
     */
    private String generateKey(long tunnelId, String direction) {
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
        TERMINATION;
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

        private Result(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public int getCode() {
            return code;
        }

        @Override
        public String toString() {
            return code + ": " + description;
        }
    }

}
