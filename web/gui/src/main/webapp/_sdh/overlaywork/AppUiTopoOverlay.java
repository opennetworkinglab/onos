/*
 * Copyright 2015 Open Networking Laboratory
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

package org.meowster.over;

import org.onosproject.ui.UiTopoOverlay;
import org.onosproject.ui.topo.ButtonId;
import org.onosproject.ui.topo.PropertyPanel;
import org.onosproject.ui.topo.TopoConstants.CoreButtons;
import org.onosproject.ui.topo.TopoConstants.Glyphs;

import static org.onosproject.ui.topo.TopoConstants.Properties.*;

/**
 * Our topology overlay.
 */
public class AppUiTopoOverlay extends UiTopoOverlay {

    // NOTE: this must match the ID defined in topov.js
    private static final String OVERLAY_ID = "meowster-overlay";

    private static final String MY_TITLE = "I changed the title";
    private static final String MY_VERSION = "Beta-1.0.0042";

    private static final ButtonId FOO_BUTTON = new ButtonId("foo");
    private static final ButtonId BAR_BUTTON = new ButtonId("bar");

    public AppUiTopoOverlay() {
        super(OVERLAY_ID);
    }


    @Override
    public void modifySummary(PropertyPanel pp) {
        pp.title("My App Rocks!")
                .typeId(Glyphs.CROWN)
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
    public void modifyDeviceDetails(PropertyPanel pp) {
        pp.title(MY_TITLE);
        pp.removeProps(LATITUDE, LONGITUDE);

        pp.addButton(FOO_BUTTON)
                .addButton(BAR_BUTTON);

        pp.removeButtons(CoreButtons.SHOW_PORT_VIEW)
                .removeButtons(CoreButtons.SHOW_GROUP_VIEW);
    }

}
