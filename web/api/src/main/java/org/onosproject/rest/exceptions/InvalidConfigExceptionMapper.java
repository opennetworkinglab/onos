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

package org.onosproject.rest.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.rest.exceptions.AbstractMapper;
import org.onosproject.net.config.InvalidConfigException;
import org.onosproject.net.config.InvalidFieldException;

import javax.ws.rs.core.Response;

/**
 * Maps InvalidConfigException to JSON output.
 */
public class InvalidConfigExceptionMapper extends AbstractMapper<InvalidConfigException> {

    @Override
    protected Response.Status responseStatus() {
        return Response.Status.BAD_REQUEST;
    }

    @Override
    protected Response.ResponseBuilder response(Response.Status status, Throwable exception) {
        error = exception;

        InvalidConfigException ex = (InvalidConfigException) exception;

        ObjectMapper mapper = new ObjectMapper();
        String message = messageFrom(exception);
        ObjectNode result = mapper.createObjectNode()
                .put("code", status.getStatusCode())
                .put("message", message)
                .put("subjectKey", ex.subjectKey())
                .put("subject", ex.subject())
                .put("configKey", ex.configKey());

        if (ex.getCause() instanceof InvalidFieldException) {
            InvalidFieldException fieldException = (InvalidFieldException) ex.getCause();
            result.put("field", fieldException.field())
                    .put("reason", fieldException.reason());
        }

        return Response.status(status).entity(result.toString());
    }
}
