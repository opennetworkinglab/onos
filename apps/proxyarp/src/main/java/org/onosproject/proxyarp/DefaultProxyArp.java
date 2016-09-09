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

package org.onosproject.proxyarp;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.neighbour.DefaultNeighbourMessageHandler;
import org.onosproject.incubator.net.neighbour.NeighbourResolutionService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.edge.EdgePortEvent;
import org.onosproject.net.edge.EdgePortListener;
import org.onosproject.net.edge.EdgePortService;

/**
 * Implements proxy ARP and NDP functionality by considering the entire network
 * as a single L2 broadcast domain.
 * <p>
 * This application maintains a DefaultNeighbourMessageHandler on all edge ports
 * in the network, and the handler implements the desired proxying functionality.
 * </p>
 */
@Component(immediate = true)
public class DefaultProxyArp {

    private static final String APP_NAME = "org.onosproject.proxyarp";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected EdgePortService edgeService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NeighbourResolutionService neighbourResolutionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private ApplicationId appId;

    private InternalEdgeListener edgeListener = new InternalEdgeListener();
    private DefaultNeighbourMessageHandler defaultHandler = new DefaultNeighbourMessageHandler();

    @Activate
    protected void activate() {
        appId = coreService.registerApplication(APP_NAME);

        edgeService.addListener(edgeListener);
        edgeService.getEdgePoints().forEach(this::addDefault);
    }

    @Deactivate
    protected void deactivate() {
        edgeService.removeListener(edgeListener);
        neighbourResolutionService.unregisterNeighbourHandlers(appId);
    }

    private void addDefault(ConnectPoint port) {
        neighbourResolutionService.registerNeighbourHandler(port, defaultHandler, appId);
    }

    private void removeDefault(ConnectPoint port) {
        neighbourResolutionService.unregisterNeighbourHandler(port, defaultHandler, appId);
    }

    private class InternalEdgeListener implements EdgePortListener {
        @Override
        public void event(EdgePortEvent event) {
            switch (event.type()) {
            case EDGE_PORT_ADDED:
                addDefault(event.subject());
                break;
            case EDGE_PORT_REMOVED:
                removeDefault(event.subject());
                break;
            default:
                break;
            }
        }
    }
}
