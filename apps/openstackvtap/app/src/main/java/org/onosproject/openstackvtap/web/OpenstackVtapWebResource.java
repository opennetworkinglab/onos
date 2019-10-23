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
package org.onosproject.openstackvtap.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import org.onosproject.openstackvtap.api.OpenstackVtap;
import org.onosproject.openstackvtap.api.OpenstackVtapCriterion;
import org.onosproject.openstackvtap.api.OpenstackVtapId;
import org.onosproject.openstackvtap.api.OpenstackVtapService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.io.InputStream;
import java.util.Set;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static org.onlab.util.Tools.readTreeFromStream;
import static org.onosproject.openstackvtap.util.OpenstackVtapUtil.getVtapTypeFromString;

/**
 * Handles REST API call of openstack vtap.
 */
@Path("vtap")
public class OpenstackVtapWebResource extends AbstractWebResource {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MESSAGE_VTAP = "Received vtap %s request";
    private static final String CREATE = "CREATE";
    private static final String QUERY = "QUERY";
    private static final String DELETE = "DELETE";

    private static final String VTAP = "vtap";
    private static final String CRITERION = "criterion";
    private static final String TYPE = "type";
    private static final String ID = "id";

    private final ObjectNode  root = mapper().createObjectNode();
    private final ArrayNode jsonVtaps = root.putArray("vtaps");

    private final OpenstackVtapService vtapService = get(OpenstackVtapService.class);

    @Context
    private UriInfo uriInfo;

    /**
     * Creates an openstack vTap from the JSON input stream.
     *
     * @param input openstack vtap JSON input stream
     * @return 201 CREATED if the JSON is correct, 400 BAD_REQUEST if the JSON
     * is invalid or duplicated vtap already exists
     *
     * @onos.rsModel OpenstackVtap
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createVtap(InputStream input) {
        log.trace(String.format(MESSAGE_VTAP, CREATE));

        OpenstackVtap vtap = readAndCreateVtap(input);

        UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                .path(VTAP)
                .path(vtap.id().toString());

        return created(locationBuilder.build()).build();
    }

    /**
     * Removes an openstack vTap with the given vTap UUID.
     *
     * @param id openstack vtap UUID
     * @return 204 NO_CONTENT, 400 BAD_REQUEST if the JSON is malformed
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{id}")
    public Response deleteVtap(@PathParam(ID) String id) {
        log.trace(String.format(MESSAGE_VTAP, DELETE + id));

        vtapService.removeVtap(OpenstackVtapId.vtapId(id));
        return noContent().build();
    }

    /**
     * Gets openstack vtap entities.
     *
     * @return 200 OK with openstack vtap entities
     *         404 NOT_FOUND if openstack vtap does not exist
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVtaps() {
        log.trace(String.format(MESSAGE_VTAP, QUERY));

        Set<OpenstackVtap> allVtaps = vtapService.getVtaps(OpenstackVtap.Type.VTAP_ALL);
        Set<OpenstackVtap> txVtaps = vtapService.getVtaps(OpenstackVtap.Type.VTAP_TX);
        Set<OpenstackVtap> rxVtaps = vtapService.getVtaps(OpenstackVtap.Type.VTAP_RX);
        Set<OpenstackVtap> anyVtaps = vtapService.getVtaps(OpenstackVtap.Type.VTAP_ANY);

        Set<OpenstackVtap> vTaps = Sets.newConcurrentHashSet();
        vTaps.addAll(allVtaps);
        vTaps.addAll(txVtaps);
        vTaps.addAll(rxVtaps);
        vTaps.addAll(anyVtaps);

        for (OpenstackVtap vtap : vTaps) {
            ObjectNode json = mapper().createObjectNode();
            json.set(CRITERION, codec(OpenstackVtapCriterion.class)
                    .encode(vtap.vtapCriterion(), this));
            json.put(TYPE, vtap.type().name());
            json.put(ID, vtap.id().toString());

            jsonVtaps.add(json);
        }

        return ok(root).build();
    }

    private OpenstackVtap readAndCreateVtap(InputStream input) {
        OpenstackVtap.Type type = OpenstackVtap.Type.VTAP_ALL;
        OpenstackVtapCriterion criterion;
        try {
            JsonNode jsonTree = readTreeFromStream(mapper().enable(INDENT_OUTPUT), input);
            String typeStr = jsonTree.path(TYPE).asText();

            if (typeStr != null) {
                type = getVtapTypeFromString(typeStr);
            }

            ObjectNode jsonCriterion = jsonTree.path(CRITERION).deepCopy();
            criterion = codec(OpenstackVtapCriterion.class).decode(jsonCriterion, this);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }

        return vtapService.createVtap(type, criterion);
    }
}
