/*
 * Copyright 2015-present Open Networking Foundation
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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.rest.AbstractWebResource;

import static org.onlab.util.Tools.nullIsNotFound;

/**
 * Manage cluster of ONOS instances.
 */
@Path("cluster")
public class ClusterWebResource extends AbstractWebResource {

    private static final String NODE_NOT_FOUND = "Node is not found";

    /**
     * Get all cluster nodes.
     * Returns array of all cluster nodes.
     *
     * @return 200 OK with a collection of cluster nodes
     * @onos.rsModel Cluster
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClusterNodes() {
        Iterable<ControllerNode> nodes = get(ClusterService.class).getNodes();
        return ok(encodeArray(ControllerNode.class, "nodes", nodes)).build();
    }

    /**
     * Get cluster node details.
     * Returns details of the specified cluster node.
     *
     * @param id cluster node identifier
     * @return 200 OK with a cluster node
     * @onos.rsModel ClusterNode
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getClusterNode(@PathParam("id") String id) {
        ControllerNode node = nullIsNotFound(get(ClusterService.class).getNode(new NodeId(id)),
                                             NODE_NOT_FOUND);
        return ok(codec(ControllerNode.class).encode(node, this)).build();
    }
}
