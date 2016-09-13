/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.openstacknetworking.web;

/**
 * Handles Rest API call from Neutron ML2 plugin.
 */
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

@Path("subnets")
public class OpenstackSubnetWebResource extends AbstractWebResource {
    protected static final Logger log = LoggerFactory
            .getLogger(OpenstackSubnetWebResource.class);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSubnet(InputStream input) {
        return Response.status(Response.Status.OK).build();
    }


    @PUT
    @Path("{subnetId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateSubnet(@PathParam("subnetId") String id,
                                 final InputStream input) {
        return Response.status(Response.Status.OK).build();
    }

    @DELETE
    @Path("{subnetId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteSubnet(@PathParam("subnetId") String id) {
        return Response.noContent().build();
    }
}
