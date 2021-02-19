/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.kubevirtnode.api.KubevirtApiConfig;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigAdminService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static javax.ws.rs.core.Response.created;
import static org.onlab.util.Tools.readTreeFromStream;
import static org.onosproject.kubevirtnode.util.KubevirtNodeUtil.endpoint;

/**
 * Handles REST API call of kubernetes node config.
 */
@Path("api-config")
public class KubevirtApiConfigWebResource extends AbstractWebResource {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MESSAGE_CONFIG = "Received API config %s request";

    private static final String CREATE = "CREATE";
    private static final String UPDATE = "UPDATE";
    private static final String REMOVE = "REMOVE";
    private static final String QUERY = "QUERY";
    private static final String API_CONFIG = "apiConfig";

    private static final String ENDPOINT = "endpoint";
    private static final String ERROR_MESSAGE = " cannot be null";

    @Context
    private UriInfo uriInfo;

    /**
     * Creates a set of KubeVirt API config from the JSON input stream.
     *
     * @param input KubeVirt API configs JSON input stream
     * @return 201 CREATED if the JSON is correct, 400 BAD_REQUEST if the JSON
     * is malformed
     * @onos.rsModel KubevirtApiConfig
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createApiConfigs(InputStream input) {
        log.trace(String.format(MESSAGE_CONFIG, CREATE));

        KubevirtApiConfig config = readApiConfig(input);
        KubevirtApiConfigAdminService service = get(KubevirtApiConfigAdminService.class);

        if (config != null) {
            service.createApiConfig(config);
        }

        UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                .path(API_CONFIG);

        return created(locationBuilder.build()).build();
    }

    /**
     * Removes a KubeVirt API config.
     *
     * @param endpoint KubeVirt API endpoint
     * @return 204 NO_CONTENT, 400 BAD_REQUEST if the JSON is malformed
     */
    @DELETE
    @Path("{endpoint : .+}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteApiConfig(@PathParam("endpoint") String endpoint) {
        log.trace(String.format(MESSAGE_CONFIG, REMOVE));

        KubevirtApiConfigAdminService service = get(KubevirtApiConfigAdminService.class);
        KubevirtApiConfig existing = service.apiConfig();

        if (existing == null) {
            log.warn("There is no API configuration to delete");
            return Response.notModified().build();
        } else {
            if (endpoint.equals(endpoint(existing))) {
                service.removeApiConfig(endpoint);
            } else {
                log.warn("There is no API configuration to delete for endpoint {}", endpoint);
            }
        }

        return Response.noContent().build();
    }

    private KubevirtApiConfig readApiConfig(InputStream input) {
        KubevirtApiConfig config;
        try {
            JsonNode jsonTree = readTreeFromStream(mapper().enable(INDENT_OUTPUT), input);
            ObjectNode objectNode = jsonTree.deepCopy();
            config = codec(KubevirtApiConfig.class).decode(objectNode, this);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        return config;
    }
}
