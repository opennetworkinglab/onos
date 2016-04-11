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

import org.onosproject.event.EventDispatcher;
import org.onosproject.net.Device;
import org.onosproject.ui.model.topo.UiDevice;

/**
 * UI Topology Model cache.
 */
class ModelCache {

    private final EventDispatcher dispatcher;

    ModelCache(EventDispatcher eventDispatcher) {
        this.dispatcher = eventDispatcher;
    }

    /**
     * Clear our model.
     */
    void clear() {
        // TODO: clear our internal state
    }

    /**
     * Create our internal model of the global topology.
     */
    void load() {
//        loadClusterMembers();
//        loadRegions();
//        loadDevices();
//        loadHosts();
//        loadLinks();
    }


    // add or update device
    void addOrUpdateDevice(Device device) {
        // fetch UiDevice
        UiDevice uiDevice = new UiDevice();

        dispatcher.post(
                new UiModelEvent(UiModelEvent.Type.DEVICE_ADDED, uiDevice)
        );

    }

    void removeDevice(Device device) {
        UiDevice uiDevice = new UiDevice();

        dispatcher.post(
                new UiModelEvent(UiModelEvent.Type.DEVICE_REMOVED, uiDevice)
        );

    }

    // TODO remaining model objects

}
