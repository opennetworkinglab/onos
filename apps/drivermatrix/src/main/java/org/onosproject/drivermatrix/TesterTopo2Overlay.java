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
 *
 */

package org.onosproject.drivermatrix;

import org.onosproject.ui.UiTopo2Overlay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A test implementation of UiTopo2Overlay.
 */
public class TesterTopo2Overlay extends UiTopo2Overlay {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // NOTE: this must match the ID defined in dmatrixTopo2v.js
    private static final String OVERLAY_ID = "dmatrix-test-overlay";
    private static final String NAME = "Test D-Matrix Overlay";

    /**
     * Constructs the overlay.
     */
    public TesterTopo2Overlay() {
        super(OVERLAY_ID, NAME);
        log.debug("+++ CREATE +++ TesterTopo2Overlay");
    }

    @Override
    public String glyphId() {
        return "thatsNoMoon";
    }

    @Override
    public void highlightingCallback() {
        // TODO: figure out what API to use to set highlights....

    }
}
