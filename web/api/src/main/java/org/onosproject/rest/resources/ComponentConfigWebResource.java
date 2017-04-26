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
package org.onosproject.rest.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

import static org.onlab.util.Tools.nullIsNotFound;

/**
 * Manage component configurations.
 */
@Path("configuration")
public class ComponentConfigWebResource extends AbstractWebResource {
    private static final int  MULTI_STATUS_RESPONE = 207;

    /**
     * Gets all component configurations.
     * Returns collection of all registered component configurations.
     *
     * @return 200 OK with a collection of component configurations
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getComponentConfigs() {
        ComponentConfigService service = get(ComponentConfigService.class);
        Set<String> components = service.getComponentNames();
        ObjectNode root = mapper().createObjectNode();
        components.forEach(c -> encodeConfigs(c, service.getProperties(c), root));
        return ok(root).build();
    }

    /**
     * Gets configuration of the specified component.
     *
     * @param component component name
     * @return 200 OK with a collection of component configurations
     */
    @GET
    @Path("{component}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getComponentConfigs(@PathParam("component") String component) {
        ComponentConfigService service = get(ComponentConfigService.class);
        ObjectNode root = mapper().createObjectNode();
        encodeConfigs(component, nullIsNotFound(service.getProperties(component),
                                                "No such component"), root);
        return ok(root).build();
    }

    // Encodes the specified properties as an object in the given node.
    private void encodeConfigs(String component, Set<ConfigProperty> props,
                               ObjectNode node) {
        ObjectNode compNode = mapper().createObjectNode();
        node.set(component, compNode);
        props.forEach(p -> compNode.put(p.name(), p.value()));
    }

    /**
     * Selectively sets configuration properties.
     * Sets only the properties present in the JSON request.
     *
     * @param component component name
     * @param preset preset the property if true
     * @param request   JSON configuration
     * @return 200 OK
     * @throws IOException to signify bad request
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("{component}")
    public Response setConfigs(@PathParam("component") String component,
                               @DefaultValue("false") @QueryParam("preset") boolean preset,
                               InputStream request) throws IOException {
        ComponentConfigService service = get(ComponentConfigService.class);
        ObjectNode props = (ObjectNode) mapper().readTree(request);
        List<String> errorMsgs = new ArrayList<String>();
        if (preset) {
                props.fieldNames().forEachRemaining(k -> {
                    try {
                        service.preSetProperty(component, k, props.path(k).asText());
                    } catch (IllegalArgumentException e) {
                        errorMsgs.add(e.getMessage());
                    }
                });
        } else {
                props.fieldNames().forEachRemaining(k -> {
                    try {
                        service.setProperty(component, k, props.path(k).asText());
                    } catch (IllegalArgumentException e) {
                        errorMsgs.add(e.getMessage());
                    }
                });
        }
        if (!errorMsgs.isEmpty()) {
            return Response.status(MULTI_STATUS_RESPONE).entity(produceErrorJson(errorMsgs)).build();
        }
        return Response.ok().build();
    }

    private ObjectNode produceErrorJson(List<String> errorMsgs) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode result = mapper.createObjectNode().put("code", 207).putPOJO("message", errorMsgs);
        return result;
    }

    /**
     * Selectively clears configuration properties.
     * Clears only the properties present in the JSON request.
     *
     * @param component component name
     * @param request   JSON configuration
     * @return 204 NO CONTENT
     * @throws IOException to signify bad request
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("{component}")
    public Response unsetConfigs(@PathParam("component") String component,
                                 InputStream request) throws IOException {
        ComponentConfigService service = get(ComponentConfigService.class);
        ObjectNode props = (ObjectNode) mapper().readTree(request);
        props.fieldNames().forEachRemaining(k -> service.unsetProperty(component, k));
        return Response.noContent().build();
    }
}
