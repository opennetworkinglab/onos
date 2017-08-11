/*
 * Copyright 2015-present Open Networking Foundation
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

import org.onosproject.net.ConnectPoint;
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
     * @param pp       property panel model of device data
     * @param deviceId device id
     */
    public void modifyDeviceDetails(PropertyPanel pp, DeviceId deviceId) {
    }

    /**
     * Callback to modify the contents of the details panel for
     * a selected host.
     * This default implementation does nothing.
     *
     * @param pp     property panel model of host data
     * @param hostId host id
     */
    public void modifyHostDetails(PropertyPanel pp, HostId hostId) {
    }

    /**
     * Callback to modify the contents of the details panel for a selected
     * edge link. The parameters include identifiers for the host and the
     * connect point (device and port) to which the host is connected.
     * <p>
     * This default implementation does nothing.
     *
     * @param pp     property panel model of edge link data
     * @param hostId host ID
     * @param cp     connect point
     */
    public void modifyEdgeLinkDetails(PropertyPanel pp,
                                      HostId hostId, ConnectPoint cp) {
    }

    /**
     * Callback to modify the contents of the details panel for a selected
     * infrastructure link. The parameters include the two connect points
     * at either end of the link.
     * <p>
     * Note that links in the topology view are (usually) "bi-directional",
     * meaning that both {@literal A-->B} and {@literal B-->A} backing links
     * exist. If, however, there is only one backing link, it is guaranteed
     * to be {@literal A-->B}.
     * <p>
     * This default implementation does nothing.
     *
     * @param pp  property panel model of infrastructure link data
     * @param cpA connect point A
     * @param cpB connect point B
     */
    public void modifyInfraLinkDetails(PropertyPanel pp,
                                       ConnectPoint cpA, ConnectPoint cpB) {
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
     * and is superceded by use of {@link #modifyEdgeLinkDetails} and
     * {@link #modifyInfraLinkDetails}.
     */
    @Deprecated
    public Map<String, String> additionalLinkData(LinkEvent event) {
        return null;
    }
}
