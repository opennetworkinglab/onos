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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.app.ApplicationAdminService;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
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
import java.net.URL;
import java.util.Set;

import static org.onlab.util.Tools.nullIsNotFound;

/**
 * Manage inventory of applications.
 */
@Path("applications")
public class ApplicationsWebResource extends AbstractWebResource {

    private static final String APP_ID_NOT_FOUND = "Application ID is not found";
    private static final String APP_NOT_FOUND = "Application is not found";

    private static final String URL = "url";
    private static final String ACTIVATE = "activate";

    /**
     * Get all installed applications.
     * Returns array of all installed applications.
     *
     * @return 200 OK
     * @onos.rsModel Applications
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
     *
     * @param name application name
     * @return 200 OK; 404; 401
     * @onos.rsModel Application
     */
    @GET
    @Path("{name}")
    public Response getApp(@PathParam("name") String name) {
        ApplicationAdminService service = get(ApplicationAdminService.class);
        ApplicationId appId = nullIsNotFound(service.getId(name), APP_NOT_FOUND);
        return response(service, appId);
    }

    /**
     * Install a new application.
     * Uploads application archive stream and optionally activates the
     * application.

     * @param raw   json object containing location (url) of application oar
     * @return 200 OK; 404; 401
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response installApp(InputStream raw) {
        Application app;
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(raw);
            URL url = new URL(jsonTree.get(URL).asText());
            boolean activate = false;
            if (jsonTree.has(ACTIVATE)) {
              activate = jsonTree.get(ACTIVATE).asBoolean();
            }

            ApplicationAdminService service = get(ApplicationAdminService.class);
            app = service.install(url.openStream());
            if (activate) {
                service.activate(app.id());
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
        return ok(codec(Application.class).encode(app, this)).build();
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
     * @return 204 NO CONTENT
     */
    @DELETE
    @Path("{name}")
    public Response uninstallApp(@PathParam("name") String name) {
        ApplicationAdminService service = get(ApplicationAdminService.class);
        ApplicationId appId = service.getId(name);
        service.uninstall(appId);
        return Response.noContent().build();
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
        ApplicationId appId = nullIsNotFound(service.getId(name), APP_NOT_FOUND);
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

    /**
     * Registers an on or off platform application.
     *
     * @param name application name
     * @return 200 OK; 404; 401
     * @onos.rsModel ApplicationId
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{name}/register")
    public Response registerAppId(@PathParam("name") String name) {
        CoreService service = get(CoreService.class);
        ApplicationId appId = service.registerApplication(name);
        return response(appId);
    }

    /**
     * Gets applicationId entry by either id or name.
     *
     * @param id   id of application
     * @param name name of application
     * @return 200 OK; 404; 401
     * @onos.rsModel ApplicationId
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("ids/entry")
    public Response getAppIdByName(@QueryParam("id") String id,
                                   @QueryParam("name") String name) {
        CoreService service = get(CoreService.class);
        ApplicationId appId = null;
        if (id != null) {
            appId = service.getAppId(Short.valueOf(id));
        } else if (name != null) {
            appId = service.getAppId(name);
        }
        return response(appId);
    }

    /**
     * Gets a collection of application ids.
     * Returns array of all registered application ids.
     *
     * @return 200 OK; 404; 401
     * @onos.rsModel ApplicationIds
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("ids")
    public Response getAppIds() {
        CoreService service = get(CoreService.class);
        Set<ApplicationId> appIds = service.getAppIds();
        return ok(encodeArray(ApplicationId.class, "applicationIds", appIds)).build();
    }

    private Response response(ApplicationAdminService service, ApplicationId appId) {
        Application app = nullIsNotFound(service.getApplication(appId), APP_NOT_FOUND);
        return ok(codec(Application.class).encode(app, this)).build();
    }

    private Response response(ApplicationId appId) {
        ApplicationId checkedAppId = nullIsNotFound(appId, APP_ID_NOT_FOUND);
        return ok(codec(ApplicationId.class).encode(checkedAppId, this)).build();
    }
}
