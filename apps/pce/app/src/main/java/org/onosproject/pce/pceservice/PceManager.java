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
package org.onosproject.pce.pceservice;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.incubator.net.tunnel.Tunnel.Type.MPLS;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Path;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.pce.pceservice.api.PceService;
import org.onosproject.pce.pceservice.constraint.CapabilityConstraint;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

/**
 * Implementation of PCE service.
 */
@Component(immediate = true)
@Service
public class PceManager implements PceService {
    private static final Logger log = LoggerFactory.getLogger(PceManager.class);

    public static final String PCE_SERVICE_APP = "org.onosproject.pce";

    private static final String LOCAL_LSP_ID_GEN_TOPIC = "pcep-local-lsp-id";
    private IdGenerator localLspIdIdGen;
    protected DistributedSet<Short> localLspIdFreeList;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TunnelService tunnelService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PathService pathService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ResourceService resourceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private ApplicationId appId;

    /**
     * Creates new instance of PceManager.
     */
    public PceManager() {
    }

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(PCE_SERVICE_APP);
        log.info("Started");

        localLspIdIdGen = coreService.getIdGenerator(LOCAL_LSP_ID_GEN_TOPIC);
        localLspIdFreeList = storageService.<Short>setBuilder()
                .withName("pcepLocalLspIdDeletedList")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .build()
                .asDistributedSet();
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    /**
     * Returns an edge-weight capable of evaluating links on the basis of the
     * specified constraints.
     *
     * @param constraints path constraints
     * @return edge-weight function
     */
    private LinkWeight weight(List<Constraint> constraints) {
        return new TeConstraintBasedLinkWeight(constraints);
    }

    /**
     * Computes a path between two devices.
     *
     * @param src ingress device
     * @param dst egress device
     * @param constraints path constraints
     * @return computed path based on constraints
     */
    protected Set<Path> computePath(DeviceId src, DeviceId dst, List<Constraint> constraints) {
        if (pathService == null) {
            return null;
        }
        Set<Path> paths = pathService.getPaths(src, dst, weight(constraints));
        if (!paths.isEmpty()) {
            return paths;
        }
        return null;
    }

    //[TODO:] handle requests in queue
    @Override
    public boolean setupPath(DeviceId src, DeviceId dst, String tunnelName, List<Constraint> constraints,
                             LspType lspType) {
        checkNotNull(src);
        checkNotNull(dst);
        checkNotNull(tunnelName);
        checkNotNull(constraints);
        checkNotNull(lspType);

        // TODO: compute and setup path.
        //TODO: gets the path based on constraints and creates a tunnel in network via tunnel manager
        return computePath(src, dst, constraints) != null ? true : false;
    }


    @Override
    public boolean updatePath(TunnelId tunnelId, List<Constraint> constraints) {
        checkNotNull(tunnelId);
        checkNotNull(constraints);

        // TODO: compute and update path.

        return true;
    }

    @Override
    public boolean releasePath(TunnelId tunnelId) {
        checkNotNull(tunnelId);
        // 1. Query Tunnel from Tunnel manager.
        Tunnel tunnel = tunnelService.queryTunnel(tunnelId);

        if (tunnel == null) {
            return false;
        }

        // 2. Call tunnel service.
        return tunnelService.downTunnel(appId, tunnel.tunnelId());
    }

    @Override
    public Iterable<Tunnel> queryAllPath() {
        return tunnelService.queryTunnel(MPLS);
    }

    @Override
    public Tunnel queryPath(TunnelId tunnelId) {
        return tunnelService.queryTunnel(tunnelId);
    }

    /**
     * Returns the next local LSP identifier to be used either by getting from
     * freed list if available otherwise generating a new one.
     *
     * @return value of local LSP identifier
     */
    private short getNextLocalLspId() {
        // If there is any free id use it. Otherwise generate new id.
        if (localLspIdFreeList.isEmpty()) {
            return (short) localLspIdIdGen.getNewId();
        }
        Iterator<Short> it = localLspIdFreeList.iterator();
        Short value = it.next();
        localLspIdFreeList.remove(value);
        return value;
    }

    protected class TeConstraintBasedLinkWeight implements LinkWeight {

        private final List<Constraint> constraints;

        /**
         * Creates a new edge-weight function capable of evaluating links
         * on the basis of the specified constraints.
         *
         * @param constraints path constraints
         */
        public TeConstraintBasedLinkWeight(List<Constraint> constraints) {
            if (constraints == null) {
                this.constraints = Collections.emptyList();
            } else {
                this.constraints = ImmutableList.copyOf(constraints);
            }
        }

        @Override
        public double weight(TopologyEdge edge) {
            if (!constraints.iterator().hasNext()) {
                //Takes default cost/hopcount as 1 if no constraints specified
                return 1.0;
            }

            Iterator<Constraint> it = constraints.iterator();
            double cost = 1;

            //If any constraint fails return -1 also value of cost returned from cost constraint can't be negative
            while (it.hasNext() && cost > 0) {
                Constraint constraint = it.next();
                if (constraint instanceof CapabilityConstraint) {
                    cost = ((CapabilityConstraint) constraint).isValidLink(edge.link(), deviceService) ? 1 : -1;
                } else {
                    cost = constraint.cost(edge.link(), resourceService::isAvailable);
                }
            }
            return cost;
        }
    }
}