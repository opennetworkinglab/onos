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

import org.onosproject.net.region.RegionId;
import org.onosproject.ui.UiTopoLayoutService;
import org.onosproject.ui.impl.UiWebSocket;
import org.onosproject.ui.model.topo.UiLinkId;
import org.onosproject.ui.model.topo.UiModelEvent;
import org.onosproject.ui.impl.topo.model.UiModelListener;
import org.onosproject.ui.impl.topo.model.UiSharedTopologyModel;
import org.onosproject.ui.model.topo.UiClusterMember;
import org.onosproject.ui.model.topo.UiNode;
import org.onosproject.ui.model.topo.UiRegion;
import org.onosproject.ui.model.topo.UiSynthLink;
import org.onosproject.ui.model.topo.UiTopoLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    private static final String TOPO2_UI_MODEL_EVENT = "topo2UiModelEvent";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final UiWebSocket webSocket;
    private final String username;
    private final Topo2Jsonifier t2json;

    final UiSharedTopologyModel sharedModel;

    private boolean registered = false;

    private UiTopoLayoutService layoutService;
    private UiTopoLayout currentLayout;
    private boolean messagesEnabled = true;

    /**
     * Creates a new topology session for the specified web socket connection,
     * and references to JSONifier, shared model, and layout service.
     *
     * @param webSocket     web socket
     * @param jsonifier     JSONifier instance
     * @param model         share topology model
     * @param layoutService topology layout service
     */
    public UiTopoSession(UiWebSocket webSocket,
                         Topo2Jsonifier jsonifier,
                         UiSharedTopologyModel model,
                         UiTopoLayoutService layoutService) {
        this.webSocket = webSocket;
        this.username = webSocket.userName();
        this.t2json = jsonifier;
        this.sharedModel = model;
        this.layoutService = layoutService;
    }

    // constructs a neutered instance, for unit testing
    UiTopoSession() {
        webSocket = null;
        username = null;
        t2json = null;
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
    public boolean isRelevant(UiModelEvent event) {
        if (!messagesEnabled) {
            return false;
        }
        UiRegion uiRegion = sharedModel.getRegion(currentLayout.regionId());
        return uiRegion.isRelevant(event);
    }

    @Override
    public void event(UiModelEvent event) {
        webSocket.sendMessage(TOPO2_UI_MODEL_EVENT, t2json.jsonEvent(event));
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
     * Returns the breadcrumb trail from current layout to root. That is,
     * element 0 of the list will be the current layout; the last element
     * of the list will be the root layout. This list is guaranteed to have
     * size of at least 1.
     *
     * @return breadcrumb trail
     */
    public List<UiTopoLayout> breadCrumbs() {
        UiTopoLayout current = currentLayout;
        List<UiTopoLayout> crumbs = new ArrayList<>();
        crumbs.add(current);
        while (!current.isRoot()) {
            current = layoutService.getLayout(current.parent());
            crumbs.add(current);
        }
        return crumbs;
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
        RegionId rid = layout.regionId();
        return rid == null ? sharedModel.getNullRegion() : sharedModel.getRegion(rid);
    }

    /**
     * Returns the regions/devices that are "peers" to this region. That is,
     * based on the layout the user is viewing, all the regions/devices that
     * are associated with layouts that share the same parent layout as this
     * layout, AND that are linked to an element within this region.
     *
     * @param layout the layout being viewed
     * @return all regions/devices that are "siblings" to this layout's region
     */
    public Set<UiNode> getPeerNodes(UiTopoLayout layout) {
        Set<UiNode> peers = new HashSet<>();

        // first, get the peer regions
        Set<UiTopoLayout> peerLayouts = layoutService.getPeerLayouts(layout.id());
        peerLayouts.forEach(l -> {
            RegionId peerRegion = l.regionId();
            peers.add(sharedModel.getRegion(peerRegion));
        });

        // now add the devices that reside in the parent region
        if (!layout.isRoot()) {
            UiTopoLayout parentLayout = layoutService.getLayout(layout.parent());
            peers.addAll(getRegion(parentLayout).devices());
        }

        // TODO: Finally, filter out regions / devices that are not connected
        //       directly to this region by an implicit link
        return peers;
    }

    /**
     * Returns the subregions of the region in the specified layout.
     *
     * @param layout the layout being viewed
     * @return all regions that are "contained within" this layout's region
     */
    public Set<UiRegion> getSubRegions(UiTopoLayout layout) {
        Set<UiTopoLayout> kidLayouts = layoutService.getChildren(layout.id());
        Set<UiRegion> kids = new HashSet<>();
        kidLayouts.forEach(l -> kids.add(sharedModel.getRegion(l.regionId())));
        return kids;
    }

    /**
     * Returns the (synthetic) links of the region in the specified layout.
     *
     * @param layout the layout being viewed
     * @return all links that are contained by this layout's region
     */
    public List<UiSynthLink> getLinks(UiTopoLayout layout) {
        return sharedModel.getSynthLinks(layout.regionId());
    }

    /**
     * Refreshes the model's internal state.
     */
    public void refreshModel() {
        sharedModel.refresh();
    }

    /**
     * Navigates to the specified region by setting the associated layout as
     * current.
     *
     * @param regionId region identifier
     */
    public void navToRegion(String regionId) {
        // 1. find the layout corresponding to the region ID
        // 2. set this layout to be "current"
        RegionId r = RegionId.regionId(regionId);
        UiTopoLayout layout = layoutService.getLayout(r);
        setCurrentLayout(layout);
    }

    /**
     * Returns synthetic links that are in the current region, mapped by
     * original link ID.
     *
     * @return map of synth links
     */
    public Map<UiLinkId, UiSynthLink> relevantSynthLinks() {
        return sharedModel.relevantSynthLinks(currentLayout.regionId());
    }
}
