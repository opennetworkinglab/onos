/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.simplefabric.web;

import org.onosproject.rest.AbstractWebResource;
import org.onosproject.simplefabric.api.SimpleFabricService;

import java.io.ByteArrayOutputStream;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * Manage SIMPLE_FABRIC Status.
 */
@Path("")
public class SimpleFabricWebResource extends AbstractWebResource {

    /**
     * SIMPLE_FABRIC Show Status; dummy for now.
     *
     * @return 200 OK
     */
    @GET
    @Path("status")
    public Response queryStatus() {
        return Response.ok("ok").build();
    }

    /**
     * SIMPLE_FABRIC Show Configurations.
     *
     * @return 200 OK
     */
    @GET
    @Path("show")
    public Response queryShow() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        get(SimpleFabricService.class).dumpToStream("show", outputStream);
        return Response.ok(outputStream.toString()).build();
    }

    /**
     * SIMPLE_FABRIC Intents Infos.
     *
     * @return 200 OK
     */
    @GET
    @Path("intents")
    public Response queryIntents() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        get(SimpleFabricService.class).dumpToStream("intents", outputStream);
        return Response.ok(outputStream.toString()).build();
    }

    /**
     * SIMPLE_FABRIC Reactive Intents Infos.
     *
     * @return 200 OK
     */
    @GET
    @Path("reactive-intents")
    public Response queryReactiveIntents() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        get(SimpleFabricService.class).dumpToStream("reactive-intents", outputStream);
        return Response.ok(outputStream.toString()).build();
    }

    /**
     * Trigger SimpleFabric Service Refresh.
     *
     * @return 204 No Content
     */
    @PUT
    @Path("refresh")
    public Response triggerRefresh() {
        get(SimpleFabricService.class).triggerRefresh();
        return Response.status(204).build();
    }

    /**
     * Trigger SimpleFabric Service Flush Reactive Intents.
     *
     * @return 204 No Content
     */
    @PUT
    @Path("flush")
    public Response triggerFlush() {
        get(SimpleFabricService.class).triggerFlush();
        return Response.status(204).build();
    }

}

