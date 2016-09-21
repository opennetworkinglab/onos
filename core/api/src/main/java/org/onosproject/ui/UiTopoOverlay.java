/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.ui;

import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.ui.topo.PropertyPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Represents user interface topology view overlay.
 * <p>
 * This base class does little more than provide a logger and an identifier.
 * Subclasses will probably want to override some or all of the base methods
 * to do useful things during the life-cycle of the overlay.
 */
public class UiTopoOverlay {

    /**
     * Logger for this overlay.
     */
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final String id;

    private boolean isActive = false;

    /**
     * Creates a new user interface topology view overlay descriptor, with
     * the given identifier.
     *
     * @param id overlay identifier
     */
    public UiTopoOverlay(String id) {
        this.id = id;
    }

    /**
     * Returns the identifier for this overlay.
     *
     * @return the identifier
     */
    public String id() {
        return id;
    }

    /**
     * Callback invoked to initialize this overlay, soon after creation.
     * This default implementation does nothing.
     */
    public void init() {
    }

    /**
     * Callback invoked when this overlay is activated.
     */
    public void activate() {
        isActive = true;
    }

    /**
     * Callback invoked when this overlay is deactivated.
     */
    public void deactivate() {
        isActive = false;
    }

    /**
     * Returns true if this overlay is currently active.
     *
     * @return true if overlay active
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Callback invoked to destroy this instance by cleaning up any
     * internal state ready for garbage collection.
     * This default implementation holds no state and does nothing.
     */
    public void destroy() {
    }

    /**
     * Callback to modify the contents of the summary panel.
     * This default implementation does nothing.
     *
     * @param pp property panel model of summary data
     */
    public void modifySummary(PropertyPanel pp) {
    }

    /**
     * Callback to modify the contents of the details panel for
     * a selected device.
     * This default implementation does nothing.
     *
     * @param pp       property panel model of summary data
     * @param deviceId device id
     */
    public void modifyDeviceDetails(PropertyPanel pp, DeviceId deviceId) {
    }

    /**
     * Callback to modify the contents of the details panel for
     * a selected host.
     * This default implementation does nothing.
     *
     * @param pp     property panel model of summary data
     * @param hostId host id
     */
    public void modifyHostDetails(PropertyPanel pp, HostId hostId) {
    }

    /**
     * Callback invoked when a link event is processed (e.g.&nbsp;link added).
     * A subclass may override this method to return a map of property
     * key/value pairs to be included in the JSON event back to the client,
     * so that those additional properties are available to be displayed as
     * link details.
     * <p>
     * The default implementation returns {@code null}, that is, no additional
     * properties to be added.
     *
     * @param event the link event
     * @return map of additional key/value pairs to be added to the JSON event
     * @deprecated this is a temporary addition for Goldeneye (1.6) release,
     * and expected to be replaced in the Ibis (1.8) release
     */
    @Deprecated
    public Map<String, String> additionalLinkData(LinkEvent event) {
        return null;
    }
}
