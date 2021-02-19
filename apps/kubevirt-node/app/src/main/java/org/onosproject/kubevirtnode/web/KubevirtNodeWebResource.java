/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.onosproject.kubevirtnode.api.KubevirtNode;
import org.onosproject.kubevirtnode.api.KubevirtNodeAdminService;
import org.onosproject.kubevirtnode.api.KubevirtNodeState;
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

/**
 * Handles REST API call of KubeVirt node config.
 */
@Path("node")
public class KubevirtNodeWebResource extends AbstractWebResource {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MESSAGE_NODE = "Received node %s request";
    private static final String NODES = "nodes";
    private static final String CREATE = "CREATE";
    private static final String UPDATE = "UPDATE";
    private static final String NODE_ID = "NODE_ID";
    private static final String REMOVE = "REMOVE";
    private static final String QUERY = "QUERY";
    private static final String INIT = "INIT";
    private static final String NOT_EXIST = "Not exist";
    private static final String STATE = "State";
    private static final String RESULT = "Result";

    private static final String HOST_NAME = "hostname";
    private static final String ERROR_MESSAGE = " cannot be null";

    @Context
    private UriInfo uriInfo;

    /**
     * Creates a set of KubeVirt nodes' config from the JSON input stream.
     *
     * @param input KubeVirt nodes JSON input stream
     * @return 201 CREATED if the JSON is correct, 400 BAD_REQUEST if the JSON
     * is malformed
     * @onos.rsModel KubevirtNode
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createNodes(InputStream input) {
        log.trace(String.format(MESSAGE_NODE, CREATE));

        KubevirtNodeAdminService service = get(KubevirtNodeAdminService.class);

        readNodeConfiguration(input).forEach(node -> {
            KubevirtNode existing = service.node(node.hostname());
            if (existing == null) {
                service.createNode(node);
            }
        });

        UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                .path(NODES)
                .path(NODE_ID);

        return created(locationBuilder.build()).build();
    }

    /**
     * Updates a set of KubeVirt nodes' config from the JSON input stream.
     *
     * @param input KubeVirt nodes JSON input stream
     * @return 200 OK with the updated KubeVirt node's config, 400 BAD_REQUEST
     * if the JSON is malformed, and 304 NOT_MODIFIED without the updated config
     * @onos.rsModel KubevirtNode
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateNodes(InputStream input) {
        log.trace(String.format(MESSAGE_NODE, UPDATE));

        KubevirtNodeAdminService service = get(KubevirtNodeAdminService.class);
        Set<KubevirtNode> nodes = readNodeConfiguration(input);
        for (KubevirtNode node: nodes) {
            KubevirtNode existing = service.node(node.hostname());
            if (existing == null) {
                log.warn("There is no node configuration to update : {}", node.hostname());
                return Response.notModified().build();
            } else if (!existing.equals(node)) {
                service.updateNode(node);
            }
        }

        return Response.ok().build();
    }

    /**
     * Removes a set of KubeVirt nodes' config from the JSON input stream.
     *
     * @param hostname host name contained in KubeVirt nodes configuration
     * @return 204 NO_CONTENT, 400 BAD_REQUEST if the JSON is malformed, and
     * 304 NOT_MODIFIED without the updated config
     */
    @DELETE
    @Path("{hostname}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteNode(@PathParam("hostname") String hostname) {
        log.trace(String.format(MESSAGE_NODE, REMOVE));

        KubevirtNodeAdminService service = get(KubevirtNodeAdminService.class);
        KubevirtNode existing = service.node(
                nullIsIllegal(hostname, HOST_NAME + ERROR_MESSAGE));

        if (existing == null) {
            log.warn("There is no node configuration to delete : {}", hostname);
            return Response.notModified().build();
        } else {
            service.removeNode(hostname);
        }

        return Response.noContent().build();
    }

    /**
     * Obtains the state of the KubeVirt node.
     *
     * @param hostname hostname of the KubeVirt
     * @return the state of the KubeVirt node in Json
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("state/{hostname}")
    public Response stateOfNode(@PathParam("hostname") String hostname) {
        log.trace(String.format(MESSAGE_NODE, QUERY));

        KubevirtNodeAdminService service = get(KubevirtNodeAdminService.class);
        KubevirtNode node = service.node(hostname);
        String nodeState = node != null ? node.state().toString() : NOT_EXIST;

        return ok(mapper().createObjectNode().put(STATE, nodeState)).build();
    }

    /**
     * Initializes KubeVirt node.
     *
     * @param hostname hostname of KubeVirt node
     * @return 200 OK with init result, 404 not found, 500 server error
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("init/{hostname}")
    public Response initNode(@PathParam("hostname") String hostname) {
        log.trace(String.format(MESSAGE_NODE, QUERY));

        KubevirtNodeAdminService service = get(KubevirtNodeAdminService.class);
        KubevirtNode node = service.node(hostname);
        if (node == null) {
            log.error("Given node {} does not exist", hostname);
            return Response.serverError().build();
        }
        KubevirtNode updated = node.updateState(KubevirtNodeState.INIT);
        service.updateNode(updated);
        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Initializes all KubeVirt nodes.
     *
     * @return 200 OK with init result, 500 server error
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("init/all")
    public Response initAllNodes() {
        log.trace(String.format(MESSAGE_NODE, QUERY));

        KubevirtNodeAdminService service = get(KubevirtNodeAdminService.class);

        service.nodes()
                .forEach(n -> {
                    KubevirtNode updated = n.updateState(KubevirtNodeState.INIT);
                    service.updateNode(updated);
                });

        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Initializes KubeVirt nodes which are in the stats other than COMPLETE.
     *
     * @return 200 OK with init result, 500 server error
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("init/incomplete")
    public Response initIncompleteNodes() {
        log.trace(String.format(MESSAGE_NODE, QUERY));

        KubevirtNodeAdminService service = get(KubevirtNodeAdminService.class);
        service.nodes().stream()
                .filter(n -> n.state() != KubevirtNodeState.COMPLETE)
                .forEach(n -> {
                    KubevirtNode updated = n.updateState(KubevirtNodeState.INIT);
                    service.updateNode(updated);
                });

        return ok(mapper().createObjectNode()).build();
    }

    private Set<KubevirtNode> readNodeConfiguration(InputStream input) {
        Set<KubevirtNode> nodeSet = Sets.newHashSet();
        try {
            JsonNode jsonTree = readTreeFromStream(mapper().enable(INDENT_OUTPUT), input);
            ArrayNode nodes = (ArrayNode) jsonTree.path(NODES);
            nodes.forEach(node -> {
                try {
                    ObjectNode objectNode = node.deepCopy();
                    KubevirtNode kubevirtNode =
                            codec(KubevirtNode.class).decode(objectNode, this);
                    nodeSet.add(kubevirtNode);
                } catch (Exception e) {
                    log.error("Exception occurred due to {}", e);
                    throw new IllegalArgumentException();
                }
            });
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        return nodeSet;
    }
}
