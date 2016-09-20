/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.protocol.restconf.server.rpp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.glassfish.jersey.server.ChunkedOutput;
import org.onosproject.protocol.restconf.server.api.Patch;
import org.onosproject.protocol.restconf.server.api.RestconfException;
import org.onosproject.protocol.restconf.server.api.RestconfService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;

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
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static org.slf4j.LoggerFactory.getLogger;


/*
 * This class is the main implementation of the RESTCONF Protocol
 * Proxy module. Currently it only handles some basic operations
 * on data resource nodes. However, the design intention is to
 * create a code structure that allows new methods/functionality
 * to be easily added in future releases.
 */

/**
 * Implementation of the RESTCONF Protocol Proxy module.
 */
@Path("/")
public class RestconfWebResource extends AbstractWebResource {

    @Context
    UriInfo uriInfo;

    private final RestconfService service = get(RestconfService.class);
    private final Logger log = getLogger(getClass());

    /**
     * Handles a RESTCONF GET operation against a target data resource. If the
     * operation is successful, the JSON presentation of the resource plus HTTP
     * status code "200 OK" is returned. Otherwise, HTTP error status code
     * "400 Bad Request" is returned.
     *
     * @param uriString URI of the data resource.
     * @return HTTP response
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("data/{identifier : .+}")
    public Response handleGetRequest(@PathParam("identifier") String uriString) {

        log.debug("handleGetRequest: {}", uriString);

        try {
            ObjectNode node = service.runGetOperationOnDataResource(uriString);
            return ok(node).build();
        } catch (RestconfException e) {
            log.error("ERROR: handleGetRequest: {}", e.getMessage());
            log.debug("Exception in handleGetRequest:", e);
            return e.getResponse();
        }
    }

    /**
     * Handles the RESTCONF Event Notification Subscription request. If the
     * subscription is successful, a ChunkedOutput stream is created and returned
     * to the caller.
     * <P></P>
     * This function is not blocked on streaming the data (so that it can handle
     * other incoming requests). Instead, a worker thread running in the background
     * does the data streaming. If errors occur during streaming, the worker thread
     * calls ChunkedOutput.close() to disconnect the session and terminates itself.
     *
     * @param streamId Event stream ID
     * @return A string data stream over HTTP keep-alive session
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("streams/{streamId}")
    public ChunkedOutput<String> handleNotificationRegistration(@PathParam("streamId") String streamId) {
        final ChunkedOutput<String> output = new ChunkedOutput<String>(String.class);
        try {
            service.subscribeEventStream(streamId, output);
        } catch (RestconfException e) {
            log.error("ERROR: handleNotificationRegistration: {}", e.getMessage());
            log.debug("Exception in handleNotificationRegistration:", e);
            try {
                output.close();
            } catch (IOException ex) {
                log.error("ERROR: handleNotificationRegistration:", ex);
            }
        }

        return output;
    }

    /**
     * Handles a RESTCONF POST operation against a data resource. If the
     * operation is successful, HTTP status code "201 Created" is returned
     * and there is no response message-body. If the data resource already
     * exists, then the HTTP status code "409 Conflict" is returned.
     *
     * @param uriString URI of the data resource
     * @param stream    Input JSON object
     * @return HTTP response
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("data/{identifier : .+}")
    public Response handlePostRequest(@PathParam("identifier") String uriString,
                                      InputStream stream) {

        log.debug("handlePostRequest: {}", uriString);

        try {
            ObjectNode rootNode = (ObjectNode) mapper().readTree(stream);

            service.runPostOperationOnDataResource(uriString, rootNode);
            return Response.created(uriInfo.getRequestUri()).build();
        } catch (JsonProcessingException e) {
            log.error("ERROR: handlePostRequest ", e);
            return Response.status(BAD_REQUEST).build();
        } catch (RestconfException e) {
            log.error("ERROR: handlePostRequest: {}", e.getMessage());
            log.debug("Exception in handlePostRequest:", e);
            return e.getResponse();
        } catch (IOException ex) {
            log.error("ERROR: handlePostRequest ", ex);
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Handles a RESTCONF PUT operation against a data resource. If a new
     * resource is successfully created, then the HTTP status code "201 Created"
     * is returned. If an existing resource is modified, then the HTTP
     * status code "204 No Content" is returned. If the input JSON payload
     * contains errors, then "400 Bad Request" is returned. If an exception
     * occurs during the operation, the status code enclosed in
     * the RestconfException object, such as "500 Internal Server Error",
     * is returned.
     *
     * @param uriString URI of the data resource.
     * @param stream    Input JSON object
     * @return HTTP response
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("data/{identifier : .+}")
    public Response handlePutRequest(@PathParam("identifier") String uriString,
                                     InputStream stream) {

        log.debug("handlePutRequest: {}", uriString);

        try {
            ObjectNode rootNode = (ObjectNode) mapper().readTree(stream);

            service.runPutOperationOnDataResource(uriString, rootNode);
            return Response.created(uriInfo.getRequestUri()).build();
        } catch (JsonProcessingException e) {
            log.error("ERROR: handlePutRequest ", e);
            return Response.status(BAD_REQUEST).build();
        } catch (RestconfException e) {
            log.error("ERROR: handlePutRequest: {}", e.getMessage());
            log.debug("Exception in handlePutRequest:", e);
            return e.getResponse();
        } catch (IOException ex) {
            log.error("ERROR: handlePutRequest ", ex);
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Handles the RESTCONF DELETION Operation against a data resource. If the
     * resource is successfully deleted, the HTTP status code "204 No Content"
     * is returned in the response. If an exception occurs, then the
     * HTTP status code enclosed in the RestconfException object is
     * returned.
     *
     * @param uriString URI of the data resource to be deleted.
     * @return HTTP response
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("data/{identifier : .+}")
    public Response handleDeleteRequest(@PathParam("identifier") String uriString) {

        log.debug("handleDeleteRequest: {}", uriString);

        try {
            service.runDeleteOperationOnDataResource(uriString);
            return Response.ok().build();
        } catch (RestconfException e) {
            log.error("ERROR: handleDeleteRequest: {}", e.getMessage());
            log.debug("Exception in handleDeleteRequest:", e);
            return e.getResponse();
        }
    }

    /**
     * Handles a RESTCONF PATCH operation against a data resource.
     * If the PATCH request succeeds, a "200 OK" status-line is returned if
     * there is a message-body, and "204 No Content" is returned if no
     * response message-body is sent.
     *
     * @param uriString URI of the data resource
     * @param stream    Input JSON object
     * @return HTTP response
     */
    @Patch
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("data/{identifier : .+}")
    public Response handlePatchRequest(@PathParam("identifier") String uriString,
                                       InputStream stream) {

        log.debug("handlePatchRequest: {}", uriString);

        try {
            ObjectNode rootNode = (ObjectNode) mapper().readTree(stream);

            service.runPatchOperationOnDataResource(uriString, rootNode);
            return Response.ok().build();
        } catch (JsonProcessingException e) {
            log.error("ERROR: handlePatchRequest ", e);
            return Response.status(BAD_REQUEST).build();
        } catch (RestconfException e) {
            log.error("ERROR: handlePatchRequest: {}", e.getMessage());
            log.debug("Exception in handlePatchRequest:", e);
            return e.getResponse();
        } catch (IOException ex) {
            log.error("ERROR: handlePatchRequest ", ex);
            return Response.status(INTERNAL_SERVER_ERROR).build();
        }
    }

}
