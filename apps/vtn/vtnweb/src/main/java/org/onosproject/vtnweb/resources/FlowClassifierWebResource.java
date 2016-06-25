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
package org.onosproject.vtnweb.resources;

import static javax.ws.rs.core.Response.Status.OK;
import static org.onlab.util.Tools.nullIsNotFound;

import java.io.IOException;
import java.io.InputStream;

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

import org.onosproject.rest.AbstractWebResource;
import org.onosproject.vtnrsc.FlowClassifier;
import org.onosproject.vtnrsc.FlowClassifierId;
import org.onosproject.vtnrsc.flowclassifier.FlowClassifierService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Query and program flow classifier.
 */
@Path("flow_classifiers")
public class FlowClassifierWebResource extends AbstractWebResource {

    private final Logger log = LoggerFactory.getLogger(FlowClassifierWebResource.class);

    public static final String FLOW_CLASSIFIER_NOT_FOUND = "Flow classifier not found";

    /**
     * Get all flow classifiers created.
     *
     * @return 200 OK
     */
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFlowClassifiers() {
        Iterable<FlowClassifier> flowClassifiers = get(FlowClassifierService.class).getFlowClassifiers();
        ObjectNode result = mapper().createObjectNode();
        ArrayNode flowClassifierEntry = result.putArray("flow_classifiers");
        if (flowClassifiers != null) {
            for (final FlowClassifier flowClassifier : flowClassifiers) {
                flowClassifierEntry.add(codec(FlowClassifier.class).encode(flowClassifier, this));
            }
        }
        return ok(result.toString()).build();
    }

    /**
     * Get details of a flow classifier.
     *
     * @param id
     *            flow classifier id
     * @return 200 OK , 404 if given identifier does not exist
     */
    @GET
    @Path("{flow_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFlowClassifier(@PathParam("flow_id") String id) {
        FlowClassifier flowClassifier = nullIsNotFound(get(FlowClassifierService.class)
                                         .getFlowClassifier(FlowClassifierId.of(id)), FLOW_CLASSIFIER_NOT_FOUND);

        ObjectNode result = mapper().createObjectNode();
        result.set("flow_classifier", codec(FlowClassifier.class).encode(flowClassifier, this));

        return ok(result.toString()).build();
    }

    /**
     * Creates and stores a new flow classifier.
     *
     * @param stream
     *            flow classifier from JSON
     * @return status of the request - CREATED if the JSON is correct,
     *         BAD_REQUEST if the JSON is invalid
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createFlowClassifier(InputStream stream) {
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode flow = jsonTree.get("flow_classifier");

            FlowClassifier flowClassifier = codec(FlowClassifier.class).decode((ObjectNode) flow, this);
            Boolean issuccess = nullIsNotFound(get(FlowClassifierService.class).createFlowClassifier(flowClassifier),
                                               FLOW_CLASSIFIER_NOT_FOUND);
            return Response.status(OK).entity(issuccess.toString()).build();
        } catch (IOException ex) {
            log.error("Exception while creating flow classifier {}.", ex.toString());
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Update details of a flow classifier.
     *
     * @param id
     *            flow classifier id
     * @param stream
     *            InputStream
     * @return 200 OK, 404 if given identifier does not exist
     */
    @PUT
    @Path("{flow_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateFlowClassifier(@PathParam("flow_id") String id, final InputStream stream) {
        try {

            JsonNode jsonTree = mapper().readTree(stream);
            JsonNode flow = jsonTree.get("flow_classifier");
            FlowClassifier flowClassifier = codec(FlowClassifier.class).decode((ObjectNode) flow, this);
            Boolean result = nullIsNotFound(get(FlowClassifierService.class).updateFlowClassifier(flowClassifier),
                                            FLOW_CLASSIFIER_NOT_FOUND);
            return Response.status(OK).entity(result.toString()).build();
        } catch (IOException e) {
            log.error("Update flow classifier failed because of exception {}.", e.toString());
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Delete details of a flow classifier.
     *
     * @param id flow classifier id
     * @return 204 NO CONTENT
     */
    @Path("{flow_id}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteFlowClassifier(@PathParam("flow_id") String id) {
        log.debug("Deletes flow classifier by identifier {}.", id);
        FlowClassifierId flowClassifierId = FlowClassifierId.of(id);
        Boolean issuccess = nullIsNotFound(get(FlowClassifierService.class).removeFlowClassifier(flowClassifierId),
                                           FLOW_CLASSIFIER_NOT_FOUND);
        return Response.noContent().build();
    }
}
