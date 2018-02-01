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
package org.onosproject.rest.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.ui.UiPreferencesService;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.onlab.util.Tools.nullIsNotFound;

/**
 * Manage user preferences.
 */
@Path("ui/preferences")
public class UiPreferencesWebResource extends AbstractWebResource {

    /**
     * Gets all user preferences.
     *
     * @return 200 OK with user preferences JSON
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response download() {
        UiPreferencesService service = get(UiPreferencesService.class);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        service.getUserNames().forEach(user -> {
            ObjectNode prefs = mapper.createObjectNode();
            root.set(user, prefs);
            service.getPreferences(user).forEach(prefs::set);
        });
        return ok(root).build();
    }

    /**
     * Gets user preferences for the given user.
     *
     * @param user user name
     * @return 200 OK with user preferences JSON
     */
    @GET
    @Path("{user}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response download(@PathParam("user") String user) {
        UiPreferencesService service = get(UiPreferencesService.class);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode prefs = mapper.createObjectNode();
        service.getPreferences(user).forEach(prefs::set);
        return ok(prefs).build();
    }

    /**
     * Gets the specified user preferences for the given user.
     *
     * @param user user name
     * @param pref preferences name
     * @return 200 OK with user preferences JSON
     */
    @GET
    @Path("{user}/{pref}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response download(@PathParam("user") String user,
                             @PathParam("pref") String pref) {
        UiPreferencesService service = get(UiPreferencesService.class);
        return ok(nullIsNotFound(service.getPreference(user, pref), "No such preference")).build();
    }

    /**
     * Gets the specified user preferences for the given user.
     *
     * @param user    user name
     * @param pref    preferences name
     * @param request preferences JSON
     * @return 200 OK
     * @throws IOException if given JSON is invalid
     */
    @PUT
    @Path("{user}/{pref}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response upload(@PathParam("user") String user,
                           @PathParam("pref") String pref,
                           String request) throws IOException {
        UiPreferencesService service = get(UiPreferencesService.class);
        ObjectNode json = (ObjectNode) mapper().readTree(request);
        service.setPreference(user, pref, json);
        return Response.ok().build();
    }

    /**
     * Removes the specified user preferences for the given user.
     *
     * @param user user name
     * @param pref preferences name
     * @return 204 no content
     */
    @DELETE
    @Path("{user}/{pref}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response remove(@PathParam("user") String user,
                           @PathParam("pref") String pref) {
        UiPreferencesService service = get(UiPreferencesService.class);
        service.setPreference(user, pref, null);
        return Response.noContent().build();
    }

}
