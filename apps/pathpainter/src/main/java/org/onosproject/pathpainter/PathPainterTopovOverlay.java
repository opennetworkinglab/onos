/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.pathpainter;

import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.ui.UiTopoOverlay;
import org.onosproject.ui.topo.ButtonId;
import org.onosproject.ui.topo.PropertyPanel;

/**
 * Our topology overlay.
 */
public class PathPainterTopovOverlay extends UiTopoOverlay {

    // NOTE: this must match the ID defined in ppTopovOverlay.js
    private static final String OVERLAY_ID = "pp-overlay";

    private static final ButtonId SRC_BUTTON = new ButtonId("src");
    private static final ButtonId DST_BUTTON = new ButtonId("dst");

    public PathPainterTopovOverlay() {
        super(OVERLAY_ID);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        log.debug("PathPainterOverlay Deactivated");
    }

    @Override
    public void modifyDeviceDetails(PropertyPanel pp, DeviceId deviceId) {
        pp.addButton(SRC_BUTTON).addButton(DST_BUTTON);
    }

    @Override
    public void modifyHostDetails(PropertyPanel pp, HostId hostId) {
        pp.addButton(SRC_BUTTON).addButton(DST_BUTTON);
    }


}
