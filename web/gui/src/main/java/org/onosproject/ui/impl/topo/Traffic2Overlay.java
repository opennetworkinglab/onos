/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.ui.impl.topo;

import org.onosproject.ui.UiTopo2Overlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Traffic overlay for topology 2 view.
 */
public class Traffic2Overlay extends UiTopo2Overlay {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // NOTE: this must match the ID defined in topo2TrafficOverlay.js
    public static final String OVERLAY_ID = "traffic-2-overlay";

    /**
     * Creates a traffic overlay instance.
     */
    public Traffic2Overlay() {
        super(OVERLAY_ID);
    }

    @Override
    public void highlightingCallback() {
        log.debug("highlightingCallback() invoked");
        // TODO: implement
    }
}
