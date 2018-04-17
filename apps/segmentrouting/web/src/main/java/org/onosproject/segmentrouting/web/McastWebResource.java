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
package org.onosproject.segmentrouting.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.onlab.packet.IpAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.segmentrouting.SegmentRoutingService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Query multicast trees.
 */
@Path("mcast")
public class McastWebResource extends AbstractWebResource {

    private ObjectNode encodeMcastTrees(String gAddr, String source) {
        SegmentRoutingService srService = get(SegmentRoutingService.class);
        Set<IpAddress> mcastGroups = ImmutableSet.copyOf(srService.getMcastLeaders(null)
                                                                 .keySet());

        if (!isNullOrEmpty(gAddr)) {
            mcastGroups = mcastGroups.stream()
                    .filter(mcastIp -> mcastIp.equals(IpAddress.valueOf(gAddr)))
                    .collect(Collectors.toSet());
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        // Print the trees for each group or build json objects
        mcastGroups.forEach(group -> {
            // We want to use source cp only for a specific group
            ConnectPoint sourcecp = null;
            if (!isNullOrEmpty(source) &&
                    !isNullOrEmpty(gAddr)) {
                sourcecp = ConnectPoint.deviceConnectPoint(source);
            }
            Multimap<ConnectPoint, List<ConnectPoint>> mcastTree = srService.getMcastTrees(group, sourcecp);
            // Build a json object for each group
            root.putPOJO(group.toString(), json(mcastTree));

        });
        return root;
    }

    private ObjectNode json(Multimap<ConnectPoint, List<ConnectPoint>> mcastTree) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonSinks = mapper.createObjectNode();
        mcastTree.asMap().forEach((sink, paths) -> {
            ArrayNode jsonPaths = mapper.createArrayNode();
            paths.forEach(path -> {
                ArrayNode jsonPath = mapper.createArrayNode();
                path.forEach(connectPoint -> jsonPath.add(connectPoint.toString()));
                jsonPaths.addPOJO(jsonPath);
            });
            jsonSinks.putPOJO(sink.toString(), jsonPaths);
        });
        return jsonSinks;
    }

    /**
     * Get all multicast trees.
     * Returns an object of the multicast trees.
     *
     * @return status of OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMcastTrees() {
        ObjectNode root = encodeMcastTrees(null, null);
        return ok(root).build();
    }

    /**
     * Get the multicast trees of a group.
     *
     * @param group group IP address
     * @return 200 OK with a multicast routes
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{group}")
    public Response getRoute(@PathParam("group") String group) {
        ObjectNode root = encodeMcastTrees(group, null);
        return ok(root).build();
    }

    /**
     * Get the multicast tree of a group.
     *
     * @param group group IP address
     * @param sourcecp source connect point
     * @return 200 OK with a multicast routes
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{group}/{sourcecp}")
    public Response getRoute(@PathParam("group") String group,
                             @PathParam("sourcecp") String sourcecp) {
        ObjectNode root = encodeMcastTrees(group, sourcecp);
        return ok(root).build();
    }

}
