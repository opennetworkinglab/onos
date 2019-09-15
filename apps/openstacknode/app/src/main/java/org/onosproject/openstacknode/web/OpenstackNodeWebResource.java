/*
 * Copyright 2017-present Open Networking Foundation
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.onosproject.openstacknode.api.NodeState;
import org.onosproject.openstacknode.api.OpenstackNode;
import org.onosproject.openstacknode.api.OpenstackNodeAdminService;
import org.onosproject.openstacknode.api.OpenstackNodeService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
import static org.onlab.util.Tools.nullIsIllegal;
import static org.onlab.util.Tools.readTreeFromStream;
import static org.onosproject.openstacknode.api.NodeState.COMPLETE;

/**
 * Handles REST API call of openstack node config.
 */

@Path("configure")
public class OpenstackNodeWebResource extends AbstractWebResource {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MESSAGE_NODE = "Received node %s request";
    private static final String NODES = "nodes";
    private static final String CREATE = "CREATE";
    private static final String UPDATE = "UPDATE";
    private static final String NODE_ID = "NODE_ID";
    private static final String DELETE = "DELETE";
    private static final String QUERY = "QUERY";
    private static final String INIT = "INIT";
    private static final String NOT_EXIST = "Not exist";
    private static final String STATE = "State";

    private static final String HOST_NAME = "hostname";
    private static final String ERROR_MESSAGE = " cannot be null";

    private final ObjectNode  root = mapper().createObjectNode();
    private final ArrayNode osJsonNodes = root.putArray("nodes");

    private final OpenstackNodeAdminService osNodeAdminService =
                                            get(OpenstackNodeAdminService.class);
    private final OpenstackNodeService osNodeService =
                                            get(OpenstackNodeService.class);

    @Context
    private UriInfo uriInfo;

    /**
     * Creates a set of openstack nodes' config from the JSON input stream.
     *
     * @param input openstack nodes JSON input stream
     * @return 201 CREATED if the JSON is correct, 400 BAD_REQUEST if the JSON
     * is malformed
     * @onos.rsModel OpenstackNode
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createNodes(InputStream input) {
        log.info(String.format(MESSAGE_NODE, CREATE));

        readNodeConfiguration(input).forEach(osNode -> {
            OpenstackNode existing = osNodeService.node(osNode.hostname());
            if (existing == null) {
                osNodeAdminService.createNode(osNode);
            }
        });

        UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                .path(NODES)
                .path(NODE_ID);

        return created(locationBuilder.build()).build();
    }

    /**
     * Updates a set of openstack nodes' config from the JSON input stream.
     *
     * @param input openstack nodes JSON input stream
     * @return 200 OK with the updated openstack node's config, 400 BAD_REQUEST
     * if the JSON is malformed, and 304 NOT_MODIFIED without the updated config
     * @onos.rsModel OpenstackNode
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateNodes(InputStream input) {
        log.info(String.format(MESSAGE_NODE, UPDATE));

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

    /**
     * Removes a set of openstack nodes' config from the JSON input stream.
     *
     * @param hostname host name contained in openstack nodes configuration
     * @return 204 NO_CONTENT, 400 BAD_REQUEST if the JSON is malformed, and
     * 304 NOT_MODIFIED without the updated config
     * @onos.rsModel OpenstackNode
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{hostname}")
    public Response deleteNodes(@PathParam("hostname") String hostname) {
        log.info(String.format(MESSAGE_NODE, DELETE));

        OpenstackNode existing =
                osNodeService.node(nullIsIllegal(hostname, HOST_NAME + ERROR_MESSAGE));

        if (existing == null) {
            log.warn("There is no node configuration to delete : {}", hostname);
            return Response.notModified().build();
        } else {
            osNodeAdminService.removeNode(hostname);
        }

        return Response.noContent().build();
    }

    /**
     * Obtains a set of openstack node's.
     *
     * @return 200 OK with array of all the openstack nodes stored in the system
     * @onos.rsModel OpenstackNode
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response nodes() {
        log.trace(String.format(MESSAGE_NODE, QUERY));

        Set<OpenstackNode> osNodes = osNodeService.nodes();

        for (OpenstackNode osNode : osNodes) {
            osJsonNodes.add(codec(OpenstackNode.class).encode(osNode, this));
        }
        return ok(root).build();
    }

    /**
     * Obtains the state of the openstack node.
     *
     * @param hostname hostname of the openstack
     * @return the state of the openstack node in Json
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("state/{hostname}")
    public Response stateOfNode(@PathParam("hostname") String hostname) {
        log.trace(String.format(MESSAGE_NODE, QUERY));

        OpenstackNode osNode = osNodeService.node(hostname);
        String nodeState = osNode != null ? osNode.state().toString() : NOT_EXIST;

        return ok(mapper().createObjectNode().put(STATE, nodeState)).build();
    }

    /**
     * Initializes openstack node.
     *
     * @param hostname hostname of openstack node
     * @return 200 OK with init result, 404 not found, 500 server error
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("init/node/{hostname}")
    public Response initNode(@PathParam("hostname") String hostname) {
        log.info(String.format(MESSAGE_NODE, INIT));

        OpenstackNode osNode = osNodeService.node(hostname);
        if (osNode == null) {
            log.error("Given node {} does not exist", hostname);
            return Response.serverError().build();
        }
        OpenstackNode updated = osNode.updateState(NodeState.INIT);
        osNodeAdminService.updateNode(updated);
        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Initializes all openstack nodes.
     *
     * @return 200 OK with init result, 500 server error
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("init/all")
    public Response initAllNodes() {
        log.info(String.format(MESSAGE_NODE, INIT));

        osNodeService.nodes()
                .forEach(n -> {
                    OpenstackNode updated = n.updateState(NodeState.INIT);
                    osNodeAdminService.updateNode(updated);
                });

        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Initializes openstack nodes which are in the stats other than COMPLETE.
     *
     * @return 200 OK with init result, 500 server error
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("init/incomplete")
    public Response initIncompleteNodes() {
        log.debug(String.format(MESSAGE_NODE, INIT));

        osNodeService.nodes().stream()
                .filter(n -> n.state() != COMPLETE)
                .forEach(n -> {
                    log.info("Node {} isn't COMPLETE state so performs initialization again.",
                            n.hostname());
                    OpenstackNode updated = n.updateState(NodeState.INIT);
                    osNodeAdminService.updateNode(updated);
                });

        return ok(mapper().createObjectNode()).build();
    }

    private Set<OpenstackNode> readNodeConfiguration(InputStream input) {
        Set<OpenstackNode> nodeSet = Sets.newHashSet();
        try {
             JsonNode jsonTree = readTreeFromStream(mapper().enable(INDENT_OUTPUT), input);
             ArrayNode nodes = (ArrayNode) jsonTree.path(NODES);
             nodes.forEach(node -> {
                     ObjectNode objectNode = node.deepCopy();
                     OpenstackNode openstackNode =
                             codec(OpenstackNode.class).decode(objectNode, this);

                     nodeSet.add(openstackNode);
             });
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        return nodeSet;
    }
}
