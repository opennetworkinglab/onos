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
package org.onosproject.vtnweb.resources;

import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.onlab.util.Tools.nullIsNotFound;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onosproject.vtnrsc.FlowClassifier;
import org.onosproject.vtnrsc.FlowClassifierId;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.vtnrsc.flowClassifier.FlowClassifierService;
import org.onosproject.vtnrsc.web.FlowClassifierCodec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Query and program flow classifier.
 */
@Path("flow_classifiers")
public class FlowClassifierWebResource extends AbstractWebResource {

    final FlowClassifierService service = get(FlowClassifierService.class);
    final ObjectNode root = mapper().createObjectNode();
    public static final String FLOW_CLASSIFIER_NOT_FOUND = "Flow classifier not found";

    /**
     * Get all flow classifiers created. Returns list of all flow classifiers
     * created.
     *
     * @return 200 OK
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFlowClassifiers() {
        Iterable<FlowClassifier> flowClassifiers = service.getFlowClassifiers();
        ObjectNode result = new ObjectMapper().createObjectNode();
        result.set("flow_classifiers", new FlowClassifierCodec().encode(flowClassifiers, this));
        return ok(result.toString()).build();
    }

    /**
     * Get details of a flow classifier. Returns details of a specified flow
     * classifier id.
     *
     * @param id flow classifier id
     * @return 200 OK
     */
    @GET
    @Path("{flow_id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFlowClassifier(@PathParam("flow_id") String id) {

        if (!service.hasFlowClassifier(FlowClassifierId.flowClassifierId(UUID.fromString(id)))) {
            return Response.status(NOT_FOUND).entity(FLOW_CLASSIFIER_NOT_FOUND).build();
        }
        FlowClassifier flowClassifier = nullIsNotFound(
                service.getFlowClassifier(FlowClassifierId.flowClassifierId(UUID.fromString(id))),
                FLOW_CLASSIFIER_NOT_FOUND);

        ObjectNode result = new ObjectMapper().createObjectNode();
        result.set("flow_classifier", new FlowClassifierCodec().encode(flowClassifier, this));
        return ok(result.toString()).build();
    }

    /**
     * Creates and stores a new flow classifier.
     *
     * @param flowClassifierId flow classifier identifier
     * @param stream flow classifier from JSON
     * @return status of the request - CREATED if the JSON is correct,
     *         BAD_REQUEST if the JSON is invalid
     */
    @POST
    @Path("{flow_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createFlowClassifier(@PathParam("flow_id") String flowClassifierId, InputStream stream) {
        URI location;
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);

            FlowClassifier flowClassifier = codec(FlowClassifier.class).decode(jsonTree, this);
            service.createFlowClassifier(flowClassifier);
            location = new URI(flowClassifierId);
        } catch (IOException | URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
        return Response.created(location).build();
    }

    /**
     * Creates and stores a new flow classifier.
     *
     * @param stream flow classifier from JSON
     * @return status of the request - CREATED if the JSON is correct,
     *         BAD_REQUEST if the JSON is invalid
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createFlowClassifier(InputStream stream) {
        URI location;
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);

            FlowClassifier flowClassifier = codec(FlowClassifier.class).decode(jsonTree, this);
            service.createFlowClassifier(flowClassifier);
            location = new URI(flowClassifier.flowClassifierId().toString());
        } catch (IOException | URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
        return Response.created(location).build();
    }

    /**
     * Update details of a flow classifier. Update details of a specified flow
     * classifier id.
     *
     * @param id flow classifier id
     * @param stream InputStream
     * @return 200 OK
     */
    @PUT
    @Path("{flow_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateFlowClassifier(@PathParam("flow_id") String id, final InputStream stream) {
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            FlowClassifier flowClassifier = codec(FlowClassifier.class).decode(jsonTree, this);
            Boolean result = nullIsNotFound(service.updateFlowClassifier(flowClassifier), FLOW_CLASSIFIER_NOT_FOUND);
            if (!result) {
                return Response.status(204).entity(FLOW_CLASSIFIER_NOT_FOUND).build();
            }
            return Response.status(203).entity(result.toString()).build();
        } catch (Exception e) {
            return Response.status(INTERNAL_SERVER_ERROR).entity(e.toString()).build();
        }
    }

    /**
     * Delete details of a flow classifier. Delete details of a specified flow
     * classifier id.
     *
     * @param id flow classifier id
     * @return 200 OK
     * @throws IOException when input doesn't match.
     */
    @Path("{flow_id}")
    @DELETE
    public Response deleteFlowClassifier(@PathParam("flow_id") String id) throws IOException {
        try {
            FlowClassifierId flowClassifierId = FlowClassifierId.flowClassifierId(UUID.fromString(id));
            service.removeFlowClassifier(flowClassifierId);
            return Response.status(201).entity("SUCCESS").build();
        } catch (Exception e) {
            return Response.status(INTERNAL_SERVER_ERROR).entity(e.toString()).build();
        }
    }
}
