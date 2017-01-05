/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.restconf.api;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.glassfish.jersey.server.ChunkedOutput;

/**
 * Abstraction of RESTCONF Server functionality according to the
 * RESTCONF RFC (no official RFC number yet).
 */
public interface RestconfService {
    /**
     * Processes a GET request against a data resource. The
     * target data resource is identified by its URI. If the
     * GET operation cannot be fulfilled due to reasons such
     * as the nonexistence of the target resource, then a
     * RestconfException exception is raised. The proper
     * HTTP error status code is enclosed in the exception, so
     * that the caller may return it to the RESTCONF client to
     * display.
     *
     * @param uri URI of the target data resource
     * @return JSON representation of the data resource
     * @throws RestconfException if the GET operation cannot be fulfilled
     */
    ObjectNode runGetOperationOnDataResource(String uri)
            throws RestconfException;

    /**
     * Processes a POST request against a data resource. The location of
     * the target resource is passed in as a URI. And the resource's
     * content is passed in as a JSON ObjectNode. If the POST operation
     * cannot be fulfilled due to reasons such as wrong input URIs or
     * syntax errors in the JSON payloads, a RestconfException exception
     * is raised. The proper HTTP error status code is enclosed in the
     * exception.
     *
     * @param uri      URI of the data resource to be created
     * @param rootNode JSON representation of the data resource
     * @throws RestconfException if the POST operation cannot be fulfilled
     */
    void runPostOperationOnDataResource(String uri, ObjectNode rootNode)
            throws RestconfException;

    /**
     * Processes a PUT request against a data resource. The location of
     * the target resource is passed in as a URI. And the resource's
     * content is passed in as a JSON ObjectNode. If the PUT operation
     * cannot be fulfilled due to reasons such as wrong input URIs or
     * syntax errors in the JSON payloads, a RestconfException exception
     * is raised. The proper HTTP error status code is enclosed in the
     * exception.
     *
     * @param uri      URI of the data resource to be created or updated
     * @param rootNode JSON representation of the data resource
     * @throws RestconfException if the PUT operation cannot be fulfilled
     */
    void runPutOperationOnDataResource(String uri, ObjectNode rootNode)
            throws RestconfException;

    /**
     * Processes the DELETE operation against a data resource. The target
     * data resource is identified by its URI. If the DELETE operation
     * cannot be fulfilled due reasons such as the nonexistence of the
     * target resource, a RestconfException exception is raised. The
     * proper HTTP error status code is enclosed in the exception.
     *
     * @param uri URI of the data resource to be deleted
     * @throws RestconfException if the DELETE operation cannot be fulfilled
     */
    void runDeleteOperationOnDataResource(String uri) throws RestconfException;

    /**
     * Processes a PATCH operation on a data resource. The target data
     * resource is identified by its URI passed in by the caller.
     * And the content of the data resource is passed in as a JSON ObjectNode.
     * If the PATCH operation cannot be fulfilled due reasons such as
     * the nonexistence of the target resource, a RestconfException
     * exception is raised. The proper HTTP error status code is
     * enclosed in the exception.
     *
     * @param uri      URI of the data resource to be patched
     * @param rootNode JSON representation of the data resource
     * @throws RestconfException if the PATCH operation cannot be fulfilled
     */
    void runPatchOperationOnDataResource(String uri, ObjectNode rootNode)
            throws RestconfException;

    /**
     * Retrieves the RESTCONF Root directory.
     *
     * @return the RESTCONF Root directory
     */
    String getRestconfRootPath();

    /**
     * Handles an Event Stream subscription request. This function creates
     * a worker thread to listen to events and writes to a ChunkedOutput,
     * which is passed in from the caller. (The worker thread blocks if
     * no events arrive.) The ChuckedOutput is a pipe to which this
     * function acts as the writer and the caller the reader.
     * <p>
     * If the Event Stream cannot be subscribed due to reasons such as
     * the nonexistence of the target stream or failure to allocate
     * worker thread to handle the request, a RestconfException exception
     * is raised. The proper HTTP error status code is enclosed in the
     * exception, so that the caller may return it to the RESTCONF client
     * to display.
     *
     * @param streamId ID of the RESTCONF stream to subscribe
     * @param output   A string data stream
     * @throws RestconfException if the Event Stream cannot be subscribed
     */
    void subscribeEventStream(String streamId, ChunkedOutput<String> output)
            throws RestconfException;
}
