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


import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Annotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.ui.UiTopoOverlay;
import org.onosproject.ui.topo.ButtonId;
import org.onosproject.ui.topo.PropertyPanel;
import org.onosproject.net.HostId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.ui.topo.TopoConstants.CoreButtons;

import static org.onosproject.ui.topo.TopoConstants.Properties.*;
import org.onosproject.cli.AbstractShellCommand;

/**
 * PCE WEB topology overlay.
 */
public class PceWebTopovOverlay extends UiTopoOverlay {

  // NOTE: this must match the ID defined in pcewebTopovOverlay.js
    private static final String OVERLAY_ID = "PCE-web-overlay";
    private static final String MY_TITLE = "Device details";

    public static final String AS_NUMBER = "asNumber";
    public static final String DOMAIN_IDENTIFIER = "domainIdentifier";
    public static final String ROUTING_UNIVERSE = "routingUniverse";

    private static final ButtonId SRC_BUTTON = new ButtonId("src");
    private static final ButtonId DST_BUTTON = new ButtonId("dst");
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
         log.info("Modify device details called.");

         DeviceService deviceService = AbstractShellCommand.get(DeviceService.class);

         pp.removeAllProps();

         pp.addButton(SRC_BUTTON).addButton(DST_BUTTON);

         pp.removeButtons(CoreButtons.SHOW_PORT_VIEW)
                .removeButtons(CoreButtons.SHOW_GROUP_VIEW)
                .removeButtons(CoreButtons.SHOW_METER_VIEW);

         if (deviceService != null) {

            Device device = deviceService.getDevice(deviceId);
            Annotations annot = device.annotations();

            String routerId = annot.value(AnnotationKeys.ROUTER_ID);
            String type = annot.value(AnnotationKeys.TYPE);
            String asNumber = annot.value(AS_NUMBER);
            String domain = annot.value(DOMAIN_IDENTIFIER);
            String routingUnverse = annot.value(ROUTING_UNIVERSE);

            if (type != null) {
                pp.addProp("Type", type);
            }
            /* TBD: Router ID need to print
            if (routerId != null) {
                pp.addProp("Router-ID", routerId);
            } */
            if (routingUnverse != null) {
                pp.addProp("Routing Universe", routingUnverse);
            }
            if (asNumber != null) {
                pp.addProp("AS Number", asNumber);
            }
            if (domain != null) {
                pp.addProp("Domain ID", domain);
            }
        }
    }

    @Override
    public void modifyHostDetails(PropertyPanel pp, HostId hostId) {
        pp.addButton(SRC_BUTTON).addButton(DST_BUTTON);
    }
}
