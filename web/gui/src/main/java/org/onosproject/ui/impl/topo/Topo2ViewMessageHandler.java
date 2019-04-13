/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.ui.impl.topo;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.impl.UiWebSocket;
import org.onosproject.ui.model.topo.UiClusterMember;
import org.onosproject.ui.model.topo.UiNode;
import org.onosproject.ui.model.topo.UiRegion;
import org.onosproject.ui.model.topo.UiSynthLink;
import org.onosproject.ui.model.topo.UiTopoLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/*
 NOTES:

    The original topology view message handler was broken into two classes
    TopologyViewMessageHandler, and TopologyViewMessageHandlerBase.

    We do not need to follow that model necessarily. Instead, we have this
    class and Topo2Jsonifier, which takes UiModel objects and renders them
    as JSON objects.

 */

/**
 * Server-side component for interacting with the new "Region aware" topology
 * view in the Web UI.
 */
public class Topo2ViewMessageHandler extends UiMessageHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // === Inbound event identifiers
    private static final String START = "topo2Start";
    private static final String NAV_REGION = "topo2navRegion";
    private static final String STOP = "topo2Stop";
    private static final String UPDATE_META2 = "updateMeta2";

    // === Outbound event identifiers
    private static final String ALL_INSTANCES = "topo2AllInstances";
    private static final String CURRENT_LAYOUT = "topo2CurrentLayout";
    private static final String CURRENT_REGION = "topo2CurrentRegion";
    private static final String PEER_REGIONS = "topo2PeerRegions";
    private static final String OVERLAYS = "topo2Overlays";
    private static final String TOPO_SELECT_OVERLAY = "topoSelectOverlay";

    // fields
    private static final String ACTIVATE = "activate";
    private static final String DEACTIVATE = "deactivate";

    private UiTopoSession topoSession;
    private Topo2Jsonifier t2json;
    private Topo2OverlayCache overlay2Cache;
    private Topo2TrafficMessageHandler trafficHandler;


    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);

        // get the topo session from the UiWebSocket
        topoSession = ((UiWebSocket) connection).topoSession();
        t2json = new Topo2Jsonifier(directory, connection.userName());
    }

    /**
     * Sets a reference to the overlay cache for interacting with registered
     * overlays.
     *
     * @param overlay2Cache the overlay cache
     */
    public void setOverlayCache(Topo2OverlayCache overlay2Cache) {
        this.overlay2Cache = overlay2Cache;
    }

    /**
     * Sets a reference to the traffic message handler.
     *
     * @param traffic the traffic message handler instance
     */
    public void setTrafficHandler(Topo2TrafficMessageHandler traffic) {
        trafficHandler = traffic;
    }


    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new Topo2Start(),
                new Topo2NavRegion(),
                new Topo2Stop(),
                new Topo2UpdateMeta()
        );
    }

    private final class TopoSelectOverlay extends RequestHandler {
        private TopoSelectOverlay() {
            super(TOPO_SELECT_OVERLAY);
        }

        @Override
        public void process(ObjectNode payload) {
            String deact = string(payload, DEACTIVATE);
            String act = string(payload, ACTIVATE);
            overlay2Cache.switchOverlay(deact, act);
        }
    }

    // ==================================================================

    private ObjectNode mkLayoutMessage(UiTopoLayout currentLayout) {
        List<UiTopoLayout> crumbs = topoSession.breadCrumbs();
        return t2json.layout(currentLayout, crumbs);
    }

    private ObjectNode mkRegionMessage(UiTopoLayout currentLayout) {
        UiRegion region = topoSession.getRegion(currentLayout);
        Set<UiRegion> kids = topoSession.getSubRegions(currentLayout);
        List<UiSynthLink> links = topoSession.getLinks(currentLayout);
        return t2json.region(region, kids, links);
    }

    private ObjectNode mkPeersMessage(UiTopoLayout currentLayout) {
        Set<UiNode> peers = topoSession.getPeerNodes(currentLayout);
        ObjectNode peersPayload = objectNode();
        String rid = currentLayout.regionId().toString();
        peersPayload.set("peers", t2json.closedNodes(rid, peers));
        return peersPayload;
    }

    // ==================================================================


    private final class Topo2Start extends RequestHandler {
        private Topo2Start() {
            super(START);
        }

        @Override
        public void process(ObjectNode payload) {
            // client view is ready to receive data to display; so start up
            // server-side processing, and send over initial state

            log.debug("topo2Start: {}", payload);

            // this may be a little heavyweight, but it might be safer to do
            //  this than make assumptions about the order in which devices
            //  and regions are added... and thus internal linkages set up
            //  correctly
            topoSession.refreshModel();

            // start with the list of ONOS cluster members
            List<UiClusterMember> instances = topoSession.getAllInstances();
            sendMessage(ALL_INSTANCES, t2json.instances(instances));


            // Send layout, region, peers data...

            // this is the layout that the user has chosen to display
            UiTopoLayout currentLayout = topoSession.currentLayout();
            sendMessage(CURRENT_LAYOUT, mkLayoutMessage(currentLayout));

            // this is the region that is associated with the current layout
            //   this message includes details of the sub-regions, devices,
            //   hosts, and links within the region
            //   (as well as layer-order hints)
            sendMessage(CURRENT_REGION, mkRegionMessage(currentLayout));

            // these are the regions/devices that are siblings to this region
            sendMessage(PEER_REGIONS, mkPeersMessage(currentLayout));
        }
    }

    private final class Topo2NavRegion extends RequestHandler {
        private Topo2NavRegion() {
            super(NAV_REGION);
        }

        @Override
        public void process(ObjectNode payload) {
            String rid = string(payload, "rid");
            log.debug("topo2navRegion: rid={}", rid);

            // NOTE: we are NOT re-issuing information about the cluster nodes

            // switch to the selected region...
            topoSession.navToRegion(rid);

            // re-send layout, region, peers data...
            UiTopoLayout currentLayout = topoSession.currentLayout();
            sendMessage(CURRENT_LAYOUT, mkLayoutMessage(currentLayout));
            sendMessage(CURRENT_REGION, mkRegionMessage(currentLayout));
            sendMessage(PEER_REGIONS, mkPeersMessage(currentLayout));
        }
    }

    private final class Topo2Stop extends RequestHandler {
        private Topo2Stop() {
            super(STOP);
        }

        @Override
        public void process(ObjectNode payload) {
            // client view has gone away; so shut down server-side processing

            log.debug("topo2Stop: {}", payload);
            trafficHandler.ceaseAndDesist();

            // OLD CODE DID THE FOLLOWING...
//            stopSummaryMonitoring();
        }
    }

    private final class Topo2UpdateMeta extends RequestHandler {
        private Topo2UpdateMeta() {
            super(UPDATE_META2);
        }

        @Override
        public void process(ObjectNode payload) {
            // NOTE: metadata for a node is stored within the context of the
            //       current region.
            String rid = topoSession.currentLayout().regionId().toString();
            t2json.updateMeta(rid, payload);
        }
    }

}
