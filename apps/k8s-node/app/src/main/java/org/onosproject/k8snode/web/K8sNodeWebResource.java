/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snode.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.onosproject.k8snode.api.K8sApiConfig;
import org.onosproject.k8snode.api.K8sApiConfigAdminService;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeAdminService;
import org.onosproject.k8snode.api.K8sNodeState;
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
import static org.onosproject.k8snode.api.K8sNodeState.POST_ON_BOARD;
import static org.onosproject.k8snode.util.K8sNodeUtil.endpoint;

/**
 * Handles REST API call of kubernetes node config.
 */

@Path("configure")
public class K8sNodeWebResource extends AbstractWebResource {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MESSAGE_NODE = "Received node %s request";
    private static final String NODES = "nodes";
    private static final String API_CONFIGS = "apiConfigs";
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
    private static final String ENDPOINT = "endpoint";
    private static final String ERROR_MESSAGE = " cannot be null";

    private final K8sNodeAdminService nodeAdminService = get(K8sNodeAdminService.class);
    private final K8sApiConfigAdminService configAdminService = get(K8sApiConfigAdminService.class);

    @Context
    private UriInfo uriInfo;

    /**
     * Creates a set of kubernetes nodes' config from the JSON input stream.
     *
     * @param input kubernetes nodes JSON input stream
     * @return 201 CREATED if the JSON is correct, 400 BAD_REQUEST if the JSON
     * is malformed
     * @onos.rsModel K8sNode
     */
    @POST
    @Path("node")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createNodes(InputStream input) {
        log.trace(String.format(MESSAGE_NODE, CREATE));

        readNodeConfiguration(input).forEach(node -> {
            K8sNode existing = nodeAdminService.node(node.hostname());
            if (existing == null) {
                nodeAdminService.createNode(node);
            }
        });

        UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                .path(NODES)
                .path(NODE_ID);

        return created(locationBuilder.build()).build();
    }

    /**
     * Updates a set of kubernetes nodes' config from the JSON input stream.
     *
     * @param input kubernetes nodes JSON input stream
     * @return 200 OK with the updated kubernetes node's config, 400 BAD_REQUEST
     * if the JSON is malformed, and 304 NOT_MODIFIED without the updated config
     * @onos.rsModel K8sNode
     */
    @PUT
    @Path("node")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateNodes(InputStream input) {
        log.trace(String.format(MESSAGE_NODE, UPDATE));

        Set<K8sNode> nodes = readNodeConfiguration(input);
        for (K8sNode node: nodes) {
            K8sNode existing = nodeAdminService.node(node.hostname());
            if (existing == null) {
                log.warn("There is no node configuration to update : {}", node.hostname());
                return Response.notModified().build();
            } else if (!existing.equals(node)) {
                nodeAdminService.updateNode(node);
            }
        }

        return Response.ok().build();
    }

    /**
     * Removes a set of kubernetes nodes' config from the JSON input stream.
     *
     * @param hostname host name contained in kubernetes nodes configuration
     * @return 204 NO_CONTENT, 400 BAD_REQUEST if the JSON is malformed, and
     * 304 NOT_MODIFIED without the updated config
     */
    @DELETE
    @Path("node/{hostname}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteNodes(@PathParam("hostname") String hostname) {
        log.trace(String.format(MESSAGE_NODE, REMOVE));

        K8sNode existing =
                nodeAdminService.node(nullIsIllegal(hostname, HOST_NAME + ERROR_MESSAGE));

        if (existing == null) {
            log.warn("There is no node configuration to delete : {}", hostname);
            return Response.notModified().build();
        } else {
            nodeAdminService.removeNode(hostname);
        }

        return Response.noContent().build();
    }


    /**
     * Obtains the state of the kubernetes node.
     *
     * @param hostname hostname of the kubernetes
     * @return the state of the kubernetes node in Json
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("state/{hostname}")
    public Response stateOfNode(@PathParam("hostname") String hostname) {
        log.trace(String.format(MESSAGE_NODE, QUERY));

        K8sNode k8sNode = nodeAdminService.node(hostname);
        String nodeState = k8sNode != null ? k8sNode.state().toString() : NOT_EXIST;

        return ok(mapper().createObjectNode().put(STATE, nodeState)).build();
    }

    /**
     * Initializes kubernetes node.
     *
     * @param hostname hostname of kubernetes node
     * @return 200 OK with init result, 404 not found, 500 server error
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("init/node/{hostname}")
    public Response initNode(@PathParam("hostname") String hostname) {
        log.trace(String.format(MESSAGE_NODE, QUERY));

        K8sNode k8sNode = nodeAdminService.node(hostname);
        if (k8sNode == null) {
            log.error("Given node {} does not exist", hostname);
            return Response.serverError().build();
        }
        K8sNode updated = k8sNode.updateState(K8sNodeState.INIT);
        nodeAdminService.updateNode(updated);
        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Initializes all kubernetes nodes.
     *
     * @return 200 OK with init result, 500 server error
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("init/all")
    public Response initAllNodes() {
        log.trace(String.format(MESSAGE_NODE, QUERY));

        nodeAdminService.nodes()
                .forEach(n -> {
                    K8sNode updated = n.updateState(K8sNodeState.INIT);
                    nodeAdminService.updateNode(updated);
                });

        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Initializes kubernetes nodes which are in the stats other than COMPLETE.
     *
     * @return 200 OK with init result, 500 server error
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("init/incomplete")
    public Response initIncompleteNodes() {
        log.trace(String.format(MESSAGE_NODE, QUERY));

        nodeAdminService.nodes().stream()
                .filter(n -> n.state() != K8sNodeState.COMPLETE)
                .forEach(n -> {
                    K8sNode updated = n.updateState(K8sNodeState.INIT);
                    nodeAdminService.updateNode(updated);
                });

        return ok(mapper().createObjectNode()).build();
    }

    /**
     * Updates a kubernetes nodes' state as post-on-board.
     *
     * @param hostname kubernetes node name
     * @return 200 OK with the updated kubernetes node's config, 400 BAD_REQUEST
     * if the JSON is malformed, and 304 NOT_MODIFIED without the updated config
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("update/postonboard/{hostname}")
    public Response postOnBoardNode(@PathParam("hostname") String hostname) {
        K8sNode node = nodeAdminService.node(hostname);
        if (node != null && node.state() != POST_ON_BOARD) {
            K8sNode updated = node.updateState(POST_ON_BOARD);
            nodeAdminService.updateNode(updated);
        }
        return Response.ok().build();
    }

    /**
     * Indicates whether all kubernetes nodes are in post-on-board state.
     *
     * @return 200 OK with True, or 200 OK with False
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("get/postonboard/all")
    public Response postOnBoardNodes() {
        long numOfAllNodes = nodeAdminService.nodes().size();
        long numOfReadyNodes = nodeAdminService.nodes().stream()
                .filter(n -> n.state() == POST_ON_BOARD)
                .count();
        boolean result;
        if (numOfAllNodes == 0) {
            result = false;
        } else {
            result = numOfAllNodes == numOfReadyNodes;
        }

        return ok(mapper().createObjectNode().put(RESULT, result)).build();
    }

    private Set<K8sNode> readNodeConfiguration(InputStream input) {
        Set<K8sNode> nodeSet = Sets.newHashSet();
        try {
            JsonNode jsonTree = readTreeFromStream(mapper().enable(INDENT_OUTPUT), input);
            ArrayNode nodes = (ArrayNode) jsonTree.path(NODES);
            nodes.forEach(node -> {
                try {
                    ObjectNode objectNode = node.deepCopy();
                    K8sNode k8sNode =
                            codec(K8sNode.class).decode(objectNode, this);

                    nodeSet.add(k8sNode);
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

    /**
     * Creates a set of kubernetes API config from the JSON input stream.
     *
     * @param input kubernetes API configs JSON input stream
     * @return 201 CREATED if the JSON is correct, 400 BAD_REQUEST if the JSON
     * is malformed
     * @onos.rsModel K8sApiConfig
     */
    @POST
    @Path("api")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createApiConfigs(InputStream input) {
        log.trace(String.format(MESSAGE_NODE, CREATE));

        readApiConfigConfiguration(input).forEach(config -> {
            K8sApiConfig existing = configAdminService.apiConfig(endpoint(config));
            if (existing == null) {
                configAdminService.createApiConfig(config);
            }
        });

        UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                .path(API_CONFIGS);

        return created(locationBuilder.build()).build();
    }

    /**
     * Updates a set of kubernetes API config from the JSON input stream.
     *
     * @param input kubernetes API configs JSON input stream
     * @return 200 OK with the updated kubernetes API config, 400 BAD_REQUEST
     * if the JSON is malformed, and 304 NOT_MODIFIED without the updated config
     * @onos.rsModel K8sApiConfig
     */
    @PUT
    @Path("api")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateApiConfigs(InputStream input) {
        log.trace(String.format(MESSAGE_NODE, UPDATE));

        Set<K8sApiConfig> configs = readApiConfigConfiguration(input);
        for (K8sApiConfig config: configs) {
            K8sApiConfig existing = configAdminService.apiConfig(endpoint(config));
            if (existing == null) {
                log.warn("There is no API configuration to update : {}", endpoint(config));
                return Response.notModified().build();
            } else if (!existing.equals(config)) {
                configAdminService.updateApiConfig(config);
            }
        }

        return Response.ok().build();
    }

    /**
     * Removes a kubernetes API config.
     *
     * @param endpoint kubernetes API endpoint
     * @return 204 NO_CONTENT, 400 BAD_REQUEST if the JSON is malformed
     */
    @DELETE
    @Path("api/{endpoint : .+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteApiConfig(@PathParam("endpoint") String endpoint) {
        log.trace(String.format(MESSAGE_NODE, REMOVE));

        K8sApiConfig existing =
                configAdminService.apiConfig(nullIsIllegal(endpoint, ENDPOINT + ERROR_MESSAGE));

        if (existing == null) {
            log.warn("There is no API configuration to delete : {}", endpoint);
            return Response.notModified().build();
        } else {
            configAdminService.removeApiConfig(endpoint);
        }

        return Response.noContent().build();
    }

    private Set<K8sApiConfig> readApiConfigConfiguration(InputStream input) {
        Set<K8sApiConfig> configSet = Sets.newHashSet();
        try {
            JsonNode jsonTree = readTreeFromStream(mapper().enable(INDENT_OUTPUT), input);
            ArrayNode configs = (ArrayNode) jsonTree.path(API_CONFIGS);
            configs.forEach(config -> {
                try {
                    ObjectNode objectNode = config.deepCopy();
                    K8sApiConfig k8sApiConfig =
                            codec(K8sApiConfig.class).decode(objectNode, this);

                    configSet.add(k8sApiConfig);
                } catch (Exception e) {
                    log.error("Exception occurred due to {}", e);
                    throw new IllegalArgumentException();
                }
            });
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        return configSet;
    }
}
