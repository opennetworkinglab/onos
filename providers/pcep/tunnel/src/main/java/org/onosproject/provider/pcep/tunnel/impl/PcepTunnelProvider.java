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
package org.onosproject.provider.pcep.tunnel.impl;

import com.google.common.collect.Maps;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.GroupId;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.tunnel.DefaultLabelStack;
import org.onosproject.incubator.net.tunnel.DefaultOpticalTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.DefaultTunnel;
import org.onosproject.incubator.net.tunnel.DefaultTunnelDescription;
import org.onosproject.incubator.net.tunnel.DefaultTunnelStatistics;
import org.onosproject.incubator.net.tunnel.IpTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.OpticalLogicId;
import org.onosproject.incubator.net.tunnel.OpticalTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.Tunnel.State;
import org.onosproject.incubator.net.tunnel.TunnelAdminService;
import org.onosproject.incubator.net.tunnel.TunnelDescription;
import org.onosproject.incubator.net.tunnel.TunnelEndPoint;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelName;
import org.onosproject.incubator.net.tunnel.TunnelProvider;
import org.onosproject.incubator.net.tunnel.TunnelProviderRegistry;
import org.onosproject.incubator.net.tunnel.TunnelProviderService;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.incubator.net.tunnel.TunnelStatistics;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultAnnotations.Builder;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.IpElementId;
import org.onosproject.net.Link;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.pcep.api.PcepController;
import org.onosproject.pcep.api.PcepDpid;
import org.onosproject.pcep.api.PcepHopNodeDescription;
import org.onosproject.pcep.api.PcepOperator.OperationType;
import org.onosproject.pcep.api.PcepTunnel;
import org.onosproject.pcep.api.PcepTunnel.PathState;
import org.onosproject.pcep.api.PcepTunnel.PathType;
import org.onosproject.pcep.api.PcepTunnelListener;
import org.onosproject.pcep.api.PcepTunnelStatistics;
import org.onosproject.pcep.controller.LspKey;
import org.onosproject.pcep.controller.LspType;
import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcep.controller.PcepClient;
import org.onosproject.pcep.controller.PcepClientController;
import org.onosproject.pcep.controller.PcepClientListener;
import org.onosproject.pcep.controller.PcepEventListener;
import org.onosproject.pcep.controller.PcepLspStatus;
import org.onosproject.pcep.controller.PcepLspSyncAction;
import org.onosproject.pcep.controller.SrpIdGenerators;
import org.onosproject.pcep.controller.PcepSyncStatus;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcInitiatedLspRequest;
import org.onosproject.pcepio.protocol.PcepAttribute;
import org.onosproject.pcepio.protocol.PcepBandwidthObject;
import org.onosproject.pcepio.protocol.PcepEndPointsObject;
import org.onosproject.pcepio.protocol.PcepEroObject;
import org.onosproject.pcepio.protocol.PcepInitiateMsg;
import org.onosproject.pcepio.protocol.PcepLspObject;
import org.onosproject.pcepio.protocol.PcepMessage;
import org.onosproject.pcepio.protocol.PcepMetricObject;
import org.onosproject.pcepio.protocol.PcepMsgPath;
import org.onosproject.pcepio.protocol.PcepReportMsg;
import org.onosproject.pcepio.protocol.PcepSrpObject;
import org.onosproject.pcepio.protocol.PcepStateReport;
import org.onosproject.pcepio.protocol.PcepUpdateMsg;
import org.onosproject.pcepio.protocol.PcepUpdateRequest;
import org.onosproject.pcepio.types.IPv4SubObject;
import org.onosproject.pcepio.types.PathSetupTypeTlv;
import org.onosproject.pcepio.types.PcepNaiIpv4Adjacency;
import org.onosproject.pcepio.types.PcepValueType;
import org.onosproject.pcepio.types.SrEroSubObject;
import org.onosproject.pcepio.types.StatefulIPv4LspIdentifiersTlv;
import org.onosproject.pcepio.types.SymbolicPathNameTlv;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.onosproject.incubator.net.tunnel.Tunnel.State.INIT;
import static org.onosproject.incubator.net.tunnel.Tunnel.Type.MPLS;
import static org.onosproject.net.DefaultAnnotations.EMPTY;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.pcep.api.PcepDpid.uri;
import static org.onosproject.pcep.controller.LspType.WITH_SIGNALLING;
import static org.onosproject.pcep.controller.LspType.SR_WITHOUT_SIGNALLING;
import static org.onosproject.pcep.controller.LspType.WITHOUT_SIGNALLING_AND_WITHOUT_SR;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.BANDWIDTH;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.LOCAL_LSP_ID;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.LSP_SIG_TYPE;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.PCC_TUNNEL_ID;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.PCE_INIT;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.PLSP_ID;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.DELEGATE;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.COST_TYPE;
import static org.onosproject.provider.pcep.tunnel.impl.RequestType.CREATE;
import static org.onosproject.provider.pcep.tunnel.impl.RequestType.DELETE;
import static org.onosproject.provider.pcep.tunnel.impl.RequestType.LSP_STATE_RPT;
import static org.onosproject.provider.pcep.tunnel.impl.RequestType.UPDATE;
import static org.onosproject.incubator.net.tunnel.Tunnel.State.UNSTABLE;
import static org.onosproject.pcep.controller.PcepLspSyncAction.REMOVE;
import static org.onosproject.pcep.controller.PcepLspSyncAction.SEND_UPDATE;
import static org.onosproject.pcepio.protocol.ver1.PcepMetricObjectVer1.IGP_METRIC;
import static org.onosproject.pcepio.protocol.ver1.PcepMetricObjectVer1.TE_METRIC;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses an PCEP controller to detect, update, create network
 * tunnels.
 */
@Component(immediate = true)
@Service
public class PcepTunnelProvider extends AbstractProvider implements TunnelProvider {

    private static final Logger log = getLogger(PcepTunnelProvider.class);
    private static final long MAX_BANDWIDTH = 99999744;
    private static final long MIN_BANDWIDTH = 64;
    private static final String BANDWIDTH_UINT = "kbps";
    static final String PROVIDER_ID = "org.onosproject.provider.tunnel.pcep";
    public static final long IDENTIFIER_SET = 0x100000000L;
    public static final long SET = 0xFFFFFFFFL;
    private static final int DELAY = 2;
    private static final int WAIT_TIME = 5;
    public static final String LSRID = "lsrId";

    static final int POLL_INTERVAL = 10;
    @Property(name = "tunnelStatsPollFrequency", intValue = POLL_INTERVAL,
            label = "Frequency (in seconds) for polling tunnel statistics")
    private int tunnelStatsPollFrequency = POLL_INTERVAL;

    private static final String TUNNLE_NOT_NULL = "Create failed,The given port may be wrong or has been occupied.";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TunnelProviderRegistry tunnelProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PcepController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PcepClientController pcepClientController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TunnelService tunnelService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TunnelAdminService tunnelAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    TunnelProviderService service;

    HashMap<String, TunnelId> tunnelMap = new HashMap<String, TunnelId>();
    HashMap<TunnelId, TunnelStatistics> tunnelStatisticsMap = new HashMap<>();
    private HashMap<String, TunnelStatsCollector> collectors = Maps.newHashMap();

    private InnerTunnelProvider listener = new InnerTunnelProvider();

    protected PcepTunnelApiMapper pcepTunnelApiMapper = new PcepTunnelApiMapper();
    private static final int DEFAULT_BANDWIDTH_VALUE = 10;

    /**
     * Creates a Tunnel provider.
     */
    public PcepTunnelProvider() {
        super(new ProviderId("pcep", PROVIDER_ID));
    }

    @Activate
    public void activate() {
        cfgService.registerProperties(getClass());
        service = tunnelProviderRegistry.register(this);
        controller.addTunnelListener(listener);
        pcepClientController.addListener(listener);
        pcepClientController.addEventListener(listener);
        tunnelService.queryAllTunnels().forEach(tunnel -> {
            String pcepTunnelId = getPcepTunnelKey(tunnel.tunnelId());
            TunnelStatsCollector tsc = new TunnelStatsCollector(pcepTunnelId, tunnelStatsPollFrequency);
            tsc.start();
            collectors.put(tunnel.tunnelId().id(), tsc);

        });

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        tunnelProviderRegistry.unregister(this);
        controller.removeTunnelListener(listener);
        collectors.values().forEach(TunnelStatsCollector::stop);
        pcepClientController.removeListener(listener);
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        int newTunnelStatsPollFrequency;
        try {
            String s = get(properties, "tunnelStatsPollFrequency");
            newTunnelStatsPollFrequency = isNullOrEmpty(s) ? tunnelStatsPollFrequency : Integer.parseInt(s.trim());

        } catch (NumberFormatException | ClassCastException e) {
            newTunnelStatsPollFrequency = tunnelStatsPollFrequency;
        }

        if (newTunnelStatsPollFrequency != tunnelStatsPollFrequency) {
            tunnelStatsPollFrequency = newTunnelStatsPollFrequency;
            collectors.values().forEach(tsc -> tsc.adjustPollInterval(tunnelStatsPollFrequency));
            log.info("New setting: tunnelStatsPollFrequency={}", tunnelStatsPollFrequency);
        }
    }

    @Override
    public void setupTunnel(Tunnel tunnel, Path path) {
        if (tunnel.type() != MPLS) {
            log.error("Tunnel Type MPLS is only supported");
            return;
        }

        // check for tunnel end points
        if (!(tunnel.src() instanceof IpTunnelEndPoint) || !(tunnel.dst() instanceof IpTunnelEndPoint)) {
            log.error("Tunnel source or destination is not valid");
            return;
        }

        // Get the pcc client
        PcepClient pc = pcepClientController.getClient(PccId.pccId(((IpTunnelEndPoint) tunnel.src()).ip()));

        if (!(pc instanceof PcepClient)) {
            log.error("There is no PCC connected with ip addresss {}"
                              + ((IpTunnelEndPoint) tunnel.src()).ip().toString());
            return;
        }

        //If stateful and PC Initiation capability is not supported by client not sending Initiate msg
        //Only master will initiate setup tunnel
        if (pc.capability().pcInstantiationCapability() && mastershipService.isLocalMaster(getDevice(pc.getPccId()))) {
            pcepSetupTunnel(tunnel, path, pc);
        }
    }

    @Override
    public void setupTunnel(ElementId srcElement, Tunnel tunnel, Path path) {

        //TODO: tunnel which is passed doesn't have tunnelID
        if (tunnel.annotations().value(PLSP_ID) != null) {
            if (LspType.valueOf(tunnel.annotations().value(LSP_SIG_TYPE)) != WITHOUT_SIGNALLING_AND_WITHOUT_SR) {
                updateTunnel(tunnel, path);
            } else {
                // Download labels and send update message.
                // To get new tunnel ID (modified tunnel ID)
                Collection<Tunnel> tunnels = tunnelService.queryTunnel(tunnel.src(), tunnel.dst());
                for (Tunnel t : tunnels) {
                    if (t.state().equals(INIT) && t.tunnelName().equals(tunnel.tunnelName())) {
                        tunnel = new DefaultTunnel(tunnel.providerId(), tunnel.src(),
                                tunnel.dst(), tunnel.type(),
                                t.state(), tunnel.groupId(),
                                t.tunnelId(),
                                tunnel.tunnelName(),
                                tunnel.path(),
                                tunnel.resource(),
                                tunnel.annotations());
                                break;
                    }
                }
                if (!pcepClientController.allocateLocalLabel(tunnel)) {
                    log.error("Unable to allocate labels for the tunnel {}.", tunnel.toString());
                }
            }
            return;
        }

        if (tunnel.type() != MPLS) {
            log.error("Tunnel Type MPLS is only supported");
            return;
        }

        // check for tunnel end points
        if (!(tunnel.src() instanceof IpTunnelEndPoint) || !(tunnel.dst() instanceof IpTunnelEndPoint)) {
            log.error("Tunnel source or destination is not valid");
            return;
        }

        PcepClient pc = pcepClientController.getClient(PccId.pccId(((IpTunnelEndPoint) tunnel.src()).ip()));

        if (!(pc instanceof PcepClient)) {
            log.error("There is no PCC connected with this device {}"
                    + srcElement.toString());
            return;
        }

        //If stateful and PC Initiation capability is not supported by client not sending Initiate msg
        //Only master will initiate setup tunnel
        if (pc.capability().pcInstantiationCapability()
                && mastershipService.isLocalMaster(getDevice(pc.getPccId()))) {
            pcepSetupTunnel(tunnel, path, pc);
        }
    }

    @Override
    public void releaseTunnel(Tunnel tunnel) {

        if (tunnel.type() != MPLS) {
            log.error("Tunnel Type MPLS is only supported");
            return;
        }

        // check for tunnel end points
        if (!(tunnel.src() instanceof IpTunnelEndPoint) || !(tunnel.dst() instanceof IpTunnelEndPoint)) {
            log.error("Tunnel source or destination is not valid");
            return;
        }

        PcepClient pc = pcepClientController.getClient(PccId.pccId(((IpTunnelEndPoint) tunnel.src()).ip()));

        if (!(pc instanceof PcepClient)) {
            log.error("There is no PCC connected with ip addresss {}"
                    + ((IpTunnelEndPoint) tunnel.src()).ip().toString());
            return;
        }

        //Only master will release tunnel
        if (pc.capability().pcInstantiationCapability()
                && mastershipService.isLocalMaster(getDevice(pc.getPccId()))) {
            pcepReleaseTunnel(tunnel, pc);
        }
    }

    @Override
    public void releaseTunnel(ElementId srcElement, Tunnel tunnel) {
        if (tunnel.type() != MPLS) {
            log.error("Tunnel Type MPLS is only supported");
            return;
        }

        if (!(srcElement instanceof IpElementId)) {
            log.error("Element id is not valid");
            return;
        }

        // check for tunnel end points
        if (!(tunnel.src() instanceof IpTunnelEndPoint) || !(tunnel.dst() instanceof IpTunnelEndPoint)) {
            log.error("Tunnel source or destination is not valid");
            return;
        }

        PcepClient pc = pcepClientController.getClient(PccId.pccId(((IpElementId) srcElement).ipAddress()));

        if (!(pc instanceof PcepClient)) {
            log.error("There is no PCC connected with ip addresss {}"
                    + ((IpElementId) srcElement).ipAddress().toString());
            return;
        }

        //Only master will release tunnel
        if (pc.capability().pcInstantiationCapability()
                && mastershipService.isLocalMaster(getDevice(pc.getPccId()))) {
            pcepReleaseTunnel(tunnel, pc);
        }
    }

    @Override
    public void updateTunnel(Tunnel tunnel, Path path) {
        if (tunnel.type() != MPLS) {
            log.error("Tunnel Type MPLS is only supported");
            return;
        }

        // check for tunnel end points
        if (!(tunnel.src() instanceof IpTunnelEndPoint) || !(tunnel.dst() instanceof IpTunnelEndPoint)) {
            log.error("Tunnel source or destination is not valid");
            return;
        }

        //To get new tunnel ID (modified tunnel ID)
        Collection<Tunnel> tunnels = tunnelService.queryTunnel(tunnel.src(), tunnel.dst());
        for (Tunnel t : tunnels) {
            if (t.state().equals(INIT) && t.tunnelName().equals(tunnel.tunnelName())) {
                tunnel = new DefaultTunnel(tunnel.providerId(), tunnel.src(),
                        tunnel.dst(), tunnel.type(),
                        t.state(), tunnel.groupId(),
                        t.tunnelId(),
                        tunnel.tunnelName(),
                        tunnel.path(),
                        tunnel.resource(),
                        tunnel.annotations());
                        break;
            }
        }

        PcepClient pc = pcepClientController.getClient(PccId.pccId(((IpTunnelEndPoint) tunnel.src()).ip()));

        if (!(pc instanceof PcepClient)) {
            log.error("There is no PCC connected with ip addresss {}"
                    + ((IpTunnelEndPoint) tunnel.src()).ip().toString());
            return;
        }

        //PCInitiate tunnels are always have D flag set, else check for tunnels who are delegated via LspKey
        if (pc.capability().statefulPceCapability()) {
            if (tunnel.annotations().value(PCE_INIT) != null && tunnel.annotations().value(PCE_INIT).equals("true")) {
                pcepUpdateTunnel(tunnel, path, pc);
            } else {
                // If delegation flag is set then only send update message[means delegated PCE can send update msg for
                // that LSP. If annotation is null D flag is not set else it is set.
                Short localLspId = 0;
                for (Tunnel t : tunnels) {
                    if (!t.tunnelId().equals(tunnel.tunnelId()) && t.tunnelName().equals(tunnel.tunnelName())) {
                        localLspId = Short.valueOf(t.annotations().value(LOCAL_LSP_ID));
                    }
                }

                if (localLspId == 0) {
                    log.error("Local LSP ID for old tunnel not found");
                    return;
                }

                if (pc.delegationInfo(new LspKey(Integer.valueOf(tunnel.annotations().value(PLSP_ID)),
                                                 localLspId.shortValue())) != null) {

                    pcepUpdateTunnel(tunnel, path, pc);
                }
            }
        }
    }

    @Override
    public void updateTunnel(ElementId srcElement, Tunnel tunnel, Path path) {

        if (tunnel.type() != MPLS) {
            log.error("Tunnel Type MPLS is only supported");
            return;
        }

        if (!(srcElement instanceof IpElementId)) {
            log.error("Element id is not valid");
            return;
        }

        // check for tunnel end points
        if (!(tunnel.src() instanceof IpTunnelEndPoint) || !(tunnel.dst() instanceof IpTunnelEndPoint)) {
            log.error("Tunnel source or destination is not valid");
            return;
        }

        PcepClient pc = pcepClientController.getClient(PccId.pccId(((IpElementId) srcElement).ipAddress()));

        if (!(pc instanceof PcepClient)) {
            log.error("There is no PCC connected with ip addresss {}"
                    + ((IpElementId) srcElement).ipAddress().toString());
            return;
        }

        // If delegation flag is set then only send update message[means delegated PCE can send update msg for that
        // LSP].If annotation is null D flag is not set else it is set.
        if (pc.capability().statefulPceCapability()
                && pc.delegationInfo(
                        new LspKey(Integer.valueOf(tunnel.annotations().value(PLSP_ID)), Short.valueOf(tunnel
                                .annotations().value(LOCAL_LSP_ID)))) != null) {
            pcepUpdateTunnel(tunnel, path, pc);
        }
    }

    @Override
    public TunnelId tunnelAdded(TunnelDescription tunnel) {
        return handleTunnelAdded(tunnel, null);
    }

    public TunnelId tunnelAdded(TunnelDescription tunnel, State tunnelState) {
        return handleTunnelAdded(tunnel, tunnelState);
    }

    private TunnelId handleTunnelAdded(TunnelDescription tunnel, State tunnelState) {

        if (tunnel.type() == MPLS) {
            pcepTunnelApiMapper.removeFromCoreTunnelRequestQueue(tunnel.id());

            if (tunnelState == null) {
                return service.tunnelAdded(tunnel);
            } else {
                return service.tunnelAdded(tunnel, tunnelState);
            }
        }

        long bandwidth = Long.parseLong(tunnel.annotations().value(BANDWIDTH));

        if (bandwidth < MIN_BANDWIDTH || bandwidth > MAX_BANDWIDTH) {
            error("Update failed, invalid bandwidth.");
            return null;
        }

        // endpoints
        OpticalTunnelEndPoint src = (org.onosproject.incubator.net.tunnel.OpticalTunnelEndPoint) tunnel
                .src();
        OpticalTunnelEndPoint dst = (OpticalTunnelEndPoint) tunnel.dst();
        // devices
        DeviceId srcId = (DeviceId) src.elementId().get();
        DeviceId dstId = (DeviceId) dst.elementId().get();

        // ports
        long srcPort = src.portNumber().get().toLong();
        long dstPort = dst.portNumber().get().toLong();

        // type
        if (tunnel.type() != Tunnel.Type.VLAN) {
            error("Illegal tunnel type. Only support VLAN tunnel creation.");
            return null;
        }

        PcepTunnel pcepTunnel = controller.applyTunnel(srcId, dstId, srcPort,
                                                       dstPort, bandwidth,
                                                       tunnel.tunnelName()
                                                       .value());

        checkNotNull(pcepTunnel, TUNNLE_NOT_NULL);
        TunnelDescription tunnelAdded = buildOpticalTunnel(pcepTunnel, null);
        TunnelId tunnelId = service.tunnelAdded(tunnelAdded);

        tunnelMap.put(String.valueOf(pcepTunnel.id()), tunnelId);
        return tunnelId;
    }

    private void tunnelUpdated(Tunnel tunnel, Path path, State tunnelState) {
        handleTunnelUpdate(tunnel, path, tunnelState);
    }

    //Handles tunnel updated using tunnel admin service[specially to update annotations].
    private void handleTunnelUpdate(Tunnel tunnel, Path path, State tunnelState) {

        if (tunnel.type() == MPLS) {
            pcepTunnelApiMapper.removeFromCoreTunnelRequestQueue(tunnel.tunnelId());

            TunnelDescription td = new DefaultTunnelDescription(tunnel.tunnelId(), tunnel.src(), tunnel.dst(),
                    tunnel.type(), tunnel.groupId(), tunnel.providerId(),
                    tunnel.tunnelName(), path, tunnel.resource(),
                    (SparseAnnotations) tunnel.annotations());

            service.tunnelUpdated(td, tunnelState);
            return;
        }

        Tunnel tunnelOld = tunnelQueryById(tunnel.tunnelId());
        if (tunnelOld.type() != Tunnel.Type.VLAN) {
            error("Illegal tunnel type. Only support VLAN tunnel update.");
            return;
        }

        long bandwidth = Long
                .parseLong(tunnel.annotations().value("bandwidth"));
        if (bandwidth < MIN_BANDWIDTH || bandwidth > MAX_BANDWIDTH) {
            error("Update failed, invalid bandwidth.");
            return;
        }
        String pcepTunnelId = getPcepTunnelKey(tunnel.tunnelId());

        checkNotNull(pcepTunnelId, "Invalid tunnel id");
        if (!controller.updateTunnelBandwidth(pcepTunnelId, bandwidth)) {
            error("Update failed,maybe invalid bandwidth.");
            return;
        }
        tunnelAdminService.updateTunnel(tunnel, path);
    }

    @Override
    public void tunnelRemoved(TunnelDescription tunnel) {
        if (tunnel.type() == MPLS) {
            pcepTunnelApiMapper.removeFromCoreTunnelRequestQueue(tunnel.id());
            service.tunnelRemoved(tunnel);
            return;
        }

        Tunnel tunnelOld = tunnelQueryById(tunnel.id());
        checkNotNull(tunnelOld, "The tunnel id is not exsited.");
        if (tunnelOld.type() != Tunnel.Type.VLAN) {
            error("Illegal tunnel type. Only support VLAN tunnel deletion.");
            return;
        }
        String pcepTunnelId = getPcepTunnelKey(tunnel.id());
        checkNotNull(pcepTunnelId, "The tunnel id is not exsited.");
        if (!controller.deleteTunnel(pcepTunnelId)) {
            error("Delete tunnel failed, Maybe some devices have been disconnected.");
            return;
        }
        tunnelMap.remove(pcepTunnelId);
        service.tunnelRemoved(tunnel);
    }

    @Override
    public void tunnelUpdated(TunnelDescription tunnel) {
        handleTunnelUpdate(tunnel, null);
    }

    public void tunnelUpdated(TunnelDescription tunnel, State tunnelState) {
        handleTunnelUpdate(tunnel, tunnelState);
    }

    private void handleTunnelUpdate(TunnelDescription tunnel, State tunnelState) {
        if (tunnel.type() == MPLS) {
            pcepTunnelApiMapper.removeFromCoreTunnelRequestQueue(tunnel.id());

            if (tunnelState == null) {
                service.tunnelUpdated(tunnel);
            } else {
                service.tunnelUpdated(tunnel, tunnelState);
            }
            return;
        }

        Tunnel tunnelOld = tunnelQueryById(tunnel.id());
        if (tunnelOld.type() != Tunnel.Type.VLAN) {
            error("Illegal tunnel type. Only support VLAN tunnel update.");
            return;
        }
        long bandwidth = Long
                .parseLong(tunnel.annotations().value("bandwidth"));
        if (bandwidth < MIN_BANDWIDTH || bandwidth > MAX_BANDWIDTH) {
            error("Update failed, invalid bandwidth.");
            return;
        }
        String pcepTunnelId = getPcepTunnelKey(tunnel.id());

        checkNotNull(pcepTunnelId, "Invalid tunnel id");
        if (!controller.updateTunnelBandwidth(pcepTunnelId, bandwidth)) {

            error("Update failed,maybe invalid bandwidth.");
            return;

        }
        service.tunnelUpdated(tunnel);
    }

    private void error(String info) {
        System.err.println(info);
    }

    // Short-hand for creating a connection point.
    private ConnectPoint connectPoint(PcepDpid id, long port) {
        return new ConnectPoint(deviceId(uri(id)), portNumber(port));
    }

    // Short-hand for creating a link.
    private Link link(PcepDpid src, long sp, PcepDpid dst, long dp) {
        return DefaultLink.builder()
                .providerId(id())
                .src(connectPoint(src, sp))
                .dst(connectPoint(dst, dp))
                .type(Link.Type.TUNNEL)
                .build();
    }

    // Creates a path that leads through the given devices.
    private Path createPath(List<PcepHopNodeDescription> hopList,
                            PathType pathtype, PathState pathState) {
        if (hopList == null || hopList.isEmpty()) {
            return null;
        }
        List<Link> links = new ArrayList<>();
        for (int i = 1; i < hopList.size() - 1; i = i + 2) {
            links.add(link(hopList.get(i).getDeviceId(), hopList.get(i)
                    .getPortNum(), hopList.get(i + 1).getDeviceId(), hopList
                    .get(i + 1).getPortNum()));
        }

        int hopNum = hopList.size() - 2;
        DefaultAnnotations extendAnnotations = DefaultAnnotations.builder()
                .set("pathNum", String.valueOf(hopNum))
                .set("pathState", String.valueOf(pathState))
                .set("pathType", String.valueOf(pathtype)).build();
        return new DefaultPath(id(), links, hopNum, extendAnnotations);
    }

    // convert the path description to a string.
    public String pathToString(List<Link> links) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        for (Link link : links) {
            builder.append("(Device:" + link.src().deviceId() + "  Port:"
                    + link.src().port().toLong());
            builder.append(" Device:" + link.dst().deviceId() + "  Port:"
                    + link.dst().port().toLong());
            builder.append(")");
        }
        builder.append("}");
        return builder.toString();
    }

    // build a TunnelDescription.
    private TunnelDescription buildOpticalTunnel(PcepTunnel pcepTunnel,
                                                 TunnelId tunnelId) {
        TunnelEndPoint srcPoint = null;
        TunnelEndPoint dstPoint = null;
        Tunnel.Type tunnelType = null;
        TunnelName name = TunnelName.tunnelName(pcepTunnel.name());

        // add path after codes of tunnel's path merged
        Path path = createPath(pcepTunnel.getHopList(),
                               pcepTunnel.getPathType(),
                               pcepTunnel.getPathState());

        OpticalTunnelEndPoint.Type endPointType = null;
        switch (pcepTunnel.type()) {
        case OCH:
            tunnelType = Tunnel.Type.OCH;
            endPointType = OpticalTunnelEndPoint.Type.LAMBDA;
            break;

        case OTN:
            tunnelType = Tunnel.Type.ODUK;
            endPointType = OpticalTunnelEndPoint.Type.TIMESLOT;
            break;

        case UNI:
            tunnelType = Tunnel.Type.VLAN;
            endPointType = null;
            break;

        default:
            break;
        }
        DeviceId srcDid = deviceId(uri(pcepTunnel.srcDeviceID()));
        DeviceId dstDid = deviceId(uri(pcepTunnel.dstDeviceId()));
        PortNumber srcPort = PortNumber.portNumber(pcepTunnel.srcPort());
        PortNumber dstPort = PortNumber.portNumber(pcepTunnel.dstPort());

        srcPoint = new DefaultOpticalTunnelEndPoint(id(), Optional.of(srcDid),
                                                    Optional.of(srcPort), null,
                                                    endPointType,
                                                    OpticalLogicId.logicId(0),
                                                    true);
        dstPoint = new DefaultOpticalTunnelEndPoint(id(), Optional.of(dstDid),
                                                    Optional.of(dstPort), null,
                                                    endPointType,
                                                    OpticalLogicId.logicId(0),
                                                    true);

        // basic annotations
        DefaultAnnotations annotations = DefaultAnnotations
                .builder()
                .set("SLA", String.valueOf(pcepTunnel.getSla()))
                .set("bandwidth",
                     String.valueOf(pcepTunnel.bandWidth()) + BANDWIDTH_UINT)
                .set("index", String.valueOf(pcepTunnel.id())).build();

        // a VLAN tunnel always carry OCH tunnel, this annotation is the index
        // of a OCH tunnel.
        if (pcepTunnel.underlayTunnelId() != 0) {
            DefaultAnnotations extendAnnotations = DefaultAnnotations
                    .builder()
                    .set("underLayTunnelIndex",
                         String.valueOf(pcepTunnel.underlayTunnelId())).build();
            annotations = DefaultAnnotations.merge(annotations,
                                                   extendAnnotations);

        }
        TunnelDescription tunnel = new DefaultTunnelDescription(tunnelId,
                                                                srcPoint,
                                                                dstPoint,
                                                                tunnelType,
                                                                new GroupId(0),
                                                                id(), name,
                                                                path,
                                                                annotations);
        return tunnel;
    }

    /**
     * Get the tunnelID according to the tunnel key.
     *
     * @param tunnelKey tunnel key
     * @return corresponding tunnel id of the a tunnel key.
     */
    private TunnelId getTunnelId(String tunnelKey) {
        for (String key : tunnelMap.keySet()) {
            if (key.equals(tunnelKey)) {
                return tunnelMap.get(key);
            }
        }
        return null;
    }

    /**
     * Get the tunnel key according to the tunnelID.
     *
     * @param tunnelId tunnel id
     * @return corresponding a tunnel key of the tunnel id.
     */
    private String getPcepTunnelKey(TunnelId tunnelId) {
        for (String key : tunnelMap.keySet()) {
            if (Objects.equals(tunnelMap.get(key).id(), tunnelId.id())) {
                return key;
            }
        }
        return null;

    }

    /**
     * Build a DefaultTunnelStatistics from a PcepTunnelStatistics.
     *
     * @param statistics statistics data from a PCEP tunnel
     * @return TunnelStatistics
     */
    private TunnelStatistics buildTunnelStatistics(PcepTunnelStatistics statistics) {
        DefaultTunnelStatistics.Builder builder = new DefaultTunnelStatistics.Builder();
        DefaultTunnelStatistics tunnelStatistics =  builder.setBwUtilization(statistics.bandwidthUtilization())
                    .setPacketLossRatio(statistics.packetLossRate())
                    .setFlowDelay(statistics.flowDelay())
                    .setAlarms(statistics.alarms())
                .build();
        return tunnelStatistics;
   }
    /**
     * Creates list of hops for ERO object from Path.
     *
     * @param path network path
     * @return list of ERO subobjects
     */
    private LinkedList<PcepValueType> createPcepPath(Path path) {
        LinkedList<PcepValueType> llSubObjects = new LinkedList<PcepValueType>();
        List<Link> listLink = path.links();
        ConnectPoint source = null;
        ConnectPoint destination = null;
        IpAddress ipDstAddress = null;
        IpAddress ipSrcAddress = null;
        PcepValueType subObj = null;
        long portNo;

        for (Link link : listLink) {
            source = link.src();
            if (!(source.equals(destination))) {
                //set IPv4SubObject for ERO object
                portNo = source.port().toLong();
                portNo = ((portNo & IDENTIFIER_SET) == IDENTIFIER_SET) ? portNo & SET : portNo;
                ipSrcAddress = Ip4Address.valueOf((int) portNo);
                subObj = new IPv4SubObject(ipSrcAddress.getIp4Address().toInt());
                llSubObjects.add(subObj);
            }

            destination = link.dst();
            portNo = destination.port().toLong();
            portNo = ((portNo & IDENTIFIER_SET) == IDENTIFIER_SET) ? portNo & SET : portNo;
            ipDstAddress = Ip4Address.valueOf((int) portNo);
            subObj = new IPv4SubObject(ipDstAddress.getIp4Address().toInt());
            llSubObjects.add(subObj);
        }

        return llSubObjects;
    }

    /**
     * Creates PcInitiated lsp request list for setup tunnel.
     *
     * @param tunnel mpls tunnel
     * @param path network path
     * @param pc pcep client
     * @param srpId unique id for pcep message
     * @return list of PcInitiatedLspRequest
     * @throws PcepParseException while building pcep objects fails
     */
    LinkedList<PcInitiatedLspRequest> createPcInitiatedLspReqList(Tunnel tunnel, Path path,
                                                                  PcepClient pc, int srpId)
                                                                          throws PcepParseException {
        PcepValueType tlv;
        LinkedList<PcepValueType> llSubObjects = null;
        LspType lspType = LspType.valueOf(tunnel.annotations().value(LSP_SIG_TYPE));

        if (lspType == SR_WITHOUT_SIGNALLING) {
            NetworkResource labelStack = tunnel.resource();
            if (labelStack == null || !(labelStack instanceof DefaultLabelStack)) {
                labelStack = pcepClientController.computeLabelStack(tunnel.path());
                if (labelStack == null) {
                    log.error("Unable to create label stack.");
                    return null;
                }
            }
            llSubObjects = pcepClientController.createPcepLabelStack((DefaultLabelStack) labelStack, path);
        } else {
            llSubObjects = createPcepPath(path);
        }

        if (llSubObjects == null || llSubObjects.isEmpty()) {
            log.error("There is no link information to create tunnel");
            return null;
        }

        LinkedList<PcepValueType> llOptionalTlv = new LinkedList<PcepValueType>();

        // set PathSetupTypeTlv of SRP object
        tlv = new PathSetupTypeTlv(lspType.type());
        llOptionalTlv.add(tlv);

        // build SRP object
        PcepSrpObject srpobj = pc.factory().buildSrpObject().setSrpID(srpId).setRFlag(false)
                .setOptionalTlv(llOptionalTlv).build();

        llOptionalTlv = new LinkedList<PcepValueType>();
        LinkedList<PcInitiatedLspRequest> llPcInitiatedLspRequestList = new LinkedList<PcInitiatedLspRequest>();

        // set LSP identifiers TLV
        short localLspId = 0;
        if (LspType.valueOf(tunnel.annotations().value(LSP_SIG_TYPE)) != WITH_SIGNALLING) {
            String localLspIdString = tunnel.annotations().value(LOCAL_LSP_ID);
            if (localLspIdString != null) {
                localLspId = Short.valueOf(localLspIdString);
            }
        }

        tunnel.annotations().value(LSP_SIG_TYPE);
        tlv = new StatefulIPv4LspIdentifiersTlv((((IpTunnelEndPoint) tunnel.src()).ip().getIp4Address().toInt()),
                                                localLspId, (short) 0, 0, (((IpTunnelEndPoint) tunnel.dst()).ip()
                                                        .getIp4Address().toInt()));
        llOptionalTlv.add(tlv);
        //set SymbolicPathNameTlv of LSP object
        tlv = new SymbolicPathNameTlv(tunnel.tunnelName().value().getBytes());
        llOptionalTlv.add(tlv);

        //build LSP object
        PcepLspObject lspobj = pc.factory().buildLspObject().setAFlag(true).setDFlag(true).setOFlag((byte) 0)
                .setPlspId(0).setOptionalTlv(llOptionalTlv).build();

        //build ENDPOINTS object
        PcepEndPointsObject endpointsobj = pc.factory().buildEndPointsObject()
                .setSourceIpAddress(((IpTunnelEndPoint) tunnel.src()).ip().getIp4Address().toInt())
                .setDestIpAddress(((IpTunnelEndPoint) tunnel.dst()).ip().getIp4Address().toInt())
                .setPFlag(true).build();

        //build ERO object
        PcepEroObject eroobj = pc.factory().buildEroObject().setSubObjects(llSubObjects).build();

        float  iBandwidth = DEFAULT_BANDWIDTH_VALUE;
        if (tunnel.annotations().value(BANDWIDTH) != null) {
            iBandwidth = Float.valueOf(tunnel.annotations().value(BANDWIDTH));
        }
        // build bandwidth object
        PcepBandwidthObject bandwidthObject = pc.factory().buildBandwidthObject().setBandwidth(iBandwidth).build();
        // build pcep attribute
        PcepAttribute pcepAttribute = pc.factory().buildPcepAttribute().setBandwidthObject(bandwidthObject).build();

        PcInitiatedLspRequest initiateLspRequest = pc.factory().buildPcInitiatedLspRequest().setSrpObject(srpobj)
                .setLspObject(lspobj).setEndPointsObject(endpointsobj).setEroObject(eroobj)
                .setPcepAttribute(pcepAttribute).build();
        llPcInitiatedLspRequestList.add(initiateLspRequest);
        return llPcInitiatedLspRequestList;
    }

    /**
     * To send initiate tunnel message to pcc.
     *
     * @param tunnel mpls tunnel info
     * @param path explicit route for the tunnel
     * @param pc pcep client to send message
     */
    private void pcepSetupTunnel(Tunnel tunnel, Path path, PcepClient pc) {
        try {
            if (!(pc.lspDbSyncStatus().equals(PcepSyncStatus.SYNCED))) {
                log.error("Setup tunnel has failed as LSP DB sync is not finished");
                return;
            }

            int srpId = SrpIdGenerators.create();
            Collection<Tunnel> tunnels = tunnelService.queryTunnel(tunnel.src(), tunnel.dst());
            for (Tunnel t : tunnels) {
                if (t.tunnelName().equals(tunnel.tunnelName())) {
                    tunnel = new DefaultTunnel(tunnel.providerId(), tunnel.src(),
                            tunnel.dst(), tunnel.type(),
                            t.state(), tunnel.groupId(),
                            t.tunnelId(),
                            tunnel.tunnelName(),
                            tunnel.path(),
                            tunnel.resource(),
                            tunnel.annotations());
                            break;
                }
            }

            if (tunnel.tunnelId() == null) {
                log.error("Tunnel ID not found");
                return;
            }

            PcepTunnelData pcepTunnelData = new PcepTunnelData(tunnel, path, CREATE);

            pcepTunnelApiMapper.addToCoreTunnelRequestQueue(pcepTunnelData);

            LinkedList<PcInitiatedLspRequest> llPcInitiatedLspRequestList = createPcInitiatedLspReqList(tunnel, path,
                                                                                                        pc, srpId);
            if (llPcInitiatedLspRequestList == null || llPcInitiatedLspRequestList.isEmpty()) {
                log.error("Failed to create PcInitiatedLspRequestList");
                return;
            }

            //build PCInitiate message
            PcepInitiateMsg pcInitiateMsg = pc.factory().buildPcepInitiateMsg()
                    .setPcInitiatedLspRequestList(llPcInitiatedLspRequestList)
                    .build();

            pc.sendMessage(Collections.singletonList(pcInitiateMsg));

            pcepTunnelApiMapper.addToTunnelRequestQueue(srpId, pcepTunnelData);
        } catch (PcepParseException e) {
            log.error("PcepParseException occurred while processing setup tunnel {}", e.getMessage());
        }
    }

    /**
     * To send Release tunnel message to pcc.
     *
     * @param tunnel mpls tunnel info
     * @param pc pcep client to send message
     */
    private void pcepReleaseTunnel(Tunnel tunnel, PcepClient pc) {
        try {
            if (!(pc.lspDbSyncStatus().equals(PcepSyncStatus.SYNCED))) {
                log.error("Release tunnel has failed as LSP DB sync is not finished");
                return;
            }

            PcepTunnelData pcepTunnelData = new PcepTunnelData(tunnel, DELETE);
            pcepTunnelApiMapper.addToCoreTunnelRequestQueue(pcepTunnelData);
            int srpId = SrpIdGenerators.create();

            PcepValueType tlv;
            LinkedList<PcepValueType> llOptionalTlv = new LinkedList<PcepValueType>();

            // set PathSetupTypeTlv of SRP object
            tlv = new PathSetupTypeTlv(LspType.valueOf(tunnel.annotations().value(LSP_SIG_TYPE))
                    .type());
            llOptionalTlv.add(tlv);

            // build SRP object
            PcepSrpObject srpobj = pc.factory().buildSrpObject().setSrpID(srpId).setRFlag(true)
                    .setOptionalTlv(llOptionalTlv).build();

            llOptionalTlv = new LinkedList<PcepValueType>();
            LinkedList<PcInitiatedLspRequest> llPcInitiatedLspRequestList = new LinkedList<PcInitiatedLspRequest>();

            tlv = new SymbolicPathNameTlv(tunnel.tunnelName().value().getBytes());
            llOptionalTlv.add(tlv);

            String localLspIdString = tunnel.annotations().value(LOCAL_LSP_ID);
            String pccTunnelIdString = tunnel.annotations().value(PCC_TUNNEL_ID);
            String pLspIdString = tunnel.annotations().value(PLSP_ID);

            short localLspId = 0;
            short pccTunnelId = 0;
            int plspId = 0;

            if (localLspIdString != null) {
                localLspId = Short.valueOf(localLspIdString);
            }

            if (pccTunnelIdString != null) {
                pccTunnelId = Short.valueOf(pccTunnelIdString);
            }

            if (pLspIdString != null) {
                plspId = Integer.valueOf(pLspIdString);
            }

            tlv = new StatefulIPv4LspIdentifiersTlv((((IpTunnelEndPoint) tunnel.src())
                    .ip().getIp4Address().toInt()),
                    localLspId, pccTunnelId, 0, (((IpTunnelEndPoint) tunnel.dst()).ip()
                    .getIp4Address().toInt()));
            llOptionalTlv.add(tlv);

            // build lsp object, set r flag as false to delete the tunnel
            PcepLspObject lspobj = pc.factory().buildLspObject().setRFlag(false).setPlspId(plspId)
                    .setOptionalTlv(llOptionalTlv).build();

            PcInitiatedLspRequest releaseLspRequest = pc.factory().buildPcInitiatedLspRequest().setSrpObject(srpobj)
                    .setLspObject(lspobj).build();

            llPcInitiatedLspRequestList.add(releaseLspRequest);

            PcepInitiateMsg pcInitiateMsg = pc.factory().buildPcepInitiateMsg()
                    .setPcInitiatedLspRequestList(llPcInitiatedLspRequestList).build();

            pc.sendMessage(Collections.singletonList(pcInitiateMsg));

            pcepTunnelApiMapper.addToTunnelRequestQueue(srpId, pcepTunnelData);
        } catch (PcepParseException e) {
            log.error("PcepParseException occurred while processing release tunnel {}", e.getMessage());
        }
    }

    /**
     * To send Update tunnel request message to pcc.
     *
     * @param tunnel mpls tunnel info
     * @param path explicit route for the tunnel
     * @param pc pcep client to send message
     */
    private void pcepUpdateTunnel(Tunnel tunnel, Path path, PcepClient pc) {
        try {
            PcepTunnelData pcepTunnelData = new PcepTunnelData(tunnel, path, UPDATE);
            pcepTunnelApiMapper.addToCoreTunnelRequestQueue(pcepTunnelData);
            int srpId = SrpIdGenerators.create();
            PcepValueType tlv;

            LinkedList<PcepValueType> llSubObjects = null;
            LspType lspSigType = LspType.valueOf(tunnel.annotations().value(LSP_SIG_TYPE));

            if (lspSigType == SR_WITHOUT_SIGNALLING) {
                NetworkResource labelStack = tunnel.resource();
                if (labelStack == null || !(labelStack instanceof DefaultLabelStack)) {
                    labelStack = pcepClientController.computeLabelStack(tunnel.path());
                    if (labelStack == null) {
                        log.error("Unable to create label stack.");
                        return;
                    }
                }
                llSubObjects = pcepClientController.createPcepLabelStack((DefaultLabelStack) labelStack, path);
            } else {
                llSubObjects = createPcepPath(path);
            }

            LinkedList<PcepValueType> llOptionalTlv = new LinkedList<PcepValueType>();
            LinkedList<PcepUpdateRequest> llUpdateRequestList = new LinkedList<PcepUpdateRequest>();

            // set PathSetupTypeTlv of SRP object
            tlv = new PathSetupTypeTlv(lspSigType.type());
            llOptionalTlv.add(tlv);

            // build SRP object
            PcepSrpObject srpobj = pc.factory().buildSrpObject().setSrpID(srpId).setRFlag(false)
                    .setOptionalTlv(llOptionalTlv).build();

            llOptionalTlv = new LinkedList<PcepValueType>();

            // Lsp Identifier tlv is required for all modes of lsp
            String localLspIdString = tunnel.annotations().value(LOCAL_LSP_ID);
            String pccTunnelIdString = tunnel.annotations().value(PCC_TUNNEL_ID);
            short localLspId = 0;
            short pccTunnelId = 0;

            if (localLspIdString != null) {
                localLspId = Short.valueOf(localLspIdString);
            }

            if (pccTunnelIdString != null) {
                pccTunnelId = Short.valueOf(pccTunnelIdString);
            }

            tlv = new StatefulIPv4LspIdentifiersTlv((((IpTunnelEndPoint) tunnel.src())
                    .ip().getIp4Address().toInt()),
                    localLspId, pccTunnelId,
                    ((IpTunnelEndPoint) tunnel.src()).ip().getIp4Address().toInt(),
                    (((IpTunnelEndPoint) tunnel.dst()).ip().getIp4Address().toInt()));
            llOptionalTlv.add(tlv);

            if (tunnel.tunnelName().value() != null) {
                tlv = new SymbolicPathNameTlv(tunnel.tunnelName().value().getBytes());
                llOptionalTlv.add(tlv);
            }
            boolean delegated = (tunnel.annotations().value(DELEGATE) == null) ? false
                                                                               : Boolean.valueOf(tunnel.annotations()
                                                                                       .value(DELEGATE));
            boolean initiated = (tunnel.annotations().value(PCE_INIT) == null) ? false
                                                                               : Boolean.valueOf(tunnel.annotations()
                                                                                       .value(PCE_INIT));
            // build lsp object
            PcepLspObject lspobj = pc.factory().buildLspObject().setAFlag(true)
                    .setPlspId(Integer.valueOf(tunnel.annotations().value(PLSP_ID)))
                    .setDFlag(delegated)
                    .setCFlag(initiated)
                    .setOptionalTlv(llOptionalTlv).build();
            // build ero object
            PcepEroObject eroobj = pc.factory().buildEroObject().setSubObjects(llSubObjects).build();
            float iBandwidth = DEFAULT_BANDWIDTH_VALUE;
            if (tunnel.annotations().value(BANDWIDTH) != null) {
                iBandwidth = Float.parseFloat(tunnel.annotations().value(BANDWIDTH));
            }
            // build bandwidth object
            PcepBandwidthObject bandwidthObject = pc.factory().buildBandwidthObject().setBandwidth(iBandwidth).build();
            // build pcep attribute
            PcepAttribute pcepAttribute = pc.factory().buildPcepAttribute().setBandwidthObject(bandwidthObject).build();
            // build pcep msg path
            PcepMsgPath msgPath = pc.factory().buildPcepMsgPath().setEroObject(eroobj).setPcepAttribute(pcepAttribute)
                    .build();

            PcepUpdateRequest updateRequest = pc.factory().buildPcepUpdateRequest().setSrpObject(srpobj)
                    .setLspObject(lspobj).setMsgPath(msgPath).build();

            llUpdateRequestList.add(updateRequest);

            PcepUpdateMsg pcUpdateMsg = pc.factory().buildUpdateMsg().setUpdateRequestList(llUpdateRequestList).build();

            pc.sendMessage(Collections.singletonList(pcUpdateMsg));
            pcepTunnelApiMapper.addToTunnelRequestQueue(srpId, pcepTunnelData);
        } catch (PcepParseException e) {
            log.error("PcepParseException occurred while processing release tunnel {}", e.getMessage());
        }
    }

    private class InnerTunnelProvider implements PcepTunnelListener, PcepEventListener, PcepClientListener {

        @Override
        public void handlePcepTunnel(PcepTunnel pcepTunnel) {
            TunnelDescription tunnel = null;
            // instance and id identify a tunnel together
            String tunnelKey = String.valueOf(pcepTunnel.getInstance())
                    + String.valueOf(pcepTunnel.id());

            if (tunnelKey == null || "".equals(tunnelKey)) {
                log.error("Invalid PCEP tunnel");
                return;
            }

            TunnelId tunnelId = getTunnelId(tunnelKey);
            tunnel = buildOpticalTunnel(pcepTunnel, tunnelId);

            OperationType operType = pcepTunnel.getOperationType();
            switch (operType) {
            case ADD:
                tunnelId = service.tunnelAdded(tunnel);
                tunnelMap.put(tunnelKey, tunnelId);
                break;

            case UPDATE:
                service.tunnelUpdated(tunnel);
                break;

            case DELETE:
                service.tunnelRemoved(tunnel);
                tunnelMap.remove(tunnelKey);
                break;

            default:
                log.error("Invalid tunnel operation");
            }
        }

        @Override
        public void handleMessage(PccId pccId, PcepMessage msg) {
            try {
                log.debug("tunnel provider handle message {}", msg.getType().toString());
                switch (msg.getType()) {
                case REPORT:
                    int srpId = 0;
                    LinkedList<PcepStateReport> llStateReportList = null;
                    llStateReportList = ((PcepReportMsg) msg).getStateReportList();
                    ListIterator<PcepStateReport> listIterator = llStateReportList.listIterator();
                    PcepSrpObject srpObj = null;
                    PcepLspObject lspObj = null;
                    while (listIterator.hasNext()) {
                        PcepStateReport stateRpt = listIterator.next();
                        srpObj = stateRpt.getSrpObject();
                        lspObj = stateRpt.getLspObject();

                        if (srpObj instanceof PcepSrpObject) {
                            srpId = srpObj.getSrpID();
                        }

                        log.debug("Plsp ID in handle message " + lspObj.getPlspId());
                        log.debug("SRP ID in handle message " + srpId);

                        if (!(pcepTunnelApiMapper.checkFromTunnelRequestQueue(srpId))) {
                            // For PCRpt without matching SRP id.
                            handleRptWithoutSrpId(stateRpt, pccId);
                            continue;
                        }

                        handleReportMessage(srpId, lspObj, stateRpt);
                    }
                    break;

                default:
                    log.debug("Received unsupported message type {}", msg.getType().toString());
                }
            } catch (Exception e) {
                log.error("Exception occured while processing report message {}", e.getMessage());
            }
        }

        /**
         * Handles report message for setup/update/delete tunnel request.
         *
         * @param srpId unique identifier for PCEP message
         * @param lspObj LSP object
         * @param stateRpt parsed PCEP report msg.
         */
        private void handleReportMessage(int srpId, PcepLspObject lspObj, PcepStateReport stateRpt) {
            ProviderId providerId = new ProviderId("pcep", PROVIDER_ID);
            PcepTunnelData pcepTunnelData = pcepTunnelApiMapper.getDataFromTunnelRequestQueue(srpId);

            // store the values required from report message
            pcepTunnelData.setPlspId(lspObj.getPlspId());
            pcepTunnelData.setLspAFlag(lspObj.getAFlag());
            pcepTunnelData.setLspOFlag(lspObj.getOFlag());
            pcepTunnelData.setLspDFlag(lspObj.getDFlag());

            StatefulIPv4LspIdentifiersTlv ipv4LspTlv = null;
            ListIterator<PcepValueType> listTlvIterator = lspObj.getOptionalTlv().listIterator();
            while (listTlvIterator.hasNext()) {
                PcepValueType tlv = listTlvIterator.next();
                if (tlv.getType() == StatefulIPv4LspIdentifiersTlv.TYPE) {
                    ipv4LspTlv = (StatefulIPv4LspIdentifiersTlv) tlv;
                    break;
                }
            }
            if (ipv4LspTlv != null) {
                pcepTunnelData.setStatefulIpv4IndentifierTlv(ipv4LspTlv);
            }

            Path path = pcepTunnelData.path();
            Tunnel tunnel = pcepTunnelData.tunnel();
            Builder annotationBuilder = DefaultAnnotations.builder();
            annotationBuilder.putAll(pcepTunnelData.tunnel().annotations());

            // PCRpt in response to PCInitate msg will carry PLSP id allocated by PCC.
            if (tunnel.annotations().value(PLSP_ID) == null) {
                annotationBuilder.set(PLSP_ID, String.valueOf(lspObj.getPlspId()));
            }

            // Signalled LSPs will carry local LSP id allocated by signalling protocol(PCC).
            if (tunnel.annotations().value(LOCAL_LSP_ID) == null) {
                annotationBuilder.set(LOCAL_LSP_ID, String.valueOf(ipv4LspTlv.getLspId()));
            }

            if (tunnel.annotations().value(PCC_TUNNEL_ID) == null) {
                annotationBuilder.set(PCC_TUNNEL_ID, String.valueOf(ipv4LspTlv.getTunnelId()));
            }

            SparseAnnotations annotations = annotationBuilder.build();
            DefaultTunnelDescription td = new DefaultTunnelDescription(tunnel.tunnelId(), tunnel.src(),
                                                                       tunnel.dst(), tunnel.type(), tunnel.groupId(),
                                                                       providerId, tunnel.tunnelName(), path,
                                                                       tunnel.resource(), annotations);

            if (CREATE == pcepTunnelData.requestType()) {
                pcepTunnelApiMapper.addToTunnelIdMap(pcepTunnelData);
                pcepTunnelApiMapper.handleCreateTunnelRequestQueue(srpId, pcepTunnelData);
            } else if (DELETE == pcepTunnelData.requestType()) {
                pcepTunnelApiMapper.handleRemoveFromTunnelRequestQueue(srpId, pcepTunnelData);
            } else if (UPDATE == pcepTunnelData.requestType()) {
                pcepTunnelData.setRptFlag(true);
                pcepTunnelApiMapper.addToTunnelIdMap(pcepTunnelData);
                pcepTunnelApiMapper.handleUpdateTunnelRequestQueue(srpId, pcepTunnelData);
            }

            PcepLspStatus pcepLspStatus = PcepLspStatus.values()[lspObj.getOFlag()];

            if (lspObj.getRFlag()) {
                tunnelRemoved(td);
            } else {
                State tunnelState = PcepLspStatus.getTunnelStatusFromLspStatus(pcepLspStatus);
                tunnelUpdated(td, tunnelState);
            }

            // SR-TE also needs PCUpd msg after receiving PCRpt with status GOING-UP even
            // though there are no labels to download for SR-TE.
            if (((pcepLspStatus == PcepLspStatus.GOING_UP)
                    && (LspType.valueOf(tunnel.annotations().value(LSP_SIG_TYPE)) == SR_WITHOUT_SIGNALLING))
                // For PCInit tunnel up, few PCC expects PCUpd message after PCInit message,
                || ((tunnel.state() == State.INIT)
                    && (pcepLspStatus == PcepLspStatus.DOWN)
                    && (tunnel.annotations().value(PCE_INIT) != null
                        && tunnel.annotations().value(PCE_INIT).equals("true"))
                    && (LspType.valueOf(tunnel.annotations().value(LSP_SIG_TYPE)) == WITH_SIGNALLING))) {

                // Query again to get latest tunnel updated with protocol values from PCRpt msg.
                updateTunnel(service.tunnelQueryById(tunnel.tunnelId()), tunnel.path());
            }
        }

        private SparseAnnotations getAnnotations(PcepLspObject lspObj, StatefulIPv4LspIdentifiersTlv ipv4LspIdenTlv,
                String bandwidth, LspType lspType, String costType, boolean isPceInit) {
            Builder builder = DefaultAnnotations.builder();
            /*
             * [RFC 5440] The absence of the METRIC object MUST be interpreted by the PCE as a path computation request
             * for which no constraints need be applied to any of the metrics.
             */
            if (costType != null) {
                builder.set(COST_TYPE, costType);
            }

            if (isPceInit) {
                builder.set(PCE_INIT, String.valueOf(isPceInit));
            }

            if (bandwidth != null) {
                builder.set(BANDWIDTH, bandwidth);
            }

            SparseAnnotations annotations = builder
                    .set(LSP_SIG_TYPE, lspType.name())
                    .set(PCC_TUNNEL_ID, String.valueOf(ipv4LspIdenTlv.getTunnelId()))
                    .set(PLSP_ID, String.valueOf(lspObj.getPlspId()))
                    .set(LOCAL_LSP_ID, String.valueOf(ipv4LspIdenTlv.getLspId()))
                    .set(DELEGATE, String.valueOf(lspObj.getDFlag()))
                    .build();
            return annotations;
        }

        private LspType getLspType(PcepSrpObject srpObj) {
            LspType lspType = WITH_SIGNALLING;

            if (null != srpObj) {
                LinkedList<PcepValueType> llOptionalTlv = srpObj.getOptionalTlv();
                ListIterator<PcepValueType> listIterator = llOptionalTlv.listIterator();

                while (listIterator.hasNext()) {
                    PcepValueType tlv = listIterator.next();
                    switch (tlv.getType()) {
                    case PathSetupTypeTlv.TYPE:
                        lspType = LspType.values()[Integer.valueOf(((PathSetupTypeTlv) tlv).getPst())];
                        break;
                    default:
                        break;
                    }
                }
            }
            return lspType;
        }

        private void handleRptWithoutSrpId(PcepStateReport stateRpt, PccId pccId) {
            ProviderId providerId = new ProviderId("pcep", PROVIDER_ID);
            String costType = null;
            PcepStateReport.PcepMsgPath msgPath = stateRpt.getMsgPath();
            checkNotNull(msgPath);
            PcepEroObject eroObj = msgPath.getEroObject();
            if (eroObj == null) {
                log.error("ERO object is null in report message.");
                return;
            }
            PcepAttribute attributes = msgPath.getPcepAttribute();
            float bandwidth = 0;
            int cost = 0;
            if (attributes != null) {
                if (attributes.getMetricObjectList() != null) {
                    ListIterator<PcepMetricObject> iterator = attributes.getMetricObjectList().listIterator();
                    PcepMetricObject metricObj = iterator.next();

                    while (metricObj != null) {
                        if (metricObj.getBType() == IGP_METRIC) {
                            costType = "COST";
                        } else if (metricObj.getBType() == TE_METRIC) {
                            costType = "TE_COST";
                        }
                        if (costType != null) {
                            cost = metricObj.getMetricVal();
                            log.debug("Path cost {}", cost);
                            break;
                        }
                        metricObj = iterator.next();
                    }
                }
                if (attributes.getBandwidthObject() != null) {
                    bandwidth = attributes.getBandwidthObject().getBandwidth();
                }
            }
            PcepLspObject lspObj = stateRpt.getLspObject();
            List<Object> eroSubObjList = buildPathFromEroObj(eroObj, providerId);
            List<Link> links = new ArrayList<>();
            List<LabelResourceId> labels = new ArrayList<>();
            for (Object linkOrLabel : eroSubObjList) {
                if (linkOrLabel instanceof Link) {
                    links.add((Link) linkOrLabel);
                } else if (linkOrLabel instanceof Integer) {
                    labels.add(LabelResourceId.labelResourceId(((Integer) linkOrLabel).longValue()));
                }
            }
            Path path = null;
            if (!links.isEmpty()) {
                path = new DefaultPath(providerId, links, cost, EMPTY);
            } else if (!lspObj.getRFlag()) {
                return;
            }
            NetworkResource labelStack = new DefaultLabelStack(labels);
            // To carry PST TLV, SRP object can be present with value 0 even when PCRpt is not in response to any action
            // from PCE.
            PcepSrpObject srpObj = stateRpt.getSrpObject();
            LspType lspType = getLspType(srpObj);
            ListIterator<PcepValueType> listTlvIterator = lspObj.getOptionalTlv().listIterator();
            StatefulIPv4LspIdentifiersTlv ipv4LspIdenTlv = null;
            SymbolicPathNameTlv pathNameTlv = null;
            while (listTlvIterator.hasNext()) {
                PcepValueType tlv = listTlvIterator.next();
                switch (tlv.getType()) {
                case StatefulIPv4LspIdentifiersTlv.TYPE:
                    ipv4LspIdenTlv = (StatefulIPv4LspIdentifiersTlv) tlv;
                    break;
                case SymbolicPathNameTlv.TYPE:
                    pathNameTlv = (SymbolicPathNameTlv) tlv;
                    break;
                default:
                    break;
                }
            }
            /*
             * Draft says: The LSP-IDENTIFIERS TLV MUST be included in the LSP object in PCRpt messages for
             * RSVP-signaled LSPs. For ONOS PCECC implementation, it is mandatory.
             */
            if (ipv4LspIdenTlv == null) {
                log.error("Stateful IPv4 identifier TLV is null in PCRpt msg.");
                return;
            }
            IpTunnelEndPoint tunnelEndPointSrc = IpTunnelEndPoint
                    .ipTunnelPoint(IpAddress.valueOf(ipv4LspIdenTlv.getIpv4IngressAddress()));
            IpTunnelEndPoint tunnelEndPointDst = IpTunnelEndPoint
                    .ipTunnelPoint(IpAddress.valueOf(ipv4LspIdenTlv.getIpv4EgressAddress()));
            Collection<Tunnel> tunnelQueryResult = tunnelService.queryTunnel(tunnelEndPointSrc, tunnelEndPointDst);
            // Store delegation flag info and that LSP info because only delegated PCE sends update message
            // Storing if D flag is set, if not dont store. while checking whether delegation if annotation for D flag
            // not present then non-delegated , if present it is delegated.
            if (lspObj.getDFlag()) {
                pcepClientController.getClient(pccId).setLspAndDelegationInfo(
                        new LspKey(lspObj.getPlspId(), ipv4LspIdenTlv.getLspId()), lspObj.getDFlag());
            }
            Tunnel tunnel = null;
            SparseAnnotations oldTunnelAnnotations = null;
            // Asynchronous status change message from PCC for LSP reported earlier.
            for (Tunnel tunnelObj : tunnelQueryResult) {
                if (tunnelObj.annotations().value(PLSP_ID) == null) {
                    /*
                     * PLSP_ID is null while Tunnel is created at PCE and PCInit msg carries it as 0. It is allocated by
                     * PCC and in that case it becomes the first PCRpt msg from PCC for this LSP, and hence symbolic
                     * path name must be carried in the PCRpt msg. Draft says: The SYMBOLIC-PATH-NAME TLV "MUST" be
                     * included in the LSP object in the LSP State Report (PCRpt) message when during a given PCEP
                     * session an LSP is "first" reported to a PCE.
                     */
                    if ((pathNameTlv != null)
                            && Arrays.equals(tunnelObj.tunnelName().value().getBytes(), pathNameTlv.getValue())) {
                        tunnel = tunnelObj;
                        break;
                    }
                    continue;
                }
                if ((Integer.valueOf(tunnelObj.annotations().value(PLSP_ID)) == lspObj.getPlspId())) {
                    if ((Integer
                            .valueOf(tunnelObj.annotations().value(LOCAL_LSP_ID)) == ipv4LspIdenTlv.getLspId())) {
                        tunnel = tunnelObj;
                    }
                    if ((Integer
                            .valueOf(tunnelObj.annotations().value(LOCAL_LSP_ID)) != ipv4LspIdenTlv.getLspId())) {
                        oldTunnelAnnotations = (SparseAnnotations) tunnelObj.annotations();
                    }
                }
            }
            DefaultTunnelDescription td;
            SparseAnnotations annotations = null;
            State tunnelState = PcepLspStatus.getTunnelStatusFromLspStatus(PcepLspStatus.values()[lspObj.getOFlag()]);
            if (tunnel == null) {
                if (lspObj.getRFlag()) {
                    /*
                     * If PCC sends remove message and for any reason PCE does not have that entry, simply discard the
                     * message. Or if PCRpt for initiated LSP received and PCE doesn't know, then too discard.
                     */
                    return;
                }
                DeviceId deviceId = getDevice(pccId);
                if (deviceId == null) {
                    log.error("Ingress deviceId not found");
                    return;
                }
                String tempBandwidth = null;
                String temoCostType = null;
                if (oldTunnelAnnotations != null) {
                    tempBandwidth = oldTunnelAnnotations.value(BANDWIDTH);
                    temoCostType = oldTunnelAnnotations.value(COST_TYPE);
                }
                annotations = getAnnotations(lspObj, ipv4LspIdenTlv, tempBandwidth, lspType,
                    temoCostType, lspObj.getCFlag());
                td = new DefaultTunnelDescription(null, tunnelEndPointSrc, tunnelEndPointDst, MPLS, new GroupId(
                        0), providerId, TunnelName.tunnelName(new String(pathNameTlv.getValue())), path, labelStack,
                        annotations);
                // Do not support PCC initiated LSP after LSP DB sync is completed.
                if (!lspObj.getSFlag() && !lspObj.getCFlag()) {
                    log.error("Received PCC initiated LSP while not in sync.");
                    return;
                }
                /*
                 * If ONOS instance is master for PCC then set delegated flag as annotation and add the tunnel to store.
                 * Because all LSPs need not be delegated, hence mastership for the PCC is confirmed whereas not the
                 * delegation set to all LSPs.If ONOS is not the master for that PCC then check if D flag is set, if yes
                 * wait for 2 seconds [while master has added the tunnel to the store] then update the tunnel. Tunnel is
                 * updated because in case of resilency only delegated LSPs are recomputed and only delegated PCE can
                 * send update message to that client.
                 *
                 * 1)Master can 1st get the Rpt message
                 * a)Master adds the tunnel into core.
                 * b)If a non-master for ingress gets Rpt message with D flag set[as delegation owner]
                 *  after master, then runs timer then update the tunnel with D flag set.
                 * 2)Non-Master can 1st get the Rpt message
                 * a)Non-Master runs the timer check for the tunnel then updates the tunnel with D flag set
                 * b)Master would have got the message while the non-master running timer, hence master adds
                 *  tunnel to core
                 *
                 * In general always master adds the tunnel to the core
                 * while delegated owner [master or non-master with D flag set] always updates the tunnel running timer
                 */
                if (mastershipService.isLocalMaster(deviceId)) {
                    TunnelId tId = tunnelAdded(td, tunnelState);
                    Tunnel tunnelInserted = new DefaultTunnel(providerId, tunnelEndPointSrc, tunnelEndPointDst, MPLS,
                            tunnelState, new GroupId(0), tId, TunnelName.tunnelName(String.valueOf(pathNameTlv
                                    .getValue())), path, labelStack, annotations);

                    PcepTunnelData pcepTunnelData = new PcepTunnelData(tunnelInserted, path, LSP_STATE_RPT);
                    pcepTunnelData.setStatefulIpv4IndentifierTlv(ipv4LspIdenTlv);
                    pcepTunnelApiMapper.addToTunnelIdMap(pcepTunnelData);
                } else if (!mastershipService.isLocalMaster(deviceId) && lspObj.getDFlag()) {
                    //Start timer then update the tunnel with D flag
                    tunnelUpdateInDelegatedCase(pccId, annotations, td, providerId, tunnelState, ipv4LspIdenTlv);
                }
                return;
            }
            //delegated owner will update can be a master or non-master
            if (lspObj.getDFlag() && !lspObj.getRFlag()) {
                tunnelUpdateForDelegatedLsp(tunnel, lspObj,
                        lspType, tunnelState, pccId, labelStack, ipv4LspIdenTlv);
                return;
            }
            removeOrUpdatetunnel(tunnel, lspObj, providerId, tunnelState, ipv4LspIdenTlv);
        }

        private void tunnelUpdateForDelegatedLsp(Tunnel tunnel, PcepLspObject lspObj,
                                                 LspType lspType, State tunnelState, PccId pccId,
                                                 NetworkResource labelStack,
                                                 StatefulIPv4LspIdentifiersTlv ipv4LspIdenTlv) {
            SparseAnnotations annotations = null;
            DefaultTunnelDescription td;
            boolean isPceInit = tunnel.annotations().value(PCE_INIT) == null ? false :
                    Boolean.valueOf((tunnel.annotations().value(PCE_INIT))).booleanValue();
            annotations = getAnnotations(lspObj, ipv4LspIdenTlv,
                    tunnel.annotations().value(BANDWIDTH), lspType,
                    tunnel.annotations().value(COST_TYPE), isPceInit);
            td = new DefaultTunnelDescription(null, tunnel.src(), tunnel.dst(), MPLS, new GroupId(
                    0), tunnel.providerId(), tunnel.tunnelName(),
                    tunnel.path(), labelStack, annotations);
            tunnelUpdateInDelegatedCase(pccId, annotations, td, tunnel.providerId(), tunnelState, ipv4LspIdenTlv);
        }

        private void removeOrUpdatetunnel(Tunnel tunnel, PcepLspObject lspObj, ProviderId providerId,
                State tunnelState, StatefulIPv4LspIdentifiersTlv ipv4LspIdenTlv) {
            DefaultTunnelDescription td = new DefaultTunnelDescription(tunnel.tunnelId(), tunnel.src(), tunnel.dst(),
                    tunnel.type(), tunnel.groupId(), providerId, tunnel.tunnelName(), tunnel.path(),
                    (SparseAnnotations) tunnel.annotations());
            if (lspObj.getRFlag()) {
                tunnelRemoved(td);
            } else {
                PcepTunnelData pcepTunnelData = new PcepTunnelData(tunnel, tunnel.path(), LSP_STATE_RPT);
                pcepTunnelData.setStatefulIpv4IndentifierTlv(ipv4LspIdenTlv);
                pcepTunnelApiMapper.addToTunnelIdMap(pcepTunnelData);
                tunnelUpdated(td, tunnelState);
            }
        }

        private void tunnelUpdateInDelegatedCase(PccId pccId, SparseAnnotations annotations,
                DefaultTunnelDescription td, ProviderId providerId, State tunnelState,
                                                 StatefulIPv4LspIdentifiersTlv ipv4LspIdentifiersTlv) {
            // Wait for 2sec then query tunnel based on ingress PLSP-ID and local LSP-ID

            /*
             * If ONOS is not the master for that PCC then check if D flag is set, if yes wait [while
             * master has added the tunnel to the store] then update the tunnel.
             */
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

            // Thread is started after 2 seconds first time later periodically after 2 seconds to update the tunnel
            executor.scheduleAtFixedRate(new UpdateDelegation(td, providerId, annotations, pccId,
                    executor, tunnelState, ipv4LspIdentifiersTlv), DELAY, DELAY, TimeUnit.SECONDS);
        }

        /**
         * To build Path in network from ERO object.
         *
         * @param eroObj ERO object
         * @param providerId provider id
         * @return list of links and labels
         */
        private List<Object> buildPathFromEroObj(PcepEroObject eroObj, ProviderId providerId) {
            checkNotNull(eroObj);
            List<Object> subObjList = new ArrayList<>();
            LinkedList<PcepValueType> llSubObj = eroObj.getSubObjects();
            if (llSubObj.isEmpty()) {
                log.error("ERO in report message does not have hop information");
                return new ArrayList<>();
            }
            ListIterator<PcepValueType> tlvIterator = llSubObj.listIterator();

            ConnectPoint src = null;
            ConnectPoint dst = null;
            boolean isSrcSet = false;
            while (tlvIterator.hasNext()) {
                PcepValueType subObj = tlvIterator.next();
                switch (subObj.getType()) {

                case IPv4SubObject.TYPE:

                    IPv4SubObject ipv4SubObj = (IPv4SubObject) subObj;
                    if (!isSrcSet) {
                            Iterable<Link> links = linkService.getActiveLinks();
                            for (Link l : links) {
                                if (l.src().port().equals(PortNumber.portNumber(ipv4SubObj.getIpAddress()))) {
                                    src = l.src();
                                    isSrcSet = true;
                                    break;
                                } else if (l.dst().port().equals(PortNumber.portNumber(ipv4SubObj.getIpAddress()))) {
                                    src = l.dst();
                                    isSrcSet = true;
                                    break;
                                }
                            }
                        } else {
                            Iterable<Link> links = linkService.getActiveLinks();
                            for (Link l : links) {
                                if (l.src().port().equals(PortNumber.portNumber(ipv4SubObj.getIpAddress()))) {
                                    dst = l.src();
                                    break;
                                } else if (l.dst().port().equals(PortNumber.portNumber(ipv4SubObj.getIpAddress()))) {
                                    dst = l.dst();
                                    break;
                                }
                            }
                        Link link = DefaultLink.builder()
                                .providerId(providerId)
                                .src(src)
                                .dst(dst)
                                .type(Link.Type.DIRECT)
                                .build();
                        subObjList.add(link);
                        src = dst;
                    }
                    break;
                case SrEroSubObject.TYPE:
                    SrEroSubObject srEroSubObj = (SrEroSubObject) subObj;
                    subObjList.add(srEroSubObj.getSid());

                    if (srEroSubObj.getSt() == PcepNaiIpv4Adjacency.ST_TYPE) {
                        PcepNaiIpv4Adjacency nai = (PcepNaiIpv4Adjacency) (srEroSubObj.getNai());
                        int srcIp = nai.getLocalIpv4Addr();
                        int dstIp = nai.getRemoteIpv4Addr();
                        Iterable<Link> links = linkService.getActiveLinks();
                        for (Link l : links) {
                            long lSrc = l.src().port().toLong();
                            long lDst = l.dst().port().toLong();
                            if (lSrc == srcIp) {
                                src = l.src();
                            } else if (lDst == srcIp) {
                                src = l.dst();
                            }
                            if (lSrc == dstIp) {
                                dst = l.src();
                            } else if (lDst == dstIp) {
                                dst = l.dst();
                            }
                            if (src != null && dst != null) {
                                break;
                            }
                        }
                        if (src == null || dst == null) {
                            return new ArrayList<>();
                        }
                        Link link = DefaultLink.builder()
                                .providerId(providerId)
                                .src(src)
                                .dst(dst)
                                .type(Link.Type.DIRECT)
                                .build();
                        subObjList.add(link);
                    }
                default:
                    // the other sub objects are not required
                }
            }
            return subObjList;
        }

        @Override
        public void clientConnected(PccId pccId) {
            // TODO
        }

        @Override
        public void clientDisconnected(PccId pccId) {
            // TODO
        }

        @Override
        public void handlePcepTunnelStatistics(PcepTunnelStatistics pcepTunnelStatistics) {
            TunnelId id = getTunnelId(String.valueOf(pcepTunnelStatistics.id()));
            TunnelStatistics tunnelStatistics = buildTunnelStatistics(pcepTunnelStatistics);
            tunnelStatisticsMap.put(id, tunnelStatistics);
        }

        @Override
        public void handleEndOfSyncAction(Tunnel tunnel, PcepLspSyncAction endOfSyncAction) {

            if (endOfSyncAction == SEND_UPDATE) {
                updateTunnel(tunnel, tunnel.path());
                return;
            }

            TunnelDescription td = new DefaultTunnelDescription(tunnel.tunnelId(),
                                                                tunnel.src(), tunnel.dst(),
                                                                tunnel.type(),
                                                                tunnel.groupId(),
                                                                tunnel.providerId(),
                                                                tunnel.tunnelName(),
                                                                tunnel.path(),
                                                                (SparseAnnotations) tunnel.annotations());


            if (endOfSyncAction == PcepLspSyncAction.UNSTABLE) {
                // Send PCInit msg again after global reoptimization.
                tunnelUpdated(td, UNSTABLE);

                // To remove the old tunnel from store whose PLSPID is not recognized by ingress PCC.
                tunnelRemoved(td);
            } else if (endOfSyncAction == REMOVE) {
                tunnelRemoved(td);
            }
        }
    }
    @Override
    public Tunnel tunnelQueryById(TunnelId tunnelId) {
        return service.tunnelQueryById(tunnelId);
    }

    private DeviceId getDevice(PccId pccId) {
        // Get lsrId of the PCEP client from the PCC ID. Session info is based on lsrID.
        IpAddress lsrId = pccId.ipAddress();
        String lsrIdentifier = String.valueOf(lsrId);

        // Find PCC deviceID from lsrId stored as annotations
        Iterable<Device> devices = deviceService.getAvailableDevices();
        for (Device dev : devices) {
            if (dev.annotations().value(AnnotationKeys.TYPE).equals("L3")
                    && dev.annotations().value(LSRID).equals(lsrIdentifier)) {
                return dev.id();
            }
        }
        return null;
    }

    /**
     * Updates the tunnel with updated tunnel annotation after a delay of two seconds and checks it till
     * tunnel is found.
     */
    private class UpdateDelegation implements Runnable {
        DefaultTunnelDescription td;
        ProviderId providerId;
        SparseAnnotations annotations;
        PccId pccId;
        ScheduledExecutorService executor;
        State tunnelState;
        StatefulIPv4LspIdentifiersTlv ipv4LspIdentifiersTlv;

        /**
         * Creates an instance of UpdateDelegation.
         *
         * @param td tunnel description
         * @param providerId provider id
         * @param annotations tunnel annotations
         * @param pccId PCEP client id
         * @param executor service of delegated owner
         */
        public UpdateDelegation(DefaultTunnelDescription td, ProviderId providerId, SparseAnnotations annotations,
                PccId pccId, ScheduledExecutorService executor, State tunnelState,
                                StatefulIPv4LspIdentifiersTlv ipv4LspIdentifiersTlv) {
            this.td = td;
            this.providerId = providerId;
            this.annotations = annotations;
            this.pccId = pccId;
            this.executor = executor;
            this.tunnelState = tunnelState;
            this.ipv4LspIdentifiersTlv = ipv4LspIdentifiersTlv;
        }

        //Temporary using annotations later will use projection/network config service
        @Override
        public void run() {
            Collection<Tunnel> tunnelQueryResult = tunnelService.queryTunnel(td.src(), td.dst());
            TunnelId tempTunnelId = null;
            for (Tunnel t : tunnelQueryResult) {
                if (t.annotations().value(LOCAL_LSP_ID) == null ||  t.annotations().value(PLSP_ID) == null) {
                    continue;
                }

                if (t.annotations().value(LOCAL_LSP_ID).equals(td.annotations().value(LOCAL_LSP_ID))
                        && t.annotations().value(PLSP_ID).equals(td.annotations().value(PLSP_ID))
                        && ((IpTunnelEndPoint) t.src()).ip().equals(pccId.id())) {
                    tempTunnelId = t.tunnelId();
                    break;
                }
            }

            //If tunnel is found update the tunnel and shutdown the thread otherwise thread will be executing
            //periodically
            if (tempTunnelId != null) {
                Tunnel tunnel = new DefaultTunnel(providerId, td.src(), td.dst(), MPLS, new GroupId(0),
                        tempTunnelId, td.tunnelName(), td.path(), annotations);
                PcepTunnelData pcepTunnelData = new PcepTunnelData(tunnel, tunnel.path(), LSP_STATE_RPT);
                pcepTunnelData.setStatefulIpv4IndentifierTlv(ipv4LspIdentifiersTlv);
                pcepTunnelData.setLspDFlag(Boolean.valueOf(tunnel.annotations().value(DELEGATE)));
                pcepTunnelApiMapper.addToTunnelIdMap(pcepTunnelData);
                tunnelUpdated(tunnel, td.path(), tunnelState);
                executor.shutdown();
                try {
                    executor.awaitTermination(WAIT_TIME, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    log.error("updating delegation failed");
                }
            }
        }
    }
}
