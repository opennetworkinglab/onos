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

import org.onosproject.ui.impl.topo.UiTopoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A lazily-initialized Singleton that creates and maintains the UI-model
 * of the network topology.
 */
public final class UiSharedTopologyModel {

    private static final Logger log =
            LoggerFactory.getLogger(UiSharedTopologyModel.class);


    private UiSharedTopologyModel() {
        // TODO: set up core model listeners and build the state of the model
    }

    // TODO: Note to Thomas (or others)..
    // Don't we have a common pattern for adding/removing listeners and
    //  invoking them when things happen?


    /**
     * Registers a UI topology session with the topology model.
     *
     * @param session the session to register
     */
    public void register(UiTopoSession session) {
        log.info("Registering topology session {}", session);
        // TODO: register the session
    }

    /**
     * Unregisters a UI topology session from the topology model.
     *
     * @param session the session to unregister
     */
    public void unregister(UiTopoSession session) {
        log.info("Unregistering topology session {}", session);
        // TODO: unregister the session
    }

    /**
     * Bill Pugh Singleton pattern. INSTANCE won't be instantiated until the
     * LazyHolder class is loaded via a call to the instance() method below.
     */
    private static class LazyHolder {
        private static final UiSharedTopologyModel INSTANCE =
                new UiSharedTopologyModel();
    }

    /**
     * Returns a reference to the Singleton UI network topology model.
     *
     * @return the singleton topology model
     */
    public static UiSharedTopologyModel instance() {
        return LazyHolder.INSTANCE;
    }
}
