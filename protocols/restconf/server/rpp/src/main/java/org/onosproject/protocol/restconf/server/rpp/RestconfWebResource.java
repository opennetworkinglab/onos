/*
 * Copyright 2016-present Open Networking Foundation
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
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.restconf.api.MediaTypeRestconf;
import org.onosproject.restconf.api.RestconfError;
import org.onosproject.restconf.api.RestconfException;
import org.onosproject.restconf.api.RestconfRpcOutput;
import org.onosproject.restconf.api.RestconfService;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
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
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static org.onlab.util.Tools.readTreeFromStream;
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
     * status code "200 OK" is returned. If it is not found then "404 Not Found"
     * is returned. On internal error "500 Internal Server Error" is returned.
     *
     * @param uriString URI of the data resource.
     * @return HTTP response - 200, 404 or 500
     */
    @GET
    @Produces(MediaTypeRestconf.APPLICATION_YANG_DATA_JSON)
    @Path("data/{identifier : .+}")
    public Response handleGetRequest(@PathParam("identifier") String uriString) {
        log.debug("handleGetRequest: {}", uriString);

        URI uri = uriInfo.getRequestUri();

        try {
            ObjectNode node = service.runGetOperationOnDataResource(uri);
            if (node == null) {
                RestconfError error =
                        RestconfError.builder(RestconfError.ErrorType.PROTOCOL,
                                RestconfError.ErrorTag.INVALID_VALUE)
                        .errorMessage("Resource not found")
                        .errorPath(uriString)
                        .errorAppTag("handleGetRequest")
                        .build();
                return Response.status(NOT_FOUND)
                        .entity(RestconfError.wrapErrorAsJson(Arrays.asList(error)))
                        .build();
            }
            return Response.ok(node)
            .build();
        } catch (RestconfException e) {
            log.error("ERROR: handleGetRequest: {}", e.getMessage());
            log.debug("Exception in handleGetRequest:", e);
            return Response.status(e.getResponse().getStatus())
                    .entity(e.toRestconfErrorJson())
                    .build();
        } catch (Exception e) {
            RestconfError error = RestconfError
                    .builder(RestconfError.ErrorType.APPLICATION, RestconfError.ErrorTag.OPERATION_FAILED)
                    .errorMessage(e.getMessage()).errorAppTag("handlePostRequest").build();
            return Response.status(INTERNAL_SERVER_ERROR)
                    .entity(RestconfError.wrapErrorAsJson(Arrays.asList(error)))
                    .build();
        }
    }

    /**
     * Handles the RESTCONF Event Notification Subscription request. If the
     * subscription is successful, a ChunkedOutput stream is created and returned
     * to the caller.
     * <p>
     * This function is not blocked on streaming the data (so that it can handle
     * other incoming requests). Instead, a worker thread running in the background
     * does the data streaming. If errors occur during streaming, the worker thread
     * calls ChunkedOutput.close() to disconnect the session and terminates itself.
     *
     * @param streamId Event stream ID
     * @param request  RESTCONF client information from which the client IP
     *                 address is retrieved
     * @return A string data stream over HTTP keep-alive session
     */
    @GET
    @Produces(MediaTypeRestconf.APPLICATION_YANG_DATA_JSON)
    @Path("streams/{streamId}")
    public ChunkedOutput<String> handleNotificationRegistration(@PathParam("streamId") String streamId,
                                                                @Context HttpServletRequest request) {
        final ChunkedOutput<String> output = new ChunkedOutput<>(String.class);
        try {
            service.subscribeEventStream(streamId, request.getRemoteAddr(), output);
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
     * Handles a RESTCONF POST operation against the entire data store. If the
     * operation is successful, HTTP status code "201 Created" is returned
     * and there is no response message-body. If the data resource already
     * exists, then the HTTP status code "409 Conflict" is returned.
     *
     * @param stream Input JSON object
     * @return HTTP response
     */
    @POST
    @Consumes({MediaTypeRestconf.APPLICATION_YANG_DATA_JSON, MediaType.APPLICATION_JSON})
    @Produces(MediaTypeRestconf.APPLICATION_YANG_DATA_JSON)
    @Path("data")
    public Response handlePostDatastore(InputStream stream) {

        log.debug("handlePostDatastore");
        return handlePostRequest(null, stream);
    }

    /**
     * Handles a RESTCONF POST operation against a data resource. If the
     * operation is successful, HTTP status code "201 Created" is returned
     * and there is no response message-body. If the data resource already
     * exists, then the HTTP status code "409 Conflict" is returned.
     *
     * @param uriString URI of the parent data resource
     * @param stream    Input JSON object
     * @return HTTP response
     */
    @POST
    @Consumes({ MediaTypeRestconf.APPLICATION_YANG_DATA_JSON, MediaType.APPLICATION_JSON })
    @Produces(MediaTypeRestconf.APPLICATION_YANG_DATA_JSON)
    @Path("data/{identifier : .+}")
    public Response handlePostRequest(@PathParam("identifier") String uriString,
                                      InputStream stream) {
        log.debug("handlePostRequest: {}", uriString);

        URI uri = uriInfo.getRequestUri();

        try {
            ObjectNode rootNode = readTreeFromStream(mapper(), stream);

            service.runPostOperationOnDataResource(uri, rootNode);
            return Response.created(uriInfo.getRequestUri()).build();
        } catch (JsonProcessingException e) {
            log.error("ERROR: handlePostRequest ", e);
            RestconfError error = RestconfError
                    .builder(RestconfError.ErrorType.APPLICATION, RestconfError.ErrorTag.MALFORMED_MESSAGE)
                    .errorMessage(e.getMessage()).errorAppTag("handlePostRequest").build();
            return Response.status(BAD_REQUEST)
                    .entity(RestconfError.wrapErrorAsJson(Arrays.asList(error))).build();
        } catch (RestconfException e) {
            log.error("ERROR: handlePostRequest: {}", e.getMessage());
            log.debug("Exception in handlePostRequest:", e);
            return Response.status(e.getResponse().getStatus())
                    .entity(e.toRestconfErrorJson()).build();
        } catch (IOException ex) {
            log.error("ERROR: handlePostRequest ", ex);
            RestconfError error = RestconfError
                    .builder(RestconfError.ErrorType.APPLICATION, RestconfError.ErrorTag.OPERATION_FAILED)
                    .errorMessage(ex.getMessage()).errorAppTag("handlePostRequest").build();
            return Response.status(INTERNAL_SERVER_ERROR)
                    .entity(RestconfError.wrapErrorAsJson(Arrays.asList(error))).build();
        }
    }

    /**
     * Handles a RESTCONF RPC request. This function executes the RPC in
     * the target application's context and returns the results as a Future.
     *
     * @param rpcName  Name of the RPC
     * @param rpcInput Input parameters
     * @param request  RESTCONF client information from which the client IP
     *                 address is retrieved
     * @return RPC output
     */
    @POST
    @Consumes({ MediaTypeRestconf.APPLICATION_YANG_DATA_JSON, MediaType.APPLICATION_JSON })
    @Produces(MediaTypeRestconf.APPLICATION_YANG_DATA_JSON)
    @Path("operations/{rpc : .+}")
    public Response handleRpcRequest(@PathParam("rpc") String rpcName,
                                     InputStream rpcInput,
                                     @Context HttpServletRequest request) {
        URI uri = uriInfo.getRequestUri();
        try {
            ObjectNode inputNode = readTreeFromStream(mapper(), rpcInput);
            CompletableFuture<RestconfRpcOutput> rpcFuture = service.runRpc(uri,
                                                                            inputNode,
                                                                            request.getRemoteAddr());
            RestconfRpcOutput restconfRpcOutput;
            restconfRpcOutput = rpcFuture.get();
            if (restconfRpcOutput.status() != OK) {
                return Response.status(restconfRpcOutput.status())
                        .entity(restconfRpcOutput.reason()).build();
            }
            ObjectNode node = restconfRpcOutput.output();
            return ok(node).build();
        } catch (JsonProcessingException e) {
            log.error("ERROR:  handleRpcRequest", e);
            RestconfError error = RestconfError
                    .builder(RestconfError.ErrorType.APPLICATION, RestconfError.ErrorTag.MALFORMED_MESSAGE)
                    .errorMessage(e.getMessage()).errorAppTag("handleRpcRequest").build();
            return Response.status(BAD_REQUEST)
                    .entity(RestconfError.wrapErrorAsJson(Arrays.asList(error))).build();
        } catch (RestconfException e) {
            log.error("ERROR: handleRpcRequest: {}", e.getMessage());
            log.debug("Exception in handleRpcRequest:", e);
            return Response.status(e.getResponse().getStatus())
                    .entity(e.toRestconfErrorJson()).build();
        } catch (Exception e) {
            log.error("ERROR: handleRpcRequest ", e);
            RestconfError error = RestconfError
                    .builder(RestconfError.ErrorType.APPLICATION, RestconfError.ErrorTag.OPERATION_FAILED)
                    .errorMessage(e.getMessage()).errorAppTag("handleRpcRequest").build();
            return Response.status(INTERNAL_SERVER_ERROR)
                    .entity(RestconfError.wrapErrorAsJson(Arrays.asList(error))).build();
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
    @Consumes({ MediaTypeRestconf.APPLICATION_YANG_DATA_JSON, MediaType.APPLICATION_JSON })
    @Produces(MediaTypeRestconf.APPLICATION_YANG_DATA_JSON)
    @Path("data/{identifier : .+}")
    public Response handlePutRequest(@PathParam("identifier") String uriString,
                                     InputStream stream) {
        log.debug("handlePutRequest: {}", uriString);

        URI uri = uriInfo.getRequestUri();

        try {
            ObjectNode rootNode = readTreeFromStream(mapper(), stream);

            service.runPutOperationOnDataResource(uri, rootNode);
            return Response.created(uriInfo.getRequestUri()).build();
        } catch (JsonProcessingException e) {
            log.error("ERROR: handlePutRequest ", e);
            RestconfError error = RestconfError
                    .builder(RestconfError.ErrorType.APPLICATION, RestconfError.ErrorTag.MALFORMED_MESSAGE)
                    .errorMessage(e.getMessage()).errorAppTag("handlePutRequest").build();
            return Response.status(BAD_REQUEST)
                    .entity(RestconfError.wrapErrorAsJson(Arrays.asList(error))).build();
        } catch (RestconfException e) {
            log.error("ERROR: handlePutRequest: {}", e.getMessage());
            log.debug("Exception in handlePutRequest:", e);
            return Response.status(e.getResponse().getStatus())
                    .entity(e.toRestconfErrorJson()).build();
        } catch (IOException ex) {
            log.error("ERROR: handlePutRequest ", ex);
            RestconfError error = RestconfError
                    .builder(RestconfError.ErrorType.APPLICATION, RestconfError.ErrorTag.OPERATION_FAILED)
                    .errorMessage(ex.getMessage()).errorAppTag("handlePutRequest").build();
            return Response.status(INTERNAL_SERVER_ERROR)
                    .entity(RestconfError.wrapErrorAsJson(Arrays.asList(error))).build();
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
    @Produces(MediaTypeRestconf.APPLICATION_YANG_DATA_JSON)
    @Path("data/{identifier : .+}")
    public Response handleDeleteRequest(@PathParam("identifier") String uriString) {
        log.debug("handleDeleteRequest: {}", uriString);

        URI uri = uriInfo.getRequestUri();

        try {
            service.runDeleteOperationOnDataResource(uri);
            return Response.ok().build();
        } catch (RestconfException e) {
            log.error("ERROR: handleDeleteRequest: {}", e.getMessage());
            log.debug("Exception in handleDeleteRequest:", e);
            return Response.status(e.getResponse().getStatus())
                    .entity(e.toRestconfErrorJson()).build();
        }
    }

    /**
     * Handles a RESTCONF PATCH operation against a data resource.
     * If the PATCH request succeeds, a "200 OK" status-line is returned if
     * there is a message-body, and "204 No Content" is returned if no
     * response message-body is sent.
     *
     * @param uriString URI of the parent data resource
     * @param stream    Input JSON object
     * @return HTTP response
     */
    @PATCH
    @Consumes({ MediaTypeRestconf.APPLICATION_YANG_DATA_JSON, MediaType.APPLICATION_JSON })
    @Produces(MediaTypeRestconf.APPLICATION_YANG_DATA_JSON)
    @Path("data/{identifier : .+}")
    public Response handlePatchRequest(@PathParam("identifier") String uriString,
                                       InputStream stream) {

        log.debug("handlePatchRequest: {}", uriString);

        URI uri = uriInfo.getRequestUri();

        try {
            ObjectNode rootNode = readTreeFromStream(mapper(), stream);

            service.runPatchOperationOnDataResource(uri, rootNode);
            return Response.ok().build();
        } catch (JsonProcessingException e) {
            log.error("ERROR: handlePatchRequest ", e);
            RestconfError error = RestconfError
                    .builder(RestconfError.ErrorType.APPLICATION, RestconfError.ErrorTag.MALFORMED_MESSAGE)
                    .errorMessage(e.getMessage()).errorAppTag("handlePatchRequest").build();
            return Response.status(BAD_REQUEST)
                    .entity(RestconfError.wrapErrorAsJson(Arrays.asList(error))).build();
        } catch (RestconfException e) {
            log.error("ERROR: handlePatchRequest: {}", e.getMessage());
            log.debug("Exception in handlePatchRequest:", e);
            return Response.status(e.getResponse().getStatus())
                    .entity(e.toRestconfErrorJson()).build();
        } catch (IOException ex) {
            log.error("ERROR: handlePatchRequest ", ex);
            RestconfError error = RestconfError
                    .builder(RestconfError.ErrorType.APPLICATION, RestconfError.ErrorTag.OPERATION_FAILED)
                    .errorMessage(ex.getMessage()).errorAppTag("handlePatchRequest").build();
            return Response.status(INTERNAL_SERVER_ERROR)
                    .entity(RestconfError.wrapErrorAsJson(Arrays.asList(error))).build();
        }
    }


    /**
     * Handles a RESTCONF PATCH operation against the entire data store.
     * If the PATCH request succeeds, a "200 OK" status-line is returned if
     * there is a message-body, and "204 No Content" is returned if no
     * response message-body is sent.
     *
     * @param stream Input JSON object
     * @return HTTP response
     */
    @PATCH
    @Consumes({ MediaTypeRestconf.APPLICATION_YANG_DATA_JSON, MediaType.APPLICATION_JSON })
    @Produces(MediaTypeRestconf.APPLICATION_YANG_DATA_JSON)
    @Path("data")
    public Response handlePatchDatastore(InputStream stream) {
        log.debug("handlePatchDatastore");
        return handlePatchRequest(null, stream);
    }
}
