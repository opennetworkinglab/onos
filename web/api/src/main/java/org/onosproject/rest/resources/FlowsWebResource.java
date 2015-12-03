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
 * Query and program flow rules.
 */

@Path("flows")
public class FlowsWebResource extends AbstractWebResource {
    public static final String DEVICE_NOT_FOUND = "Device is not found";

    final FlowRuleService service = get(FlowRuleService.class);
    final ObjectNode root = mapper().createObjectNode();
    final ArrayNode flowsNode = root.putArray("flows");

    /**
     * Get all flow entries. Returns array of all flow rules in the system.
     * @onos.rsModel Flows
     * @return array of all the intents in the system
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFlows() {
        final Iterable<Device> devices = get(DeviceService.class).getDevices();
        for (final Device device : devices) {
            final Iterable<FlowEntry> flowEntries = service.getFlowEntries(device.id());
            if (flowEntries != null) {
                for (final FlowEntry entry : flowEntries) {
                    flowsNode.add(codec(FlowEntry.class).encode(entry, this));
                }
            }
        }

        return ok(root).build();
    }

    /**
     * Get flow entries of a device. Returns array of all flow rules for the
     * specified device.
     * @onos.rsModel Flows
     * @param deviceId device identifier
     * @return flow data as an array
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{deviceId}")
    public Response getFlowByDeviceId(@PathParam("deviceId") String deviceId) {
        final Iterable<FlowEntry> flowEntries =
                service.getFlowEntries(DeviceId.deviceId(deviceId));

        if (!flowEntries.iterator().hasNext()) {
            throw new ItemNotFoundException(DEVICE_NOT_FOUND);
        }
        for (final FlowEntry entry : flowEntries) {
            flowsNode.add(codec(FlowEntry.class).encode(entry, this));
        }
        return ok(root).build();
    }

    /**
     * Get flow rule. Returns the flow entry specified by the device id and
     * flow rule id.
     * @onos.rsModel Flows
     * @param deviceId device identifier
     * @param flowId   flow rule identifier
     * @return flow data as an array
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{deviceId}/{flowId}")
    public Response getFlowByDeviceIdAndFlowId(@PathParam("deviceId") String deviceId,
                                               @PathParam("flowId") long flowId) {
        final Iterable<FlowEntry> flowEntries =
                service.getFlowEntries(DeviceId.deviceId(deviceId));

        if (!flowEntries.iterator().hasNext()) {
            throw new ItemNotFoundException(DEVICE_NOT_FOUND);
        }
        for (final FlowEntry entry : flowEntries) {
            if (entry.id().value() == flowId) {
                flowsNode.add(codec(FlowEntry.class).encode(entry, this));
            }
        }
        return ok(root).build();
    }

    /**
     * Create new flow rule. Creates and installs a new flow rule for the
     * specified device.
     * @onos.rsModel FlowsPost
     * @param deviceId device identifier
     * @param stream   flow rule JSON
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
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode specifiedDeviceId = jsonTree.get("deviceId");
            if (specifiedDeviceId != null &&
                    !specifiedDeviceId.asText().equals(deviceId)) {
                throw new IllegalArgumentException(
                        "Invalid deviceId in flow creation request");
            }
            jsonTree.put("deviceId", deviceId);
            FlowRule rule = codec(FlowRule.class).decode(jsonTree, this);
            service.applyFlowRules(rule);
            location = new URI(Long.toString(rule.id().value()));
        } catch (IOException | URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }

        return Response
                .created(location)
                .build();
    }

    /**
     * Remove flow rule. Removes the specified flow rule.
     *
     * @param deviceId device identifier
     * @param flowId   flow rule identifier
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{deviceId}/{flowId}")
    public void deleteFlowByDeviceIdAndFlowId(@PathParam("deviceId") String deviceId,
                                              @PathParam("flowId") long flowId) {
        final Iterable<FlowEntry> flowEntries =
                service.getFlowEntries(DeviceId.deviceId(deviceId));

        if (!flowEntries.iterator().hasNext()) {
            throw new ItemNotFoundException(DEVICE_NOT_FOUND);
        }

        StreamSupport.stream(flowEntries.spliterator(), false)
                .filter(entry -> entry.id().value() == flowId)
                .forEach(service::removeFlowRules);
    }

}
