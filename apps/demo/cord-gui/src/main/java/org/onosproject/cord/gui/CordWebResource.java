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
import javax.ws.rs.core.Response;

/**
 * Web resource to use as the GUI back-end and as a proxy to XOS REST API.
 */
@Path("")
public class CordWebResource {

    @GET
    @Path("hello")
    public Response hello() {
        return Response.ok("Hello World").build();
    }

    @GET
    @Path("fake")
    public Response fake() {
        return Response.ok(FakeUtils.slurp("sample.json")).build();
    }
}
