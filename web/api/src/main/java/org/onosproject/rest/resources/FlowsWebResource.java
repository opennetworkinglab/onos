/*
 * Copyright 2015-present Open Networking Laboratory
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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.app.ApplicationService;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.onlab.util.Tools.nullIsNotFound;

/**
 * Query and program flow rules.
 */

@Path("flows")
public class FlowsWebResource extends AbstractWebResource {

    @Context
    private UriInfo uriInfo;

    private static final String DEVICE_NOT_FOUND = "Device is not found";
    private static final String FLOW_NOT_FOUND = "Flow is not found";
    private static final String APP_ID_NOT_FOUND = "Application Id is not found";
    private static final String FLOWS = "flows";
    private static final String DEVICE_ID = "deviceId";
    private static final String FLOW_ID = "flowId";

    private final FlowRuleService service = get(FlowRuleService.class);
    private final ObjectNode root = mapper().createObjectNode();
    private final ArrayNode flowsNode = root.putArray(FLOWS);

    /**
     * Gets all flow entries. Returns array of all flow rules in the system.
     *
     * @return 200 OK with a collection of flows
     * @onos.rsModel FlowEntries
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
     * Creates new flow rules. Creates and installs a new flow rules.<br>
     * Instructions description:
     * https://wiki.onosproject.org/display/ONOS/Flow+Rule+Instructions
     * <br>
     * Criteria description:
     * https://wiki.onosproject.org/display/ONOS/Flow+Rule+Criteria
     *
     * @param appId application id
     * @param stream flow rules JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel FlowsBatchPost
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createFlows(@QueryParam("appId") String appId, InputStream stream) {
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            ArrayNode flowsArray = (ArrayNode) jsonTree.get(FLOWS);

            if (appId != null) {
                flowsArray.forEach(flowJson -> ((ObjectNode) flowJson).put("appId", appId));
            }

            List<FlowRule> rules = codec(FlowRule.class).decode(flowsArray, this);
            service.applyFlowRules(rules.toArray(new FlowRule[rules.size()]));
            rules.forEach(flowRule -> {
                ObjectNode flowNode = mapper().createObjectNode();
                flowNode.put(DEVICE_ID, flowRule.deviceId().toString())
                        .put(FLOW_ID, flowRule.id().value());
                flowsNode.add(flowNode);
            });
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
        return Response.ok(root).build();
    }

    /**
     * Gets flow entries of a device. Returns array of all flow rules for the
     * specified device.
     *
     * @param deviceId device identifier
     * @return 200 OK with a collection of flows of given device
     * @onos.rsModel FlowEntries
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    // TODO: we need to add "/device" suffix to the path to differentiate with appId
    @Path("{deviceId}")
    public Response getFlowByDeviceId(@PathParam("deviceId") String deviceId) {
        final Iterable<FlowEntry> flowEntries =
                service.getFlowEntries(DeviceId.deviceId(deviceId));

        if (flowEntries == null || !flowEntries.iterator().hasNext()) {
            throw new ItemNotFoundException(DEVICE_NOT_FOUND);
        }
        for (final FlowEntry entry : flowEntries) {
            flowsNode.add(codec(FlowEntry.class).encode(entry, this));
        }
        return ok(root).build();
    }

    /**
     * Gets flow rules. Returns the flow entry specified by the device id and
     * flow rule id.
     *
     * @param deviceId device identifier
     * @param flowId   flow rule identifier
     * @return 200 OK with a collection of flows of given device and flow
     * @onos.rsModel FlowEntries
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{deviceId}/{flowId}")
    public Response getFlowByDeviceIdAndFlowId(@PathParam("deviceId") String deviceId,
                                               @PathParam("flowId") long flowId) {
        final Iterable<FlowEntry> flowEntries =
                service.getFlowEntries(DeviceId.deviceId(deviceId));

        if (flowEntries == null || !flowEntries.iterator().hasNext()) {
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
     * Gets flow rules generated by an application.
     * Returns the flow rule specified by the application id.
     *
     * @param appId application identifier
     * @return 200 OK with a collection of flows of given application id
     * @onos.rsModel FlowRules
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("application/{appId}")
    public Response getFlowByAppId(@PathParam("appId") String appId) {
        final ApplicationService appService = get(ApplicationService.class);
        final ApplicationId idInstant = nullIsNotFound(appService.getId(appId), APP_ID_NOT_FOUND);
        final Iterable<FlowRule> flowRules = service.getFlowRulesById(idInstant);

        flowRules.forEach(flow -> flowsNode.add(codec(FlowRule.class).encode(flow, this)));
        return ok(root).build();
    }

    /**
     * Removes flow rules by application ID.
     * Removes a collection of flow rules generated by the given application.
     *
     * @param appId application identifier
     * @return 204 NO CONTENT
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("application/{appId}")
    public Response removeFlowByAppId(@PathParam("appId") String appId) {
        final ApplicationService appService = get(ApplicationService.class);
        final ApplicationId idInstant = nullIsNotFound(appService.getId(appId), APP_ID_NOT_FOUND);
        service.removeFlowRulesById(idInstant);
        return Response.noContent().build();
    }

    /**
     * Creates new flow rule. Creates and installs a new flow rule for the
     * specified device. <br>
     * Instructions description:
     * https://wiki.onosproject.org/display/ONOS/Flow+Rule+Instructions
     * <br>
     * Criteria description:
     * https://wiki.onosproject.org/display/ONOS/Flow+Rule+Criteria
     *
     * @param deviceId device identifier
     * @param appId    application identifier
     * @param stream   flow rule JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel FlowsPost
     */
    @POST
    @Path("{deviceId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createFlow(@PathParam("deviceId") String deviceId,
                               @QueryParam("appId") String appId,
                               InputStream stream) {
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode specifiedDeviceId = jsonTree.get("deviceId");
            if (specifiedDeviceId != null &&
                    !specifiedDeviceId.asText().equals(deviceId)) {
                throw new IllegalArgumentException(
                        "Invalid deviceId in flow creation request");
            }
            jsonTree.put("deviceId", deviceId);

            if (appId != null) {
                jsonTree.put("appId", appId);
            }

            FlowRule rule = codec(FlowRule.class).decode(jsonTree, this);
            service.applyFlowRules(rule);
            UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                    .path("flows")
                    .path(deviceId)
                    .path(Long.toString(rule.id().value()));

            return Response
                    .created(locationBuilder.build())
                    .build();
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Removes flow rule. Removes the specified flow rule.
     *
     * @param deviceId device identifier
     * @param flowId   flow rule identifier
     * @return 204 NO CONTENT
     */
    @DELETE
    @Path("{deviceId}/{flowId}")
    public Response deleteFlowByDeviceIdAndFlowId(@PathParam("deviceId") String deviceId,
                                                  @PathParam("flowId") long flowId) {
        final Iterable<FlowEntry> flowEntries =
                service.getFlowEntries(DeviceId.deviceId(deviceId));

        if (!flowEntries.iterator().hasNext()) {
            throw new ItemNotFoundException(DEVICE_NOT_FOUND);
        }

        StreamSupport.stream(flowEntries.spliterator(), false)
                .filter(entry -> entry.id().value() == flowId)
                .forEach(service::removeFlowRules);
        return Response.noContent().build();
    }

    /**
     * Removes a batch of flow rules.
     *
     * @param stream stream for posted JSON
     * @return 204 NO CONTENT
     */
    @DELETE
    public Response deleteFlows(InputStream stream) {
        ListMultimap<DeviceId, Long> deviceMap = ArrayListMultimap.create();
        List<FlowEntry> rulesToRemove = new ArrayList<>();

        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);

            JsonNode jsonFlows = jsonTree.get("flows");

            jsonFlows.forEach(node -> {
                DeviceId deviceId =
                        DeviceId.deviceId(
                                nullIsNotFound(node.get(DEVICE_ID),
                                        DEVICE_NOT_FOUND).asText());
                long flowId = nullIsNotFound(node.get(FLOW_ID),
                        FLOW_NOT_FOUND).asLong();
                deviceMap.put(deviceId, flowId);

            });
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }

        deviceMap.keySet().forEach(deviceId -> {
            List<Long> flowIds = deviceMap.get(deviceId);
            Iterable<FlowEntry> entries = service.getFlowEntries(deviceId);
            flowIds.forEach(flowId -> {
                StreamSupport.stream(entries.spliterator(), false)
                        .filter(entry -> flowId == entry.id().value())
                        .forEach(rulesToRemove::add);
            });
        });

        service.removeFlowRules(rulesToRemove.toArray(new FlowEntry[0]));
        return Response.noContent().build();
    }
}
