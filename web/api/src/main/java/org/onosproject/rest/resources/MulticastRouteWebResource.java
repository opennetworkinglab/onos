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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.Beta;
import org.onlab.packet.IpAddress;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.mcast.McastRoute;
import org.onosproject.net.mcast.MulticastRouteService;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Set;

/**
 * Manage the multicast routing information.
 */
@Beta
@Path("mcast")
public class MulticastRouteWebResource extends AbstractWebResource {

    /**
     * Get all multicast routes.
     * Returns array of all known multicast routes.
     *
     * @return 200 OK with array of all known multicast routes
     * @onos.rsModel McastRoutesGet
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getRoutes() {
        Set<McastRoute> routes = get(MulticastRouteService.class).getRoutes();
        ObjectNode root = encodeArray(McastRoute.class, "routes", routes);
        return ok(root).build();
    }

    /**
     * Create new multicast route.
     * Creates a new route in the multicast RIB.
     *
     * @onos.rsModel McastRoutePost
     * @param stream multicast route JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createRoute(InputStream stream) {
        MulticastRouteService service = get(MulticastRouteService.class);
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            McastRoute route = codec(McastRoute.class).decode(jsonTree, this);
            service.add(route);
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }

        return Response
                .created(URI.create(""))
                .build();
    }

    /**
     * Remove a multicast route.
     * Removes a route from the multicast RIB.
     *
     * @param stream multicast route JSON
     * @return 204 NO CONTENT
     * @onos.rsModel McastRoutePost
     */
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public Response deleteRoute(InputStream stream) {
        MulticastRouteService service = get(MulticastRouteService.class);
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            McastRoute route = codec(McastRoute.class).decode(jsonTree, this);
            service.remove(route);
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
        return Response.noContent().build();
    }

    /**
     * Create a sink for a multicast route.
     * Creates a new sink for an existing multicast route.
     *
     * @onos.rsModel McastSinkPost
     * @param group group IP address
     * @param source source IP address
     * @param stream sink JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("sinks/{group}/{source}")
    public Response addSinks(@PathParam("group") String group,
                             @PathParam("source") String source,
                             InputStream stream) {
        MulticastRouteService service = get(MulticastRouteService.class);
        try {
            McastRoute route = new McastRoute(IpAddress.valueOf(source), IpAddress.valueOf(group),
                    McastRoute.Type.STATIC);
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            jsonTree.path("sinks").forEach(node -> {
                ConnectPoint sink = ConnectPoint.deviceConnectPoint(node.asText());
                service.addSink(route, sink);
            });
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }

        return Response
                .created(URI.create(""))
                .build();
    }
}
