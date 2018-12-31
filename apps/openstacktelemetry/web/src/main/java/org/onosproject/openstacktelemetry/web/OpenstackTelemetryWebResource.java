/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacktelemetry.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.onosproject.codec.JsonCodec;
import org.onosproject.openstacktelemetry.api.FlowInfo;
import org.onosproject.openstacktelemetry.api.StatsFlowRule;
import org.onosproject.openstacktelemetry.api.StatsFlowRuleAdminService;
import org.onosproject.openstacktelemetry.codec.rest.FlowInfoJsonCodec;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.util.Set;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static javax.ws.rs.core.Response.created;
import static org.onlab.util.Tools.readTreeFromStream;

/**
 * Handles REST API call of openstack telemetry.
 */
@Path("telemetry")
public class OpenstackTelemetryWebResource extends AbstractWebResource {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ObjectNode root = mapper().createObjectNode();

    private static final String JSON_NODE_FLOW_RULE = "rules";
    private static final String FLOW_RULE_ID = "STATS_FLOW_RULE_ID";

    private final StatsFlowRuleAdminService
                    statsFlowRuleService = get(StatsFlowRuleAdminService.class);

    @Context
    private UriInfo uriInfo;

    /**
     * Creates a flow rule for metric.
     *
     * @param input openstack flow rule JSON input stream
     * @return 201 CREATED if the JSON is correct,
     *         400 BAD_REQUEST if the JSON is malformed.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createBulkFlowRule(InputStream input) {
        log.info("CREATE BULK FLOW RULE: {}", input.toString());

        readNodeConfiguration(input).forEach(flowRule -> {
                log.debug("FlowRule: {}", flowRule.toString());
                statsFlowRuleService.createStatFlowRule(flowRule);
            });

        UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                                            .path(JSON_NODE_FLOW_RULE)
                                            .path(FLOW_RULE_ID);

        return created(locationBuilder.build()).build();
    }

    /**
     * Delete flow rules.
     *
     * @param input openstack flow rule JSON input stream
     * @return 200 OK if processing is correct.
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteBulkFlowRule(InputStream input) {
        log.info("DELETE BULK FLOW RULE: {}", input.toString());

        readNodeConfiguration(input).forEach(flowRule -> {
            log.debug("FlowRule: {}", flowRule.toString());
            statsFlowRuleService.deleteStatFlowRule(flowRule);
        });

        return ok(root).build();
    }

    /**
     * Get flow rules which is installed on ONOS.
     *
     * @return 200 OK
     */
    public Response readBulkFlowRule() {
        log.info("READ BULK FLOW RULE");

        return ok(root).build();
    }

    /**
     * Get flow information list.
     *
     * @return Flow information list
     */
    @GET
    @Path("list")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFlowInfoBulk() {
        log.info("GET BULK FLOW RULE");

        Set<FlowInfo> flowInfoSet;
        flowInfoSet = statsFlowRuleService.getOverlayFlowInfos();

        JsonCodec<FlowInfo> flowInfoCodec = new FlowInfoJsonCodec();

        ObjectNode nodeJson;
        int idx = 0;
        for (FlowInfo flowInfo: flowInfoSet) {
            nodeJson = flowInfoCodec.encode(flowInfo, this);
            root.put("FlowInfo" + idx++, nodeJson.toString());
        }
        return ok(root).build();
    }

    @GET
    @Path("list/{srcIpPrefix}/{dstIpPrefix}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFlowRule(@PathParam("srcIpPrefix") String srcIpPrefix,
                                @PathParam("dstIpPrefix") String dstIpPrefix) {
        return ok(root).build();
    }

    private Set<StatsFlowRule> readNodeConfiguration(InputStream input) {
        log.info("Input JSON Data: \n\t\t{}", input.toString());
        Set<StatsFlowRule> flowRuleSet = Sets.newHashSet();
        try {
            JsonNode jsonTree = readTreeFromStream(mapper().enable(INDENT_OUTPUT), input);
            ArrayNode nodes = (ArrayNode) jsonTree.path(JSON_NODE_FLOW_RULE);
            nodes.forEach(node -> {
                try {
                    ObjectNode objectNode = node.deepCopy();
                    log.debug("ObjectNode: {}", objectNode.toString());
                    StatsFlowRule statsFlowRule = codec(StatsFlowRule.class)
                                                        .decode(objectNode, this);
                    log.debug("StatsFlowRule: {}", statsFlowRule.toString());
                    flowRuleSet.add(statsFlowRule);
                } catch (Exception ex) {
                    log.error("Exception Stack:\n{}", ExceptionUtils.getStackTrace(ex));
                    throw new IllegalArgumentException();
                }
            });
        } catch (Exception ex) {
            log.error("Exception Stack:\n{}", ExceptionUtils.getStackTrace(ex));
            throw new IllegalArgumentException(ex);
        }

        return flowRuleSet;
    }
}
