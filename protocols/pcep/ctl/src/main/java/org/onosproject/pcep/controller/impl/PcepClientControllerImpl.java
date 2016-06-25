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
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.incubator.net.tunnel.IpTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.incubator.net.tunnel.Tunnel.State;
import org.onosproject.net.device.DeviceService;
import org.onosproject.pcep.controller.LspKey;
import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcep.controller.PcepClient;
import org.onosproject.pcep.controller.PcepClientController;
import org.onosproject.pcep.controller.PcepClientListener;
import org.onosproject.pcep.controller.PcepEventListener;
import org.onosproject.pcep.controller.PcepLspStatus;
import org.onosproject.pcep.controller.PcepNodeListener;
import org.onosproject.pcep.controller.PcepPacketListener;
import org.onosproject.pcep.controller.PcepSyncStatus;
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
import org.onosproject.pcepio.protocol.PcepReportMsg;
import org.onosproject.pcepio.protocol.PcepSrpObject;
import org.onosproject.pcepio.protocol.PcepStateReport;
import org.onosproject.pcepio.types.PcepValueType;
import org.onosproject.pcepio.types.StatefulIPv4LspIdentifiersTlv;
import org.onosproject.pcepio.types.SymbolicPathNameTlv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import static com.google.common.base.Preconditions.checkNotNull;

import static org.onosproject.pcep.controller.PcepSyncStatus.IN_SYNC;
import static org.onosproject.pcep.controller.PcepLspSyncAction.REMOVE;
import static org.onosproject.pcep.controller.PcepLspSyncAction.SEND_UPDATE;
import static org.onosproject.pcep.controller.PcepLspSyncAction.UNSTABLE;
import static org.onosproject.pcepio.types.PcepErrorDetailInfo.ERROR_TYPE_19;
import static org.onosproject.pcepio.types.PcepErrorDetailInfo.ERROR_VALUE_5;

/**
 * Implementation of PCEP client controller.
 */
@Component(immediate = true)
@Service
public class PcepClientControllerImpl implements PcepClientController {

    private static final Logger log = LoggerFactory.getLogger(PcepClientControllerImpl.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    protected ConcurrentHashMap<PccId, PcepClient> connectedClients =
            new ConcurrentHashMap<>();

    protected PcepClientAgent agent = new PcepClientAgent();
    protected Set<PcepClientListener> pcepClientListener = new HashSet<>();

    protected Set<PcepEventListener> pcepEventListener = Sets.newHashSet();
    protected Set<PcepNodeListener> pcepNodeListener = Sets.newHashSet();
    protected Set<PcepPacketListener> pcepPacketListener = Sets.newHashSet();

    private final Controller ctrl = new Controller();

    public static final String BANDWIDTH = "bandwidth";
    public static final String LSP_SIG_TYPE = "lspSigType";
    public static final String PCC_TUNNEL_ID = "PccTunnelId";
    public static final String PLSP_ID = "PLspId";
    public static final String LOCAL_LSP_ID = "localLspId";
    public static final String PCE_INIT = "pceInit";
    public static final String COST_TYPE = "costType";
    public static final String DELEGATE = "delegation";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TunnelService tunnelService;

    @Activate
    public void activate() {
        ctrl.start(agent);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        // Close all connected clients
        closeConnectedClients();
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
    public void addPacketListener(PcepPacketListener listener) {
        pcepPacketListener.add(listener);
    }

    @Override
    public void removePacketListener(PcepPacketListener listener) {
        pcepPacketListener.remove(listener);
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
                        if (pc.lspDbSyncStatus() != PcepSyncStatus.IN_SYNC) {
                            log.debug("LSP DB sync started for PCC {}", pc.getPccId().id().toString());
                            // Initialize LSP DB sync and temporary cache.
                            pc.setLspDbSyncStatus(PcepSyncStatus.IN_SYNC);
                            pc.initializeSyncMsgList(pccId);
                        }
                        // Store stateRpt in temporary cache.
                        pc.addSyncMsgToList(pccId, stateRpt);

                        // Don't send to provider as of now.
                        continue;
                    } else if (lspObj.getPlspId() == 0) {
                        if (pc.lspDbSyncStatus() == PcepSyncStatus.IN_SYNC
                                || pc.lspDbSyncStatus() == PcepSyncStatus.NOT_SYNCED) {
                            // Set end of LSPDB sync.
                            log.debug("LSP DB sync completed for PCC {}", pc.getPccId().id().toString());
                            pc.setLspDbSyncStatus(PcepSyncStatus.SYNCED);

                            // Call packet provider to initiate label DB sync (only if PCECC capable).
                            if (pc.capability().pceccCapability()) {
                                log.debug("Trigger label DB sync for PCC {}", pc.getPccId().id().toString());
                                pc.setLabelDbSyncStatus(IN_SYNC);
                                for (PcepPacketListener l : pcepPacketListener) {
                                    l.sendPacketIn(pccId);
                                }
                            } else {
                                // If label db sync is not to be done, handle end of LSPDB sync actions.
                                agent.analyzeSyncMsgList(pccId);
                            }
                            continue;
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
                        preSyncLspDbByName.remove(tunnel.tunnelName());
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
}
