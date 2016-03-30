/*
 *  Copyright 2016 Open Networking Laboratory
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onosproject.ui.impl.topo.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A lazily-initialized Singleton that creates and maintains the UI-model
 * of the network topology.
 */
public final class UiSharedTopologyModel {

    private static final Logger log =
            LoggerFactory.getLogger(UiSharedTopologyModel.class);

    private static UiSharedTopologyModel singleton = null;

    private UiSharedTopologyModel() {
        // TODO: set up core model listeners and build the state of the model
    }

    public void register(UiTopoLayout layout) {
        log.info("Registering topology layout {}", layout);
        // TODO: register the view
    }

    public void unregister(UiTopoLayout layout) {
        log.info("Unregistering topology layout {}", layout);
        // TODO: unregister the view
    }

    /**
     * Returns a reference to the Singleton UI network topology model.
     *
     * @return the singleton topology model
     */
    public static synchronized UiSharedTopologyModel instance() {
        if (singleton == null) {
            log.info("Instantiating Singleton.");
            singleton = new UiSharedTopologyModel();
        }
        return singleton;
    }
}
