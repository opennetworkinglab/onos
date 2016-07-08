/*
 * Copyright 2016-present Open Networking Laboratory
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
import org.onosproject.ui.model.topo.UiRegion;
import org.onosproject.ui.model.topo.UiTopoLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

/*
 NOTES:

    The original topology view message handler was broken into two classes
    TopologyViewMessageHandler, and TopologyViewMessageHandlerBase.
    We do not need to follow that model necessarily. Starting with a
    single class, and breaking it apart later if necessary.

    Need to figure out the connection between this message handler and the
    new way of doing things with UiTopoSession...

 */

/**
 * Server-side component for interacting with the new "Region aware" topology
 * view in the Web UI.
 */
public class Topo2ViewMessageHandler extends UiMessageHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // === Inbound event identifiers
    private static final String TOPO2_START = "topo2Start";
    private static final String TOPO2_STOP = "topo2Stop";

    // === Outbound event identifiers
    private static final String CURRENT_LAYOUT = "topo2CurrentLayout";
    private static final String CURRENT_REGION = "topo2CurrentRegion";
    private static final String ALL_INSTANCES = "topo2AllInstances";
    private static final String TOPO_START_DONE = "topo2StartDone";

    private UiTopoSession topoSession;
    private Topo2Jsonifier t2json;


    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);

        // get the topo session from the UiWebSocket
        topoSession = ((UiWebSocket) connection).topoSession();
        t2json = new Topo2Jsonifier(directory);
    }

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new Topo2Start(),
                new Topo2Stop()
        );
    }

    // ==================================================================


    private final class Topo2Start extends RequestHandler {
        private Topo2Start() {
            super(TOPO2_START);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            // client view is ready to receive data to display; so start up
            // server-side processing, and send over initial state

            log.debug("topo2Start: {}", payload);

            List<UiClusterMember> instances = topoSession.getAllInstances();
            sendMessage(ALL_INSTANCES, t2json.instances(instances));

            UiTopoLayout currentLayout = topoSession.currentLayout();
            sendMessage(CURRENT_LAYOUT, t2json.layout(currentLayout));

            UiRegion region = topoSession.getRegion(currentLayout);
            sendMessage(CURRENT_REGION, t2json.region(region));

            // TODO: send information about devices/hosts/links in non-region
            // TODO: send information about "linked, peer" regions

            sendMessage(TOPO_START_DONE, null);


            // OLD CODE DID THE FOLLOWING...
//            addListeners();
//            sendAllInstances(null);
//            sendAllDevices();
//            sendAllLinks();
//            sendAllHosts();
//            sendTopoStartDone();
        }
    }

    private final class Topo2Stop extends RequestHandler {
        private Topo2Stop() {
            super(TOPO2_STOP);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            // client view has gone away; so shut down server-side processing
            // TODO: implement...

            log.debug("topo2Stop: {}", payload);

            // OLD CODE DID THE FOLLOWING...
//            removeListeners();
//            stopSummaryMonitoring();
//            traffic.stopMonitoring();
        }
    }

}
