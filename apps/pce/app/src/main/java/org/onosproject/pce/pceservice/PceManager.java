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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.util.Bandwidth;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.incubator.net.tunnel.DefaultTunnel;
import org.onosproject.incubator.net.tunnel.IpTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelEndPoint;
import org.onosproject.incubator.net.tunnel.TunnelEvent;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelListener;
import org.onosproject.incubator.net.tunnel.TunnelName;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultAnnotations.Builder;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.Path;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.MastershipRole;
import org.onosproject.pce.pceservice.constraint.CapabilityConstraint;
import org.onosproject.pce.pceservice.constraint.CapabilityConstraint.CapabilityType;
import org.onosproject.pce.pceservice.constraint.CostConstraint;
import org.onosproject.pce.pceservice.constraint.SharedBandwidthConstraint;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.net.resource.ResourceQueryService;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.Resources;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyService;
import org.onosproject.pce.pceservice.api.PceService;
import org.onosproject.pce.pcestore.PcePathInfo;
import org.onosproject.pce.pcestore.api.PceStore;
import org.onosproject.pcep.api.DeviceCapability;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import static org.onosproject.incubator.net.tunnel.Tunnel.State.INIT;
import static org.onosproject.incubator.net.tunnel.Tunnel.State.UNSTABLE;
import static org.onosproject.incubator.net.tunnel.Tunnel.Type.MPLS;
import static org.onosproject.pce.pceservice.LspType.WITH_SIGNALLING;
import static org.onosproject.pce.pceservice.PcepAnnotationKeys.BANDWIDTH;
import static org.onosproject.pce.pceservice.PcepAnnotationKeys.LOCAL_LSP_ID;
import static org.onosproject.pce.pceservice.PcepAnnotationKeys.LSP_SIG_TYPE;
import static org.onosproject.pce.pceservice.PcepAnnotationKeys.PCE_INIT;
import static org.onosproject.pce.pceservice.PcepAnnotationKeys.PLSP_ID;
import static org.onosproject.pce.pceservice.PcepAnnotationKeys.PCC_TUNNEL_ID;
import static org.onosproject.pce.pceservice.PcepAnnotationKeys.DELEGATE;
import static org.onosproject.pce.pceservice.PcepAnnotationKeys.COST_TYPE;

/**
 * Implementation of PCE service.
 */
@Component(immediate = true)
@Service
public class PceManager implements PceService {
    private static final Logger log = LoggerFactory.getLogger(PceManager.class);

    public static final long GLOBAL_LABEL_SPACE_MIN = 4097;
    public static final long GLOBAL_LABEL_SPACE_MAX = 5121;
    public static final String PCE_SERVICE_APP = "org.onosproject.pce";
    private static final String LOCAL_LSP_ID_GEN_TOPIC = "pcep-local-lsp-id";
    public static final String DEVICE_TYPE = "type";
    public static final String L3_DEVICE = "L3";

    private static final String TUNNEL_CONSUMER_ID_GEN_TOPIC = "pcep-tunnel-consumer-id";
    private IdGenerator tunnelConsumerIdGen;

    private static final String LSRID = "lsrId";
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    public static final int PCEP_PORT = 4189;

    private IdGenerator localLspIdIdGen;
    protected DistributedSet<Short> localLspIdFreeList;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ResourceService resourceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ResourceQueryService resourceQueryService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PathService pathService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PceStore pceStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TunnelService tunnelService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService netCfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TopologyService topologyService;

    private TunnelListener listener = new InnerTunnelListener();
    private ApplicationId appId;

    private final TopologyListener topologyListener = new InternalTopologyListener();

    public static final int INITIAL_DELAY = 30;
    public static final int PERIODIC_DELAY = 30;

    /**
     * Creates new instance of PceManager.
     */
    public PceManager() {
    }

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(PCE_SERVICE_APP);

        tunnelService.addListener(listener);

        tunnelConsumerIdGen = coreService.getIdGenerator(TUNNEL_CONSUMER_ID_GEN_TOPIC);
        localLspIdIdGen = coreService.getIdGenerator(LOCAL_LSP_ID_GEN_TOPIC);
        localLspIdIdGen.getNewId(); // To prevent 0, the 1st value generated from being used in protocol.
        localLspIdFreeList = storageService.<Short>setBuilder()
                .withName("pcepLocalLspIdDeletedList")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .build()
                .asDistributedSet();

        topologyService.addListener(topologyListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        tunnelService.removeListener(listener);
        topologyService.removeListener(topologyListener);
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
            return ImmutableSet.of();
        }
        Set<Path> paths = pathService.getPaths(src, dst, weight(constraints));
        if (!paths.isEmpty()) {
            return paths;
        }
        return ImmutableSet.of();
    }

    //Computes the partial path from partial computed path to specified dst.
    private List<Path> computePartialPath(List<Path> computedPath, DeviceId src, DeviceId dst,
                                    List<Constraint> constraints) {
        int size = computedPath.size();
        Path path = null;
        DeviceId deviceId = size == 0 ? src :
                computedPath.get(size - 1).dst().deviceId();

        Set<Path> tempComputePath = computePath(deviceId, dst, constraints);
        if (tempComputePath.isEmpty()) {
            return null;
        }

        //if path validation fails return null
        //Validate computed path to avoid loop in the path
        for (Path p : tempComputePath) {
            if (pathValidation(computedPath, p)) {
                path = p;
                break;
            }
        }
        if (path == null) {
            return null;
        }

        //Store the partial path result in a list
        computedPath.add(path);
        return computedPath;
    }

    private List<DeviceId> createListOfDeviceIds(List<? extends NetworkResource> list) {
        List<Link> links = new LinkedList<>();
        if (!list.isEmpty() && list.iterator().next() instanceof Path) {
            for (Path path : (List<Path>) list) {
                links.addAll(path.links());
            }
        } else if (!list.isEmpty() && list.iterator().next() instanceof Link) {
            links.addAll((List<Link>) list);
        }

        //List of devices for new path computed
        DeviceId source = null;
        DeviceId destination = null;
        List<DeviceId> devList = new LinkedList<>();

        for (Link l : links) {
            if (!devList.contains(l.src().deviceId())) {
                devList.add(l.src().deviceId());
            }
            if (!devList.contains(l.dst().deviceId())) {
                devList.add(l.dst().deviceId());
            }
        }

        return devList;
    }

    //To dectect loops in the path i.e if the partial paths has intersection node avoid it.
    private boolean pathValidation(List<Path> partialPath, Path path) {

        //List of devices in new path computed
        List<DeviceId> newPartialPathDevList;
        newPartialPathDevList = createListOfDeviceIds(path.links());

        //List of devices in partial computed path
        List<DeviceId> partialComputedPathDevList;
        partialComputedPathDevList = createListOfDeviceIds(partialPath);

        for (DeviceId deviceId : newPartialPathDevList) {
            for (DeviceId devId : partialComputedPathDevList) {
                if (!newPartialPathDevList.get(0).equals(deviceId) &&
                        !partialComputedPathDevList.get(partialComputedPathDevList.size() - 1).equals(devId)
                        && deviceId.equals(devId)) {
                    return false;
                }
            }
        }

        return true;
    }

    //Returns final computed explicit path (list of partial computed paths).
    private List<Path> computeExplicitPath(List<ExplicitPathInfo> explicitPathInfo, DeviceId src, DeviceId dst,
            List<Constraint> constraints) {
        List<Path> finalComputedPath = new LinkedList<>();
        for (ExplicitPathInfo info : explicitPathInfo) {
            /*
             * If explicit path object is LOOSE,
             * 1) If specified as DeviceId (node) :
             * If it is source , compute from source to destination (partial computation not required),
             * otherwise compute from specified source to specified device
             * 2) If specified as Link :
             * Compute partial path from source to link's source , if path exists compute from link's source to dst
             */
            if (info.type().equals(ExplicitPathInfo.Type.LOOSE)) {
                if (info.value() instanceof DeviceId) {
                    // If deviceId is source no need to compute
                    if (!(info.value()).equals(src)) {
                        finalComputedPath = computePartialPath(finalComputedPath, src, (DeviceId) info.value(),
                                constraints);
                    }

                } else if (info.value() instanceof Link) {
                    if ((((Link) info.value()).src().deviceId().equals(src))
                            || (!finalComputedPath.isEmpty()
                            && finalComputedPath.get(finalComputedPath.size() - 1).dst().equals(
                                    ((Link) info.value()).src().deviceId()))) {

                        finalComputedPath = computePartialPath(finalComputedPath, src, ((Link) info.value()).dst()
                                .deviceId(), constraints);
                    } else {

                        finalComputedPath = computePartialPath(finalComputedPath, src, ((Link) info.value()).src()
                                .deviceId(), constraints) != null ? computePartialPath(finalComputedPath, src,
                                ((Link) info.value()).dst().deviceId(), constraints) : null;
                    }
                }
                /*
                 * If explicit path object is STRICT,
                 * 1) If specified as DeviceId (node) :
                 * Check whether partial computed path has reachable to strict specified node or
                 * strict node is the source, if no set path as null else do nothing
                 * 2) If specified as Link :
                 * Check whether partial computed path has reachable to strict link's src, if yes compute
                 * path from strict link's src to link's dst (to include specified link)
                 */
            } else if (info.type().equals(ExplicitPathInfo.Type.STRICT)) {
                if (info.value() instanceof DeviceId) {
                    if (!(!finalComputedPath.isEmpty() && finalComputedPath.get(finalComputedPath.size() - 1).dst()
                            .deviceId().equals(info.value()))
                            && !info.value().equals(src)) {
                        finalComputedPath = null;
                    }

                } else if (info.value() instanceof Link) {

                    finalComputedPath = ((Link) info.value()).src().deviceId().equals(src)
                            || !finalComputedPath.isEmpty()
                            && finalComputedPath.get(finalComputedPath.size() - 1).dst().deviceId()
                                    .equals(((Link) info.value()).src().deviceId()) ? computePartialPath(
                            finalComputedPath, src, ((Link) info.value()).dst().deviceId(), constraints) : null;

                }
            }
            if (finalComputedPath == null) {
                return null;
            }
        }
        // Destination is not reached in Partial computed path then compute till destination
        if (finalComputedPath.isEmpty() || !finalComputedPath.isEmpty()
                && !finalComputedPath.get(finalComputedPath.size() - 1).dst().deviceId().equals(dst)) {

            finalComputedPath = computePartialPath(finalComputedPath, src, dst, constraints);
            if (finalComputedPath == null) {
                return null;
            }
        }

        return finalComputedPath;
    }

    @Override
    public boolean setupPath(DeviceId src, DeviceId dst, String tunnelName, List<Constraint> constraints,
                             LspType lspType) {
        return setupPath(src, dst, tunnelName, constraints, lspType, null);
    }

    //[TODO:] handle requests in queue
    @Override
    public boolean setupPath(DeviceId src, DeviceId dst, String tunnelName, List<Constraint> constraints,
                             LspType lspType, List<ExplicitPathInfo> explicitPathInfo) {
        checkNotNull(src);
        checkNotNull(dst);
        checkNotNull(tunnelName);
        checkNotNull(lspType);

        // Convert from DeviceId to TunnelEndPoint
        Device srcDevice = deviceService.getDevice(src);
        Device dstDevice = deviceService.getDevice(dst);

        if (srcDevice == null || dstDevice == null) {
            // Device is not known.
            pceStore.addFailedPathInfo(new PcePathInfo(src, dst, tunnelName, constraints, lspType, explicitPathInfo));
            return false;
        }

        // In future projections instead of annotations will be used to fetch LSR ID.
        String srcLsrId = srcDevice.annotations().value(LSRID);
        String dstLsrId = dstDevice.annotations().value(LSRID);

        if (srcLsrId == null || dstLsrId == null) {
            // LSR id is not known.
            pceStore.addFailedPathInfo(new PcePathInfo(src, dst, tunnelName, constraints, lspType, explicitPathInfo));
            return false;
        }

        // Get device config from netconfig, to ascertain that session with ingress is present.
        DeviceCapability cfg = netCfgService.getConfig(DeviceId.deviceId(srcLsrId), DeviceCapability.class);
        if (cfg == null) {
            log.debug("No session to ingress.");
            pceStore.addFailedPathInfo(new PcePathInfo(src, dst, tunnelName, constraints, lspType, explicitPathInfo));
            return false;
        }

        TunnelEndPoint srcEndPoint = IpTunnelEndPoint.ipTunnelPoint(IpAddress.valueOf(srcLsrId));
        TunnelEndPoint dstEndPoint = IpTunnelEndPoint.ipTunnelPoint(IpAddress.valueOf(dstLsrId));

        double bwConstraintValue = 0;
        CostConstraint costConstraint = null;
        if (constraints != null) {
            constraints.add(CapabilityConstraint.of(CapabilityType.valueOf(lspType.name())));
            Iterator<Constraint> iterator = constraints.iterator();

            while (iterator.hasNext()) {
                Constraint constraint = iterator.next();
                if (constraint instanceof BandwidthConstraint) {
                    bwConstraintValue = ((BandwidthConstraint) constraint).bandwidth().bps();
                } else if (constraint instanceof CostConstraint) {
                    costConstraint = (CostConstraint) constraint;
                }
            }

            /*
             * Add cost at the end of the list of constraints. The path computation algorithm also computes cumulative
             * cost. The function which checks the limiting/capability constraints also returns per link cost. This
             * function can either return the result of limiting/capability constraint validation or the value of link
             * cost, depending upon what is the last constraint in the loop.
             */
            if (costConstraint != null) {
                constraints.remove(costConstraint);
                constraints.add(costConstraint);
            }
        } else {
            constraints = new LinkedList<>();
            constraints.add(CapabilityConstraint.of(CapabilityType.valueOf(lspType.name())));
        }
        Set<Path> computedPathSet = Sets.newLinkedHashSet();

        if (explicitPathInfo != null && !explicitPathInfo.isEmpty()) {
            List<Path> finalComputedPath = computeExplicitPath(explicitPathInfo, src, dst, constraints);
            if (finalComputedPath == null) {
                return false;
            }

            pceStore.tunnelNameExplicitPathInfoMap(tunnelName, explicitPathInfo);
            List<Link> links = new LinkedList<>();
            double totalCost = 0;
            // Add all partial computed paths
            for (Path path : finalComputedPath) {
                links.addAll(path.links());
                totalCost = totalCost + path.cost();
            }
            computedPathSet.add(new DefaultPath(finalComputedPath.iterator().next().providerId(), links, totalCost));
        } else {
            computedPathSet = computePath(src, dst, constraints);
        }

        // NO-PATH
        if (computedPathSet.isEmpty()) {
            pceStore.addFailedPathInfo(new PcePathInfo(src, dst, tunnelName, constraints, lspType, explicitPathInfo));
            return false;
        }

        Builder annotationBuilder = DefaultAnnotations.builder();
        if (bwConstraintValue != 0) {
            annotationBuilder.set(BANDWIDTH, String.valueOf(bwConstraintValue));
        }
        if (costConstraint != null) {
            annotationBuilder.set(COST_TYPE, String.valueOf(costConstraint.type()));
        }
        annotationBuilder.set(LSP_SIG_TYPE, lspType.name());
        annotationBuilder.set(PCE_INIT, TRUE);
        annotationBuilder.set(DELEGATE, TRUE);

        Path computedPath = computedPathSet.iterator().next();

        if (lspType != WITH_SIGNALLING) {
            /*
             * Local LSP id which is assigned by RSVP for RSVP signalled LSPs, will be assigned by
             * PCE for non-RSVP signalled LSPs.
             */
            annotationBuilder.set(LOCAL_LSP_ID, String.valueOf(getNextLocalLspId()));
        }

        // For SR-TE tunnels, call SR manager for label stack and put it inside tunnel.
        Tunnel tunnel = new DefaultTunnel(null, srcEndPoint, dstEndPoint, MPLS, INIT, null, null,
                                          TunnelName.tunnelName(tunnelName), computedPath,
                                          annotationBuilder.build());

        // Allocate bandwidth.
        TunnelConsumerId consumerId = null;
        if (bwConstraintValue != 0) {
            consumerId = reserveBandwidth(computedPath, bwConstraintValue, null);
            if (consumerId == null) {
                pceStore.addFailedPathInfo(new PcePathInfo(src, dst, tunnelName, constraints,
                        lspType, explicitPathInfo));
                return false;
            }
        }

        TunnelId tunnelId = tunnelService.setupTunnel(appId, src, tunnel, computedPath);
        if (tunnelId == null) {
            pceStore.addFailedPathInfo(new PcePathInfo(src, dst, tunnelName, constraints, lspType, explicitPathInfo));
            if (consumerId != null) {
                resourceService.release(consumerId);
            }
            return false;
        }

        if (consumerId != null) {
            // Store tunnel consumer id in LSP store.
            pceStore.addTunnelInfo(tunnelId, consumerId);
        }
        return true;
    }

    @Override
    public boolean updatePath(TunnelId tunnelId, List<Constraint> constraints) {
        checkNotNull(tunnelId);
        Set<Path> computedPathSet = Sets.newLinkedHashSet();
        Tunnel tunnel = tunnelService.queryTunnel(tunnelId);

        if (tunnel == null) {
            return false;
        }

        if (tunnel.type() != MPLS || FALSE.equalsIgnoreCase(tunnel.annotations().value(DELEGATE))) {
            // Only delegated LSPs can be updated.
            return false;
        }

        List<Link> links = tunnel.path().links();
        String lspSigType = tunnel.annotations().value(LSP_SIG_TYPE);
        double bwConstraintValue = 0;
        String costType = null;
        SharedBandwidthConstraint shBwConstraint = null;
        BandwidthConstraint bwConstraint = null;
        CostConstraint costConstraint = null;

        if (constraints != null) {
            // Call path computation in shared bandwidth mode.
            Iterator<Constraint> iterator = constraints.iterator();
            while (iterator.hasNext()) {
                Constraint constraint = iterator.next();
                if (constraint instanceof BandwidthConstraint) {
                    bwConstraint = (BandwidthConstraint) constraint;
                    bwConstraintValue = bwConstraint.bandwidth().bps();
                } else if (constraint instanceof CostConstraint) {
                    costConstraint = (CostConstraint) constraint;
                    costType = costConstraint.type().name();
                }
            }

            // Remove and keep the cost constraint at the end of the list of constraints.
            if (costConstraint != null) {
                constraints.remove(costConstraint);
            }

            Bandwidth existingBwValue = null;
            String existingBwAnnotation = tunnel.annotations().value(BANDWIDTH);
            if (existingBwAnnotation != null) {
                existingBwValue = Bandwidth.bps(Double.parseDouble(existingBwAnnotation));

                /*
                 * The computation is a shared bandwidth constraint based, so need to remove bandwidth constraint which
                 * has been utilized to create shared bandwidth constraint.
                 */
                if (bwConstraint != null) {
                    constraints.remove(bwConstraint);
                }
            }

            if (existingBwValue != null) {
                if (bwConstraint == null) {
                    bwConstraintValue = existingBwValue.bps();
                }
                //If bandwidth constraints not specified , take existing bandwidth for shared bandwidth calculation
                shBwConstraint = bwConstraint != null ? new SharedBandwidthConstraint(links,
                        existingBwValue, bwConstraint.bandwidth()) : new SharedBandwidthConstraint(links,
                        existingBwValue, existingBwValue);

                constraints.add(shBwConstraint);
            }
        } else {
            constraints = new LinkedList<>();
        }

        constraints.add(CapabilityConstraint.of(CapabilityType.valueOf(lspSigType)));
        if (costConstraint != null) {
            constraints.add(costConstraint);
        } else {
            //Take cost constraint from old tunnel if it is not specified in update flow
            costType = tunnel.annotations().value(COST_TYPE);
            costConstraint = CostConstraint.of(CostConstraint.Type.valueOf(costType));
            constraints.add(costConstraint);
        }

        List<ExplicitPathInfo> explicitPathInfo = pceStore
                .getTunnelNameExplicitPathInfoMap(tunnel.tunnelName().value());
        if (explicitPathInfo != null) {
            List<Path> finalComputedPath = computeExplicitPath(explicitPathInfo,
                    tunnel.path().src().deviceId(), tunnel.path().dst().deviceId(),
                    constraints);

            if (finalComputedPath == null) {
                return false;
            }

            List<Link> totalLinks = new LinkedList<>();
            double totalCost = 0;
            //Add all partial computed paths
            for (Path path : finalComputedPath) {
                totalLinks.addAll(path.links());
                totalCost = totalCost + path.cost();
            }
            computedPathSet.add(new DefaultPath(finalComputedPath.iterator().next().providerId(),
                    totalLinks, totalCost));
        } else {
            computedPathSet = computePath(tunnel.path().src().deviceId(), tunnel.path().dst().deviceId(),
                    constraints);
        }

        // NO-PATH
        if (computedPathSet.isEmpty()) {
            return false;
        }

        Builder annotationBuilder = DefaultAnnotations.builder();
        annotationBuilder.set(BANDWIDTH, String.valueOf(bwConstraintValue));
        if (costType != null) {
            annotationBuilder.set(COST_TYPE, costType);
        }
        annotationBuilder.set(LSP_SIG_TYPE, lspSigType);
        annotationBuilder.set(PCE_INIT, TRUE);
        annotationBuilder.set(DELEGATE, TRUE);
        annotationBuilder.set(PLSP_ID, tunnel.annotations().value(PLSP_ID));
        annotationBuilder.set(PCC_TUNNEL_ID, tunnel.annotations().value(PCC_TUNNEL_ID));

        Path computedPath = computedPathSet.iterator().next();
        TunnelConsumerId consumerId = null;
        LspType lspType = LspType.valueOf(lspSigType);
        long localLspId = 0;
        if (lspType != WITH_SIGNALLING) {
            /*
             * Local LSP id which is assigned by RSVP for RSVP signalled LSPs, will be assigned by
             * PCE for non-RSVP signalled LSPs.
             */
            localLspId = getNextLocalLspId();
            annotationBuilder.set(LOCAL_LSP_ID, String.valueOf(localLspId));
        }

        Tunnel updatedTunnel = new DefaultTunnel(null, tunnel.src(), tunnel.dst(), MPLS, INIT, null, null,
                                                 tunnel.tunnelName(), computedPath,
                                                 annotationBuilder.build());

        // Allocate shared bandwidth.
        if (bwConstraintValue != 0) {
            consumerId = reserveBandwidth(computedPath, bwConstraintValue, shBwConstraint);
            if (consumerId == null) {
                return false;
            }
        }

        TunnelId updatedTunnelId = tunnelService.setupTunnel(appId, links.get(0).src().deviceId(), updatedTunnel,
                                                             computedPath);

        if (updatedTunnelId == null) {
            if (consumerId != null) {
                resourceService.release(consumerId);
            }
            return false;
        }

        if (consumerId != null) {
            // Store tunnel consumer id in LSP store.
            pceStore.addTunnelInfo(updatedTunnelId, consumerId);
        }

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
    private synchronized short getNextLocalLspId() {
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
                    cost = ((CapabilityConstraint) constraint).isValidLink(edge.link(), deviceService,
                                                                           netCfgService) ? 1 : -1;
                } else {
                    cost = constraint.cost(edge.link(), resourceService::isAvailable);
                }
            }
            return cost;
        }
    }

    //TODO: annotations used for temporarily later projection/network config will be used
    private class InternalTopologyListener implements TopologyListener {
       @Override
        public void event(TopologyEvent event) {
             event.reasons().forEach(e -> {
                //If event type is link removed, get the impacted tunnel
                if (e instanceof LinkEvent) {
                    LinkEvent linkEvent = (LinkEvent) e;
                    if (linkEvent.type() == LinkEvent.Type.LINK_REMOVED) {
                        tunnelService.queryTunnel(MPLS).forEach(t -> {
                                if (t.path().links().contains((e.subject()))) {
                                    // Check whether this ONOS instance is master for ingress device if yes,
                                    // recompute and send update
                                    checkForMasterAndUpdateTunnel(t.path().src().deviceId(), t);
                                }
                        });
                    }
                }
                });
        }
    }

    private boolean checkForMasterAndUpdateTunnel(DeviceId src, Tunnel tunnel) {
        /**
         * Master of ingress node will recompute and also delegation flag must be set.
         */
        if (mastershipService.isLocalMaster(src)
                && Boolean.valueOf(tunnel.annotations().value(DELEGATE)) != null) {
            LinkedList<Constraint> constraintList = new LinkedList<>();

            if (tunnel.annotations().value(BANDWIDTH) != null) {
                //Requested bandwidth will be same as previous allocated bandwidth for the tunnel
                BandwidthConstraint localConst = new BandwidthConstraint(Bandwidth.bps(Double.parseDouble(tunnel
                        .annotations().value(BANDWIDTH))));
                constraintList.add(localConst);
            }
            if (tunnel.annotations().value(COST_TYPE) != null) {
                constraintList.add(CostConstraint.of(CostConstraint.Type.valueOf(tunnel.annotations().value(
                        COST_TYPE))));
            }

            /*
             * If tunnel was UP after recomputation failed then store failed path in PCE store send PCIntiate(remove)
             * and If tunnel is failed and computation fails nothing to do because tunnel status will be same[Failed]
             */
            if (!updatePath(tunnel.tunnelId(), constraintList) && !tunnel.state().equals(Tunnel.State.FAILED)) {
                // If updation fails store in PCE store as failed path
                // then PCInitiate (Remove)
                pceStore.addFailedPathInfo(new PcePathInfo(tunnel.path().src().deviceId(), tunnel
                        .path().dst().deviceId(), tunnel.tunnelName().value(), constraintList,
                        LspType.valueOf(tunnel.annotations().value(LSP_SIG_TYPE)),
                         pceStore.getTunnelNameExplicitPathInfoMap(tunnel.tunnelName().value())));
                //Release that tunnel calling PCInitiate
                releasePath(tunnel.tunnelId());
            }
        }

        return false;
    }

     // Allocates the bandwidth locally for PCECC tunnels.
    private TunnelConsumerId reserveBandwidth(Path computedPath, double bandwidthConstraint,
                                  SharedBandwidthConstraint shBwConstraint) {
        checkNotNull(computedPath);
        checkNotNull(bandwidthConstraint);
        Resource resource = null;
        double bwToAllocate = 0;

        TunnelConsumerId consumer = TunnelConsumerId.valueOf(tunnelConsumerIdGen.getNewId());

        /**
         * Shared bandwidth sub-case : Lesser bandwidth required than original -
         * No reservation required.
         */
        Double additionalBwValue = null;
        if (shBwConstraint != null) {
            additionalBwValue = ((bandwidthConstraint - shBwConstraint.sharedBwValue().bps()) <= 0) ? null
                : (bandwidthConstraint - shBwConstraint.sharedBwValue().bps());
        }

        Optional<ResourceAllocation> resAlloc = null;
        for (Link link : computedPath.links()) {
            bwToAllocate = 0;
            if ((shBwConstraint != null) && (shBwConstraint.links().contains(link))) {
                if (additionalBwValue != null) {
                    bwToAllocate = additionalBwValue;
                }
            } else {
                bwToAllocate = bandwidthConstraint;
            }

            /**
             *  In shared bandwidth cases, where new BW is lesser than old BW, it
             *  is not required to allocate anything.
             */
            if (bwToAllocate != 0) {
                resource = Resources.continuous(link.src().deviceId(), link.src().port(), Bandwidth.class)
                        .resource(bwToAllocate);
                resAlloc = resourceService.allocate(consumer, resource);

                // If allocation for any link fails, then release the partially allocated bandwidth.
                if (!resAlloc.isPresent()) {
                    resourceService.release(consumer);
                    return null;
                }
            }
        }

        /*
         * Note: Storing of tunnel consumer id is done by caller of bandwidth reservation function. So deleting tunnel
         * consumer id should be done by caller of bandwidth releasing function. This will prevent ambiguities related
         * to who is supposed to store/delete.
         */
        return consumer;
    }

    /*
     * Deallocates the bandwidth which is reserved locally for PCECC tunnels.
     */
    private void releaseBandwidth(Tunnel tunnel) {
        // Between same source and destination, search the tunnel with same symbolic path name.
        Collection<Tunnel> tunnelQueryResult = tunnelService.queryTunnel(tunnel.src(), tunnel.dst());
        Tunnel newTunnel = null;
        for (Tunnel tunnelObj : tunnelQueryResult) {
            if (tunnel.tunnelName().value().equals(tunnelObj.tunnelName().value())) {
                newTunnel = tunnelObj;
                break;
            }
        }

        // Even if one link is shared, the bandwidth release should happen based on shared mechanism.
        boolean isLinkShared = false;
        if (newTunnel != null) {
            for (Link link : tunnel.path().links()) {
                if (newTunnel.path().links().contains(link)) {
                    isLinkShared = true;
                    break;
                }
            }
        }

        ResourceConsumer tunnelConsumerId = pceStore.getTunnelInfo(tunnel.tunnelId());
        if (tunnelConsumerId == null) {
            //If bandwidth for old tunnel is not allocated i,e 0 then no need to release
            log.debug("Bandwidth not allocated (0 bandwidth) for old LSP.");
            return;
        }

        if (isLinkShared) {
            releaseSharedBandwidth(newTunnel, tunnel);
            return;
        }

        resourceService.release(tunnelConsumerId);
        /*
         * Note: Storing of tunnel consumer id is done by caller of bandwidth reservation function. So deleting tunnel
         * consumer id should be done by caller of bandwidth releasing function. This will prevent ambiguities related
         * to who is supposed to store/delete.
         */
    }

    /**
     *  Re-allocates the bandwidth for the tunnel for which the bandwidth was
     *  allocated in shared mode initially.
     */
    private synchronized void releaseSharedBandwidth(Tunnel newTunnel, Tunnel oldTunnel) {
        // 1. Release old tunnel's bandwidth.
        resourceService.release(pceStore.getTunnelInfo(oldTunnel.tunnelId()));

        // 2. Release new tunnel's bandwidth, if new tunnel bandwidth is allocated
        ResourceConsumer consumer = pceStore.getTunnelInfo(newTunnel.tunnelId());
        if (consumer == null) {
            //If bandwidth for new tunnel is not allocated i,e 0 then no need to allocate
            return;
        }

        resourceService.release(consumer);

        // 3. Allocate new tunnel's complete bandwidth.
        double bandwidth = Double.parseDouble(newTunnel.annotations().value(BANDWIDTH));
        Resource resource;

        for (Link link : newTunnel.path().links()) {
            resource = Resources.continuous(link.src().deviceId(), link.src().port(), Bandwidth.class)
                    .resource(bandwidth);
            resourceService.allocate(consumer, resource); // Reusing new tunnel's TunnelConsumerId intentionally.

        }
    }

    // Listens on tunnel events.
    private class InnerTunnelListener implements TunnelListener {
        @Override
        public void event(TunnelEvent event) {
            // Event gets generated with old tunnel object.
            Tunnel tunnel = event.subject();
            if (tunnel.type() != MPLS) {
                return;
            }

            LspType lspType = LspType.valueOf(tunnel.annotations().value(LSP_SIG_TYPE));
            String tunnelBandwidth = tunnel.annotations().value(BANDWIDTH);
            double bwConstraintValue = 0;
            if (tunnelBandwidth != null) {
                bwConstraintValue = Double.parseDouble(tunnelBandwidth);
            }

            switch (event.type()) {
            case TUNNEL_ADDED:
                // Allocate bandwidth for non-initiated, delegated LSPs with non-zero bandwidth (learned LSPs).
                String pceInit = tunnel.annotations().value(PCE_INIT);
                if (FALSE.equalsIgnoreCase(pceInit) && bwConstraintValue != 0) {
                    TunnelConsumerId consumerId = reserveBandwidth(tunnel.path(), bwConstraintValue, null);
                    if (consumerId != null) {
                        // Store tunnel consumer id in LSP store.
                        pceStore.addTunnelInfo(tunnel.tunnelId(), consumerId);
                    }
                }
                break;

            case TUNNEL_UPDATED:
                if (tunnel.state() == UNSTABLE) {
                    /*
                     * During LSP DB sync if PCC doesn't report LSP which was PCE initiated, it's state is turned into
                     * unstable so that it can be setup again. Add into failed path store so that it can be recomputed
                     * and setup while global reoptimization.
                     */

                    List<Constraint> constraints = new LinkedList<>();
                    String bandwidth = tunnel.annotations().value(BANDWIDTH);
                    if (bandwidth != null) {
                        constraints.add(new BandwidthConstraint(Bandwidth
                                .bps(Double.parseDouble(bandwidth))));
                    }

                    String costType = tunnel.annotations().value(COST_TYPE);
                    if (costType != null) {
                        CostConstraint costConstraint = new CostConstraint(CostConstraint.Type.valueOf(costType));
                        constraints.add(costConstraint);
                    }

                    constraints.add(CapabilityConstraint
                            .of(CapabilityType.valueOf(tunnel.annotations().value(LSP_SIG_TYPE))));

                    List<Link> links = tunnel.path().links();
                    pceStore.addFailedPathInfo(new PcePathInfo(links.get(0).src().deviceId(),
                                                                  links.get(links.size() - 1).dst().deviceId(),
                                                                  tunnel.tunnelName().value(), constraints, lspType,
                                                                  pceStore.getTunnelNameExplicitPathInfoMap(tunnel
                                                                          .tunnelName().value())));
                }

                break;

            case TUNNEL_REMOVED:
                if (lspType != WITH_SIGNALLING) {
                    localLspIdFreeList.add(Short.valueOf(tunnel.annotations().value(LOCAL_LSP_ID)));
                }
                // If not zero bandwidth, and delegated (initiated LSPs will also be delegated).
                if (bwConstraintValue != 0
                        && mastershipService.getLocalRole(tunnel.path().src().deviceId()) == MastershipRole.MASTER) {
                    releaseBandwidth(tunnel);
                }

                if (pceStore.getTunnelInfo(tunnel.tunnelId()) != null) {
                    pceStore.removeTunnelInfo(tunnel.tunnelId());
                }

                break;

            default:
                break;

            }
            return;
        }
    }

    @Override
    public List<ExplicitPathInfo> explicitPathInfoList(String tunnelName) {
        return pceStore.getTunnelNameExplicitPathInfoMap(tunnelName);
    }

    //Computes path from tunnel store and also path failed to setup.
    private void callForOptimization() {
        //Recompute the LSPs which it was delegated [LSPs stored in PCE store (failed paths)]
        for (PcePathInfo failedPathInfo : pceStore.getFailedPathInfos()) {
            checkForMasterAndSetupPath(failedPathInfo);
        }

        //Recompute the LSPs for which it was delegated [LSPs stored in tunnel store]
        tunnelService.queryTunnel(MPLS).forEach(t -> {
        checkForMasterAndUpdateTunnel(t.path().src().deviceId(), t);
        });
    }

    private boolean checkForMasterAndSetupPath(PcePathInfo failedPathInfo) {
        /**
         * Master of ingress node will setup the path failed stored in PCE store.
         */
        if (mastershipService.isLocalMaster(failedPathInfo.src())) {
            if (setupPath(failedPathInfo.src(), failedPathInfo.dst(), failedPathInfo.name(),
                    failedPathInfo.constraints(), failedPathInfo.lspType(), failedPathInfo.explicitPathInfo())) {
                // If computation is success remove that path
                pceStore.removeFailedPathInfo(failedPathInfo);
                return true;
            }
        }

        return false;
    }

    //Timer to call global optimization
    private class GlobalOptimizationTimer implements Runnable {

        public GlobalOptimizationTimer() {
        }

        @Override
        public void run() {
            callForOptimization();
        }
    }
}