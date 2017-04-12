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

import org.onosproject.net.DisjointPath;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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
import org.onosproject.net.LinkKey;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultAnnotations.Builder;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.Path;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.MastershipRole;
import org.onosproject.bandwidthmgr.api.BandwidthMgmtService;
import org.onosproject.pce.pceservice.constraint.CapabilityConstraint;
import org.onosproject.pce.pceservice.constraint.CapabilityConstraint.CapabilityType;
import org.onosproject.pce.pceservice.constraint.CostConstraint;
import org.onosproject.pce.pceservice.constraint.PceBandwidthConstraint;
import org.onosproject.pce.pceservice.constraint.SharedBandwidthConstraint;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceAllocation;
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
import org.onosproject.pcep.api.TeLinkConfig;
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

    private static final String LSRID = "lsrId";
    private static final String TRUE = "true";
    private static final String FALSE = "false";
    public static final int PCEP_PORT = 4189;

    private IdGenerator localLspIdIdGen;
    protected DistributedSet<Short> localLspIdFreeList;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected BandwidthMgmtService bandwidthMgmtService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry netConfigRegistry;

    private TunnelListener listener = new InnerTunnelListener();
    private ApplicationId appId;

    private final TopologyListener topologyListener = new InternalTopologyListener();
    public static final String LOAD_BALANCING_PATH_NAME = "loadBalancingPathName";

    private List<TunnelId> rsvpTunnelsWithLocalBw = new ArrayList<>();

    private final ConfigFactory<LinkKey, TeLinkConfig> configFactory =
            new ConfigFactory<LinkKey, TeLinkConfig>(SubjectFactories.LINK_SUBJECT_FACTORY,
                    TeLinkConfig.class, "teLinkConfig") {
                @Override
                public TeLinkConfig createConfig() {
                    return new TeLinkConfig();
                }
            };

    /**
     * Creates new instance of PceManager.
     */
    public PceManager() {
    }

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(PCE_SERVICE_APP);

        tunnelService.addListener(listener);

        localLspIdIdGen = coreService.getIdGenerator(LOCAL_LSP_ID_GEN_TOPIC);
        localLspIdIdGen.getNewId(); // To prevent 0, the 1st value generated from being used in protocol.
        localLspIdFreeList = storageService.<Short>setBuilder()
                .withName("pcepLocalLspIdDeletedList")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .build()
                .asDistributedSet();

        topologyService.addListener(topologyListener);
        netConfigRegistry.registerConfigFactory(configFactory);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        tunnelService.removeListener(listener);
        topologyService.removeListener(topologyListener);
        netConfigRegistry.unregisterConfigFactory(configFactory);

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
        log.info("paths in computePath ::" + paths);
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
                        log.debug("computeExplicitPath :: Loose , device");
                        finalComputedPath = computePartialPath(finalComputedPath, src, (DeviceId) info.value(),
                                constraints);
                        log.debug("finalComputedPath in computeExplicitPath ::" + finalComputedPath);
                    }

                } else if (info.value() instanceof Link) {
                    if ((((Link) info.value()).src().deviceId().equals(src))
                            || (!finalComputedPath.isEmpty()
                            && finalComputedPath.get(finalComputedPath.size() - 1).dst().deviceId().equals(
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
                 * Check whether partial computed path has reachable to strict specified node orde
                 * strict node is the source, if no set path as null else do nothing
                 * 2) If specified as Link :
                 * Check whether partial computed path has reachable to strict link's src, if yes compute
                 * path from strict link's src to link's dst (to include specified link)
                 */
            } else if (info.type().equals(ExplicitPathInfo.Type.STRICT)) {
                if (info.value() instanceof DeviceId) {
                    log.debug("computeExplicitPath :: Strict , device");
                    if (!(!finalComputedPath.isEmpty() && finalComputedPath.get(finalComputedPath.size() - 1).dst()
                            .deviceId().equals(info.value()))
                            && !info.value().equals(src)) {
                        finalComputedPath = null;
                    }

                } else if (info.value() instanceof Link) {
                    log.info("computeExplicitPath :: Strict");
                    finalComputedPath = ((Link) info.value()).src().deviceId().equals(src)
                            || !finalComputedPath.isEmpty()
                            && finalComputedPath.get(finalComputedPath.size() - 1).dst().deviceId()
                                    .equals(((Link) info.value()).src().deviceId()) ? computePartialPath(
                            finalComputedPath, src, ((Link) info.value()).dst().deviceId(), constraints) : null;

                    //Log.info("computeExplicitPath :: (Link) info.value() " + (Link) info.value());
                    //Log.info("computeExplicitPath :: finalComputedPath " + finalComputedPath);

                    if (finalComputedPath != null && !finalComputedPath.get(finalComputedPath.size() - 1).links()
                            .contains((Link) info.value())) {
                        finalComputedPath = null;
                    }
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
        return setupPath(src, dst, tunnelName, constraints, lspType, null, false);
    }

    //[TODO:] handle requests in queue
    @Override
    public boolean setupPath(DeviceId src, DeviceId dst, String tunnelName, List<Constraint> constraints,
                             LspType lspType, List<ExplicitPathInfo> explicitPathInfo) {
        return setupPath(src, dst, tunnelName, constraints, lspType, explicitPathInfo, false);

    }

    @Override
    public boolean setupPath(DeviceId src, DeviceId dst, String tunnelName, List<Constraint> constraints,
                             LspType lspType, boolean loadBalancing) {
        return setupPath(src, dst, tunnelName, constraints, lspType, null, loadBalancing);
    }

    @Override
    public boolean setupPath(DeviceId src, DeviceId dst, String tunnelName, List<Constraint> constraints,
                             LspType lspType, List<ExplicitPathInfo> explicitPathInfo, boolean loadBalancing) {
        checkNotNull(src);
        checkNotNull(dst);
        checkNotNull(tunnelName);
        checkNotNull(lspType);

        // Convert from DeviceId to TunnelEndPoint
        Device srcDevice = deviceService.getDevice(src);
        Device dstDevice = deviceService.getDevice(dst);

        if (srcDevice == null || dstDevice == null) {
            // Device is not known.
            pceStore.addFailedPathInfo(new PcePathInfo(src, dst, tunnelName, constraints, lspType, explicitPathInfo,
                    loadBalancing));
            return false;
        }

        // In future projections instead of annotations will be used to fetch LSR ID.
        String srcLsrId = srcDevice.annotations().value(LSRID);
        String dstLsrId = dstDevice.annotations().value(LSRID);

        if (srcLsrId == null || dstLsrId == null) {
            // LSR id is not known.
            pceStore.addFailedPathInfo(new PcePathInfo(src, dst, tunnelName, constraints, lspType, explicitPathInfo,
                    loadBalancing));
            return false;
        }

        // Get device config from netconfig, to ascertain that session with ingress is present.
        DeviceCapability cfg = netCfgService.getConfig(DeviceId.deviceId(srcLsrId), DeviceCapability.class);
        if (cfg == null) {
            log.debug("No session to ingress.");
            pceStore.addFailedPathInfo(new PcePathInfo(src, dst, tunnelName, constraints, lspType, explicitPathInfo,
                    loadBalancing));
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
                if (constraint instanceof PceBandwidthConstraint) {
                    bwConstraintValue = ((PceBandwidthConstraint) constraint).bandwidth().bps();
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

        if (loadBalancing) {
            return setupDisjointPaths(src, dst, constraints, tunnelName, bwConstraintValue, lspType, costConstraint,
                    srcEndPoint, dstEndPoint);
        }

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
            pceStore.addFailedPathInfo(new PcePathInfo(src, dst, tunnelName, constraints, lspType, explicitPathInfo,
                    loadBalancing));
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

        // Allocate bandwidth for all tunnels.
        if (bwConstraintValue != 0) {
            if (!reserveBandwidth(computedPath, bwConstraintValue, null)) {
                pceStore.addFailedPathInfo(new PcePathInfo(src, dst, tunnelName, constraints,
                        lspType, explicitPathInfo, loadBalancing));
                return false;
            }
        }

        TunnelId tunnelId = tunnelService.setupTunnel(appId, src, tunnel, computedPath);
        if (tunnelId == null) {
            pceStore.addFailedPathInfo(new PcePathInfo(src, dst, tunnelName, constraints, lspType, explicitPathInfo,
                    loadBalancing));

            if (bwConstraintValue != 0) {
                computedPath.links().forEach(ln -> bandwidthMgmtService.releaseLocalReservedBw(LinkKey.linkKey(ln),
                        Double.parseDouble(tunnel.annotations().value(BANDWIDTH))));
            }

            return false;
        }

        if (bwConstraintValue != 0 && lspType == WITH_SIGNALLING) {
            rsvpTunnelsWithLocalBw.add(tunnelId);
        }

        return true;
    }

    private boolean setupDisjointPaths(DeviceId src, DeviceId dst, List<Constraint> constraints, String tunnelName,
                                       double bwConstraintValue, LspType lspType, CostConstraint costConstraint,
                                       TunnelEndPoint srcEndPoint, TunnelEndPoint dstEndPoint) {
        Set<DisjointPath> paths = pathService.getDisjointPaths(src, dst, weight(constraints));

        // NO-PATH
        if (paths.isEmpty()) {
            pceStore.addFailedPathInfo(new PcePathInfo(src, dst, tunnelName, constraints, lspType, null, true));
            return false;
        }

        DisjointPath path = null;
        if (!paths.isEmpty()) {
            path = paths.iterator().next();
        }

        Builder annotationBuilder = DefaultAnnotations.builder();
        double bw = 0;
        if (bwConstraintValue != 0) {
            //TODO: BW needs to be divided by 2 :: bwConstraintValue/2
            bw = bwConstraintValue / 2;
            annotationBuilder.set(BANDWIDTH, String.valueOf(bw));
        }
        if (costConstraint != null) {
            annotationBuilder.set(COST_TYPE, String.valueOf(costConstraint.type()));
        }
        annotationBuilder.set(LSP_SIG_TYPE, lspType.name());
        annotationBuilder.set(PCE_INIT, TRUE);
        annotationBuilder.set(DELEGATE, TRUE);
        annotationBuilder.set(LOAD_BALANCING_PATH_NAME, tunnelName);

        //Path computedPath = computedPathSet.iterator().next();

        if (lspType != WITH_SIGNALLING) {
            /*
             * Local LSP id which is assigned by RSVP for RSVP signalled LSPs, will be assigned by
             * PCE for non-RSVP signalled LSPs.
             */
            annotationBuilder.set(LOCAL_LSP_ID, String.valueOf(getNextLocalLspId()));
        }

        //Generate different tunnel name for disjoint paths
        String tunnel1 = (new StringBuilder()).append(tunnelName).append("_1").toString();
        String tunnel2 = (new StringBuilder()).append(tunnelName).append("_2").toString();

        // For SR-TE tunnels, call SR manager for label stack and put it inside tunnel.
        Tunnel tunnelPrimary = new DefaultTunnel(null, srcEndPoint, dstEndPoint, MPLS, INIT, null, null,
                TunnelName.tunnelName(tunnel1), path.primary(),
                annotationBuilder.build());

        Tunnel tunnelBackup = new DefaultTunnel(null, srcEndPoint, dstEndPoint, MPLS, INIT, null, null,
                TunnelName.tunnelName(tunnel2), path.backup(),
                annotationBuilder.build());

        // Allocate bandwidth.
        if (bwConstraintValue != 0) {
            if (!reserveBandwidth(path.primary(), bw, null)) {
                pceStore.addFailedPathInfo(new PcePathInfo(src, dst, tunnel1, constraints,
                                                           lspType, null, true));
                return false;
            }

            if (!reserveBandwidth(path.backup(), bw, null)) {
                //Release bandwidth resource for tunnel1
                if (bwConstraintValue != 0) {
                    path.primary().links().forEach(ln ->
                                         bandwidthMgmtService.releaseLocalReservedBw(LinkKey.linkKey(ln),
                                         Double.parseDouble(tunnelPrimary.annotations().value(BANDWIDTH))));
                }

                pceStore.addFailedPathInfo(new PcePathInfo(src, dst, tunnel2, constraints,
                                                           lspType, null, true));
                return false;
            }
        }

        TunnelId tunnelId1 = tunnelService.setupTunnel(appId, src, tunnelPrimary, path.primary());
        if (tunnelId1 == null) {
            pceStore.addFailedPathInfo(new PcePathInfo(src, dst, tunnelName, constraints, lspType, null, true));

            if (bwConstraintValue != 0) {
                path.primary().links().forEach(ln -> bandwidthMgmtService.releaseLocalReservedBw(LinkKey.linkKey(ln),
                                                   Double.parseDouble(tunnelPrimary.annotations().value(BANDWIDTH))));
            }

            return false;
        }

        TunnelId tunnelId2 = tunnelService.setupTunnel(appId, src, tunnelBackup, path.backup());
        if (tunnelId2 == null) {
            //Release 1st tunnel
            releasePath(tunnelId1);

            pceStore.addFailedPathInfo(new PcePathInfo(src, dst, tunnelName, constraints, lspType, null, true));

            if (bwConstraintValue != 0) {
                path.backup().links().forEach(ln -> bandwidthMgmtService.releaseLocalReservedBw(LinkKey.linkKey(ln),
                                                    Double.parseDouble(tunnelBackup.annotations().value(BANDWIDTH))));
            }

            return false;
        }

        pceStore.addLoadBalancingTunnelIdsInfo(tunnelName, tunnelId1, tunnelId2);
        //pceStore.addDisjointPathInfo(tunnelName, path);
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
        PceBandwidthConstraint bwConstraint = null;
        CostConstraint costConstraint = null;

        if (constraints != null) {
            // Call path computation in shared bandwidth mode.
            Iterator<Constraint> iterator = constraints.iterator();
            while (iterator.hasNext()) {
                Constraint constraint = iterator.next();
                if (constraint instanceof PceBandwidthConstraint) {
                    bwConstraint = (PceBandwidthConstraint) constraint;
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

        // Allocate shared bandwidth for all tunnels.
        if (bwConstraintValue != 0) {
            if (!reserveBandwidth(computedPath, bwConstraintValue, shBwConstraint)) {
                return false;
            }
        }

        TunnelId updatedTunnelId = tunnelService.setupTunnel(appId, links.get(0).src().deviceId(), updatedTunnel,
                                                             computedPath);

        if (updatedTunnelId == null) {
            if (bwConstraintValue != 0) {
                releaseSharedBwForNewTunnel(computedPath, bwConstraintValue, shBwConstraint);
            }
            return false;
        }

        if (bwConstraintValue != 0 && lspType == WITH_SIGNALLING) {
            rsvpTunnelsWithLocalBw.add(updatedTunnelId);
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
    public boolean releasePath(String loadBalancingPathName) {
        checkNotNull(loadBalancingPathName);

        List<TunnelId> tunnelIds = pceStore.getLoadBalancingTunnelIds(loadBalancingPathName);
        if (tunnelIds != null && !tunnelIds.isEmpty()) {
            for (TunnelId id : tunnelIds) {
                if (!tunnelService.downTunnel(appId, id)) {
                    return false;
                }
            }

            //pceStore.removeDisjointPathInfo(loadBalancedPathName);
            pceStore.removeLoadBalancingTunnelIdsInfo(loadBalancingPathName);
            return true;
        }

        return false;
    }

    @Override
    public Iterable<Tunnel> queryAllPath() {
        return tunnelService.queryTunnel(MPLS);
    }

    @Override
    public Tunnel queryPath(TunnelId tunnelId) {
        return tunnelService.queryTunnel(tunnelId);
    }

    private boolean releaseSharedBwForNewTunnel(Path computedPath, double bandwidthConstraint,
                                                SharedBandwidthConstraint shBwConstraint) {
        checkNotNull(computedPath);
        checkNotNull(bandwidthConstraint);
        double bwToAllocate;

        Double additionalBwValue = null;
        if (shBwConstraint != null) {
            additionalBwValue = ((bandwidthConstraint - shBwConstraint.sharedBwValue().bps()) <= 0) ? null
                    : (bandwidthConstraint - shBwConstraint.sharedBwValue().bps());
        }

        for (Link link : computedPath.links()) {
            bwToAllocate = 0;
            if ((shBwConstraint != null) && (shBwConstraint.links().contains(link))) {
                if (additionalBwValue != null) {
                    bwToAllocate = additionalBwValue;
                }
            } else {
                bwToAllocate = bandwidthConstraint;
            }

            if (bwToAllocate != 0) {
                bandwidthMgmtService.releaseLocalReservedBw(LinkKey.linkKey(link), bwToAllocate);
            }
        }
        return true;
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
                } else if (constraint instanceof PceBandwidthConstraint) {
                    cost = ((PceBandwidthConstraint) constraint).isValidLink(edge.link(),
                            bandwidthMgmtService) ? 1 : -1;
                } else if (constraint instanceof SharedBandwidthConstraint) {
                    cost = ((SharedBandwidthConstraint) constraint).isValidLink(edge.link(),
                            bandwidthMgmtService) ? 1 : -1;
                } else if (constraint instanceof CostConstraint) {
                    cost = ((CostConstraint) constraint).isValidLink(edge.link(), netCfgService);
                } else {
                    cost = constraint.cost(edge.link(), null);
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
                PceBandwidthConstraint localConst = new PceBandwidthConstraint(Bandwidth.bps(Double.parseDouble(tunnel
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
                         pceStore.getTunnelNameExplicitPathInfoMap(tunnel.tunnelName().value()),
                        tunnel.annotations().value(LOAD_BALANCING_PATH_NAME) != null ? true : false));
                //Release that tunnel calling PCInitiate
                releasePath(tunnel.tunnelId());
            }
        }

        return false;
    }

     // Allocates the bandwidth locally for PCECC tunnels.
    private boolean reserveBandwidth(Path computedPath, double bandwidthConstraint,
                                  SharedBandwidthConstraint shBwConstraint) {
        checkNotNull(computedPath);
        checkNotNull(bandwidthConstraint);
        Resource resource = null;
        double bwToAllocate = 0;
        Map<Link, Double> linkMap = new HashMap<>();

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
                if (!bandwidthMgmtService.allocLocalReservedBw(LinkKey.linkKey(link.src(), link.dst()),
                        bwToAllocate)) {
                    // If allocation for any link fails, then release the partially allocated bandwidth
                    // for all links allocated
                    linkMap.forEach((ln, aDouble) -> bandwidthMgmtService
                                                     .releaseLocalReservedBw(LinkKey.linkKey(ln), aDouble));
                    return false;
                }

                linkMap.put(link, bwToAllocate);
            }
        }

        return true;
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

        if (isLinkShared) {
            releaseSharedBandwidth(newTunnel, tunnel);
            return;
        }

        tunnel.path().links().forEach(tn -> bandwidthMgmtService.releaseLocalReservedBw(LinkKey.linkKey(tn),
                Double.parseDouble(tunnel.annotations().value(BANDWIDTH))));
    }

    /**
     *  Re-allocates the bandwidth for the tunnel for which the bandwidth was
     *  allocated in shared mode initially.
     */
    private synchronized void releaseSharedBandwidth(Tunnel newTunnel, Tunnel oldTunnel) {

        boolean isAllocate = false;
        Double oldTunnelBw = Double.parseDouble(oldTunnel.annotations().value(BANDWIDTH));
        Double newTunnelBw = Double.parseDouble(newTunnel.annotations().value(BANDWIDTH));

        if (newTunnelBw > oldTunnelBw) {
            isAllocate = true;
        }

        for (Link link : newTunnel.path().links()) {
            if (oldTunnel.path().links().contains(link)) {
                if (!isAllocate) {
                    bandwidthMgmtService.releaseLocalReservedBw(LinkKey.linkKey(link),
                            oldTunnelBw - newTunnelBw);
                }
            } else {
                bandwidthMgmtService.releaseLocalReservedBw(LinkKey.linkKey(link), oldTunnelBw);
            }
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
            case TUNNEL_UPDATED:
                if (rsvpTunnelsWithLocalBw.contains(tunnel.tunnelId())) {
                    releaseBandwidth(event.subject());
                        rsvpTunnelsWithLocalBw.remove(tunnel.tunnelId());
                }

                if (tunnel.state() == UNSTABLE) {
                    /*
                     * During LSP DB sync if PCC doesn't report LSP which was PCE initiated, it's state is turned into
                     * unstable so that it can be setup again. Add into failed path store so that it can be recomputed
                     * and setup while global reoptimization.
                     */

                    List<Constraint> constraints = new LinkedList<>();
                    String bandwidth = tunnel.annotations().value(BANDWIDTH);
                    if (bandwidth != null) {
                        constraints.add(new PceBandwidthConstraint(Bandwidth
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
                                                                          .tunnelName().value()), tunnel.annotations()
                            .value(LOAD_BALANCING_PATH_NAME) != null ? true : false));
                }

                break;

            case TUNNEL_REMOVED:
                if (lspType != WITH_SIGNALLING) {
                    localLspIdFreeList.add(Short.valueOf(tunnel.annotations().value(LOCAL_LSP_ID)));
                }
                // If not zero bandwidth, and delegated (initiated LSPs will also be delegated).
                if (bwConstraintValue != 0 && mastershipService.getLocalRole(tunnel.path().src()
                        .deviceId()) == MastershipRole.MASTER) {
                    if (lspType != WITH_SIGNALLING) {
                        releaseBandwidth(tunnel);
                    }
                }

                /*if (pceStore.getTunnelInfo(tunnel.tunnelId()) != null) {
                    pceStore.removeTunnelInfo(tunnel.tunnelId());
                }*/

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

    @Override
    public List<TunnelId> queryLoadBalancingPath(String pathName) {
        return pceStore.getLoadBalancingTunnelIds(pathName);
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
