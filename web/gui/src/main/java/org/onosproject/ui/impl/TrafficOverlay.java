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
 *
 */

package org.onosproject.ui.impl;

import org.onosproject.net.DeviceId;
import org.onosproject.ui.UiTopoOverlay;
import org.onosproject.ui.topo.ButtonId;
import org.onosproject.ui.topo.PropertyPanel;

/**
 * Topology Overlay for network traffic.
 */
public class TrafficOverlay extends UiTopoOverlay {
    /**
     * Traffic Overlay identifier.
     */
    public static final String TRAFFIC_ID = "traffic";

    private static final String SDF_ID = "showDeviceFlows";
    private static final String SRT_ID = "showRelatedTraffic";

    private static final ButtonId SHOW_DEVICE_FLOWS = new ButtonId(SDF_ID);
    private static final ButtonId SHOW_RELATED_TRAFFIC = new ButtonId(SRT_ID);


    public TrafficOverlay() {
        super(TRAFFIC_ID);
    }

    // override activate and deactivate, to write log messages
    @Override
    public void activate() {
        super.activate();
        log.debug("TrafficOverlay Activated");
    }

    @Override
    public void deactivate() {
        super.deactivate();
        log.debug("TrafficOverlay Deactivated");
    }

    @Override
    public void modifyDeviceDetails(PropertyPanel pp, DeviceId deviceId) {
        pp.addButton(SHOW_DEVICE_FLOWS)
            .addButton(SHOW_RELATED_TRAFFIC);
    }
}
