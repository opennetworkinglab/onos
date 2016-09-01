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
package org.onosproject.pcep.controller.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onosproject.incubator.net.resource.label.LabelResourceAdminService;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.resource.label.LabelResourceService;
import org.onosproject.incubator.net.tunnel.DefaultLabelStack;
import org.onosproject.incubator.net.tunnel.DefaultTunnel;
import org.onosproject.incubator.net.tunnel.IpTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.LabelStack;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.incubator.net.tunnel.Tunnel.State;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultAnnotations.Builder;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Path;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.onosproject.pcelabelstore.PcepLabelOp;
import org.onosproject.pcelabelstore.api.PceLabelStore;
import org.onosproject.pcep.api.DeviceCapability;
import org.onosproject.pcep.controller.LspKey;
import org.onosproject.pcep.controller.LspType;
import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcep.controller.PcepClient;
import org.onosproject.pcep.controller.PcepClientController;
import org.onosproject.pcep.controller.PcepClientListener;
import org.onosproject.pcep.controller.PcepEventListener;
import org.onosproject.pcep.controller.PcepLspStatus;
import org.onosproject.pcep.controller.PcepNodeListener;
import org.onosproject.pcep.controller.SrpIdGenerators;
import org.onosproject.pcep.controller.driver.PcepAgent;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcInitiatedLspRequest;
import org.onosproject.pcepio.protocol.PcepError;
import org.onosproject.pcepio.protocol.PcepErrorInfo;
import org.onosproject.pcepio.protocol.PcepErrorMsg;
import org.onosproject.pcepio.protocol.PcepErrorObject;
import org.onosproject.pcepio.protocol.PcepFactory;
import org.onosproject.pcepio.protocol.PcepInitiateMsg;
import org.onosproject.pcepio.protocol.PcepLspObject;
import org.onosproject.pcepio.protocol.PcepMessage;
import org.onosproject.pcepio.protocol.PcepNai;
import org.onosproject.pcepio.protocol.PcepReportMsg;
import org.onosproject.pcepio.protocol.PcepSrpObject;
import org.onosproject.pcepio.protocol.PcepStateReport;
import org.onosproject.pcepio.types.PathSetupTypeTlv;
import org.onosproject.pcepio.types.PcepNaiIpv4Adjacency;
import org.onosproject.pcepio.types.PcepNaiIpv4NodeId;
import org.onosproject.pcepio.types.PcepValueType;
import org.onosproject.pcepio.types.SrEroSubObject;
import org.onosproject.pcepio.types.StatefulIPv4LspIdentifiersTlv;
import org.onosproject.pcepio.types.SymbolicPathNameTlv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import static com.google.common.base.Preconditions.checkNotNull;

import static org.onosproject.pcep.controller.PcepSyncStatus.IN_SYNC;
import static org.onosproject.pcep.controller.LspType.WITHOUT_SIGNALLING_AND_WITHOUT_SR;
import static org.onosproject.pcep.controller.LspType.WITH_SIGNALLING;
import static org.onosproject.pcep.controller.PcepLspSyncAction.REMOVE;
import static org.onosproject.pcep.controller.PcepLspSyncAction.SEND_UPDATE;
import static org.onosproject.pcep.controller.PcepLspSyncAction.UNSTABLE;
import static org.onosproject.pcepio.types.PcepErrorDetailInfo.ERROR_TYPE_19;
import static org.onosproject.pcepio.types.PcepErrorDetailInfo.ERROR_VALUE_5;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.BANDWIDTH;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.LOCAL_LSP_ID;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.LSP_SIG_TYPE;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.PCC_TUNNEL_ID;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.PCE_INIT;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.PLSP_ID;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.DELEGATE;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.COST_TYPE;
import static org.onosproject.pcep.controller.PcepSyncStatus.SYNCED;
import static org.onosproject.pcep.controller.PcepSyncStatus.NOT_SYNCED;

/**
 * Implementation of PCEP client controller.
 */
@Component(immediate = true)
@Service
public class PcepClientControllerImpl implements PcepClientController {

    private static final Logger log = LoggerFactory.getLogger(PcepClientControllerImpl.class);
    private static final long IDENTIFIER_SET = 0x100000000L;
    private static final long SET = 0xFFFFFFFFL;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TunnelService tunnelService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService netCfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LabelResourceAdminService labelRsrcAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LabelResourceService labelRsrcService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PceLabelStore pceStore;

    protected ConcurrentHashMap<PccId, PcepClient> connectedClients =
            new ConcurrentHashMap<>();

    protected PcepClientAgent agent = new PcepClientAgent();
    protected Set<PcepClientListener> pcepClientListener = new HashSet<>();

    protected Set<PcepEventListener> pcepEventListener = Sets.newHashSet();
    protected Set<PcepNodeListener> pcepNodeListener = Sets.newHashSet();

    // LSR-id and device-id mapping for checking capability if L3 device is not
    // having its capability
    private Map<String, DeviceId> lsrIdDeviceIdMap = new HashMap<>();

    private final Controller ctrl = new Controller();
    public static final long GLOBAL_LABEL_SPACE_MIN = 4097;
    public static final long GLOBAL_LABEL_SPACE_MAX = 5121;
    private static final String LSRID = "lsrId";
    private static final String DEVICE_NULL = "Device-cannot be null";
    private static final String LINK_NULL = "Link-cannot be null";

    private BasicPceccHandler crHandler;
    private PceccSrTeBeHandler srTeHandler;

    private DeviceListener deviceListener = new InternalDeviceListener();
    private LinkListener linkListener = new InternalLinkListener();
    private InternalConfigListener cfgListener = new InternalConfigListener();

    @Activate
    public void activate() {
        ctrl.start(agent);
        crHandler = BasicPceccHandler.getInstance();
        crHandler.initialize(labelRsrcService, deviceService, pceStore, this);

        srTeHandler = PceccSrTeBeHandler.getInstance();
        srTeHandler.initialize(labelRsrcAdminService, labelRsrcService, this, pceStore,
                               deviceService);

        deviceService.addListener(deviceListener);
        linkService.addListener(linkListener);
        netCfgService.addListener(cfgListener);

        // Reserve global node pool
        if (!srTeHandler.reserveGlobalPool(GLOBAL_LABEL_SPACE_MIN, GLOBAL_LABEL_SPACE_MAX)) {
            log.debug("Global node pool was already reserved.");
        }

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        // Close all connected clients
        closeConnectedClients();
        deviceService.removeListener(deviceListener);
        linkService.removeListener(linkListener);
        netCfgService.removeListener(cfgListener);
        ctrl.stop();
        log.info("Stopped");
    }

    @Override
    public Collection<PcepClient> getClients() {
        return connectedClients.values();
    }

    @Override
    public PcepClient getClient(PccId pccId) {
        return connectedClients.get(pccId);
    }

    @Override
    public void addListener(PcepClientListener listener) {
        if (!pcepClientListener.contains(listener)) {
            this.pcepClientListener.add(listener);
        }
    }

    @Override
    public void removeListener(PcepClientListener listener) {
        this.pcepClientListener.remove(listener);
    }

    @Override
    public void addEventListener(PcepEventListener listener) {
        pcepEventListener.add(listener);
    }

    @Override
    public void removeEventListener(PcepEventListener listener) {
        pcepEventListener.remove(listener);
    }

    @Override
    public void writeMessage(PccId pccId, PcepMessage msg) {
        this.getClient(pccId).sendMessage(msg);
    }

    @Override
    public void addNodeListener(PcepNodeListener listener) {
        pcepNodeListener.add(listener);
    }

    @Override
    public void removeNodeListener(PcepNodeListener listener) {
        pcepNodeListener.remove(listener);
    }

    @Override
    public void processClientMessage(PccId pccId, PcepMessage msg) {
        PcepClient pc = getClient(pccId);

        switch (msg.getType()) {
        case NONE:
            break;
        case OPEN:
            break;
        case KEEP_ALIVE:
            break;
        case PATH_COMPUTATION_REQUEST:
            break;
        case PATH_COMPUTATION_REPLY:
            break;
        case NOTIFICATION:
            break;
        case ERROR:
            break;
        case INITIATE:
            if (!pc.capability().pcInstantiationCapability()) {
                pc.sendMessage(Collections.singletonList(getErrMsg(pc.factory(), ERROR_TYPE_19,
                        ERROR_VALUE_5)));
            }
            break;
        case UPDATE:
            if (!pc.capability().statefulPceCapability()) {
                pc.sendMessage(Collections.singletonList(getErrMsg(pc.factory(), ERROR_TYPE_19,
                        ERROR_VALUE_5)));
            }
            break;
        case LABEL_UPDATE:
            if (!pc.capability().pceccCapability()) {
                pc.sendMessage(Collections.singletonList(getErrMsg(pc.factory(), ERROR_TYPE_19,
                        ERROR_VALUE_5)));
            }
            break;
        case CLOSE:
            log.info("Sending Close Message  to {" + pccId.toString() + "}");
            pc.sendMessage(Collections.singletonList(pc.factory().buildCloseMsg().build()));
            //now disconnect client
            pc.disconnectClient();
            break;
        case REPORT:
            //Only update the listener if respective capability is supported else send PCEP-ERR msg
            if (pc.capability().statefulPceCapability()) {

                ListIterator<PcepStateReport> listIterator = ((PcepReportMsg) msg).getStateReportList().listIterator();
                while (listIterator.hasNext()) {
                    PcepStateReport stateRpt = listIterator.next();
                    PcepLspObject lspObj = stateRpt.getLspObject();
                    if (lspObj.getSFlag()) {
                        if (pc.lspDbSyncStatus() != IN_SYNC) {
                            log.debug("LSP DB sync started for PCC {}", pc.getPccId().id().toString());
                            // Initialize LSP DB sync and temporary cache.
                            pc.setLspDbSyncStatus(IN_SYNC);
                            pc.initializeSyncMsgList(pccId);
                        }
                        // Store stateRpt in temporary cache.
                        pc.addSyncMsgToList(pccId, stateRpt);

                        // Don't send to provider as of now.
                        continue;
                    } else if (lspObj.getPlspId() == 0) {
                        if (pc.lspDbSyncStatus() == IN_SYNC
                                || pc.lspDbSyncStatus() == NOT_SYNCED) {
                            // Set end of LSPDB sync.
                            log.debug("LSP DB sync completed for PCC {}", pc.getPccId().id().toString());
                            pc.setLspDbSyncStatus(SYNCED);

                            // Call packet provider to initiate label DB sync (only if PCECC capable).
                            if (pc.capability().pceccCapability()) {
                                log.debug("Trigger label DB sync for PCC {}", pc.getPccId().id().toString());
                                pc.setLabelDbSyncStatus(IN_SYNC);
                                // Get lsrId of the PCEP client from the PCC ID. Session info is based on lsrID.
                                String lsrId = String.valueOf(pccId.ipAddress());
                                DeviceId pccDeviceId = DeviceId.deviceId(lsrId);
                                try {
                                    syncLabelDb(pccDeviceId);
                                    pc.setLabelDbSyncStatus(SYNCED);
                                } catch (PcepParseException e) {
                                    log.error("Exception caught in sending label masg to PCC while in sync.");
                                }
                            } else {
                                // If label db sync is not to be done, handle end of LSPDB sync actions.
                                agent.analyzeSyncMsgList(pccId);
                            }
                            continue;
                        }
                    }

                    PcepLspStatus pcepLspStatus = PcepLspStatus.values()[lspObj.getOFlag()];
                    LspType lspType = getLspType(stateRpt.getSrpObject());

                    // Download (or remove) labels for basic PCECC LSPs.
                    if (lspType.equals(WITHOUT_SIGNALLING_AND_WITHOUT_SR)) {
                        boolean isRemove = lspObj.getRFlag();
                        Tunnel tunnel = null;

                        if (isRemove || pcepLspStatus.equals(PcepLspStatus.GOING_UP)) {
                            tunnel = getTunnel(lspObj);
                        }

                        if (tunnel != null) {
                            if (isRemove) {
                                crHandler.releaseLabel(tunnel);
                            } else {
                                crHandler.allocateLabel(tunnel);
                            }
                        }
                    }

                    // It's a usual report message while sync is not undergoing. So process it immediately.
                    LinkedList<PcepStateReport> llPcRptList = new LinkedList<>();
                    llPcRptList.add(stateRpt);
                    PcepMessage pcReportMsg = pc.factory().buildReportMsg().setStateReportList((llPcRptList))
                            .build();
                    for (PcepEventListener l : pcepEventListener) {
                        l.handleMessage(pccId, pcReportMsg);
                    }
                }
            } else {
                // Send PCEP-ERROR message.
                pc.sendMessage(Collections.singletonList(getErrMsg(pc.factory(),
                        ERROR_TYPE_19, ERROR_VALUE_5)));
            }
            break;
        case LABEL_RANGE_RESERV:
            break;
        case LS_REPORT: //TODO: need to handle LS report to add or remove node
            break;
        case MAX:
            break;
        case END:
            break;
        default:
            break;
        }
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

    private Tunnel getTunnel(PcepLspObject lspObj) {
        ListIterator<PcepValueType> listTlvIterator = lspObj.getOptionalTlv().listIterator();
        StatefulIPv4LspIdentifiersTlv ipv4LspIdenTlv = null;
        SymbolicPathNameTlv pathNameTlv = null;
        Tunnel tunnel = null;
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
            return null;
        }
        IpTunnelEndPoint tunnelEndPointSrc = IpTunnelEndPoint
                .ipTunnelPoint(IpAddress.valueOf(ipv4LspIdenTlv.getIpv4IngressAddress()));
        IpTunnelEndPoint tunnelEndPointDst = IpTunnelEndPoint
                .ipTunnelPoint(IpAddress.valueOf(ipv4LspIdenTlv.getIpv4EgressAddress()));
        Collection<Tunnel> tunnelQueryResult = tunnelService.queryTunnel(tunnelEndPointSrc, tunnelEndPointDst);

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
                    break;
                }
            }
        }

        if (tunnel == null || tunnel.annotations().value(PLSP_ID) != null) {
            return tunnel;
        }

        // The returned tunnel is used just for filling values in Label message. So manipulate locally
        // and return so that to allocate label, we don't need to wait for the tunnel in the "core"
        // to be updated, as that depends on listener mechanism and there may be timing/multi-threading issues.
        Builder annotationBuilder = DefaultAnnotations.builder();
        annotationBuilder.set(BANDWIDTH, tunnel.annotations().value(BANDWIDTH));
        annotationBuilder.set(COST_TYPE, tunnel.annotations().value(COST_TYPE));
        annotationBuilder.set(LSP_SIG_TYPE, tunnel.annotations().value(LSP_SIG_TYPE));
        annotationBuilder.set(PCE_INIT, tunnel.annotations().value(PCE_INIT));
        annotationBuilder.set(DELEGATE, tunnel.annotations().value(DELEGATE));
        annotationBuilder.set(PLSP_ID, String.valueOf(lspObj.getPlspId()));
        annotationBuilder.set(PCC_TUNNEL_ID, String.valueOf(ipv4LspIdenTlv.getTunnelId()));
        annotationBuilder.set(LOCAL_LSP_ID, tunnel.annotations().value(LOCAL_LSP_ID));

        Tunnel updatedTunnel = new DefaultTunnel(tunnel.providerId(), tunnel.src(),
                            tunnel.dst(), tunnel.type(),
                            tunnel.state(), tunnel.groupId(),
                            tunnel.tunnelId(),
                            tunnel.tunnelName(),
                            tunnel.path(),
                            tunnel.resource(),
                            annotationBuilder.build());

        return updatedTunnel;
    }

    @Override
    public void closeConnectedClients() {
        PcepClient pc;
        for (PccId id : connectedClients.keySet()) {
            pc = getClient(id);
            pc.disconnectClient();
        }
    }

    /**
     * Returns pcep error message with specific error type and value.
     *
     * @param factory represents pcep factory
     * @param errorType pcep error type
     * @param errorValue pcep error value
     * @return pcep error message
     */
    public PcepErrorMsg getErrMsg(PcepFactory factory, byte errorType, byte errorValue) {
        LinkedList<PcepError> llPcepErr = new LinkedList<>();

        LinkedList<PcepErrorObject> llerrObj = new LinkedList<>();
        PcepErrorMsg errMsg;

        PcepErrorObject errObj = factory.buildPcepErrorObject().setErrorValue(errorValue).setErrorType(errorType)
                .build();

        llerrObj.add(errObj);
        PcepError pcepErr = factory.buildPcepError().setErrorObjList(llerrObj).build();

        llPcepErr.add(pcepErr);

        PcepErrorInfo errInfo = factory.buildPcepErrorInfo().setPcepErrorList(llPcepErr).build();

        errMsg = factory.buildPcepErrorMsg().setPcepErrorInfo(errInfo).build();
        return errMsg;
    }

    private boolean syncLabelDb(DeviceId deviceId) throws PcepParseException {
        checkNotNull(deviceId);

        DeviceId actualDevcieId = pceStore.getLsrIdDevice(deviceId.toString());
        if (actualDevcieId == null) {
            log.error("Device not available {}.", deviceId.toString());
            pceStore.addPccLsr(deviceId);
            return false;
        }
        PcepClient pc = connectedClients.get(PccId.pccId(IpAddress.valueOf(deviceId.toString())));

        Device specificDevice = deviceService.getDevice(actualDevcieId);
        if (specificDevice == null) {
            log.error("Unable to find device for specific device id {}.", actualDevcieId.toString());
            return false;
        }

        if (pceStore.getGlobalNodeLabel(actualDevcieId) != null) {
            Map<DeviceId, LabelResourceId> globalNodeLabelMap = pceStore.getGlobalNodeLabels();

            for (Entry<DeviceId, LabelResourceId> entry : globalNodeLabelMap.entrySet()) {

                // Convert from DeviceId to TunnelEndPoint
                Device srcDevice = deviceService.getDevice(entry.getKey());

                /*
                 * If there is a slight difference in timing such that if device subsystem has removed the device but
                 * PCE store still has it, just ignore such devices.
                 */
                if (srcDevice == null) {
                    continue;
                }

                String srcLsrId = srcDevice.annotations().value(LSRID);
                if (srcLsrId == null) {
                    continue;
                }

                srTeHandler.pushGlobalNodeLabel(pc, entry.getValue(),
                                    IpAddress.valueOf(srcLsrId).getIp4Address().toInt(),
                                    PcepLabelOp.ADD, false);
            }

            Map<Link, LabelResourceId> adjLabelMap = pceStore.getAdjLabels();
            for (Entry<Link, LabelResourceId> entry : adjLabelMap.entrySet()) {
                if (entry.getKey().src().deviceId().equals(actualDevcieId)) {
                    srTeHandler.pushAdjacencyLabel(pc,
                                       entry.getValue(),
                                       (int) entry.getKey().src().port().toLong(),
                                       (int) entry.getKey().dst().port().toLong(),
                                       PcepLabelOp.ADD
                                       );
                }
            }
        }

        srTeHandler.pushGlobalNodeLabel(pc, LabelResourceId.labelResourceId(0),
                            0, PcepLabelOp.ADD, true);

        log.debug("End of label DB sync for device {}", actualDevcieId);

        if (mastershipService.getLocalRole(specificDevice.id()) == MastershipRole.MASTER) {
            // Allocate node-label to this specific device.
            allocateNodeLabel(specificDevice);

            // Allocate adjacency label
            Set<Link> links = linkService.getDeviceEgressLinks(specificDevice.id());
            if (links != null) {
                for (Link link : links) {
                    allocateAdjacencyLabel(link);
                }
            }
        }
        return true;
    }

    /**
     * Allocates node label to specific device.
     *
     * @param specificDevice device to which node label needs to be allocated
     */
    public void allocateNodeLabel(Device specificDevice) {
        checkNotNull(specificDevice, DEVICE_NULL);

        DeviceId deviceId = specificDevice.id();

        // Retrieve lsrId of a specific device
        if (specificDevice.annotations() == null) {
            log.debug("Device {} does not have annotations.", specificDevice.toString());
            return;
        }

        String lsrId = specificDevice.annotations().value(LSRID);
        if (lsrId == null) {
            log.debug("Unable to retrieve lsr-id of a device {}.", specificDevice.toString());
            return;
        }

        // Get capability config from netconfig
        DeviceCapability cfg = netCfgService.getConfig(DeviceId.deviceId(lsrId), DeviceCapability.class);
        if (cfg == null) {
            log.error("Unable to find corresponding capability for a lsrd {} from NetConfig.", lsrId);
            // Save info. When PCEP session is comes up then allocate node-label
            lsrIdDeviceIdMap.put(lsrId, specificDevice.id());
            return;
        }

        // Check whether device has SR-TE Capability
        if (cfg.labelStackCap()) {
            srTeHandler.allocateNodeLabel(deviceId, lsrId);
        }
    }

    /**
     * Releases node label of a specific device.
     *
     * @param specificDevice this device label and lsr-id information will be
     *            released in other existing devices
     */
    public void releaseNodeLabel(Device specificDevice) {
        checkNotNull(specificDevice, DEVICE_NULL);

        DeviceId deviceId = specificDevice.id();

        // Retrieve lsrId of a specific device
        if (specificDevice.annotations() == null) {
            log.debug("Device {} does not have annotations.", specificDevice.toString());
            return;
        }

        String lsrId = specificDevice.annotations().value(LSRID);
        if (lsrId == null) {
            log.debug("Unable to retrieve lsr-id of a device {}.", specificDevice.toString());
            return;
        }

        // Get capability config from netconfig
        DeviceCapability cfg = netCfgService.getConfig(DeviceId.deviceId(lsrId), DeviceCapability.class);
        if (cfg == null) {
            log.error("Unable to find corresponding capabilty for a lsrd {} from NetConfig.", lsrId);
            return;
        }

        // Check whether device has SR-TE Capability
        if (cfg.labelStackCap()) {
            if (!srTeHandler.releaseNodeLabel(deviceId, lsrId)) {
                log.error("Unable to release node label for a device id {}.", deviceId.toString());
            }
        }
    }

    /**
     * Allocates adjacency label for a link.
     *
     * @param link link
     */
    public void allocateAdjacencyLabel(Link link) {
        checkNotNull(link, LINK_NULL);

        Device specificDevice = deviceService.getDevice(link.src().deviceId());

        // Retrieve lsrId of a specific device
        if (specificDevice.annotations() == null) {
            log.debug("Device {} does not have annotations.", specificDevice.toString());
            return;
        }

        String lsrId = specificDevice.annotations().value(LSRID);
        if (lsrId == null) {
            log.debug("Unable to retrieve lsr-id of a device {}.", specificDevice.toString());
            return;
        }

        // Get capability config from netconfig
        DeviceCapability cfg = netCfgService.getConfig(DeviceId.deviceId(lsrId), DeviceCapability.class);
        if (cfg == null) {
            log.error("Unable to find corresponding capabilty for a lsrd {} from NetConfig.", lsrId);
            // Save info. When PCEP session comes up then allocate adjacency
            // label
            if (lsrIdDeviceIdMap.get(lsrId) != null) {
                lsrIdDeviceIdMap.put(lsrId, specificDevice.id());
            }
            return;
        }

        // Check whether device has SR-TE Capability
        if (cfg.labelStackCap()) {
            srTeHandler.allocateAdjacencyLabel(link);
        }
    }

    /**
     * Releases allocated adjacency label of a link.
     *
     * @param link link
     */
    public void releaseAdjacencyLabel(Link link) {
        checkNotNull(link, LINK_NULL);

        Device specificDevice = deviceService.getDevice(link.src().deviceId());

        // Retrieve lsrId of a specific device
        if (specificDevice.annotations() == null) {
            log.debug("Device {} does not have annotations.", specificDevice.toString());
            return;
        }

        String lsrId = specificDevice.annotations().value(LSRID);
        if (lsrId == null) {
            log.debug("Unable to retrieve lsr-id of a device {}.", specificDevice.toString());
            return;
        }

        // Get capability config from netconfig
        DeviceCapability cfg = netCfgService.getConfig(DeviceId.deviceId(lsrId), DeviceCapability.class);
        if (cfg == null) {
            log.error("Unable to find corresponding capabilty for a lsrd {} from NetConfig.", lsrId);
            return;
        }

        // Check whether device has SR-TE Capability
        if (cfg.labelStackCap()) {
            if (!srTeHandler.releaseAdjacencyLabel(link)) {
                log.error("Unable to release adjacency labels for a link {}.", link.toString());
            }
        }
    }

    @Override
    public LabelStack computeLabelStack(Path path) {
        return srTeHandler.computeLabelStack(path);
    }

    @Override
    public boolean allocateLocalLabel(Tunnel tunnel) {
        return crHandler.allocateLabel(tunnel);
    }

    /**
     * Creates label stack for ERO object from network resource.
     *
     * @param labelStack
     * @param path (hop list)
     * @return list of ERO subobjects
     */
    @Override
    public LinkedList<PcepValueType> createPcepLabelStack(DefaultLabelStack labelStack, Path path) {
        checkNotNull(labelStack);

        LinkedList<PcepValueType> llSubObjects = new LinkedList<PcepValueType>();
        Iterator<Link> links = path.links().iterator();
        LabelResourceId label = null;
        Link link = null;
        PcepValueType subObj = null;
        PcepNai nai = null;
        Device dstNode = null;
        long srcPortNo, dstPortNo;

        ListIterator<LabelResourceId> labelListIterator = labelStack.labelResources().listIterator();
        while (labelListIterator.hasNext()) {
            label = labelListIterator.next();
            link = links.next();

            srcPortNo = link.src().port().toLong();
            srcPortNo = ((srcPortNo & IDENTIFIER_SET) == IDENTIFIER_SET) ? srcPortNo & SET : srcPortNo;

            dstPortNo = link.dst().port().toLong();
            dstPortNo = ((dstPortNo & IDENTIFIER_SET) == IDENTIFIER_SET) ? dstPortNo & SET : dstPortNo;

            nai = new PcepNaiIpv4Adjacency((int) srcPortNo, (int) dstPortNo);
            subObj = new SrEroSubObject(PcepNaiIpv4Adjacency.ST_TYPE, false, false, false, true, (int) label.labelId(),
                                        nai);
            llSubObjects.add(subObj);

            dstNode = deviceService.getDevice(link.dst().deviceId());
            nai = new PcepNaiIpv4NodeId(Ip4Address.valueOf(dstNode.annotations().value(LSRID)).toInt());

            if (!labelListIterator.hasNext()) {
                log.error("Malformed label stack.");
            }
            label = labelListIterator.next();
            subObj = new SrEroSubObject(PcepNaiIpv4NodeId.ST_TYPE, false, false, false, true, (int) label.labelId(),
                                        nai);
            llSubObjects.add(subObj);
        }
        return llSubObjects;
    }

    /**
     * Implementation of an Pcep Agent which is responsible for
     * keeping track of connected clients and the state in which
     * they are.
     */
    public class PcepClientAgent implements PcepAgent {

        private final Logger log = LoggerFactory.getLogger(PcepClientAgent.class);

        @Override
        public boolean addConnectedClient(PccId pccId, PcepClient pc) {

            if (connectedClients.get(pccId) != null) {
                log.error("Trying to add connectedClient but found a previous "
                        + "value for pcc ip: {}", pccId.toString());
                return false;
            } else {
                log.debug("Added Client {}", pccId.toString());
                connectedClients.put(pccId, pc);
                for (PcepClientListener l : pcepClientListener) {
                    l.clientConnected(pccId);
                }
                return true;
            }
        }

        @Override
        public boolean validActivation(PccId pccId) {
            if (connectedClients.get(pccId) == null) {
                log.error("Trying to activate client but is not in "
                        + "connected client: pccIp {}. Aborting ..", pccId.toString());
                return false;
            }
            return true;
        }

        @Override
        public void removeConnectedClient(PccId pccId) {

            connectedClients.remove(pccId);
            for (PcepClientListener l : pcepClientListener) {
                log.warn("removal for {}", pccId.toString());
                l.clientDisconnected(pccId);
            }
        }

        @Override
        public void processPcepMessage(PccId pccId, PcepMessage m) {
            processClientMessage(pccId, m);
        }

        @Override
        public void addNode(PcepClient pc) {
            for (PcepNodeListener l : pcepNodeListener) {
                l.addDevicePcepConfig(pc);
            }
        }

        @Override
        public void deleteNode(PccId pccId) {
            for (PcepNodeListener l : pcepNodeListener) {
                l.deleteDevicePcepConfig(pccId);
            }
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public boolean analyzeSyncMsgList(PccId pccId) {
            PcepClient pc = getClient(pccId);
            /*
             * PLSP_ID is null while tunnel is created at PCE and PCInit msg carries it as 0. It is allocated by PCC and
             * in that case it becomes the first PCRpt msg from PCC for this LSP, and hence symbolic path name must be
             * carried in the PCRpt msg. Draft says: The SYMBOLIC-PATH-NAME TLV "MUST" be included in the LSP object in
             * the LSP State Report (PCRpt) message when during a given PCEP session an LSP is "first" reported to a
             * PCE. So two separate lists with separate keys are maintained.
             */
            Map<LspKey, Tunnel> preSyncLspDbByKey = new HashMap<>();
            Map<String, Tunnel> preSyncLspDbByName = new HashMap<>();

            // Query tunnel service and fetch all the tunnels with this PCC as ingress.
            // Organize into two maps, with LSP key if known otherwise with symbolic path name, for quick search.
            Collection<Tunnel> queriedTunnels = tunnelService.queryTunnel(Tunnel.Type.MPLS);
            for (Tunnel tunnel : queriedTunnels) {
                if (((IpTunnelEndPoint) tunnel.src()).ip().equals(pccId.ipAddress())) {
                    String pLspId = tunnel.annotations().value(PLSP_ID);
                    if (pLspId != null) {
                        String localLspId = tunnel.annotations().value(LOCAL_LSP_ID);
                        checkNotNull(localLspId);
                        LspKey lspKey = new LspKey(Integer.valueOf(pLspId), Short.valueOf(localLspId));
                        preSyncLspDbByKey.put(lspKey, tunnel);
                    } else {
                        preSyncLspDbByName.put(tunnel.tunnelName().value(), tunnel);
                    }
                }
            }

            List<PcepStateReport> syncStateRptList = pc.getSyncMsgList(pccId);
            if (syncStateRptList == null) {
                // When there are no LSPs to sync, directly end-of-sync PCRpt will come and the
                // list will be null.
                syncStateRptList = Collections.EMPTY_LIST;
                log.debug("No LSPs reported from PCC during sync.");
            }

            Iterator<PcepStateReport> stateRptListIterator = syncStateRptList.iterator();

            // For every report, fetch PLSP id, local LSP id and symbolic path name from the message.
            while (stateRptListIterator.hasNext()) {
                PcepStateReport stateRpt = stateRptListIterator.next();
                Tunnel tunnel = null;

                PcepLspObject lspObj = stateRpt.getLspObject();
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

                LspKey lspKeyOfRpt = new LspKey(lspObj.getPlspId(), ipv4LspIdenTlv.getLspId());
                tunnel = preSyncLspDbByKey.get(lspKeyOfRpt);
                // PCE tunnel is matched with PCRpt LSP. Now delete it from the preSyncLspDb list as the residual
                // non-matching list will be processed at the end.
                if (tunnel != null) {
                    preSyncLspDbByKey.remove(lspKeyOfRpt);
                } else if (pathNameTlv != null) {
                    tunnel = preSyncLspDbByName.get(Arrays.toString(pathNameTlv.getValue()));
                    if (tunnel != null) {
                        preSyncLspDbByName.remove(tunnel.tunnelName().value());
                    }
                }

                if (tunnel == null) {
                    // If remove flag is set, and tunnel is not known to PCE, ignore it.
                    if (lspObj.getCFlag() && !lspObj.getRFlag()) {
                        // For initiated LSP, need to send PCInit delete msg.
                        try {
                            PcepSrpObject srpobj = pc.factory().buildSrpObject().setSrpID(SrpIdGenerators.create())
                                    .setRFlag(true).build();
                            PcInitiatedLspRequest releaseLspRequest = pc.factory().buildPcInitiatedLspRequest()
                                    .setLspObject(lspObj).setSrpObject(srpobj).build();
                            LinkedList<PcInitiatedLspRequest> llPcInitiatedLspRequestList
                                    = new LinkedList<PcInitiatedLspRequest>();
                            llPcInitiatedLspRequestList.add(releaseLspRequest);

                            PcepInitiateMsg pcInitiateMsg = pc.factory().buildPcepInitiateMsg()
                                    .setPcInitiatedLspRequestList(llPcInitiatedLspRequestList).build();

                            pc.sendMessage(Collections.singletonList(pcInitiateMsg));
                        } catch (PcepParseException e) {
                            log.error("Exception occured while sending initiate delete message {}", e.getMessage());
                        }
                        continue;
                    }
                }

                if (!lspObj.getCFlag()) {
                    // For learned LSP process both add/update PCRpt.
                    LinkedList<PcepStateReport> llPcRptList = new LinkedList<>();
                    llPcRptList.add(stateRpt);
                    PcepMessage pcReportMsg = pc.factory().buildReportMsg().setStateReportList((llPcRptList))
                            .build();

                    for (PcepEventListener l : pcepEventListener) {
                        l.handleMessage(pccId, pcReportMsg);
                    }
                    continue;
                }

                // Implied that tunnel != null and lspObj.getCFlag() is set
                // State different for PCC sent LSP and PCE known LSP, send PCUpd msg.
                State tunnelState = PcepLspStatus
                        .getTunnelStatusFromLspStatus(PcepLspStatus.values()[lspObj.getOFlag()]);
                if (tunnelState != tunnel.state()) {
                    for (PcepEventListener l : pcepEventListener) {
                        l.handleEndOfSyncAction(tunnel, SEND_UPDATE);
                    }
                }
            }

            // Check which tunnels are extra at PCE that were not reported by PCC.
            Map<Object, Tunnel> preSyncLspDb = (Map) preSyncLspDbByKey;
            handleResidualTunnels(preSyncLspDb);
            preSyncLspDbByKey = null;

            preSyncLspDb = (Map) preSyncLspDbByName;
            handleResidualTunnels(preSyncLspDb);
            preSyncLspDbByName = null;
            preSyncLspDb = null;

            pc.removeSyncMsgList(pccId);
            return true;
        }

        /*
         * Go through the tunnels which are known by PCE but were not reported by PCC during LSP DB sync and take
         * appropriate actions.
         */
        private void handleResidualTunnels(Map<Object, Tunnel> preSyncLspDb) {
            for (Tunnel pceExtraTunnel : preSyncLspDb.values()) {
                if (pceExtraTunnel.annotations().value(PCE_INIT) == null
                        || "false".equalsIgnoreCase(pceExtraTunnel.annotations().value(PCE_INIT))) {
                    // PCC initiated tunnels should be removed from tunnel store.
                    for (PcepEventListener l : pcepEventListener) {
                        l.handleEndOfSyncAction(pceExtraTunnel, REMOVE);
                    }
                } else {
                    // PCE initiated tunnels should be initiated again.
                    for (PcepEventListener l : pcepEventListener) {
                        l.handleEndOfSyncAction(pceExtraTunnel, UNSTABLE);
                    }
                }
            }
        }
    }

    /*
     * Handle device events.
     */
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            Device specificDevice = event.subject();
            if (specificDevice == null) {
                log.error("Unable to find device from device event.");
                return;
            }

            switch (event.type()) {

            case DEVICE_ADDED:
                // Node-label allocation is being done during Label DB Sync.
                // So, when device is detected, no need to do node-label
                // allocation.
                String lsrId = specificDevice.annotations().value(LSRID);
                if (lsrId != null) {
                    pceStore.addLsrIdDevice(lsrId, specificDevice.id());

                    // Search in failed DB sync store. If found, trigger label DB sync.
                    DeviceId pccDeviceId = DeviceId.deviceId(lsrId);
                    if (pceStore.hasPccLsr(pccDeviceId)) {
                        log.debug("Continue to perform label DB sync for device {}.", pccDeviceId.toString());
                        try {
                            syncLabelDb(pccDeviceId);
                        } catch (PcepParseException e) {
                            log.error("Exception caught in sending label masg to PCC while in sync.");
                        }
                        pceStore.removePccLsr(pccDeviceId);
                    }
                }
                break;

            case DEVICE_REMOVED:
                // Release node-label
                if (mastershipService.getLocalRole(specificDevice.id()) == MastershipRole.MASTER) {
                    releaseNodeLabel(specificDevice);
                }

                if (specificDevice.annotations().value(LSRID) != null) {
                    pceStore.removeLsrIdDevice(specificDevice.annotations().value(LSRID));
                }
                break;

            default:
                break;
            }
        }
    }

    /*
     * Handle link events.
     */
    private class InternalLinkListener implements LinkListener {
        @Override
        public void event(LinkEvent event) {
            Link link = event.subject();

            switch (event.type()) {

            case LINK_ADDED:
                // Allocate adjacency label
                if (mastershipService.getLocalRole(link.src().deviceId()) == MastershipRole.MASTER) {
                    allocateAdjacencyLabel(link);
                }
                break;

            case LINK_REMOVED:
                // Release adjacency label
                if (mastershipService.getLocalRole(link.src().deviceId()) == MastershipRole.MASTER) {
                    releaseAdjacencyLabel(link);
                }
                break;

            default:
                break;
            }
        }
    }

    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {

            if ((event.type() == NetworkConfigEvent.Type.CONFIG_ADDED)
                    && event.configClass().equals(DeviceCapability.class)) {

                DeviceId deviceIdLsrId = (DeviceId) event.subject();
                String lsrId = deviceIdLsrId.toString();
                DeviceId deviceId = lsrIdDeviceIdMap.get(lsrId);
                if (deviceId == null) {
                    log.debug("Unable to find device id for a lsr-id {} from lsr-id and device-id map.", lsrId);
                    return;
                }

                DeviceCapability cfg = netCfgService.getConfig(DeviceId.deviceId(lsrId), DeviceCapability.class);
                if (cfg == null) {
                    log.error("Unable to find corresponding capabilty for a lsrd {}.", lsrId);
                    return;
                }

                if (cfg.labelStackCap()) {
                    if (mastershipService.getLocalRole(deviceId) == MastershipRole.MASTER) {
                        // Allocate node-label
                        srTeHandler.allocateNodeLabel(deviceId, lsrId);

                        // Allocate adjacency label to links which are
                        // originated from this specific device id
                        Set<Link> links = linkService.getDeviceEgressLinks(deviceId);
                        for (Link link : links) {
                            if (!srTeHandler.allocateAdjacencyLabel(link)) {
                                return;
                            }
                        }
                    }
                }
                // Remove lsrId info from map
                lsrIdDeviceIdMap.remove(lsrId);
            }
        }
    }
}
