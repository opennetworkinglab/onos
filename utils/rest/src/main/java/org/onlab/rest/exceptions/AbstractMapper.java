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

package org.onlab.rest.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Base exception mapper implementation.
 */
public abstract class AbstractMapper<E extends Throwable> implements ExceptionMapper<E> {

    /**
     * Holds the current exception for use in subclasses.
     */
    protected Throwable error;

    /**
     * Returns the response status to be given when the exception occurs.
     *
     * @return response status
     */
    protected abstract Response.Status responseStatus();

    @Override
    public Response toResponse(E exception) {
        error = exception;
        return response(responseStatus(), exception).build();
    }

    /**
     * Produces a response builder primed with the supplied status code
     * and JSON entity with the status code and exception message.
     *
     * @param status    response status
     * @param exception exception to encode
     * @return response builder
     */
    protected Response.ResponseBuilder response(Response.Status status,
                                                Throwable exception) {
        error = exception;
        ObjectMapper mapper = new ObjectMapper();
        String message = messageFrom(exception);
        ObjectNode result = mapper.createObjectNode()
                .put("code", status.getStatusCode())
                .put("message", message);
        return Response.status(status).entity(result.toString());
    }

    /**
     * Produces a response message from the supplied exception. Either it will
     * use the exception message, if there is one, or it will use the top
     * stack-frame message.
     *
     * @param exception exception from which to produce a message
     * @return response message
     */
    protected String messageFrom(Throwable exception) {
        if (isNullOrEmpty(exception.getMessage())) {
            StackTraceElement[] trace = exception.getStackTrace();
            return trace.length == 0 ? "Unknown error" : trace[0].toString();
        }
        return exception.getMessage();
    }

}
