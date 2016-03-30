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
 * Base class for modeling the Topology View layout.
 * <p>
 * Note that an instance of this class will be created for each
 * {@link org.onosproject.ui.impl.UiWebSocket} connection, and will contain
 * the state of how the topology is laid out for the logged-in user.
 */
public class UiTopoLayout {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final String username;
    private final UiSharedTopologyModel sharedModel;

    private boolean registered = false;

    /**
     * Creates a new topology layout.
     */
    public UiTopoLayout(String username) {
        this.username = username;
        this.sharedModel = UiSharedTopologyModel.instance();
    }

    /**
     * Initializes the layout; registering with the shared model.
     */
    public void init() {
        if (!registered) {
            sharedModel.register(this);
            registered = true;
        } else {
            log.warn("already registered");
        }
    }

    /**
     * Destroys the layout; unregistering from the shared model.
     */
    public void destroy() {
        if (!registered) {
            sharedModel.unregister(this);
        } else {
            log.warn("already unregistered");
        }
    }

    @Override
    public String toString() {
        return String.format("{UiTopoLayout for user <%s>}", username);
    }
}
