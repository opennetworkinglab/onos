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

import org.onosproject.app.ApplicationAdminService;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
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
import java.io.InputStream;
import java.util.Set;

/**
 * Manage inventory of applications.
 */
@Path("applications")
public class ApplicationsWebResource extends AbstractWebResource {

    /**
     * Get all installed applications.
     * Returns array of all installed applications.
     *
     * @onos.rsModel Applications
     * @return 200 OK
     */
    @GET
    public Response getApps() {
        ApplicationAdminService service = get(ApplicationAdminService.class);
        Set<Application> apps = service.getApplications();
        return ok(encodeArray(Application.class, "applications", apps)).build();
    }

    /**
     * Get application details.
     * Returns details of the specified application.
     * @onos.rsModel Application
     * @param name application name
     * @return 200 OK; 404; 401
     */
    @GET
    @Path("{name}")
    public Response getApp(@PathParam("name") String name) {
        ApplicationAdminService service = get(ApplicationAdminService.class);
        ApplicationId appId = service.getId(name);
        return response(service, appId);
    }

    /**
     * Install a new application.
     * Uploads application archive stream and optionally activates the
     * application.
     *
     * @param activate true to activate app also
     * @param stream   application archive stream
     * @return 200 OK; 404; 401
     */
    @POST
    @Consumes(MediaType.APPLICATION_OCTET_STREAM)
    @Produces(MediaType.APPLICATION_JSON)
    public Response installApp(@QueryParam("activate")
                               @DefaultValue("false") boolean activate,
                               InputStream stream) {
        ApplicationAdminService service = get(ApplicationAdminService.class);
        Application app = service.install(stream);
        if (activate) {
            service.activate(app.id());
        }
        return ok(codec(Application.class).encode(app, this)).build();
    }

    /**
     * Uninstall application.
     * Uninstalls the specified application deactivating it first if necessary.
     *
     * @param name application name
     * @return 200 OK; 404; 401
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{name}")
    public Response uninstallApp(@PathParam("name") String name) {
        ApplicationAdminService service = get(ApplicationAdminService.class);
        ApplicationId appId = service.getId(name);
        service.uninstall(appId);
        return Response.ok().build();
    }

    /**
     * Activate application.
     * Activates the specified application.
     *
     * @param name application name
     * @return 200 OK; 404; 401
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{name}/active")
    public Response activateApp(@PathParam("name") String name) {
        ApplicationAdminService service = get(ApplicationAdminService.class);
        ApplicationId appId = service.getId(name);
        service.activate(appId);
        return response(service, appId);
    }

    /**
     * De-activate application.
     * De-activates the specified application.
     *
     * @param name application name
     * @return 200 OK; 404; 401
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{name}/active")
    public Response deactivateApp(@PathParam("name") String name) {
        ApplicationAdminService service = get(ApplicationAdminService.class);
        ApplicationId appId = service.getId(name);
        service.deactivate(appId);
        return response(service, appId);
    }

    private Response response(ApplicationAdminService service, ApplicationId appId) {
        Application app = service.getApplication(appId);
        return ok(codec(Application.class).encode(app, this)).build();
    }

}
