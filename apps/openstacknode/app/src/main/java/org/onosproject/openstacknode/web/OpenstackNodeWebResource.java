/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.openstacknode.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Sets;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeAdminService;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.onosproject.openstacknode.impl.DefaultOpenstackNode;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
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

@Path("configure")
public class OpenstackNodeWebResource extends AbstractWebResource {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MESSAGE_NODE = "Received node %s request";
    private static final String NODES = "nodes";

    private final OpenstackNodeAdminService osNodeAdminService =
            DefaultServiceDirectory.getService(OpenstackNodeAdminService.class);
    private final OpenstackNodeService osNodeService =
            DefaultServiceDirectory.getService(OpenstackNodeService.class);

    @Context
    private UriInfo uriInfo;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createNodes(InputStream input) {
        log.trace(String.format(MESSAGE_NODE, "CREATE"));

        readNodeConfiguration(input).forEach(osNode -> {
            OpenstackNode existing = osNodeService.node(osNode.hostname());
            if (existing == null) {
                osNodeAdminService.createNode(osNode);
            }
        });

        UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                .path(NODES)
                .path("NODE_ID");

        return created(locationBuilder.build()).build();
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateNodes(InputStream input) {
        log.trace(String.format(MESSAGE_NODE, "UPDATE"));

        Set<OpenstackNode> nodes = readNodeConfiguration(input);
        for (OpenstackNode osNode: nodes) {
            OpenstackNode existing = osNodeService.node(osNode.hostname());
            if (existing == null) {
                log.warn("There is no node configuration to update : {}", osNode.hostname());
                return Response.notModified().build();
            } else if (!existing.equals(osNode)) {
                osNodeAdminService.updateNode(osNode);
            }
        }

        return Response.ok().build();
    }

    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteNodes(InputStream input) {
        log.trace(String.format(MESSAGE_NODE, "DELETE"));

        Set<OpenstackNode> nodes = readNodeConfiguration(input);
        for (OpenstackNode osNode: nodes) {
            OpenstackNode existing = osNodeService.node(osNode.hostname());
            if (existing == null) {
                log.warn("There is no node configuration to delete : {}", osNode.hostname());
                return Response.notModified().build();
            } else {
                osNodeAdminService.removeNode(osNode.hostname());
            }
        }

        return Response.ok().build();
    }

    private Set<OpenstackNode> readNodeConfiguration(InputStream input) {
        Set<OpenstackNode> nodeSet = Sets.newHashSet();
        try {
             JsonNode jsonTree = mapper().enable(INDENT_OUTPUT).readTree(input);
             ArrayNode nodes = (ArrayNode) jsonTree.path("nodes");
             nodes.forEach(node -> {
                 try {
                     String hostname = node.get("hostname").asText();
                     String type = node.get("type").asText();
                     String mIp = node.get("managementIp").asText();
                     String dIp = node.get("dataIp").asText();
                     String iBridge = node.get("integrationBridge").asText();
                     String rBridge = null;
                     if (node.get("routerBridge") != null) {
                         rBridge = node.get("routerBridge").asText();
                     }
                     DefaultOpenstackNode.Builder nodeBuilder = DefaultOpenstackNode.builder()
                             .hostname(hostname)
                             .type(OpenstackNode.NodeType.valueOf(type))
                             .managementIp(IpAddress.valueOf(mIp))
                             .dataIp(IpAddress.valueOf(dIp))
                             .intgBridge(DeviceId.deviceId(iBridge))
                             .state(NodeState.INIT);
                     if (rBridge != null) {
                         nodeBuilder.routerBridge(DeviceId.deviceId(rBridge));
                     }
                     log.trace("node is {}", nodeBuilder.build().toString());
                     nodeSet.add(nodeBuilder.build());
                 } catch (Exception e) {
                     log.error(e.toString());
                     throw  new IllegalArgumentException();
                 }
             });
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        return nodeSet;
    }
}
