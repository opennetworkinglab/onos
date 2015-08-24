/*
 * Copyright 2015 Open Networking Laboratory
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

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onosproject.net.DefaultAnnotations.EMPTY;
import static org.onlab.util.Tools.get;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.pcep.api.PcepDpid.uri;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.incubator.net.tunnel.DefaultOpticalTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.DefaultTunnel;
import org.onosproject.incubator.net.tunnel.DefaultTunnelDescription;
import org.onosproject.incubator.net.tunnel.IpTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.DefaultTunnelStatistics;
import org.onosproject.incubator.net.tunnel.OpticalLogicId;
import org.onosproject.incubator.net.tunnel.OpticalTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelDescription;
import org.onosproject.incubator.net.tunnel.TunnelEndPoint;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelName;
import org.onosproject.incubator.net.tunnel.TunnelProvider;
import org.onosproject.incubator.net.tunnel.TunnelProviderRegistry;
import org.onosproject.incubator.net.tunnel.TunnelProviderService;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.incubator.net.tunnel.TunnelStatistics;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.IpElementId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.pcep.api.PcepController;
import org.onosproject.pcep.api.PcepDpid;
import org.onosproject.pcep.api.PcepHopNodeDescription;
import org.onosproject.pcep.api.PcepOperator.OperationType;
import org.onosproject.pcep.api.PcepTunnel;
import org.onosproject.pcep.api.PcepTunnel.PATHTYPE;
import org.onosproject.pcep.api.PcepTunnel.PathState;
import org.onosproject.pcep.api.PcepTunnelListener;
import org.onosproject.pcep.api.PcepTunnelStatistics;
import org.osgi.service.component.annotations.Modified;
import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcep.controller.PcepClient;
import org.onosproject.pcep.controller.PcepClientController;
import org.onosproject.pcep.controller.PcepClientListener;
import org.onosproject.pcep.controller.PcepEventListener;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcInitiatedLspRequest;
import org.onosproject.pcepio.protocol.PcepAttribute;
import org.onosproject.pcepio.protocol.PcepBandwidthObject;
import org.onosproject.pcepio.protocol.PcepEndPointsObject;
import org.onosproject.pcepio.protocol.PcepEroObject;
import org.onosproject.pcepio.protocol.PcepInitiateMsg;
import org.onosproject.pcepio.protocol.PcepLspObject;
import org.onosproject.pcepio.protocol.PcepMessage;
import org.onosproject.pcepio.protocol.PcepMsgPath;
import org.onosproject.pcepio.protocol.PcepReportMsg;
import org.onosproject.pcepio.protocol.PcepRroObject;
import org.onosproject.pcepio.protocol.PcepSrpObject;
import org.onosproject.pcepio.protocol.PcepStateReport;
import org.onosproject.pcepio.protocol.PcepUpdateMsg;
import org.onosproject.pcepio.protocol.PcepUpdateRequest;
import org.onosproject.pcepio.types.IPv4SubObject;
import org.onosproject.pcepio.types.PcepValueType;
import org.onosproject.pcepio.types.StatefulIPv4LspIdentidiersTlv;
import org.onosproject.pcepio.types.SymbolicPathNameTlv;
import org.slf4j.Logger;
import org.osgi.service.component.ComponentContext;

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

    TunnelProviderService service;

    HashMap<String, TunnelId> tunnelMap = new HashMap<String, TunnelId>();
    HashMap<TunnelId, TunnelStatistics> tunnelStatisticsMap = new HashMap<>();
    private HashMap<Long, TunnelStatsCollector> collectors = Maps.newHashMap();

    private InnerTunnelProvider listener = new InnerTunnelProvider();

    protected PcepTunnelApiMapper pcepTunnelAPIMapper = new PcepTunnelApiMapper();
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
            String pcepTunnelId = getPCEPTunnelKey(tunnel.tunnelId());
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
        if (tunnel.type() != Tunnel.Type.MPLS) {
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
        pcepSetupTunnel(tunnel, path, pc);
    }

    @Override
    public void setupTunnel(ElementId srcElement, Tunnel tunnel, Path path) {

        if (tunnel.type() != Tunnel.Type.MPLS) {
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
        pcepSetupTunnel(tunnel, path, pc);
    }

    @Override
    public void releaseTunnel(Tunnel tunnel) {

        if (tunnel.type() != Tunnel.Type.MPLS) {
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
        pcepReleaseTunnel(tunnel, pc);
    }

    @Override
    public void releaseTunnel(ElementId srcElement, Tunnel tunnel) {
        if (tunnel.type() != Tunnel.Type.MPLS) {
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
        pcepReleaseTunnel(tunnel, pc);
    }

    @Override
    public void updateTunnel(Tunnel tunnel, Path path) {
        if (tunnel.type() != Tunnel.Type.MPLS) {
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
        pcepUpdateTunnel(tunnel, path, pc);
    }

    @Override
    public void updateTunnel(ElementId srcElement, Tunnel tunnel, Path path) {

        if (tunnel.type() != Tunnel.Type.MPLS) {
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
        pcepUpdateTunnel(tunnel, path, pc);
    }

    @Override
    public TunnelId tunnelAdded(TunnelDescription tunnel) {
        if (tunnel.type() == Tunnel.Type.MPLS) {
            pcepTunnelAPIMapper.removeFromCoreTunnelRequestQueue(tunnel.id());
            return service.tunnelAdded(tunnel);
        }

        long bandwidth = Long
                .parseLong(tunnel.annotations().value("bandwidth"));

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

    @Override
    public void tunnelRemoved(TunnelDescription tunnel) {
        if (tunnel.type() == Tunnel.Type.MPLS) {
            pcepTunnelAPIMapper.removeFromCoreTunnelRequestQueue(tunnel.id());
            service.tunnelRemoved(tunnel);
        }

        Tunnel tunnelOld = tunnelQueryById(tunnel.id());
        checkNotNull(tunnelOld, "The tunnel id is not exsited.");
        if (tunnelOld.type() != Tunnel.Type.VLAN) {
            error("Illegal tunnel type. Only support VLAN tunnel deletion.");
            return;
        }
        String pcepTunnelId = getPCEPTunnelKey(tunnel.id());
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
        if (tunnel.type() == Tunnel.Type.MPLS) {
            pcepTunnelAPIMapper.removeFromCoreTunnelRequestQueue(tunnel.id());
            service.tunnelUpdated(tunnel);
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
        String pcepTunnelId = getPCEPTunnelKey(tunnel.id());

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
        return new DefaultLink(id(), connectPoint(src, sp), connectPoint(dst,
                                                                         dp),
                               Link.Type.TUNNEL);
    }

    // Creates a path that leads through the given devices.
    private Path createPath(List<PcepHopNodeDescription> hopList,
                            PATHTYPE pathtype, PathState pathState) {
        if (hopList == null || hopList.size() == 0) {
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
        TunnelDescription tunnel = new DefaultTunnelDescription(
                                                                tunnelId,
                                                                srcPoint,
                                                                dstPoint,
                                                                tunnelType,
                                                                new DefaultGroupId(
                                                                                   0),
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
    private String getPCEPTunnelKey(TunnelId tunnelId) {
        for (String key : tunnelMap.keySet()) {
            if (tunnelMap.get(key).id() == tunnelId.id()) {
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
     * @return list of ipv4 subobjects
     */
    private LinkedList<PcepValueType> createPcepPath(Path path) {
        LinkedList<PcepValueType> llSubObjects = new LinkedList<PcepValueType>();
        List<Link> listLink = path.links();
        ConnectPoint source = null;
        ConnectPoint destination = null;
        IpAddress ipDstAddress = null;
        IpAddress ipSrcAddress = null;
        PcepValueType subObj = null;

        for (Link link : listLink) {
            source = link.src();
            if (!(source.equals(destination))) {
                //set IPv4SubObject for ERO object
                ipSrcAddress = source.ipElementId().ipAddress();
                subObj = new IPv4SubObject(ipSrcAddress.getIp4Address().toInt());
                llSubObjects.add(subObj);
            }

            destination = link.dst();
            ipDstAddress = destination.ipElementId().ipAddress();
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
        LinkedList<PcepValueType> llSubObjects = createPcepPath(path);

        if (llSubObjects == null || llSubObjects.size() == 0) {
            log.error("There is no link information to create tunnel");
            return null;
        }

        //build SRP object
        PcepSrpObject srpobj = pc.factory().buildSrpObject().setSrpID(srpId).setRFlag(false).build();

        LinkedList<PcepValueType> llOptionalTlv = new LinkedList<PcepValueType>();
        LinkedList<PcInitiatedLspRequest> llPcInitiatedLspRequestList = new LinkedList<PcInitiatedLspRequest>();
        // set LSP identifiers TLV
        tlv = new StatefulIPv4LspIdentidiersTlv((((IpTunnelEndPoint) tunnel.src()).ip().getIp4Address().toInt()),
                                                (short) 0, (short) 0, 0,
                                                (((IpTunnelEndPoint) tunnel.dst()).ip().getIp4Address().toInt()));
        llOptionalTlv.add(tlv);
        //set SymbolicPathNameTlv of LSP object
        tlv = new SymbolicPathNameTlv(tunnel.tunnelName().value().getBytes());
        llOptionalTlv.add(tlv);

        //build LSP object
        PcepLspObject lspobj = pc.factory().buildLspObject().setAFlag(true).setOFlag((byte) 0).setPlspId(0)
                .setOptionalTlv(llOptionalTlv).build();

        //build ENDPOINTS object
        PcepEndPointsObject endpointsobj = pc.factory().buildEndPointsObject()
                .setSourceIpAddress(((IpTunnelEndPoint) tunnel.src()).ip().getIp4Address().toInt())
                .setDestIpAddress(((IpTunnelEndPoint) tunnel.dst()).ip().getIp4Address().toInt())
                .setPFlag(true).build();

        //build ERO object
        PcepEroObject eroobj = pc.factory().buildEroObject().setSubObjects(llSubObjects).build();

        int iBandwidth = DEFAULT_BANDWIDTH_VALUE;
        if (tunnel.annotations().value("bandwidth") != null) {
            iBandwidth = Integer.parseInt(tunnel.annotations().value("bandwidth"));
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
            int srpId = SrpIdGenerators.create();
            PcepTunnelData pcepTunnelData = new PcepTunnelData(tunnel, path, RequestType.CREATE);

            pcepTunnelAPIMapper.addToCoreTunnelRequestQueue(pcepTunnelData);

            LinkedList<PcInitiatedLspRequest> llPcInitiatedLspRequestList = createPcInitiatedLspReqList(tunnel, path,
                                                                                                        pc, srpId);
            if (llPcInitiatedLspRequestList == null || llPcInitiatedLspRequestList.size() == 0) {
                log.error("Failed to create PcInitiatedLspRequestList");
                return;
            }

            //build PCInitiate message
            PcepInitiateMsg pcInitiateMsg = pc.factory().buildPcepInitiateMsg()
                    .setPcInitiatedLspRequestList(llPcInitiatedLspRequestList)
                    .build();

            pc.sendMessage(Collections.singletonList(pcInitiateMsg));

            pcepTunnelAPIMapper.addToTunnelRequestQueue(srpId, pcepTunnelData);
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
            PcepTunnelData pcepTunnelData = new PcepTunnelData(tunnel, RequestType.DELETE);
            pcepTunnelAPIMapper.addToCoreTunnelRequestQueue(pcepTunnelData);
            int srpId = SrpIdGenerators.create();
            TunnelId tunnelId = tunnel.tunnelId();
            int plspId = 0;
            StatefulIPv4LspIdentidiersTlv statefulIpv4IndentifierTlv = null;

            if (!(pcepTunnelAPIMapper.checkFromTunnelDBQueue(tunnelId))) {
                log.error("Tunnel doesnot exists. Tunnel id {}" + tunnelId.toString());
                return;
            } else {
                PcepTunnelData pcepTunnelDbData = pcepTunnelAPIMapper.getDataFromTunnelDBQueue(tunnelId);
                plspId = pcepTunnelDbData.plspId();
                statefulIpv4IndentifierTlv = pcepTunnelDbData.statefulIpv4IndentifierTlv();
            }
            // build srp object
            PcepSrpObject srpobj = pc.factory().buildSrpObject().setSrpID(srpId).setRFlag(true).build();

            PcepValueType tlv;
            LinkedList<PcepValueType> llOptionalTlv = new LinkedList<PcepValueType>();
            LinkedList<PcInitiatedLspRequest> llPcInitiatedLspRequestList = new LinkedList<PcInitiatedLspRequest>();

            if (statefulIpv4IndentifierTlv != null) {
                tlv = statefulIpv4IndentifierTlv;
            } else {
                tlv = new StatefulIPv4LspIdentidiersTlv((
                        ((IpTunnelEndPoint) tunnel.src()).ip().getIp4Address().toInt()),
                        (short) 0, (short) 0, 0,
                        (((IpTunnelEndPoint) tunnel.dst()).ip().getIp4Address().toInt()));
            }
            llOptionalTlv.add(tlv);
            tlv = new SymbolicPathNameTlv(tunnel.tunnelName().value().getBytes());
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

            pcepTunnelAPIMapper.addToTunnelRequestQueue(srpId, pcepTunnelData);
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
            PcepTunnelData pcepTunnelData = new PcepTunnelData(tunnel, path, RequestType.UPDATE);
            pcepTunnelAPIMapper.addToCoreTunnelRequestQueue(pcepTunnelData);
            int srpId = SrpIdGenerators.create();
            TunnelId tunnelId = tunnel.tunnelId();
            PcepValueType tlv;
            int plspId = 0;

            LinkedList<PcepValueType> llSubObjects = createPcepPath(path);
            LinkedList<PcepValueType> llOptionalTlv = new LinkedList<PcepValueType>();
            LinkedList<PcepUpdateRequest> llUpdateRequestList = new LinkedList<PcepUpdateRequest>();

            //build SRP object
            PcepSrpObject srpobj = pc.factory().buildSrpObject().setSrpID(srpId).setRFlag(false).build();

            if (!(pcepTunnelAPIMapper.checkFromTunnelDBQueue(tunnelId))) {
                log.error("Tunnel doesnot exists in DB");
                return;
            } else {
                PcepTunnelData pcepTunnelDBData = pcepTunnelAPIMapper.getDataFromTunnelDBQueue(tunnelId);
                plspId = pcepTunnelDBData.plspId();
            }

            tlv = new StatefulIPv4LspIdentidiersTlv((((IpTunnelEndPoint) tunnel.src()).ip().getIp4Address().toInt()),
                                                    (short) 0, (short) 0, 0,
                                                    (((IpTunnelEndPoint) tunnel.dst()).ip().getIp4Address().toInt()));
            llOptionalTlv.add(tlv);

            if (tunnel.tunnelName().value() != null) {
                tlv = new SymbolicPathNameTlv(tunnel.tunnelName().value().getBytes());
                llOptionalTlv.add(tlv);
            }

            // build lsp object
            PcepLspObject lspobj = pc.factory().buildLspObject().setAFlag(true).setPlspId(plspId)
                    .setOptionalTlv(llOptionalTlv).build();
            // build ero object
            PcepEroObject eroobj = pc.factory().buildEroObject().setSubObjects(llSubObjects).build();

            int iBandwidth = DEFAULT_BANDWIDTH_VALUE;
            if (tunnel.annotations().value("bandwidth") != null) {
                iBandwidth = Integer.parseInt(tunnel.annotations().value("bandwidth"));
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
            pcepTunnelAPIMapper.addToTunnelRequestQueue(srpId, pcepTunnelData);
        } catch (PcepParseException e) {
            log.error("PcepParseException occurred while processing release tunnel {}", e.getMessage());
        }
    }



    private class InnerTunnelProvider implements PcepTunnelListener, PcepEventListener, PcepClientListener {

        @Override
        public void handlePCEPTunnel(PcepTunnel pcepTunnel) {
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

                        if (!(pcepTunnelAPIMapper.checkFromTunnelRequestQueue(srpId))) {

                            // Check the sync status
                            if (lspObj.getSFlag()) {
                                handleSyncReport(stateRpt);
                            } else if (!pcepClientController.getClient(pccId).isSyncComplete()) {
                                // sync is done
                                pcepClientController.getClient(pccId).setIsSyncComplete(true);
                            }
                            continue;
                        }

                        handleReportMessage(srpId, lspObj);
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
         * @param srpId unique identifier for pcep message
         * @param lspObj lsp object
         */
        private void handleReportMessage(int srpId, PcepLspObject lspObj) {
            ProviderId providerId = new ProviderId("pcep", PROVIDER_ID);
            PcepTunnelData pcepTunnelData = pcepTunnelAPIMapper.getDataFromTunnelRequestQueue(srpId);
            SparseAnnotations annotations = (SparseAnnotations) pcepTunnelData.tunnel().annotations();

            // store the values required from report message
            pcepTunnelData.setPlspId(lspObj.getPlspId());
            pcepTunnelData.setLspAFlag(lspObj.getAFlag());
            pcepTunnelData.setLspOFlag(lspObj.getOFlag());
            pcepTunnelData.setLspDFlag(lspObj.getDFlag());

            StatefulIPv4LspIdentidiersTlv ipv4LspTlv = null;
            ListIterator<PcepValueType> listTlvIterator = lspObj.getOptionalTlv().listIterator();
            while (listTlvIterator.hasNext()) {
                PcepValueType tlv = listTlvIterator.next();
                if (tlv.getType() == StatefulIPv4LspIdentidiersTlv.TYPE) {
                    ipv4LspTlv = (StatefulIPv4LspIdentidiersTlv) tlv;
                    break;
                }
            }
            if (ipv4LspTlv != null) {
                pcepTunnelData.setStatefulIpv4IndentifierTlv(ipv4LspTlv);
            }

            Path path = pcepTunnelData.path();
            Tunnel tunnel = pcepTunnelData.tunnel();
            DefaultTunnelDescription td = new DefaultTunnelDescription(tunnel.tunnelId(), tunnel.src(),
                                                                       tunnel.dst(), tunnel.type(), tunnel.groupId(),
                                                                       providerId, tunnel.tunnelName(), path,
                                                                       annotations);

            if (RequestType.CREATE == pcepTunnelData.requestType()) {
                log.debug("Report received for create request");

                pcepTunnelAPIMapper.handleCreateTunnelRequestQueue(srpId, pcepTunnelData);
                if (0 == lspObj.getOFlag()) {
                    log.warn("The tunnel is in down state");
                }
                tunnelAdded(td);
            }
            if (RequestType.DELETE == pcepTunnelData.requestType()) {
                log.debug("Report received for delete request");
                pcepTunnelAPIMapper.handleRemoveFromTunnelRequestQueue(srpId, pcepTunnelData);
                tunnelRemoved(td);
            }

            if (RequestType.UPDATE == pcepTunnelData.requestType()) {
                log.debug("Report received for update request");
                pcepTunnelData.setRptFlag(true);
                pcepTunnelAPIMapper.addToTunnelIdMap(pcepTunnelData);
                pcepTunnelAPIMapper.handleUpdateTunnelRequestQueue(srpId, pcepTunnelData);

                if (0 == lspObj.getOFlag()) {
                    log.warn("The tunnel is in down state");
                }
                if (!(pcepTunnelAPIMapper.checkFromTunnelRequestQueue(srpId))) {
                    tunnelUpdated(td);
                }
            }
        }

        /**
         * Handles sync report received from pcc.
         *
         * @param stateRpt pcep state report
         */
        private void handleSyncReport(PcepStateReport stateRpt) {
            PcepLspObject lspObj = stateRpt.getLspObject();
            PcepStateReport.PcepMsgPath msgPath = stateRpt.getMsgPath();
            checkNotNull(msgPath);
            PcepRroObject rroObj = msgPath.getRroObject();
            if (rroObj == null) {
                log.debug("RRO object is null in sate report");
                return;
            }
            int bandwidth = 0;

            log.debug("Handle Sync report received from PCC.");

            if (0 == lspObj.getOFlag()) {
                log.warn("The PCC reported tunnel is in down state");
            }
            log.debug("Sync report received");

            if (msgPath.getBandwidthObject() != null) {
                bandwidth = msgPath.getBandwidthObject().getBandwidth();
            }

            buildAndStorePcepTunnelData(lspObj, rroObj, bandwidth);
        }

        /**
         * To build Path in network from RRO object.
         *
         * @param rroObj rro object
         * @param providerId provider id
         * @return path object
         */
        private Path buildPathFromRroObj(PcepRroObject rroObj, ProviderId providerId) {
            checkNotNull(rroObj);
            List<Link> links = new ArrayList<Link>();
            LinkedList<PcepValueType> llSubObj = rroObj.getSubObjects();
            if (0 == llSubObj.size()) {
                log.error("RRO in report message does not have hop information");
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
                        IpAddress srcIp = IpAddress.valueOf(ipv4SubObj.getIpAddress());
                        src = new ConnectPoint(IpElementId.ipElement(srcIp), PortNumber.portNumber(0));
                        isSrcSet = true;
                    } else {
                        IpAddress dstIp = IpAddress.valueOf(ipv4SubObj.getIpAddress());
                        dst = new ConnectPoint(IpElementId.ipElement(dstIp), PortNumber.portNumber(0));
                        Link link = new DefaultLink(providerId, src, dst, Link.Type.DIRECT, EMPTY);
                        links.add(link);
                        src = dst;
                    }
                    break;
                default:
                    // the other sub objects are not required
                }
            }
            return new DefaultPath(providerId, links, 0, EMPTY);
        }

        /**
         * To build pcepTunnelData and informs core about the pcc reported tunnel.
         *
         * @param lspObj pcep lsp object
         * @param rroObj pcep rro object
         * @param bandwidth bandwidth of tunnel
         */
        private void buildAndStorePcepTunnelData(PcepLspObject lspObj, PcepRroObject rroObj,
                                                 int bandwidth) {

            ProviderId providerId = new ProviderId("pcep", PROVIDER_ID);

            // StatefulIPv4LspIdentidiersTlv in LSP object will have the source and destination address.
            StatefulIPv4LspIdentidiersTlv lspIdenTlv = null;
            SymbolicPathNameTlv pathNameTlv = null;
            LinkedList<PcepValueType> llOptionalTlv = lspObj.getOptionalTlv();
            ListIterator<PcepValueType> listIterator = llOptionalTlv.listIterator();
            while (listIterator.hasNext()) {
                PcepValueType tlv = listIterator.next();
                switch (tlv.getType()) {
                case StatefulIPv4LspIdentidiersTlv.TYPE:
                    lspIdenTlv = (StatefulIPv4LspIdentidiersTlv) tlv;
                    break;
                case SymbolicPathNameTlv.TYPE:
                    pathNameTlv = (SymbolicPathNameTlv) tlv;
                    break;
                default:
                    // currently this tlv is not required
                }
            }

            IpTunnelEndPoint tunnelEndPointSrc;
            tunnelEndPointSrc = IpTunnelEndPoint.ipTunnelPoint(IpAddress.valueOf(lspIdenTlv.getIpv4IngressAddress()));
            IpTunnelEndPoint tunnelEndPointDst;
            tunnelEndPointDst = IpTunnelEndPoint.ipTunnelPoint(IpAddress.valueOf(lspIdenTlv.getIpv4EgressAddress()));

            Path path = buildPathFromRroObj(rroObj, providerId);

            SparseAnnotations annotations = DefaultAnnotations.builder()
                    .set("bandwidth", (new Integer(bandwidth)).toString())
                    .build();

            DefaultTunnelDescription td = new DefaultTunnelDescription(null, tunnelEndPointSrc,
                                                                       tunnelEndPointDst, Tunnel.Type.MPLS,
                                                                       new DefaultGroupId(0), providerId,
                                                                       TunnelName.tunnelName(pathNameTlv.toString()),
                                                                       path, annotations);
            TunnelId tId = tunnelAdded(td);

            Tunnel tunnel = new DefaultTunnel(providerId, tunnelEndPointSrc, tunnelEndPointDst, Tunnel.Type.MPLS,
                                              new DefaultGroupId(0), tId,
                                              TunnelName.tunnelName(pathNameTlv.toString()), path, annotations);

            PcepTunnelData pcepTunnelData = new PcepTunnelData(tunnel, path, RequestType.LSP_STATE_RPT);
            pcepTunnelData.setStatefulIpv4IndentifierTlv(lspIdenTlv);
            pcepTunnelAPIMapper.addPccTunnelDB(pcepTunnelData);
            pcepTunnelAPIMapper.addToTunnelIdMap(pcepTunnelData);
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
    }

    @Override
    public Tunnel tunnelQueryById(TunnelId tunnelId) {
        return service.tunnelQueryById(tunnelId);
    }
}
