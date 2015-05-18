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

    private Response fakeData(String which, String suffix) {
        String path = "local/" + which + "-" + suffix + ".json";
        String content = FakeUtils.slurp(path);
        if (content == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(content).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("dashboard/{suffix}")
    public Response dashboard(@PathParam("suffix") String suffix) {
        return fakeData("dashboard", suffix);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("bundle/{suffix}")
    public Response bundle(@PathParam("suffix") String suffix) {
        return fakeData("bundle", suffix);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("users/{suffix}")
    public Response users(@PathParam("suffix") String suffix) {
        return fakeData("users", suffix);
    }

}
