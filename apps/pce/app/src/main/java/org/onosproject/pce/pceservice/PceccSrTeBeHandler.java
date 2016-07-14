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
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MplsLabel;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.resource.label.DefaultLabelResource;
import org.onosproject.incubator.net.resource.label.LabelResource;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.resource.label.LabelResourceAdminService;
import org.onosproject.incubator.net.resource.label.LabelResourceService;
import org.onosproject.incubator.net.tunnel.DefaultLabelStack;
import org.onosproject.incubator.net.tunnel.LabelStack;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.pce.pcestore.api.PceStore;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.Objective;
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
    private static final int PREFIX_LENGTH = 32;
    private static PceccSrTeBeHandler srTeHandlerInstance = null;
    private LabelResourceAdminService labelRsrcAdminService;
    private LabelResourceService labelRsrcService;
    private FlowObjectiveService flowObjectiveService;
    private DeviceService deviceService;
    private PceStore pceStore;
    private ApplicationId appId;

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
     * @param flowObjectiveService flow objective service to push device label information
     * @param appId application id
     * @param pceStore PCE label store
     * @param deviceService device service
     */
    public void initialize(LabelResourceAdminService labelRsrcAdminService, LabelResourceService labelRsrcService,
                           FlowObjectiveService flowObjectiveService, ApplicationId appId, PceStore pceStore,
                           DeviceService deviceService) {
        this.labelRsrcAdminService = labelRsrcAdminService;
        this.labelRsrcService = labelRsrcService;
        this.flowObjectiveService = flowObjectiveService;
        this.pceStore = pceStore;
        this.appId = appId;
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
        if (result.size() > 0) {
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
        advertiseNodeLabelRule(specificDeviceId, specificLabelId,
                               IpPrefix.valueOf(IpAddress.valueOf(specificLsrId), PREFIX_LENGTH),
                               Objective.Operation.ADD, false);

        // Configure (node-label, lsr-id) mapping of each devices into specific device and vice versa.
        for (Map.Entry<DeviceId, LabelResourceId> element : pceStore.getGlobalNodeLabels().entrySet()) {
            DeviceId otherDevId = element.getKey();
            LabelResourceId otherLabelId = element.getValue();

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
                advertiseNodeLabelRule(otherDevId, specificLabelId,
                                       IpPrefix.valueOf(IpAddress.valueOf(specificLsrId), PREFIX_LENGTH),
                                       Objective.Operation.ADD, false);

                advertiseNodeLabelRule(specificDeviceId, otherLabelId,
                                       IpPrefix.valueOf(IpAddress.valueOf(otherLsrId), PREFIX_LENGTH),
                                       Objective.Operation.ADD, false);
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
                advertiseNodeLabelRule(otherDevId, labelId,
                                       IpPrefix.valueOf(IpAddress.valueOf(specificLsrId), PREFIX_LENGTH),
                                       Objective.Operation.REMOVE, false);
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
        if (labelList.size() <= 0) {
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
        installAdjLabelRule(srcDeviceId, labelId, link.src().port(), link.dst().port(), Objective.Operation.ADD);

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
        installAdjLabelRule(srcDeviceId, labelId, link.src().port(), link.dst().port(), Objective.Operation.REMOVE);

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
        if ((linkList != null) && (linkList.size() > 0)) {
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

    /**
     * Install a rule for pushing unique global labels to the device.
     *
     * @param deviceId device to which flow should be pushed
     * @param labelId label for the device
     * @param type type of operation
     */
    private void installNodeLabelRule(DeviceId deviceId, LabelResourceId labelId, Objective.Operation type) {
        checkNotNull(flowObjectiveService);
        checkNotNull(appId);
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        selectorBuilder.matchMplsLabel(MplsLabel.mplsLabel(labelId.id().intValue()));

        TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

        ForwardingObjective.Builder forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build()).withTreatment(treatment)
                .withFlag(ForwardingObjective.Flag.VERSATILE).fromApp(appId).makePermanent();

        if (type.equals(Objective.Operation.ADD)) {

            flowObjectiveService.forward(deviceId, forwardingObjective.add());
        } else {
            flowObjectiveService.forward(deviceId, forwardingObjective.remove());
        }
    }

    /**
     * Install a rule for pushing node labels to the device of other nodes.
     *
     * @param deviceId device to which flow should be pushed
     * @param labelId label for the device
     * @param ipPrefix device for which label is pushed
     * @param type type of operation
     * @param bBos is this the end of sync push
     */
    public void advertiseNodeLabelRule(DeviceId deviceId, LabelResourceId labelId, IpPrefix ipPrefix,
                                       Objective.Operation type, boolean bBos) {
        checkNotNull(flowObjectiveService);
        checkNotNull(appId);
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        selectorBuilder.matchMplsLabel(MplsLabel.mplsLabel(labelId.id().intValue()));
        selectorBuilder.matchIPSrc(ipPrefix);

        if (bBos) {
            selectorBuilder.matchMplsBos(bBos);
        }

        TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

        ForwardingObjective.Builder forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build()).withTreatment(treatment)
                .withFlag(ForwardingObjective.Flag.VERSATILE).fromApp(appId).makePermanent();

        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(deviceId, forwardingObjective.add());
        } else {
            flowObjectiveService.forward(deviceId, forwardingObjective.remove());
        }
    }

    /**
     * Install a rule for pushing Adjacency labels to the device.
     *
     * @param deviceId device to which flow should be pushed
     * @param labelId label for the adjacency
     * @param srcPortNum local port of the adjacency
     * @param dstPortNum remote port of the adjacency
     * @param type type of operation
     */
    public void installAdjLabelRule(DeviceId deviceId, LabelResourceId labelId, PortNumber srcPortNum,
                                    PortNumber dstPortNum, Objective.Operation type) {
        checkNotNull(flowObjectiveService);
        checkNotNull(appId);
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        selectorBuilder.matchMplsLabel(MplsLabel.mplsLabel(labelId.id().intValue()));
        selectorBuilder.matchIPSrc(IpPrefix.valueOf((int) srcPortNum.toLong(), 32));
        selectorBuilder.matchIPDst(IpPrefix.valueOf((int) dstPortNum.toLong(), 32));

        TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

        ForwardingObjective.Builder forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build()).withTreatment(treatment)
                .withFlag(ForwardingObjective.Flag.VERSATILE).fromApp(appId).makePermanent();

        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(deviceId, forwardingObjective.add());
        } else {
            flowObjectiveService.forward(deviceId, forwardingObjective.remove());
        }
    }
}
