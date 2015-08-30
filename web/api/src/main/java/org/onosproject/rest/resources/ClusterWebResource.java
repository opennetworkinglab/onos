/*
 * Copyright 2015 Open Networking Laboratory
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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.cluster.ClusterAdminService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.codec.JsonCodec;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;

import static org.onlab.util.Tools.nullIsNotFound;

/**
 * Manage cluster of ONOS instances.
 */
@Path("cluster")
public class ClusterWebResource extends AbstractWebResource {

    public static final String NODE_NOT_FOUND = "Node is not found";

    /**
     * Get all cluster nodes.
     * Returns array of all cluster nodes.
     *
     * @return 200 OK
     */
    @GET
    public Response getClusterNodes() {
        Iterable<ControllerNode> nodes = get(ClusterService.class).getNodes();
        return ok(encodeArray(ControllerNode.class, "nodes", nodes)).build();
    }

    /**
     * Get cluster node details.
     * Returns details of the specified cluster node.
     *
     * @param id cluster node identifier
     * @return 200 OK
     */
    @GET
    @Path("{id}")
    public Response getClusterNode(@PathParam("id") String id) {
        ControllerNode node = nullIsNotFound(get(ClusterService.class).getNode(new NodeId(id)),
                                             NODE_NOT_FOUND);
        return ok(codec(ControllerNode.class).encode(node, this)).build();
    }

    /**
     * Forms cluster of ONOS instances.
     * Forms ONOS cluster using the uploaded JSON definition.
     *
     * @param config cluster definition
     * @return 200 OK
     * @throws IOException to signify bad request
     */
    @POST
    @Path("configuration")
    public Response formCluster(InputStream config) throws IOException {
        JsonCodec<ControllerNode> codec = codec(ControllerNode.class);
        ObjectNode root = (ObjectNode) mapper().readTree(config);
        String ipPrefix = root.path("ipPrefix").asText();

        List<ControllerNode> nodes = codec.decode((ArrayNode) root.path("nodes"), this);
        get(ClusterAdminService.class).formCluster(new HashSet<>(nodes), ipPrefix);

        return Response.ok().build();
    }

}
