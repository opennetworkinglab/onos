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
package org.onosproject.pcep.controller.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.LinkedList;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
import org.onosproject.incubator.net.resource.label.DefaultLabelResource;
import org.onosproject.incubator.net.resource.label.LabelResource;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.resource.label.LabelResourceService;
import org.onosproject.incubator.net.tunnel.IpTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.pcelabelstore.DefaultLspLocalLabelInfo;
import org.onosproject.pcelabelstore.PcepLabelOp;
import org.onosproject.pcelabelstore.api.LspLocalLabelInfo;
import org.onosproject.pcelabelstore.api.PceLabelStore;
import org.onosproject.pcep.controller.LspType;
import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcep.controller.PcepAnnotationKeys;
import org.onosproject.pcep.controller.PcepClient;
import org.onosproject.pcep.controller.PcepClientController;
import org.onosproject.pcep.controller.SrpIdGenerators;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepAttribute;
import org.onosproject.pcepio.protocol.PcepBandwidthObject;
import org.onosproject.pcepio.protocol.PcepEroObject;
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
import org.onosproject.pcepio.types.PcepValueType;
import org.onosproject.pcepio.types.StatefulIPv4LspIdentifiersTlv;
import org.onosproject.pcepio.types.SymbolicPathNameTlv;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import static org.onosproject.pcep.controller.PcepAnnotationKeys.BANDWIDTH;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.LSP_SIG_TYPE;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.PCE_INIT;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.DELEGATE;

/**
 * Basic PCECC handler.
 * In Basic PCECC, after path computation will configure IN and OUT label to nodes.
 * [X]OUT---link----IN[Y]OUT---link-----IN[Z] where X, Y and Z are nodes.
 * For generating labels, will go thorough links in the path from Egress to Ingress.
 * In each link, will take label from destination node local pool as IN label,
 * and assign this label as OUT label to source node.
 */
public final class BasicPceccHandler {
    private static final Logger log = LoggerFactory.getLogger(BasicPceccHandler.class);
    public static final int OUT_LABEL_TYPE = 0;
    public static final int IN_LABEL_TYPE = 1;
    public static final long IDENTIFIER_SET = 0x100000000L;
    public static final long SET = 0xFFFFFFFFL;
    private static final String LSRID = "lsrId";
    private static final String LABEL_RESOURCE_SERVICE_NULL = "Label Resource Service cannot be null";
    private static final String PCE_STORE_NULL = "PCE Store cannot be null";
    private static BasicPceccHandler crHandlerInstance = null;
    private LabelResourceService labelRsrcService;
    private DeviceService deviceService;
    private PceLabelStore pceStore;
    private PcepClientController clientController;
    private PcepLabelObject labelObj;

    /**
     * Initializes default values.
     */
    private BasicPceccHandler() {
    }

    /**
     * Returns single instance of this class.
     *
     * @return this class single instance
     */
    public static BasicPceccHandler getInstance() {
        if (crHandlerInstance == null) {
            crHandlerInstance = new BasicPceccHandler();
        }
        return crHandlerInstance;
    }

    /**
     * Initialization of label manager and pce store.
     *
     * @param labelRsrcService label resource service
     * @param pceStore pce label store
     */
    public void initialize(LabelResourceService labelRsrcService,
                           DeviceService deviceService,
                           PceLabelStore pceStore,
                           PcepClientController clientController) {
        this.labelRsrcService = labelRsrcService;
        this.deviceService = deviceService;
        this.pceStore = pceStore;
        this.clientController = clientController;
    }

    /**
     * Allocates labels from local resource pool and configure these (IN and OUT) labels into devices.
     *
     * @param tunnel tunnel between ingress to egress
     * @return success or failure
     */
    public boolean allocateLabel(Tunnel tunnel) {
        long applyNum = 1;
        boolean isLastLabelToPush = false;
        Collection<LabelResource> labelRscList;

        checkNotNull(labelRsrcService, LABEL_RESOURCE_SERVICE_NULL);
        checkNotNull(pceStore, PCE_STORE_NULL);

        List<Link> linkList = tunnel.path().links();
        if ((linkList != null) && (linkList.size() > 0)) {
            // Sequence through reverse order to push local labels into devices
            // Generation of labels from egress to ingress
            for (ListIterator<Link> iterator = linkList.listIterator(linkList.size()); iterator.hasPrevious();) {
                Link link = iterator.previous();
                DeviceId dstDeviceId = link.dst().deviceId();
                DeviceId srcDeviceId = link.src().deviceId();
                labelRscList = labelRsrcService.applyFromDevicePool(dstDeviceId, applyNum);
                if ((labelRscList != null) && (labelRscList.size() > 0)) {
                    // Link label value is taken from destination device local pool.
                    // [X]OUT---link----IN[Y]OUT---link-----IN[Z] where X, Y and Z are nodes.
                    // Link label value is used as OUT and IN for both ends
                    // (source and destination devices) of the link.
                    // Currently only one label is allocated to a device (destination device).
                    // So, no need to iterate through list
                    Iterator<LabelResource> labelIterator = labelRscList.iterator();
                    DefaultLabelResource defaultLabelResource = (DefaultLabelResource) labelIterator.next();
                    LabelResourceId labelId = defaultLabelResource.labelResourceId();
                    log.debug("Allocated local label: " + labelId.toString()
                              + "to device: " + defaultLabelResource.deviceId().toString());
                    PortNumber dstPort = link.dst().port();

                    // Check whether this is last link label to push
                    if (!iterator.hasPrevious()) {
                       isLastLabelToPush = true;
                    }

                    try {
                        // Push into destination device
                        // Destination device IN port is link.dst().port()
                        pushLocalLabels(dstDeviceId, labelId, dstPort, tunnel, false,
                                            Long.valueOf(LabelType.IN_LABEL.value), PcepLabelOp.ADD);

                        // Push into source device
                        // Source device OUT port will be link.dst().port(). Means its remote port used to send packet.
                        pushLocalLabels(srcDeviceId, labelId, dstPort, tunnel, isLastLabelToPush,
                                            Long.valueOf(LabelType.OUT_LABEL.value), PcepLabelOp.ADD);
                    } catch (PcepParseException e) {
                        log.error("Failed to push local label for device {} or {} for tunnel {}.",
                                  dstDeviceId.toString(), srcDeviceId.toString(), tunnel.tunnelName().toString());
                    }

                    // Add or update pcecc tunnel info in pce store.
                    updatePceccTunnelInfoInStore(srcDeviceId, dstDeviceId, labelId, dstPort,
                                                 tunnel);
                } else {
                    log.error("Unable to allocate label to device id {}.", dstDeviceId.toString());
                    releaseLabel(tunnel);
                    return false;
                }
            }
        } else {
           log.error("Tunnel {} is having empty links.", tunnel.toString());
           return false;
        }
        return true;
    }

    /**
     * Updates list of local labels of PCECC tunnel info in pce store.
     *
     * @param srcDeviceId source device in a link
     * @param dstDeviceId destination device in a link
     * @param labelId label id of a link
     * @param dstPort destination device port number of a link
     * @param tunnel tunnel
     */
    public void updatePceccTunnelInfoInStore(DeviceId srcDeviceId, DeviceId dstDeviceId, LabelResourceId labelId,
                                                PortNumber dstPort, Tunnel tunnel) {
       // First try to retrieve device from store and update its label id if it is exists,
       // otherwise add it
       boolean dstDeviceUpdated = false;
       boolean srcDeviceUpdated = false;

       List<LspLocalLabelInfo> lspLabelInfoList = pceStore.getTunnelInfo(tunnel.tunnelId());
       if ((lspLabelInfoList != null) && (lspLabelInfoList.size() > 0)) {
           for (int i = 0; i < lspLabelInfoList.size(); ++i) {
               LspLocalLabelInfo lspLocalLabelInfo =
                       lspLabelInfoList.get(i);
               LspLocalLabelInfo.Builder lspLocalLabelInfoBuilder = null;
               if (dstDeviceId.equals(lspLocalLabelInfo.deviceId())) {
                   lspLocalLabelInfoBuilder = DefaultLspLocalLabelInfo.builder(lspLocalLabelInfo);
                   lspLocalLabelInfoBuilder.inLabelId(labelId);
                   // Destination device IN port will be link destination port
                   lspLocalLabelInfoBuilder.inPort(dstPort);
                   dstDeviceUpdated = true;
               } else if (srcDeviceId.equals(lspLocalLabelInfo.deviceId())) {
                   lspLocalLabelInfoBuilder = DefaultLspLocalLabelInfo.builder(lspLocalLabelInfo);
                   lspLocalLabelInfoBuilder.outLabelId(labelId);
                   // Source device OUT port will be link destination (remote) port
                   lspLocalLabelInfoBuilder.outPort(dstPort);
                   srcDeviceUpdated = true;
               }

               // Update
               if ((lspLocalLabelInfoBuilder != null) && (dstDeviceUpdated || srcDeviceUpdated)) {
                   lspLabelInfoList.set(i, lspLocalLabelInfoBuilder.build());
               }
           }
       }

       // If it is not found in store then add it to store
       if (!dstDeviceUpdated || !srcDeviceUpdated) {
           // If tunnel info itself not available then create new one, otherwise add node to list.
           if (lspLabelInfoList == null) {
              lspLabelInfoList = new LinkedList<>();
           }

           if (!dstDeviceUpdated) {
               LspLocalLabelInfo lspLocalLabelInfo = DefaultLspLocalLabelInfo.builder()
                   .deviceId(dstDeviceId)
                   .inLabelId(labelId)
                   .outLabelId(null)
                   .inPort(dstPort) // Destination device IN port will be link destination port
                   .outPort(null)
                   .build();
               lspLabelInfoList.add(lspLocalLabelInfo);
           }

           if (!srcDeviceUpdated) {
               LspLocalLabelInfo lspLocalLabelInfo = DefaultLspLocalLabelInfo.builder()
                   .deviceId(srcDeviceId)
                   .inLabelId(null)
                   .outLabelId(labelId)
                   .inPort(null)
                   .outPort(dstPort) // Source device OUT port will be link destination (remote) port
                   .build();
               lspLabelInfoList.add(lspLocalLabelInfo);
           }

           pceStore.addTunnelInfo(tunnel.tunnelId(), lspLabelInfoList);
       }
    }

    /**
     * Deallocates unused labels to device pools.
     *
     * @param tunnel tunnel between ingress to egress
     */
    public void releaseLabel(Tunnel tunnel) {

       checkNotNull(labelRsrcService, LABEL_RESOURCE_SERVICE_NULL);
       checkNotNull(pceStore, PCE_STORE_NULL);

       Multimap<DeviceId, LabelResource> release = ArrayListMultimap.create();
       List<LspLocalLabelInfo> lspLocalLabelInfoList = pceStore.getTunnelInfo(tunnel.tunnelId());
       if ((lspLocalLabelInfoList != null) && (lspLocalLabelInfoList.size() > 0)) {
           for (Iterator<LspLocalLabelInfo> iterator = lspLocalLabelInfoList.iterator(); iterator.hasNext();) {
               LspLocalLabelInfo lspLocalLabelInfo = iterator.next();
               DeviceId deviceId = lspLocalLabelInfo.deviceId();
               LabelResourceId inLabelId = lspLocalLabelInfo.inLabelId();
               LabelResourceId outLabelId = lspLocalLabelInfo.outLabelId();
               PortNumber inPort = lspLocalLabelInfo.inPort();
               PortNumber outPort = lspLocalLabelInfo.outPort();

               try {
                   // Push into device
                   if ((outLabelId != null) && (outPort != null)) {
                       pushLocalLabels(deviceId, outLabelId, outPort, tunnel, false,
                                       Long.valueOf(LabelType.OUT_LABEL.value), PcepLabelOp.REMOVE);
                   }

                   if ((inLabelId != null) && (inPort != null)) {
                       pushLocalLabels(deviceId, inLabelId, inPort, tunnel, false,
                                       Long.valueOf(LabelType.IN_LABEL.value), PcepLabelOp.REMOVE);
                   }
               } catch (PcepParseException e) {
                   log.error("Failed to push local label for device {}for tunnel {}.", deviceId.toString(),
                             tunnel.tunnelName().toString());
               }

               // List is stored from egress to ingress. So, using IN label id to release.
               // Only one local label is assigned to device (destination node)
               // and that is used as OUT label for source node.
               // No need to release label for last node in the list from pool because label was not allocated to
               // ingress node (source node).
               if ((iterator.hasNext()) && (inLabelId != null)) {
                   LabelResource labelRsc = new DefaultLabelResource(deviceId, inLabelId);
                   release.put(deviceId, labelRsc);
               }
           }
       }

       // Release from label pool
       if (!release.isEmpty()) {
          labelRsrcService.releaseToDevicePool(release);
       }

       pceStore.removeTunnelInfo(tunnel.tunnelId());
   }

   //Pushes local labels to the device which is specific to path [CR-case].
   private void pushLocalLabels(DeviceId deviceId, LabelResourceId labelId,
            PortNumber portNum, Tunnel tunnel,
            Boolean isBos, Long labelType, PcepLabelOp type) throws PcepParseException {

        checkNotNull(deviceId);
        checkNotNull(labelId);
        checkNotNull(portNum);
        checkNotNull(tunnel);
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
            if (labelType == OUT_LABEL_TYPE) {
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

       float  iBandwidth = 0;
       if (tunnel.annotations().value(BANDWIDTH) != null) {
           //iBandwidth = Float.floatToIntBits(Float.parseFloat(tunnel.annotations().value(BANDWIDTH)));
           iBandwidth = Float.parseFloat(tunnel.annotations().value(BANDWIDTH));
       }
       // build bandwidth object
       PcepBandwidthObject bandwidthObject = pc.factory().buildBandwidthObject()
                                             .setBandwidth(iBandwidth)
                                             .build();
       // build pcep attribute
       PcepAttribute pcepAttribute = pc.factory().buildPcepAttribute()
                                     .setBandwidthObject(bandwidthObject)
                                     .build();

       PcepMsgPath msgPath = pc.factory().buildPcepMsgPath()
                             .setEroObject(eroObj)
                             .setPcepAttribute(pcepAttribute)
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

   private PcepSrpObject getSrpObject(PcepClient pc, PcepLabelOp type, boolean bSFlag)
           throws PcepParseException {
       PcepSrpObject srpObj;
       boolean bRFlag = false;

       if (!type.equals(PcepLabelOp.ADD)) {
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

   /**
    * Returns PCEP client.
    *
    * @return PCEP client
    */
   private PcepClient getPcepClient(DeviceId deviceId) {
       Device device = deviceService.getDevice(deviceId);

       // In future projections instead of annotations will be used to fetch LSR ID.
       String lsrId = device.annotations().value(LSRID);
       PcepClient pcc = clientController.getClient(PccId.pccId(IpAddress.valueOf(lsrId)));
       return pcc;
   }
}
