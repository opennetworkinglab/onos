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
package org.onosproject.sfcweb;

import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.ui.UiTopoOverlay;
import org.onosproject.ui.topo.PropertyPanel;


/**
 * Our sfcweb topology overlay.
 */
public class SfcwebUiTopovOverlay extends UiTopoOverlay {

    // NOTE: this must match the ID defined in sfcwebTopov.js
    private static final String OVERLAY_ID = "SFC-Service-overlay";
    private static final String MY_DEVICE_TITLE = "SFF specific device details";
    private static final String MY_HOST_TITLE = "SF specific host details";

    public SfcwebUiTopovOverlay() {
        super(OVERLAY_ID);
    }

    @Override
    public void modifyDeviceDetails(PropertyPanel pp, DeviceId deviceId) {
        pp.title(MY_DEVICE_TITLE);
        pp.removeAllProps();
        pp.addProp("SFF Device Id", deviceId.toString());
    }

    @Override
    public void modifyHostDetails(PropertyPanel pp, HostId hostId) {
        pp.title(MY_HOST_TITLE);
        pp.removeAllProps();
        pp.addProp("SF host Address", hostId.toString());
    }

}
