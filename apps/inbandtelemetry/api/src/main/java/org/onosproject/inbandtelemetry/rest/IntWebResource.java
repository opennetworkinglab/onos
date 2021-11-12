/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.inbandtelemetry.rest;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.onosproject.inbandtelemetry.api.IntIntent;
import org.onosproject.inbandtelemetry.api.IntIntentId;
import org.onosproject.inbandtelemetry.api.IntService;
import org.onosproject.rest.AbstractWebResource;


import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.GET;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.core.Context;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.onlab.util.Tools.nullIsNotFound;
import static org.onlab.util.Tools.readTreeFromStream;


/**
 * Query and program intIntents.
 */
@Path("intIntent")
public class IntWebResource extends AbstractWebResource {
    @Context
    private UriInfo uriInfo;

    private static final String INTS = "IntIntents";
    private static final String INT = "IntIntent";
    static final String ID = "id";
    private static final String INT_NOT_FOUND = "IntIntent is not found for ";

    private final ObjectNode root = mapper().createObjectNode();

    /**
     * Gets all IntIntents. Returns array of all IntIntents in the system.
     * @return 200 OK with a collection of IntIntents
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIntents() {
        ArrayNode intsNode = root.putArray(INTS);
        IntService service = get(IntService.class);
        Map<IntIntentId, IntIntent> intIntents = service.getIntIntents();
        if (!intIntents.isEmpty()) {
            intIntents.entrySet().forEach(intIntentEntry -> {
                intsNode.add(codec(IntIntent.class).encode(intIntentEntry.getValue(), this)
                        .put(ID, intIntentEntry.getKey().id()));
                }
            );
        }
        return ok(root).build();
    }

    /**
     * Get an IntIntent. Returns an IntIntent in the system.
     * @param  id IntIntentId
     * @return 200 OK with a IntIntent
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    public Response getIntent(@PathParam("id") String id) {
        ArrayNode intsNode = root.putArray(INT);
        IntService service = get(IntService.class);
        final IntIntent intIntent = nullIsNotFound(service.getIntIntent(IntIntentId.valueOf(Long.parseLong(id))),
                INT_NOT_FOUND + id);
        intsNode.add(codec(IntIntent.class).encode(intIntent, this));
        return ok(root).build();
    }

    /**
     * Creates new IntIntent. Creates and installs a new IntIntent.
     *
     * @param stream IntIntent JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel Int
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createIntent(InputStream stream) {
        IntService service = get(IntService.class);
        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), stream);
            IntIntent intIntent = codec(IntIntent.class).decode(jsonTree, this);
            IntIntentId intIntentId = service.installIntIntent(intIntent);
            UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                    .path(INT)
                    .path(Long.toString(intIntentId.id()));
            return Response
                    .created(locationBuilder.build())
                    .build();

            } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

    }

    /**
     * Removes the specified IntIntent.
     *
     * @param  id InIntentId
     * @return 204 NO CONTENT
     */
    @DELETE
    @Path("{id}")
    public Response deleteIntent(@PathParam("id") String id) {
        IntService service = get(IntService.class);
        service.removeIntIntent(IntIntentId.valueOf(Long.parseLong(id)));
        return Response.noContent().build();
    }

}
