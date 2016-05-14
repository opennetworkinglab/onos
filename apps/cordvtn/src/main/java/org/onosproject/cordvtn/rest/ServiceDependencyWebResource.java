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
package org.onosproject.cordvtn.rest;

import org.onosproject.cordvtn.api.CordVtnService;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.xosclient.api.VtnServiceId;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Manages service dependency.
 */
@Path("service-dependency")
public class ServiceDependencyWebResource extends AbstractWebResource {

    private final CordVtnService service = get(CordVtnService.class);
    private static final String BIDIRECTION = "b";

    /**
     * Creates service dependencies with unidirectional access between the services.
     *
     * @param tServiceId tenant service id
     * @param pServiceId provider service id
     * @return 200 OK
     */
    @POST
    @Path("{tenantServiceId}/{providerServiceId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createServiceDependency(@PathParam("tenantServiceId") String tServiceId,
                                            @PathParam("providerServiceId") String pServiceId) {
        service.createServiceDependency(VtnServiceId.of(tServiceId),
                                        VtnServiceId.of(pServiceId),
                                        false);
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Creates service dependencies with an access type extension between the services.
     *
     * @param tServiceId tenant service id
     * @param pServiceId provider service id
     * @param direction b for bidirectional access, otherwise unidirectional access
     * @return 200 OK
     */
    @POST
    @Path("{tenantServiceId}/{providerServiceId}/{direction}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response createServiceDependency(@PathParam("tenantServiceId") String tServiceId,
                                            @PathParam("providerServiceId") String pServiceId,
                                            @PathParam("direction") String direction) {
        service.createServiceDependency(VtnServiceId.of(tServiceId),
                                        VtnServiceId.of(pServiceId),
                                        direction.equals(BIDIRECTION));
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Removes service dependencies.
     *
     * @param tServiceId tenant service id
     * @param pServiceId provider service id
     * @return 204 NO CONTENT
     */
    @DELETE
    @Path("{tenantServiceId}/{providerServiceId}")
    public Response removeServiceDependency(@PathParam("tenantServiceId") String tServiceId,
                                            @PathParam("providerServiceId") String pServiceId) {
        service.removeServiceDependency(VtnServiceId.of(tServiceId), VtnServiceId.of(pServiceId));
        return Response.noContent().build();
    }
}
