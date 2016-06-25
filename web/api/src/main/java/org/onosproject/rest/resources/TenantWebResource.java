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

package org.onosproject.rest.resources;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.incubator.net.virtual.TenantId;
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminService;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;

/**
 * Query and manage tenants of virtual networks.
 */
@Path("tenants")
public class TenantWebResource extends AbstractWebResource {

    private static final String MISSING_TENANTID = "Missing tenant identifier";
    private static final String TENANTID_NOT_FOUND = "Tenant identifier not found";
    private static final String INVALID_TENANTID = "Invalid tenant identifier ";

    @Context
    private UriInfo uriInfo;

    private final VirtualNetworkAdminService vnetAdminService = get(VirtualNetworkAdminService.class);

    /**
     * Returns all tenant identifiers.
     *
     * @return 200 OK with set of tenant identifiers
     * @onos.rsModel TenantIds
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVirtualNetworkTenantIds() {
        Iterable<TenantId> tenantIds = vnetAdminService.getTenantIds();
        return ok(encodeArray(TenantId.class, "tenants", tenantIds)).build();
    }

    /**
     * Creates a tenant with the given tenant identifier.
     *
     * @param stream TenantId JSON stream
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel TenantId
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addTenantId(InputStream stream) {
        try {
            final TenantId tid = getTenantIdFromJsonStream(stream);
            vnetAdminService.registerTenantId(tid);
            final TenantId resultTid = getExistingTenantId(vnetAdminService, tid);
            UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                    .path("tenants")
                    .path(resultTid.id());
            return Response
                    .created(locationBuilder.build())
                    .build();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Removes the specified tenant with the specified tenant identifier.
     *
     * @param tenantId tenant identifier
     * @return 204 NO CONTENT
     */
    @DELETE
    @Path("{tenantId}")
    public Response removeTenantId(@PathParam("tenantId") String tenantId) {
        final TenantId tid = TenantId.tenantId(tenantId);
        final TenantId existingTid = getExistingTenantId(vnetAdminService, tid);
        vnetAdminService.unregisterTenantId(existingTid);
        return Response.noContent().build();
    }

    /**
     * Gets the tenant identifier from the JSON stream.
     *
     * @param stream TenantId JSON stream
     * @return TenantId
     * @throws IOException if unable to parse the request
     */
    private TenantId getTenantIdFromJsonStream(InputStream stream) throws IOException {
        ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
        JsonNode specifiedTenantId = jsonTree.get("id");

        if (specifiedTenantId == null) {
            throw new IllegalArgumentException(MISSING_TENANTID);
        }
        return TenantId.tenantId(specifiedTenantId.asText());
    }

    /**
     * Get the matching tenant identifier from existing tenant identifiers in system.
     *
     * @param vnetAdminSvc virtual network administration service
     * @param tidIn        tenant identifier
     * @return TenantId
     */
    protected static TenantId getExistingTenantId(VirtualNetworkAdminService vnetAdminSvc,
                                                TenantId tidIn) {
        return vnetAdminSvc
                .getTenantIds()
                .stream()
                .filter(tenantId -> tenantId.equals(tidIn))
                .findFirst()
                .orElseThrow(() -> new ItemNotFoundException(TENANTID_NOT_FOUND));
    }
}
