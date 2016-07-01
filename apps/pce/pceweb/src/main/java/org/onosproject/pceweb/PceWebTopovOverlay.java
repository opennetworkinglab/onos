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

package org.onosproject.pceweb;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Annotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.resource.ContinuousResource;
import org.onosproject.net.resource.ResourceService;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.Resources;
import org.onosproject.ui.UiTopoOverlay;
import org.onosproject.ui.topo.PropertyPanel;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.ui.topo.TopoConstants.CoreButtons;
import org.onosproject.cli.AbstractShellCommand;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.IpAddress;
/**
 * PCE WEB topology overlay.
 */
public class PceWebTopovOverlay extends UiTopoOverlay {

  // NOTE: this must match the ID defined in pcewebTopovOverlay.js
    private static final String OVERLAY_ID = "PCE-web-overlay";
    private static final String MY_TITLE = "Device details";

    public static final String AS_NUMBER = "asNumber";
    public static final String LSR_ID = "lsrId";
    public static final String ABR_BIT = "abrBit";
    public static final String ASBR_BIT = "externalBit";
    public static final String TE_METRIC = "teCost";
    public static final String ABR = "ABR";
    public static final String ASBR = "ASBR";
    public static final String ABR_ASBR = "ABR/ASBR";
    public static final String INNER = "Inner";
    public static final long IDENTIFIER_SET = 0x100000000L;
    public static final long SET = 0xFFFFFFFFL;
    /**
     * Initialize the overlay ID.
     */
    public PceWebTopovOverlay() {
        super(OVERLAY_ID);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        log.debug("Deactivated");
    }

    @Override
    public void modifyDeviceDetails(PropertyPanel pp, DeviceId deviceId) {

         pp.title(MY_TITLE);
         DeviceService deviceService = AbstractShellCommand.get(DeviceService.class);
         pp.removeAllProps();
         pp.removeButtons(CoreButtons.SHOW_PORT_VIEW)
                .removeButtons(CoreButtons.SHOW_GROUP_VIEW)
                .removeButtons(CoreButtons.SHOW_METER_VIEW);

         if (deviceService != null) {

            Device device = deviceService.getDevice(deviceId);
            Annotations annots = device.annotations();

            String type = annots.value(AnnotationKeys.TYPE);
            String asNumber = annots.value(AS_NUMBER);
            String lsrId = annots.value(LSR_ID);
            String abrStatus = annots.value(ABR_BIT);
            String asbrStatus = annots.value(ASBR_BIT);

            if (type != null) {
                pp.addProp("Type", type);
            }

            if (asNumber != null) {
                pp.addProp("AS Number", asNumber);
            }

            if (lsrId != null) {
                pp.addProp("LSR ID", lsrId);
            }

            if (abrStatus.equals(true) && asbrStatus.equals(true)) {
                pp.addProp("Position", ABR_ASBR);
            } else if (abrStatus.equals(true)) {
                pp.addProp("Position", ABR);
            } else if (asbrStatus.equals(true)) {
                pp.addProp("Position", ASBR);
            } else {
                pp.addProp("Position", INNER);
            }
        }
    }

    @Override
    public Map<String, String> additionalLinkData(LinkEvent event) {
        Map<String, String> map = new HashMap<>();
        Link link = event.subject();
        long srcPortNo;
        long dstPortNo;
        IpAddress ipDstAddress = null;
        IpAddress ipSrcAddress = null;
        String srcPort;
        String dstPort;
        String bandWidth;

        srcPortNo = link.src().port().toLong();
        if (((srcPortNo & IDENTIFIER_SET) == IDENTIFIER_SET)) {
            srcPort = String.valueOf(srcPortNo);
        } else {
            ipSrcAddress = Ip4Address.valueOf((int) srcPortNo);
            srcPort = ipSrcAddress.toString();
        }

        dstPortNo = link.dst().port().toLong();
        if (((dstPortNo & IDENTIFIER_SET) == IDENTIFIER_SET)) {
            dstPort = String.valueOf(dstPortNo);
        } else {
            ipDstAddress = Ip4Address.valueOf((int) dstPortNo);
            dstPort = ipDstAddress.toString();
        }

        map.put("Src Address", srcPort);
        map.put("Dst Address", dstPort);
        map.put("Te metric", link.annotations().value(TE_METRIC));

        ResourceService resService = AbstractShellCommand.get(ResourceService.class);
        DiscreteResource devResource = Resources.discrete(link.src().deviceId(), link.src().port()).resource();
        if (resService == null) {
            log.warn("resource service does not exist");
            return map;
        }

        if (devResource == null) {
            log.warn("Device resources does not exist");
            return map;
        }
        double regBandwidth = 0;
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            log.error("Exception occured while getting the bandwidth.");
        }
        Set<Resource> resources = resService.getRegisteredResources(devResource.id());
        for (Resource res : resources) {
            if (res instanceof ContinuousResource) {
                regBandwidth = ((ContinuousResource) res).value();
                break;
            }
        }

        if (regBandwidth != 0) {
            bandWidth = String.valueOf(regBandwidth);
            map.put("Bandwidth", bandWidth);
        }

        return map;
    }
}
