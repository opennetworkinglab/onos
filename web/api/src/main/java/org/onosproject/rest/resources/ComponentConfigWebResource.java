/*
 * Copyright 2015 Open Networking Laboratory
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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static org.onlab.util.Tools.nullIsNotFound;

/**
 * REST resource for cluster-wide component configuration.
 */
@Path("configuration")
public class ComponentConfigWebResource extends AbstractWebResource {

    @GET
    public Response getComponentConfigs() {
        ComponentConfigService service = get(ComponentConfigService.class);
        Set<String> components = service.getComponentNames();
        ObjectNode root = mapper().createObjectNode();
        components.forEach(c -> encodeConfigs(c, service.getProperties(c), root));
        return ok(root).build();
    }

    @GET
    @Path("{component}")
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

    @POST
    @Path("{component}")
    public Response setComponentConfigs(@PathParam("component") String component,
                                        InputStream request) throws IOException {
        ComponentConfigService service = get(ComponentConfigService.class);
        ObjectNode props = (ObjectNode) mapper().readTree(request);
        props.fieldNames().forEachRemaining(k -> service.setProperty(component, k,
                                                                     props.path(k).asText()));
        return Response.noContent().build();
    }

    @DELETE
    @Path("{component}")
    public Response unsetComponentConfigs(@PathParam("component") String component,
                                          InputStream request) throws IOException {
        ComponentConfigService service = get(ComponentConfigService.class);
        ObjectNode props = (ObjectNode) mapper().readTree(request);
        props.fieldNames().forEachRemaining(k -> service.unsetProperty(component, k));
        return Response.noContent().build();
    }
}
