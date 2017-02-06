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

package org.onosproject.ui.impl;

import org.onosproject.ui.UiTopoOverlay;

/**
 * Topology Overlay for network traffic.
 */
public class ProtectedIntentOverlay extends UiTopoOverlay {
    /**
     * Traffic Overlay identifier.
     */
    public static final String PROTECTED_INTENTS_ID = "protectedIntent";

    public ProtectedIntentOverlay() {
        super(PROTECTED_INTENTS_ID);
    }

    // override activate and deactivate, to write log messages
    @Override
    public void activate() {
        super.activate();
        log.debug("ProtectedIntentOverlay Activated");
    }

    @Override
    public void deactivate() {
        super.deactivate();
        log.debug("ProtectedIntentOverlay Deactivated");
    }
}
