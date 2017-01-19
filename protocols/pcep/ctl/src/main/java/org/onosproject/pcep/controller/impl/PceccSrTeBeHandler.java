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
import static org.onosproject.pcep.controller.PcepSyncStatus.IN_SYNC;
import static org.onosproject.pcep.controller.PcepSyncStatus.SYNCED;

import java.util.Collection;
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.onlab.packet.IpAddress;
import org.onosproject.incubator.net.resource.label.DefaultLabelResource;
import org.onosproject.incubator.net.resource.label.LabelResource;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.resource.label.LabelResourceAdminService;
import org.onosproject.incubator.net.resource.label.LabelResourceService;
import org.onosproject.incubator.net.tunnel.DefaultLabelStack;
import org.onosproject.incubator.net.tunnel.LabelStack;
import org.onosproject.net.device.DeviceService;
import org.onosproject.pcelabelstore.PcepLabelOp;
import org.onosproject.pcelabelstore.api.PceLabelStore;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcep.controller.PcepClient;
import org.onosproject.pcep.controller.PcepClientController;
import org.onosproject.pcep.controller.SrpIdGenerators;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepFecObjectIPv4;
import org.onosproject.pcepio.protocol.PcepFecObjectIPv4Adjacency;
import org.onosproject.pcepio.protocol.PcepLabelObject;
import org.onosproject.pcepio.protocol.PcepLabelUpdate;
import org.onosproject.pcepio.protocol.PcepLabelUpdateMsg;
import org.onosproject.pcepio.protocol.PcepSrpObject;
import org.onosproject.pcepio.types.PcepLabelMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/**
 * PCE SR-BE and SR-TE functionality.
 * SR-BE: Each node (PCC) is allocated a node-SID (label) by the PCECC. The PCECC sends PCLabelUpd to
 * update the label map of each node to all the nodes in the domain.
 * SR-TE: apart from node-SID, Adj-SID is used where each adjacency is allocated an Adj-SID (label) by the PCECC.
 * The PCECC sends PCLabelUpd to update the label map of each Adj to the corresponding nodes in the domain.
 */
public final class PceccSrTeBeHandler {
    private static final Logger log = LoggerFactory.getLogger(PceccSrTeBeHandler.class);

    private static final String LABEL_RESOURCE_ADMIN_SERVICE_NULL = "Label Resource Admin Service cannot be null";
    private static final String LABEL_RESOURCE_SERVICE_NULL = "Label Resource Service cannot be null";
    private static final String PCE_STORE_NULL = "PCE Store cannot be null";
    private static final String DEVICE_ID_NULL = "Device-Id cannot be null";
    private static final String LSR_ID_NULL = "LSR-Id cannot be null";
    private static final String LINK_NULL = "Link cannot be null";
    private static final String PATH_NULL = "Path cannot be null";
    private static final String LSR_ID = "lsrId";
    private static PceccSrTeBeHandler srTeHandlerInstance = null;
    private LabelResourceAdminService labelRsrcAdminService;
    private LabelResourceService labelRsrcService;
    private DeviceService deviceService;
    private PcepClientController clientController;
    private PceLabelStore pceStore;

    /**
     * Initializes default values.
     */
    private PceccSrTeBeHandler() {
    }

    /**
     * Returns single instance of this class.
     *
     * @return this class single instance
     */
    public static PceccSrTeBeHandler getInstance() {
        if (srTeHandlerInstance == null) {
            srTeHandlerInstance = new PceccSrTeBeHandler();
        }
        return srTeHandlerInstance;
    }

    /**
     * Initialization of label manager interfaces and pce store.
     *
     * @param labelRsrcAdminService label resource admin service
     * @param labelRsrcService label resource service
     * @param clientController client controller
     * @param pceStore PCE label store
     * @param deviceService device service
     */
    public void initialize(LabelResourceAdminService labelRsrcAdminService,
                           LabelResourceService labelRsrcService,
                           PcepClientController clientController,
                           PceLabelStore pceStore,
                           DeviceService deviceService) {
        this.labelRsrcAdminService = labelRsrcAdminService;
        this.labelRsrcService = labelRsrcService;
        this.clientController = clientController;
        this.pceStore = pceStore;
        this.deviceService = deviceService;
    }

    /**
     * Reserves the global label pool.
     *
     * @param beginLabel minimum value of global label space
     * @param endLabel maximum value of global label space
     * @return success or failure
     */
    public boolean reserveGlobalPool(long beginLabel, long endLabel) {
        checkNotNull(labelRsrcAdminService, LABEL_RESOURCE_ADMIN_SERVICE_NULL);
        return labelRsrcAdminService.createGlobalPool(LabelResourceId.labelResourceId(beginLabel),
                                                      LabelResourceId.labelResourceId(endLabel));
    }

    /**
     * Retrieve lsr-id from device annotation.
     *
     * @param deviceId specific device id from which lsr-id needs to be retrieved
     * @return lsr-id of a device
     */
    public String getLsrId(DeviceId deviceId) {
        checkNotNull(deviceId, DEVICE_ID_NULL);
        Device device = deviceService.getDevice(deviceId);
        if (device == null) {
            log.debug("Device is not available for device id {} in device service.", deviceId.toString());
            return null;
        }

        // Retrieve lsr-id from device
        if (device.annotations() == null) {
            log.debug("Device {} does not have annotation.", device.toString());
            return null;
        }

        String lsrId = device.annotations().value(LSR_ID);
        if (lsrId == null) {
            log.debug("The lsr-id of device {} is null.", device.toString());
            return null;
        }
        return lsrId;
    }

    /**
     * Allocates node label from global node label pool to specific device.
     * Configure this device with labels and lsrid mapping of all other devices and vice versa.
     *
     * @param specificDeviceId node label needs to be allocated to specific device
     * @param specificLsrId lsrid of specific device
     * @return success or failure
     */
    public boolean allocateNodeLabel(DeviceId specificDeviceId, String specificLsrId) {
        long applyNum = 1; // For each node only one node label
        LabelResourceId specificLabelId = null;

        checkNotNull(specificDeviceId, DEVICE_ID_NULL);
        checkNotNull(specificLsrId, LSR_ID_NULL);
        checkNotNull(labelRsrcService, LABEL_RESOURCE_SERVICE_NULL);
        checkNotNull(pceStore, PCE_STORE_NULL);

        // Check whether node-label was already configured for this specific device.
        if (pceStore.getGlobalNodeLabel(specificDeviceId) != null) {
            log.debug("Node label was already configured for device {}.", specificDeviceId.toString());
            return false;
        }

        // The specificDeviceId is the new device and is not there in the pce store.
        // So, first generate its label and configure label and its lsr-id to it.
        Collection<LabelResource> result = labelRsrcService.applyFromGlobalPool(applyNum);
        if (!result.isEmpty()) {
            // Only one element (label-id) to retrieve
            Iterator<LabelResource> iterator = result.iterator();
            DefaultLabelResource defaultLabelResource = (DefaultLabelResource) iterator.next();
            specificLabelId = defaultLabelResource.labelResourceId();
            if (specificLabelId == null) {
                log.error("Unable to retrieve global node label for a device id {}.", specificDeviceId.toString());
                return false;
            }
        } else {
            log.error("Unable to allocate global node label for a device id {}.", specificDeviceId.toString());
            return false;
        }

        // store it
        pceStore.addGlobalNodeLabel(specificDeviceId, specificLabelId);

        // Push its label information into specificDeviceId
        PcepClient pcc = getPcepClient(specificDeviceId);
        try {
            pushGlobalNodeLabel(pcc,
                                specificLabelId,
                                IpAddress.valueOf(specificLsrId).getIp4Address().toInt(),
                                PcepLabelOp.ADD,
                                false);
        } catch (PcepParseException e) {
            log.error("Failed to push global node label for LSR {}.", specificLsrId.toString());
        }

        // Configure (node-label, lsr-id) mapping of each devices into specific device and vice versa.
        for (Map.Entry<DeviceId, LabelResourceId> element : pceStore.getGlobalNodeLabels().entrySet()) {
            DeviceId otherDevId = element.getKey();

            // Get lsr-id of a device
            String otherLsrId = getLsrId(otherDevId);
            if (otherLsrId == null) {
                log.error("The lsr-id of device id {} is null.", otherDevId.toString());
                releaseNodeLabel(specificDeviceId, specificLsrId);
                return false;
            }

            // Push to device
            // Push label information of specificDeviceId to otherDevId in list and vice versa.
            if (!otherDevId.equals(specificDeviceId)) {
                try {
                    pushGlobalNodeLabel(getPcepClient(otherDevId),
                                        specificLabelId,
                                        IpAddress.valueOf(specificLsrId).getIp4Address().toInt(),
                                        PcepLabelOp.ADD,
                                        false);

                    pushGlobalNodeLabel(pcc, element.getValue(),
                                        IpAddress.valueOf(otherLsrId).getIp4Address().toInt(),
                                        PcepLabelOp.ADD,
                                        false);
                } catch (PcepParseException e) {
                    log.error("Failed to push global node label for LSR {} or LSR {}.", specificLsrId.toString(),
                              otherLsrId.toString());
                }
            }
        }
        return true;
    }

    /**
     * Releases assigned node label of specific device from global node label pool and pce store.
     * and remove configured this node label from all other devices.
     *
     * @param specificDeviceId node label needs to be released for specific device
     * @param specificLsrId lsrid of specific device
     * @return success or failure
     */
    public boolean releaseNodeLabel(DeviceId specificDeviceId, String specificLsrId) {
        checkNotNull(specificDeviceId, DEVICE_ID_NULL);
        checkNotNull(specificLsrId, LSR_ID_NULL);
        checkNotNull(labelRsrcService, LABEL_RESOURCE_SERVICE_NULL);
        checkNotNull(pceStore, PCE_STORE_NULL);
        boolean retValue = true;

        // Release node label entry of this specific device from all other devices
        // Retrieve node label of this specific device from store
        LabelResourceId labelId = pceStore.getGlobalNodeLabel(specificDeviceId);
        if (labelId == null) {
            log.error("Unable to retrieve label of a device id {} from store.", specificDeviceId.toString());
            return false;
        }

        // Go through all devices in the pce store and remove label entry from device
        for (Map.Entry<DeviceId, LabelResourceId> element : pceStore.getGlobalNodeLabels().entrySet()) {
            DeviceId otherDevId = element.getKey();

            // Remove this specific device label information from all other nodes except
            // this specific node where connection already lost.
            if (!specificDeviceId.equals(otherDevId)) {
                try {
                    pushGlobalNodeLabel(getPcepClient(otherDevId),
                                        labelId,
                                        IpAddress.valueOf(specificLsrId).getIp4Address().toInt(),
                                        PcepLabelOp.REMOVE,
                                        false);
                } catch (PcepParseException e) {
                    log.error("Failed to push global node label for LSR {}.", specificLsrId.toString());
                }
            }
        }

        // Release from label manager
        Set<LabelResourceId> release = new HashSet<>();
        release.add(labelId);
        if (!labelRsrcService.releaseToGlobalPool(release)) {
            log.error("Unable to release label id {} from label manager.", labelId.toString());
            retValue = false;
        }

        // Remove from store
        if (!pceStore.removeGlobalNodeLabel(specificDeviceId)) {
            log.error("Unable to remove global node label id {} from store.", labelId.toString());
            retValue = false;
        }
        return retValue;
    }

    /**
     * Allocates adjacency label to a link from local resource pool by a specific device id.
     *
     * @param link between devices
     * @return success or failure
     */
    public boolean allocateAdjacencyLabel(Link link) {
        long applyNum = 1; // Single label to each link.
        DeviceId srcDeviceId = link.src().deviceId();
        Collection<LabelResource> labelList;

        checkNotNull(link, LINK_NULL);
        checkNotNull(labelRsrcService, LABEL_RESOURCE_SERVICE_NULL);
        checkNotNull(pceStore, PCE_STORE_NULL);

        // Checks whether adjacency label was already allocated
        LabelResourceId labelId = pceStore.getAdjLabel(link);
        if (labelId != null) {
            log.debug("Adjacency label {} was already allocated for a link {}.", labelId.toString(), link.toString());
            return false;
        }

        // Allocate adjacency label to a link from label manager.
        // Take label from source device pool to allocate.
        labelList = labelRsrcService.applyFromDevicePool(srcDeviceId, applyNum);
        if (labelList.isEmpty()) {
            log.error("Unable to allocate label to a device id {}.", srcDeviceId.toString());
            return false;
        }

        // Currently only one label to a device. So, no need to iterate through list
        Iterator<LabelResource> iterator = labelList.iterator();
        DefaultLabelResource defaultLabelResource = (DefaultLabelResource) iterator.next();
        labelId = defaultLabelResource.labelResourceId();
        if (labelId == null) {
            log.error("Unable to allocate label to a device id {}.", srcDeviceId.toString());
            return false;
        }
        log.debug("Allocated adjacency label {} to a link {}.", labelId.toString(), link.toString());

        // Push adjacency label to device
        try {
            pushAdjacencyLabel(getPcepClient(srcDeviceId), labelId, (int) link.src().port().toLong(),
                               (int) link.dst().port().toLong(), PcepLabelOp.ADD);
        } catch (PcepParseException e) {
            log.error("Failed to push adjacency label for link {}-{}.", (int) link.src().port().toLong(),
                      (int) link.dst().port().toLong());
        }

        // Save in store
        pceStore.addAdjLabel(link, labelId);
        return true;
    }

    /**
      * Releases unused adjacency labels from device pools.
      *
      * @param link between devices
      * @return success or failure
      */
    public boolean releaseAdjacencyLabel(Link link) {
        checkNotNull(link, LINK_NULL);
        checkNotNull(labelRsrcService, LABEL_RESOURCE_SERVICE_NULL);
        checkNotNull(pceStore, PCE_STORE_NULL);
        boolean retValue = true;

        // Retrieve link label from store
        LabelResourceId labelId = pceStore.getAdjLabel(link);
        if (labelId == null) {
            log.error("Unabel to retrieve label for a link {} from store.", link.toString());
            return false;
        }

        // Device
        DeviceId srcDeviceId = link.src().deviceId();

        // Release adjacency label from device
        try {
            pushAdjacencyLabel(getPcepClient(srcDeviceId), labelId, (int) link.src().port().toLong(),
                               (int) link.dst().port().toLong(), PcepLabelOp.REMOVE);
        } catch (PcepParseException e) {
            log.error("Failed to push adjacency label for link {}-{}.", (int) link.src().port().toLong(),
                      (int) link.dst().port().toLong());
        }


        // Release link label from label manager
        Multimap<DeviceId, LabelResource> release = ArrayListMultimap.create();
        DefaultLabelResource defaultLabelResource = new DefaultLabelResource(srcDeviceId, labelId);
        release.put(srcDeviceId, defaultLabelResource);
        if (!labelRsrcService.releaseToDevicePool(release)) {
            log.error("Unable to release label id {} from label manager.", labelId.toString());
            retValue = false;
        }

        // Remove adjacency label from store
        if (!pceStore.removeAdjLabel(link)) {
            log.error("Unable to remove adjacency label id {} from store.", labelId.toString());
            retValue = false;
        }
        return retValue;
    }

    /**
     * Computes label stack for a path.
     *
     * @param path lsp path
     * @return label stack
     */
    public LabelStack computeLabelStack(Path path) {
        checkNotNull(path, PATH_NULL);
        // Label stack is linked list to make labels in order.
        List<LabelResourceId> labelStack = new LinkedList<>();
        List<Link> linkList = path.links();
        if ((linkList != null) && (!linkList.isEmpty())) {
            // Path: [x] ---- [y] ---- [z]
            // For other than last link, add only source[x] device label.
            // For the last link, add both source[y] and destination[z] device labels.
            // For all links add adjacency label
            Link link = null;
            LabelResourceId nodeLabelId = null;
            LabelResourceId adjLabelId = null;
            DeviceId deviceId = null;
            for (Iterator<Link> iterator = linkList.iterator(); iterator.hasNext();) {
                link = iterator.next();
                // Add adjacency label for this link
                adjLabelId = pceStore.getAdjLabel(link);
                if (adjLabelId == null) {
                    log.error("Adjacency label id is null for a link {}.", link.toString());
                    return null;
                }
                labelStack.add(adjLabelId);

                deviceId = link.dst().deviceId();
                nodeLabelId = pceStore.getGlobalNodeLabel(deviceId);
                if (nodeLabelId == null) {
                    log.error("Unable to find node label for a device id {} in store.", deviceId.toString());
                    return null;
                }
                labelStack.add(nodeLabelId);
            }
        } else {
            log.debug("Empty link in path.");
            return null;
        }
        return new DefaultLabelStack(labelStack);
    }

    //Pushes node labels to the specified device.
    void pushGlobalNodeLabel(PcepClient pc, LabelResourceId labelId,
            int labelForNode, PcepLabelOp type, boolean isBos) throws PcepParseException {

        checkNotNull(pc);
        checkNotNull(labelId);
        checkNotNull(type);

        LinkedList<PcepLabelUpdate> labelUpdateList = new LinkedList<>();
        PcepFecObjectIPv4 fecObject = pc.factory().buildFecObjectIpv4()
                                      .setNodeID(labelForNode)
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

    //Pushes adjacency labels to the specified device.
    void pushAdjacencyLabel(PcepClient pc, LabelResourceId labelId, int srcPortNo,
                                    int dstPortNo, PcepLabelOp type)
            throws PcepParseException {

        checkNotNull(pc);
        checkNotNull(labelId);
        checkNotNull(type);

        LinkedList<PcepLabelUpdate> labelUpdateList = new LinkedList<>();
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
        String lsrId = device.annotations().value(LSR_ID);
        PcepClient pcc = clientController.getClient(PccId.pccId(IpAddress.valueOf(lsrId)));
        return pcc;
    }
}
