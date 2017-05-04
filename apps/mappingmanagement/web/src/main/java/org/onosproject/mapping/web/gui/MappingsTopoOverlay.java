/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.mapping.web.gui;

import org.onosproject.net.DeviceId;
import org.onosproject.ui.UiTopoOverlay;
import org.onosproject.ui.topo.ButtonId;
import org.onosproject.ui.topo.PropertyPanel;

import static org.onosproject.ui.topo.TopoConstants.Properties.FLOWS;
import static org.onosproject.ui.topo.TopoConstants.Properties.INTENTS;
import static org.onosproject.ui.topo.TopoConstants.Properties.LATITUDE;
import static org.onosproject.ui.topo.TopoConstants.Properties.LONGITUDE;
import static org.onosproject.ui.topo.TopoConstants.Properties.PORTS;
import static org.onosproject.ui.topo.TopoConstants.Properties.TUNNELS;

/**
 * Customized topology overlay for mapping management app.
 */
public class MappingsTopoOverlay extends UiTopoOverlay {

    // NOTE: this must match the ID defined in mappingTopo.js
    private static final String OVERLAY_ID = "mapping-overlay";

    private static final ButtonId MAPPINGS_BUTTON = new ButtonId("mappings");

    /**
     * Creates a new user interface topology view overlay descriptor, with
     * the given identifier.
     */
    public MappingsTopoOverlay() {
        super(OVERLAY_ID);
    }

    @Override
    public void modifySummary(PropertyPanel pp) {
        pp.removeProps(
                INTENTS,
                TUNNELS,
                FLOWS
        );
    }

    @Override
    public void modifyDeviceDetails(PropertyPanel pp, DeviceId deviceId) {
        pp.removeAllButtons();
        pp.removeProps(LATITUDE, LONGITUDE, PORTS, FLOWS, TUNNELS);
        pp.addButton(MAPPINGS_BUTTON);
    }
}
