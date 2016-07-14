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
import java.util.List;
import java.util.ListIterator;
import java.util.LinkedList;

import org.onlab.packet.MplsLabel;
import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.resource.label.DefaultLabelResource;
import org.onosproject.incubator.net.resource.label.LabelResource;
import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.incubator.net.resource.label.LabelResourceService;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.pce.pcestore.api.PceStore;
import org.onosproject.pce.pcestore.api.LspLocalLabelInfo;
import org.onosproject.pce.pcestore.PceccTunnelInfo;
import org.onosproject.pce.pcestore.DefaultLspLocalLabelInfo;
import org.onosproject.net.Link;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

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

    private static final String LABEL_RESOURCE_SERVICE_NULL = "Label Resource Service cannot be null";
    private static final String PCE_STORE_NULL = "PCE Store cannot be null";
    private static BasicPceccHandler crHandlerInstance = null;
    private LabelResourceService labelRsrcService;
    private PceStore pceStore;
    private FlowObjectiveService flowObjectiveService;
    private ApplicationId appId;

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
     * @param flowObjectiveService flow objective service to push device label information
     * @param appId applicaton id
     * @param pceStore pce label store
     */
    public void initialize(LabelResourceService labelRsrcService, FlowObjectiveService flowObjectiveService,
                           ApplicationId appId, PceStore pceStore) {
        this.labelRsrcService = labelRsrcService;
        this.flowObjectiveService = flowObjectiveService;
        this.appId = appId;
        this.pceStore = pceStore;
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

                    // Push into destination device
                    // Destination device IN port is link.dst().port()
                    installLocalLabelRule(dstDeviceId, labelId, dstPort, tunnel.tunnelId(), false,
                                          Long.valueOf(LabelType.IN_LABEL.value), Objective.Operation.ADD);

                    // Push into source device
                    // Source device OUT port will be link.dst().port(). Means its remote port used to send packet.
                    installLocalLabelRule(srcDeviceId, labelId, dstPort, tunnel.tunnelId(), isLastLabelToPush,
                                          Long.valueOf(LabelType.OUT_LABEL.value), Objective.Operation.ADD);

                    // Add or update pcecc tunnel info in pce store.
                    updatePceccTunnelInfoInStore(srcDeviceId, dstDeviceId, labelId, dstPort,
                                                 tunnel, isLastLabelToPush);
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
     * @param isLastLabelToPush indicates this is the last label to push in Basic PCECC case
     */
    public void updatePceccTunnelInfoInStore(DeviceId srcDeviceId, DeviceId dstDeviceId, LabelResourceId labelId,
                                                PortNumber dstPort, Tunnel tunnel, boolean isLastLabelToPush) {
       // First try to retrieve device from store and update its label id if it is exists,
       // otherwise add it
       boolean dstDeviceUpdated = false;
       boolean srcDeviceUpdated = false;

       PceccTunnelInfo pceccTunnelInfo = pceStore.getTunnelInfo(tunnel.tunnelId());
       List<LspLocalLabelInfo> lspLabelInfoList;
       if (pceccTunnelInfo != null) {
           lspLabelInfoList = pceccTunnelInfo.lspLocalLabelInfoList();
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
       }

       // If it is not found in store then add it to store
       if (!dstDeviceUpdated || !srcDeviceUpdated) {
           // If tunnel info itself not available then create new one, otherwise add node to list.
           if (pceccTunnelInfo == null) {
              pceccTunnelInfo = new PceccTunnelInfo();
              lspLabelInfoList = new LinkedList<>();
           } else {
              lspLabelInfoList = pceccTunnelInfo.lspLocalLabelInfoList();
              if (lspLabelInfoList == null) {
                 lspLabelInfoList = new LinkedList<>();
              }
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

           pceccTunnelInfo.lspLocalLabelInfoList(lspLabelInfoList);
           pceStore.addTunnelInfo(tunnel.tunnelId(), pceccTunnelInfo);
       }
    }

    /**
     * Deallocates unused labels to device pools.
     *
     * @param tunnel tunnel between ingress to egress
     */
    public void releaseLabel(Tunnel tunnel) {
       boolean isLastLabelToPush = false;

       checkNotNull(labelRsrcService, LABEL_RESOURCE_SERVICE_NULL);
       checkNotNull(pceStore, PCE_STORE_NULL);

       Multimap<DeviceId, LabelResource> release = ArrayListMultimap.create();
       PceccTunnelInfo pceccTunnelInfo = pceStore.getTunnelInfo(tunnel.tunnelId());
       if (pceccTunnelInfo != null) {
           List<LspLocalLabelInfo> lspLocalLabelInfoList = pceccTunnelInfo.lspLocalLabelInfoList();
           if ((lspLocalLabelInfoList != null) && (lspLocalLabelInfoList.size() > 0)) {
               for (Iterator<LspLocalLabelInfo> iterator = lspLocalLabelInfoList.iterator(); iterator.hasNext();) {
                   LspLocalLabelInfo lspLocalLabelInfo = iterator.next();
                   DeviceId deviceId = lspLocalLabelInfo.deviceId();
                   LabelResourceId inLabelId = lspLocalLabelInfo.inLabelId();
                   LabelResourceId outLabelId = lspLocalLabelInfo.outLabelId();
                   PortNumber inPort = lspLocalLabelInfo.inPort();
                   PortNumber outPort = lspLocalLabelInfo.outPort();

                   // Check whether this is last link label to push
                   if (!iterator.hasNext()) {
                      isLastLabelToPush = true;
                   }

                   // Push into device
                   if ((inLabelId != null) && (inPort != null)) {
                       installLocalLabelRule(deviceId, inLabelId, inPort, tunnel.tunnelId(), isLastLabelToPush,
                                             Long.valueOf(LabelType.IN_LABEL.value), Objective.Operation.REMOVE);
                   }

                   if ((outLabelId != null) && (outPort != null)) {
                       installLocalLabelRule(deviceId, outLabelId, outPort, tunnel.tunnelId(), isLastLabelToPush,
                                             Long.valueOf(LabelType.OUT_LABEL.value), Objective.Operation.REMOVE);
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

           // Remove tunnel info only if tunnel consumer id is not saved.
           // If tunnel consumer id is saved, this tunnel info will be removed during releasing bandwidth.
           if (pceccTunnelInfo.tunnelConsumerId() == null) {
               pceStore.removeTunnelInfo(tunnel.tunnelId());
           }
       } else {
           log.error("Unable to find PCECC tunnel info in store for a tunnel {}.", tunnel.toString());
       }
   }

    // Install a rule for pushing local labels to the device which is specific to path.
    private void installLocalLabelRule(DeviceId deviceId, LabelResourceId labelId,
                                       PortNumber portNum, TunnelId tunnelId,
                                       Boolean isBos, Long labelType,
                                       Objective.Operation type) {
        checkNotNull(flowObjectiveService);
        checkNotNull(appId);
        TrafficSelector.Builder selectorBuilder = DefaultTrafficSelector.builder();

        selectorBuilder.matchMplsLabel(MplsLabel.mplsLabel(labelId.id().intValue()));
        selectorBuilder.matchInPort(portNum);
        selectorBuilder.matchTunnelId(Long.parseLong(tunnelId.id()));
        selectorBuilder.matchMplsBos(isBos);
        selectorBuilder.matchMetadata(labelType);

        TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();

        ForwardingObjective.Builder forwardingObjective = DefaultForwardingObjective.builder()
                .withSelector(selectorBuilder.build())
                .withTreatment(treatment)
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .fromApp(appId)
                .makePermanent();

        if (type.equals(Objective.Operation.ADD)) {
            flowObjectiveService.forward(deviceId, forwardingObjective.add());
        } else {
            flowObjectiveService.forward(deviceId, forwardingObjective.remove());
        }
    }
}
