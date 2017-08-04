/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.ui.impl;

import org.onosproject.rest.AbstractInjectionResource;
import org.onosproject.ui.UiExtension;
import org.onosproject.ui.UiExtensionService;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;
import static javax.ws.rs.core.MediaType.TEXT_HTML;

/**
 * Resource for serving the dynamically composed onos.js.
 */
@Path("/")
public class MainViewResource extends AbstractInjectionResource {

    static final String CONTENT_TYPE = "Content-Type";
    static final String STYLESHEET = "text/css";
    static final String SCRIPT = "text/javascript";

    @Path("{view}/{resource}")
    @GET
    public Response getViewResource(@PathParam("view") String viewId,
                                    @PathParam("resource") String resource) throws IOException {
        UiExtensionService service = get(UiExtensionService.class);
        UiExtension extension = service.getViewExtension(viewId);
        return extension != null ?
                Response.ok(extension.resource(viewId, resource))
                        .header(CONTENT_TYPE, contentType(resource)).build() :
                Response.status(Response.Status.NOT_FOUND).build();
    }

    static String contentType(String resource) {
        return resource.endsWith(".html") ? TEXT_HTML :
                resource.endsWith(".css") ? STYLESHEET :
                        resource.endsWith(".js") ? SCRIPT :
                                APPLICATION_OCTET_STREAM;
    }

}
