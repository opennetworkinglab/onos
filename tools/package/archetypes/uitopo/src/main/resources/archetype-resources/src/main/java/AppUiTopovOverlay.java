#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
/*
 * Copyright ${year}-present Open Networking Laboratory
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
package ${package};

import org.onosproject.net.DeviceId;
import org.onosproject.ui.UiTopoOverlay;
import org.onosproject.ui.topo.ButtonId;
import org.onosproject.ui.topo.PropertyPanel;
import org.onosproject.ui.topo.TopoConstants.CoreButtons;
import org.onosproject.ui.GlyphConstants;

import static org.onosproject.ui.topo.TopoConstants.Properties.FLOWS;
import static org.onosproject.ui.topo.TopoConstants.Properties.INTENTS;
import static org.onosproject.ui.topo.TopoConstants.Properties.LATITUDE;
import static org.onosproject.ui.topo.TopoConstants.Properties.LONGITUDE;
import static org.onosproject.ui.topo.TopoConstants.Properties.TOPOLOGY_SSCS;
import static org.onosproject.ui.topo.TopoConstants.Properties.TUNNELS;
import static org.onosproject.ui.topo.TopoConstants.Properties.VERSION;

/**
 * Our topology overlay.
 */
public class AppUiTopovOverlay extends UiTopoOverlay {

    // NOTE: this must match the ID defined in sampleTopov.js
    private static final String OVERLAY_ID = "meowster-overlay";

    private static final String MY_TITLE = "My App Rocks!";
    private static final String MY_VERSION = "Beta-1.0.0042";
    private static final String MY_DEVICE_TITLE = "I changed the title";

    private static final ButtonId FOO_BUTTON = new ButtonId("foo");
    private static final ButtonId BAR_BUTTON = new ButtonId("bar");

    public AppUiTopovOverlay() {
        super(OVERLAY_ID);
    }


    @Override
    public void modifySummary(PropertyPanel pp) {
        pp.title(MY_TITLE)
                .typeId(GlyphConstants.CROWN)
                .removeProps(
                        TOPOLOGY_SSCS,
                        INTENTS,
                        TUNNELS,
                        FLOWS,
                        VERSION
                )
                .addProp(VERSION, MY_VERSION);
    }

    @Override
    public void modifyDeviceDetails(PropertyPanel pp, DeviceId deviceId) {
        pp.title(MY_DEVICE_TITLE);
        pp.removeProps(LATITUDE, LONGITUDE);

        pp.addButton(FOO_BUTTON)
                .addButton(BAR_BUTTON);

        pp.removeButtons(CoreButtons.SHOW_PORT_VIEW)
                .removeButtons(CoreButtons.SHOW_GROUP_VIEW)
                .removeButtons(CoreButtons.SHOW_METER_VIEW);
    }

}
