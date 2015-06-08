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

package org.onosproject.cord.gui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Web resource to use as the GUI back-end and as a proxy to XOS REST API.
 */
@Path("")
public class CordWebResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("login/{email}")
    public Response login(@PathParam("email") String email) {
        return Response.ok(CordModelCache.INSTANCE.jsonLogin(email)).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("dashboard")
    public Response dashboard() {
        return Response.ok(CordModelCache.INSTANCE.jsonDashboard()).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("bundle")
    public Response bundle() {
        return Response.ok(CordModelCache.INSTANCE.jsonBundle()).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("users")
    public Response users() {
        return Response.ok(CordModelCache.INSTANCE.jsonUsers()).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("logout")
    public Response logout() {
        return Response.ok(CordModelCache.INSTANCE.jsonLogout()).build();
    }

    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("bundle/{id}")
    public Response bundle(@PathParam("id") String bundleId) {
        CordModelCache.INSTANCE.setCurrentBundle(bundleId);
        return bundle();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("users/{id}/apply/{func}/{param}/{value}")
    public Response bundle(@PathParam("id") String userId,
                           @PathParam("func") String funcId,
                           @PathParam("param") String param,
                           @PathParam("value") String value) {
        CordModelCache.INSTANCE.applyPerUserParam(userId, funcId, param, value);
        return users();
    }
}
