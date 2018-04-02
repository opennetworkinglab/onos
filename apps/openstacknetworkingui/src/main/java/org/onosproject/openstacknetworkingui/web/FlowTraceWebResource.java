/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.openstacknetworkingui.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.openstacknetworkingui.OpenstackNetworkingUiService;
import org.onosproject.rest.AbstractWebResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;

import static javax.ws.rs.core.Response.status;
import static org.onlab.util.Tools.readTreeFromStream;

/**
 * Handles REST API from monitoring server.
 */

@Path("result")
public class FlowTraceWebResource extends AbstractWebResource {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private final OpenstackNetworkingUiService uiService =
            DefaultServiceDirectory.getService(OpenstackNetworkingUiService.class);

    private static final String FLOW_TRACE_RESULT = "flowTraceResult";

    @Context
    private UriInfo uriInfo;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response flowTraceResponse(InputStream inputStream) throws IOException {
        try {
            JsonNode jsonNode = readTreeFromStream(mapper().enable(SerializationFeature.INDENT_OUTPUT), inputStream);
            ObjectNode objectNode = jsonNode.deepCopy();

            log.debug("FlowTraceResponse: {}", jsonNode.toString());

            uiService.sendMessage(FLOW_TRACE_RESULT, objectNode);

        } catch (IOException e) {
            log.error("Exception occured because of {}", e.toString());
        }

        return status(Response.Status.OK).build();
    }

}
