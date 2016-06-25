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
package org.onosproject.rest.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;

/**
 * Manage flow objectives.
 */
@Path("flowobjectives")
public class FlowObjectiveWebResource extends AbstractWebResource {

    @Context
    private UriInfo uriInfo;

    private static final String DEVICE_INVALID =
            "Invalid deviceId in objective creation request";
    private static final String POLICY_INVALID = "Invalid policy";

    private final FlowObjectiveService flowObjectiveService = get(FlowObjectiveService.class);
    private final ObjectNode root = mapper().createObjectNode();

    /**
     * Creates and installs a new filtering objective for the specified device.
     *
     * @param appId    application identifier
     * @param deviceId device identifier
     * @param stream   filtering objective JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel FilteringObjective
     */
    @POST
    @Path("{deviceId}/filter")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createFilteringObjective(@QueryParam("appId") String appId,
                                             @PathParam("deviceId") String deviceId,
                                             InputStream stream) {
        try {
            UriBuilder locationBuilder = null;
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            if (validateDeviceId(deviceId, jsonTree)) {

                if (appId != null) {
                    jsonTree.put("appId", appId);
                }

                DeviceId did = DeviceId.deviceId(deviceId);
                FilteringObjective filteringObjective =
                        codec(FilteringObjective.class).decode(jsonTree, this);
                flowObjectiveService.filter(did, filteringObjective);
                locationBuilder = uriInfo.getBaseUriBuilder()
                        .path("flowobjectives")
                        .path(did.toString())
                        .path("filter")
                        .path(Integer.toString(filteringObjective.id()));
            }
            return Response
                    .created(locationBuilder.build())
                    .build();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Creates and installs a new forwarding objective for the specified device.
     *
     * @param appId    application identifier
     * @param deviceId device identifier
     * @param stream   forwarding objective JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel ForwardingObjective
     */
    @POST
    @Path("{deviceId}/forward")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createForwardingObjective(@QueryParam("appId") String appId,
                                              @PathParam("deviceId") String deviceId,
                                              InputStream stream) {
        try {
            UriBuilder locationBuilder = null;
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            if (validateDeviceId(deviceId, jsonTree)) {

                if (appId != null) {
                    jsonTree.put("appId", appId);
                }

                DeviceId did = DeviceId.deviceId(deviceId);
                ForwardingObjective forwardingObjective =
                        codec(ForwardingObjective.class).decode(jsonTree, this);
                flowObjectiveService.forward(did, forwardingObjective);
                locationBuilder = uriInfo.getBaseUriBuilder()
                        .path("flowobjectives")
                        .path(did.toString())
                        .path("forward")
                        .path(Integer.toString(forwardingObjective.id()));
            }
            return Response
                    .created(locationBuilder.build())
                    .build();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Creates and installs a new next objective for the specified device.
     *
     * @param appId    application identifier
     * @param deviceId device identifier
     * @param stream   next objective JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel NextObjective
     */
    @POST
    @Path("{deviceId}/next")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createNextObjective(@QueryParam("appId") String appId,
                                        @PathParam("deviceId") String deviceId,
                                        InputStream stream) {
        try {
            UriBuilder locationBuilder = null;
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            if (validateDeviceId(deviceId, jsonTree)) {

                if (appId != null) {
                    jsonTree.put("appId", appId);
                }

                DeviceId did = DeviceId.deviceId(deviceId);
                NextObjective nextObjective =
                        codec(NextObjective.class).decode(jsonTree, this);
                flowObjectiveService.next(did, nextObjective);
                locationBuilder = uriInfo.getBaseUriBuilder()
                        .path("flowobjectives")
                        .path(did.toString())
                        .path("next")
                        .path(Integer.toString(nextObjective.id()));
            }
            return Response
                    .created(locationBuilder.build())
                    .build();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns the globally unique nextId.
     *
     * @return 200 OK with next identifier
     * @onos.rsModel NextId
     */
    @GET
    @Path("next")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNextId() {
        root.put("nextId", flowObjectiveService.allocateNextId());
        return ok(root).build();
    }

    /**
     * Installs the filtering rules onto the specified device.
     *
     * @param stream filtering rule JSON
     * @return 200 OK
     * @onos.rsModel ObjectivePolicy
     */
    @POST
    @Path("policy")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response initPolicy(InputStream stream) {

        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode policyJson = jsonTree.get("policy");

            if (policyJson == null || policyJson.asText().isEmpty()) {
                throw new IllegalArgumentException(POLICY_INVALID);
            }

            flowObjectiveService.initPolicy(policyJson.asText());
            return Response.ok().build();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Validates the deviceId that is contained in json string against the
     * input deviceId.
     *
     * @param deviceId device identifier
     * @param node     object node
     * @return validity
     */
    private boolean validateDeviceId(String deviceId, ObjectNode node) {
        JsonNode specifiedDeviceId = node.get("deviceId");

        if (specifiedDeviceId != null &&
                !specifiedDeviceId.asText().equals(deviceId)) {
            throw new IllegalArgumentException(DEVICE_INVALID);
        }
        return true;
    }
}
