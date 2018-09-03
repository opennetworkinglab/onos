/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstackvtap.gui;

import org.onosproject.ui.UiTopoOverlay;

public class OpenstackVtapUiTopovOverlay extends UiTopoOverlay {
    private static final String OVERLAY_ID = "vtap-overlay";

    public OpenstackVtapUiTopovOverlay() {
        super(OVERLAY_ID);
    }

    @Override
    public void activate() {
        super.activate();
        log.debug("Openstack VtapOverlay Activated");
    }

    @Override
    public void deactivate() {
        super.deactivate();
        log.debug("Openstack VtapOverlay Deactivated");
    }

}

