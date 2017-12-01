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
 */

package org.onosproject.imr.rest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.tuple.Pair;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.imr.IntentMonitorAndRerouteService;
import org.onosproject.imr.data.Route;
import org.onosproject.imr.data.RoutingConfigurations;
import org.onosproject.net.ElementId;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.intent.Key;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Intent Monitor and Reroute REST API.
 */
@Path("imr")
public class ImrWebResource extends AbstractWebResource {

    public static final String ROOT_FIELD_STATISTICS_NAME = "statistics";
    public static final String ROOT_FIELD_MONITORED_INTENTS = "response";
    private final Logger log = LoggerFactory.getLogger(getClass());

    private IntentMonitorAndRerouteService imrService;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Get the statistics of the monitored intents.
     * Shows for each intent all the flow entries.
     *
     * @onos.rsModel intentStatsGet
     * @return 200 OK
     */
    @GET
    @Path("intentStats")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIntentsStats() {
        ObjectNode root = mapper.createObjectNode();
        imrService = get(IntentMonitorAndRerouteService.class);
        root.putArray(ROOT_FIELD_STATISTICS_NAME).addAll(
                getJsonNodesIntentStats(imrService.getStats()));
        return ok(root).build();
    }

    /**
     * Get the statistics of the monitored intent of a specific application
     * Shows for each intent all the flow entries.
     *
     * @onos.rsModel intentStatsGet
     * @param id Application ID
     * @param name Application Name
     * @return 200 OK
     */
    @GET
    @Path("intentStats/{id}/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIntentsStats(@PathParam("id") short id, @PathParam("name") String name) {
        ObjectNode root = mapper.createObjectNode();
        imrService = get(IntentMonitorAndRerouteService.class);
        ApplicationId appId = new DefaultApplicationId(id, name);
        root.putArray(ROOT_FIELD_STATISTICS_NAME).addAll(
                getJsonNodesIntentStats(imrService.getStats(appId)));
        return ok(root).build();
    }

    /**
     * Get the statistics of a specific monitored intent.
     * Shows all the flow entries of the specific intent
     *
     * @onos.rsModel intentStatsGet
     * @param id Application ID
     * @param name Application Name
     * @param intentK Intent Key
     * @return 200 OK
     */
    @GET
    @Path("intentStats/{id}/{name}/{intentKey}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIntentsStats(@PathParam("id") short id,
                                    @PathParam("name") String name,
                                    @PathParam("intentKey") String intentK) {
        ObjectNode root = mapper.createObjectNode();
        imrService = get(IntentMonitorAndRerouteService.class);
        ApplicationId appId = new DefaultApplicationId(id, name);
        Key intentKey = Key.of(intentK, appId);
        root.putArray("statistics").addAll(
                getJsonNodesIntentStats(imrService.getStats(appId, intentKey)));
        return ok(root).build();
    }

    /**
     * Build the Json Nodes from the intent stats retrieved from {@link IntentMonitorAndRerouteService}.
     *
     * @param mapKeyToStats Intent statistics
     * @return {@link ArrayNode} built from the statistics
     */
    private ArrayNode getJsonNodesIntentStats(Map<ApplicationId, Map<Key, List<FlowEntry>>> mapKeyToStats) {
        final ArrayNode rootArrayNode = mapper.createArrayNode();
        mapKeyToStats.forEach((appId, mapIntentKeyStats) -> {
            ObjectNode appObjNode = codec(ApplicationId.class).encode(appId, this);
            ArrayNode intentArrayNode = appObjNode.putArray("intents");
            mapIntentKeyStats.forEach((intentKey, lstStats) -> {
                ObjectNode intentKeyObjNode = mapper.createObjectNode();
                ArrayNode statsArrayNode = intentKeyObjNode.putArray(intentKey.toString());
                lstStats.forEach(stat -> {
                    statsArrayNode.add(codec(FlowEntry.class).encode(stat, this));
                });

                intentArrayNode.add(intentKeyObjNode);
            });

            rootArrayNode.add(appObjNode);
        });
        return rootArrayNode;
    }

    /**
     * Get the list of monitored intents.
     * Shows for each intent key the related end points (as inElements and OutElements).
     *
     * @onos.rsModel monitoredIntentsGet
     * @return 200 OK
     */
    @GET
    @Path("monitoredIntents")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMonitoredIntents() {
        imrService = get(IntentMonitorAndRerouteService.class);
        ArrayNode jsonMonitoredIntents = getJsonMonitoredIntents(imrService.getMonitoredIntents());
        ObjectNode result = mapper.createObjectNode();
        result.putArray(ROOT_FIELD_MONITORED_INTENTS).addAll(jsonMonitoredIntents);
        return ok(result).build();
    }

    /**
     * Get the list of monitored intents of a specific application.
     * Shows for each intent key the related end points (as inElements and OutElements).
     *
     * @onos.rsModel monitoredIntentsGet
     * @param id Application ID
     * @param name Application Name
     * @return 200 OK
     */
    @GET
    @Path("monitoredIntents/{id}/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMonitoredIntents(@PathParam("id") short id, @PathParam("name") String name) {
        imrService = get(IntentMonitorAndRerouteService.class);
        ApplicationId appId = new DefaultApplicationId(id, name);
        ArrayNode jsonMonitoredIntents = getJsonMonitoredIntents(imrService.getMonitoredIntents(appId));
        ObjectNode result = mapper.createObjectNode();
        result.putArray(ROOT_FIELD_MONITORED_INTENTS).addAll(jsonMonitoredIntents);
        return ok(result).build();
    }

    /**
     * Build the JSON Node from the monitored intents retrieved from {@link IntentMonitorAndRerouteService}.
     *
     * @param monIntents Monitored intents structure.
     * @return {@link ArrayNode} built from the monitored intents.
     */
    private ArrayNode getJsonMonitoredIntents(
            Map<ApplicationId, Map<Key, Pair<Set<ElementId>, Set<ElementId>>>> monIntents) {
        final ArrayNode rootArrayNode = mapper.createArrayNode();
        monIntents.forEach((appId, mapIntentKeyEndElements) -> {
            ObjectNode appObjNode = codec(ApplicationId.class).encode(appId, this);
            ArrayNode intentArrayNode = appObjNode.putArray("intents");
            mapIntentKeyEndElements.forEach((intentKey, inOutElem) -> {
                ObjectNode intentKeyObjNode = mapper.createObjectNode()
                        .put("key", intentKey.toString());
                ArrayNode inElements = intentKeyObjNode.putArray("inElements");
                inOutElem.getLeft().forEach(elementId -> inElements.add(elementId.toString()));
                ArrayNode outElements = intentKeyObjNode.putArray("outElements");
                inOutElem.getRight().forEach(elementId -> outElements.add(elementId.toString()));
                intentArrayNode.add(intentKeyObjNode);
            });

            rootArrayNode.add(appObjNode);
        });
        return rootArrayNode;
    }


    /**
     * POST a list of routing configurations. For each intents a list of paths
     * with relative weights is specified. The body of HTTP POST is a JSON
     *
     * @onos.rsModel reRouteIntentsPost
     * @param stream JSON stream
     * @return 200 OK if the routing configurations are applied correctly,
     * otherwise 500 Internal Server Error
     */
    @POST
    @Path("reRouteIntents")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response reRouteIntents(InputStream stream) {
        imrService = get(IntentMonitorAndRerouteService.class);
        ObjectNode result = mapper().createObjectNode();
        StringBuilder resultString = new StringBuilder();

        mapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            RoutingConfigurations msg = mapper().readValue(stream, RoutingConfigurations.class);
            for (Route routingConfiguration: msg.routingList()) {
                String outcome;
                try {
                     if (!imrService.applyPath(routingConfiguration)) {
                         outcome = "Application ID:" + routingConfiguration.appId()
                                 + " or Intent Key:" + routingConfiguration.key()
                                 + " not monitored!";
                     } else {
                         outcome = "OK";
                     }
                } catch (IllegalArgumentException | NullPointerException ex) {
                    outcome = ex.getMessage();
                }
                if (!outcome.equals("OK")) {
                    if (resultString.length() > 0) {
                        resultString.append(" ");
                    }
                    resultString.append(outcome);
                }
            }
            if (resultString.length() > 0) {
                result.put("response", "addRouting() failed: ".concat(resultString.toString()));
            } else {
                result.put("response", "OK");
            }
            return ok(result).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).
                    entity(e.toString())
                    .build();
        }
    }
}
