/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.rest.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Base exception mapper implementation.
 */
public abstract class AbstractMapper<E extends Throwable> implements ExceptionMapper<E> {

    /**
     * Returns the response status to be given when the exception occurs.
     *
     * @return response status
     */
    protected abstract Response.Status responseStatus();

    @Override
    public Response toResponse(E exception) {
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
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode()
                .put("code", status.getStatusCode())
                .put("message", exception.getMessage());
        return Response.status(status).entity(result.toString());
    }
}
