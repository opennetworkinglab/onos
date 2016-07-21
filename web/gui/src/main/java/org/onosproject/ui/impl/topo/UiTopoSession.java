/*
 *  Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.ui.impl.topo;

import org.onosproject.ui.UiTopoLayoutService;
import org.onosproject.ui.impl.UiWebSocket;
import org.onosproject.ui.impl.topo.model.UiModelEvent;
import org.onosproject.ui.impl.topo.model.UiModelListener;
import org.onosproject.ui.impl.topo.model.UiSharedTopologyModel;
import org.onosproject.ui.model.topo.UiClusterMember;
import org.onosproject.ui.model.topo.UiDevice;
import org.onosproject.ui.model.topo.UiHost;
import org.onosproject.ui.model.topo.UiLink;
import org.onosproject.ui.model.topo.UiRegion;
import org.onosproject.ui.model.topo.UiTopoLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Coordinates with the {@link UiTopoLayoutService} to access
 * {@link UiTopoLayout}s, and with the {@link UiSharedTopologyModel} which
 * maintains a local model of the network entities, tailored specifically
 * for displaying on the UI.
 * <p>
 * Note that an instance of this class will be created for each
 * {@link UiWebSocket} connection, and will contain
 * the state of how the topology is laid out for the logged-in user.
 * <p>
 * The expected pattern is for the {@link Topo2ViewMessageHandler} to obtain
 * a reference to the session instance (via the {@link UiWebSocket}), and
 * interact with it when topo-related events come in from the client.
 */
public class UiTopoSession implements UiModelListener {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final UiWebSocket webSocket;
    private final String username;

    final UiSharedTopologyModel sharedModel;

    private boolean registered = false;

    private UiTopoLayoutService layoutService;
    private UiTopoLayout currentLayout;
    private boolean messagesEnabled;

    /**
     * Creates a new topology session for the specified web socket connection.
     *
     * @param webSocket     web socket
     * @param model         share topology model
     * @param layoutService topology layout service
     */
    public UiTopoSession(UiWebSocket webSocket,
                         UiSharedTopologyModel model,
                         UiTopoLayoutService layoutService) {
        this.webSocket = webSocket;
        this.username = webSocket.userName();
        this.sharedModel = model;
        this.layoutService = layoutService;
    }

    // constructs a neutered instance, for unit testing
    UiTopoSession() {
        webSocket = null;
        username = null;
        sharedModel = null;
    }

    /**
     * Initializes the session; registering with the shared model.
     */
    public void init() {
        if (!registered) {
            log.debug("{} : Registering with shared model", this);
            sharedModel.register(this);
            currentLayout = layoutService.getRootLayout();
            registered = true;
        } else {
            log.warn("already registered");
        }
    }

    /**
     * Destroys the session; unregistering from the shared model.
     */
    public void destroy() {
        if (registered) {
            log.debug("{} : Unregistering from shared model", this);
            sharedModel.unregister(this);
            registered = false;
        } else {
            log.warn("already unregistered");
        }
    }

    @Override
    public String toString() {
        return String.format("{UiTopoSession for user <%s>}", username);
    }

    @Override
    public void event(UiModelEvent event) {
        log.debug("Event received: {}", event);
        // TODO: handle model events from the cache...
    }

    /**
     * Returns the current layout context.
     *
     * @return current topology layout
     */
    public UiTopoLayout currentLayout() {
        return currentLayout;
    }

    /**
     * Changes the current layout context to the specified layout.
     *
     * @param topoLayout new topology layout context
     */
    public void setCurrentLayout(UiTopoLayout topoLayout) {
        currentLayout = topoLayout;
    }

    /**
     * Enables or disables the transmission of topology event update messages.
     *
     * @param enabled true if messages should be sent
     */
    public void enableEvent(boolean enabled) {
        messagesEnabled = enabled;
    }

    /**
     * Returns the list of ONOS instances (cluster members).
     *
     * @return the list of ONOS instances
     */
    public List<UiClusterMember> getAllInstances() {
        return sharedModel.getClusterMembers();
    }

    /**
     * Returns the region for the specified layout.
     *
     * @param layout layout filter
     * @return region that the layout is based upon
     */
    public UiRegion getRegion(UiTopoLayout layout) {
        return sharedModel.getRegion(layout.regionId());
    }

    /**
     * Returns the regions that are "peers" to this region. That is, based on
     * the layout the user is viewing, all the regions that are associated with
     * layouts that are children of the parent layout to this layout.
     *
     * @param layout the layout being viewed
     * @return all regions that are "siblings" to this layout's region
     */
    public Set<UiRegion> getPeerRegions(UiTopoLayout layout) {
        UiRegion currentRegion = getRegion(layout);

        // TODO: consult topo layout service to get hierarchy info...
        // TODO: then consult shared model to get regions
        return Collections.emptySet();
    }

    /**
     * Returns the subregions of the region in the specified layout.
     *
     * @param layout the layout being viewed
     * @return all regions that are "contained within" this layout's region
     */
    public Set<UiRegion> getSubRegions(UiTopoLayout layout) {
        UiRegion currentRegion = getRegion(layout);

        // TODO: consult topo layout service to get child layouts...
        // TODO: then consult shared model to get regions
        return Collections.emptySet();
    }


    /**
     * Returns all devices that are not in a region.
     *
     * @return all devices not in a region
     */
    public Set<UiDevice> getOrphanDevices() {
        // TODO: get devices with no region
        return Collections.emptySet();
    }

    /**
     * Returns all hosts that are not in a region.
     *
     * @return all hosts not in a region
     */
    public Set<UiHost> getOrphanHosts() {
        // TODO: get hosts with no region
        return Collections.emptySet();
    }

    /**
     * Returns all links that are not in a region.
     *
     * @return all links not in a region
     */
    public Set<UiLink> getOrphanLinks() {
        // TODO: get links with no region
        return Collections.emptySet();
    }

}
