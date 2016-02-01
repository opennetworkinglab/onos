/*
 * Copyright 2016 Open Networking Laboratory
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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Manage flow objectives.
 */
@Path("flowobjectives")
public class FlowObjectiveWebResource extends AbstractWebResource {

    public static final String DEVICE_INVALID =
            "Invalid deviceId in objective creation request";
    public static final String POLICY_INVALID = "Invalid policy";

    final FlowObjectiveService flowObjectiveService = get(FlowObjectiveService.class);
    final ObjectNode root = mapper().createObjectNode();

    /**
     * Creates and installs a new filtering objective for the specified device.
     *
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
    public Response createFilteringObjective(@PathParam("deviceId") String deviceId,
                                             InputStream stream) {
        URI location = null;
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            if (validateDeviceId(deviceId, jsonTree)) {
                DeviceId did = DeviceId.deviceId(deviceId);
                FilteringObjective filteringObjective =
                        codec(FilteringObjective.class).decode(jsonTree, this);
                flowObjectiveService.filter(did, filteringObjective);
                location = new URI(Integer.toString(filteringObjective.id()));
            }
        } catch (IOException | URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }

        return Response
                .created(location)
                .build();
    }

    /**
     * Creates and installs a new forwarding objective for the specified device.
     *
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
    public Response createForwardingObjective(@PathParam("deviceId") String deviceId,
                                              InputStream stream) {
        URI location = null;
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            if (validateDeviceId(deviceId, jsonTree)) {
                DeviceId did = DeviceId.deviceId(deviceId);
                ForwardingObjective forwardingObjective =
                        codec(ForwardingObjective.class).decode(jsonTree, this);
                flowObjectiveService.forward(did, forwardingObjective);
                location = new URI(Integer.toString(forwardingObjective.id()));
            }
        } catch (IOException | URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }

        return Response
                .created(location)
                .build();
    }

    /**
     * Creates and installs a new next objective for the specified device.
     *
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
    public Response createNextObjective(@PathParam("deviceId") String deviceId,
                                        InputStream stream) {
        URI location = null;
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            if (validateDeviceId(deviceId, jsonTree)) {
                DeviceId did = DeviceId.deviceId(deviceId);
                NextObjective nextObjective =
                        codec(NextObjective.class).decode(jsonTree, this);
                flowObjectiveService.next(did, nextObjective);
                location = new URI(Integer.toString(nextObjective.id()));
            }
        } catch (IOException | URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }

        return Response
                .created(location)
                .build();
    }

    /**
     * Returns the globally unique nextId.
     *
     * @return nextId
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
     * @onos.rsModel ObjectivePolicy
     */
    @POST
    @Path("policy")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void initPolicy(InputStream stream) {

        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode policyJson = jsonTree.get("policy");

            if (policyJson == null || policyJson.asText().isEmpty()) {
                throw new IllegalArgumentException(POLICY_INVALID);
            }

            flowObjectiveService.initPolicy(policyJson.asText());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Validate the deviceId that is contained in json string against the
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
