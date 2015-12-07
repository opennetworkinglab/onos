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
package org.onosproject.cordvtn.rest;

import org.onosproject.cordvtn.CordVtnService;
import org.onosproject.cordvtn.CordServiceId;
import org.onosproject.rest.AbstractWebResource;

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
 * Manages service dependency.
 */
@Path("service-dependency")
public class ServiceDependencyWebResource extends AbstractWebResource {

    private final CordVtnService service = get(CordVtnService.class);

    /**
     * Creates service dependencies.
     *
     * @param tServiceId tenant service id
     * @param pServiceId provider service id
     * @return 200 OK
     */
    @POST
    @Path("{tenantServiceId}/{providerServiceId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createServiceDependency(@PathParam("tenantServiceId") String tServiceId,
                                            @PathParam("providerServiceId") String pServiceId) {
        service.createServiceDependency(CordServiceId.of(tServiceId), CordServiceId.of(pServiceId));
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Removes service dependencies.
     *
     * @param serviceId service id
     * @return 200 OK, or 400 Bad Request
     */
    @DELETE
    @Path("{serviceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeServiceDependency(@PathParam("serviceId") String serviceId) {
        service.removeServiceDependency(CordServiceId.of(serviceId));
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Updates service dependencies.
     *
     * @param serviceId service id
     * @param stream input JSON
     * @return 200 OK, or 400 Bad Request
     */
    @PUT
    @Path("{serviceId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateServiceDependency(@PathParam("serviceId") String serviceId,
                                            InputStream stream) {
        // TODO define input stream
        return Response.status(Response.Status.OK).build();
    }
}
