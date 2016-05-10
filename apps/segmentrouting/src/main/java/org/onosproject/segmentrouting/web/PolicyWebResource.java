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
package org.onosproject.segmentrouting.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.onosproject.rest.AbstractWebResource;
import org.onosproject.segmentrouting.Policy;
import org.onosproject.segmentrouting.SegmentRoutingService;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Query, create and remove segment routing plicies.
 */
@Path("policy")
public class PolicyWebResource extends AbstractWebResource {

    private static final PolicyCodec POLICY_CODEC = new PolicyCodec();

    /**
     * Get all segment routing policies.
     * Returns an array of segment routing policies.
     *
     * @return status of OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPolicy() {
        SegmentRoutingService srService = get(SegmentRoutingService.class);
        List<Policy> policies = srService.getPolicies();
        ObjectNode result = new ObjectMapper().createObjectNode();
        result.set("policy", new PolicyCodec().encode(policies, this));

        return ok(result.toString()).build();
    }

    /**
     * Create a new segment routing policy.
     *
     * @param input JSON stream for policy to create
     * @return status of the request - OK if the policy is created,
     * @throws IOException if JSON processing fails
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createPolicy(InputStream input) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode policyJson = (ObjectNode) mapper.readTree(input);
        SegmentRoutingService srService = get(SegmentRoutingService.class);
        Policy policyInfo = POLICY_CODEC.decode(policyJson, this);

        if (policyInfo.type() == Policy.Type.TUNNEL_FLOW) {
            srService.createPolicy(policyInfo);
            return Response.ok().build();
        } else {
            return Response.serverError().build();
        }
    }

    /**
     * Delete a segment routing policy.
     *
     * @param input JSON stream for policy to delete
     * @return 204 NO CONTENT if the policy is removed
     * @throws IOException if JSON is invalid
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public Response removePolicy(InputStream input) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode policyJson = (ObjectNode) mapper.readTree(input);
        SegmentRoutingService srService = get(SegmentRoutingService.class);
        Policy policyInfo = POLICY_CODEC.decode(policyJson, this);
        // TODO: Check the result
        srService.removePolicy(policyInfo);

        return Response.noContent().build();
    }

}
