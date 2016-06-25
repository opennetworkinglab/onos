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
package org.onosproject.provider.bgpcep.flow.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MplsLabel;
import org.onosproject.bgp.controller.BgpController;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.tunnel.IpTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.CompletedBatchOperation;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleBatchEntry;
import org.onosproject.net.flow.FlowRuleBatchOperation;
import org.onosproject.net.flow.FlowRuleProvider;
import org.onosproject.net.flow.FlowRuleProviderRegistry;
import org.onosproject.net.flow.FlowRuleProviderService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.FlowEntry.FlowEntryState;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.IPCriterion;
import org.onosproject.net.flow.criteria.MetadataCriterion;
import org.onosproject.net.flow.criteria.MplsBosCriterion;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.TunnelIdCriterion;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcep.controller.PcepClient;
import org.onosproject.pcep.controller.PcepClientController;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepEroObject;
import org.onosproject.pcepio.protocol.PcepFecObjectIPv4;
import org.onosproject.pcepio.protocol.PcepFecObjectIPv4Adjacency;
import org.onosproject.pcepio.protocol.PcepLabelObject;
import org.onosproject.pcepio.protocol.PcepLabelUpdate;
import org.onosproject.pcepio.protocol.PcepLabelUpdateMsg;
import org.onosproject.pcepio.protocol.PcepLspObject;
import org.onosproject.pcepio.protocol.PcepMsgPath;
import org.onosproject.pcepio.protocol.PcepSrpObject;
import org.onosproject.pcepio.protocol.PcepUpdateMsg;
import org.onosproject.pcepio.protocol.PcepUpdateRequest;
import org.onosproject.pcepio.types.IPv4SubObject;
import org.onosproject.pcepio.types.NexthopIPv4addressTlv;
import org.onosproject.pcepio.types.PathSetupTypeTlv;
import org.onosproject.pcepio.types.PcepLabelDownload;
import org.onosproject.pcepio.types.PcepLabelMap;
import org.onosproject.pcepio.types.PcepValueType;
import org.onosproject.pcepio.types.StatefulIPv4LspIdentifiersTlv;
import org.onosproject.pcepio.types.SymbolicPathNameTlv;
import org.onosproject.pcep.controller.LspType;
import org.onosproject.pcep.controller.SrpIdGenerators;
import org.onosproject.pcep.controller.PcepAnnotationKeys;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import static org.onosproject.pcep.controller.PcepAnnotationKeys.DELEGATE;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.LSP_SIG_TYPE;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.PCE_INIT;
import static org.onosproject.pcep.controller.PcepSyncStatus.IN_SYNC;
import static org.onosproject.pcep.controller.PcepSyncStatus.SYNCED;
import static org.onosproject.net.flow.criteria.Criterion.Type.EXTENSION;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of BGP-PCEP flow provider.
 */
@Component(immediate = true)
public class BgpcepFlowRuleProvider extends AbstractProvider
        implements FlowRuleProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected BgpController bgpController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PcepClientController pcepController;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ResourceService resourceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TunnelService tunnelService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private FlowRuleProviderService providerService;
    private PcepLabelObject labelObj;
    public static final int OUT_LABEL_TYPE = 0;
    public static final int IN_LABEL_TYPE = 1;
    public static final long IDENTIFIER_SET = 0x100000000L;
    public static final long SET = 0xFFFFFFFFL;
    private static final String LSRID = "lsrId";

    private enum PcepFlowType {
        ADD,
        MODIFY,
        REMOVE
    }

    /**
     * Creates a BgpFlow host provider.
     */
    public BgpcepFlowRuleProvider() {
        super(new ProviderId("l3", "org.onosproject.provider.bgpcep"));
    }

    @Activate
    public void activate(ComponentContext context) {
        providerService = providerRegistry.register(this);
        log.info("Started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        providerRegistry.unregister(this);
        providerService = null;
        log.info("Stopped");
    }

    @Override
    public void applyFlowRule(FlowRule... flowRules) {
        for (FlowRule flowRule : flowRules) {
            processRule(flowRule, PcepFlowType.ADD);
        }
    }

    @Override
    public void removeFlowRule(FlowRule... flowRules) {
        for (FlowRule flowRule : flowRules) {
            processRule(flowRule, PcepFlowType.REMOVE);
        }
    }

    private void processRule(FlowRule flowRule, PcepFlowType type) {
        MplsLabel mplsLabel = null;
        IpPrefix ip4PrefixSrc = null;
        IpPrefix ip4PrefixDst = null;
        PortNumber port = null;
        TunnelId tunnelId = null;
        long labelType = 0;
        boolean bottomOfStack = false;

        TrafficSelector selector = flowRule.selector();
        for (Criterion c : selector.criteria()) {
            switch (c.type()) {
            case MPLS_LABEL:
                MplsCriterion lc = (MplsCriterion) c;
                mplsLabel = lc.label();
                break;
            case IPV4_SRC:
                IPCriterion ipCriterion = (IPCriterion) c;
                ip4PrefixSrc = ipCriterion.ip().getIp4Prefix();
                break;
            case IPV4_DST:
                ipCriterion = (IPCriterion) c;
                ip4PrefixDst = ipCriterion.ip().getIp4Prefix();
                break;
            case IN_PORT:
                PortCriterion inPort = (PortCriterion) c;
                port = inPort.port();
                break;
            case TUNNEL_ID:
                TunnelIdCriterion tc = (TunnelIdCriterion) c;
                tunnelId = TunnelId.valueOf(String.valueOf(tc.tunnelId()));
                break;
            case METADATA:
                MetadataCriterion metadata = (MetadataCriterion) c;
                labelType = metadata.metadata();
                break;
            case MPLS_BOS:
                MplsBosCriterion mplsBos = (MplsBosCriterion) c;
                bottomOfStack = mplsBos.mplsBos();
                break;
            default:
                break;
            }
        }

        checkNotNull(mplsLabel);
        LabelResourceId label = LabelResourceId.labelResourceId(mplsLabel.toInt());

        try {
            if (tunnelId != null) {
                pushLocalLabels(flowRule.deviceId(), label, port, tunnelId, bottomOfStack, labelType, type);
                return;
            }

            if (ip4PrefixDst != null) {
                pushAdjacencyLabel(flowRule.deviceId(), label, ip4PrefixSrc, ip4PrefixDst, type);
                return;
            }

            pushGlobalNodeLabel(flowRule.deviceId(), label, ip4PrefixSrc, type, bottomOfStack);

        } catch (PcepParseException e) {
            log.error("Exception occured while sending label message to PCC {}", e.getMessage());
        }

    }

    /**
     * Returns PCEP client.
     *
     * @return PCEP client
     */
    private PcepClient getPcepClient(DeviceId deviceId) {
        Device device = deviceService.getDevice(deviceId);

        // In future projections instead of annotations will be used to fetch LSR ID.
        String lsrId = device.annotations().value(LSRID);

        PcepClient pcc = pcepController.getClient(PccId.pccId(IpAddress.valueOf(lsrId)));
        return pcc;
    }

    //Pushes node labels to the specified device.
    private void pushGlobalNodeLabel(DeviceId deviceId, LabelResourceId labelId,
            IpPrefix ipPrefix, PcepFlowType type, boolean isBos) throws PcepParseException {

        checkNotNull(deviceId);
        checkNotNull(labelId);
        checkNotNull(type);

        PcepClient pc = getPcepClient(deviceId);
        if (pc == null) {
            log.error("PCEP client not found");
            return;
        }

        LinkedList<PcepLabelUpdate> labelUpdateList = new LinkedList<>();

        if (ipPrefix == null) {
            // Pushing self node label to device.
            ipPrefix = IpPrefix.valueOf(pc.getPccId().ipAddress(), 32);
        }

        PcepFecObjectIPv4 fecObject = pc.factory().buildFecObjectIpv4()
                                      .setNodeID(ipPrefix.address().getIp4Address().toInt())
                                      .build();

        boolean bSFlag = false;
        if (pc.labelDbSyncStatus() == IN_SYNC && !isBos) {
            // Need to set sync flag in all messages till sync completes.
            bSFlag = true;
        }

        PcepSrpObject srpObj = getSrpObject(pc, type, bSFlag);

        //Global NODE-SID as label object
        PcepLabelObject labelObject = pc.factory().buildLabelObject()
                                      .setLabel((int) labelId.labelId())
                                      .build();

        PcepLabelMap labelMap = new PcepLabelMap();
        labelMap.setFecObject(fecObject);
        labelMap.setLabelObject(labelObject);
        labelMap.setSrpObject(srpObj);

        labelUpdateList.add(pc.factory().buildPcepLabelUpdateObject()
                            .setLabelMap(labelMap)
                            .build());

        PcepLabelUpdateMsg labelMsg = pc.factory().buildPcepLabelUpdateMsg()
                                      .setPcLabelUpdateList(labelUpdateList)
                                      .build();

        pc.sendMessage(labelMsg);

        if (isBos) {
            // Sync is completed.
            pc.setLabelDbSyncStatus(SYNCED);
        }
    }

    private PcepSrpObject getSrpObject(PcepClient pc, PcepFlowType type, boolean bSFlag)
            throws PcepParseException {
        PcepSrpObject srpObj;
        boolean bRFlag = false;

        if (!type.equals(PcepFlowType.ADD)) {
            // To cleanup labels, R bit is set
            bRFlag = true;
        }

        srpObj = pc.factory().buildSrpObject()
                .setRFlag(bRFlag)
                .setSFlag(bSFlag)
                .setSrpID(SrpIdGenerators.create())
                .build();

        return srpObj;
    }

    //Pushes adjacency labels to the specified device.
    private void pushAdjacencyLabel(DeviceId deviceId, LabelResourceId labelId, IpPrefix ip4PrefixSrc,
                                    IpPrefix ip4PrefixDst, PcepFlowType type)
            throws PcepParseException {

        checkNotNull(deviceId);
        checkNotNull(labelId);
        checkNotNull(ip4PrefixSrc);
        checkNotNull(ip4PrefixDst);
        checkNotNull(type);

        PcepClient pc = getPcepClient(deviceId);
        if (pc == null) {
            log.error("PCEP client not found");
            return;
        }

        LinkedList<PcepLabelUpdate> labelUpdateList = new LinkedList<>();

        int srcPortNo = ip4PrefixSrc.address().getIp4Address().toInt();
        int dstPortNo = ip4PrefixDst.address().getIp4Address().toInt();

        PcepFecObjectIPv4Adjacency fecAdjObject = pc.factory().buildFecIpv4Adjacency()
                                                  .seRemoteIPv4Address(dstPortNo)
                                                  .seLocalIPv4Address(srcPortNo)
                                                  .build();

        boolean bSFlag = false;
        if (pc.labelDbSyncStatus() == IN_SYNC) {
            // Need to set sync flag in all messages till sync completes.
            bSFlag = true;
        }

        PcepSrpObject srpObj = getSrpObject(pc, type, bSFlag);

        //Adjacency label object
        PcepLabelObject labelObject = pc.factory().buildLabelObject()
                                      .setLabel((int) labelId.labelId())
                                      .build();

        PcepLabelMap labelMap = new PcepLabelMap();
        labelMap.setFecObject(fecAdjObject);
        labelMap.setLabelObject(labelObject);
        labelMap.setSrpObject(srpObj);

        labelUpdateList.add(pc.factory().buildPcepLabelUpdateObject()
                            .setLabelMap(labelMap)
                            .build());

        PcepLabelUpdateMsg labelMsg = pc.factory().buildPcepLabelUpdateMsg()
                                      .setPcLabelUpdateList(labelUpdateList)
                                      .build();

        pc.sendMessage(labelMsg);
    }

    //Pushes local labels to the device which is specific to path [CR-case].
    private void pushLocalLabels(DeviceId deviceId, LabelResourceId labelId,
            PortNumber portNum, TunnelId tunnelId,
            Boolean isBos, Long labelType, PcepFlowType type) throws PcepParseException {

        checkNotNull(deviceId);
        checkNotNull(labelId);
        checkNotNull(portNum);
        checkNotNull(tunnelId);
        checkNotNull(labelType);
        checkNotNull(type);

        PcepClient pc = getPcepClient(deviceId);
        if (pc == null) {
            log.error("PCEP client not found");
            return;
        }

        PcepLspObject lspObj;
        LinkedList<PcepLabelUpdate> labelUpdateList = new LinkedList<>();
        LinkedList<PcepLabelObject> labelObjects = new LinkedList<>();
        PcepSrpObject srpObj;
        PcepLabelDownload labelDownload = new PcepLabelDownload();
        LinkedList<PcepValueType> optionalTlv = new LinkedList<>();

        long portNo = portNum.toLong();
        portNo = ((portNo & IDENTIFIER_SET) == IDENTIFIER_SET) ? portNo & SET : portNo;

        optionalTlv.add(NexthopIPv4addressTlv.of((int) portNo));

        Tunnel tunnel = tunnelService.queryTunnel(tunnelId);

        PcepLabelObject labelObj = pc.factory().buildLabelObject()
                                   .setOFlag(labelType == OUT_LABEL_TYPE ? true : false)
                                   .setOptionalTlv(optionalTlv)
                                   .setLabel((int) labelId.labelId())
                                   .build();

        /**
         * Check whether transit node or not. For transit node, label update message should include IN and OUT labels.
         * Hence store IN label object and next when out label comes add IN and OUT label objects and encode label
         * update message and send to specified client.
         */
        if (!deviceId.equals(tunnel.path().src().deviceId()) && !deviceId.equals(tunnel.path().dst().deviceId())) {
            //Device is transit node
            if (labelType == IN_LABEL_TYPE) {
                //Store label object having IN label value
                this.labelObj = labelObj;
                return;
            }
            //Add IN label object
            labelObjects.add(this.labelObj);
        }

        //Add OUT label object in case of transit node
        labelObjects.add(labelObj);

        srpObj = getSrpObject(pc, type, false);

        String lspId = tunnel.annotations().value(PcepAnnotationKeys.LOCAL_LSP_ID);
        String plspId = tunnel.annotations().value(PcepAnnotationKeys.PLSP_ID);
        String tunnelIdentifier = tunnel.annotations().value(PcepAnnotationKeys.PCC_TUNNEL_ID);

        LinkedList<PcepValueType> tlvs = new LinkedList<>();
        StatefulIPv4LspIdentifiersTlv lspIdTlv = new StatefulIPv4LspIdentifiersTlv(((IpTunnelEndPoint) tunnel.src())
                .ip().getIp4Address().toInt(), Short.valueOf(lspId), Short.valueOf(tunnelIdentifier),
                ((IpTunnelEndPoint) tunnel.src()).ip().getIp4Address().toInt(),
                ((IpTunnelEndPoint) tunnel.dst()).ip().getIp4Address().toInt());
        tlvs.add(lspIdTlv);

        if (tunnel.tunnelName().value() != null) {
            SymbolicPathNameTlv pathNameTlv = new SymbolicPathNameTlv(tunnel.tunnelName().value().getBytes());
            tlvs.add(pathNameTlv);
        }

        boolean delegated = (tunnel.annotations().value(DELEGATE) == null) ? false
                                                                           : Boolean.valueOf(tunnel.annotations()
                                                                                   .value(DELEGATE));
        boolean initiated = (tunnel.annotations().value(PCE_INIT) == null) ? false
                                                                           : Boolean.valueOf(tunnel.annotations()
                                                                                   .value(PCE_INIT));

        lspObj = pc.factory().buildLspObject()
                .setRFlag(false)
                .setAFlag(true)
                .setDFlag(delegated)
                .setCFlag(initiated)
                .setPlspId(Integer.valueOf(plspId))
                .setOptionalTlv(tlvs)
                .build();

        labelDownload.setLabelList(labelObjects);
        labelDownload.setLspObject(lspObj);
        labelDownload.setSrpObject(srpObj);

        labelUpdateList.add(pc.factory().buildPcepLabelUpdateObject()
                            .setLabelDownload(labelDownload)
                            .build());

        PcepLabelUpdateMsg labelMsg = pc.factory().buildPcepLabelUpdateMsg()
                                      .setPcLabelUpdateList(labelUpdateList)
                                      .build();

        pc.sendMessage(labelMsg);

        //If isBos is true, label download is done along the LSP, send PCEP update message.
        if (isBos) {
            sendPcepUpdateMsg(pc, lspObj, tunnel);
        }
    }

    //Sends PCEP update message.
    private void sendPcepUpdateMsg(PcepClient pc, PcepLspObject lspObj, Tunnel tunnel) throws PcepParseException {
        LinkedList<PcepUpdateRequest> updateRequestList = new LinkedList<>();
        LinkedList<PcepValueType> subObjects = createEroSubObj(tunnel.path());

        if (subObjects == null) {
            log.error("ERO subjects not present");
            return;
        }

        // set PathSetupTypeTlv of SRP object
        LinkedList<PcepValueType> llOptionalTlv = new LinkedList<PcepValueType>();
        LspType lspSigType = LspType.valueOf(tunnel.annotations().value(LSP_SIG_TYPE));
        llOptionalTlv.add(new PathSetupTypeTlv(lspSigType.type()));

        PcepSrpObject srpObj = pc.factory().buildSrpObject()
                               .setRFlag(false)
                               .setSrpID(SrpIdGenerators.create())
                               .setOptionalTlv(llOptionalTlv)
                               .build();

        PcepEroObject eroObj = pc.factory().buildEroObject()
                              .setSubObjects(subObjects)
                              .build();

        PcepMsgPath msgPath = pc.factory().buildPcepMsgPath()
                              .setEroObject(eroObj)
                              .build();

        PcepUpdateRequest updateReq = pc.factory().buildPcepUpdateRequest()
                                     .setSrpObject(srpObj)
                                     .setMsgPath(msgPath)
                                     .setLspObject(lspObj)
                                     .build();

        updateRequestList.add(updateReq);

        //TODO: P = 1 is it P flag in PCEP obj header
        PcepUpdateMsg updateMsg = pc.factory().buildUpdateMsg()
                                  .setUpdateRequestList(updateRequestList)
                                  .build();

        pc.sendMessage(updateMsg);
    }

    private LinkedList<PcepValueType> createEroSubObj(Path path) {
        LinkedList<PcepValueType> subObjects = new LinkedList<>();
        List<Link> links = path.links();
        ConnectPoint source = null;
        ConnectPoint destination = null;
        IpAddress ipDstAddress = null;
        IpAddress ipSrcAddress = null;
        PcepValueType subObj = null;
        long portNo;

        for (Link link : links) {
            source = link.src();
            if (!(source.equals(destination))) {
                //set IPv4SubObject for ERO object
                portNo = source.port().toLong();
                portNo = ((portNo & IDENTIFIER_SET) == IDENTIFIER_SET) ? portNo & SET : portNo;
                ipSrcAddress = Ip4Address.valueOf((int) portNo);
                subObj = new IPv4SubObject(ipSrcAddress.getIp4Address().toInt());
                subObjects.add(subObj);
            }

            destination = link.dst();
            portNo = destination.port().toLong();
            portNo = ((portNo & IDENTIFIER_SET) == IDENTIFIER_SET) ? portNo & SET : portNo;
            ipDstAddress = Ip4Address.valueOf((int) portNo);
            subObj = new IPv4SubObject(ipDstAddress.getIp4Address().toInt());
            subObjects.add(subObj);
        }
        return subObjects;
    }

    @Override
    public void removeRulesById(ApplicationId id, FlowRule... flowRules) {
        // TODO
        removeFlowRule(flowRules);
    }

    @Override
    public void executeBatch(FlowRuleBatchOperation batch) {
        Collection<FlowEntry> flowEntries = new ArrayList<>();

        for (FlowRuleBatchEntry fbe : batch.getOperations()) {
            Criterion criteria = fbe.target().selector().getCriterion(EXTENSION);

            switch (fbe.operator()) {
            case ADD:
                if (criteria == null) {
                    processRule(fbe.target(), PcepFlowType.ADD);
                    flowEntries.add(new DefaultFlowEntry(fbe.target(), FlowEntryState.ADDED, 0, 0, 0));
                }
                break;
            case REMOVE:
                if (criteria == null) {
                    processRule(fbe.target(), PcepFlowType.REMOVE);
                    flowEntries.add(new DefaultFlowEntry(fbe.target(), FlowEntryState.REMOVED, 0, 0, 0));
                }
                break;
            default:
                log.error("Unknown flow operation: {}", fbe);
            }
        }

        CompletedBatchOperation status = new CompletedBatchOperation(true, Collections.emptySet(), batch.deviceId());
        providerService.batchOperationCompleted(batch.id(), status);
        providerService.pushFlowMetrics(batch.deviceId(), flowEntries);
    }
}
