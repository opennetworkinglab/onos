/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.StreamSupport;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onlab.util.ItemNotFoundException;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.rest.AbstractWebResource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * REST resource for interacting with the inventory of flows.
 */

@Path("flows")
public class FlowsWebResource extends AbstractWebResource {
    public static final String DEVICE_NOT_FOUND = "Device is not found";

    final FlowRuleService service = get(FlowRuleService.class);
    final ObjectNode root = mapper().createObjectNode();
    final ArrayNode flowsNode = root.putArray("flows");

    /**
     * Gets an array containing all the intents in the system.
     *
     * @return array of all the intents in the system
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFlows() {

        final Iterable<Device> devices = get(DeviceService.class).getDevices();
        for (final Device device : devices) {
            final Iterable<FlowEntry> deviceEntries = service.getFlowEntries(device.id());
            if (deviceEntries != null) {
                for (final FlowEntry entry : deviceEntries) {
                    flowsNode.add(codec(FlowEntry.class).encode(entry, this));
                }
            }
        }

        return ok(root).build();
    }

    /**
     * Gets the flows for a device, where the device is specified by Id.
     *
     * @param deviceId Id of device to look up
     * @return flow data as an array
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{deviceId}")
    public Response getFlowByDeviceId(@PathParam("deviceId") String deviceId) {
        final Iterable<FlowEntry> deviceEntries =
                service.getFlowEntries(DeviceId.deviceId(deviceId));

        if (!deviceEntries.iterator().hasNext()) {
            throw new ItemNotFoundException(DEVICE_NOT_FOUND);
        }
        for (final FlowEntry entry : deviceEntries) {
            flowsNode.add(codec(FlowEntry.class).encode(entry, this));
        }
        return ok(root).build();
    }

    /**
     * Gets the flows for a device, where the device is specified by Id.
     *
     * @param deviceId Id of device to look up
     * @param flowId   Id of flow to look up
     * @return flow data as an array
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{deviceId}/{flowId}")
    public Response getFlowByDeviceIdAndFlowId(@PathParam("deviceId") String deviceId,
                                               @PathParam("flowId") long flowId) {
        final Iterable<FlowEntry> deviceEntries =
                service.getFlowEntries(DeviceId.deviceId(deviceId));

        if (!deviceEntries.iterator().hasNext()) {
            throw new ItemNotFoundException(DEVICE_NOT_FOUND);
        }
        for (final FlowEntry entry : deviceEntries) {
            if (entry.id().value() == flowId) {
                flowsNode.add(codec(FlowEntry.class).encode(entry, this));
            }
        }
        return ok(root).build();
    }

    /**
     * Creates a flow rule from a POST of a JSON string and attempts to apply it.
     *
     * @param deviceId device identifier
     * @param stream input JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     */
    @POST
    @Path("{deviceId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createFlow(@PathParam("deviceId") String deviceId,
                               InputStream stream) {
        URI location;
        try {
            FlowRuleService service = get(FlowRuleService.class);
            ObjectNode root = (ObjectNode) mapper().readTree(stream);
            JsonNode specifiedDeviceId = root.get("deviceId");
            if (specifiedDeviceId != null &&
                    !specifiedDeviceId.asText().equals(deviceId)) {
                throw new IllegalArgumentException(
                        "Invalid deviceId in flow creation request");
            }
            root.put("deviceId", deviceId);
            FlowRule rule = codec(FlowRule.class).decode(root, this);
            service.applyFlowRules(rule);
            location = new URI(Long.toString(rule.id().value()));
        } catch (IOException | URISyntaxException ex) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response
                .created(location)
                .build();
    }

    /**
     * Removes the flows for a given device with the given flow id.
     *
     * @param deviceId Id of device to look up
     * @param flowId   Id of flow to look up
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{deviceId}/{flowId}")
    public void deleteFlowByDeviceIdAndFlowId(@PathParam("deviceId") String deviceId,
                                                  @PathParam("flowId") long flowId) {
        final Iterable<FlowEntry> deviceEntries =
                service.getFlowEntries(DeviceId.deviceId(deviceId));

        if (!deviceEntries.iterator().hasNext()) {
            throw new ItemNotFoundException(DEVICE_NOT_FOUND);
        }

        StreamSupport.stream(deviceEntries.spliterator(), false)
                .filter(entry -> entry.id().value() == flowId)
                .forEach(service::removeFlowRules);
    }

}
