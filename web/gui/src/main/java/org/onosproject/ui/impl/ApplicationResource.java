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
package org.onosproject.ui.impl;

import org.glassfish.jersey.media.multipart.FormDataParam;
import org.onlab.rest.BaseResource;
import org.onosproject.app.ApplicationAdminService;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;

import javax.ws.rs.Consumes;
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
import java.util.Objects;

/**
 * Application upload resource.
 */
@Path("applications")
public class ApplicationResource extends BaseResource {

    static String lastInstalledAppName = null;


    @Path("upload")
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response upload(@QueryParam("activate") @DefaultValue("false") String activate,
                           @FormDataParam("file") InputStream stream) throws IOException {
        ApplicationAdminService service = get(ApplicationAdminService.class);
        Application app = service.install(stream);
        lastInstalledAppName = app.id().name();
        if (Objects.equals(activate, "true")) {
            service.activate(app.id());
        }
        return Response.ok().build();
    }

    @Path("{name}/icon")
    @GET
    @Produces("image/png")
    public Response getIcon(@PathParam("name") String name) throws IOException {
        ApplicationAdminService service = get(ApplicationAdminService.class);
        ApplicationId appId = service.getId(name);
        Application app = service.getApplication(appId);
        return Response.ok(app.icon()).build();
    }
}
