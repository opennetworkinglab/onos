/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.cordvtn.rest;

import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * Dummy Neutron ML2 mechanism driver.
 * It just returns OK for networks resource requests.
 */
@Path("networks")
public class NeutronMl2NetworksWebResource extends AbstractWebResource {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private static final String NETWORKS_MESSAGE = "Received networks %s";

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createNetwork(InputStream input) {
        log.trace(String.format(NETWORKS_MESSAGE, "create"));
        return Response.status(Response.Status.OK).build();
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateNetwork(@PathParam("id") String id, InputStream input) {
        log.trace(String.format(NETWORKS_MESSAGE, "update"));
        return Response.status(Response.Status.OK).build();
    }

    @DELETE
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteNetwork(@PathParam("id") String id) {
        log.trace(String.format(NETWORKS_MESSAGE, "delete"));
        return Response.status(Response.Status.OK).build();
    }
}
