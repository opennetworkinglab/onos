/*
 * Copyright 2017-present Open Networking Foundation
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
 *
 */

package org.onosproject.ui.impl.topo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.impl.TrafficMonitorBase.Mode;
import org.onosproject.ui.impl.UiWebSocket;
import org.onosproject.ui.impl.topo.util.ServicesBundle;
import org.onosproject.ui.model.topo.UiLinkId;
import org.onosproject.ui.model.topo.UiSynthLink;
import org.onosproject.ui.topo.Highlights;
import org.onosproject.ui.topo.NodeSelection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.onosproject.net.HostId.hostId;
import static org.onosproject.ui.topo.TopoJson.topo2HighlightsMessage;

/**
 * Server-side component to handle messages pertaining to topo-2 traffic.
 */
public class Topo2TrafficMessageHandler extends TopoologyTrafficMessageHandlerAbstract {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // === Inbound event identifiers
    private static final String REQUEST_ALL_TRAFFIC = "topo2RequestAllTraffic";
    private static final String CANCEL_TRAFFIC = "topo2CancelTraffic";
    private static final String ADD_HOST_INTENT = "topo2AddHostIntent";
    private static final String ADD_MULTI_SRC_INTENT = "topo2AddMultiSourceIntent";
    private static final String REQ_RELATED_INTENTS = "topo2RequestRelatedIntents";

    // field values
    private static final String TRAFFIC_TYPE = "trafficType";
    private static final String FLOW_STATS_BYTES = "flowStatsBytes";
    private static final String PORT_STATS_BIT_SEC = "portStatsBitSec";
    private static final String PORT_STATS_PKT_SEC = "portStatsPktSec";

    // fields
    private static final String ONE = "one";
    private static final String TWO = "two";
    private static final String SRC = "src";
    private static final String DST = "dst";

    protected ServicesBundle services;
    private static final String MY_APP_ID = "org.onosproject.gui";
    private ApplicationId appId;
    private UiTopoSession topoSession;
    private Topo2OverlayCache overlay2Cache;
    private Traffic2Monitor traffic2;


    @Override
    public void init(UiConnection connection, ServiceDirectory directory) {
        super.init(connection, directory);
        appId = directory.get(CoreService.class).registerApplication(MY_APP_ID);
        services = new ServicesBundle(directory);
        traffic2 = new Traffic2Monitor(services, this);
        topoSession = ((UiWebSocket) connection).topoSession();
    }

    @Override
    public void destroy() {
        super.destroy();
        traffic2.stopMonitoring();
    }

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(
                new Topo2AllTraffic(),
                new Topo2CancelTraffic(),
                new Topo2AddHostIntent(),
                new Topo2AddMultiSourceIntent()
        );
    }

    /**
     * Injects the topology overlay cache.
     *
     * @param overlay2Cache injected cache
     */
    public void setOverlayCache(Topo2OverlayCache overlay2Cache) {
        this.overlay2Cache = overlay2Cache;
    }

    /**
     * Shuts down the background traffic monitoring task.
     */
    void ceaseAndDesist() {
        traffic2.stopMonitoring();
    }

    /**
     * Sends a highlights message back to the client.
     *
     * @param highlights the highlights for transmission
     */
    @Override
    public void sendHighlights(Highlights highlights) {
        sendMessage(topo2HighlightsMessage(highlights));
    }

    /**
     * Asks the topo session for the relevant synth links for current region.
     * The returned map is keyed by "original" link.
     *
     * @return synth link map
     */
    Map<UiLinkId, UiSynthLink> retrieveRelevantSynthLinks() {
        return topoSession.relevantSynthLinks();
    }

    // ==================================================================

    private final class Topo2AllTraffic extends RequestHandler {

        private Topo2AllTraffic() {
            super(REQUEST_ALL_TRAFFIC);
        }

        @Override
        public void process(ObjectNode payload) {
            String mode = string(payload, TRAFFIC_TYPE);
            log.debug("SHOW TRAFFIC: {}", mode);

            switch (mode) {
                case FLOW_STATS_BYTES:
                    traffic2.monitor(Mode.ALL_FLOW_TRAFFIC_BYTES);
                    break;

                case PORT_STATS_BIT_SEC:
                    traffic2.monitor(Mode.ALL_PORT_TRAFFIC_BIT_PS);
                    break;

                case PORT_STATS_PKT_SEC:
                    traffic2.monitor(Mode.ALL_PORT_TRAFFIC_PKT_PS);
                    break;

                default:
                    log.warn("Unknown traffic monitor type: " + mode);
                    break;
            }
        }
    }

    private final class Topo2CancelTraffic extends RequestHandler {
        private Topo2CancelTraffic() {
            super(CANCEL_TRAFFIC);
        }

        @Override
        public void process(ObjectNode payload) {
            log.debug("CANCEL TRAFFIC");
            traffic2.stopMonitoring();
        }
    }

    private final class Topo2AddHostIntent extends RequestHandler {
        private Topo2AddHostIntent() {
            super(ADD_HOST_INTENT);
        }

        @Override
        public void process(ObjectNode payload) {
            // TODO: add protection against device ids and non-existent hosts.
            HostId one = hostId(string(payload, ONE));
            HostId two = hostId(string(payload, TWO));

            HostToHostIntent intent = HostToHostIntent.builder()
                    .appId(appId)
                    .one(one)
                    .two(two)
                    .build();

            services.intent().submit(intent);
            if (overlay2Cache.isActive(Traffic2Overlay.OVERLAY_ID)) {
                traffic2.monitor(intent);
            }
        }
    }

    private final class Topo2AddMultiSourceIntent extends RequestHandler {
        private Topo2AddMultiSourceIntent() {
            super(ADD_MULTI_SRC_INTENT);
        }

        @Override
        public void process(ObjectNode payload) {
            // TODO: add protection against device ids and non-existent hosts.
            Set<HostId> src = getHostIds((ArrayNode) payload.path(SRC));
            HostId dst = hostId(string(payload, DST));
            Host dstHost = services.host().getHost(dst);

            Set<FilteredConnectPoint> ingressPoints = getHostLocations(src);

            // FIXME: clearly, this is not enough
            TrafficSelector selector = DefaultTrafficSelector.builder()
                    .matchEthDst(dstHost.mac()).build();
            TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();

            MultiPointToSinglePointIntent intent =
                    MultiPointToSinglePointIntent.builder()
                            .appId(appId)
                            .selector(selector)
                            .treatment(treatment)
                            .filteredIngressPoints(ingressPoints)
                            .filteredEgressPoint(new FilteredConnectPoint(dstHost.location()))
                            .build();

            services.intent().submit(intent);
            if (overlay2Cache.isActive(Traffic2Overlay.OVERLAY_ID)) {
                traffic2.monitor(intent);
            }
        }
    }

    private final class ReqRelatedIntents extends RequestHandler {
        private ReqRelatedIntents() {
            super(REQ_RELATED_INTENTS);
        }

        @Override
        public void process(ObjectNode payload) {
            traffic2.monitor(Mode.RELATED_INTENTS, makeNodeSelection(payload));
        }
    }

    // Produces a list of host ids from the specified JSON array.
    private Set<HostId> getHostIds(ArrayNode ids) {
        Set<HostId> hostIds = new HashSet<>();
        for (JsonNode id : ids) {
            hostIds.add(hostId(id.asText()));
        }
        return hostIds;
    }

    private Set<FilteredConnectPoint> getHostLocations(Set<HostId> hostIds) {
        Set<FilteredConnectPoint> points = new HashSet<>();
        for (HostId hostId : hostIds) {
            points.add(new FilteredConnectPoint(getHostLocation(hostId)));
        }
        return points;
    }

    private HostLocation getHostLocation(HostId hostId) {
        return services.host().getHost(hostId).location();
    }

    private NodeSelection makeNodeSelection(ObjectNode payload) {
        return new NodeSelection(payload, services.device(), services.host(),
                                 services.link());
    }
}


