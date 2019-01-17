/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.packetstats.rest;
import javax.ws.rs.Path;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.onosproject.rest.AbstractWebResource;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.Counter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.metrics.MetricsService;
import java.util.Map;

/**
 * Packet Stats REST API.
 */
@Path("")
public class PacketStatsWebResource extends AbstractWebResource {
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String METRIC_NAME = null;
    MetricFilter filter = METRIC_NAME != null ? (name, metric) -> name.equals(METRIC_NAME) : MetricFilter.ALL;

    @GET
    @Path("counters")
    @Produces(MediaType.APPLICATION_JSON)
    public Response packetStatsCounters() {
        ObjectNode node = getPacketStatsCountersJson();
        return Response.status(200).entity(node).build();
    }

    private ObjectNode getPacketStatsCountersJson() {
        MetricsService service = get(MetricsService.class);
        ObjectNode node = mapper.createObjectNode();
        ObjectNode pktCounterNode = mapper.createObjectNode();
        Map<String, Counter> counters = service.getCounters(filter);

        Counter arpCounter = counters.get("packetStatisticsComponent.arpFeature.arpPC");
        Counter lldpCounter = counters.get("packetStatisticsComponent.lldpFeature.lldpPC");
        Counter nsCounter = counters.get("packetStatisticsComponent.nbrSolicitFeature.nbrSolicitPC");
        Counter naCounter = counters.get("packetStatisticsComponent.nbrAdvertFeature.nbrAdvertPC");

        pktCounterNode.put("arpCounter", arpCounter.getCount());
        pktCounterNode.put("lldpCounter", lldpCounter.getCount());
        pktCounterNode.put("nsCounter", nsCounter.getCount());
        pktCounterNode.put("naCounter", naCounter.getCount());

        node.put("packet_stats_counters", pktCounterNode);
        return node;

    }
}
