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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.rest.AbstractInjectionResource;
import org.onosproject.rest.ApiDocService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.URI;
import java.net.URISyntaxException;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.io.ByteStreams.toByteArray;
import static javax.ws.rs.core.MediaType.*;
import static javax.ws.rs.core.Response.temporaryRedirect;
import static org.onlab.util.Tools.nullIsNotFound;

/**
 * REST API documentation.
 */
@Path("docs")
public class ApiDocResource extends AbstractInjectionResource {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String STYLESHEET = "text/css";
    private static final String SCRIPT = "text/javascript";
    private static final String DOCS = "/docs/";

    private static final String INJECT_START = "<!-- {API-START} -->";
    private static final String INJECT_END = "<!-- {API-END} -->";

    @Context
    private UriInfo uriInfo;

    /**
     * Get all registered REST API docs.
     * Returns array of all registered API docs.
     *
     * @return 200 OK
     */
    @GET
    @Path("apis")
    public Response getApiList() {
        ObjectNode root = mapper().createObjectNode();
        ArrayNode apis = newArray(root, "apis");
        get(ApiDocService.class).getDocProviders().forEach(p -> apis.add(p.name()));
        return ok(root.toString()).build();
    }

    /**
     * Get Swagger UI JSON.
     *
     * @param key REST API web context
     * @return 200 OK
     */
    @GET
    @Path("apis/{key: .*?}/swagger.json")
    public Response getApi(@PathParam("key") String key) {
        String k = key.startsWith("/") ? key : "/" + key;
        InputStream stream = nullIsNotFound(get(ApiDocService.class).getDocProvider(k),
                                            "REST API not found for " + k).docs();
        return ok(nullIsNotFound(stream, "REST API docs not found for " + k))
                .header(CONTENT_TYPE, APPLICATION_JSON).build();
    }

    /**
     * Get REST API model schema.
     *
     * @param key REST API web context
     * @return 200 OK
     */
    @GET
    @Path("apis/{key: .*?}/model.json")
    public Response getApiModel(@PathParam("name") String key) {
        String k = key.startsWith("/") ? key : "/" + key;
        InputStream stream = nullIsNotFound(get(ApiDocService.class).getDocProvider(k),
                                            "REST API not found for " + k).model();
        return ok(nullIsNotFound(stream, "REST API model not found for " + k))
                .header(CONTENT_TYPE, APPLICATION_JSON).build();
    }

    /**
     * Get Swagger UI main index page.
     *
     * @return 200 OK
     * @throws IOException if unable to get index resource
     * @throws URISyntaxException if unable to create redirect URI
     */
    @GET
    public Response getDefault() throws IOException, URISyntaxException {
        return uriInfo.getPath().endsWith("/") ? getIndex() :
                temporaryRedirect(new URI(uriInfo.getPath() + "/")).build();
    }

    /**
     * Get Swagger UI main index page.
     *
     * @return 200 OK
     * @throws IOException if unable to get index resource
     */
    @GET
    @Path("index.html")
    public Response getIndex() throws IOException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(DOCS + "index.html");
        nullIsNotFound(stream, "index.html not found");

        String index = new String(toByteArray(stream));

        int p1s = split(index, 0, INJECT_START);
        int p1e = split(index, p1s, INJECT_END);
        int p2s = split(index, p1e, null);

        StreamEnumeration streams =
                new StreamEnumeration(of(stream(index, 0, p1s),
                                         includeOptions(get(ApiDocService.class)),
                                         stream(index, p1e, p2s)));

        return ok(new SequenceInputStream(streams))
                .header(CONTENT_TYPE, TEXT_HTML).build();
    }

    private InputStream includeOptions(ApiDocService service) {
        StringBuilder sb = new StringBuilder();
        service.getDocProviders().forEach(p -> {
            sb.append("<option value=\"").append(p.key()).append("\"")
                    .append(p.key().equals("/onos/v1") ? " selected>" : ">")
                    .append(p.name())
                    .append("</option>");
        });
        return new ByteArrayInputStream(sb.toString().getBytes());
    }

    /**
     * Get Swagger UI resource.
     *
     * @param resource path of the resource
     * @return 200 OK
     * @throws IOException if unable to get named resource
     */
    @GET
    @Path("{resource: .*}")
    public Response getResource(@PathParam("resource") String resource) throws IOException {
        if (resource != null && resource.equals("")) {
            return getIndex();
        }
        InputStream stream = getClass().getClassLoader().getResourceAsStream(DOCS + resource);
        return ok(nullIsNotFound(stream, resource + " not found"))
                .header(CONTENT_TYPE, contentType(resource)).build();
    }

    static String contentType(String resource) {
        return resource.endsWith(".html") ? TEXT_HTML :
                resource.endsWith(".css") ? STYLESHEET :
                        resource.endsWith(".js") ? SCRIPT :
                                APPLICATION_OCTET_STREAM;
    }
}
